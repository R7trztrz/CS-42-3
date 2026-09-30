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
 * Participation API
 */
import {
  getParticipation,
  type ParticipationResponse,
} from '../../services/studyApi'

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

              {/* Published interface */}
              <section className="overflow-hidden rounded-md border border-gray-200 bg-white shadow-sm [&_.cursor-move]:cursor-default">
                <InterfaceErrorBoundary>
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
                </InterfaceErrorBoundary>
              </section>
            </>
          )}
      </main>
    </div>
  )
}

export default ParticipationPage
