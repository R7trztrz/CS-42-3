import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type ActionBarWidgetProps = {
  likes?: number
  comments?: number
  shares?: number
  styleId?: PlatformStyle
}

function ActionBarWidget({
  likes = 0,
  comments = 0,
  shares = 0,
  styleId = 'facebook',
}: ActionBarWidgetProps) {
  const {
    connectors: { connect, drag },
    selected,
  } = useNode((node) => ({
    selected: node.events.selected,
  }))

  if (styleId === 'facebook') {
    return (
      <div
        ref={(ref) => {
          if (ref) {
            connect(drag(ref))
          }
        }}
        className={`flex cursor-move items-center justify-between ${
          selected
            ? 'rounded ring-2 ring-emerald-400 ring-offset-2'
            : ''
        }`}
      >
        <div className="flex items-center gap-2">
          <div className="flex -space-x-1">
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#1877f2] text-[10px] text-white ring-2 ring-white">
              👍
            </div>

            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#f55368] text-[10px] text-white ring-2 ring-white">
              ♥
            </div>
          </div>

          <span className="text-[14px] text-[#65676b]">
            {likes}
          </span>
        </div>

        <div className="flex items-center gap-3 text-[14px] text-[#65676b]">
          <span>
            {comments} comments
          </span>

          <span>
            {shares} shares
          </span>
        </div>
      </div>
    )
  }

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(drag(ref))
        }
      }}
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
  displayName: 'Action Bar Widget',
  props: {
    likes: 0,
    comments: 0,
    shares: 0,
    styleId: 'facebook',
  },
}

export default ActionBarWidget