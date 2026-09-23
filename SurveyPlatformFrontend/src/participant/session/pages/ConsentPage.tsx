import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSession } from '../state/SessionContext'

// UC-29: informed consent gate before any study content is shown.
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
      <h1 className="mb-4 text-2xl font-semibold text-gray-900">Informed consent</h1>

      <div className="mb-6 space-y-3 rounded-lg bg-white p-5 text-sm text-gray-700 shadow-sm">
        <p>
          This study asks you to browse a simulated social media feed and answer a short
          questionnaire. Your responses and interaction data (e.g. time spent, clicks) are
          recorded for research purposes and handled confidentially.
        </p>
        <p>You may leave the study at any time without penalty.</p>
        <p className="text-xs text-gray-400">
          (Placeholder copy - the researcher-authored consent text isn't wired up yet.)
        </p>
      </div>

      <label className="mb-6 flex items-start gap-2 text-sm text-gray-700">
        <input
          type="checkbox"
          checked={agreed}
          onChange={(event) => setAgreed(event.target.checked)}
          className="mt-1"
        />
        I have read the information above and agree to take part in this study.
      </label>

      <button
        type="button"
        onClick={handleContinue}
        disabled={!agreed || isSubmitting}
        className="rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isSubmitting ? 'Continuing...' : 'Agree and continue'}
      </button>
    </div>
  )
}
