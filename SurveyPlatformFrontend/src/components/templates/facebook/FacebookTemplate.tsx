import {
  Element,
  useNode,
} from '@craftjs/core'

import CanvasContainer from '../../editor/CanvasContainer'

import {
  PostWidget,
} from '../../widgets'

import FacebookTopBar from './FacebookTopBar'
import FacebookStories from './FacebookStories'
import FacebookCreatePost from './FacebookCreatePost'
import FacebookLeftSidebar from './FacebookLeftSidebar'
import FacebookRightSidebar from './FacebookRightSidebar'

import {
  FacebookDesktopShell,
  FacebookMainFeed,
} from './FacebookLayout'


type FacebookTemplateProps = {
  canvasComponent?: React.ElementType
}


function FacebookTemplate({
  canvasComponent:
    CanvasComponent =
      CanvasContainer,
}: FacebookTemplateProps) {
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
      className="w-full"
    >
      <Element
        id="facebook-root"
        is={CanvasComponent}
        canvas
      >
        <Element
          id="facebook-shell"
          is={FacebookDesktopShell}
          canvas
        >
          <div className="flex h-full w-full flex-col overflow-hidden bg-[#f0f2f5]">

            {/* Top navigation */}
            <FacebookTopBar />


            <div className="flex min-h-0 flex-1 overflow-hidden">

              {/* Left sidebar */}
              <div className="w-[250px] shrink-0">
                <FacebookLeftSidebar />
              </div>


              {/* ==================================================
                  MAIN FEED

                  IMPORTANT:
                  Stories, composer and ALL posts are direct
                  children of the same Craft canvas.

                  Newly dragged PostWidget nodes therefore land
                  at the same level as the default posts.
                 ================================================== */}

              <Element
                id="facebook-main-feed"
                is={FacebookMainFeed}
                canvas
              >

                <FacebookStories />


                <FacebookCreatePost />


                {/* Default Facebook Post 1 */}

                <PostWidget
                  styleId="facebook"
                  username="@emmaw"
                  displayName="Emma Wilson"
                  time="2h ago"
                  avatarSrc=""
                  imageSrc=""
                  caption="Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂"
                  likes={47}
                  comments={12}
                  shares={8}
                />


                {/* Default Facebook Post 2 */}

                <PostWidget
                  styleId="facebook"
                  username="@alexc"
                  displayName="Alex Chen"
                  time="4h ago"
                  avatarSrc=""
                  imageSrc=""
                  caption="Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign"
                  likes={31}
                  comments={6}
                  shares={3}
                />

              </Element>


              {/* Right sidebar */}
              <div className="w-[250px] shrink-0">
                <FacebookRightSidebar />
              </div>

            </div>

          </div>
        </Element>
      </Element>
    </div>
  )
}


FacebookTemplate.craft = {
  displayName:
    'Facebook Template',

  props: {
    canvasComponent:
      CanvasContainer,
  },
}


export default FacebookTemplate