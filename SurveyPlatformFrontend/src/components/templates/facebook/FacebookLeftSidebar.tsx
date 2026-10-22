import { useNode } from '@craftjs/core'
import FacebookIcon from './FacebookIcon'

const shortcuts = [
  { label: 'Research Group' },
  { label: 'Campus Hub' },
  { label: 'Tech Society' },
]

function SidebarItem({
  icon,
  label,
  nodeId,
  trackingKey,
}: {
  icon: 'home' | 'friends' | 'groups' | 'video' | 'saved'
  label: string
  nodeId: string
  trackingKey: string
}) {
  return (
    <div
      data-component-id={`${nodeId}:${trackingKey}`}
      data-parent-component-id={nodeId}
      data-component-type="NavigationItem"
      className="flex items-center gap-3 rounded-lg px-2 py-2 text-[#050505] hover:bg-[#e4e6eb]"
    >
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#e4e6eb] text-[#050505]">
        <FacebookIcon name={icon} size={21} />
      </div>
      <span className="text-[14px] font-semibold">{label}</span>
    </div>
  )
}

function FacebookLeftSidebar() {
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
      data-component-type="FacebookLeftSidebar"
      className="h-full w-[230px] shrink-0 overflow-y-auto bg-[#f0f2f5] px-3 py-4"
    >
      <div
        data-component-id={`${nodeId}:profile`}
        data-parent-component-id={nodeId}
        data-component-type="FacebookProfile"
        className="mb-2 flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-[#e4e6eb]"
      >
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#5b49ff] text-[11px] font-bold text-white">
          You
        </div>
        <span className="text-[14px] font-semibold text-[#050505]">
          Your Profile
        </span>
      </div>

      <div
        data-component-id={`${nodeId}:navigation`}
        data-parent-component-id={nodeId}
        data-component-type="FacebookLeftNavigation"
        className="space-y-1"
      >
        <SidebarItem icon="home" label="Home" nodeId={nodeId} trackingKey="nav-home" />
        <SidebarItem icon="friends" label="Friends" nodeId={nodeId} trackingKey="nav-friends" />
        <SidebarItem icon="groups" label="Groups" nodeId={nodeId} trackingKey="nav-groups" />
        <SidebarItem icon="video" label="Video" nodeId={nodeId} trackingKey="nav-video" />
        <SidebarItem icon="saved" label="Saved" nodeId={nodeId} trackingKey="nav-saved" />
      </div>

      <div className="my-4 h-px bg-[#ced0d4]" />

      <div
        data-component-id={`${nodeId}:shortcuts`}
        data-parent-component-id={nodeId}
        data-component-type="FacebookShortcuts"
      >
        <p className="mb-2 px-2 text-[13px] font-semibold text-[#65676b]">
          Your shortcuts
        </p>

        <div className="space-y-1">
          {shortcuts.map(({ label }, index) => (
            <div
              key={label}
              data-component-id={`${nodeId}:shortcut-${index}`}
              data-parent-component-id={nodeId}
              data-component-type="ShortcutItem"
              className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-[#e4e6eb]"
            >
              <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-[#dbe7ff] to-[#b9ccff]" />
              <span className="text-[14px] font-medium text-[#050505]">
                {label}
              </span>
            </div>
          ))}
        </div>
      </div>
    </aside>
  )
}

FacebookLeftSidebar.craft = {
  displayName: 'Facebook Left Sidebar',
}

export default FacebookLeftSidebar