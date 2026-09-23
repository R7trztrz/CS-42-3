import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'

// UC-29：查看研究内容之前的知情同意关卡。
export default function ConsentPage() {
  const { recordConsent } = useSession()
  const navigate = useNavigate()
  const [agreed, setAgreed] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleContinue() {
    setIsSubmitting(true)
    await recordConsent()
    navigate('../feed', { replace: true })
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-12">
      <h1 className="mb-4 text-2xl font-semibold text-gray-900">知情同意</h1>

      <div className="mb-6 space-y-3 rounded-lg bg-white p-5 text-sm text-gray-700 shadow-sm">
        <p>
          本研究会请你浏览一段模拟社交媒体信息流，并回答一份简短的问卷。你的作答
          和行为数据（例如停留时长、点击情况）将用于研究目的，并会被保密处理。
        </p>
        <p>你可以在任何时候退出研究，不会受到任何影响。</p>
        <p className="text-xs text-gray-400">
          （这里是占位文案 —— 研究者自定义的知情同意文本还没接通。）
        </p>
      </div>

      <label className="mb-6 flex items-start gap-2 text-sm text-gray-700">
        <input
          type="checkbox"
          checked={agreed}
          onChange={(event) => setAgreed(event.target.checked)}
          className="mt-1"
        />
        我已阅读以上内容，并同意参与本研究。
      </label>

      <button
        type="button"
        onClick={handleContinue}
        disabled={!agreed || isSubmitting}
        className="rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isSubmitting ? '正在继续...' : '同意并继续'}
      </button>
    </div>
  )
}
