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
      data-component-type="BlueskyRightSidebar"
      className="h-full w-[300px] shrink-0 overflow-y-auto border-l border-[#eef2f7] bg-white px-5 py-5"
    >
      <section
        data-component-id={`${nodeId}:feeds`}
        data-parent-component-id={nodeId}
        data-component-type="BlueskyFeeds"
      >
        <h2 className="text-[16px] font-bold text-[#111827]">
          Feeds
        </h2>

        <div className="mt-4 space-y-4">
          {feeds.map(([name, meta], index) => (
            <div
              key={name}
              data-component-id={`${nodeId}:feed-${index}`}
              data-parent-component-id={nodeId}
              data-component-type="FeedItem"
              className="flex items-center gap-3"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#eef4ff] text-[16px]">
                {index === 0 ? '✨' : index === 1 ? '👥' : index === 2 ? '🦋' : '🧪'}
              </div>

              <div className="min-w-0 flex-1">
                <p className="truncate text-[14px] font-semibold text-[#1f2937]">
                  {name}
                </p>

                <p className="truncate text-[11px] text-[#94a3b8]">
                  {meta}
                </p>
              </div>

              <button
                type="button"
                data-component-id={`${nodeId}:feed-${index}-pin`}
                data-parent-component-id={nodeId}
                data-component-type="FeedAction"
                className="text-[12px] font-semibold text-[#1185fe]"
              >
                Pin
              </button>
            </div>
          ))}
        </div>
      </section>

      <div className="my-6 border-t border-[#eef2f7]" />

      <section
        data-component-id={`${nodeId}:suggestions`}
        data-parent-component-id={nodeId}
        data-component-type="BlueskySuggestions"
      >
        <h2 className="text-[16px] font-bold text-[#111827]">
          Suggested for you
        </h2>

        <div className="mt-4 space-y-4">
          {people.map(([initials, name, handle, color], index) => (
            <div
              key={handle}
              data-component-id={`${nodeId}:suggestion-${index}`}
              data-parent-component-id={nodeId}
              data-component-type="SuggestionItem"
              className="flex items-center gap-3"
            >
              <div
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[11px] font-bold text-white"
                style={{
                  background: color,
                }}
              >
                {initials}
              </div>

              <div className="min-w-0 flex-1">
                <p className="truncate text-[14px] font-semibold text-[#1f2937]">
                  {name}
                </p>

                <p className="truncate text-[11px] text-[#94a3b8]">
                  {handle}
                </p>
              </div>

              <button
                type="button"
                data-component-id={`${nodeId}:suggestion-${index}-follow`}
                data-parent-component-id={nodeId}
                data-component-type="SuggestionAction"
                className="rounded-full border border-[#7cc4ff] px-3 py-1 text-[12px] font-semibold text-[#1185fe]"
              >
                Follow
              </button>
            </div>
          ))}
        </div>
      </section>
    </aside>
  )
}

BlueskyRightSidebar.craft = {
  displayName: 'Bluesky Right Sidebar',
}

export default BlueskyRightSidebar