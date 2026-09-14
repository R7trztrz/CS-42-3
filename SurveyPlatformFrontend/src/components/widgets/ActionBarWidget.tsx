import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type ActionBarWidgetProps = {
  likes?: number
  comments?: number
  shares?: number
  styleId?: PlatformStyle
}

const platformActionStyles: Record<PlatformStyle, string> = {
  facebook: 'text-gray-600',
  instagram: 'text-gray-900',
  tiktok: 'text-black',
  x: 'text-[#536471]',
  threads: 'text-black',
  bluesky: 'text-[#42576c]',
  'truth-social': 'text-blue-800',
}

function ActionBarWidget({
  likes = 0,
  comments = 0,
  shares = 0,
  styleId = 'facebook',
}: ActionBarWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className={`flex cursor-move gap-6 text-sm ${platformActionStyles[styleId]}`}
    >
      <button type="button">Like {likes}</button>
      <button type="button">Comment {comments}</button>
      <button type="button">Share {shares}</button>
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