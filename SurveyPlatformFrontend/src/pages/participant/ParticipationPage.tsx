import {

  Editor,

  Frame,

} from '@craftjs/core'

import axios from 'axios'

import {

  useEffect,

  useState,

} from 'react'

import {

  useParams,

} from 'react-router-dom'

import CanvasContainer from '../../components/editor/CanvasContainer'

import InterfaceErrorBoundary from '../../components/editor/InterfaceErrorBoundary'

import {

  ActionBarWidget,

  AvatarWidget,

  ImageWidget,

  PostWidget,

  TextWidget,

} from '../../components/widgets'

import { AssetImageProvider } from '../../components/widgets/AssetImageContext'

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

  FacebookComposerCard,

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

 * Participation API

 */

import {

  getParticipation,

  type ParticipationResponse,

} from '../../services/studyApi'

/*

 * M5 participant session (creation + restoration)

 */

import { useParticipantSession } from '../../participant/hooks/useParticipantSession'

import type { ParticipantDeviceInfo } from '../../participant/model/participantSession'

import ConsentStep from '../../participant/components/ConsentStep'

import CalibrationStep from '../../participant/components/CalibrationStep'

import QuestionnaireStep from '../../participant/components/QuestionnaireStep'

import FinishedStep from '../../participant/components/FinishedStep'

/*

 * Old Craft component names that may still exist

 * inside previously saved Study feeds.

 */

const legacyComponentNames: Record<string, string> = {

  ResearcherEditCanvas: 'CanvasContainer',

}

function normalizePublishedContent(

  value: unknown,

): unknown {

  if (Array.isArray(value)) {

    return value.map(

      normalizePublishedContent,

    )

  }

  if (

    !value ||

    typeof value !== 'object'

  ) {

    return value

  }

  return Object.fromEntries(

    Object.entries(

      value as Record<

        string,

        unknown

      >,

    ).map(([key, entry]) => {

      if (

        key === 'resolvedName' &&

        typeof entry === 'string'

      ) {

        return [

          key,

          legacyComponentNames[

            entry

          ] ?? entry,

        ]

      }

      return [

        key,

        normalizePublishedContent(

          entry,

        ),

      ]

    }),

  )

}

function getParticipationError(

  error: unknown,

) {

  if (axios.isAxiosError(error)) {

    if (!error.response) {

      return 'Unable to reach the study server.'

    }

    if (

      error.response.status === 401

    ) {

      return 'This participation session is not authorised.'

    }

    if (

      error.response.status === 410

    ) {

      return 'This study is now closed.'

    }

    if (

      error.response.status === 409

    ) {

      return 'The study interface is not ready.'

    }

    if (

      error.response.status === 404

    ) {

      return 'This participation link is not valid.'

    }

  }

  return 'The study could not be loaded.'

}

// Backend limits: browser <= 64 chars, os/timezone <= 64 (see
// ParticipantDeviceInfoRequest). navigator.userAgent is routinely well over
// 64 chars, so it must be truncated rather than passed through -- an
// oversized value fails validation with 400 REQUEST_VALIDATION_FAILED.
// There is no reliable cross-browser way to extract just "Chrome 131"
// without a UA-parsing dependency, so browserVersion is left unset.
const DEVICE_INFO_MAX_LENGTH = 64

function collectDeviceInfo(): ParticipantDeviceInfo {
  return {
    browser: navigator.userAgent.slice(0, DEVICE_INFO_MAX_LENGTH),
    os: navigator.platform.slice(0, DEVICE_INFO_MAX_LENGTH),
    screenWidth: window.screen?.width,
    screenHeight: window.screen?.height,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone.slice(0, DEVICE_INFO_MAX_LENGTH),
  }
}

