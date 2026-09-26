import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type AvatarWidgetProps = {
  src?: string
  alt?: string
  size?: number
  styleId?: PlatformStyle
}

function AvatarWidget({
  src = '',
  alt = 'User',
  size = 40,
  styleId = 'facebook',
}: AvatarWidgetProps) {
  const {
    connectors: { connect, drag },
    selected,
  } = useNode((node) => ({
    selected: node.events.selected,
  }))

  const initials = alt
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()

  const displaySize =
    styleId === 'facebook'
      ? Math.max(size, 48)
      : size

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(drag(ref))
        }
      }}
      className={`inline-flex shrink-0 cursor-move items-center justify-center rounded-full ${
        selected
          ? 'ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
      style={{
        width: displaySize,
        height: displaySize,
      }}
    >
      {src ? (
        <img
          src={src}
          alt={alt}
          className="h-full w-full rounded-full object-cover"
        />
      ) : (
        <div
          className={
            styleId === 'facebook'
              ? 'flex h-full w-full items-center justify-center rounded-full bg-[#e4e6eb] text-sm font-semibold text-[#65676b]'
              : 'flex h-full w-full items-center justify-center rounded-full bg-gray-200 text-sm font-semibold text-gray-600'
          }
        >
          {initials || 'U'}
        </div>
      )}
    </div>
  )
}

AvatarWidget.craft = {
  displayName: 'Avatar Widget',
  props: {
    src: '',
    alt: 'User',
    size: 40,
    styleId: 'facebook',
  },
}

export default AvatarWidget