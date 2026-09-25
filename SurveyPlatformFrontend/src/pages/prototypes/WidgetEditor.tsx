import { Editor, Frame, Element } from '@craftjs/core'
import { useSearchParams } from 'react-router-dom'
import {
  AvatarWidget,
  TextWidget,
  ImageWidget,
  ActionBarWidget,
} from '../../components/widgets'
import Toolbox from '../../components/editor/Toolbox'
import PropertiesPanel from '../../components/editor/PropertiesPanel'

const templateNames = {
  facebook: 'Facebook',
  instagram: 'Instagram',
  tiktok: 'TikTok',
  x: 'X',
  threads: 'Threads',
  bluesky: 'Bluesky',
  'truth-social': 'Truth Social',
  'blank-canvas': 'Blank Canvas',
} as const

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
  const [searchParams] = useSearchParams()
  const templateId = searchParams.get('template')
  const currentTemplate =
    templateNames[templateId as keyof typeof templateNames] ??
    templateNames['blank-canvas']

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold">
        Widget Editor
      </h1>
      <p className="mb-6 mt-2 text-sm font-medium text-gray-600">
        Current template: {currentTemplate}
      </p>

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
