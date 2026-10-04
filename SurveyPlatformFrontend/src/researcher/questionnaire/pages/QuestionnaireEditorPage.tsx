import { useEffect, useReducer, useState, type FormEvent } from 'react'
import { useBlocker, useParams } from 'react-router-dom'
import UnsavedChangesDialogTemplate from '../../../components/studies/UnsavedChangesDialogTemplate'
import BranchRuleEditor from '../components/BranchRuleEditor'
import { getQuestionnaire, saveQuestionnaire } from '../api/questionnaireApi'
import { getQuestion, listQuestions } from '../../questionBank/api/questionBankApi'
import { asApiError, describeError } from '../../../shared/types/apiError'
import {
  QUESTION_TYPE_LABELS,
  type QuestionSummaryResponse,
  type QuestionType,
} from '../../../shared/types/question'
import type {
  QuestionnaireContentSource,
  QuestionnaireEditorItem,
} from '../../../shared/types/questionnaire'
import {
  apiErrorDetailToView,
  buildSaveQuestionnaireRequest,
  hydrateQuestionnaireItems,
  questionnaireEditorReducer,
  questionnaireIssueLocation,
  questionnaireItemsFingerprint,
  validationIssueToView,
  type QuestionnaireIssueView,
} from '../model/questionnaireEditorModel'

let clientIdSeq = 1
function newClientId(): string {
  return `client-${clientIdSeq++}`
}

const BANK_PICKER_PAGE_SIZE = 20

