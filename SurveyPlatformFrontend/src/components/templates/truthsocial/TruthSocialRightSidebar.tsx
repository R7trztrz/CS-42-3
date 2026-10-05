import { useNode } from '@craftjs/core'

const trends = [
  ['Technology', '#TechInSociety', '9,800 truths'],
  ['Research', '#ResearchUpdate', '7,200 truths'],
  ['Campus', '#CampusLife', '4,500 truths'],
  ['Discussion', '#OpenDiscussion', '3,100 truths'],
  ['Society', '#FreeSpeech', '2,200 truths'],
]

const people = [
  ['EW', 'Emma Wilson', '@emmaw', '#5146e5'],
  ['AC', 'Alex Chen', '@alexc', '#11a4c8'],
  ['JL', 'Jordan Lee', '@jordanl', '#00a87f'],
  ['MT', 'Mia Taylor', '@miat', '#e67e00'],
]

function TruthSocialRightSidebar() {
  const { connectors: { connect } } = useNode()
  return (
    <aside ref={(ref) => { if (ref) connect(ref) }} className="h-full w-[300px] shrink-0 overflow-y-auto border-l border-[#eef1f5] bg-white px-5 py-5">
      <h2 className="text-[16px] font-bold text-[#0d2345]">Trending</h2>
      <div className="mt-4">
        {trends.map(([category, title, count]) => (
          <div key={title} className="border-b border-[#eef1f5] py-3">
            <p className="text-[11px] text-[#9aa4b3]">Trending · {category}</p>
            <p className="mt-1 text-[14px] font-bold text-[#0d2345]">{title}</p>
            <p className="mt-1 text-[11px] text-[#9aa4b3]">{count}</p>
          </div>
        ))}
      </div>

      <div className="my-6 border-t border-[#eef1f5]" />

      <h2 className="text-[16px] font-bold text-[#0d2345]">Who to follow</h2>
      <div className="mt-4 space-y-4">
        {people.map(([initials, name, handle, color]) => (
          <div key={handle} className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[11px] font-bold text-white" style={{ background: color }}>{initials}</div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[14px] font-semibold text-[#0d2345]">{name}</p>
              <p className="truncate text-[11px] text-[#9aa4b3]">{handle}</p>
            </div>
            <button type="button" className="rounded-full border border-[#ff9aa3] px-3 py-1 text-[12px] font-semibold text-[#ef1f2c]">Follow</button>
          </div>
        ))}
      </div>
    </aside>
  )
}

TruthSocialRightSidebar.craft = { displayName: 'Truth Social Right Sidebar' }

export default TruthSocialRightSidebar
