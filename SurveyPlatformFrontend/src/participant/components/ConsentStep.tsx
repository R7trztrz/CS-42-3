import { useState } from 'react'

import type { ConsentDocument } from '../model/participantSession'

type ConsentStepProps = {
  consentDocument: ConsentDocument
  isPending: boolean
  errorMessage: string | null
  onDecide: (accepted: boolean) => Promise<boolean>
}

function ConsentStep({ consentDocument, isPending, errorMessage, onDecide }: ConsentStepProps) {
  // Tracks which button the participant pressed so only that one shows a
  // pending state; both are disabled while any decision is in flight.
  const [pendingDecision, setPendingDecision] = useState<'accept' | 'decline' | null>(null)

  const handleDecide = (accepted: boolean) => {
    setPendingDecision(accepted ? 'accept' : 'decline')
    void onDecide(accepted).finally(() => setPendingDecision(null))
  }

  return (
    <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-6 shadow-sm">
      <p className="text-xs font-semibold uppercase text-emerald-700">Informed consent</p>
      <h2 className="mt-1 text-xl font-semibold text-[#172033]">{consentDocument.title}</h2>

      {!consentDocument.approvedForProduction && (
        <p
          className="mt-3 rounded-sm border border-amber-300 bg-amber-50 px-3 py-2 text-xs font-semibold text-amber-900"
          role="note"
        >
          This consent text is a non-production placeholder and has not been approved for
          real studies.
        </p>
      )}

      <div className="mt-4 max-h-64 overflow-y-auto whitespace-pre-wrap rounded-sm border border-gray-100 bg-gray-50 p-4 text-sm leading-6 text-gray-700">
        {consentDocument.content}
      </div>

      {errorMessage && (
        <p className="mt-3 text-sm font-semibold text-red-700" role="alert">
          {errorMessage}
        </p>
      )}

      <div className="mt-5 flex flex-wrap gap-3">
        <button
          type="button"
          disabled={isPending}
          onClick={() => handleDecide(true)}
          className="rounded-sm bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-60"
        >
          {pendingDecision === 'accept' ? 'Submitting...' : 'I agree to participate'}
        </button>
        <button
          type="button"
          disabled={isPending}
          onClick={() => handleDecide(false)}
          className="rounded-sm border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60"
        >
          {pendingDecision === 'decline' ? 'Submitting...' : 'I do not agree'}
        </button>
      </div>
    </section>
  )
}

export default ConsentStep
