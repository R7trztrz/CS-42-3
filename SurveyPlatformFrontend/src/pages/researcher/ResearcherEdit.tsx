import { useEffect, useRef, useState } from 'react'

import {
  Editor,
  Element,
  Frame,
  useEditor,
  useNode,
} from '@craftjs/core'

import {
  useBlocker,
  useParams,
  useSearchParams,
} from 'react-router-dom'

import Toolbox from '../../components/editor/Toolbox'
import PropertiesPanel from '../../components/editor/PropertiesPanel'
import ResearcherHeader from '../../components/researcher/ResearcherHeader'
import UnsavedChangesDialogTemplate from '../../components/studies/UnsavedChangesDialogTemplate'

import FacebookTemplate from '../../components/templates/facebook/FacebookTemplate'
import FacebookTopBar from '../../components/templates/facebook/FacebookTopBar'
import FacebookStories from '../../components/templates/facebook/FacebookStories'
import FacebookCreatePost from '../../components/templates/facebook/FacebookCreatePost'

import {
  FacebookActionButtonsRow,
  FacebookBodySection,
  FacebookComposerCard,
  FacebookFooterSection,
  FacebookHeaderLeft,
  FacebookHeaderRow,
  FacebookImageSection,
  FacebookMetaColumn,
  FacebookPostCard,
} from '../../components/templates/facebook/FacebookLayout'

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


type EditorTemplate =
  | PlatformStyle
  | 'blank'


const templateNames: Record<
  EditorTemplate,
  string
> = {
  blank: 'Blank canvas',
  facebook: 'Facebook',
  instagram: 'Instagram',
  tiktok: 'TikTok',
  x: 'X',
  threads: 'Threads',
  bluesky: 'Bluesky',
  'truth-social': 'Truth Social',
}


function isEditorTemplate(
  value: string | null,
): value is EditorTemplate {
  return (
    value !== null &&
    value in templateNames
  )
}


type SaveStatus =
  | 'loading'
  | 'clean'
  | 'saved'
  | 'unsaved'
  | 'saving'
  | 'error'


const saveStatusDetails: Record<
  SaveStatus,
  {
    label: string
    className: string
  }
> = {
  loading: {
    label: 'Loading...',
    className:
      'border-gray-200 bg-gray-50 text-gray-600',
  },

  clean: {
    label: 'No unsaved changes',
    className:
      'border-gray-200 bg-gray-50 text-gray-600',
  },

  saved: {
    label: 'Saved locally',
    className:
      'border-emerald-200 bg-emerald-50 text-emerald-800',
  },

  unsaved: {
    label: 'Unsaved changes',
    className:
      'border-amber-200 bg-amber-50 text-amber-800',
  },

  saving: {
    label: 'Saving...',
    className:
      'border-blue-200 bg-blue-50 text-blue-800',
  },

  error: {
    label: 'Save failed',
    className:
      'border-red-200 bg-red-50 text-red-700',
  },
}


/*
 * Shared editable canvas.
 */
function ResearcherEditCanvas({
  children,
}: {
  children?: React.ReactNode
}) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="min-h-[36rem] rounded-md border border-dashed border-[#b8c6d4] bg-[#f0f2f5] p-6"
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

  onReady: (
    editorState: string,
    restoredFromStorage: boolean,
  ) => void
}


/*
 * Restore an existing saved Craft.js state.
 */
function RestoreStudyInterfaceTemplate({
  studyId,
  onReady,
}: RestoreStudyInterfaceTemplateProps) {
  const {
    actions,
    query,
  } = useEditor()

  const hasRestored =
    useRef(false)

  useEffect(() => {
    if (hasRestored.current) {
      return
    }

    hasRestored.current = true

    const savedInterface =
      studyId
        ? loadStudyInterfaceTemplate(
            studyId,
          )
        : null

    let restoredFromStorage =
      false

    if (savedInterface) {
      try {
        actions.deserialize(
          savedInterface.editorState,
        )

        restoredFromStorage =
          true
      } catch (error) {
        console.error(
          'Failed to restore saved interface:',
          error,
        )

        restoredFromStorage =
          false
      }
    }

    onReady(
      query.serialize(),
      restoredFromStorage,
    )
  }, [
    actions,
    onReady,
    query,
    studyId,
  ])

  return null
}


