import { useEffect, useRef, useState } from 'react'
import { Editor, Element, Frame, useEditor, useNode } from '@craftjs/core'
import { Link, useBlocker, useParams, useSearchParams } from 'react-router-dom'
import Toolbox from '../../components/editor/Toolbox'
import PropertiesPanel from '../../components/editor/PropertiesPanel'
import UnsavedChangesDialogTemplate from '../../components/studies/UnsavedChangesDialogTemplate'
import {
  ActionBarWidget,
  AvatarWidget,
  ImageWidget,
  TextWidget,
} from '../../components/widgets'
import type { PlatformStyle } from '../../components/widgets/types'
import {
  loadStudyInterfaceTemplate,
  saveStudyInterfaceTemplate,
} from '../../services/studyInterfaceStorageTemplate'

const navigationItems = ['Dashboard', 'Interface List', 'Study List']

type EditorTemplate = PlatformStyle | 'blank'

const templateNames: Record<EditorTemplate, string> = {
  blank: 'Blank canvas',
  facebook: 'Facebook',
  instagram: 'Instagram',
  tiktok: 'TikTok',
  x: 'X',
  threads: 'Threads',
  bluesky: 'Bluesky',
  'truth-social': 'Truth Social',
}

function isEditorTemplate(value: string | null): value is EditorTemplate {
  return value !== null && value in templateNames
}

type SaveStatus = 'loading' | 'clean' | 'saved' | 'unsaved' | 'saving' | 'error'

const saveStatusDetails: Record<SaveStatus, { label: string; className: string }> = {
  loading: { label: 'Loading...', className: 'border-gray-200 bg-gray-50 text-gray-600' },
  clean: { label: 'No unsaved changes', className: 'border-gray-200 bg-gray-50 text-gray-600' },
  saved: { label: 'Saved locally', className: 'border-emerald-200 bg-emerald-50 text-emerald-800' },
  unsaved: { label: 'Unsaved changes', className: 'border-amber-200 bg-amber-50 text-amber-800' },
  saving: { label: 'Saving...', className: 'border-blue-200 bg-blue-50 text-blue-800' },
  error: { label: 'Save failed', className: 'border-red-200 bg-red-50 text-red-700' },
}

function ResearcherEditCanvas({ children }: { children?: React.ReactNode }) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="min-h-[32rem] border border-dashed border-gray-300 bg-gray-50 p-6"
    >
      {children}
    </div>
  )
}

ResearcherEditCanvas.craft = {
  displayName: 'Researcher Edit Canvas',
}

type RestoreStudyInterfaceTemplateProps = {
  studyId: string | undefined
  onReady: (editorState: string, restoredFromStorage: boolean) => void
}

function RestoreStudyInterfaceTemplate({
  studyId,
  onReady,
}: RestoreStudyInterfaceTemplateProps) {
  const { actions, query } = useEditor()
  const hasRestored = useRef(false)

  useEffect(() => {
    if (hasRestored.current) return
    hasRestored.current = true

    const savedInterface = studyId ? loadStudyInterfaceTemplate(studyId) : null
    let restoredFromStorage = false

    if (savedInterface) {
      try {
        actions.deserialize(savedInterface.editorState)
        restoredFromStorage = true
      } catch {
        restoredFromStorage = false
      }
    }

    onReady(query.serialize(), restoredFromStorage)
  }, [actions, onReady, query, studyId])

  return null
}

