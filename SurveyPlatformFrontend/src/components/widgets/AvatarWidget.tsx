import { useNode } from '@craftjs/core'

type AvatarWidgetProps = {
  src?: string
  alt?: string
  size?: number
}

function AvatarWidget({
  src,
  alt = 'User avatar',
  size = 40,
}: AvatarWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className="cursor-move overflow-hidden rounded-full bg-gray-200"
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
  },
}

export default AvatarWidget