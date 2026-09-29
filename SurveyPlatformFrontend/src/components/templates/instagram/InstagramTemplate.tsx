import {
  Element,
  useNode,
} from '@craftjs/core'

import CanvasContainer from '../../editor/CanvasContainer'

import {
  PostWidget,
} from '../../widgets'

import InstagramSidebar from './InstagramSidebar'
import InstagramStories from './InstagramStories'
import InstagramSuggestions from './InstagramSuggestions'

import {
  InstagramDesktopShell,
  InstagramMainFeed,
  InstagramPostList,
} from './InstagramLayout'


type InstagramTemplateProps = {
  canvasComponent?: React.ElementType
}


function InstagramTemplate({
  canvasComponent:
    CanvasComponent =
      CanvasContainer,
}: InstagramTemplateProps) {
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
        id="instagram-root"
        is={CanvasComponent}
        canvas
      >
        <Element
          id="instagram-shell"
          is={InstagramDesktopShell}
          canvas
        >
          <div className="flex h-full w-full overflow-hidden bg-white">

            <InstagramSidebar />


            <Element
              id="instagram-main-feed"
              is={InstagramMainFeed}
              canvas
            >

              <div className="mx-auto w-full max-w-[560px]">

                <InstagramStories />


                {/* All Instagram posts stay in one zero-gap Craft canvas */}

                <Element
                  id="instagram-post-list"
                  is={InstagramPostList}
                  canvas
                >

                  <PostWidget
                    styleId="instagram"
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


                  <PostWidget
                    styleId="instagram"
                    username="@alexc"
                    displayName="Alex Chen"
                    time="4h ago"
                    avatarSrc=""
                    imageSrc=""
                    caption="Another beautiful day around campus."
                    likes={23}
                    comments={4}
                    shares={2}
                  />

                </Element>

              </div>

            </Element>


            <InstagramSuggestions />

          </div>
        </Element>
      </Element>
    </div>
  )
}


InstagramTemplate.craft = {
  displayName:
    'Instagram Template',

  props: {
    canvasComponent:
      CanvasContainer,
  },
}


export default InstagramTemplate