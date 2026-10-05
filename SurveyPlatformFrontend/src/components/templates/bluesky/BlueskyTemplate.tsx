import { Element, useNode } from '@craftjs/core'
import CanvasContainer from '../../editor/CanvasContainer'
import { PostWidget } from '../../widgets'
import BlueskySidebar from './BlueskySidebar'
import BlueskyComposer from './BlueskyComposer'
import BlueskyRightSidebar from './BlueskyRightSidebar'
import { BlueskyDesktopShell, BlueskyMainFeed, BlueskyPostList } from './BlueskyLayout'

type BlueskyTemplateProps = { canvasComponent?: React.ElementType }

function BlueskyTemplate({ canvasComponent: CanvasComponent = CanvasContainer }: BlueskyTemplateProps) {
  const { connectors: { connect } } = useNode()

  return (
    <div ref={(ref) => { if (ref) connect(ref) }} className="w-full overflow-x-auto">
      <Element id="bluesky-root" is={CanvasComponent} canvas>
        <Element id="bluesky-shell" is={BlueskyDesktopShell} canvas>
          <div className="flex h-full w-full bg-white">
            <BlueskySidebar />
            <Element id="bluesky-main-feed" is={BlueskyMainFeed} canvas>
              <div className="sticky top-0 z-20 grid h-[58px] grid-cols-2 border-b border-[#e6edf5] bg-white/95 backdrop-blur">
                <div className="relative flex items-center justify-center">
                  <span className="text-[14px] font-semibold text-[#1185fe]">Following</span>
                  <span className="absolute bottom-0 h-[3px] w-[48px] rounded-full bg-[#1185fe]" />
                </div>
                <div className="flex items-center justify-center text-[14px] font-semibold text-[#64748b]">Discover</div>
              </div>

              <BlueskyComposer />

              <Element id="bluesky-post-list" is={BlueskyPostList} canvas>
                <PostWidget
                  styleId="bluesky"
                  displayName="Emma Wilson"
                  username="@emmaw.bsky.social"
                  time="2h ago"
                  avatarSrc=""
                  imageSrc=""
                  caption="Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂"
                  likes={47}
                  comments={12}
                  shares={8}
                />
                <PostWidget
                  styleId="bluesky"
                  displayName="Alex Chen"
                  username="@alexc.bsky.social"
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

            <BlueskyRightSidebar />
            <div className="min-w-0 flex-1 bg-white" />
          </div>
        </Element>
      </Element>
    </div>
  )
}

BlueskyTemplate.craft = { displayName: 'Bluesky Template', props: { canvasComponent: CanvasContainer } }

export default BlueskyTemplate
