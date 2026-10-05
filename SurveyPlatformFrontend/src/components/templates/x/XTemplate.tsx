import {
  Element,
  useNode,
} from '@craftjs/core'

import CanvasContainer from '../../editor/CanvasContainer'

import {
  PostWidget,
} from '../../widgets'

import XSidebar from './XSidebar'
import XRightSidebar from './XRightSidebar'
import XComposer from './XComposer'

import {
  XDesktopShell,
  XMainFeed,
  XPostList,
} from './XLayout'

type XTemplateProps = {
  canvasComponent?: React.ElementType
}

function XTemplate({
  canvasComponent:
    CanvasComponent =
      CanvasContainer,
}: XTemplateProps) {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="w-full overflow-x-auto"
    >
      <Element
        id="x-root"
        is={CanvasComponent}
        canvas
      >
        <Element
          id="x-shell"
          is={XDesktopShell}
          canvas
        >
          <div className="flex h-full w-full bg-black text-white">

            <XSidebar />

            <Element
              id="x-main-feed"
              is={XMainFeed}
              canvas
            >
              <div className="sticky top-0 z-20 grid h-[64px] shrink-0 grid-cols-2 border-b border-[#2f3336] bg-black">

                <div className="relative flex h-full items-center justify-center">
                  <span className="text-[15px] font-semibold text-[#e7e9ea]">
                    For You
                  </span>

                  <span className="absolute bottom-0 h-[4px] w-[64px] rounded-full bg-[#1d9bf0]" />
                </div>

                <div className="flex h-full items-center justify-center">
                  <span className="text-[15px] font-medium text-[#71767b]">
                    Following
                  </span>
                </div>

              </div>

              <XComposer />

              <Element
                id="x-post-list"
                is={XPostList}
                canvas
              >
                <PostWidget
                  styleId="x"
                  displayName="Emma Wilson"
                  username="@emmaw"
                  time="2h ago"
                  avatarSrc=""
                  imageSrc=""
                  caption="Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂"
                  likes={47}
                  comments={12}
                  shares={8}
                />

                <PostWidget
                  styleId="x"
                  displayName="Alex Chen"
                  username="@alexc"
                  time="4h ago"
                  avatarSrc=""
                  imageSrc=""
                  caption="Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign"
                  likes={83}
                  comments={21}
                  shares={34}
                />
              </Element>
            </Element>

            <XRightSidebar />

          </div>
        </Element>
      </Element>
    </div>
  )
}

XTemplate.craft = {
  displayName:
    'X Template',

  props: {
    canvasComponent:
      CanvasContainer,
  },
}

export default XTemplate