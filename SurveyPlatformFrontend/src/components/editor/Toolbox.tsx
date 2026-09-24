import { useEditor } from '@craftjs/core'
import {
  AvatarWidget,
  TextWidget,
  ImageWidget,
  ActionBarWidget,
} from '../widgets'

const toolButtonClass = 'w-full cursor-grab rounded-sm border border-[#cbd6e2] bg-white px-3 py-2.5 text-left text-sm font-semibold text-[#172033] shadow-sm hover:border-emerald-400 hover:bg-emerald-50'

function Toolbox() {
  const { connectors } = useEditor()

  return (
    <div className="w-full p-4">
      <p className="text-xs font-semibold uppercase text-emerald-700">Components</p>
      <h2 className="mt-1 text-lg font-semibold text-[#172033]">Widget Library</h2>
      <p className="mt-1 text-xs leading-5 text-[#52667d]">Drag a component onto the canvas.</p>

      <div className="mt-5 space-y-3">
        <button
          ref={(ref) => {
            if (ref) connectors.create(ref, <TextWidget text="New text widget" styleId="facebook" />)
          }}
          type="button"
          className={toolButtonClass}
        >
          Text
        </button>

        <button
          ref={(ref) => {
            if (ref) connectors.create(ref, <AvatarWidget styleId="facebook" />)
          }}
          type="button"
          className={toolButtonClass}
        >
          Avatar
        </button>

        <button
          ref={(ref) => {
            if (ref) connectors.create(ref, <ImageWidget styleId="facebook" />)
          }}
          type="button"
          className={toolButtonClass}
        >
          Image
        </button>

        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <ActionBarWidget
                  likes={0}
                  comments={0}
                  shares={0}
                  styleId="facebook"
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
