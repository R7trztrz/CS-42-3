import type { StudyStatus } from '../../services/studyApi'
import { useState } from 'react'
import StudyExportDialog, { type SessionExportTarget } from './StudyExportDialog'
import type { ExportDataType } from '../../services/exportApi'

type Props = {
  status: StudyStatus
  studyId: string
  studyTitle: string
  menuItem?: boolean
  className?: string
  onActivate?: () => void
  session?: SessionExportTarget
  initialDataTypes?: ExportDataType[]
  buttonLabel?: string
}

export default function StudyDataExportButton({ status, studyId, studyTitle, menuItem = false, className, onActivate, session, initialDataTypes, buttonLabel = 'Export data' }: Props) {
  const [open, setOpen] = useState(false)
  const available = status === 'COLLECTING' || status === 'CLOSED'
  return (
    <><button
      type="button"
      role={menuItem ? 'menuitem' : undefined}
      aria-label={session ? `${buttonLabel} for session ${session.sessionId}` : undefined}
      disabled={!available}
      title={available ? (session ? 'Export this session’s data' : 'Export study data') : 'Publish this study before exporting data'}
      className={className ?? 'rounded-sm border border-emerald-700 bg-white px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:border-gray-300 disabled:text-gray-400 disabled:hover:bg-white'}
      onClick={() => {
        if (!available) return
        if (onActivate) onActivate()
        else setOpen(true)
      }}
    >
      {buttonLabel}
    </button>
    {!onActivate && <StudyExportDialog key={`${studyId}:${session?.sessionId ?? 'study'}`} studyId={studyId} studyTitle={studyTitle} status={status} session={session} initialDataTypes={initialDataTypes} open={open} onClose={() => setOpen(false)} />}</>
  )
}
