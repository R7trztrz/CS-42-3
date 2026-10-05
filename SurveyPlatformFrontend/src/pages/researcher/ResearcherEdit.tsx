import { useEffect, useRef, useState } from 'react'

import {

  Editor,

  Element,

  Frame,

  useEditor,

  useNode,

} from '@craftjs/core'

import axios from 'axios'

import {

  Link,

  useBlocker,

  useParams,

  useSearchParams,

} from 'react-router-dom'

import CanvasContainer from '../../components/editor/CanvasContainer'

import Toolbox from '../../components/editor/Toolbox'

import PropertiesPanel from '../../components/editor/PropertiesPanel'

import ResearcherHeader from '../../components/researcher/ResearcherHeader'

import PublishStudyDialog from '../../components/studies/PublishStudyDialog'

import UnsavedChangesDialogTemplate from '../../components/studies/UnsavedChangesDialogTemplate'

/*

 * Facebook

 */

import FacebookTemplate from '../../components/templates/facebook/FacebookTemplate'

import FacebookTopBar from '../../components/templates/facebook/FacebookTopBar'

import FacebookStories from '../../components/templates/facebook/FacebookStories'

import FacebookCreatePost from '../../components/templates/facebook/FacebookCreatePost'

import FacebookIcon from '../../components/templates/facebook/FacebookIcon'

import FacebookLeftSidebar from '../../components/templates/facebook/FacebookLeftSidebar'

import FacebookRightSidebar from '../../components/templates/facebook/FacebookRightSidebar'

import {

  FacebookActionButtonsRow,

  FacebookBodySection,

  FacebookDesktopShell,

  FacebookFooterSection,

  FacebookHeaderLeft,

  FacebookHeaderRow,

  FacebookImageSection,

  FacebookMainFeed,

  FacebookMetaColumn,

  FacebookPostCard,

} from '../../components/templates/facebook/FacebookLayout'

/*

 * Instagram

 */

import InstagramTemplate from '../../components/templates/instagram/InstagramTemplate'

import InstagramSidebar from '../../components/templates/instagram/InstagramSidebar'

import InstagramStories from '../../components/templates/instagram/InstagramStories'

import InstagramSuggestions from '../../components/templates/instagram/InstagramSuggestions'

import InstagramIcon from '../../components/templates/instagram/InstagramIcon'

import {

  InstagramActionRow,

  InstagramCaptionSection,

  InstagramDesktopShell,

  InstagramFooterSection,

  InstagramHeaderLeft,

  InstagramHeaderRow,

  InstagramImageSection,

  InstagramMainFeed,

  InstagramMetaColumn,

  InstagramPostCard,

  InstagramPostList,

} from '../../components/templates/instagram/InstagramLayout'

/*

 * TikTok

 */

import TikTokTemplate from '../../components/templates/tiktok/TikTokTemplate'

import TikTokIcon from '../../components/templates/tiktok/TikTokIcon'

import TikTokSidebar from '../../components/templates/tiktok/TikTokSidebar'

import {

  TikTokActionRail,

  TikTokDesktopShell,

  TikTokFeedStage,

  TikTokMainArea,

  TikTokPostStage,

  TikTokVideoCard,

} from '../../components/templates/tiktok/TikTokLayout'

/*

 * X

 */

import XTemplate from '../../components/templates/x/XTemplate'

import XIcon from '../../components/templates/x/XIcon'

import XSidebar from '../../components/templates/x/XSidebar'

import XRightSidebar from '../../components/templates/x/XRightSidebar'

import XComposer from '../../components/templates/x/XComposer'

import {

  XDesktopShell,

  XMainFeed,

  XPostList,

} from '../../components/templates/x/XLayout'

/*
 * Threads
 */
import ThreadsTemplate from '../../components/templates/threads/ThreadsTemplate'
import ThreadsIcon from '../../components/templates/threads/ThreadsIcon'
import ThreadsSidebar from '../../components/templates/threads/ThreadsSidebar'
import ThreadsComposer from '../../components/templates/threads/ThreadsComposer'

import {
  ThreadsDesktopShell,
  ThreadsMainFeed,
  ThreadsPostList,
} from '../../components/templates/threads/ThreadsLayout'

/*
 * Bluesky
 */
