import { useEffect, useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'
import ResearcherHeader from '../../components/researcher/ResearcherHeader'
import StudyDataExportButton from '../../components/studies/StudyDataExportButton'
import { getStudy, getStudyParticipant, type StudyParticipantDetail, type StudyResponse } from '../../services/studyApi'
import type { ExportDataType } from '../../services/exportApi'
import './ParticipationOverviewDemo.css'
import './ParticipantSessionDetails.css'

const statuses = { IN_PROGRESS: 'In progress', COMPLETED: 'Completed', ABANDONED: 'Abandoned' }
const phases = { CONSENT: 'Informed consent', CALIBRATION: 'Eye-tracking setup', BROWSING: 'Social media activity', QUESTIONNAIRE: 'Final questionnaire', FINISHED: 'Finished' }
const calibrationStates = { DISABLED: 'Disabled', NOT_STARTED: 'Not started', IN_PROGRESS: 'In progress', RECORDED: 'Final attempt recorded' }
const reasons: Record<string, string> = { CONSENT_DECLINED: 'Consent declined', PARTICIPANT_EXIT: 'Participant exit', INACTIVITY_TIMEOUT: 'Inactivity timeout', STUDY_CLOSED: 'Study closed', NO_CAMERA_API: 'Camera API unavailable', NO_CAMERA_DEVICE: 'No camera device', PERMISSION_DENIED: 'Camera permission denied', INIT_FAILED: 'Tracker initialisation failed', INIT_CANCELLED: 'Tracker initialisation cancelled', INIT_TIMEOUT: 'Tracker initialisation timed out', ABANDONED: 'Calibration abandoned', NOT_MEASURED: 'Not measured', INSUFFICIENT_SAMPLES: 'Insufficient samples', HIGH_RESIDUAL: 'High residual error', VIEWPORT_CHANGED: 'Viewport changed', ENGINE_UNAVAILABLE: 'Tracker unavailable' }
const readable = (value: string | null) => value ? reasons[value] ?? value.toLowerCase().replaceAll('_', ' ') : '—'
const dateFormat = new Intl.DateTimeFormat('en-AU', { dateStyle: 'medium', timeStyle: 'medium', timeZone: 'Australia/Sydney', hour12: false })
function timestamp(value: string | null) {
  return value && Number.isFinite(Date.parse(value)) ? <time dateTime={value}>{dateFormat.format(new Date(value))}</time> : '—'
}
function Fields({ items }: { items: [string, ReactNode][] }) {
  return <dl className="session-detail-fields">{items.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value ?? '—'}</dd></div>)}</dl>
}
const exports: { category: ExportDataType; title: string; description: string; label: string }[] = [
  { category: 'QUESTIONNAIRE', title: 'Questionnaire', description: 'Saved answers and skipped question records.', label: 'Export questionnaire' },
  { category: 'GAZE', title: 'Eye tracking', description: 'Saved gaze events, batches and calibration attempts.', label: 'Export eye tracking' },
  { category: 'BEHAVIOR', title: 'Mouse & keyboard interactions', description: 'Saved clicks, key presses, movement, scrolling and visibility events.', label: 'Export interactions' },
]

