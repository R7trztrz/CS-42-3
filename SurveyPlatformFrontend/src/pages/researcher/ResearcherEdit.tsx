import { useEffect, useRef, useState } from 'react'
import { Editor, Element, Frame, useEditor, useNode } from '@craftjs/core'
import axios from 'axios'
import { useBlocker, useParams, useSearchParams } from 'react-router-dom'
import CanvasContainer from '../../components/editor/CanvasContainer'
import Toolbox from '../../components/editor/Toolbox'
import PropertiesPanel from '../../components/editor/PropertiesPanel'
import ResearcherHeader from '../../components/researcher/ResearcherHeader'
import PublishStudyDialog from '../../components/studies/PublishStudyDialog'
import UnsavedChangesDialogTemplate from '../../components/studies/UnsavedChangesDialogTemplate'
import {
  ActionBarWidget,
  AvatarWidget,
  ImageWidget,
  TextWidget,
} from '../../components/widgets'
import type { PlatformStyle } from '../../components/widgets/types'
import {
  getStudy,
  getStudyFeed,
  publishStudy,
  saveStudyFeed,
  type StudyFeedResponse,
  type StudyResponse,
  type StudyStatus,
} from '../../services/studyApi'
import {
  loadStudyInterfaceTemplate,
  saveStudyInterfaceTemplate,
} from '../../services/studyInterfaceStorageTemplate'

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

const statusStyles: Record<StudyStatus, string> = {
  DRAFT: 'border-amber-300 bg-amber-50 text-amber-800',
  COLLECTING: 'border-emerald-300 bg-emerald-50 text-emerald-800',
  CLOSED: 'border-sky-300 bg-sky-50 text-sky-800',
}

function isEditorTemplate(value: string | null | undefined): value is EditorTemplate {
  return value !== null && value !== undefined && value in templateNames
}

type SaveStatus = 'loading' | 'clean' | 'saved' | 'unsaved' | 'saving' | 'error'

const saveStatusDetails: Record<SaveStatus, { label: string; className: string }> = {
  loading: { label: 'Loading...', className: 'border-gray-200 bg-gray-50 text-gray-600' },
  clean: { label: 'No unsaved changes', className: 'border-gray-200 bg-gray-50 text-gray-600' },
  saved: { label: 'Saved', className: 'border-emerald-200 bg-emerald-50 text-emerald-800' },
  unsaved: { label: 'Unsaved changes', className: 'border-amber-200 bg-amber-50 text-amber-800' },
  saving: { label: 'Saving...', className: 'border-blue-200 bg-blue-50 text-blue-800' },
  error: { label: 'Save failed', className: 'border-red-200 bg-red-50 text-red-700' },
}

function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ error?: string; code?: string; message?: string }>(error)) {
    if (!error.response) return 'Unable to reach the server. Check that the backend is running.'

    const code = error.response.data?.code
    if (code === 'FEED_NOT_READY') return 'Save at least one interface component before publishing.'
    if (code === 'STUDY_VERSION_CONFLICT' || code === 'FEED_VERSION_CONFLICT') {
      return 'This study changed in another session. Reload the page and try again.'
    }
    if (code === 'STUDY_NOT_PUBLISHABLE' || code === 'STUDY_NOT_EDITABLE') {
      return 'This study is no longer editable or publishable.'
    }
    if (error.response.status === 401) return 'Your session has expired. Please log in again.'

    return error.response.data?.error ?? error.response.data?.message ?? fallback
  }

  return fallback
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
      className="min-h-[32rem] rounded-md border border-dashed border-[#b8c6d4] bg-[#f8fafc] p-6"
    >
      {children}
    </div>
  )
}

ResearcherEditCanvas.craft = {
  displayName: 'Researcher Edit Canvas',
}

type RestoreStudyInterfaceProps = {
  studyId: string | undefined
  backendContent: Record<string, unknown> | null
  onReady: (editorState: string, restoredFromPersistence: boolean) => void
}

