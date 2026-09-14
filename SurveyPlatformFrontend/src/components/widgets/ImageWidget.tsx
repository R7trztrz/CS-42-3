import { useNode } from '@craftjs/core'

type ImageWidgetProps = {
  src?: string
  alt?: string
}

function ImageWidget({
  src,
  alt = 'Content image',
}: ImageWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

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
          className="w-full rounded object-cover"
        />
      ) : (
        <div className="flex h-48 items-center justify-center rounded bg-gray-100 text-gray-500">
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
  },
}

export default ImageWidget