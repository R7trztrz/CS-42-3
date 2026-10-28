import { useMemo, useState } from 'react'
import './ParticipationOverviewDemo.css'

type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'
type Session = { id: string; participantId: string; status: SessionStatus; enteredAt: string; completedAt: string | null }
type SortField = 'enteredAt' | 'status'
const statusLabels: Record<SessionStatus, string> = { IN_PROGRESS: 'In progress', COMPLETED: 'Completed', ABANDONED: 'Abandoned' }
const statusOrder: Record<SessionStatus, number> = { IN_PROGRESS: 0, COMPLETED: 1, ABANDONED: 2 }
const dateFormat = new Intl.DateTimeFormat('en-AU', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'Australia/Sydney' })

const sampleSessions: Session[] = Array.from({ length: 36 }, (_, index) => {
  const entered = Date.parse('2026-10-27T03:30:00Z') - index * 2_280_000
  const status: SessionStatus = index % 9 === 0 ? 'ABANDONED' : index % 9 < 3 ? 'IN_PROGRESS' : 'COMPLETED'
  return {
    id: `session-${String(index + 1).padStart(4, '0')}`,
    participantId: `${(0x7c4a1000 + index).toString(16)}-23b8-4ef0-9d61-${String(104800 + index).padStart(12, '0')}`,
    status,
    enteredAt: new Date(entered).toISOString(),
    completedAt: status === 'COMPLETED' ? new Date(entered + (8 + index % 7) * 60_000).toISOString() : null,
  }
})