function RestoreStudyInterface({
  studyId,
  backendContent,
  onReady,
}: RestoreStudyInterfaceProps) {
  const { actions, query } = useEditor()
  const hasRestored = useRef(false)

  useEffect(() => {
    if (hasRestored.current) return
    hasRestored.current = true

    let restoredFromPersistence = false

    if (backendContent) {
      try {
        actions.deserialize(JSON.stringify(backendContent))
        restoredFromPersistence = true
      } catch {
        restoredFromPersistence = false
      }
    } else {
      const savedInterface = studyId ? loadStudyInterfaceTemplate(studyId) : null
      if (savedInterface) {
        try {
          actions.deserialize(savedInterface.editorState)
          restoredFromPersistence = true
        } catch {
          restoredFromPersistence = false
        }
      }
    }

    onReady(query.serialize(), restoredFromPersistence)
  }, [actions, backendContent, onReady, query, studyId])

  return null
}

function ReadOnlyPanel() {
  return (
    <div className="p-5">
      <p className="text-xs font-semibold uppercase text-emerald-700">Published</p>
      <h2 className="mt-1 text-lg font-semibold text-[#172033]">Editing locked</h2>
      <p className="mt-3 text-sm leading-6 text-[#52667d]">
        This interface is read-only while the study is collecting responses.
      </p>
    </div>
  )
}