function ResearcherEdit() {
  const { studyId } =
    useParams<{
      studyId: string
    }>()

  const [searchParams] =
    useSearchParams()

  const requestedTemplate =
    searchParams.get('template')

  const template =
    isEditorTemplate(
      requestedTemplate,
    )
      ? requestedTemplate
      : 'blank'

  const platformStyle:
    PlatformStyle =
      template === 'blank'
        ? 'facebook'
        : template


  const lastSavedStateRef =
    useRef('')

  const currentStateRef =
    useRef('')

  const isEditorReadyRef =
    useRef(false)

  const hasPersistedStateRef =
    useRef(false)


  const [
    hasUnsavedChanges,
    setHasUnsavedChanges,
  ] = useState(false)


  const [
    saveStatus,
    setSaveStatus,
  ] = useState<SaveStatus>(
    'loading',
  )


  const blocker =
    useBlocker(
      hasUnsavedChanges,
    )


  const handleEditorReady = (
    editorState: string,
    restoredFromStorage: boolean,
  ) => {
    currentStateRef.current =
      editorState

    lastSavedStateRef.current =
      editorState

    hasPersistedStateRef.current =
      restoredFromStorage

    isEditorReadyRef.current =
      true

    setHasUnsavedChanges(false)

    setSaveStatus(
      restoredFromStorage
        ? 'saved'
        : 'clean',
    )
  }


  const handleNodesChange = (
    editorState: string,
  ) => {
    currentStateRef.current =
      editorState

    if (
      !isEditorReadyRef.current
    ) {
      return
    }

    const isDirty =
      editorState !==
      lastSavedStateRef.current

    setHasUnsavedChanges(
      isDirty,
    )

    setSaveStatus(
      isDirty
        ? 'unsaved'
        : hasPersistedStateRef.current
          ? 'saved'
          : 'clean',
    )
  }


  const saveCurrentInterface =
    async () => {
      if (
        !studyId ||
        !currentStateRef.current
      ) {
        return false
      }

      setSaveStatus('saving')

      try {
        await Promise.resolve()

        saveStudyInterfaceTemplate(
          studyId,
          template,
          currentStateRef.current,
        )

        lastSavedStateRef.current =
          currentStateRef.current

        hasPersistedStateRef.current =
          true

        setHasUnsavedChanges(false)

        setSaveStatus('saved')

        return true
      } catch (error) {
        console.error(
          'Failed to save interface:',
          error,
        )

        setSaveStatus('error')

        return false
      }
    }


  useEffect(() => {
    if (
      !hasUnsavedChanges
    ) {
      return
    }

    const handleBeforeUnload = (
      event: BeforeUnloadEvent,
    ) => {
      event.preventDefault()

      event.returnValue = ''
    }

    window.addEventListener(
      'beforeunload',
      handleBeforeUnload,
    )

    return () =>
      window.removeEventListener(
        'beforeunload',
        handleBeforeUnload,
      )
  }, [hasUnsavedChanges])


  const handleSaveAndLeave =
    async () => {
      const didSave =
        await saveCurrentInterface()

      if (
        didSave &&
        blocker.state ===
          'blocked'
      ) {
        blocker.proceed()
      }
    }


  return (
    <div className="flex min-h-screen flex-col bg-[#f3f6f8] text-gray-950">

      <ResearcherHeader />


      <Editor
        onNodesChange={(query) =>
          handleNodesChange(
            query.serialize(),
          )
        }
        resolver={{
          /*
           * Shared widgets
           */
          TextWidget,
          AvatarWidget,
          ImageWidget,
          ActionBarWidget,

          /*
           * Shared canvas
           */
          ResearcherEditCanvas,

          /*
           * Facebook template
           */
          FacebookTemplate,

          /*
           * Facebook fixed shell
           */
          FacebookTopBar,
          FacebookStories,
          FacebookCreatePost,

          /*
           * Facebook Craft layout nodes
           */
          FacebookComposerCard,
          FacebookPostCard,
          FacebookHeaderRow,
          FacebookHeaderLeft,
          FacebookMetaColumn,
          FacebookBodySection,
          FacebookImageSection,
          FacebookFooterSection,
          FacebookActionButtonsRow,
        }}
      >

        <RestoreStudyInterfaceTemplate
          studyId={studyId}
          onReady={
            handleEditorReady
          }
        />


        {/* Page header */}
        <section className="border-b border-gray-300 bg-white px-6 py-6 md:px-10">

          <div className="mx-auto flex w-full max-w-[96rem] flex-col justify-between gap-4 sm:flex-row sm:items-end">

            <div>
              <p className="text-xs font-semibold uppercase text-emerald-700">
                Interface Editor
              </p>

              <h2 className="mt-1 text-2xl font-semibold text-[#172033]">
                Edit study interface
              </h2>

              {studyId && (
                <p className="mt-2 font-mono text-xs text-gray-400">
                  Study ID:{' '}
                  {studyId}
                </p>
              )}
            </div>


            <div className="rounded-md border border-sky-200 bg-sky-50 px-4 py-2.5">

              <p className="text-xs font-medium text-emerald-700">
                Current template
              </p>

              <p className="mt-0.5 text-sm font-semibold text-sky-950">
                {
                  templateNames[
                    template
                  ]
                }
              </p>

            </div>

          </div>

        </section>


        {/* Editor layout */}
        <div className="grid flex-1 grid-cols-1 md:grid-cols-[15rem_minmax(0,1fr)] xl:grid-cols-[15rem_minmax(0,1fr)_19rem]">

          {/* Toolbox */}
          <aside className="border-b border-[#cbd6e2] bg-[#edf3f8] md:border-b-0 md:border-r">

            <Toolbox
              styleId={
                platformStyle
              }
            />

          </aside>


          {/* Canvas */}
          <main className="min-h-96 border-b border-[#cbd6e2] bg-white p-6 md:border-b-0 xl:border-r xl:p-8">

            <div className="mb-5 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">

              <div>
                <p className="text-xs font-semibold uppercase text-[#52667d]">
                  Canvas
                </p>

                <h3 className="mt-1 text-lg font-semibold text-[#172033]">
                  {
                    templateNames[
                      template
                    ]
                  }{' '}
                  interface
                </h3>
              </div>


              <div className="flex flex-wrap items-center gap-3">

                <span
                  className={`rounded-sm border px-3 py-2 text-xs font-medium ${saveStatusDetails[saveStatus].className}`}
                >
                  {
                    saveStatusDetails[
                      saveStatus
                    ].label
                  }
                </span>


                <span className="rounded-sm border border-amber-300 bg-amber-50 px-3 py-2 text-xs font-semibold text-amber-800">
                  Draft
                </span>


                <button
                  type="button"
                  onClick={() =>
                    void saveCurrentInterface()
                  }
                  disabled={
                    saveStatus ===
                      'saving' ||
                    saveStatus ===
                      'loading'
                  }
                  className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {
                    saveStatus ===
                    'saving'
                      ? 'Saving...'
                      : 'Save'
                  }
                </button>

              </div>

            </div>


            {/* Blank canvas */}
            {template ===
            'blank' ? (

              <Frame
                key={`${studyId ?? 'new'}-${template}`}
              >
                <Element
                  is={
                    ResearcherEditCanvas
                  }
                  canvas
                />
              </Frame>


            ) : template ===
              'facebook' ? (

              /*
               * Facebook
               */
              <Frame
                key={`${studyId ?? 'new'}-${template}`}
              >
                <FacebookTemplate
                  canvasComponent={
                    ResearcherEditCanvas
                  }
                />
              </Frame>


            ) : (

              /*
               * Other platform templates
               */
              <Frame
                key={`${studyId ?? 'new'}-${template}`}
              >
                <Element
                  is={
                    ResearcherEditCanvas
                  }
                  canvas
                >

                  <TextWidget
                    text={`Start building your ${templateNames[template]} research interface`}
                    styleId={
                      platformStyle
                    }
                  />

                </Element>
              </Frame>

            )}

          </main>


          {/* Properties */}
          <aside className="border-t border-[#cbd6e2] bg-[#f8fafc] md:col-start-2 xl:col-start-auto xl:border-t-0">

            <PropertiesPanel />

          </aside>

        </div>

      </Editor>


      {/* Unsaved changes dialog */}
      {blocker.state ===
        'blocked' && (

        <UnsavedChangesDialogTemplate
          isSaving={
            saveStatus ===
            'saving'
          }

          onStay={() =>
            blocker.reset()
          }

          onLeave={() =>
            blocker.proceed()
          }

          onSaveAndLeave={() =>
            void handleSaveAndLeave()
          }
        />

      )}

    </div>
  )
}


export default ResearcherEdit