import BlueskyTemplate from '../../components/templates/bluesky/BlueskyTemplate'
import BlueskyIcon from '../../components/templates/bluesky/BlueskyIcon'
import BlueskySidebar from '../../components/templates/bluesky/BlueskySidebar'
import BlueskyComposer from '../../components/templates/bluesky/BlueskyComposer'
import BlueskyRightSidebar from '../../components/templates/bluesky/BlueskyRightSidebar'
import {
  BlueskyDesktopShell,
  BlueskyMainFeed,
  BlueskyPostList,
} from '../../components/templates/bluesky/BlueskyLayout'

/*
 * Truth Social
 */
import TruthSocialTemplate from '../../components/templates/truthsocial/TruthSocialTemplate'
import TruthSocialIcon from '../../components/templates/truthsocial/TruthSocialIcon'
import TruthSocialSidebar from '../../components/templates/truthsocial/TruthSocialSidebar'
import TruthSocialComposer from '../../components/templates/truthsocial/TruthSocialComposer'
import TruthSocialRightSidebar from '../../components/templates/truthsocial/TruthSocialRightSidebar'
import {
  TruthSocialDesktopShell,
  TruthSocialMainFeed,
  TruthSocialPostList,
} from '../../components/templates/truthsocial/TruthSocialLayout'
/*

 * Shared widgets

 */

import {

  ActionBarWidget,

  AvatarWidget,

  ImageWidget,

  TextWidget,

  PostWidget,

} from '../../components/widgets'

import type { PlatformStyle } from '../../components/widgets/types'

import { AssetImageProvider } from '../../components/widgets/AssetImageContext'

/*

 * Study API

 */

import {

  getStudy,

  getStudyFeed,

  publishStudy,

  saveStudyFeed,

  type StudyFeedResponse,

  type StudyResponse,

  type StudyStatus,

} from '../../services/studyApi'

/*

 * Local fallback

 */

import {

  loadStudyInterfaceTemplate,

  saveStudyInterfaceTemplate,

} from '../../services/studyInterfaceStorageTemplate'

type EditorTemplate =

  | PlatformStyle

  | 'blank'

type SaveStatus =

  | 'loading'

  | 'clean'

  | 'saved'

  | 'unsaved'

  | 'saving'

  | 'error'

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

const statusStyles: Record<

  StudyStatus,

  string

> = {

  DRAFT:

    'border-amber-300 bg-amber-50 text-amber-800',

  COLLECTING:

    'border-emerald-300 bg-emerald-50 text-emerald-800',

  CLOSED:

    'border-sky-300 bg-sky-50 text-sky-800',

}

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

    label: 'Saved',

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

function isEditorTemplate(

  value: string | null | undefined,

): value is EditorTemplate {

  return (

    value !== null &&

    value !== undefined &&

    value in templateNames

  )

}

function getApiErrorMessage(

  error: unknown,

  fallback: string,

) {

  if (

    axios.isAxiosError<{

      error?: string

      code?: string

      message?: string

    }>(error)

  ) {

    if (!error.response) {

      return 'Unable to reach the server. Check that the backend is running.'

    }

    const code =

      error.response.data?.code

    if (

      code ===

      'FEED_NOT_READY'

    ) {

      return 'Save at least one interface component before publishing.'

    }

    if (

      code ===

        'STUDY_VERSION_CONFLICT' ||

      code ===

        'FEED_VERSION_CONFLICT'

    ) {

      return 'This study changed in another session. Reload the page and try again.'

    }

    if (

      code ===

        'STUDY_NOT_PUBLISHABLE' ||

      code ===

        'STUDY_NOT_EDITABLE'

    ) {

      return 'This study is no longer editable or publishable.'

    }

    if (

      error.response.status ===

      401

    ) {

      return 'Your session has expired. Please log in again.'

    }

    return (

      error.response.data?.error ??

      error.response.data?.message ??

      fallback

    )

  }

  return fallback

}

/*

 * Researcher editing canvas

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

  displayName:

    'Researcher Edit Canvas',

}

/*

 * Temporary template JSON exporter.

 * Keep this while the seven platform templates are being finalised.

 * It can be removed after all template JSON files have been exported.

 */

