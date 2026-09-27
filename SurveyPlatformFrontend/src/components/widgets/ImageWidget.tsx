import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type ImageWidgetProps = {
  src?: string
  alt?: string
  styleId?: PlatformStyle
}

function ImageWidget({
  src = '',
  alt = 'Image',
  styleId = 'facebook',
}: ImageWidgetProps) {
  const {
    connectors: { connect, drag },
    selected,
  } = useNode((node) => ({
    selected: node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(drag(ref))
        }
      }}
      className={`w-full cursor-move ${
        selected
          ? 'ring-2 ring-emerald-400 ring-inset'
          : ''
      }`}
    >
      {src ? (
        <img
          src={src}
          alt={alt}
          className={
            styleId === 'facebook'
              ? 'max-h-[440px] w-full object-cover'
              : 'w-full rounded object-cover'
          }
        />
      ) : (
        <div
          className={
            styleId === 'facebook'
              ? 'flex h-[310px] w-full flex-col items-center justify-center bg-[#f0f2f5] text-[#65676b]'
              : 'flex h-48 w-full items-center justify-center rounded bg-gray-100 text-gray-500'
          }
        >
          <div className="mb-2 text-3xl">
            🖼️
          </div>

          <p className="text-sm font-medium">
            Image placeholder
          </p>

          {alt &&
            alt !== 'Image placeholder' &&
            alt !== 'Image' && (
              <p className="mt-1 text-xs text-gray-400">
                {alt}
              </p>
            )}
        </div>
      )}
    </div>
  )
}

ImageWidget.craft = {
  displayName: 'Image Widget',
  props: {
    src: '',
    alt: 'Image',
    styleId: 'facebook',
  },
}

export default ImageWidget