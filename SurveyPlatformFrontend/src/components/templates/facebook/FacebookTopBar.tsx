import { useNode } from '@craftjs/core'

import FacebookIcon from './FacebookIcon'


function FacebookTopBar() {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <header
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex h-[58px] w-full items-center justify-between border-b border-[#d8dadf] bg-white px-4"
    >
      {/* Left */}
      <div className="flex min-w-[280px] items-center gap-3">

        <FacebookIcon
          name="facebook"
          size={40}
        />


        <div className="flex h-10 w-[230px] items-center gap-2 rounded-full bg-[#f0f2f5] px-4 text-[#65676b]">

          <FacebookIcon
            name="search"
            size={18}
          />

          <span className="text-[14px]">
            Search Facebook
          </span>

        </div>

      </div>


      {/* Center nav */}
      <nav className="flex h-full items-center gap-1">

        <div className="flex h-full w-[74px] items-center justify-center border-b-[3px] border-[#1877f2] text-[#1877f2]">
          <FacebookIcon
            name="home"
            size={24}
          />
        </div>

        <div className="flex h-full w-[74px] items-center justify-center text-[#65676b]">
          <FacebookIcon
            name="friends"
            size={24}
          />
        </div>

        <div className="flex h-full w-[74px] items-center justify-center text-[#65676b]">
          <FacebookIcon
            name="video"
            size={24}
          />
        </div>

        <div className="flex h-full w-[74px] items-center justify-center text-[#65676b]">
          <FacebookIcon
            name="groups"
            size={24}
          />
        </div>

        <div className="flex h-full w-[74px] items-center justify-center text-[#65676b]">
          <FacebookIcon
            name="game"
            size={24}
          />
        </div>

      </nav>


      {/* Right */}
      <div className="flex min-w-[280px] justify-end gap-2">

        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#e4e6eb] text-[#050505]">
          <FacebookIcon
            name="message"
            size={20}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#e4e6eb] text-[#050505]">
          <FacebookIcon
            name="bell"
            size={20}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#e4e6eb] text-[#050505]">
          <FacebookIcon
            name="more"
            size={20}
          />
        </div>

        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#5b49ff] text-[10px] font-bold text-white">
          You
        </div>

      </div>
    </header>
  )
}


FacebookTopBar.craft = {
  displayName:
    'Facebook Top Bar',
}


export default FacebookTopBar