import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ResearcherHeader from '../../../components/researcher/ResearcherHeader'
import { deleteQuestion, listQuestions } from '../api/questionBankApi'
import { describeError } from '../../../shared/types/apiError'
import {
  QUESTION_TYPE_LABELS,
  type QuestionSummaryResponse,
  type QuestionType,
} from '../../../shared/types/question'

const PAGE_SIZE = 20

export default function QuestionBankListPage() {
  const [questions, setQuestions] = useState<QuestionSummaryResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [typeFilter, setTypeFilter] = useState<QuestionType | ''>('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function refresh(targetPage: number) {
    setIsLoading(true)
    setError('')
    try {
      const result = await listQuestions({
        page: targetPage,
        size: PAGE_SIZE,
        type: typeFilter || undefined,
        search: search || undefined,
      })
      setQuestions(result.content)
      setTotalPages(result.totalPages)
      setPage(result.page)
    } catch (err) {
      setError(describeError(err, 'Failed to load questions.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    refresh(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [typeFilter, search])

  async function handleDelete(questionId: string) {
    if (!window.confirm('Delete this question? This cannot be undone.')) {
      return
    }
    try {
      await deleteQuestion(questionId)
      // Deleting is always allowed, even if a draft still enables this
      // question (contract 5.5) - the draft will show it as a missing
      // reference next time it's loaded, so a plain refresh is enough.
      await refresh(page)
    } catch (err) {
      setError(describeError(err, 'Failed to delete question.'))
    }
  }

  return (
    <div className="min-h-screen bg-[#f3f6f8] text-gray-950">
      <ResearcherHeader activePage="questions" />

      <main className="mx-auto w-full max-w-4xl px-4 py-8">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-gray-900">Question bank</h1>
          <Link
            to="/questions/new"
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            + New question
          </Link>
        </div>

        <div className="mb-4 flex flex-wrap gap-3">
          <select
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value as QuestionType | '')}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="">All types</option>
            {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>

          <form
            onSubmit={(event) => {
              event.preventDefault()
              setSearch(searchInput)
            }}
            className="flex flex-1 gap-2"
          >
            <input
              type="text"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Search question text..."
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            <button
              type="submit"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm hover:bg-gray-50"
            >
              Search
            </button>
          </form>
        </div>

        {error && (
          <div role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {isLoading ? (
          <p className="text-sm text-gray-500">Loading...</p>
        ) : questions.length === 0 ? (
          <p className="text-sm text-gray-500">No questions yet.</p>
        ) : (
          <>
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {questions.map((question) => (
                <li
                  key={question.questionId}
                  className="flex items-center justify-between px-4 py-3"
                >
                  <div>
                    <p className="text-sm font-medium text-gray-900">{question.questionText}</p>
                    <p className="text-xs text-gray-500">
                      {QUESTION_TYPE_LABELS[question.type]}
                      {' · '}
                      Updated{' '}
                      <time dateTime={question.updatedAt}>
                        {new Date(question.updatedAt).toLocaleString()}
                      </time>
                    </p>
                  </div>
                  <div className="flex gap-3 text-sm">
                    <Link
                      to={`/questions/${question.questionId}/edit`}
                      className="text-blue-600 hover:underline"
                    >
                      Edit
                    </Link>
                    <button
                      type="button"
                      onClick={() => handleDelete(question.questionId)}
                      className="text-red-600 hover:underline"
                    >
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>

            {totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between text-sm">
                <button
                  type="button"
                  onClick={() => refresh(page - 1)}
                  disabled={page <= 0}
                  className="rounded-lg border border-gray-300 px-3 py-1.5 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Previous
                </button>
                <span className="text-gray-500">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => refresh(page + 1)}
                  disabled={page + 1 >= totalPages}
                  className="rounded-lg border border-gray-300 px-3 py-1.5 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}
