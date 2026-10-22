import { useNode } from '@craftjs/core'
import InstagramIcon from './InstagramIcon'

function InstagramSidebar() {
  const {
    id: nodeId,
    connectors: { connect },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramSidebar"
      className="flex min-h-[760px] w-[72px] shrink-0 flex-col items-center border-r border-[#efefef] bg-white py-5"
    >
      <div
        data-component-id={`${nodeId}:logo`}
        data-parent-component-id={nodeId}
        data-component-type="InstagramLogo"
        className="mb-8 flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-purple-600 via-pink-500 to-orange-400 text-white shadow-sm"
      >
        <InstagramIcon name="instagram" size={22} />
      </div>

      <nav
        data-component-id={`${nodeId}:navigation`}
        data-parent-component-id={nodeId}
        data-component-type="InstagramNavigation"
        className="flex flex-col items-center gap-6 text-[#262626]"
      >
        {[
          ['home', 'nav-home'],
          ['search', 'nav-search'],
          ['explore', 'nav-explore'],
          ['reels', 'nav-reels'],
          ['message', 'nav-message'],
          ['heart', 'nav-notifications'],
          ['create', 'nav-create'],
        ].map(([icon, trackingKey]) => (
          <div
            key={trackingKey}
            data-component-id={`${nodeId}:${trackingKey}`}
            data-parent-component-id={nodeId}
            data-component-type="NavigationItem"
            className="flex h-10 w-10 items-center justify-center rounded-lg"
          >
            <InstagramIcon
              name={icon as
                | 'home'
                | 'search'
                | 'explore'
                | 'reels'
                | 'message'
                | 'heart'
                | 'create'
              }
              size={24}
            />
          </div>
        ))}
      </nav>

      <div
        data-component-id={`${nodeId}:profile`}
        data-parent-component-id={nodeId}
        data-component-type="InstagramProfile"
        className="mt-auto flex h-9 w-9 items-center justify-center rounded-full border-2 border-pink-500 bg-white text-[9px] font-semibold text-[#262626]"
      >
        You
      </div>
    </aside>
  )
}

InstagramSidebar.craft = {
  displayName: 'Instagram Sidebar',
}

export default InstagramSidebar