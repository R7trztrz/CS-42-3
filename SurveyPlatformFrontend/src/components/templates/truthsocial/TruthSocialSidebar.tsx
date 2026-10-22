import { useNode } from '@craftjs/core'
import TruthSocialIcon from './TruthSocialIcon'

const navItems = [
  {
    label: 'Home',
    icon: 'home' as const,
    trackingKey: 'nav-home',
    active: true,
  },
  {
    label: 'Search',
    icon: 'search' as const,
    trackingKey: 'nav-search',
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
  {
    label: 'Profile',
    icon: 'profile' as const,
    trackingKey: 'nav-profile',
  },
]

function TruthSocialSidebar() {
  const {
    id: nodeId,
    connectors: { connect },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      data-component-id={nodeId}
      data-component-type="TruthSocialSidebar"
      className="flex h-full w-[215px] shrink-0 flex-col border-r border-[#eef1f5] bg-white px-6 py-6"
    >
      <div
        data-component-id={`${nodeId}:brand`}
        data-parent-component-id={nodeId}
        data-component-type="TruthSocialBrand"
        className="mb-8 flex items-center gap-3"
      >
        <TruthSocialIcon
          name="logo"
          size={36}
        />

        <div className="leading-[1.05]">
          <div className="text-[15px] font-extrabold text-[#0d2345]">
            Truth
          </div>

          <div className="text-[15px] font-extrabold text-[#ef1f2c]">
            Social
          </div>
        </div>
      </div>

      <nav
        data-component-id={`${nodeId}:navigation`}
        data-parent-component-id={nodeId}
        data-component-type="TruthSocialNavigation"
        className="space-y-5"
      >
        {navItems.map((item) => (
          <div
            key={item.label}
            data-component-id={`${nodeId}:${item.trackingKey}`}
            data-parent-component-id={nodeId}
            data-component-type="NavigationItem"
            className={`flex items-center gap-4 ${
              item.active
                ? 'font-semibold text-[#ef1f2c]'
                : 'text-[#0d2345]'
            }`}
          >
            <TruthSocialIcon
              name={item.icon}
              size={21}
            />

            <span className="text-[15px]">
              {item.label}
            </span>
          </div>
        ))}
      </nav>

      <button
        type="button"
        data-component-id={`${nodeId}:compose`}
        data-parent-component-id={nodeId}
        data-component-type="TruthSocialComposeButton"
        className="mt-auto h-[44px] w-full rounded-full bg-[#ef1f2c] text-[15px] font-semibold text-white"
      >
        Compose
      </button>
    </aside>
  )
}

TruthSocialSidebar.craft = {
  displayName: 'Truth Social Sidebar',
}

export default TruthSocialSidebar