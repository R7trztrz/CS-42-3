import { useState } from 'react'
import { ConsentView } from '../../pages/participant/ParticipantDemoTemplate'
import type { ConsentDocument } from '../model/participantSession'

type ConsentStepProps = {
  consentDocument: ConsentDocument
  isPending: boolean
  errorMessage: string | null
  onDecide: (accepted: boolean) => Promise<boolean>
}

export default function ConsentStep({ consentDocument, isPending, errorMessage, onDecide }: ConsentStepProps) {
  const [acknowledged, setAcknowledged] = useState(false)
  const [pendingDecision, setPendingDecision] = useState<'accept' | 'decline' | null>(null)
  const decide = (accepted: boolean) => {
    setPendingDecision(accepted ? 'accept' : 'decline')
    void onDecide(accepted).finally(() => setPendingDecision(null))
  }

  return (
    <ConsentView
      onBack={() => decide(false)}
      onNext={() => decide(true)}
      pending={isPending}
      nextDisabled={!acknowledged}
      backLabel={pendingDecision === 'decline' ? 'Submitting...' : 'I do not agree'}
      nextLabel={pendingDecision === 'accept' ? 'Submitting...' : 'I agree to participate'}
      notice={!consentDocument.approvedForProduction && (
        <p className="mt-3 rounded-sm border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900" role="note">
          This consent text is a non-production placeholder and has not been approved for real studies.
        </p>
      )}
      document={
        <div className="demo-consent-grid">
          <article>
            <span>01</span>
            <div><h2>{consentDocument.title}</h2><p className="study-introduction__description">{consentDocument.content}</p></div>
          </article>
          {errorMessage && <p className="mt-4 text-red-700" role="alert">{errorMessage}</p>}
        </div>
      }
      confirmation={
        <label className="demo-consent-check">
          <input type="checkbox" checked={acknowledged} disabled={isPending} onChange={(event) => setAcknowledged(event.target.checked)} />
          <span><strong>I have read the participant information and agree to take part.</strong></span>
        </label>
      }
    />
  )
}
