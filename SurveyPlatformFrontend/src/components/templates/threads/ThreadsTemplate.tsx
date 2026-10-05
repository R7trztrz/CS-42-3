import { Element, useNode } from '@craftjs/core'
import CanvasContainer from '../../editor/CanvasContainer'
import { PostWidget } from '../../widgets'
import ThreadsSidebar from './ThreadsSidebar'
import ThreadsComposer from './ThreadsComposer'
import { ThreadsDesktopShell, ThreadsMainFeed, ThreadsPostList } from './ThreadsLayout'

type ThreadsTemplateProps = {
  canvasComponent?: React.ElementType
}

function ThreadsTemplate({
  canvasComponent: CanvasComponent = CanvasContainer,
}: ThreadsTemplateProps) {
  const { connectors: { connect } } = useNode()

  return (
    <div ref={(ref) => { if (ref) connect(ref) }} className="w-full overflow-x-auto">
      <Element id="threads-root" is={CanvasComponent} canvas>
        <Element id="threads-shell" is={ThreadsDesktopShell} canvas>
          <div className="flex h-full w-full bg-white">
            <ThreadsSidebar />
            <div className="flex min-w-0 flex-1 justify-center">
              <Element id="threads-main-feed" is={ThreadsMainFeed} canvas>
                <div className="sticky top-0 z-20 grid h-[62px] grid-cols-2 border-b border-[#eeeeee] bg-white/95 backdrop-blur">
                  <div className="relative flex items-center justify-center">
                    <span className="text-[15px] font-semibold text-[#111111]">For you</span>
                    <span className="absolute bottom-0 h-[3px] w-[54px] rounded-full bg-[#111111]" />
                  </div>
                  <div className="flex items-center justify-center text-[15px] font-medium text-[#9ca3af]">Following</div>
                </div>

                <ThreadsComposer />

                <Element id="threads-post-list" is={ThreadsPostList} canvas>
                  <PostWidget
                    styleId="threads"
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
                    styleId="threads"
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
                  <PostWidget
                    styleId="threads"
                    displayName="Jordan Lee"
                    username="@jordanl"
                    time="6h ago"
                    avatarSrc=""
                    imageSrc=""
                    caption="Tried the new coffee shop near the university — incredible atmosphere for deep work. Highly recommend to fellow students! ☕"
                    likes={62}
                    comments={9}
                    shares={5}
                  />
                </Element>
              </Element>
            </div>
          </div>
        </Element>
      </Element>
    </div>
  )
}

ThreadsTemplate.craft = {
  displayName: 'Threads Template',
  props: { canvasComponent: CanvasContainer },
}

export default ThreadsTemplate
