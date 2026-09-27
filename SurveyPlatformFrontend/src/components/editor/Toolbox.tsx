import { useEditor } from '@craftjs/core'

import {
  AvatarWidget,
  TextWidget,
  ImageWidget,
  ActionBarWidget,
} from '../widgets'

import type { PlatformStyle } from '../widgets/types'


type ToolboxProps = {
  styleId: PlatformStyle
}


const toolButtonClass =
  'w-full cursor-grab rounded-sm border border-[#cbd6e2] bg-white px-3 py-2.5 text-left text-sm font-semibold text-[#172033] shadow-sm hover:border-emerald-400 hover:bg-emerald-50'


function Toolbox({
  styleId,
}: ToolboxProps) {
  const { connectors } = useEditor()

  return (
    <div className="w-full p-4">
      <p className="text-xs font-semibold uppercase text-emerald-700">
        Components
      </p>

      <h2 className="mt-1 text-lg font-semibold text-[#172033]">
        Widget Library
      </h2>

      <p className="mt-1 text-xs leading-5 text-[#52667d]">
        Drag a component onto the canvas.
      </p>

      <div className="mt-5 space-y-3">
        {/* Text Widget */}
        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <TextWidget
                  text="New text widget"
                  styleId={styleId}
                />,
              )
            }
          }}
          type="button"
          className={toolButtonClass}
        >
          Text
        </button>

        {/* Avatar Widget */}
        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <AvatarWidget
                  styleId={styleId}
                />,
              )
            }
          }}
          type="button"
          className={toolButtonClass}
        >
          Avatar
        </button>

        {/* Image Widget */}
        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <ImageWidget
                  styleId={styleId}
                />,
              )
            }
          }}
          type="button"
          className={toolButtonClass}
        >
          Image
        </button>

        {/* Action Bar Widget */}
        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <ActionBarWidget
                  likes={0}
                  comments={0}
                  shares={0}
                  styleId={styleId}
                />,
              )
            }
          }}
          type="button"
          className={toolButtonClass}
        >
          Action Bar
        </button>
      </div>
    </div>
  )
}

export default Toolbox