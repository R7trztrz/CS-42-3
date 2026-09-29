import { useNode } from '@craftjs/core'

import FacebookIcon from './FacebookIcon'


const shortcuts = [
  {
    label: 'Research Group',
  },
  {
    label: 'Campus Hub',
  },
  {
    label: 'Tech Society',
  },
]


function SidebarItem({
  icon,
  label,
}: {
  icon:
    | 'home'
    | 'friends'
    | 'groups'
    | 'video'
    | 'saved'
  label: string
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg px-2 py-2 text-[#050505] hover:bg-[#e4e6eb]">

      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#e4e6eb] text-[#050505]">
        <FacebookIcon
          name={icon}
          size={21}
        />
      </div>

      <span className="text-[14px] font-semibold">
        {label}
      </span>

    </div>
  )
}


function FacebookLeftSidebar() {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="h-full w-[230px] shrink-0 overflow-y-auto bg-[#f0f2f5] px-3 py-4"
    >
      {/* Profile */}

      <div className="mb-2 flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-[#e4e6eb]">

        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#5b49ff] text-[11px] font-bold text-white">
          You
        </div>

        <span className="text-[14px] font-semibold text-[#050505]">
          Your Profile
        </span>

      </div>


      {/* Navigation */}

      <div className="space-y-1">

        <SidebarItem
          icon="home"
          label="Home"
        />

        <SidebarItem
          icon="friends"
          label="Friends"
        />

        <SidebarItem
          icon="groups"
          label="Groups"
        />

        <SidebarItem
          icon="video"
          label="Video"
        />

        <SidebarItem
          icon="saved"
          label="Saved"
        />

      </div>


      <div className="my-4 h-px bg-[#ced0d4]" />


      <p className="mb-2 px-2 text-[13px] font-semibold text-[#65676b]">
        Your shortcuts
      </p>


      <div className="space-y-1">

        {shortcuts.map(
          ({
            label,
          }) => (
            <div
              key={label}
              className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-[#e4e6eb]"
            >
              <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-[#dbe7ff] to-[#b9ccff]" />

              <span className="text-[14px] font-medium text-[#050505]">
                {label}
              </span>

            </div>
          ),
        )}

      </div>

    </aside>
  )
}


FacebookLeftSidebar.craft = {
  displayName:
    'Facebook Left Sidebar',
}


export default FacebookLeftSidebar