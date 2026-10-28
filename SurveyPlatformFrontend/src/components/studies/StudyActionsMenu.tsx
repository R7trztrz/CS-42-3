import { useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import type { StudyResponse, StudySummaryResponse } from '../../services/studyApi'
import StudyActionDialog from './StudyActionDialog'
import StudyDataExportButton from './StudyDataExportButton'
import StudyExportDialog from './StudyExportDialog'

type Props = {
  study: StudySummaryResponse
  open: boolean
  onOpenChange: (open: boolean) => void
  onChanged: (study: StudyResponse) => void
  onDeleted: (id: string) => void
}

export default function StudyActionsMenu({ study, open, onOpenChange, onChanged, onDeleted }: Props) {
  const navigate = useNavigate()
  const id = useId()
  const trigger = useRef<HTMLButtonElement>(null)
  const menu = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState({ top: 0, left: 0 })
  const [action, setAction] = useState<'close' | 'delete' | null>(null)
  const [exportOpen, setExportOpen] = useState(false)

  function showMenu() {
    const rect = trigger.current?.getBoundingClientRect()
    if (rect) setPosition({ top: Math.max(8, Math.min(rect.bottom + 6, window.innerHeight - 244)), left: Math.max(8, Math.min(rect.right - 208, window.innerWidth - 216)) })
    onOpenChange(true)
  }

  useEffect(() => {
    if (!open) return
    menu.current?.querySelector<HTMLButtonElement>('button:not(:disabled)')?.focus()
    function outside(event: PointerEvent) {
      if (!menu.current?.contains(event.target as Node) && !trigger.current?.contains(event.target as Node)) onOpenChange(false)
    }
    const dismiss = () => onOpenChange(false)
    document.addEventListener('pointerdown', outside)
    window.addEventListener('resize', dismiss)
    window.addEventListener('scroll', dismiss, true)
    return () => {
      document.removeEventListener('pointerdown', outside)
      window.removeEventListener('resize', dismiss)
      window.removeEventListener('scroll', dismiss, true)
    }
  }, [open, onOpenChange])

  const itemClass = 'block w-full px-4 py-2.5 text-left text-sm font-medium hover:bg-emerald-50 focus:bg-emerald-50 focus:outline-none disabled:cursor-not-allowed disabled:text-gray-400 disabled:hover:bg-white'
  return (
    <div className="inline-flex" onClick={event => event.stopPropagation()} onKeyDown={event => event.stopPropagation()}>
      <button ref={trigger} type="button" aria-label={`Actions for ${study.title}`} aria-haspopup="menu" aria-expanded={open} aria-controls={open ? id : undefined}
        title="Study actions" onClick={() => open ? onOpenChange(false) : showMenu()}
        onKeyDown={event => { if (event.key === 'ArrowDown') { event.preventDefault(); showMenu() } }}
        className="flex h-9 w-9 items-center justify-center rounded-sm border border-gray-300 bg-white text-xl font-bold leading-none text-[#52667d] hover:border-emerald-700 hover:text-emerald-700 focus-visible:outline-2 focus-visible:outline-emerald-600">
        <span aria-hidden="true">&#8943;</span>
      </button>
      {open && createPortal(
        <div ref={menu} id={id} role="menu" aria-label={`Study actions for ${study.title}`} style={position}
          className="fixed z-40 w-52 overflow-hidden rounded-sm border border-gray-200 bg-white py-1 text-[#172033] shadow-lg"
          onClick={event => event.stopPropagation()} onKeyDown={event => {
            event.stopPropagation()
            if (event.key === 'Escape') { onOpenChange(false); trigger.current?.focus() }
            if (event.key === 'Tab') { onOpenChange(false); trigger.current?.focus() }
            if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
              event.preventDefault()
              const items = [...event.currentTarget.querySelectorAll<HTMLButtonElement>('button:not(:disabled)')]
              const index = items.indexOf(document.activeElement as HTMLButtonElement)
              const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : (index + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length
              items[next]?.focus()
            }
          }}>
          <button role="menuitem" type="button" disabled={study.status === 'CLOSED'} className={itemClass} onClick={() => { onOpenChange(false); navigate(`/studies/${study.id}/edit`) }}>Open study</button>
          <button role="menuitem" type="button" disabled={study.status === 'DRAFT'} className={itemClass} onClick={() => { onOpenChange(false); navigate(`/studies/${study.id}/participation`) }}>Participation overview</button>
          <StudyDataExportButton studyId={study.id} studyTitle={study.title} status={study.status} menuItem className={itemClass} onActivate={() => { onOpenChange(false); setExportOpen(true) }} />
          <button role="menuitem" type="button" disabled={study.status !== 'COLLECTING'} className={itemClass} onClick={() => { onOpenChange(false); setAction('close') }}>Close study</button>
          <div role="separator" className="my-1 border-t border-gray-200" />
          <button role="menuitem" type="button" className={`${itemClass} text-red-700 hover:bg-red-50 focus:bg-red-50`} onClick={() => { onOpenChange(false); setAction('delete') }}>Delete study</button>
        </div>, document.body,
      )}
      {action && <StudyActionDialog studyId={study.id} action={action} onChanged={onChanged} onDeleted={onDeleted} onCancel={() => { setAction(null); trigger.current?.focus() }} />}
      <StudyExportDialog key={study.id} studyId={study.id} studyTitle={study.title} status={study.status} open={exportOpen} onClose={() => { setExportOpen(false); trigger.current?.focus() }} />
    </div>
  )
}
