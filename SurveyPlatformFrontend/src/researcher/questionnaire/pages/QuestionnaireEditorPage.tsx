import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import BranchRuleEditor from '../components/BranchRuleEditor'
import { getQuestionnaire, saveQuestionnaire } from '../api/questionnaireApi'
import { getQuestion, listQuestions } from '../../questionBank/api/questionBankApi'
import type { QuestionSummaryResponse } from '../../../shared/types/question'
import type {
  QuestionnaireEditorItem,
  SaveQuestionnaireRequest,
} from '../../../shared/types/questionnaire'

let clientIdSeq = 1
function newClientId(): string {
  return `client-${clientIdSeq++}`
}

// No study-management UI exists yet (M2), so this page is reachable
// directly for preview; once study creation/listing lands, it should link
// here with a real studyId param instead of the fallback below.
const FALLBACK_STUDY_ID = 'demo-study'

export default function QuestionnaireEditorPage() {
  const { studyId = FALLBACK_STUDY_ID } = useParams<{ studyId: string }>()

  const [items, setItems] = useState<QuestionnaireEditorItem[]>([])
  const [bankQuestions, setBankQuestions] = useState<QuestionSummaryResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    Promise.all([getQuestionnaire(studyId), listQuestions()])
      .then(([questionnaire, questions]) => {
        setBankQuestions(questions)

        // Give every loaded item a stable clientId first, then resolve each
        // branch rule's targetPosition against that same array - two passes,
        // same reason as the backend's Questionnaire#replaceItems.
        const clientIds = questionnaire.items.map(() => newClientId())
        setItems(
          questionnaire.items.map((item, index) => ({
            clientId: clientIds[index],
            question: item.question,
            branchRules: item.branchRules.map((rule) => ({
              sourceOptionId: rule.sourceOptionId,
              sourceScaleValue: rule.sourceScaleValue,
              targetClientId: clientIds[rule.targetPosition] ?? clientIds[index],
            })),
          })),
        )
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load questionnaire.'))
      .finally(() => setIsLoading(false))
  }, [studyId])

  const enabledQuestionIds = new Set(items.map((i) => i.question.id))
  const availableToAdd = bankQuestions.filter((q) => !enabledQuestionIds.has(q.id))

  async function addQuestion(questionId: string) {
    // The list endpoint returns summaries; refetch full detail (options/
    // scale bounds) so branch-rule triggers have something to offer.
    const question = await getQuestion(questionId)
    setItems((prev) => [...prev, { clientId: newClientId(), question, branchRules: [] }])
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
    setError('')
    setMessage('')
    setIsSaving(true)
    try {
      const positionByClientId = new Map(items.map((item, index) => [item.clientId, index]))
      const request: SaveQuestionnaireRequest = {
        items: items.map((item) => ({
          questionId: item.question.id,
          branchRules: item.branchRules.map((rule) => ({
            sourceOptionId: rule.sourceOptionId,
            sourceScaleValue: rule.sourceScaleValue,
            targetPosition: positionByClientId.get(rule.targetClientId) ?? 0,
          })),
        })),
      }
      await saveQuestionnaire(studyId, request)
      setMessage('Questionnaire saved.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save questionnaire.')
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
        <button
          type="button"
          onClick={handleSave}
          disabled={isSaving}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSaving ? 'Saving...' : 'Save questionnaire'}
        </button>
      </div>

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
                <p className="text-sm font-medium text-gray-900">{item.question.questionText}</p>
              </div>
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
            </div>

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
          </div>
        ))}
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-4">
        <p className="mb-3 text-sm font-medium text-gray-700">Add from question bank</p>
        {availableToAdd.length === 0 ? (
          <p className="text-sm text-gray-500">
            Every question in your bank is already enabled, or your bank is empty.
          </p>
        ) : (
          <ul className="space-y-2">
            {availableToAdd.map((q) => (
              <li key={q.id} className="flex items-center justify-between text-sm">
                <span>{q.questionText}</span>
                <button
                  type="button"
                  onClick={() => addQuestion(q.id)}
                  className="text-blue-600 hover:underline"
                >
                  + Add
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
