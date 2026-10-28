import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'
import ResearcherHeader from '../../components/researcher/ResearcherHeader'
import { getStudy, getStudyParticipation, type StudyResponse, type StudyParticipationResponse, type StudyParticipantStatus, type ParticipationSort } from '../../services/studyApi'
import './ParticipationOverviewDemo.css'

const labels: Record<StudyParticipantStatus, string> = { IN_PROGRESS: 'In progress', COMPLETED: 'Completed', ABANDONED: 'Abandoned' }
const dateFormat = new Intl.DateTimeFormat('en-AU', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'Australia/Sydney' })
const formatDate = (value: string) => dateFormat.format(new Date(value))

export default function StudyParticipationPage() {
  const { studyId = '' } = useParams()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [sortBy, setSortBy] = useState<ParticipationSort>('enteredAt')
  const [direction, setDirection] = useState<'asc' | 'desc'>('desc')
  const [refresh, setRefresh] = useState(0)
  const [result, setResult] = useState<{ studyId: string; study: StudyResponse; data: StudyParticipationResponse | null; updatedAt: string } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const study = result?.studyId === studyId ? result.study : null
  const data = result?.studyId === studyId ? result.data : null

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    setLoading(true)
    setError('')
    setNotice('')
    // Check the authoritative lifecycle before requesting participant records.
    void (async () => {
      try {
        const currentStudy = await getStudy(studyId)
        if (!active) return
        if (currentStudy.status === 'DRAFT') {
          setResult({ studyId, study: currentStudy, data: null, updatedAt: '' })
          return
        }
        const response = await getStudyParticipation(studyId, { page, size, sortBy, direction }, controller.signal)
        if (!active) return
        if (page > 0 && page >= response.sessions.totalPages) {
          setPage(Math.max(0, response.sessions.totalPages - 1))
          return
        }
        setResult({ studyId, study: currentStudy, data: response, updatedAt: new Date().toISOString() })
      } catch (err) {
        if (!active || axios.isCancel(err)) return
        setResult(null)
        const status = axios.isAxiosError(err) ? err.response?.status : undefined
        setError(status === 404 ? 'This study could not be found or is not available to your account.'
          : status === 401 || status === 403 ? 'You do not have permission to view this study. Please sign in with the study owner account.'
          : 'Unable to load participation records. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    })()
    return () => { active = false; controller.abort() }
  }, [studyId, page, size, sortBy, direction, refresh])

  async function copyId(id: string) {
    try { await navigator.clipboard.writeText(id); setNotice('Participant ID copied.') }
    catch { setNotice('Unable to copy. The full ID is available in the participant field.') }
  }

  return (
    <div className="participation-overview-demo">
      <ResearcherHeader activePage="studies" />
      <main className="pod-main">
        <div className="pod-breadcrumb"><Link to="/studies">Studies</Link><span aria-hidden="true">/</span><span>Participation overview</span></div>
        <section className="pod-heading">
          <div><p className="pod-eyebrow">Study participation</p><h2>Participation overview</h2>
            {study && <><p className="pod-study-name">{study.title}</p>{study.description && <p className="pod-study-name">{study.description}</p>}
              <div className="pod-study-meta"><span>{study.id}</span><span className={`pod-study-status pod-study-status--${study.status.toLowerCase()}`}>{study.status === 'COLLECTING' ? 'Collecting' : study.status === 'CLOSED' ? 'Closed' : 'Draft'}</span></div></>}
          </div>
        </section>
        {loading && <p role="status" className="py-6">Loading participation records...</p>}
        {error && <div role="alert" className="py-6 text-red-700"><p>{error}</p><button className="mt-3 rounded-sm border px-4 py-2" onClick={() => setRefresh(value => value + 1)}>Try again</button></div>}
        {!loading && !error && study?.status === 'DRAFT' && <p className="py-6">Participation overview is available only for collecting or closed studies. Publish this study before viewing participation records.</p>}
        {!loading && !error && data && <>
          <section className="pod-metrics" aria-label="Participation summary">
            {[
              { label: 'Total sessions', value: data.summary.total, tone: 'total' },
              { label: 'In progress', value: data.summary.inProgress, tone: 'progress' },
              { label: 'Completed', value: data.summary.completed, tone: 'completed' },
              { label: 'Abandoned', value: data.summary.abandoned, tone: 'abandoned' },
            ].map(metric => <div key={metric.tone} className={`pod-metric pod-metric--${metric.tone}`}><span>{metric.label}</span><strong>{metric.value}</strong><small>Participant sessions</small></div>)}
          </section>
          <section className="pod-records" aria-labelledby="pod-records-title">
            <div className="pod-section-heading"><div><h3 id="pod-records-title">Participant sessions</h3><p>{data.sessions.totalElements} records · Session counts include declined consent and are not unique people.</p></div>
              <div className="pod-refresh"><span>Updated <time dateTime={result!.updatedAt}>{formatDate(result!.updatedAt)}</time></span><button className="pod-icon-button" aria-label="Refresh records" title="Refresh records" onClick={() => setRefresh(value => value + 1)}>↻</button></div>
            </div>
            <div className="pod-toolbar">
              <div className="pod-sort"><label htmlFor="participation-sort">Sort by</label><select id="participation-sort" value={sortBy} onChange={event => { setSortBy(event.target.value as ParticipationSort); setPage(0) }}><option value="enteredAt">Entered at</option><option value="status">Status</option></select>
                <button className="pod-icon-button" aria-label={direction === 'desc' ? 'Sort ascending' : 'Sort descending'} onClick={() => { setDirection(value => value === 'desc' ? 'asc' : 'desc'); setPage(0) }}>{direction === 'desc' ? '↓' : '↑'}</button><span className="pod-order">{direction === 'desc' ? 'Descending' : 'Ascending'}</span>
              </div>
              <div className="pod-page-size"><label htmlFor="participation-size">Rows per page</label><select id="participation-size" value={size} onChange={event => { setSize(Number(event.target.value)); setPage(0) }}>{[10, 20, 50].map(value => <option key={value} value={value}>{value}</option>)}</select></div>
            </div>
            <div className="pod-table-scroll" role="region" aria-label="Participant records" tabIndex={0}>
              <table><thead><tr><th>Participant ID</th><th>Status</th><th>Entered at</th><th>Completed at</th></tr></thead>
                <tbody>{data.sessions.content.map(session => <tr key={session.sessionId}>
                  <td><div className="pod-participant"><div><span className="pod-participant-id" title={session.anonymousParticipantId}>{session.anonymousParticipantId.slice(0, 8)}<span aria-hidden="true">...</span><span className="sr-only">{session.anonymousParticipantId.slice(8)}</span></span><small>{session.sessionId}</small></div><button className="pod-copy" aria-label={`Copy participant ID ${session.anonymousParticipantId}`} onClick={() => void copyId(session.anonymousParticipantId)}>⧉</button></div></td>
                  <td><span className={`pod-session-status pod-session-status--${session.status.toLowerCase()}`}><i aria-hidden="true" />{labels[session.status]}</span></td>
                  <td><time dateTime={session.enteredAt}>{formatDate(session.enteredAt)}</time></td><td>{session.completedAt ? <time dateTime={session.completedAt}>{formatDate(session.completedAt)}</time> : <span aria-label="Not completed">—</span>}</td>
                </tr>)}{data.sessions.content.length === 0 && <tr><td colSpan={4}>No participant sessions yet.</td></tr>}</tbody>
              </table>
            </div>
            <div className="pod-pagination"><p>Showing <strong>{data.sessions.totalElements === 0 ? 0 : data.sessions.page * data.sessions.size + 1}-{Math.min((data.sessions.page + 1) * data.sessions.size, data.sessions.totalElements)}</strong> of <strong>{data.sessions.totalElements}</strong></p><div>
              <button className="pod-icon-button" aria-label="Previous page" disabled={data.sessions.page === 0} onClick={() => setPage(value => value - 1)}>←</button>
              <span>Page <strong>{data.sessions.totalPages === 0 ? 0 : data.sessions.page + 1}</strong> of {data.sessions.totalPages}</span>
              <button className="pod-icon-button" aria-label="Next page" disabled={data.sessions.page + 1 >= data.sessions.totalPages} onClick={() => setPage(value => value + 1)}>→</button>
            </div></div>
          </section>
        </>}
        <p role="status" aria-live="polite" className="pod-notice">{notice}</p>
      </main>
    </div>
  )
}
