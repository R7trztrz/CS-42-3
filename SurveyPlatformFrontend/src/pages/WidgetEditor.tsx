import { Editor, Frame, Element } from '@craftjs/core'
import {
  AvatarWidget,
  TextWidget,
  ImageWidget,
  ActionBarWidget,
} from '../components/widgets'
import Toolbox from '../components/editor/Toolbox'
import PropertiesPanel from '../components/editor/PropertiesPanel'

function CanvasContainer({ children }: { children?: React.ReactNode }) {
  return (
    <div className="min-h-96 rounded border-2 border-dashed border-gray-400 p-6">
      {children}
    </div>
  )
}

CanvasContainer.craft = {
  displayName: 'Canvas Container',
}

function WidgetEditor() {
  return (
    <div className="p-8">
      <h1 className="mb-6 text-3xl font-bold">
        Widget Editor
      </h1>

      <Editor
        resolver={{
          TextWidget,
          AvatarWidget,
          ImageWidget,
          ActionBarWidget,
          CanvasContainer,
        }}
      >
        <div className="flex gap-6">
          <Toolbox />

          <div className="flex-1">
            <Frame>
              <Element is={CanvasContainer} canvas>
                <TextWidget text="Existing text widget" />
              </Element>
            </Frame>
          </div>

          <PropertiesPanel />
        </div>
      </Editor>
    </div>
  )
}

export default WidgetEditor