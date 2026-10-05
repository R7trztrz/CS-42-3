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


  const getFacebookTextClass =
    () => {
      if (
        text ===
        'Emma Wilson'
      ) {
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
        return 'text-[20px] font-semibold tracking-wider text-[#65676b]'
      }

      if (
        text === 'Like' ||
        text === 'Comment' ||
        text === 'Share'
      ) {
        return 'text-center text-[14px] font-semibold text-[#65676b]'
      }

      if (
        text ===
        "What's on your mind?"
      ) {
        return 'text-[15px] font-normal text-[#65676b]'
      }

      return 'text-[15px] leading-[1.45] text-[#050505]'
    }


  const getInstagramTextClass =
    () => {
      if (
        text.startsWith('@') &&
        !text.includes(' ')
      ) {
        return 'text-[13px] font-semibold leading-5 text-[#262626]'
      }

      if (
        text
          .toLowerCase()
          .includes('ago')
      ) {
        return 'text-[11px] leading-4 text-[#8e8e8e]'
      }

      if (
        text
          .toLowerCase()
          .includes('likes')
      ) {
        return 'text-[13px] font-semibold leading-5 text-[#262626]'
      }

      if (
        text.startsWith(
          'View all',
        )
      ) {
        return 'text-[12px] leading-5 text-[#8e8e8e]'
      }

      if (
        text === '···' ||
        text === '...'
      ) {
        return 'text-[18px] font-semibold tracking-widest text-[#262626]'
      }

      return 'text-[13px] leading-5 text-[#262626]'
    }


  const textClass =
    styleId === 'facebook'
      ? getFacebookTextClass()
      : styleId ===
          'instagram'
        ? getInstagramTextClass()
        : 'text-sm text-gray-800'


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
      data-component-type="TextWidget"
      className={`cursor-move rounded-sm ${
        selected
          ? 'ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
    >
      <p className={textClass}>
        {text}
      </p>
    </div>
  )
}


TextWidget.craft = {
  displayName:
    'Text Widget',

  props: {
    text:
      'New text widget',

    styleId:
      'facebook',
  },
}


export default TextWidget