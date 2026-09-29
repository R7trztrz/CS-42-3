import {
  Element,
  useNode,
} from '@craftjs/core'

import CanvasContainer from '../../editor/CanvasContainer'

import {
  PostWidget,
} from '../../widgets'

import TikTokSidebar from './TikTokSidebar'

import {
  TikTokDesktopShell,
  TikTokFeedStage,
  TikTokMainArea,
} from './TikTokLayout'


type TikTokTemplateProps = {
  canvasComponent?: React.ElementType
}


function TikTokTemplate({
  canvasComponent:
    CanvasComponent =
      CanvasContainer,
}: TikTokTemplateProps) {
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
        id="tiktok-root"
        is={CanvasComponent}
        canvas
      >
        <Element
          id="tiktok-shell"
          is={TikTokDesktopShell}
          canvas
        >
          <div className="flex h-full w-full overflow-hidden">

            {/* Sidebar */}
            <TikTokSidebar />


            {/* Main area */}
            <Element
              id="tiktok-main"
              is={TikTokMainArea}
              canvas
            >

              {/* Top tabs */}
              <div className="flex h-[72px] shrink-0 items-center justify-center border-b border-[#1f1f1f] bg-[#0f0f0f]">

                <div className="flex h-full items-center gap-9">

                  <span
                    className="flex h-full items-center text-[14px] font-semibold"
                    style={{
                      color:
                        '#777777',
                    }}
                  >
                    Following
                  </span>


                  <span
                    className="flex h-full items-center border-b-2 border-white px-1 text-[14px] font-semibold"
                    style={{
                      color:
                        '#ffffff',
                    }}
                  >
                    For You
                  </span>

                </div>

              </div>


              {/* Editable feed */}
              <Element
                id="tiktok-feed"
                is={TikTokFeedStage}
                canvas
              >
                <PostWidget
                  styleId="tiktok"
                  username="@emmaw"
                  displayName="Emma Wilson"
                  time="2h ago"
                  avatarSrc=""
                  imageSrc=""
                  caption="Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂"
                  likes={47}
                  comments={12}
                  saves={5}
                  shares={8}
                  sound="Original Sound · Emma Wilson"
                />

              </Element>

            </Element>

          </div>
        </Element>
      </Element>
    </div>
  )
}


TikTokTemplate.craft = {
  displayName:
    'TikTok Template',

  props: {
    canvasComponent:
      CanvasContainer,
  },
}


export default TikTokTemplate