function ResearcherEdit() {
  const { studyId } = useParams<{ studyId: string }>()
  const [searchParams] = useSearchParams()
  const requestedTemplate = searchParams.get('template')
  const lastSavedStateRef = useRef('')
  const currentStateRef = useRef('')
  const feedVersionRef = useRef(0)
  const hasBackendFeedRef = useRef(false)
  const isEditorReadyRef = useRef(false)
  const hasPersistedStateRef = useRef(false)
  const [study, setStudy] = useState<StudyResponse | null>(null)
  const [feed, setFeed] = useState<StudyFeedResponse | null>(null)
  const [isLoadingStudy, setIsLoadingStudy] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [reloadVersion, setReloadVersion] = useState(0)
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const [saveStatus, setSaveStatus] = useState<SaveStatus>('loading')
  const [saveError, setSaveError] = useState('')
  const [isPublishDialogOpen, setIsPublishDialogOpen] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [publishError, setPublishError] = useState('')
  const [copiedLink, setCopiedLink] = useState(false)
  const isEditable = study?.status === 'DRAFT'
  const blocker = useBlocker(Boolean(isEditable && hasUnsavedChanges))

  useEffect(() => {
    let ignore = false

    const loadStudy = async () => {
      if (!studyId) {
        setLoadError('The study ID is missing.')
        setIsLoadingStudy(false)
        return
      }

      setIsLoadingStudy(true)
      setLoadError('')

      try {
        const [studyResponse, feedResponse] = await Promise.all([
          getStudy(studyId),
          getStudyFeed(studyId),
        ])

        if (!ignore) {
          feedVersionRef.current = feedResponse.version
          hasBackendFeedRef.current = feedResponse.content !== null
          setStudy(studyResponse)
          setFeed(feedResponse)
        }
      } catch (error) {
        if (!ignore) setLoadError(getApiErrorMessage(error, 'The study could not be loaded.'))
      } finally {
        if (!ignore) setIsLoadingStudy(false)
      }
    }

    void loadStudy()
    return () => {
      ignore = true
    }
  }, [reloadVersion, studyId])

  const backendTemplate = isEditorTemplate(feed?.templateCode) ? feed.templateCode : null
  const template = isEditorTemplate(requestedTemplate)
    ? requestedTemplate
    : backendTemplate ?? 'blank'
  const platformStyle: PlatformStyle = template === 'blank' ? 'facebook' : template

  const handleEditorReady = (
    editorState: string,
    restoredFromPersistence: boolean,
  ) => {
    currentStateRef.current = editorState
    lastSavedStateRef.current = editorState
    hasPersistedStateRef.current = restoredFromPersistence
    isEditorReadyRef.current = true
    setHasUnsavedChanges(false)
    setSaveStatus(restoredFromPersistence ? 'saved' : 'clean')
  }

  const handleNodesChange = (editorState: string) => {
    currentStateRef.current = editorState

    if (!isEditorReadyRef.current || !isEditable) return

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
    if (!studyId || !currentStateRef.current || !isEditable) return false

    setSaveStatus('saving')
    setSaveError('')

    try {
      const content = JSON.parse(currentStateRef.current) as Record<string, unknown>
      const response = await saveStudyFeed(studyId, content, feedVersionRef.current)

      feedVersionRef.current = response.version
      hasBackendFeedRef.current = true
      setFeed(response)
      saveStudyInterfaceTemplate(studyId, template, currentStateRef.current)
      lastSavedStateRef.current = currentStateRef.current
      hasPersistedStateRef.current = true
      setHasUnsavedChanges(false)
      setSaveStatus('saved')
      return true
    } catch (error) {
      setSaveError(getApiErrorMessage(error, 'The interface could not be saved.'))
      setSaveStatus('error')
      return false
    }
  }

  useEffect(() => {
    if (!isEditable || !hasUnsavedChanges) return

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges, isEditable])

  const handleSaveAndLeave = async () => {
    const didSave = await saveCurrentInterface()

    if (didSave && blocker.state === 'blocked') {
      blocker.proceed()
    }
  }

  const handlePublish = async () => {
    if (!studyId || !study || !isEditable) return

    setIsPublishing(true)
    setPublishError('')

    try {
      if (hasUnsavedChanges || !hasBackendFeedRef.current) {
        const didSave = await saveCurrentInterface()
        if (!didSave) {
          setPublishError('The interface must be saved before it can be published.')
          return
        }
      }

      const latestStudy = await getStudy(studyId)
      const publishedStudy = await publishStudy(studyId, latestStudy.version)
      setStudy(publishedStudy)
      setHasUnsavedChanges(false)
      setSaveStatus('saved')
      setIsPublishDialogOpen(false)
    } catch (error) {
      setPublishError(getApiErrorMessage(error, 'The study could not be published.'))
    } finally {
      setIsPublishing(false)
    }
  }

  const copyParticipationLink = async () => {
    if (!study?.participationUrl) return

    try {
      await navigator.clipboard.writeText(study.participationUrl)
      setCopiedLink(true)
      window.setTimeout(() => setCopiedLink(false), 2000)
    } catch {
      setCopiedLink(false)
    }
  }

  if (isLoadingStudy) {
    return (
      <div className="min-h-screen bg-[#f3f6f8] text-gray-950">
        <ResearcherHeader />
        <p className="px-5 py-20 text-center text-sm text-gray-600" role="status">Loading study...</p>
      </div>
    )
  }

  if (loadError || !study || !feed) {
    return (
      <div className="min-h-screen bg-[#f3f6f8] text-gray-950">
        <ResearcherHeader />
        <main className="mx-auto max-w-3xl px-5 py-16 text-center">
          <h2 className="text-2xl font-semibold text-[#172033]">Study unavailable</h2>
          <p className="mt-3 text-sm text-red-700" role="alert">{loadError || 'The study could not be loaded.'}</p>
          <button
            type="button"
            onClick={() => setReloadVersion((value) => value + 1)}
            className="mt-5 rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-800 hover:bg-gray-50"
          >
            Try again
          </button>
        </main>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-[#f3f6f8] text-gray-950">
      <ResearcherHeader />

      <Editor
        enabled={isEditable}
        onNodesChange={(query) => handleNodesChange(query.serialize())}
        resolver={{
          TextWidget,
          AvatarWidget,
          ImageWidget,
          ActionBarWidget,
          CanvasContainer,
          ResearcherEditCanvas,
        }}
      >
        <RestoreStudyInterface
          studyId={studyId}
          backendContent={feed.content}
          onReady={handleEditorReady}
        />
        <section className="border-b border-gray-300 bg-white px-6 py-6 md:px-10">
          <div className="mx-auto flex w-full max-w-[96rem] flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <p className="text-xs font-semibold uppercase text-emerald-700">Interface Editor</p>
              <h2 className="mt-1 text-2xl font-semibold text-[#172033]">{study.title}</h2>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <span className={`rounded-sm border px-3 py-2 text-xs font-semibold ${statusStyles[study.status]}`}>
                {study.status}
              </span>
              <div className="rounded-md border border-sky-200 bg-sky-50 px-4 py-2.5">
                <p className="text-xs font-medium text-emerald-700">Current template</p>
                <p className="mt-0.5 text-sm font-semibold text-sky-950">{templateNames[template]}</p>
              </div>
            </div>
          </div>
        </section>

        {study.participationUrl && (
          <section className="border-b border-emerald-200 bg-emerald-50 px-6 py-4 md:px-10">
            <div className="mx-auto flex w-full max-w-[96rem] flex-col justify-between gap-3 md:flex-row md:items-center">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase text-emerald-700">Participation link</p>
                <p className="mt-1 truncate text-sm text-emerald-950">{study.participationUrl}</p>
              </div>
              <div className="flex shrink-0 gap-2">
                <button
                  type="button"
                  onClick={() => void copyParticipationLink()}
                  className="rounded-sm border border-emerald-700 bg-white px-3 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-100"
                >
                  {copiedLink ? 'Copied' : 'Copy link'}
                </button>
                <a
                  href={study.participationUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="rounded-sm bg-emerald-700 px-3 py-2 text-sm font-semibold text-white hover:bg-emerald-800"
                >
                  Open study
                </a>
              </div>
            </div>
          </section>
        )}

        <div className="grid flex-1 grid-cols-1 md:grid-cols-[15rem_minmax(0,1fr)] xl:grid-cols-[15rem_minmax(0,1fr)_19rem]">
          <aside className="border-b border-[#cbd6e2] bg-[#edf3f8] md:border-b-0 md:border-r">
            {isEditable ? <Toolbox /> : <ReadOnlyPanel />}
          </aside>

          <main className="min-h-96 border-b border-[#cbd6e2] bg-white p-6 md:border-b-0 xl:border-r xl:p-8">
            <div className="mb-5 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
              <div>
                <p className="text-xs font-semibold uppercase text-[#52667d]">Canvas</p>
                <h3 className="mt-1 text-lg font-semibold text-[#172033]">{templateNames[template]} interface</h3>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                {isEditable && (
                  <span className={`rounded-sm border px-3 py-2 text-xs font-medium ${saveStatusDetails[saveStatus].className}`}>
                    {saveStatusDetails[saveStatus].label}
                  </span>
                )}
                {isEditable && (
                  <>
                    <button
                      type="button"
                      onClick={() => void saveCurrentInterface()}
                      disabled={saveStatus === 'saving' || saveStatus === 'loading' || isPublishing}
                      className="rounded-sm border border-emerald-700 bg-white px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {saveStatus === 'saving' ? 'Saving...' : 'Save'}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setPublishError('')
                        setIsPublishDialogOpen(true)
                      }}
                      disabled={saveStatus === 'saving' || saveStatus === 'loading'}
                      className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      Publish study
                    </button>
                  </>
                )}
              </div>
            </div>

            {saveError && isEditable && (
              <p className="mb-4 rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
                {saveError}
              </p>
            )}

            {template === 'blank' ? (
              <Frame key={`${studyId ?? 'new'}-${template}`}>
                <Element is={CanvasContainer} canvas />
              </Frame>
            ) : (
              <Frame key={`${studyId ?? 'new'}-${template}`}>
                <Element is={CanvasContainer} canvas>
                  <TextWidget
                    text={`Start building your ${templateNames[template]} research interface`}
                    styleId={platformStyle}
                  />
                </Element>
              </Frame>
            )}
          </main>

          <aside className="border-t border-[#cbd6e2] bg-[#f8fafc] md:col-start-2 xl:col-start-auto xl:border-t-0">
            {isEditable ? <PropertiesPanel /> : <ReadOnlyPanel />}
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

      {isPublishDialogOpen && (
        <PublishStudyDialog
          studyTitle={study.title}
          isPublishing={isPublishing}
          errorMessage={publishError}
          onCancel={() => {
            if (!isPublishing) setIsPublishDialogOpen(false)
          }}
          onConfirm={() => void handlePublish()}
        />
      )}
    </div>
  )
}

export default ResearcherEdit
