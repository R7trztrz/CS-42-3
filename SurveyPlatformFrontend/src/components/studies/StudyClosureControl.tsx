import { useRef, useState } from 'react'
import { closeStudy, getStudy, type StudyResponse } from '../../services/studyApi'
import { asApiError } from '../../shared/types/apiError'

type Props = {
  study: StudyResponse
  onStudyChange: (study: StudyResponse) => void
}

export default function StudyClosureControl({ study, onStudyChange }: Props) {
  const [confirming, setConfirming] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const inFlight = useRef(false)
  const trigger = useRef<HTMLButtonElement>(null)

  function dismiss() {
    if (inFlight.current) return
    setConfirming(false)
    trigger.current?.focus()
  }

  async function confirmClose() {
    if (inFlight.current || study.status !== 'COLLECTING') return
    inFlight.current = true
    setPending(true)
    setError('')
    try {
      const updated = await closeStudy(study.id, study.version)
      setConfirming(false)
      onStudyChange(updated)
    } catch (failure) {
      const code = asApiError(failure)?.code
      if (code === 'STUDY_VERSION_CONFLICT' || code === 'STUDY_NOT_CLOSABLE') {
        setConfirming(false)
        try {
          onStudyChange(await getStudy(study.id))
          setError('The study has changed. Review its current status and confirm again if you still want to close it.')
        } catch {
          setError('The study has changed, but could not be refreshed. Reload this page before trying again.')
        }
      } else {
        setError(code === 'STUDY_NOT_FOUND'
          ? 'This study is no longer available. Return to the study list.'
          : 'The study could not be closed. Check your connection and try again.')
      }
    } finally {
      inFlight.current = false
      setPending(false)
    }
  }

  if (study.status === 'DRAFT') return null

  return (
    <div className="flex max-w-lg flex-col items-start gap-2">
      {study.status === 'CLOSED' ? (
        <p role="status" className="text-xs text-sky-800">
          {study.closedAt ? <>Closed <time dateTime={study.closedAt}>{new Date(study.closedAt).toLocaleString()}</time></> : 'Study closed'}
        </p>
      ) : (
        <button ref={trigger} type="button" disabled={pending} onClick={() => { setError(''); setConfirming(true) }}
          className="rounded-sm border border-red-700 bg-white px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-50 disabled:opacity-50">
          Close study
        </button>
      )}
      {error && !confirming && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {confirming && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#101b2b]/55 px-4">
          <section role="dialog" aria-modal="true" aria-labelledby="close-study-heading" aria-describedby="close-study-detail"
            className="w-full max-w-lg rounded-md border border-gray-200 bg-white p-6 shadow-xl"
            onKeyDown={(event) => {
              if (event.key === 'Escape') dismiss()
              if (event.key === 'Tab') {
                const buttons = [...event.currentTarget.querySelectorAll<HTMLButtonElement>('button:not(:disabled)')]
                const target = event.shiftKey ? buttons.at(-1) : buttons[0]
                if (target && (event.shiftKey ? document.activeElement === buttons[0] : document.activeElement === buttons.at(-1))) {
                  event.preventDefault()
                  target.focus()
                }
              }
            }}>
            <p className="text-xs font-semibold uppercase text-red-700">Close study</p>
            <h2 id="close-study-heading" className="mt-1 text-xl font-semibold text-[#172033]">Stop collecting responses?</h2>
            <p id="close-study-detail" className="mt-4 text-sm leading-6 text-gray-600">
              <strong className="break-words text-[#172033]">{study.title}</strong> will be permanently closed.
              Its participant link will stop working and active participant sessions will end.
              Already saved responses and study content will be retained. This cannot be undone.
            </p>
            {error && <p role="alert" className="mt-4 rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
            {pending && <p role="status" className="mt-4 text-sm text-gray-600">Closing study...</p>}
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button autoFocus type="button" onClick={dismiss} disabled={pending}
                className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50">Cancel</button>
              <button type="button" onClick={() => void confirmClose()} disabled={pending}
                className="rounded-sm bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800 disabled:opacity-50">
                {pending ? 'Closing...' : 'Confirm closure'}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
