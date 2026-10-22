import { useNode } from '@craftjs/core'
import BlueskyIcon from './BlueskyIcon'

const navItems = [
  { label: 'Home', icon: 'home' as const, trackingKey: 'nav-home', active: true },
  { label: 'Search', icon: 'search' as const, trackingKey: 'nav-search' },
  { label: 'Notifications', icon: 'notifications' as const, trackingKey: 'nav-notifications' },
  { label: 'Chat', icon: 'chat' as const, trackingKey: 'nav-chat' },
  { label: 'Feeds', icon: 'feeds' as const, trackingKey: 'nav-feeds' },
  { label: 'Lists', icon: 'lists' as const, trackingKey: 'nav-lists' },
  { label: 'Profile', icon: 'profile' as const, trackingKey: 'nav-profile' },
  { label: 'Settings', icon: 'settings' as const, trackingKey: 'nav-settings' },
]

function BlueskySidebar() {
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
      data-component-type="BlueskySidebar"
      className="flex h-full w-[215px] shrink-0 flex-col border-r border-[#eef2f7] bg-white px-6 py-6"
    >
      <div
        data-component-id={`${nodeId}:brand`}
        data-parent-component-id={nodeId}
        data-component-type="BlueskyBrand"
        className="mb-8 flex items-center gap-3 text-[#111827]"
      >
        <span className="text-[#1185fe]">
          <BlueskyIcon name="logo" size={34} />
        </span>

        <span className="text-[19px] font-bold">
          Bluesky
        </span>
      </div>

      <nav
        data-component-id={`${nodeId}:navigation`}
        data-parent-component-id={nodeId}
        data-component-type="BlueskyNavigation"
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
                ? 'font-semibold text-[#1185fe]'
                : 'text-[#334155]'
            }`}
          >
            <BlueskyIcon
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
        data-component-id={`${nodeId}:new-post`}
        data-parent-component-id={nodeId}
        data-component-type="BlueskyNewPost"
        className="mt-auto h-[44px] w-full rounded-full bg-[#1185fe] text-[15px] font-semibold text-white"
      >
        New Post
      </button>
    </aside>
  )
}

BlueskySidebar.craft = {
  displayName: 'Bluesky Sidebar',
}

export default BlueskySidebar