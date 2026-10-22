import { useNode } from '@craftjs/core'
import ThreadsIcon from './ThreadsIcon'

const items = [
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
    label: 'Create',
    icon: 'create' as const,
    trackingKey: 'nav-create',
  },
  {
    label: 'Notifications',
    icon: 'notifications' as const,
    trackingKey: 'nav-notifications',
  },
]

function ThreadsSidebar() {
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
      data-component-type="ThreadsSidebar"
      className="flex h-full w-[72px] shrink-0 flex-col items-center border-r border-[#eeeeee] bg-white py-7 text-[#0a0a0a]"
    >
      <div
        data-component-id={`${nodeId}:logo`}
        data-parent-component-id={nodeId}
        data-component-type="ThreadsLogo"
        className="mb-10"
      >
        <ThreadsIcon name="logo" size={30} />
      </div>

      <nav
        data-component-id={`${nodeId}:navigation`}
        data-parent-component-id={nodeId}
        data-component-type="ThreadsNavigation"
        className="flex flex-col items-center gap-7"
      >
        {items.map((item) => (
          <div
            key={item.label}
            title={item.label}
            data-component-id={`${nodeId}:${item.trackingKey}`}
            data-parent-component-id={nodeId}
            data-component-type="NavigationItem"
            className={
              item.active
                ? 'text-[#111111]'
                : 'text-[#9ca3af]'
            }
          >
            <ThreadsIcon
              name={item.icon}
              size={27}
            />
          </div>
        ))}

        <div
          data-component-id={`${nodeId}:profile`}
          data-parent-component-id={nodeId}
          data-component-type="ThreadsProfile"
          className="flex h-8 w-8 items-center justify-center rounded-full bg-[#eef0f3] text-[10px] font-bold text-[#5f6570]"
        >
          You
        </div>
      </nav>
    </aside>
  )
}

ThreadsSidebar.craft = {
  displayName: 'Threads Sidebar',
}

export default ThreadsSidebar