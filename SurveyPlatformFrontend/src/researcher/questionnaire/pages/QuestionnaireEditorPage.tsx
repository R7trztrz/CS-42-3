import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import BranchRuleEditor from '../components/BranchRuleEditor'
import { getQuestionnaire, saveQuestionnaire } from '../api/questionnaireApi'
import { getQuestion, listQuestions } from '../../questionBank/api/questionBankApi'
import { asApiError, describeError } from '../../../shared/types/apiError'
import type { QuestionSummaryResponse } from '../../../shared/types/question'
import type {
  QuestionnaireContentSource,
  QuestionnaireEditorItem,
  QuestionnaireValidationIssue,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'

let clientIdSeq = 1
function newClientId(): string {
  return `client-${clientIdSeq++}`
}

// Large enough that the picker rarely needs real pagination for a single
// researcher's bank; a bank past this size would need a proper paged/
// searchable picker instead of this flat list.
const BANK_PICKER_PAGE_SIZE = 100

export default function QuestionnaireEditorPage() {
  const { studyId } = useParams<{ studyId: string }>()

  const [items, setItems] = useState<QuestionnaireEditorItem[]>([])
  const [bankQuestions, setBankQuestions] = useState<QuestionSummaryResponse[]>([])
  const [expectedVersion, setExpectedVersion] = useState<number | null>(null)
  const [contentSource, setContentSource] = useState<QuestionnaireContentSource>('LIVE_DRAFT')
  const [validationIssues, setValidationIssues] = useState<QuestionnaireValidationIssue[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const isReadOnly = contentSource === 'PUBLISHED_SNAPSHOT'

  useEffect(() => {
    if (!studyId) return

    Promise.all([
      getQuestionnaire(studyId),
      listQuestions({ size: BANK_PICKER_PAGE_SIZE }),
    ])
      .then(([questionnaire, questionPage]) => {
        setBankQuestions(questionPage.content)

        if (!questionnaire) {
          // Contract 6.1: QUESTIONNAIRE_NOT_FOUND means no draft has been
          // saved yet, not an error - start from an empty, unsaved editor.
          setItems([])
          setExpectedVersion(null)
          setContentSource('LIVE_DRAFT')
          setValidationIssues([])
          return
        }

        // Give every loaded item a stable clientId first, then resolve each
        // branch rule's targetItemId against that same mapping - two
        // passes, same reason as the backend's own two-pass build.
        const clientIdByItemId = new Map(
          questionnaire.items.map((item) => [item.itemId, newClientId()]),
        )
        setItems(
          questionnaire.items.map((item) => ({
            clientId: clientIdByItemId.get(item.itemId)!,
            itemId: item.itemId,
            questionId: item.question?.questionId ?? '',
            question: item.question,
            missing: item.missing,
            branchRules: item.branchRules.map((rule) => ({
              sourceOptionId: rule.sourceOptionId,
              sourceScaleValue: rule.sourceScaleValue,
              targetClientId:
                clientIdByItemId.get(rule.targetItemId) ?? clientIdByItemId.get(item.itemId)!,
            })),
          })),
        )
        setExpectedVersion(questionnaire.version)
        setContentSource(questionnaire.contentSource)
        setValidationIssues(questionnaire.validationIssues)
      })
      .catch((err) => setError(describeError(err, 'Failed to load questionnaire.')))
      .finally(() => setIsLoading(false))
  }, [studyId])

  if (!studyId) {
    return (
      <p className="mx-auto max-w-4xl px-4 py-8 text-sm text-red-600">Missing study identifier.</p>
    )
  }

  const enabledQuestionIds = new Set(items.map((i) => i.questionId))
  const availableToAdd = bankQuestions.filter((q) => !enabledQuestionIds.has(q.questionId))

  async function addQuestion(questionId: string) {
    // The list endpoint returns summaries; refetch full detail (options/
    // scale bounds) so branch-rule triggers have something to offer.
    const question = await getQuestion(questionId)
    setItems((prev) => [
      ...prev,
      {
        clientId: newClientId(),
        itemId: null,
        questionId: question.questionId,
        question,
        missing: false,
        branchRules: [],
      },
    ])
  }

  function removeItem(clientId: string) {
    setItems((prev) =>
      prev
        .filter((item) => item.clientId !== clientId)
        // Dropping an item also drops any rule that targeted it, matching
        // the backend's full-replace save semantics.
        .map((item) => ({
          ...item,
          branchRules: item.branchRules.filter((r) => r.targetClientId !== clientId),
        })),
    )
  }

  function moveItem(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= items.length) return
    const next = items.slice()
    ;[next[index], next[target]] = [next[target], next[index]]
    setItems(next)
  }

  async function handleSave() {
    // Redundant with the component-level guard above: TypeScript doesn't
    // carry that narrowing into this closure, and this button only renders
    // once studyId is known to be defined anyway.
    if (!studyId) return

    setError('')
    setMessage('')
    setIsSaving(true)
    try {
      const positionByClientId = new Map(items.map((item, index) => [item.clientId, index]))
      const request: SaveQuestionnaireRequest = {
        expectedVersion,
        items: items.map((item) => ({
          itemId: item.itemId,
          questionId: item.questionId,
          branchRules: item.branchRules.map((rule) => ({
            sourceOptionId: rule.sourceOptionId,
            sourceScaleValue: rule.sourceScaleValue,
            targetPosition: positionByClientId.get(rule.targetClientId) ?? 0,
          })),
        })),
      }
      const saved = await saveQuestionnaire(studyId, request)
      setExpectedVersion(saved.version)
      setContentSource(saved.contentSource)
      setValidationIssues(saved.validationIssues)
      setMessage('Questionnaire saved.')
    } catch (err) {
      const apiError = asApiError(err)
      if (apiError?.code === 'QUESTIONNAIRE_VERSION_CONFLICT') {
        setError('Someone else changed this questionnaire. Reload the page before saving again.')
      } else if (apiError?.code === 'QUESTIONNAIRE_LOCKED') {
        setError('This study is no longer a draft, so its questionnaire cannot be edited.')
        setContentSource('PUBLISHED_SNAPSHOT')
      } else {
        setError(describeError(err, 'Failed to save questionnaire.'))
      }
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return <p className="mx-auto max-w-4xl px-4 py-8 text-sm text-gray-500">Loading...</p>
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">Questionnaire editor</h1>
        {isReadOnly ? (
          <span className="rounded-lg border border-sky-300 bg-sky-50 px-4 py-2 text-sm font-medium text-sky-800">
            Published - read only
          </span>
        ) : (
          <button
            type="button"
            onClick={handleSave}
            disabled={isSaving}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSaving ? 'Saving...' : 'Save questionnaire'}
          </button>
        )}
      </div>

      {isReadOnly && (
        <div className="mb-4 rounded-lg bg-sky-50 px-4 py-3 text-sm text-sky-800">
          This study has been published. The questionnaire below is the frozen snapshot
          participants are seeing; editing the question bank will not change it.
        </div>
      )}

      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {message && (
        <div role="status" className="mb-4 rounded-lg bg-green-50 px-4 py-3 text-sm text-green-700">
          {message}
        </div>
      )}
      {validationIssues.length > 0 && (
        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <p className="mb-1 font-medium">This questionnaire can't be published yet:</p>
          <ul className="list-inside list-disc">
            {validationIssues.map((issue, index) => (
              <li key={index}>{issue.message}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="mb-6 space-y-3">
        {items.length === 0 && (
          <p className="rounded-lg border border-dashed border-gray-300 px-4 py-6 text-center text-sm text-gray-500">
            No questions enabled yet. Add some from the question bank below.
          </p>
        )}

        {items.map((item, index) => (
          <div key={item.clientId} className="rounded-lg border border-gray-200 bg-white p-4">
            <div className="mb-2 flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-medium text-gray-400">Q{index + 1}</p>
                {item.missing || !item.question ? (
                  <p className="text-sm font-medium text-red-600">
                    This question was deleted from the bank. Remove it to publish.
                  </p>
                ) : (
                  <p className="text-sm font-medium text-gray-900">{item.question.questionText}</p>
                )}
              </div>
              {!isReadOnly && (
                <div className="flex shrink-0 gap-1 text-sm">
                  <button
                    type="button"
                    onClick={() => moveItem(index, -1)}
                    disabled={index === 0}
                    className="rounded px-2 py-1 hover:bg-gray-100 disabled:opacity-30"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    onClick={() => moveItem(index, 1)}
                    disabled={index === items.length - 1}
                    className="rounded px-2 py-1 hover:bg-gray-100 disabled:opacity-30"
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    onClick={() => removeItem(item.clientId)}
                    className="rounded px-2 py-1 text-red-600 hover:bg-red-50"
                  >
                    Remove
                  </button>
                </div>
              )}
            </div>

            {!isReadOnly && item.question && (
              <BranchRuleEditor
                question={item.question}
                branchRules={item.branchRules}
                otherItems={items.filter((i) => i.clientId !== item.clientId)}
                onChange={(branchRules) =>
                  setItems((prev) =>
                    prev.map((i) => (i.clientId === item.clientId ? { ...i, branchRules } : i)),
                  )
                }
              />
            )}
          </div>
        ))}
      </div>

      {!isReadOnly && (
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <p className="mb-3 text-sm font-medium text-gray-700">Add from question bank</p>
          {availableToAdd.length === 0 ? (
            <p className="text-sm text-gray-500">
              Every question in your bank is already enabled, or your bank is empty.
            </p>
          ) : (
            <ul className="space-y-2">
              {availableToAdd.map((q) => (
                <li key={q.questionId} className="flex items-center justify-between text-sm">
                  <span>{q.questionText}</span>
                  <button
                    type="button"
                    onClick={() => addQuestion(q.questionId)}
                    className="text-blue-600 hover:underline"
                  >
                    + Add
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
