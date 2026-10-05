import { useNode } from '@craftjs/core'

const feeds = [
  ['Discover', 'Trending posts from across Bluesky'],
  ['Popular With Friends', 'Posts your network is engaging with'],
  ['Following', 'Posts from accounts you follow'],
  ['Tech & Science', 'Custom feed · 12k subscribers'],
]

const people = [
  ['EW', 'Emma Wilson', '@emmaw.bsky.social', '#5146e5'],
  ['AC', 'Alex Chen', '@alexc.bsky.social', '#11a4c8'],
  ['JL', 'Jordan Lee', '@jordanl.bsky.social', '#00a87f'],
  ['MT', 'Mia Taylor', '@miat.bsky.social', '#e67e00'],
]

function BlueskyRightSidebar() {
  const { connectors: { connect } } = useNode()

  return (
    <aside ref={(ref) => { if (ref) connect(ref) }} className="h-full w-[300px] shrink-0 overflow-y-auto border-l border-[#eef2f7] bg-white px-5 py-5">
      <h2 className="text-[16px] font-bold text-[#111827]">Feeds</h2>
      <div className="mt-4 space-y-4">
        {feeds.map(([name, meta], index) => (
          <div key={name} className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#eef4ff] text-[16px]">{index === 0 ? '✨' : index === 1 ? '👥' : index === 2 ? '🦋' : '🧪'}</div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[14px] font-semibold text-[#1f2937]">{name}</p>
              <p className="truncate text-[11px] text-[#94a3b8]">{meta}</p>
            </div>
            <button type="button" className="text-[12px] font-semibold text-[#1185fe]">Pin</button>
          </div>
        ))}
      </div>

      <div className="my-6 border-t border-[#eef2f7]" />

      <h2 className="text-[16px] font-bold text-[#111827]">Suggested for you</h2>
      <div className="mt-4 space-y-4">
        {people.map(([initials, name, handle, color]) => (
          <div key={handle} className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[11px] font-bold text-white" style={{ background: color }}>{initials}</div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[14px] font-semibold text-[#1f2937]">{name}</p>
              <p className="truncate text-[11px] text-[#94a3b8]">{handle}</p>
            </div>
            <button type="button" className="rounded-full border border-[#7cc4ff] px-3 py-1 text-[12px] font-semibold text-[#1185fe]">Follow</button>
          </div>
        ))}
      </div>
    </aside>
  )
}

BlueskyRightSidebar.craft = { displayName: 'Bluesky Right Sidebar' }

export default BlueskyRightSidebar