function ParticipationPage() {

  const {

    token,

  } = useParams<{

    token: string

  }>()

  const [

    participation,

    setParticipation,

  ] =

    useState<ParticipationResponse | null>(

      null,

    )

  const [

    errorMessage,

    setErrorMessage,

  ] = useState('')

  const [

    isLoading,

    setIsLoading,

  ] = useState(true)

  useEffect(() => {

    let ignore = false

    // A new public link must not display the previous study or its error
    // while its own metadata/session are being loaded.
    setParticipation(null)
    setErrorMessage('')
    setIsLoading(true)

    const loadParticipation =

      async () => {

        if (!token) {

          setErrorMessage(

            'This participation link is not valid.',

          )

          setIsLoading(false)

          return

        }

        try {

          const response =

            await getParticipation(

              token,

            )

          if (!ignore) {

            setParticipation(

              response,

            )

          }

        } catch (error) {

          if (!ignore) {

            setErrorMessage(

              getParticipationError(

                error,

              ),

            )

          }

        } finally {

          if (!ignore) {

            setIsLoading(

              false,

            )

          }

        }

      }

    void loadParticipation()

    return () => {

      ignore = true

    }

  }, [

    token,

  ])

  const participantSession = useParticipantSession(token)

  return (

    <div className="min-h-screen bg-[#f3f6f8] text-[#172033]">

      {/* Participant header */}

      <header className="border-b border-[#314158] bg-[#101b2b] px-5 py-5 text-white md:px-8">

        <div className="mx-auto flex w-full max-w-6xl items-center gap-4">

          <div className="flex h-10 w-10 items-center justify-center rounded-md bg-emerald-500 text-sm font-bold text-[#101b2b]">

            SP

          </div>

          <div>

            <p className="text-xs font-semibold uppercase text-emerald-300">

              Research study

            </p>

            <h1 className="mt-1 text-xl font-semibold">

              Survey Platform

            </h1>

          </div>

        </div>

      </header>

      <main className="mx-auto w-full max-w-[110rem] px-5 py-8 md:px-8 md:py-10">

        {/* Loading */}

        {isLoading && (

          <p

            className="py-20 text-center text-sm text-gray-600"

            role="status"

          >

            Loading study...

          </p>

        )}

        {/* Error */}

        {!isLoading &&

          errorMessage && (

            <section className="mx-auto max-w-3xl rounded-md border border-red-200 bg-white px-6 py-16 text-center shadow-sm">

              <h2 className="text-xl font-semibold">

                Study unavailable

              </h2>

              <p

                className="mt-2 text-sm text-red-700"

                role="alert"

              >

                {errorMessage}

              </p>

            </section>

          )}

        {/* Participation */}

        {!isLoading &&

          participation && (

            <>

              {/* Study information */}

              <section className="mb-6 border-b border-gray-300 pb-6">

                <p className="text-sm font-semibold text-emerald-700">

                  Participant view

                </p>

                <h2 className="mt-1 text-3xl font-semibold">

                  {

                    participation.title

                  }

                </h2>

                {participation.description && (

                  <p className="mt-2 max-w-3xl text-sm leading-6 text-gray-600">

                    {

                      participation.description

                    }

                  </p>

                )}

              </section>

              {/* M5 session bootstrap (creation + restoration) */}

              {(participantSession.stage === 'checking' ||
                participantSession.stage === 'landing' ||
                participantSession.stage === 'creating' ||
                participantSession.stage === 'error' ||
                participantSession.stage === 'closed') && (
                <section className="mb-6 rounded-md border border-gray-200 bg-white px-6 py-5 shadow-sm">
                  {participantSession.stage === 'checking' && (
                    <p className="text-sm text-gray-600" role="status">
                      Checking for an existing session...
                    </p>
                  )}

                  {participantSession.stage === 'landing' && (
                    <div className="flex flex-col items-start gap-3">
                      <p className="text-sm text-gray-700">
                        Starting will create an anonymous session for this study.
                      </p>
                      <button
                        type="button"
                        onClick={() => void participantSession.createSession(collectDeviceInfo())}
                        className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800"
                      >
                        Start study
                      </button>
                    </div>
                  )}

                  {participantSession.stage === 'creating' && (
                    <p className="text-sm text-gray-600" role="status">
                      Starting your session...
                    </p>
                  )}

                  {(participantSession.stage === 'error' || participantSession.stage === 'closed') && (
                    <div className="flex flex-col items-start gap-3">
                      <p className="text-sm text-red-700" role="alert">
                        {participantSession.errorMessage}
                      </p>
                      {participantSession.stage === 'error' && (
                        <button
                          type="button"
                          onClick={() => void participantSession.retry()}
                          className="rounded-sm border border-red-700 bg-white px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-50"
                        >
                          Try again
                        </button>
                      )}
                    </div>
                  )}
                </section>
              )}

              {/* M5 phase-driven participant flow */}

              {participantSession.stage === 'active' && participantSession.session && (
                <>
                  {(participantSession.session.status === 'COMPLETED' ||
                    participantSession.session.status === 'ABANDONED') && (
                    <FinishedStep
                      status={participantSession.session.status}
                      abandonmentReason={participantSession.session.abandonmentReason}
                    />
                  )}

                  {participantSession.session.status === 'IN_PROGRESS' &&
                    participantSession.session.phase === 'CONSENT' && (
                      <ConsentStep
                        consentDocument={participantSession.session.consentDocument}
                        isPending={participantSession.isActionPending}
                        errorMessage={participantSession.actionErrorMessage}
                        onDecide={participantSession.decideConsent}
                      />
                    )}

                  {participantSession.session.status === 'IN_PROGRESS' &&
                    participantSession.session.phase === 'CALIBRATION' && (
                      <CalibrationStep onRefreshSession={participantSession.refreshSession} />
                    )}

                  {participantSession.session.status === 'IN_PROGRESS' &&
                    participantSession.session.phase === 'BROWSING' && (
                      <section className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-md border border-gray-200 bg-white px-6 py-4 shadow-sm">
                        <p className="text-sm text-gray-700">
                          Browse the interface below, then continue when you are done.
                        </p>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            disabled={participantSession.isActionPending}
                            onClick={() => void participantSession.completeBrowsing()}
                            className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-60"
                          >
                            Continue
                          </button>
                          <button
                            type="button"
                            disabled={participantSession.isActionPending}
                            onClick={() => void participantSession.abandon()}
                            className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60"
                          >
                            Leave study
                          </button>
                        </div>
                        {participantSession.actionErrorMessage && (
                          <p className="w-full text-sm font-semibold text-red-700" role="alert">
                            {participantSession.actionErrorMessage}
                          </p>
                        )}
                      </section>
                    )}

                  {participantSession.session.status === 'IN_PROGRESS' &&
                    participantSession.session.phase === 'QUESTIONNAIRE' &&
                    participantSession.sessionToken && (
                      <>
                        <QuestionnaireStep
                          sessionToken={participantSession.sessionToken}
                          onCompleteQuestionnaire={participantSession.completeQuestionnaire}
                          isCompleting={participantSession.isActionPending}
                          completeErrorMessage={participantSession.actionErrorMessage}
                        />
                        <div className="mb-6">
                          <button
                            type="button"
                            disabled={participantSession.isActionPending}
                            onClick={() => void participantSession.abandon()}
                            className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60"
                          >
                            Leave study
                          </button>
                        </div>
                      </>
                    )}
                </>
              )}

              {/* Published interface */}

              {participantSession.stage === 'active' &&
                participantSession.session?.status === 'IN_PROGRESS' &&
                participantSession.session?.phase === 'BROWSING' && (
              <section className="overflow-hidden rounded-md border border-gray-200 bg-white shadow-sm [&_.cursor-move]:cursor-default">

                <InterfaceErrorBoundary>

                  <AssetImageProvider

                    participationToken={token}

                  >

                  <Editor

                    enabled={false}

                    resolver={{

                      /*

                       * Shared widgets

                       */

                      TextWidget,

                      AvatarWidget,

                      ImageWidget,

                      ActionBarWidget,

                      PostWidget,

                      /*

                       * Shared canvas

                       */

                      CanvasContainer,

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

                      FacebookComposerCard,

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

                    <Frame

                      data={JSON.stringify(

                        normalizePublishedContent(

                          participation.content,

                        ),

                      )}

                    />

                  </Editor>

                  </AssetImageProvider>

                </InterfaceErrorBoundary>

              </section>
                )}

            </>

          )}

      </main>

    </div>

  )

}

export default ParticipationPage
