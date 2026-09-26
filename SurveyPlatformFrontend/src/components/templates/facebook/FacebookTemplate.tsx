import { Element, useNode } from '@craftjs/core'

import CanvasContainer from '../../editor/CanvasContainer'

import {
  ActionBarWidget,
  AvatarWidget,
  ImageWidget,
  TextWidget,
} from '../../widgets'

import FacebookTopBar from './FacebookTopBar'
import FacebookStories from './FacebookStories'
import FacebookCreatePost from './FacebookCreatePost'

import {
  FacebookActionButtonsRow,
  FacebookBodySection,
  FacebookFooterSection,
  FacebookHeaderLeft,
  FacebookHeaderRow,
  FacebookImageSection,
  FacebookMetaColumn,
  FacebookPostCard,
} from './FacebookLayout'

type FacebookTemplateProps = {
  canvasComponent: React.ElementType
}

function FacebookTemplate({
  canvasComponent: CanvasComponent,
}: FacebookTemplateProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="w-full"
    >
      <Element
        id="facebook-root"
        is={CanvasComponent}
        canvas
      >
        <div className="mx-auto w-full max-w-[620px] overflow-hidden rounded-xl bg-[#f0f2f5] shadow-sm">
          {/* Fixed Facebook shell */}
          <div className="w-full">
            <FacebookTopBar />
          </div>

          <div className="mx-auto w-full max-w-[620px] px-5 py-5">
            {/* Stories */}
            <div className="mb-4">
              <FacebookStories />
            </div>

            {/* Create post */}
            <div className="mb-4 flex justify-center">
              <FacebookCreatePost />
            </div>

            {/* Editable post */}
            <div className="flex justify-center">
              <Element
                id="facebook-post"
                is={FacebookPostCard}
                canvas
              >
                {/* Header */}
                <Element
                  id="facebook-header"
                  is={FacebookHeaderRow}
                  canvas
                >
                  <Element
                    id="facebook-header-left"
                    is={FacebookHeaderLeft}
                    canvas
                  >
                    <AvatarWidget
                      src=""
                      alt="Emma Wilson"
                      size={48}
                      styleId="facebook"
                    />

                    <Element
                      id="facebook-meta"
                      is={FacebookMetaColumn}
                      canvas
                    >
                      <TextWidget
                        text="Emma Wilson"
                        styleId="facebook"
                      />

                      <TextWidget
                        text="2h ago · 🌐"
                        styleId="facebook"
                      />
                    </Element>
                  </Element>

                  <TextWidget
                    text="···"
                    styleId="facebook"
                  />
                </Element>

                {/* Body */}
                <Element
                  id="facebook-body"
                  is={FacebookBodySection}
                  canvas
                >
                  <TextWidget
                    text="Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂"
                    styleId="facebook"
                  />
                </Element>

                {/* Image */}
                <Element
                  id="facebook-image"
                  is={FacebookImageSection}
                  canvas
                >
                  <ImageWidget
                    src=""
                    alt="Campus autumn scenery"
                    styleId="facebook"
                  />
                </Element>

                {/* Engagement row */}
                <Element
                  id="facebook-footer"
                  is={FacebookFooterSection}
                  canvas
                >
                  <ActionBarWidget
                    likes={47}
                    comments={12}
                    shares={8}
                    styleId="facebook"
                  />
                </Element>

                {/* Bottom actions */}
                <Element
                  id="facebook-actions"
                  is={FacebookActionButtonsRow}
                  canvas
                >
                  <TextWidget
                    text="👍 Like"
                    styleId="facebook"
                  />

                  <TextWidget
                    text="💬 Comment"
                    styleId="facebook"
                  />

                  <TextWidget
                    text="↗ Share"
                    styleId="facebook"
                  />
                </Element>
              </Element>
            </div>
          </div>
        </div>
      </Element>
    </div>
  )
}

FacebookTemplate.craft = {
  displayName: 'Facebook Template',
  props: {
    // `canvasComponent` is a component reference, not JSON-serializable, so it
    // never survives a save/deserialize round trip (craft.js drops function
    // props when persisting node state). Without this default, a reloaded
    // node re-renders with `canvasComponent` undefined, so its linked
    // "facebook-root" Element's `is` no longer matches the type craft.js has
    // on record for that node, and craft.js crashes trying to rebuild it.
    // ResearcherEdit still overrides this for the live, uncontrolled first
    // mount by passing `canvasComponent={ResearcherEditCanvas}` explicitly.
    canvasComponent: CanvasContainer,
  },
}

export default FacebookTemplate