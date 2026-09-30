import { useNode } from '@craftjs/core'

import TikTokIcon from './TikTokIcon'


const navigation = [
  {
    label: 'For You',
    icon: 'home' as const,
    active: true,
  },
  {
    label: 'Following',
    icon: 'friends' as const,
  },
  {
    label: 'Explore',
    icon: 'explore' as const,
  },
  {
    label: 'Messages',
    icon: 'message' as const,
  },
  {
    label: 'Search',
    icon: 'search' as const,
  },
]


function TikTokSidebar() {
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
      className="flex h-full w-[185px] shrink-0 flex-col border-r border-[#202020] bg-[#0f0f0f] px-3 py-5 text-white"
    >
      {/* Logo */}

      <div className="px-2">

        <div className="text-[25px] font-black tracking-[-1.5px] text-white">
          Tik
          <span className="relative">
            T
          </span>
          ok
        </div>

      </div>


      {/* Navigation */}

      <nav className="mt-8 space-y-2">

        {navigation.map(
          ({
            label,
            icon,
            active,
          }) => (
            <div
              key={label}
              className={`flex items-center gap-3 rounded-lg px-3 py-3 ${
                active
                  ? 'bg-[#262626] text-white'
                  : 'text-[#8a8d98] hover:bg-[#1f1f1f] hover:text-white'
              }`}
            >
              <TikTokIcon
                name={icon}
                size={21}
              />

              <span
                className={`text-[14px] ${
                  active
                    ? 'font-semibold'
                    : 'font-medium'
                }`}
              >
                {label}
              </span>
            </div>
          ),
        )}

      </nav>


      {/* Bottom */}

      <div className="mt-auto">

        <button
          type="button"
          className="flex w-full items-center justify-center gap-1 rounded-md bg-[#fe2c55] px-4 py-3 text-[14px] font-semibold text-white hover:bg-[#e7294d]"
        >
          <TikTokIcon
            name="plus"
            size={17}
          />

          Upload
        </button>


        <div className="mt-4 flex items-center gap-3 px-1">

          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#272727] text-[10px] font-semibold text-white">
            You
          </div>


          <div className="min-w-0">

            <p className="truncate text-[12px] font-semibold text-white">
              yourprofile
            </p>

            <p className="truncate text-[10px] text-[#777b87]">
              @youraccount
            </p>

          </div>

        </div>

      </div>
    </aside>
  )
}


TikTokSidebar.craft = {
  displayName: 'TikTok Sidebar',
}


export default TikTokSidebar