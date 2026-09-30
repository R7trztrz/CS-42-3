import { Editor, Frame, Element, useEditor } from '@craftjs/core'
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

function CanvasContainer({
  children,
}: {
  children?: React.ReactNode
}) {
  return (
    <div className="min-h-[560px] w-full rounded border-2 border-dashed border-gray-400 p-6">
      {children}
    </div>
  )
}

CanvasContainer.craft = {
  displayName: 'Canvas Container',
}

function ExportButton() {
  const { query } = useEditor()

  const handleExport = async () => {
    const serialized = query.serialize()

    console.log('Craft.js serialized JSON:')
    console.log(serialized)

    try {
      await navigator.clipboard.writeText(serialized)
      alert('Editor JSON copied to clipboard.')
    } catch {
      alert('JSON generated. Check the browser console.')
    }
  }

  return (
    <button
      type="button"
      onClick={handleExport}
      className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
    >
      Export JSON
    </button>
  )
}

function WidgetEditor() {
  const [searchParams] = useSearchParams()
  const templateId = searchParams.get('template')

  const currentTemplate =
    templateNames[
      templateId as keyof typeof templateNames
    ] ?? templateNames['blank-canvas']

  return (
    <div className="p-8">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">
            Widget Editor
          </h1>

          <p className="mt-2 text-sm font-medium text-gray-600">
            Current template: {currentTemplate}
          </p>
        </div>
      </div>

      <Editor
        resolver={{
          TextWidget,
          AvatarWidget,
          ImageWidget,
          ActionBarWidget,
          CanvasContainer,
        }}
      >
        <div className="mb-4 flex justify-end">
          <ExportButton />
        </div>

        <div className="grid grid-cols-[260px_minmax(0,1fr)_320px] items-start gap-6">
          <div className="min-w-0">
            <Toolbox styleId="facebook" />
          </div>

          <div className="min-w-0">
            <Frame>
              <Element is={CanvasContainer} canvas>
                <TextWidget
                  text="What's on your mind?"
                  styleId="facebook"
                />

                <Element is={CanvasContainer} canvas>
                  <AvatarWidget
                    src=""
                    alt="Emma Wilson"
                    size={40}
                    styleId="facebook"
                  />

                  <TextWidget
                    text="Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂"
                    styleId="facebook"
                  />

                  <ImageWidget
                    src=""
                    alt="Campus autumn scenery"
                    styleId="facebook"
                  />

                  <ActionBarWidget
                    likes={47}
                    comments={12}
                    shares={8}
                    styleId="facebook"
                  />
                </Element>
              </Element>
            </Frame>
          </div>

          <div className="min-w-0">
            <PropertiesPanel />
          </div>
        </div>
      </Editor>
    </div>
  )
}

export default WidgetEditor