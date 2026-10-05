import { useNode } from '@craftjs/core'
import TruthSocialIcon from './TruthSocialIcon'

const navItems = [
  { label: 'Home', icon: 'home' as const, active: true },
  { label: 'Search', icon: 'search' as const },
  { label: 'Notifications', icon: 'notifications' as const },
  { label: 'Messages', icon: 'messages' as const },
  { label: 'Bookmarks', icon: 'bookmarks' as const },
  { label: 'Profile', icon: 'profile' as const },
]

function TruthSocialSidebar() {
  const { connectors: { connect } } = useNode()
  return (
    <aside ref={(ref) => { if (ref) connect(ref) }} className="flex h-full w-[215px] shrink-0 flex-col border-r border-[#eef1f5] bg-white px-6 py-6">
      <div className="mb-8 flex items-center gap-3">
        <TruthSocialIcon name="logo" size={36} />
        <div className="leading-[1.05]">
          <div className="text-[15px] font-extrabold text-[#0d2345]">Truth</div>
          <div className="text-[15px] font-extrabold text-[#ef1f2c]">Social</div>
        </div>
      </div>

      <nav className="space-y-5">
        {navItems.map((item) => (
          <div key={item.label} className={`flex items-center gap-4 ${item.active ? 'font-semibold text-[#ef1f2c]' : 'text-[#0d2345]'}`}>
            <TruthSocialIcon name={item.icon} size={21} />
            <span className="text-[15px]">{item.label}</span>
          </div>
        ))}
      </nav>

      <button type="button" className="mt-auto h-[44px] w-full rounded-full bg-[#ef1f2c] text-[15px] font-semibold text-white">Compose</button>
    </aside>
  )
}

TruthSocialSidebar.craft = { displayName: 'Truth Social Sidebar' }

export default TruthSocialSidebar
