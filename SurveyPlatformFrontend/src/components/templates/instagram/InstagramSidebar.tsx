import { useNode } from '@craftjs/core'

import InstagramIcon from './InstagramIcon'


function InstagramSidebar() {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex min-h-[760px] w-[72px] shrink-0 flex-col items-center border-r border-[#efefef] bg-white py-5"
    >
      <div className="mb-8 flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-purple-600 via-pink-500 to-orange-400 text-white shadow-sm">
        <InstagramIcon
          name="instagram"
          size={22}
        />
      </div>


      <nav className="flex flex-col items-center gap-6 text-[#262626]">

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="home"
            size={24}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="search"
            size={24}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="explore"
            size={24}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="reels"
            size={24}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="message"
            size={24}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="heart"
            size={24}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-lg">
          <InstagramIcon
            name="create"
            size={24}
          />
        </div>

      </nav>


      <div className="mt-auto flex h-9 w-9 items-center justify-center rounded-full border-2 border-pink-500 bg-white text-[9px] font-semibold text-[#262626]">
        You
      </div>

    </aside>
  )
}


InstagramSidebar.craft = {
  displayName: 'Instagram Sidebar',
}


export default InstagramSidebar