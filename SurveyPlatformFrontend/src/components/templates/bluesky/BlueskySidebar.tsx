import { useNode } from '@craftjs/core'
import BlueskyIcon from './BlueskyIcon'

const navItems = [
  { label: 'Home', icon: 'home' as const, active: true },
  { label: 'Search', icon: 'search' as const },
  { label: 'Notifications', icon: 'notifications' as const },
  { label: 'Chat', icon: 'chat' as const },
  { label: 'Feeds', icon: 'feeds' as const },
  { label: 'Lists', icon: 'lists' as const },
  { label: 'Profile', icon: 'profile' as const },
  { label: 'Settings', icon: 'settings' as const },
]

function BlueskySidebar() {
  const { connectors: { connect } } = useNode()
  return (
    <aside ref={(ref) => { if (ref) connect(ref) }} className="flex h-full w-[215px] shrink-0 flex-col border-r border-[#eef2f7] bg-white px-6 py-6">
      <div className="mb-8 flex items-center gap-3 text-[#111827]">
        <span className="text-[#1185fe]"><BlueskyIcon name="logo" size={34} /></span>
        <span className="text-[19px] font-bold">Bluesky</span>
      </div>
      <nav className="space-y-5">
        {navItems.map((item) => (
          <div key={item.label} className={`flex items-center gap-4 ${item.active ? 'font-semibold text-[#1185fe]' : 'text-[#334155]'}`}>
            <BlueskyIcon name={item.icon} size={21} />
            <span className="text-[15px]">{item.label}</span>
          </div>
        ))}
      </nav>
      <button type="button" className="mt-auto h-[44px] w-full rounded-full bg-[#1185fe] text-[15px] font-semibold text-white">New Post</button>
    </aside>
  )
}
BlueskySidebar.craft = { displayName: 'Bluesky Sidebar' }
export default BlueskySidebar