function ExportTemplateJson({

  template,

}: {

  template: EditorTemplate

}) {

  const { query } = useEditor()

  const handleExport = () => {

    const serialized = query.serialize()

    let jsonContent = serialized

    try {

      jsonContent = JSON.stringify(

        JSON.parse(serialized),

        null,

        2,

      )

    } catch {

      jsonContent = serialized

    }

    const blob = new Blob(

      [jsonContent],

      {

        type: 'application/json',

      },

    )

    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')

    link.href = url

    link.download = `${template}-template.json`

    document.body.appendChild(link)

    link.click()

    document.body.removeChild(link)

    URL.revokeObjectURL(url)

  }

  return (

    <button

      type="button"

      onClick={handleExport}

      className="rounded-sm border border-violet-600 bg-white px-4 py-2 text-sm font-semibold text-violet-700 shadow-sm hover:bg-violet-50"

    >

      Export JSON

    </button>

  )

}

type RestoreStudyInterfaceProps = {

  studyId:

    | string

    | undefined

  backendContent:

    | Record<string, unknown>

    | null

  onReady: (

    editorState: string,

    restoredFromPersistence: boolean,

  ) => void

}

function RestoreStudyInterface({

  studyId,

  backendContent,

  onReady,

}: RestoreStudyInterfaceProps) {

  const {

    actions,

    query,

  } = useEditor()

  const hasRestored =

    useRef(false)

  useEffect(() => {

    if (

      hasRestored.current

    ) {

      return

    }

    hasRestored.current =

      true

    let restoredFromPersistence =

      false

    /*

     * Backend feed has priority.

     */

    if (backendContent) {

      try {

        actions.deserialize(

          JSON.stringify(

            backendContent,

          ),

        )

        restoredFromPersistence =

          true

      } catch {

        restoredFromPersistence =

          false

      }

    } else {

      /*

       * Local storage fallback.

       */

      const savedInterface =

        studyId

          ? loadStudyInterfaceTemplate(

              studyId,

            )

          : null

      if (savedInterface) {

        try {

          actions.deserialize(

            savedInterface.editorState,

          )

          restoredFromPersistence =

            true

        } catch {

          restoredFromPersistence =

            false

        }

      }

    }

    onReady(

      query.serialize(),

      restoredFromPersistence,

    )

  }, [

    actions,

    backendContent,

    onReady,

    query,

    studyId,

  ])

  return null

}

function ReadOnlyPanel() {

  return (

    <div className="p-5">

      <p className="text-xs font-semibold uppercase text-emerald-700">

        Published

      </p>

      <h2 className="mt-1 text-lg font-semibold text-[#172033]">

        Editing locked

      </h2>

      <p className="mt-3 text-sm leading-6 text-[#52667d]">

        This interface is read-only while

        the study is collecting responses.

      </p>

    </div>

  )

}

