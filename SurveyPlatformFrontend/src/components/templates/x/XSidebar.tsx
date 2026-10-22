import {
  useNode,
} from '@craftjs/core'

import XIcon from './XIcon'

const navigationItems = [
  {
    label: 'Home',
    icon: 'home' as const,
    trackingKey: 'nav-home',
  },
  {
    label: 'Explore',
    icon: 'search' as const,
    trackingKey: 'nav-explore',
  },
  {
    label: 'Notifications',
    icon: 'notifications' as const,
    trackingKey: 'nav-notifications',
  },
  {
    label: 'Messages',
    icon: 'messages' as const,
    trackingKey: 'nav-messages',
  },
  {
    label: 'Bookmarks',
    icon: 'bookmarks' as const,
    trackingKey: 'nav-bookmarks',
  },
]

function XSidebar() {
  const {
    id: nodeId,
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
      data-component-id={nodeId}
      data-component-type="XSidebar"
      className="flex h-full w-[280px] shrink-0 flex-col justify-between bg-black px-7 py-6 text-white"
    >
      <div>
        <div
          data-component-id={`${nodeId}:logo`}
          data-parent-component-id={nodeId}
          data-component-type="XLogo"
          className="mb-8"
        >
          <XIcon
            name="logo"
            size={36}
          />
        </div>

        <nav
          data-component-id={`${nodeId}:navigation`}
          data-parent-component-id={nodeId}
          data-component-type="XNavigation"
          className="space-y-5"
        >
          {navigationItems.map(
            (item) => (
              <div
                key={item.label}
                data-component-id={`${nodeId}:${item.trackingKey}`}
                data-parent-component-id={nodeId}
                data-component-type="NavigationItem"
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

          <div
            data-component-id={`${nodeId}:nav-profile`}
            data-parent-component-id={nodeId}
            data-component-type="NavigationItem"
            className="flex items-center gap-5"
          >
            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-[#26364a] text-[8px] font-bold">
              You
            </div>

            <span className="text-[20px]">
              Profile
            </span>
          </div>
        </nav>
      </div>

      <div
        data-component-id={`${nodeId}:account-area`}
        data-parent-component-id={nodeId}
        data-component-type="XAccountArea"
      >
        <button
          type="button"
          data-component-id={`${nodeId}:post-button`}
          data-parent-component-id={nodeId}
          data-component-type="XPostButton"
          className="mb-7 h-[58px] w-full rounded-full bg-white text-[18px] font-bold text-black"
        >
          Post
        </button>

        <div
          data-component-id={`${nodeId}:profile`}
          data-parent-component-id={nodeId}
          data-component-type="XProfile"
          className="flex items-center gap-3"
        >
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