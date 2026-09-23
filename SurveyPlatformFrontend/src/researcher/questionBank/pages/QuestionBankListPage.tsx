import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteQuestion, listQuestions } from '../api/questionBankApi'
import {
  QUESTION_TYPE_LABELS,
  type QuestionSummaryResponse,
  type QuestionType,
} from '../../../shared/types/question'

export default function QuestionBankListPage() {
  const [questions, setQuestions] = useState<QuestionSummaryResponse[]>([])
  const [typeFilter, setTypeFilter] = useState<QuestionType | ''>('')
  const [keyword, setKeyword] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function refresh() {
    setIsLoading(true)
    setError('')
    try {
      setQuestions(await listQuestions(typeFilter, keyword))
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载题目失败。')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [typeFilter])

  async function handleDelete(id: string) {
    if (!window.confirm('确认删除这道题目？此操作不可撤销。')) {
      return
    }
    try {
      await deleteQuestion(id)
      await refresh()
    } catch (err) {
      // 当题目仍被某个问卷引用时，后端会返回 409 QUESTION_IN_USE
      // （见 QuestionInUseException），这里直接原样展示后端的错误信息。
      setError(err instanceof Error ? err.message : '删除题目失败。')
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">题库管理</h1>
        <Link
          to="/researcher/questions/new"
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + 新建题目
        </Link>
      </div>

      <div className="mb-4 flex flex-wrap gap-3">
        <select
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value as QuestionType | '')}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">全部类型</option>
          {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>

        <form
          onSubmit={(event) => {
            event.preventDefault()
            refresh()
          }}
          className="flex flex-1 gap-2"
        >
          <input
            type="text"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索题目内容..."
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <button
            type="submit"
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm hover:bg-gray-50"
          >
            搜索
          </button>
        </form>
      </div>

      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {isLoading ? (
        <p className="text-sm text-gray-500">加载中...</p>
      ) : questions.length === 0 ? (
        <p className="text-sm text-gray-500">暂无题目。</p>
      ) : (
        <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
          {questions.map((question) => (
            <li key={question.id} className="flex items-center justify-between px-4 py-3">
              <div>
                <p className="text-sm font-medium text-gray-900">{question.questionText}</p>
                <p className="text-xs text-gray-500">{QUESTION_TYPE_LABELS[question.type]}</p>
              </div>
              <div className="flex gap-3 text-sm">
                <Link
                  to={`/researcher/questions/${question.id}/edit`}
                  className="text-blue-600 hover:underline"
                >
                  编辑
                </Link>
                <button
                  type="button"
                  onClick={() => handleDelete(question.id)}
                  className="text-red-600 hover:underline"
                >
                  删除
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