function ResearcherEdit() {
  const { studyId } = useParams<{ studyId: string }>()
  const [searchParams] = useSearchParams()
  const requestedTemplate = searchParams.get('template')
  const template = isEditorTemplate(requestedTemplate) ? requestedTemplate : 'blank'
  const platformStyle: PlatformStyle = template === 'blank' ? 'facebook' : template
  const lastSavedStateRef = useRef('')
  const currentStateRef = useRef('')
  const isEditorReadyRef = useRef(false)
  const hasPersistedStateRef = useRef(false)
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const [saveStatus, setSaveStatus] = useState<SaveStatus>('loading')
  const blocker = useBlocker(hasUnsavedChanges)

  const handleEditorReady = (
    editorState: string,
    restoredFromStorage: boolean,
  ) => {
    currentStateRef.current = editorState
    lastSavedStateRef.current = editorState
    hasPersistedStateRef.current = restoredFromStorage
    isEditorReadyRef.current = true
    setHasUnsavedChanges(false)
    setSaveStatus(restoredFromStorage ? 'saved' : 'clean')
  }

  const handleNodesChange = (editorState: string) => {
    currentStateRef.current = editorState

    if (!isEditorReadyRef.current) return

    const isDirty = editorState !== lastSavedStateRef.current
    setHasUnsavedChanges(isDirty)
    setSaveStatus(
      isDirty
        ? 'unsaved'
        : hasPersistedStateRef.current
          ? 'saved'
          : 'clean',
    )
  }

  const saveCurrentInterface = async () => {
    if (!studyId || !currentStateRef.current) return false

    setSaveStatus('saving')

    try {
      await Promise.resolve()
      saveStudyInterfaceTemplate(studyId, template, currentStateRef.current)
      lastSavedStateRef.current = currentStateRef.current
      hasPersistedStateRef.current = true
      setHasUnsavedChanges(false)
      setSaveStatus('saved')
      return true
    } catch {
      setSaveStatus('error')
      return false
    }
  }

  useEffect(() => {
    if (!hasUnsavedChanges) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const handleSaveAndLeave = async () => {
    const didSave = await saveCurrentInterface()

    if (didSave && blocker.state === 'blocked') {
      blocker.proceed()
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-100 text-gray-900">
      <header className="text-white">
        <div className="flex items-center justify-between gap-6 bg-gray-950 px-6 py-5 md:px-10">
          <div>
            <p className="text-sm font-medium text-emerald-400">
              Researcher workspace
            </p>
            <h1 className="mt-1 text-2xl font-semibold">Hello Researcher!</h1>
          </div>

          <div className="flex items-center gap-4 text-sm">
            <button type="button" className="text-gray-300 hover:text-white">
              Account
            </button>
            <button
              type="button"
              className="border border-gray-600 px-3 py-2 font-medium text-gray-200 hover:border-gray-400 hover:text-white"
            >
              Logout
            </button>
          </div>
        </div>

        <nav
          aria-label="Researcher pages"
          className="flex flex-wrap gap-7 border-y border-gray-700 bg-gray-800 px-6 pt-3 md:px-10"
        >
          {navigationItems.map((item) => {
            const className = 'border-b-2 border-transparent pb-3 text-sm font-medium text-gray-300'

            if (item === 'Dashboard' || item === 'Study List') {
              return (
                <Link
                  key={item}
                  to={item === 'Dashboard' ? '/researcher-dashboard' : '/studies'}
                  className={`${className} hover:text-white`}
                >
                  {item}
                </Link>
              )
            }

            return (
              <span key={item} className={className}>
                {item}
              </span>
            )
          })}
        </nav>
      </header>

      <Editor
        onNodesChange={(query) => handleNodesChange(query.serialize())}
        resolver={{
          TextWidget,
          AvatarWidget,
          ImageWidget,
          ActionBarWidget,
          ResearcherEditCanvas,
        }}
      >
        <RestoreStudyInterfaceTemplate
          studyId={studyId}
          onReady={handleEditorReady}
        />
        <section className="border-b border-gray-300 bg-white px-6 py-5 md:px-10">
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
            <div>
              <p className="text-xs font-semibold uppercase text-gray-500">
                Interface Editor
              </p>
              <h2 className="mt-1 text-xl font-semibold text-gray-950">
                Edit study interface
              </h2>
              {studyId && <p className="mt-1 text-xs text-gray-500">Study ID: {studyId}</p>}
            </div>

            <div className="border border-emerald-200 bg-emerald-50 px-4 py-2">
              <p className="text-xs font-medium text-emerald-700">Current template</p>
              <p className="mt-0.5 text-sm font-semibold text-emerald-950">
                {templateNames[template]}
              </p>
            </div>
          </div>
        </section>

        <div className="grid flex-1 grid-cols-1 md:grid-cols-[14rem_minmax(0,1fr)] xl:grid-cols-[14rem_minmax(0,1fr)_18rem]">
          <aside className="border-b border-gray-300 bg-gray-50 md:border-b-0 md:border-r">
            <Toolbox />
          </aside>

          <main className="min-h-96 border-b border-gray-300 bg-white p-6 md:border-b-0 xl:border-r xl:p-8">
            <div className="mb-5 flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase text-gray-500">Canvas</p>
                <h3 className="mt-1 text-lg font-semibold">{templateNames[template]} interface</h3>
              </div>
              <div className="flex items-center gap-3">
                <span
                  className={`border px-3 py-2 text-xs font-medium ${saveStatusDetails[saveStatus].className}`}
                >
                  {saveStatusDetails[saveStatus].label}
                </span>
                <span className="border border-gray-300 bg-gray-50 px-3 py-2 text-xs font-medium text-gray-600">
                  Draft
                </span>
                <button
                  type="button"
                  onClick={() => void saveCurrentInterface()}
                  disabled={saveStatus === 'saving' || saveStatus === 'loading'}
                  className="bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {saveStatus === 'saving' ? 'Saving...' : 'Save'}
                </button>
              </div>
            </div>

            {template === 'blank' ? (
              <Frame key={`${studyId ?? 'new'}-${template}`}>
                <Element is={ResearcherEditCanvas} canvas />
              </Frame>
            ) : (
              <Frame key={`${studyId ?? 'new'}-${template}`}>
                <Element is={ResearcherEditCanvas} canvas>
                  <TextWidget
                    text={`Start building your ${templateNames[template]} research interface`}
                    styleId={platformStyle}
                  />
                </Element>
              </Frame>
            )}
          </main>

          <aside className="border-t border-gray-300 bg-gray-50 md:col-start-2 xl:col-start-auto xl:border-t-0">
            <PropertiesPanel />
          </aside>
        </div>
      </Editor>

      {blocker.state === 'blocked' && (
        <UnsavedChangesDialogTemplate
          isSaving={saveStatus === 'saving'}
          onStay={() => blocker.reset()}
          onLeave={() => blocker.proceed()}
          onSaveAndLeave={() => void handleSaveAndLeave()}
        />
      )}
    </div>
  )
}

export default ResearcherEdit
