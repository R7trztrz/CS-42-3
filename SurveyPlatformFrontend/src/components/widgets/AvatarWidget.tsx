import { useNode } from '@craftjs/core'
import type { PlatformStyle } from './types'

type AvatarWidgetProps = {
  src?: string
  alt?: string
  size?: number
  username?: string
  time?: string
  styleId?: PlatformStyle
}

function AvatarWidget({
  src = '',
  alt = 'User',
  size = 40,
  username = '@username',
  time = '2h ago',
  styleId = 'facebook',
}: AvatarWidgetProps) {
  const {
    connectors: { connect, drag },
    selected,
    nodeId,
  } = useNode((node) => ({
    selected: node.events.selected,
    nodeId: node.id,
  }))

  const initials = alt
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()

  if (styleId === 'instagram') {
    return (
      <div
        ref={(ref) => {
          if (ref) connect(drag(ref))
        }}
        data-component-id={nodeId}
        data-component-type="AvatarWidget"
        className={`flex w-full cursor-move items-center justify-between py-2 ${
          selected
            ? 'rounded ring-2 ring-emerald-400 ring-offset-2'
            : ''
        }`}
      >
        <div className="flex min-w-0 items-center gap-3">
          <div className="shrink-0 rounded-full bg-gradient-to-br from-purple-600 via-pink-500 to-orange-400 p-[2px]">
            <div className="rounded-full bg-white p-[2px]">
              <div
                className="overflow-hidden rounded-full"
                style={{
                  width: size,
                  height: size,
                }}
              >
                {src ? (
                  <img
                    src={src}
                    alt={alt}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center bg-[#efefef] text-[12px] font-semibold text-[#555]">
                    {initials || 'U'}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="min-w-0">
            <p className="truncate text-[13px] font-semibold leading-5 text-[#262626]">
              {username}
            </p>

            <p className="text-[11px] leading-4 text-[#8e8e8e]">
              {time}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-[3px] px-2 text-[#262626]">
          <span className="h-[3px] w-[3px] rounded-full bg-current" />
          <span className="h-[3px] w-[3px] rounded-full bg-current" />
          <span className="h-[3px] w-[3px] rounded-full bg-current" />
        </div>
      </div>
    )
  }

  const displaySize =
    styleId === 'facebook'
      ? Math.max(size, 48)
      : size

  return (
    <div
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      data-component-id={nodeId}
      data-component-type="AvatarWidget"
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
              ? 'flex h-full w-full items-center justify-center rounded-full bg-[#5b49ff] text-[13px] font-bold text-white'
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
    username: '@username',
    time: '2h ago',
    styleId: 'facebook',
  },
}

export default AvatarWidget