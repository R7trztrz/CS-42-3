import { useNode } from '@craftjs/core'

import FacebookIcon from './FacebookIcon'


const contacts = [
  {
    initials: 'EW',
    name: 'Emma Wilson',
    bg: 'bg-indigo-600',
  },
  {
    initials: 'AC',
    name: 'Alex Chen',
    bg: 'bg-cyan-600',
  },
  {
    initials: 'JL',
    name: 'Jordan Lee',
    bg: 'bg-emerald-600',
  },
  {
    initials: 'MT',
    name: 'Mia Taylor',
    bg: 'bg-orange-500',
  },
  {
    initials: 'SK',
    name: 'Sam Kim',
    bg: 'bg-red-500',
  },
]


const suggestions = [
  {
    initials: 'EW',
    name: 'Emma Wilson',
    mutual:
      '2 mutual friends',
    bg: 'bg-indigo-600',
  },
  {
    initials: 'AC',
    name: 'Alex Chen',
    mutual:
      '2 mutual friends',
    bg: 'bg-cyan-600',
  },
  {
    initials: 'JL',
    name: 'Jordan Lee',
    mutual:
      '2 mutual friends',
    bg: 'bg-emerald-600',
  },
]


const events = [
  {
    month: 'SEP',
    day: '14',
    title:
      'CS Research Symposium',
    time:
      'Mon · 2:00 PM',
  },
  {
    month: 'SEP',
    day: '18',
    title:
      'Campus Tech Meetup',
    time:
      'Fri · 6:30 PM',
  },
]


function FacebookRightSidebar() {
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
      className="h-full w-[250px] shrink-0 overflow-y-auto bg-[#f0f2f5] px-4 py-4"
    >
      {/* Contacts */}

      <div className="mb-3 flex items-center justify-between">

        <h3 className="text-[15px] font-semibold text-[#050505]">
          Contacts
        </h3>

        <div className="flex items-center gap-3 text-[#65676b]">

          <FacebookIcon
            name="search"
            size={18}
          />

          <FacebookIcon
            name="more"
            size={18}
          />

        </div>

      </div>


      <div className="space-y-2">

        {contacts.map(
          ({
            initials,
            name,
            bg,
          }) => (
            <div
              key={name}
              className="flex items-center gap-3 rounded-lg px-1 py-1.5 hover:bg-[#e4e6eb]"
            >
              <div className="relative shrink-0">

                <div
                  className={`flex h-8 w-8 items-center justify-center rounded-full text-[9px] font-bold text-white ${bg}`}
                >
                  {initials}
                </div>

                <span className="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-[#f0f2f5] bg-[#31a24c]" />

              </div>

              <span className="text-[12px] font-medium text-[#050505]">
                {name}
              </span>

            </div>
          ),
        )}

      </div>


      <div className="my-5 h-px bg-[#ced0d4]" />


      {/* Suggested */}

      <h3 className="mb-3 text-[15px] font-semibold text-[#050505]">
        Suggested for you
      </h3>


      <div className="space-y-3">

        {suggestions.map(
          ({
            initials,
            name,
            mutual,
            bg,
          }) => (
            <div
              key={name}
              className="flex items-center gap-3"
            >
              <div
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[9px] font-bold text-white ${bg}`}
              >
                {initials}
              </div>


              <div className="min-w-0 flex-1">

                <p className="truncate text-[12px] font-semibold text-[#050505]">
                  {name}
                </p>

                <p className="truncate text-[10px] text-[#65676b]">
                  {mutual}
                </p>

              </div>


              <button
                type="button"
                className="text-[11px] font-semibold text-[#1877f2]"
              >
                Add
              </button>

            </div>
          ),
        )}

      </div>


      <div className="my-5 h-px bg-[#ced0d4]" />


      {/* Events */}

      <h3 className="mb-3 text-[15px] font-semibold text-[#050505]">
        Upcoming events
      </h3>


      <div className="space-y-4">

        {events.map(
          ({
            month,
            day,
            title,
            time,
          }) => (
            <div
              key={title}
              className="flex items-center gap-3"
            >
              <div className="flex h-11 w-11 shrink-0 flex-col items-center justify-center rounded-lg bg-white shadow-sm">

                <span className="text-[8px] font-bold text-[#1877f2]">
                  {month}
                </span>

                <span className="text-[15px] font-bold text-[#050505]">
                  {day}
                </span>

              </div>


              <div className="min-w-0">

                <p className="truncate text-[11px] font-semibold text-[#050505]">
                  {title}
                </p>

                <p className="mt-0.5 text-[9px] text-[#65676b]">
                  {time}
                </p>

              </div>
            </div>
          ),
        )}

      </div>

    </aside>
  )
}


FacebookRightSidebar.craft = {
  displayName:
    'Facebook Right Sidebar',
}


export default FacebookRightSidebar