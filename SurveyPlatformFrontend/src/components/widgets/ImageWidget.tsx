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


  /*
   * Instagram
   */
  if (
    styleId ===
    'instagram'
  ) {
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
        data-component-type="ImageWidget"
        className={`aspect-square w-full cursor-move overflow-hidden bg-[#f4f5f7] ${
          selected
            ? 'ring-2 ring-emerald-400 ring-inset'
            : ''
        }`}
      >
        {src ? (
          <img
            src={src}
            alt={alt}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full min-h-[320px] w-full flex-col items-center justify-center text-[#8e8e8e]">

            <div className="text-3xl">
              🖼️
            </div>

            <p className="mt-2 text-[13px] font-medium">
              Image placeholder
            </p>

            {alt &&
              alt !== 'Image' &&
              alt !==
                'Image placeholder' && (
                <p className="mt-1 text-[11px] text-[#b0b0b0]">
                  {alt}
                </p>
              )}

          </div>
        )}
      </div>
    )
  }


  /*
   * Facebook
   */
  if (
    styleId ===
    'facebook'
  ) {
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
        data-component-type="ImageWidget"
        className={`w-full cursor-move overflow-hidden bg-[#e9edf2] ${
          selected
            ? 'ring-2 ring-emerald-400 ring-inset'
            : ''
        }`}
      >
        {src ? (
          <img
            src={src}
            alt={alt}
            className="max-h-[380px] w-full object-cover"
          />
        ) : (
          <div className="flex h-[300px] w-full flex-col items-center justify-center bg-[#e9edf2] text-[#65676b]">

            <div className="text-3xl">
              🖼️
            </div>

            <p className="mt-2 text-[13px] font-medium">
              Image placeholder
            </p>

            {alt &&
              alt !== 'Image' &&
              alt !==
                'Image placeholder' && (
                <p className="mt-1 text-[11px] text-[#8a8d91]">
                  {alt}
                </p>
              )}

          </div>
        )}
      </div>
    )
  }


  /*
   * Other platforms
   */
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
      data-component-type="ImageWidget"
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
          className="w-full rounded object-cover"
        />
      ) : (
        <div className="flex h-48 w-full flex-col items-center justify-center rounded bg-gray-100 text-gray-500">

          <div className="text-3xl">
            🖼️
          </div>

          <p className="mt-2 text-sm font-medium">
            Image placeholder
          </p>

        </div>
      )}
    </div>
  )
}


ImageWidget.craft = {
  displayName:
    'Image Widget',

  props: {
    src: '',
    alt: 'Image',
    styleId:
      'facebook',
  },
}


export default ImageWidget