function ResearcherEdit() {

  const { studyId } =

    useParams<{

      studyId: string

    }>()

  const [searchParams] =

    useSearchParams()

  const requestedTemplate =

    searchParams.get(

      'template',

    )

  /*

   * Development source-template preview.

   *

   * Example:

   *

   * ?template=facebook&preview=true

   * ?template=instagram&preview=true

   *

   * Preview mode ignores persisted Craft content.

   */

  const isTemplatePreview =

    searchParams.get(

      'preview',

    ) === 'true'

  const lastSavedStateRef =

    useRef('')

  const currentStateRef =

    useRef('')

  const feedVersionRef =

    useRef(0)

  const hasBackendFeedRef =

    useRef(false)

  const isEditorReadyRef =

    useRef(false)

  const hasPersistedStateRef =

    useRef(false)

  const [

    study,

    setStudy,

  ] =

    useState<StudyResponse | null>(

      null,

    )

  const [

    feed,

    setFeed,

  ] =

    useState<StudyFeedResponse | null>(

      null,

    )

  const [

    isLoadingStudy,

    setIsLoadingStudy,

  ] =

    useState(true)

  const [

    loadError,

    setLoadError,

  ] =

    useState('')

  const [

    reloadVersion,

    setReloadVersion,

  ] =

    useState(0)

  const [

    hasUnsavedChanges,

    setHasUnsavedChanges,

  ] =

    useState(false)

  const [

    saveStatus,

    setSaveStatus,

  ] =

    useState<SaveStatus>(

      'loading',

    )

  const [

    saveError,

    setSaveError,

  ] =

    useState('')

  const [

    isPublishDialogOpen,

    setIsPublishDialogOpen,

  ] =

    useState(false)

  const [

    isPublishing,

    setIsPublishing,

  ] =

    useState(false)

  const [

    publishError,

    setPublishError,

  ] =

    useState('')

  const [

    copiedLink,

    setCopiedLink,

  ] =

    useState(false)

  const isEditable =

    study?.status ===

    'DRAFT'

  const blocker =

    useBlocker(

      Boolean(

        !isTemplatePreview &&

          isEditable &&

          hasUnsavedChanges,

      ),

    )

  /*

   * Load Study + Feed

   */

  useEffect(() => {

    let ignore = false

    const loadStudy =

      async () => {

        if (!studyId) {

          setLoadError(

            'The study ID is missing.',

          )

          setIsLoadingStudy(

            false,

          )

          return

        }

        setIsLoadingStudy(

          true,

        )

        setLoadError('')

        try {

          const [

            studyResponse,

            feedResponse,

          ] =

            await Promise.all([

              getStudy(

                studyId,

              ),

              getStudyFeed(

                studyId,

              ),

            ])

          if (!ignore) {

            feedVersionRef.current =

              feedResponse.version

            hasBackendFeedRef.current =

              feedResponse.content !==

              null

            setStudy(

              studyResponse,

            )

            setFeed(

              feedResponse,

            )

          }

        } catch (error) {

          if (!ignore) {

            setLoadError(

              getApiErrorMessage(

                error,

                'The study could not be loaded.',

              ),

            )

          }

        } finally {

          if (!ignore) {

            setIsLoadingStudy(

              false,

            )

          }

        }

      }

    void loadStudy()

    return () => {

      ignore = true

    }

  }, [

    reloadVersion,

    studyId,

  ])

  const backendTemplate =

    isEditorTemplate(

      feed?.templateCode,

    )

      ? feed.templateCode

      : null

  const template =

    isEditorTemplate(

      requestedTemplate,

    )

      ? requestedTemplate

      : backendTemplate ??

        'blank'

  const platformStyle:

    PlatformStyle =

      template === 'blank'

        ? 'facebook'

        : template

  const handleEditorReady = (

    editorState: string,

    restoredFromPersistence: boolean,

  ) => {

    currentStateRef.current =

      editorState

    lastSavedStateRef.current =

      editorState

    hasPersistedStateRef.current =

      restoredFromPersistence

    isEditorReadyRef.current =

      true

    setHasUnsavedChanges(

      false,

    )

    setSaveStatus(

      restoredFromPersistence

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

      !isEditorReadyRef.current ||

      !isEditable

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

      /*

       * Never save source-template preview

       * into a real Study.

       */

      if (

        isTemplatePreview

      ) {

        return false

      }

      if (

        !studyId ||

        !currentStateRef.current ||

        !isEditable

      ) {

        return false

      }

      setSaveStatus(

        'saving',

      )

      setSaveError('')

      try {

        const content =

          JSON.parse(

            currentStateRef.current,

          ) as Record<

            string,

            unknown

          >

        const response =

          await saveStudyFeed(

            studyId,

            content,

            feedVersionRef.current,

          )

        feedVersionRef.current =

          response.version

        hasBackendFeedRef.current =

          true

        setFeed(

          response,

        )

        saveStudyInterfaceTemplate(

          studyId,

          template,

          currentStateRef.current,

        )

        lastSavedStateRef.current =

          currentStateRef.current

        hasPersistedStateRef.current =

          true

        setHasUnsavedChanges(

          false,

        )

        setSaveStatus(

          'saved',

        )

        return true

      } catch (error) {

        setSaveError(

          getApiErrorMessage(

            error,

            'The interface could not be saved.',

          ),

        )

        setSaveStatus(

          'error',

        )

        return false

      }

    }

  useEffect(() => {

    if (

      isTemplatePreview ||

      !isEditable ||

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

  }, [

    hasUnsavedChanges,

    isEditable,

    isTemplatePreview,

  ])

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

  const handlePublish =

    async () => {

      if (

        isTemplatePreview

      ) {

        return

      }

      if (

        !studyId ||

        !study ||

        !isEditable

      ) {

        return

      }

      setIsPublishing(

        true,

      )

      setPublishError('')

      try {

        if (

          hasUnsavedChanges ||

          !hasBackendFeedRef.current

        ) {

          const didSave =

            await saveCurrentInterface()

          if (!didSave) {

            setPublishError(

              'The interface must be saved before it can be published.',

            )

            return

          }

        }

        const latestStudy =

          await getStudy(

            studyId,

          )

        const publishedStudy =

          await publishStudy(

            studyId,

            latestStudy.version,

          )

        setStudy(

          publishedStudy,

        )

        setHasUnsavedChanges(

          false,

        )

        setSaveStatus(

          'saved',

        )

        setIsPublishDialogOpen(

          false,

        )

      } catch (error) {

        setPublishError(

          getApiErrorMessage(

            error,

            'The study could not be published.',

          ),

        )

      } finally {

        setIsPublishing(

          false,

        )

      }

    }

  const copyParticipationLink =

    async () => {

      if (

        !study?.participationUrl

      ) {

        return

      }

      try {

        await navigator.clipboard.writeText(

          study.participationUrl,

        )

        setCopiedLink(

          true,

        )

        window.setTimeout(

          () =>

            setCopiedLink(

              false,

            ),

          2000,

        )

      } catch {

        setCopiedLink(

          false,

        )

      }

    }

  if (

    isLoadingStudy

  ) {

    return (

      <div className="min-h-screen bg-[#f3f6f8] text-gray-950">

        <ResearcherHeader />

        <p

          className="px-5 py-20 text-center text-sm text-gray-600"

          role="status"

        >

          Loading study...

        </p>

      </div>

    )

  }

  if (

    loadError ||

    !study ||

    !feed

  ) {

    return (

      <div className="min-h-screen bg-[#f3f6f8] text-gray-950">

        <ResearcherHeader />

        <main className="mx-auto max-w-3xl px-5 py-16 text-center">

          <h2 className="text-2xl font-semibold text-[#172033]">

            Study unavailable

          </h2>

          <p

            className="mt-3 text-sm text-red-700"

            role="alert"

          >

            {loadError ||

              'The study could not be loaded.'}

          </p>

          <button

            type="button"

            onClick={() =>

              setReloadVersion(

                (value) =>

                  value + 1,

              )

            }

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

      <AssetImageProvider

        studyId={

          study.id

        }

      >

      <Editor

        enabled={

          isEditable

        }

        onNodesChange={(

          query,

        ) =>

          handleNodesChange(

            query.serialize(),

          )

        }

        resolver={{

          /*

           * Shared widgets

           */

          PostWidget,

          TextWidget,

          AvatarWidget,

          ImageWidget,

          ActionBarWidget,

          /*

           * Shared canvas

           */

          CanvasContainer,

          ResearcherEditCanvas,

          /*

           * Facebook

           */

          FacebookTemplate,

          FacebookTopBar,

          FacebookStories,

          FacebookCreatePost,

          FacebookIcon,

          FacebookLeftSidebar,

          FacebookRightSidebar,

          FacebookDesktopShell,

          FacebookMainFeed,

          FacebookPostCard,

          FacebookHeaderRow,

          FacebookHeaderLeft,

          FacebookMetaColumn,

          FacebookBodySection,

          FacebookImageSection,

          FacebookFooterSection,

          FacebookActionButtonsRow,

          /*

           * Instagram

           */

          InstagramTemplate,

          InstagramSidebar,

          InstagramStories,

          InstagramSuggestions,

          InstagramIcon,

          InstagramDesktopShell,

          InstagramMainFeed,

          InstagramPostList,

          InstagramPostCard,

          InstagramHeaderRow,

          InstagramHeaderLeft,

          InstagramMetaColumn,

          InstagramImageSection,

          InstagramActionRow,

          InstagramFooterSection,

          InstagramCaptionSection,

          /*

           * TikTok

           */

          TikTokTemplate,

          TikTokIcon,

          TikTokSidebar,

          TikTokDesktopShell,

          TikTokMainArea,

          TikTokFeedStage,

          TikTokPostStage,

          TikTokVideoCard,

          TikTokActionRail,

          /*

           * X

           */

          XTemplate,

          XIcon,

          XSidebar,

          XRightSidebar,

          XComposer,

          XDesktopShell,

          XMainFeed,

          XPostList,

          /*
           * Threads
           */
          ThreadsTemplate,
          ThreadsIcon,
          ThreadsSidebar,
          ThreadsComposer,
          ThreadsDesktopShell,
          ThreadsMainFeed,
          ThreadsPostList,

          /*
           * Bluesky
           */
          BlueskyTemplate,
          BlueskyIcon,
          BlueskySidebar,
          BlueskyComposer,
          BlueskyRightSidebar,
          BlueskyDesktopShell,
          BlueskyMainFeed,
          BlueskyPostList,

          /*
           * Truth Social
           */
          TruthSocialTemplate,
          TruthSocialIcon,
          TruthSocialSidebar,
          TruthSocialComposer,
          TruthSocialRightSidebar,
          TruthSocialDesktopShell,
          TruthSocialMainFeed,
          TruthSocialPostList,

        }}

      >

        <RestoreStudyInterface

          key={`${studyId ?? 'new'}-${template}-${

            isTemplatePreview

              ? 'preview'

              : 'persisted'

          }`}

          studyId={

            isTemplatePreview

              ? undefined

              : studyId

          }

          backendContent={

            isTemplatePreview

              ? null

              : feed.content

          }

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

                {study.title}

              </h2>

            </div>

            <div className="flex flex-wrap items-center gap-3">

              {isTemplatePreview && (

                <span className="rounded-sm border border-violet-300 bg-violet-50 px-3 py-2 text-xs font-semibold text-violet-800">

                  TEMPLATE PREVIEW

                </span>

              )}

              <span

                className={`rounded-sm border px-3 py-2 text-xs font-semibold ${

                  statusStyles[

                    study.status

                  ]

                }`}

              >

                {study.status}

              </span>

              {study.questionnaireEnabled && !isTemplatePreview && (

                <Link

                  to={`/studies/${study.id}/questionnaire`}

                  className="rounded-sm border border-emerald-700 bg-white px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50"

                >

                  {study.status === 'DRAFT' ? 'Edit questionnaire' : 'View questionnaire'}

                </Link>

              )}

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

          </div>

        </section>

        {/* Participation */}

        {study.participationUrl &&

          !isTemplatePreview && (

            <section className="border-b border-emerald-200 bg-emerald-50 px-6 py-4 md:px-10">

              <div className="mx-auto flex w-full max-w-[96rem] flex-col justify-between gap-3 md:flex-row md:items-center">

                <div className="min-w-0">

                  <p className="text-xs font-semibold uppercase text-emerald-700">

                    Participation link

                  </p>

                  <p className="mt-1 truncate text-sm text-emerald-950">

                    {

                      study.participationUrl

                    }

                  </p>

                </div>

                <div className="flex shrink-0 gap-2">

                  <button

                    type="button"

                    onClick={() =>

                      void copyParticipationLink()

                    }

                    className="rounded-sm border border-emerald-700 bg-white px-3 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-100"

                  >

                    {copiedLink

                      ? 'Copied'

                      : 'Copy link'}

                  </button>

                  <a

                    href={

                      study.participationUrl

                    }

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

        {/* Editor */}

        <div className="grid flex-1 grid-cols-1 md:grid-cols-[15rem_minmax(0,1fr)] xl:grid-cols-[15rem_minmax(0,1fr)_19rem]">

          {/* Toolbox */}

          <aside className="border-b border-[#cbd6e2] bg-[#edf3f8] md:border-b-0 md:border-r">

            {isEditable ? (

              <Toolbox

                studyId={

                  study.id

                }

                styleId={

                  platformStyle

                }

              />

            ) : (

              <ReadOnlyPanel />

            )}

          </aside>

          {/* Canvas */}

          <main className="min-h-96 overflow-x-auto border-b border-[#cbd6e2] bg-white p-6 md:border-b-0 xl:border-r xl:p-8">

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

                {isEditable &&

                  !isTemplatePreview && (

                    <span

                      className={`rounded-sm border px-3 py-2 text-xs font-medium ${

                        saveStatusDetails[

                          saveStatus

                        ].className

                      }`}

                    >

                      {

                        saveStatusDetails[

                          saveStatus

                        ].label

                      }

                    </span>

                  )}

                {isTemplatePreview && (

                  <>

                    <span className="rounded-sm border border-violet-300 bg-violet-50 px-3 py-2 text-xs font-medium text-violet-800">

                      Source template

                    </span>

                    <ExportTemplateJson

                      template={template}

                    />

                  </>

                )}

                {isEditable &&

                  !isTemplatePreview && (

                    <>

                      <button

                        type="button"

                        onClick={() =>

                          void saveCurrentInterface()

                        }

                        disabled={

                          saveStatus ===

                            'saving' ||

                          saveStatus ===

                            'loading' ||

                          isPublishing

                        }

                        className="rounded-sm border border-emerald-700 bg-white px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-60"

                      >

                        {saveStatus ===

                        'saving'

                          ? 'Saving...'

                          : 'Save'}

                      </button>

                      <button

                        type="button"

                        onClick={() => {

                          setPublishError(

                            '',

                          )

                          setIsPublishDialogOpen(

                            true,

                          )

                        }}

                        disabled={

                          saveStatus ===

                            'saving' ||

                          saveStatus ===

                            'loading'

                        }

                        className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"

                      >

                        Publish study

                      </button>

                    </>

                  )}

              </div>

            </div>

            {saveError &&

              isEditable &&

              !isTemplatePreview && (

                <p

                  className="mb-4 rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"

                  role="alert"

                >

                  {saveError}

                </p>

              )}

            {/* Blank */}

            {template ===

            'blank' ? (

              <Frame

                key={`${studyId ?? 'new'}-${template}-${

                  isTemplatePreview

                    ? 'preview'

                    : 'persisted'

                }`}

              >

                <Element

                  is={

                    CanvasContainer

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

                key={`${studyId ?? 'new'}-${template}-${

                  isTemplatePreview

                    ? 'preview'

                    : 'persisted'

                }`}

              >

                <FacebookTemplate

                  canvasComponent={

                    ResearcherEditCanvas

                  }

                />

              </Frame>

            ) : template ===

              'instagram' ? (

              /*

               * Instagram

               */

              <Frame

                key={`${studyId ?? 'new'}-${template}-${

                  isTemplatePreview

                    ? 'preview'

                    : 'persisted'

                }`}

              >

                <InstagramTemplate

                  canvasComponent={

                    ResearcherEditCanvas

                  }

                />

              </Frame>

            ) : template ===

              'tiktok' ? (

              /*

               * TikTok

               */

              <Frame

                key={`${studyId ?? 'new'}-${template}-${

                  isTemplatePreview

                    ? 'preview'

                    : 'persisted'

                }`}

              >

                <TikTokTemplate

                  canvasComponent={

                    ResearcherEditCanvas

                  }

                />

              </Frame>

            ) : template ===

              'x' ? (

              /*

               * X

               */

              <Frame

                key={`${studyId ?? 'new'}-${template}-${

                  isTemplatePreview

                    ? 'preview'

                    : 'persisted'

                }`}

              >

                <XTemplate

                  canvasComponent={

                    ResearcherEditCanvas

                  }

                />

              </Frame>

             ) : template ===
              'threads' ? (
              /*
               * Threads
               */
              <Frame
                key={`${studyId ?? 'new'}-${template}-${
                  isTemplatePreview
                    ? 'preview'
                    : 'persisted'
                }`}
              >
                <ThreadsTemplate
                  canvasComponent={
                    ResearcherEditCanvas
                  }
                />
              </Frame>

             ) : template ===
              'bluesky' ? (
              /*
               * Bluesky
               */
              <Frame
                key={`${studyId ?? 'new'}-${template}-${isTemplatePreview ? 'preview' : 'persisted'}`}
              >
                <BlueskyTemplate canvasComponent={ResearcherEditCanvas} />
              </Frame>
             ) : template ===
              'truth-social' ? (
              /*
               * Truth Social
               */
              <Frame key={`${studyId ?? 'new'}-${template}-${isTemplatePreview ? 'preview' : 'persisted'}`}>
                <TruthSocialTemplate canvasComponent={ResearcherEditCanvas} />
              </Frame>
            ) : (
              /*
               * Remaining platforms
               */

              <Frame

                key={`${studyId ?? 'new'}-${template}-${

                  isTemplatePreview

                    ? 'preview'

                    : 'persisted'

                }`}

              >

                <Element

                  is={

                    CanvasContainer

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

            {isEditable ? (

              <PropertiesPanel />

            ) : (

              <ReadOnlyPanel />

            )}

          </aside>

        </div>

      </Editor>

      </AssetImageProvider>

      {/* Unsaved changes */}

      {!isTemplatePreview &&

        blocker.state ===

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

      {/* Publish */}

      {!isTemplatePreview &&

        isPublishDialogOpen && (

          <PublishStudyDialog

            studyTitle={

              study.title

            }

            isPublishing={

              isPublishing

            }

            errorMessage={

              publishError

            }

            onCancel={() => {

              if (

                !isPublishing

              ) {

                setIsPublishDialogOpen(

                  false,

                )

              }

            }}

            onConfirm={() =>

              void handlePublish()

            }

          />

        )}

    </div>

  )

}

export default ResearcherEdit
