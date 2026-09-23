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

// 目前还没有研究管理（study）相关的前端页面（M2），所以这个页面可以直接
// 访问预览；等研究创建/列表页上线后，应该从那边带着真实 studyId 跳转过来，
// 而不是用下面这个兜底值。
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

        // 先给每道已加载的题目分配稳定的 clientId，再用这份数组去解析每条
        // 跳转规则的 targetPosition —— 分两步走，原因和后端
        // Questionnaire#replaceItems 的做法一样。
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
      .catch((err) => setError(err instanceof Error ? err.message : '加载问卷失败。'))
      .finally(() => setIsLoading(false))
  }, [studyId])

  const enabledQuestionIds = new Set(items.map((i) => i.question.id))
  const availableToAdd = bankQuestions.filter((q) => !enabledQuestionIds.has(q.id))

  async function addQuestion(questionId: string) {
    // 列表接口只返回摘要，这里重新拉一次完整详情（选项/刻度范围），
    // 跳转规则的触发值列表才有内容可选。
    const question = await getQuestion(questionId)
    setItems((prev) => [...prev, { clientId: newClientId(), question, branchRules: [] }])
  }

  function removeItem(clientId: string) {
    setItems((prev) =>
      prev
        .filter((item) => item.clientId !== clientId)
        // 移除一道题目时，指向它的跳转规则也要一并去掉，
        // 和后端整体替换保存的语义保持一致。
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
      setMessage('问卷已保存。')
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存问卷失败。')
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return <p className="mx-auto max-w-4xl px-4 py-8 text-sm text-gray-500">加载中...</p>
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">问卷编排器</h1>
        <button
          type="button"
          onClick={handleSave}
          disabled={isSaving}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSaving ? '保存中...' : '保存问卷'}
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
            还没有启用任何题目，从下面的题库里添加一些吧。
          </p>
        )}

        {items.map((item, index) => (
          <div key={item.clientId} className="rounded-lg border border-gray-200 bg-white p-4">
            <div className="mb-2 flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-medium text-gray-400">第 {index + 1} 题</p>
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
                  移除
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
        <p className="mb-3 text-sm font-medium text-gray-700">从题库添加</p>
        {availableToAdd.length === 0 ? (
          <p className="text-sm text-gray-500">
            题库里的题目都已启用，或者题库还是空的。
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
                  + 添加
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
