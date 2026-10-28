import type { StudyStatus } from '../../services/studyApi'

type Props = {
  status: StudyStatus
  menuItem?: boolean
  className?: string
  onActivate?: () => void
}

export default function StudyDataExportButton({ status, menuItem = false, className, onActivate }: Props) {
  const available = status === 'COLLECTING' || status === 'CLOSED'
  return (
    <button
      type="button"
      role={menuItem ? 'menuitem' : undefined}
      disabled={!available}
      title={available ? 'Data export is coming soon' : 'Publish this study before exporting data'}
      className={className ?? 'rounded-sm border border-emerald-700 bg-white px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:border-gray-300 disabled:text-gray-400 disabled:hover:bg-white'}
      onClick={() => {
        if (!available) return
        onActivate?.()
        // Reserved entry point: replace this notice when the export contract is available.
        window.alert('Data export is coming soon. Planned exports include participant test data, questionnaire responses, eye-tracking data, and mouse and keyboard interaction records. No file has been generated.')
      }}
    >
      Export data
    </button>
  )
}
