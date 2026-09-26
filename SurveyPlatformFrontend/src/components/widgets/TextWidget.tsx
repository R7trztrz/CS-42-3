import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type TextWidgetProps = {
  text?: string
  styleId?: PlatformStyle
}

function TextWidget({
  text = 'New text widget',
  styleId = 'facebook',
}: TextWidgetProps) {
  const {
    connectors: { connect, drag },
    selected,
  } = useNode((node) => ({
    selected: node.events.selected,
  }))

  const getFacebookTextClass = () => {
    if (text === 'Emma Wilson') {
      return 'text-[15px] font-semibold leading-5 text-[#050505]'
    }

    if (
      text.includes('ago') ||
      text.includes('🌐')
    ) {
      return 'text-[13px] leading-4 text-[#65676b]'
    }

    if (
      text === '···' ||
      text === '...'
    ) {
      return 'text-xl font-semibold tracking-wider text-[#65676b]'
    }

    if (
      text.includes('Like') ||
      text.includes('Comment') ||
      text.includes('Share')
    ) {
      return 'text-center text-[14px] font-semibold text-[#65676b]'
    }

    if (
      text === "What's on your mind?"
    ) {
      return 'text-[16px] font-normal text-[#65676b]'
    }

    return 'text-[15px] leading-6 text-[#050505]'
  }

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(drag(ref))
        }
      }}
      className={`cursor-move rounded-sm ${
        selected
          ? 'ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
    >
      <p
        className={
          styleId === 'facebook'
            ? getFacebookTextClass()
            : 'text-sm text-gray-800'
        }
      >
        {text}
      </p>
    </div>
  )
}

TextWidget.craft = {
  displayName: 'Text Widget',
  props: {
    text: 'New text widget',
    styleId: 'facebook',
  },
}

export default TextWidget