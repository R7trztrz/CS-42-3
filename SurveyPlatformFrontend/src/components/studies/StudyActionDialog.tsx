import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { closeStudy, deleteStudy, getStudy, type StudyResponse } from '../../services/studyApi'
import { asApiError } from '../../shared/types/apiError'

type Props = {
  studyId: string
  action: 'close' | 'delete'
  onCancel: () => void
  onChanged: (study: StudyResponse) => void
  onDeleted: (id: string) => void
}

export default function StudyActionDialog({ studyId, action, onCancel, onChanged, onDeleted }: Props) {
  const [study, setStudy] = useState<StudyResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [title, setTitle] = useState('')
  const [retry, setRetry] = useState(0)
  const inFlight = useRef(false)
  const cancel = useRef<HTMLButtonElement>(null)
  const deleting = action === 'delete'

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    getStudy(studyId).then(result => {
      if (active) setStudy(result)
    }).catch(() => {
      if (active) setError('The study could not be loaded. Check that it is still available and try again.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [studyId, retry])

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    cancel.current?.focus()
    return () => { previous?.focus() }
  }, [])

  async function confirm() {
    if (!study || loading || inFlight.current || (deleting ? title !== study.title : study.status !== 'COLLECTING')) return
    inFlight.current = true
    setPending(true)
    setError('')
    try {
      if (deleting) {
        await deleteStudy(study.id, study.version)
        onDeleted(study.id)
      } else {
        onChanged(await closeStudy(study.id, study.version))
      }
      onCancel()
    } catch (failure) {
      const code = asApiError(failure)?.code
      if (code === 'STUDY_VERSION_CONFLICT' || code === 'STUDY_NOT_CLOSABLE') {
        setTitle('')
        setStudy(null)
        try {
          const current = await getStudy(studyId)
          setStudy(current)
          onChanged(current)
          setError('The study has changed. Review the updated details and confirm again.')
        } catch {
          setError('The study has changed and could not be refreshed. Cancel and reload the page.')
        }
      } else {
        setError(code === 'STUDY_NOT_FOUND'
          ? 'This study is no longer available. Cancel and refresh the list.'
          : `The study could not be ${deleting ? 'deleted' : 'closed'}. Please try again.`)
      }
    } finally {
      inFlight.current = false
      setPending(false)
    }
  }

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#101b2b]/55 p-4" onClick={event => event.stopPropagation()}>
      <section role="dialog" aria-modal="true" aria-labelledby="study-action-heading" aria-describedby="study-action-description"
        className="max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-md border border-gray-200 bg-white p-6 text-left shadow-xl"
        onKeyDown={event => {
          event.stopPropagation()
          if (event.key === 'Escape' && !inFlight.current) onCancel()
          if (event.key === 'Tab') {
            const elements = [...event.currentTarget.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled)')]
            const first = elements[0]
            const last = elements.at(-1)
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
            if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
          }
        }}>
        <p className="text-xs font-semibold uppercase text-red-700">{deleting ? 'Delete study' : 'Close study'}</p>
        <h2 id="study-action-heading" className="mt-1 text-xl font-semibold text-[#172033]">{deleting ? 'Permanently delete this study?' : 'Stop collecting responses?'}</h2>
        {loading && <p role="status" className="mt-4 text-sm text-gray-600">Loading study details...</p>}
        <div id="study-action-description" className="mt-4 space-y-3 text-sm leading-6 text-gray-600">
          {study && <p className="break-words font-semibold text-[#172033]">{study.title}</p>}
          <p>{deleting
            ? 'This permanently deletes the study, feed, questionnaire, participant sessions, answers and associated eye-tracking data. Study images will become inaccessible and their files will be cleaned up. Shared templates and question bank entries are retained.'
            : 'The participant link will stop working and active sessions will end. Already saved responses and study content will be retained.'} This cannot be undone.</p>
          {study && !deleting && study.status !== 'COLLECTING' && <p>This study is {study.status.toLowerCase()} and cannot be closed again.</p>}
        </div>
        {deleting && study && <label className="mt-5 block text-sm font-semibold text-gray-700">
          Type the study title to confirm
          <input value={title} onChange={event => setTitle(event.target.value)} disabled={pending} autoComplete="off"
            className="mt-2 w-full min-w-0 rounded-sm border border-gray-300 px-3 py-2 font-normal outline-none focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600" />
        </label>}
        {error && <p role="alert" className="mt-4 rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div className="mt-6 flex flex-wrap justify-end gap-3">
          <button ref={cancel} type="button" disabled={pending} onClick={onCancel} className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50">Cancel</button>
          {!study && !loading && <button type="button" disabled={pending} onClick={() => setRetry(value => value + 1)} className="rounded-sm border border-gray-300 px-4 py-2 text-sm">Reload details</button>}
          <button type="button" disabled={loading || pending || !study || (deleting ? title !== study.title : study.status !== 'COLLECTING')}
            onClick={() => void confirm()} className="rounded-sm bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800 disabled:cursor-not-allowed disabled:opacity-40">
            {pending ? (deleting ? 'Deleting...' : 'Closing...') : (deleting ? 'Delete permanently' : 'Confirm closure')}
          </button>
        </div>
      </section>
    </div>, document.body,
  )
}
