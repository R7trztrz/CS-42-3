import { useNode } from '@craftjs/core'

type ActionBarWidgetProps = {
  likes?: number
  comments?: number
  shares?: number
}

function ActionBarWidget({
  likes = 0,
  comments = 0,
  shares = 0,
}: ActionBarWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className="flex cursor-move gap-6 text-sm text-gray-600"
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
  },
}

export default ActionBarWidget