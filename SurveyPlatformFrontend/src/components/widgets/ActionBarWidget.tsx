import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type ActionBarWidgetProps = {
  likes?: number
  comments?: number
  shares?: number
  caption?: string
  styleId?: PlatformStyle
}

type ActionIconProps = {
  name:
    | 'heart'
    | 'comment'
    | 'send'
    | 'bookmark'
  size?: number
}

function ActionIcon({
  name,
  size = 24,
}: ActionIconProps) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  }

  if (name === 'heart') {
    return (
      <svg {...common}>
        <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" />
      </svg>
    )
  }

  if (name === 'comment') {
    return (
      <svg {...common}>
        <path d="M21 12a8 8 0 0 1-8 8H6l-3 2 1-5a8 8 0 1 1 17-5Z" />
      </svg>
    )
  }

  if (name === 'send') {
    return (
      <svg {...common}>
        <path d="m22 2-7 20-4-9-9-4 20-7Z" />
        <path d="M22 2 11 13" />
      </svg>
    )
  }

  return (
    <svg {...common}>
      <path d="M6 3h12v18l-6-4-6 4V3Z" />
    </svg>
  )
}

function ActionBarWidget({
  likes = 0,
  comments = 0,
  shares = 0,
  caption = '@username New post caption',
  styleId = 'facebook',
}: ActionBarWidgetProps) {
  const {
    connectors: {
      connect,
      drag,
    },
    selected,
    nodeId,
  } = useNode((node) => ({
    selected:
      node.events.selected,

    nodeId:
      node.id,
  }))

  if (styleId === 'facebook') {
    return (
      <div
        ref={(ref) => {
          if (ref) {
            connect(
              drag(ref),
            )
          }
        }}
        data-component-id={nodeId}
        data-component-type="ActionBarWidget"
        className={`w-full cursor-move ${
          selected
            ? 'rounded ring-2 ring-emerald-400 ring-offset-2'
            : ''
        }`}
      >
        <div className="flex min-h-[42px] items-center justify-between px-1 py-2">

          <div className="flex items-center gap-2">
            <div className="flex -space-x-1">
              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#1877f2] text-[10px] text-white ring-2 ring-white">
                👍
              </div>

              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#f55368] text-[10px] text-white ring-2 ring-white">
                ♥
              </div>
            </div>

            <span className="text-[13px] leading-none text-[#65676b]">
              {likes}
            </span>
          </div>

          <div className="flex items-center gap-3 text-[13px] leading-none text-[#65676b]">
            <span>
              {comments} comments
            </span>

            <span>
              {shares} shares
            </span>
          </div>

        </div>
      </div>
    )
  }

  if (styleId === 'instagram') {
    return (
      <div
        ref={(ref) => {
          if (ref) {
            connect(
              drag(ref),
            )
          }
        }}
        data-component-id={nodeId}
        data-component-type="ActionBarWidget"
        className={`w-full cursor-move text-[#262626] ${
          selected
            ? 'rounded ring-2 ring-emerald-400 ring-offset-2'
            : ''
        }`}
      >
        <div className="flex items-center justify-between">

          <div className="flex items-center gap-4">

            <ActionIcon
              name="heart"
              size={25}
            />

            <ActionIcon
              name="comment"
              size={25}
            />

            <ActionIcon
              name="send"
              size={24}
            />

          </div>

          <ActionIcon
            name="bookmark"
            size={24}
          />

        </div>

        <p className="mt-2 text-[13px] font-semibold">
          {likes} likes
        </p>

        <p className="mt-1 text-[13px] leading-5">
          {caption}
        </p>

        {comments > 0 && (
          <p className="mt-1 text-[12px] text-[#8e8e8e]">
            View all {comments} comments
          </p>
        )}
      </div>
    )
  }

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(
            drag(ref),
          )
        }
      }}
      data-component-id={nodeId}
      data-component-type="ActionBarWidget"
      className={`flex cursor-move gap-5 text-sm text-gray-600 ${
        selected
          ? 'rounded ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
    >
      <span>
        Like {likes}
      </span>

      <span>
        Comment {comments}
      </span>

      <span>
        Share {shares}
      </span>
    </div>
  )
}

ActionBarWidget.craft = {
  displayName:
    'Action Bar Widget',

  props: {
    likes:
      0,

    comments:
      0,

    shares:
      0,

    caption:
      '@username New post caption',

    styleId:
      'facebook',
  },
}

export default ActionBarWidget