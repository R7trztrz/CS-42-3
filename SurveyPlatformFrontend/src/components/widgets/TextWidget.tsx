import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type TextWidgetProps = {
  text?: string
  className?: string
  styleId?: PlatformStyle
}

const platformTextStyles: Record<PlatformStyle, string> = {
  facebook: 'text-[#050505]',
  instagram: 'text-[#262626]',
  tiktok: 'text-black',
  x: 'text-[#0f1419]',
  threads: 'text-black',
  bluesky: 'text-[#161e27]',
  'truth-social': 'text-[#243447]',
}

function TextWidget({
  text = 'Sample text',
  className = '',
  styleId = 'facebook',
}: TextWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  return (
    <p
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className={`cursor-move text-base ${platformTextStyles[styleId]} ${className}`}
    >
      {text}
    </p>
  )
}

TextWidget.craft = {
  displayName: 'Text Widget',
  props: {
    text: 'Sample text',
    className: '',
    styleId: 'facebook',
  },
}

export default TextWidget