export default function ParticipationOverviewDemo() {
  const [studyStatus, setStudyStatus] = useState<'COLLECTING' | 'CLOSED'>('COLLECTING')
  const [sortBy, setSortBy] = useState<SortField>('enteredAt')
  const [descending, setDescending] = useState(true)
  const [size, setSize] = useState(10)
  const [page, setPage] = useState(0)
  const [notice, setNotice] = useState('')
  const [updatedAt, setUpdatedAt] = useState('2026-10-27T03:30:00Z')
  const sessions = useMemo(() => sampleSessions.map(session => studyStatus === 'CLOSED' && session.status === 'IN_PROGRESS'
    ? { ...session, status: 'ABANDONED' as const } : session), [studyStatus])
  const counts = sessions.reduce((totals, session) => { totals[session.status]++; return totals }, { IN_PROGRESS: 0, COMPLETED: 0, ABANDONED: 0 })
  const sorted = [...sessions].sort((a, b) => {
    const primary = sortBy === 'status' ? statusOrder[a.status] - statusOrder[b.status] : Date.parse(a.enteredAt) - Date.parse(b.enteredAt)
    return (descending ? -primary : primary) || Date.parse(b.enteredAt) - Date.parse(a.enteredAt) || a.id.localeCompare(b.id)
  })
  const pages = Math.ceil(sorted.length / size)
  const visible = sorted.slice(page * size, (page + 1) * size)

  async function copyId(id: string) {
    try { await navigator.clipboard.writeText(id); setNotice('Participant ID copied.') }
    catch { setNotice('Unable to copy. The full ID is available in the participant field.') }
  }

  return (
    <div className="participation-overview-demo">
      <header className="pod-header">
        <div className="pod-brand-row"><div className="pod-brand"><span className="pod-brand-mark">SP</span><div><p>Researcher workspace</p><h1>Survey Platform</h1></div></div><span className="pod-sample">Sample study</span></div>
        <nav aria-label="Researcher sections"><div><span>Dashboard</span><span aria-current="page">Study List</span><span>Question Bank</span></div></nav>
      </header>
      <main className="pod-main">
        <div className="pod-breadcrumb"><span>Studies</span><span aria-hidden="true">/</span><span>Participation overview</span></div>
        <section className="pod-heading">
          <div><p className="pod-eyebrow">Study participation</p><h2>Participation overview</h2><p className="pod-study-name">Social media browsing experience</p><div className="pod-study-meta"><span>ST-2026-014</span><span className={`pod-study-status pod-study-status--${studyStatus.toLowerCase()}`}>{studyStatus === 'COLLECTING' ? 'Collecting' : 'Closed'}</span></div></div>
          <fieldset className="pod-study-switch"><legend className="sr-only">Study status</legend>{(['COLLECTING', 'CLOSED'] as const).map(status => <label key={status}><input type="radio" name="study-status" value={status} checked={studyStatus === status} onChange={() => { setStudyStatus(status); setPage(0); setNotice('') }} /><span>{status === 'COLLECTING' ? 'Collecting' : 'Closed'}</span></label>)}</fieldset>
        </section>
        <section className="pod-metrics" aria-label="Participation summary">
          {[
            { label: 'Total sessions', value: sessions.length, tone: 'total' },
            { label: 'In progress', value: counts.IN_PROGRESS, tone: 'progress' },
            { label: 'Completed', value: counts.COMPLETED, tone: 'completed' },
            { label: 'Abandoned', value: counts.ABANDONED, tone: 'abandoned' },
          ].map(metric => <div key={metric.tone} className={`pod-metric pod-metric--${metric.tone}`}><span>{metric.label}</span><strong>{metric.value}</strong><small>Participant sessions</small></div>)}
        </section>
        <section className="pod-records" aria-labelledby="pod-records-title">
          <div className="pod-section-heading"><div><h3 id="pod-records-title">Participant sessions</h3><p>{sessions.length} records</p></div><div className="pod-refresh"><span>Updated <time dateTime={updatedAt}>{dateFormat.format(new Date(updatedAt))}</time></span><button type="button" className="pod-icon-button" title="Refresh records" aria-label="Refresh records" onClick={() => { setUpdatedAt(new Date().toISOString()); setNotice('Records refreshed.'); }}><span aria-hidden="true">&#8635;</span></button></div></div>
          <div className="pod-toolbar"><div className="pod-sort"><label htmlFor="pod-sort">Sort by</label><select id="pod-sort" value={sortBy} onChange={event => { setSortBy(event.target.value as SortField); setPage(0) }}><option value="enteredAt">Entered at</option><option value="status">Status</option></select><button className="pod-icon-button" type="button" aria-label={descending ? 'Sort ascending' : 'Sort descending'} title={descending ? 'Sort ascending' : 'Sort descending'} onClick={() => { setDescending(value => !value); setPage(0) }}><span aria-hidden="true">{descending ? '\u2193' : '\u2191'}</span></button><span className="pod-order">{descending ? 'Descending' : 'Ascending'}</span></div><div className="pod-page-size"><label htmlFor="pod-size">Rows per page</label><select id="pod-size" value={size} onChange={event => { setSize(Number(event.target.value)); setPage(0) }}>{[10, 20, 50].map(value => <option key={value} value={value}>{value}</option>)}</select></div></div>
          <div className="pod-table-scroll" role="region" aria-label="Participant records" tabIndex={0}><table><thead><tr><th>Participant ID</th><th>Status</th><th>Entered at</th><th>Completed at</th></tr></thead><tbody>{visible.map(session => <tr key={session.id}><td><div className="pod-participant"><div><span className="pod-participant-id" title={session.participantId}>{session.participantId.slice(0, 8)}<span aria-hidden="true">...</span><span className="sr-only">{session.participantId.slice(8)}</span></span><small>{session.id}</small></div><button className="pod-copy" type="button" title={`Copy participant ID ${session.participantId}`} aria-label={`Copy participant ID ${session.participantId}`} onClick={() => void copyId(session.participantId)}><span aria-hidden="true">&#10697;</span></button></div></td><td><span className={`pod-session-status pod-session-status--${session.status.toLowerCase()}`}><i aria-hidden="true" />{statusLabels[session.status]}</span></td><td><time dateTime={session.enteredAt}>{dateFormat.format(new Date(session.enteredAt))}</time></td><td>{session.completedAt ? <time dateTime={session.completedAt}>{dateFormat.format(new Date(session.completedAt))}</time> : <span className="pod-missing" aria-label="Not completed">-</span>}</td></tr>)}</tbody></table></div>
          <div className="pod-pagination"><p>Showing <strong>{page * size + 1}-{Math.min((page + 1) * size, sessions.length)}</strong> of <strong>{sessions.length}</strong></p><div><button className="pod-icon-button" type="button" aria-label="Previous page" title="Previous page" disabled={page === 0} onClick={() => setPage(value => value - 1)}>&larr;</button><span>Page <strong>{page + 1}</strong> of {pages}</span><button className="pod-icon-button" type="button" aria-label="Next page" title="Next page" disabled={page + 1 === pages} onClick={() => setPage(value => value + 1)}>&rarr;</button></div></div>
        </section>
        <p role="status" aria-live="polite" className="pod-notice">{notice}</p>
      </main>
    </div>
  )
}
