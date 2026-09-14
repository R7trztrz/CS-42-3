import { useEditor } from '@craftjs/core'
import {
  AvatarWidget,
  TextWidget,
  ImageWidget,
  ActionBarWidget,
} from '../widgets'

function Toolbox() {
  const { connectors } = useEditor()

  return (
    <aside className="w-56 rounded border border-gray-300 bg-white p-4">
      <h2 className="mb-4 text-lg font-semibold">
        Widget Library
      </h2>

      <div className="space-y-3">
        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <TextWidget text="New text widget" />
              )
            }
          }}
          type="button"
          className="w-full cursor-grab rounded border border-gray-300 px-3 py-2 text-left hover:bg-gray-100"
        >
          Text
        </button>

        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <AvatarWidget />
              )
            }
          }}
          type="button"
          className="w-full cursor-grab rounded border border-gray-300 px-3 py-2 text-left hover:bg-gray-100"
        >
          Avatar
        </button>

        <button
          ref={(ref) => {
            if (ref) {
              connectors.create(
                ref,
                <ImageWidget />
              )
            }
          }}
          type="button"
          className="w-full cursor-grab rounded border border-gray-300 px-3 py-2 text-left hover:bg-gray-100"
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
                />
              )
            }
          }}
          type="button"
          className="w-full cursor-grab rounded border border-gray-300 px-3 py-2 text-left hover:bg-gray-100"
        >
          Action Bar
        </button>
      </div>
    </aside>
  )
}

export default Toolbox