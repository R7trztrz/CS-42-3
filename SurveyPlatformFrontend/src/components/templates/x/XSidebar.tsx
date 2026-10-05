import {
  useNode,
} from '@craftjs/core'

import XIcon from './XIcon'

const navigationItems = [
  {
    label: 'Home',
    icon: 'home' as const,
  },
  {
    label: 'Explore',
    icon: 'search' as const,
  },
  {
    label: 'Notifications',
    icon: 'notifications' as const,
  },
  {
    label: 'Messages',
    icon: 'messages' as const,
  },
  {
    label: 'Bookmarks',
    icon: 'bookmarks' as const,
  },
]

function XSidebar() {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex h-full w-[280px] shrink-0 flex-col justify-between bg-black px-7 py-6 text-white"
    >
      <div>
        <div className="mb-8">
          <XIcon
            name="logo"
            size={36}
          />
        </div>

        <nav className="space-y-5">
          {navigationItems.map(
            (item) => (
              <div
                key={item.label}
                className="flex items-center gap-5"
              >
                <XIcon
                  name={item.icon}
                  size={25}
                />

                <span
                  className={`text-[20px] ${
                    item.label === 'Home'
                      ? 'font-bold'
                      : 'font-normal'
                  }`}
                >
                  {item.label}
                </span>
              </div>
            ),
          )}

          <div className="flex items-center gap-5">
            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-[#26364a] text-[8px] font-bold">
              You
            </div>

            <span className="text-[20px]">
              Profile
            </span>
          </div>
        </nav>
      </div>

      <div>
        <button
          type="button"
          className="mb-7 h-[58px] w-full rounded-full bg-white text-[18px] font-bold text-black"
        >
          Post
        </button>

        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#26364a] text-[9px] font-bold">
            You
          </div>

          <div className="min-w-0 flex-1">
            <p className="truncate text-[14px] font-bold">
              Your Account
            </p>

            <p className="truncate text-[12px] text-[#71767b]">
              @youraccount
            </p>
          </div>

          <div className="text-[#71767b]">
            ···
          </div>
        </div>
      </div>
    </aside>
  )
}

XSidebar.craft = {
  displayName:
    'X Sidebar',
}

export default XSidebar