export default function QuestionnaireEditorPage() {
  const { studyId } = useParams<{ studyId: string }>()
  const [editor, dispatch] = useReducer(questionnaireEditorReducer, {
    items: [],
    past: [],
  })
  const items = editor.items

  const [savedFingerprint, setSavedFingerprint] = useState(
    questionnaireItemsFingerprint([]),
  )
  const [bankQuestions, setBankQuestions] = useState<QuestionSummaryResponse[]>([])
  const [bankPage, setBankPage] = useState(0)
  const [bankTotalPages, setBankTotalPages] = useState(0)
  const [bankType, setBankType] = useState<QuestionType | ''>('')
  const [bankSearchInput, setBankSearchInput] = useState('')
  const [bankSearch, setBankSearch] = useState('')
  const [isBankLoading, setIsBankLoading] = useState(false)
  const [bankError, setBankError] = useState('')
  const [addingQuestionId, setAddingQuestionId] = useState<string | null>(null)

  const [expectedVersion, setExpectedVersion] = useState<number | null>(null)
  const [contentSource, setContentSource] =
    useState<QuestionnaireContentSource>('LIVE_DRAFT')
  const [validationIssues, setValidationIssues] = useState<QuestionnaireIssueView[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const isReadOnly = contentSource === 'PUBLISHED_SNAPSHOT'
  const isDirty = questionnaireItemsFingerprint(items) !== savedFingerprint
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      isDirty &&
      !isReadOnly &&
      (currentLocation.pathname !== nextLocation.pathname ||
        currentLocation.search !== nextLocation.search),
  )

  useEffect(() => {
    if (!studyId) return

    let cancelled = false
    setIsLoading(true)
    setError('')
    getQuestionnaire(studyId)
      .then((questionnaire) => {
        if (cancelled) return

        if (!questionnaire) {
          const emptyItems: QuestionnaireEditorItem[] = []
          dispatch({ type: 'reset', items: emptyItems })
          setSavedFingerprint(questionnaireItemsFingerprint(emptyItems))
          setExpectedVersion(null)
          setContentSource('LIVE_DRAFT')
          setValidationIssues([])
          return
        }

        const loadedItems = hydrateQuestionnaireItems(
          questionnaire.items,
          [],
          newClientId,
        )
        dispatch({ type: 'reset', items: loadedItems })
        setSavedFingerprint(questionnaireItemsFingerprint(loadedItems))
        setExpectedVersion(questionnaire.version)
        setContentSource(questionnaire.contentSource)
        setValidationIssues(questionnaire.validationIssues.map(validationIssueToView))
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(describeError(loadError, 'Failed to load questionnaire.'))
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [studyId])

  useEffect(() => {
    if (!studyId) return

    let cancelled = false
    setIsBankLoading(true)
    setBankError('')
    setBankQuestions([])
    listQuestions({
      page: 0,
      size: BANK_PICKER_PAGE_SIZE,
      type: bankType || undefined,
      search: bankSearch || undefined,
    })
      .then((questionPage) => {
        if (cancelled) return
        setBankQuestions(questionPage.content)
        setBankPage(questionPage.page)
        setBankTotalPages(questionPage.totalPages)
      })
      .catch((loadError) => {
        if (!cancelled) {
          setBankError(describeError(loadError, 'Failed to load the question bank.'))
        }
      })
      .finally(() => {
        if (!cancelled) setIsBankLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [bankSearch, bankType, studyId])

  useEffect(() => {
    if (!isDirty || isReadOnly) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty, isReadOnly])

  if (!studyId) {
    return (
      <p className="mx-auto max-w-4xl px-4 py-8 text-sm text-red-600">
        Missing study identifier.
      </p>
    )
  }

  const enabledQuestionIds = new Set(items.map((item) => item.questionId))
  const availableToAdd = bankQuestions.filter(
    (question) => !enabledQuestionIds.has(question.questionId),
  )

  function clearEditFeedback() {
    setError('')
    setMessage('')
    setValidationIssues([])
  }

  async function loadMoreBankQuestions() {
    if (isBankLoading || bankPage + 1 >= bankTotalPages) return

    setIsBankLoading(true)
    setBankError('')
    try {
      const questionPage = await listQuestions({
        page: bankPage + 1,
        size: BANK_PICKER_PAGE_SIZE,
        type: bankType || undefined,
        search: bankSearch || undefined,
      })
      setBankQuestions((previous) => {
        const byId = new Map(previous.map((question) => [question.questionId, question]))
        questionPage.content.forEach((question) => byId.set(question.questionId, question))
        return [...byId.values()]
      })
      setBankPage(questionPage.page)
      setBankTotalPages(questionPage.totalPages)
    } catch (loadError) {
      setBankError(describeError(loadError, 'Failed to load more questions.'))
    } finally {
      setIsBankLoading(false)
    }
  }

  async function addQuestion(questionId: string) {
    setAddingQuestionId(questionId)
    setBankError('')
    try {
      // The list endpoint returns summaries; refetch full detail so branch
      // triggers have the current option IDs and scale bounds.
      const question = await getQuestion(questionId)
      dispatch({
        type: 'append',
        item: {
          clientId: newClientId(),
          itemId: null,
          questionId: question.questionId,
          question,
          missing: false,
          branchRules: [],
        },
      })
      clearEditFeedback()
    } catch (loadError) {
      setBankError(describeError(loadError, 'Failed to add the question.'))
    } finally {
      setAddingQuestionId(null)
    }
  }

  function removeItem(clientId: string) {
    dispatch({ type: 'remove', clientId })
    clearEditFeedback()
  }

  function moveItem(index: number, direction: -1 | 1) {
    dispatch({ type: 'move', index, direction })
    clearEditFeedback()
  }

  function undoLastChange() {
    dispatch({ type: 'undo' })
    clearEditFeedback()
  }

  async function handleSave(): Promise<boolean> {
    if (!studyId) return false

    setError('')
    setMessage('')
    setIsSaving(true)
    try {
      const requestItems = items
      const saved = await saveQuestionnaire(
        studyId,
        buildSaveQuestionnaireRequest(requestItems, expectedVersion),
      )
      const savedItems = hydrateQuestionnaireItems(
        saved.items,
        requestItems,
        newClientId,
      )
      dispatch({ type: 'reset', items: savedItems })
      setSavedFingerprint(questionnaireItemsFingerprint(savedItems))
      setExpectedVersion(saved.version)
      setContentSource(saved.contentSource)
      setValidationIssues(saved.validationIssues.map(validationIssueToView))
      setMessage('Questionnaire saved.')
      return true
    } catch (saveError) {
      const apiError = asApiError(saveError)
      if (apiError?.details?.length) {
        setValidationIssues(apiError.details.map(apiErrorDetailToView))
      }
      if (apiError?.code === 'QUESTIONNAIRE_VERSION_CONFLICT') {
        setError('Someone else changed this questionnaire. Reload the page before saving again.')
      } else if (apiError?.code === 'QUESTIONNAIRE_LOCKED') {
        setError('This study is no longer a draft, so its questionnaire cannot be edited.')
        setContentSource('PUBLISHED_SNAPSHOT')
      } else {
        setError(describeError(saveError, 'Failed to save questionnaire.'))
      }
      return false
    } finally {
      setIsSaving(false)
    }
  }

  async function handleSaveAndLeave() {
    const didSave = await handleSave()
    if (didSave && blocker.state === 'blocked') {
      blocker.proceed()
    }
  }

  function handleBankSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBankSearch(bankSearchInput.trim())
  }

  if (isLoading) {
    return <p className="mx-auto max-w-4xl px-4 py-8 text-sm text-gray-500">Loading...</p>
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Questionnaire editor</h1>
          {isDirty && !isReadOnly && (
            <p className="mt-1 text-xs font-medium text-amber-700">Unsaved changes</p>
          )}
        </div>
        {isReadOnly ? (
          <span className="rounded-lg border border-sky-300 bg-sky-50 px-4 py-2 text-sm font-medium text-sky-800">
            Published - read only
          </span>
        ) : (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={undoLastChange}
              disabled={isSaving || editor.past.length === 0}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Undo
            </button>
            <button
              type="button"
              onClick={() => void handleSave()}
              disabled={isSaving}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSaving ? 'Saving...' : 'Save questionnaire'}
            </button>
          </div>
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
          <p className="mb-2 font-medium">Questionnaire validation details:</p>
          <ul className="space-y-2">
            {validationIssues.map((issue, index) => (
              <li key={`${issue.code}-${index}`} className="rounded bg-white/60 px-3 py-2">
                <p>{issue.message}</p>
                <p className="mt-1 break-all font-mono text-xs text-amber-900">
                  {questionnaireIssueLocation(issue).join(' · ')}
                </p>
              </li>
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
                    disabled={isSaving || index === 0}
                    aria-label={`Move question ${index + 1} up`}
                    className="rounded px-2 py-1 hover:bg-gray-100 disabled:opacity-30"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    onClick={() => moveItem(index, 1)}
                    disabled={isSaving || index === items.length - 1}
                    aria-label={`Move question ${index + 1} down`}
                    className="rounded px-2 py-1 hover:bg-gray-100 disabled:opacity-30"
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    onClick={() => removeItem(item.clientId)}
                    disabled={isSaving}
                    className="rounded px-2 py-1 text-red-600 hover:bg-red-50 disabled:opacity-40"
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
                items={items}
                sourceClientId={item.clientId}
                disabled={isSaving}
                onChange={(branchRules) => {
                  dispatch({
                    type: 'replace-branch-rules',
                    clientId: item.clientId,
                    branchRules,
                  })
                  clearEditFeedback()
                }}
              />
            )}
          </div>
        ))}
      </div>

      {!isReadOnly && (
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <p className="mb-3 text-sm font-medium text-gray-700">Add from question bank</p>
          <div className="mb-4 flex flex-wrap gap-2">
            <select
              value={bankType}
              onChange={(event) => setBankType(event.target.value as QuestionType | '')}
              disabled={isBankLoading}
              aria-label="Filter question bank by type"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm disabled:opacity-50"
            >
              <option value="">All types</option>
              {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            <form onSubmit={handleBankSearch} className="flex min-w-64 flex-1 gap-2">
              <input
                type="search"
                value={bankSearchInput}
                onChange={(event) => setBankSearchInput(event.target.value)}
                placeholder="Search question text..."
                aria-label="Search question bank"
                className="min-w-0 flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              <button
                type="submit"
                disabled={isBankLoading}
                className="rounded-lg border border-gray-300 px-3 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
              >
                Search
              </button>
            </form>
          </div>

          {bankError && (
            <div role="alert" className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {bankError}
            </div>
          )}

          {availableToAdd.length === 0 && !isBankLoading ? (
            <p className="text-sm text-gray-500">
              No matching questions are available to add on the loaded pages.
            </p>
          ) : (
            <ul className="space-y-2">
              {availableToAdd.map((question) => (
                <li key={question.questionId} className="flex items-center justify-between gap-4 text-sm">
                  <span>{question.questionText}</span>
                  <button
                    type="button"
                    onClick={() => void addQuestion(question.questionId)}
                    disabled={isSaving || addingQuestionId !== null}
                    className="shrink-0 text-blue-600 hover:underline disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    {addingQuestionId === question.questionId ? 'Adding...' : '+ Add'}
                  </button>
                </li>
              ))}
            </ul>
          )}

          {isBankLoading && (
            <p role="status" className="mt-3 text-sm text-gray-500">Loading questions...</p>
          )}
          {bankPage + 1 < bankTotalPages && (
            <button
              type="button"
              onClick={() => void loadMoreBankQuestions()}
              disabled={isBankLoading}
              className="mt-4 rounded-lg border border-gray-300 px-3 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
            >
              Load more questions
            </button>
          )}
        </div>
      )}

      {blocker.state === 'blocked' && (
        <UnsavedChangesDialogTemplate
          isSaving={isSaving}
          onStay={() => blocker.reset()}
          onLeave={() => blocker.proceed()}
          onSaveAndLeave={() => void handleSaveAndLeave()}
        />
      )}
    </div>
  )
}
