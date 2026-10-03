import { useEditor } from '@craftjs/core'
import axios from 'axios'
import {
  useState,
  type FormEvent,
} from 'react'

import {
  generateLinkPreview,
} from '../../services/studyApi'
import PostWidget from '../widgets/PostWidget'
import type { PlatformStyle } from '../widgets/types'


type ImportPostDialogProps = {
  initialStyleId: PlatformStyle
  studyId: string
}


const platformLabels: Record<PlatformStyle, string> = {
  facebook: 'Facebook',
  instagram: 'Instagram',
  tiktok: 'TikTok',
  x: 'X',
  threads: 'Threads',
  bluesky: 'Bluesky',
  'truth-social': 'Truth Social',
}


const targetCanvasIds: Partial<Record<PlatformStyle, string>> = {
  facebook: 'facebook-main-feed',
  instagram: 'instagram-post-list',
  tiktok: 'tiktok-feed',
}


function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const responseData =
      error.response?.data as {
        code?: string
        error?: string
        message?: string
      } | undefined
    const code = responseData?.code
    const status = error.response?.status

    if (!error.response) {
      return 'Unable to reach the study server.'
    }

    if (
      code === 'PREVIEW_URL_INVALID' ||
      status === 400
    ) {
      return 'Enter a public HTTP or HTTPS URL.'
    }

    if (
      code === 'PREVIEW_FETCH_FAILED' ||
      status === 502
    ) {
      return 'The website could not be reached within the preview limits.'
    }

    if (
      code === 'PREVIEW_CONTENT_UNSUPPORTED' ||
      status === 422
    ) {
      return 'This URL does not point to a supported web page.'
    }

    if (code === 'STUDY_NOT_EDITABLE') {
      return 'Posts can only be imported into a draft study.'
    }

    return (
      responseData?.error ??
      responseData?.message ??
      'The post could not be generated.'
    )
  }

  if (
    error instanceof Error &&
    error.message === 'Canvas not found'
  ) {
    return 'No editable canvas was found for the generated post.'
  }

  return 'The post could not be generated.'
}


function ImportPostDialog({
  initialStyleId,
  studyId,
}: ImportPostDialogProps) {
  const {
    actions,
    query,
  } = useEditor()

  const [isOpen, setIsOpen] =
    useState(false)
  const [isGenerating, setIsGenerating] =
    useState(false)
  const [url, setUrl] =
    useState('')
  const [styleId, setStyleId] =
    useState<PlatformStyle>(initialStyleId)
  const [errorMessage, setErrorMessage] =
    useState('')

  const closeDialog = () => {
    if (isGenerating) {
      return
    }

    setIsOpen(false)
    setUrl('')
    setErrorMessage('')
    setStyleId(initialStyleId)
  }

  const findTargetCanvas = () => {
    const nodes = query.getNodes()
    const preferredId = targetCanvasIds[initialStyleId]

    if (
      preferredId &&
      nodes[preferredId]
    ) {
      return preferredId
    }

    const depth = (nodeId: string) => {
      let currentId: string | null = nodeId
      let value = 0
      const visited = new Set<string>()

      while (
        currentId &&
        currentId !== 'ROOT' &&
        !visited.has(currentId)
      ) {
        visited.add(currentId)
        currentId = nodes[currentId]?.data.parent ?? null
        value += 1
      }

      return value
    }

    const fallback = Object.entries(nodes)
      .filter(
        ([, node]) => node.data.isCanvas,
      )
      .sort(
        ([leftId], [rightId]) =>
          depth(rightId) - depth(leftId),
      )[0]

    return fallback?.[0]
  }

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault()

    const trimmedUrl = url.trim()

    if (!trimmedUrl) {
      setErrorMessage('Enter a URL to generate a post.')
      return
    }

    setIsGenerating(true)
    setErrorMessage('')

    try {
      const targetId = findTargetCanvas()

      if (!targetId) {
        throw new Error('Canvas not found')
      }

      const preview = await generateLinkPreview(
        studyId,
        trimmedUrl,
      )

      const caption = [
        preview.title,
        preview.description,
      ].filter(
        (value): value is string =>
          Boolean(value?.trim()),
      ).join('\n\n')

      const nodeTree = query
        .parseReactElement(
          <PostWidget
            styleId={styleId}
            displayName="Imported post"
            username="@imported"
            time="Just now"
            caption={caption}
            assetId={preview.image?.assetId}
            imageSrc=""
            likes={0}
            comments={0}
            shares={0}
            saves={0}
          />,
        )
        .toNodeTree()

      actions.addNodeTree(
        nodeTree,
        targetId,
      )

      setIsOpen(false)
      setUrl('')
      setErrorMessage('')
      setStyleId(initialStyleId)
    } catch (error) {
      setErrorMessage(
        getErrorMessage(error),
      )
    } finally {
      setIsGenerating(false)
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => {
          setStyleId(initialStyleId)
          setIsOpen(true)
        }}
        className="w-full rounded-sm border border-emerald-700 bg-emerald-700 px-3 py-2.5 text-left text-sm font-semibold text-white shadow-sm hover:bg-emerald-800"
      >
        Import post from URL
      </button>

      {isOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeDialog()
            }
          }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="import-post-title"
            className="w-full max-w-lg rounded-md border border-gray-200 bg-white p-6 shadow-xl"
          >
            <h2
              id="import-post-title"
              className="text-lg font-semibold text-[#172033]"
            >
              Import post from URL
            </h2>

            <p className="mt-1 text-sm leading-6 text-[#52667d]">
              Generate an editable post from the page title, description and image.
            </p>

            <form
              className="mt-5 space-y-4"
              onSubmit={(event) =>
                void handleSubmit(event)
              }
            >
              <div>
                <label
                  htmlFor="post-import-url"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Website URL
                </label>

                <input
                  id="post-import-url"
                  type="url"
                  required
                  maxLength={2048}
                  autoFocus
                  value={url}
                  onChange={(event) =>
                    setUrl(event.target.value)
                  }
                  placeholder="https://example.com/article"
                  className="w-full rounded-sm border border-gray-300 px-3 py-2 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                />
              </div>

              <div>
                <label
                  htmlFor="post-import-platform"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Post style
                </label>

                <select
                  id="post-import-platform"
                  value={styleId}
                  onChange={(event) =>
                    setStyleId(event.target.value as PlatformStyle)
                  }
                  className="w-full rounded-sm border border-gray-300 bg-white px-3 py-2 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                >
                  {Object.entries(platformLabels).map(
                    ([value, label]) => (
                      <option
                        key={value}
                        value={value}
                      >
                        {label}
                      </option>
                    ),
                  )}
                </select>
              </div>

              {errorMessage && (
                <p
                  className="rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
                  role="alert"
                >
                  {errorMessage}
                </p>
              )}

              <div className="flex justify-end gap-2 border-t border-gray-200 pt-4">
                <button
                  type="button"
                  onClick={closeDialog}
                  disabled={isGenerating}
                  className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={isGenerating}
                  className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:cursor-wait disabled:opacity-60"
                >
                  {isGenerating
                    ? 'Generating...'
                    : 'Generate post'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}


export default ImportPostDialog
