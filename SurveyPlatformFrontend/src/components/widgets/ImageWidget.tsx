import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type ImageWidgetProps = {
  src?: string
  alt?: string
  styleId?: PlatformStyle
}

const platformImageStyles: Record<PlatformStyle, string> = {
  facebook: 'rounded-lg',
  instagram: 'rounded-none',
  tiktok: 'rounded-xl',
  x: 'rounded-2xl',
  threads: 'rounded-lg',
  bluesky: 'rounded-xl',
  'truth-social': 'rounded-lg',
}

function ImageWidget({
  src,
  alt = 'Content image',
  styleId = 'facebook',
}: ImageWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  const imageStyle = platformImageStyles[styleId]

  return (
    <div
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className="cursor-move"
    >
      {src ? (
        <img
          src={src}
          alt={alt}
          className={`w-full object-cover ${imageStyle}`}
        />
      ) : (
        <div
          className={`flex h-48 items-center justify-center bg-gray-100 text-gray-500 ${imageStyle}`}
        >
          Image placeholder
        </div>
      )}
    </div>
  )
}

ImageWidget.craft = {
  displayName: 'Image Widget',
  props: {
    src: '',
    alt: 'Content image',
    styleId: 'facebook',
  },
}

export default ImageWidget