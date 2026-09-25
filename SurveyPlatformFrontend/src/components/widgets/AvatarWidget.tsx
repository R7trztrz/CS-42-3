import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type AvatarWidgetProps = {
  src?: string
  alt?: string
  size?: number
  styleId?: PlatformStyle
}

const platformAvatarStyles: Record<PlatformStyle, string> = {
  facebook: 'rounded-full ring-1 ring-gray-200',
  instagram: 'rounded-full ring-2 ring-pink-500',
  tiktok: 'rounded-full ring-2 ring-cyan-400',
  x: 'rounded-full ring-1 ring-gray-300',
  threads: 'rounded-full ring-1 ring-black',
  bluesky: 'rounded-full ring-2 ring-sky-400',
  'truth-social': 'rounded-full ring-2 ring-blue-700',
}

function AvatarWidget({
  src,
  alt = 'User avatar',
  size = 40,
  styleId = 'facebook',
}: AvatarWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className={`cursor-move overflow-hidden bg-gray-200 ${platformAvatarStyles[styleId]}`}
      style={{ width: size, height: size }}
    >
      {src ? (
        <img
          src={src}
          alt={alt}
          className="h-full w-full object-cover"
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center text-sm text-gray-500">
          U
        </div>
      )}
    </div>
  )
}

AvatarWidget.craft = {
  displayName: 'Avatar Widget',
  props: {
    src: '',
    alt: 'User avatar',
    size: 40,
    styleId: 'facebook',
  },
}

export default AvatarWidget