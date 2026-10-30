import { useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import axios from 'axios'
import { getStudy, type StudyStatus } from '../../services/studyApi'
import { createSessionExport, createStudyExport, downloadStudyExport, getStudyExport, type ExportDataType, type ExportJob } from '../../services/exportApi'

export type SessionExportTarget = { sessionId: string; participantId: string }
type Props = { studyId: string; studyTitle: string; status: StudyStatus; open: boolean; onClose: () => void; session?: SessionExportTarget; initialDataTypes?: ExportDataType[] }
const categories: { value: ExportDataType; label: string; description: string }[] = [
  { value: 'QUESTIONNAIRE', label: 'Questionnaire', description: 'Saved answers and skipped questions.' },
  { value: 'GAZE', label: 'Eye tracking', description: 'Saved gaze events, batches and calibration attempts.' },
  { value: 'BEHAVIOR', label: 'Mouse & keyboard interactions', description: 'Saved clicks, key presses, mouse movement, scrolling and page visibility events.' },
]
const labels: Record<ExportJob['status'], string> = { PENDING: 'Queued', RUNNING: 'Generating', COMPLETED: 'Ready to download', FAILED: 'Export failed', EXPIRED: 'Export expired' }
const buttonClass = 'rounded-sm border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40'
const primaryClass = 'rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-40'

export default function StudyExportDialog({ studyId, studyTitle, status, open, onClose, session, initialDataTypes }: Props) {
  const headingId = useId()
  const closeButton = useRef<HTMLButtonElement>(null)
  const busy = useRef(false)
  const mounted = useRef(true)
  const [selected, setSelected] = useState<ExportDataType[]>(() => initialDataTypes ?? ['QUESTIONNAIRE', 'GAZE', 'BEHAVIOR'])
  const [job, setJob] = useState<ExportJob | null>(null)
  const [pending, setPending] = useState<'create' | 'download' | null>(null)
  const [error, setError] = useState('')
  const [pollError, setPollError] = useState('')
  const [retry, setRetry] = useState(0)
  const [downloadNotice, setDownloadNotice] = useState('')
  const running = job?.status === 'PENDING' || job?.status === 'RUNNING'
  const jobId = job?.id
  const available = status === 'COLLECTING' || status === 'CLOSED'

  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  useEffect(() => {
    if (!open) return
    const previous = document.activeElement as HTMLElement | null
    closeButton.current?.focus()
    const oldOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = oldOverflow; previous?.focus() }
  }, [open])

  useEffect(() => {
    if (!open || !jobId || !running) return
    let active = true
    const controller = new AbortController()
    let timer: ReturnType<typeof setTimeout>
    const poll = async () => {
      try {
        const updated = await getStudyExport(studyId, jobId, controller.signal)
        if (!active) return
        setPollError('')
        setJob(updated)
        if (updated.status === 'PENDING' || updated.status === 'RUNNING') timer = setTimeout(() => void poll(), 2000)
      } catch (failure) {
        if (active && !axios.isCancel(failure)) setPollError('Unable to check export status. Your task may still be running. Retry the status check before creating another export.')
      }
    }
    void poll()
    return () => { active = false; controller.abort(); clearTimeout(timer) }
  }, [open, studyId, jobId, running, retry])

  async function generate() {
    if (busy.current || !available || !selected.length || running) return
    busy.current = true
    setPending('create'); setError(''); setDownloadNotice('')
    try {
      const current = await getStudy(studyId)
      if (!mounted.current) return
      if (current.status !== 'COLLECTING' && current.status !== 'CLOSED') {
        setError('Publish this study before exporting data.'); return
      }
      const created = session
        ? await createSessionExport(studyId, session.sessionId, selected)
        : await createStudyExport(studyId, selected)
      if (mounted.current) { setJob(created); setPollError('') }
    } catch {
      if (mounted.current) setError('Unable to confirm export creation. A request may have reached the server; retrying may create another task. Check your connection and account access.')
    } finally {
      busy.current = false
      if (mounted.current) setPending(null)
    }
  }

  async function download() {
    if (busy.current || !job || job.status !== 'COMPLETED') return
    busy.current = true; setPending('download'); setError(''); setDownloadNotice('')
    try {
      if (session) await downloadStudyExport(studyId, job.id, 'SESSION')
      else await downloadStudyExport(studyId, job.id)
      if (mounted.current) setDownloadNotice('Download requested. Check your browser downloads.')
    } catch (failure) {
      if (!mounted.current) return
      if (axios.isAxiosError(failure) && failure.response?.status === 410) {
        setJob({ ...job, status: 'EXPIRED' })
        setError('This export has expired or its file is unavailable. Generate a new export.')
      } else if (axios.isAxiosError(failure) && failure.response?.status === 409) {
        setError('The export is not ready. Refresh its status and try again.')
      } else setError('Download failed. Check your connection and account access, then try again.')
    } finally {
      busy.current = false
      if (mounted.current) setPending(null)
    }
  }

  if (!open) return null
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#101b2b]/55 p-4" onClick={event => event.stopPropagation()}>
      <section role="dialog" aria-modal="true" aria-labelledby={headingId} className="max-h-[calc(100dvh-2rem)] w-full max-w-xl overflow-y-auto rounded-md border border-gray-200 bg-white p-6 text-left text-[#172033] shadow-xl"
        onKeyDown={event => {
          event.stopPropagation()
          if (event.key === 'Escape' && !busy.current) onClose()
          if (event.key === 'Tab') {
            const controls = [...event.currentTarget.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled)')]
            const first = controls[0]; const last = controls.at(-1)
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
            if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
          }
        }}>
        <p className="text-xs font-semibold uppercase text-emerald-700">Study data</p>
        <h2 id={headingId} className="mt-1 text-xl font-semibold">Export data</h2>
        <p className="mt-3 break-words font-semibold">{studyTitle}</p>
        {session ? <div className="mt-2 space-y-1 break-all text-sm text-gray-600">
          <p className="font-semibold">Scope: this participant session only.</p>
          <p>Participant ID: {session.participantId}</p>
          <p>Session ID: {session.sessionId}</p>
        </div> : <p className="mt-1 text-sm text-gray-600">Scope: all participant sessions in this study, including in-progress, completed and abandoned sessions.</p>}
        <fieldset disabled={!!pending || running} className="mt-5 space-y-3">
          <legend className="mb-3 text-sm font-semibold">Select data to export</legend>
          {categories.map(category => <label key={category.value} className="flex cursor-pointer items-start gap-3 rounded-sm border border-gray-200 p-3 text-sm has-[:checked]:border-emerald-600 has-[:checked]:bg-emerald-50">
            <input type="checkbox" className="mt-1 accent-emerald-700" checked={selected.includes(category.value)} onChange={event => setSelected(values => event.target.checked ? [...values, category.value] : values.filter(value => value !== category.value))} />
            <span><strong>{category.label}</strong><span className="mt-1 block text-xs leading-5 text-gray-600">{category.description}</span></span>
          </label>)}
        </fieldset>
        <div className="mt-4 space-y-2 text-xs leading-5 text-gray-600">
          <p>Only data saved at the generation snapshot is included. New data collected later requires a new export.</p>
          <p>Files without saved records may be omitted. See {session ? 'README.md' : 'README.md and data_availability.csv'} in the ZIP for availability and explanations. No camera images or video are included.</p>
        </div>
        {job && <div className="mt-4 rounded-sm border border-sky-200 bg-sky-50 p-3 text-sm" role="status" aria-live="polite">
          <strong>{labels[job.status]}</strong>
          <p className="mt-1 text-xs">Categories: {job.dataTypes.join(', ')}</p>
          {job.snapshotAt && <p className="mt-1 text-xs">Snapshot: {new Date(job.snapshotAt).toLocaleString()}</p>}
          {job.expiresAt && <p className="mt-1 text-xs">Download expires: {new Date(job.expiresAt).toLocaleString()}</p>}
          {job.status === 'FAILED' && <p className="mt-2">{job.errorMessage || 'The export could not be generated.'}{job.errorCode ? ` (${job.errorCode})` : ''}{job.failedSessionId ? ` Session: ${job.failedSessionId}` : ''}</p>}
          {job.status === 'EXPIRED' && <p className="mt-2">Generate a new export to download the data.</p>}
        </div>}
        {pollError && <p role="alert" className="mt-3 text-sm text-red-700">{pollError}</p>}
        {error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
        {downloadNotice && <p role="status" className="mt-3 text-sm text-emerald-700">{downloadNotice}</p>}
        {running && <p className="mt-3 text-xs text-gray-600">You may close this dialog while the server generates the file. Reopen this entry to check the task while this page remains open.</p>}
        <div className="mt-6 flex flex-wrap justify-end gap-3">
          <button ref={closeButton} type="button" className={buttonClass} disabled={!!pending} onClick={onClose}>Close</button>
          {pollError && <button type="button" className={buttonClass} onClick={() => { setPollError(''); setRetry(value => value + 1) }}>Retry status check</button>}
          {job?.status === 'COMPLETED' && <button type="button" className={primaryClass} disabled={!!pending} onClick={() => void download()}>{pending === 'download' ? 'Downloading...' : 'Download ZIP'}</button>}
          <button type="button" className={primaryClass} disabled={!available || !selected.length || !!pending || running} onClick={() => void generate()}>{pending === 'create' ? 'Creating export...' : job ? 'Generate new export' : 'Generate export'}</button>
        </div>
      </section>
    </div>, document.body,
  )
}