export default function ParticipantSessionDetails() {
  const { studyId = '', sessionId = '' } = useParams()
  const requestKey = `${studyId}:${sessionId}`
  const [result, setResult] = useState<{ key: string; study: StudyResponse; participant: StudyParticipantDetail; updatedAt: string } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [draft, setDraft] = useState(false)
  const [refresh, setRefresh] = useState(0)
  const current = result?.key === requestKey ? result : null

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    setLoading(true); setError(''); setDraft(false); setResult(null)
    void (async () => {
      try {
        const study = await getStudy(studyId)
        if (!active) return
        if (study.status !== 'COLLECTING' && study.status !== 'CLOSED') { setDraft(true); return }
        const participant = await getStudyParticipant(studyId, sessionId, controller.signal)
        if (!active) return
        if (participant.studyId !== studyId || participant.sessionId !== sessionId) throw new Error('Session identity mismatch')
        setResult({ key: requestKey, study, participant, updatedAt: new Date().toISOString() })
      } catch (failure) {
        if (!active || axios.isCancel(failure)) return
        const status = axios.isAxiosError(failure) ? failure.response?.status : undefined
        setError(status === 404 ? 'This study or participant session could not be found, or is not available to your account.'
          : status === 401 || status === 403 ? 'You do not have permission to view this session. Please sign in with the study owner account.'
          : 'Unable to load session details. Please try again.')
      } finally { if (active) setLoading(false) }
    })()
    return () => { active = false; controller.abort() }
  }, [studyId, sessionId, requestKey, refresh])

  const participant = current?.participant
  const eye = participant?.eyeTracking
  return <div className="participation-overview-demo">
    <ResearcherHeader activePage="studies" />
    <main className="pod-main session-detail-main">
      <nav className="pod-breadcrumb" aria-label="Breadcrumb"><Link to="/studies">Studies</Link><span aria-hidden="true">/</span><Link to={`/studies/${studyId}/participation`}>Participation overview</Link><span aria-hidden="true">/</span><span>Session details</span></nav>
      <div className="pod-heading"><div><p className="pod-eyebrow">Participant session</p><h2>Session details</h2>{current && <p className="pod-study-name">{current.study.title} <span className={`pod-study-status pod-study-status--${current.study.status.toLowerCase()}`}>{current.study.status === 'CLOSED' ? 'Closed' : 'Collecting'}</span></p>}</div>
        <div className="session-detail-actions"><Link to={`/studies/${studyId}/participation`} className="session-detail-button">Back to participants</Link><button type="button" className="session-detail-button" disabled={loading} onClick={() => setRefresh(value => value + 1)}>Refresh details</button></div>
      </div>
      {loading && <p role="status" className="py-6">Loading session details...</p>}
      {!loading && draft && <p className="py-6">Session details are available only for collecting or closed studies.</p>}
      {error && <div role="alert" className="py-6 text-red-700"><p>{error}</p><button type="button" className="session-detail-button mt-3" onClick={() => setRefresh(value => value + 1)}>Try again</button></div>}
      {!loading && !error && current && participant && <>
        <p className="session-detail-updated">Updated {timestamp(current.updatedAt)} · All displayed times are Australia/Sydney.</p>
        <section className="session-detail-section" aria-labelledby="session-overview"><h3 id="session-overview">Session overview</h3>
          <Fields items={[
            ['Participant ID', participant.anonymousParticipantId], ['Session ID', participant.sessionId], ['Study ID', participant.studyId],
            ['Status', <span className={`pod-session-status pod-session-status--${participant.status.toLowerCase()}`}><i aria-hidden="true" />{statuses[participant.status]}</span>],
            ['Current phase', phases[participant.phase]], ['Abandonment reason', readable(participant.abandonmentReason)],
          ]} />
        </section>
        <div className="session-detail-grid">
          <section className="session-detail-section" aria-labelledby="session-times"><h3 id="session-times">Participation timeline</h3><p className="session-detail-help">A dash means the corresponding time has not been recorded.</p>
            <Fields items={[
              ['Entered study', timestamp(participant.enteredAt)], ['Consent given', timestamp(participant.consentedAt)],
              ['Calibration completed', timestamp(participant.calibrationCompletedAt)], ['Browsing completed', timestamp(participant.browsingCompletedAt)],
              ['Session completed', timestamp(participant.completedAt)], ['Session abandoned', timestamp(participant.abandonedAt)], ['Last activity', timestamp(participant.lastActivityAt)],
            ]} />
          </section>
          <section className="session-detail-section" aria-labelledby="session-device"><h3 id="session-device">Device information</h3>
            {participant.deviceInfo ? <Fields items={[
              ['Browser', participant.deviceInfo.browser], ['Browser version', participant.deviceInfo.browserVersion], ['Operating system', participant.deviceInfo.os],
              ['Screen width', participant.deviceInfo.screenWidth === null ? '—' : `${participant.deviceInfo.screenWidth} px`],
              ['Screen height', participant.deviceInfo.screenHeight === null ? '—' : `${participant.deviceInfo.screenHeight} px`], ['Device time zone', participant.deviceInfo.timezone],
            ]} /> : <p className="session-detail-help">No device information was recorded for this session.</p>}
          </section>
        </div>
        <section className="session-detail-section" aria-labelledby="session-eye"><h3 id="session-eye">Eye-tracking summary</h3>
          {eye && <>
            {!eye.enabled && <p className="session-detail-help">Eye tracking is disabled for this study.</p>}
            {eye.enabled && eye.calibrationState === 'NOT_STARTED' && <p className="session-detail-help">No calibration attempt has started.</p>}
            <Fields items={[
              ['Eye tracking enabled', eye.enabled ? 'Yes' : 'No'], ['Calibration state', calibrationStates[eye.calibrationState]],
              ['Calibration attempts', eye.calibrationAttemptCount], ['Final attempt ID', eye.completedAttemptId],
              ['Recorded tracker availability', eye.trackingAvailable === null ? 'Not recorded' : eye.trackingAvailable ? 'Available' : 'Unavailable'],
              ['Unavailable reason', readable(eye.unavailableReason)], ['Calibration quality', readable(eye.quality)], ['Quality basis', readable(eye.qualityBasis)],
              ['Quality notes', eye.lowQualityReasons.length ? eye.lowQualityReasons.map(reason => readable(reason)).join('; ') : '—'],
            ]} />
          </>}
          <p className="session-detail-help">Calibration completion and tracker availability do not prove that gaze events were saved. Download the eye-tracking export to inspect saved records. This page does not display raw gaze trajectories.</p>
        </section>
        <section className="session-detail-section" aria-labelledby="session-downloads"><h3 id="session-downloads">Session data downloads</h3>
          <p className="session-detail-help">Each export below is limited to this session. Only saved records are included; missing files are explained in the ZIP README. Data availability and event counts are not supplied by this details endpoint.</p>
          <div className="session-detail-exports">{exports.map(item => <article key={item.category}><h4>{item.title}</h4><p>{item.description}</p><StudyDataExportButton key={`${requestKey}:${item.category}`} studyId={studyId} studyTitle={current.study.title} status={current.study.status} session={{ sessionId, participantId: participant.anonymousParticipantId }} initialDataTypes={[item.category]} buttonLabel={item.label} /></article>)}</div>
        </section>
      </>}
    </main>
  </div>
}
