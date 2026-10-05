import { Element, useNode } from '@craftjs/core'
import CanvasContainer from '../../editor/CanvasContainer'
import { PostWidget } from '../../widgets'
import TruthSocialSidebar from './TruthSocialSidebar'
import TruthSocialComposer from './TruthSocialComposer'
import TruthSocialRightSidebar from './TruthSocialRightSidebar'
import { TruthSocialDesktopShell, TruthSocialMainFeed, TruthSocialPostList } from './TruthSocialLayout'

type TruthSocialTemplateProps = { canvasComponent?: React.ElementType }

function TruthSocialTemplate({ canvasComponent: CanvasComponent = CanvasContainer }: TruthSocialTemplateProps) {
  const { connectors: { connect } } = useNode()
  return (
    <div ref={(ref) => { if (ref) connect(ref) }} className="w-full overflow-x-auto">
      <Element id="truth-social-root" is={CanvasComponent} canvas>
        <Element id="truth-social-shell" is={TruthSocialDesktopShell} canvas>
          <div className="flex h-full w-full bg-white">
            <TruthSocialSidebar />

            <Element id="truth-social-main-feed" is={TruthSocialMainFeed} canvas>
              <div className="sticky top-0 z-20 grid h-[58px] grid-cols-[1fr_1fr_0.85fr] border-b border-[#e6eaf0] bg-white/95 backdrop-blur">
                <div className="relative flex items-center justify-center">
                  <span className="text-[14px] font-semibold text-[#ef1f2c]">For You</span>
                  <span className="absolute bottom-0 h-[3px] w-[48px] rounded-full bg-[#ef1f2c]" />
                </div>
                <div className="flex items-center justify-center text-[14px] font-semibold text-[#687386]">Following</div>
                <div className="flex items-center justify-center text-[13px] font-medium text-[#9aa4b3]">Groups</div>
              </div>

              <TruthSocialComposer />

              <Element id="truth-social-post-list" is={TruthSocialPostList} canvas>
                <PostWidget
                  styleId="truth-social"
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
                  styleId="truth-social"
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

            <TruthSocialRightSidebar />
            <div className="min-w-0 flex-1 bg-white" />
          </div>
        </Element>
      </Element>
    </div>
  )
}

TruthSocialTemplate.craft = { displayName: 'Truth Social Template', props: { canvasComponent: CanvasContainer } }

export default TruthSocialTemplate
