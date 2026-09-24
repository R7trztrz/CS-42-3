const statusSummary = [
  {
    label: 'Draft',
    value: 4,
    detail: 'Studies being prepared',
    panelClass: 'border-amber-200 bg-amber-50',
    markerClass: 'bg-amber-500',
    valueClass: 'text-amber-950',
  },
  {
    label: 'Collecting',
    value: 2,
    detail: 'Studies accepting responses',
    panelClass: 'border-emerald-200 bg-emerald-50',
    markerClass: 'bg-emerald-600',
    valueClass: 'text-emerald-950',
  },
  {
    label: 'Closed',
    value: 7,
    detail: 'Completed studies',
    panelClass: 'border-sky-200 bg-sky-50',
    markerClass: 'bg-sky-600',
    valueClass: 'text-sky-950',
  },
]

const recentStudies = [
  {
    title: 'Social Media Browsing Study',
    id: 'f5275b8e-4128-48ba-a666-417e893f15ae',
    status: 'DRAFT',
    created: '24 Sep 2026',
    statusClass: 'border-amber-300 bg-amber-50 text-amber-800',
    dotClass: 'bg-amber-500',
  },
  {
    title: 'Short-form Video Interaction',
    id: '320b1266-bbf6-40bc-b8ff-ecb6b2e780fb',
    status: 'COLLECTING',
    created: '20 Sep 2026',
    statusClass: 'border-emerald-300 bg-emerald-50 text-emerald-800',
    dotClass: 'bg-emerald-600',
  },
  {
    title: 'News Feed Trust Signals',
    id: '8e13ab67-8321-4f3c-bdd1-94094d28dcb6',
    status: 'CLOSED',
    created: '18 Sep 2026',
    statusClass: 'border-sky-300 bg-sky-50 text-sky-800',
    dotClass: 'bg-sky-600',
  },
]

function NewInterface() {
  return (
    <div className="min-h-screen bg-[#f3f6f8] text-gray-950">
      <header className="text-white">
        <div className="bg-[#101b2b] px-6 py-5 md:px-10">
          <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-md bg-emerald-500 text-sm font-bold text-[#101b2b]">
                SP
              </div>
              <div>
                <p className="text-xs font-semibold uppercase text-emerald-300">
                  Researcher workspace
                </p>
                <h1 className="mt-1 text-xl font-semibold">Survey Platform</h1>
              </div>
            </div>

            <div className="flex items-center gap-3 text-sm">
              <span className="hidden text-gray-300 sm:inline">researcher@example.com</span>
              <span className="border-l border-gray-600 pl-3 font-medium text-white">
                Account
              </span>
            </div>
          </div>
        </div>

        <nav
          aria-label="Researcher pages"
          className="border-b border-[#314158] bg-[#1d2b3f] px-6 md:px-10"
        >
          <div className="mx-auto flex w-full max-w-7xl gap-8">
            <span className="border-b-2 border-emerald-400 px-1 py-3 text-sm font-semibold text-white">
              Dashboard
            </span>
            <span className="border-b-2 border-transparent px-1 py-3 text-sm font-medium text-gray-300">
              Study List
            </span>
          </div>
        </nav>
      </header>

      <main className="mx-auto w-full max-w-7xl px-5 py-8 md:px-8 md:py-10">
        <section className="flex flex-col justify-between gap-4 border-b border-gray-300 pb-7 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-semibold text-emerald-700">Overview</p>
            <h2 className="mt-1 text-3xl font-semibold text-[#172033]">
              Research dashboard
            </h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-gray-600">
              Track the status of your studies and return to recent research work.
            </p>
          </div>
          <div className="border-l-2 border-sky-500 pl-4 text-right">
            <p className="text-xs font-semibold uppercase text-gray-500">Workspace total</p>
            <p className="mt-1 text-2xl font-semibold text-[#172033]">13 studies</p>
          </div>
        </section>

        <section aria-labelledby="prototype-status-heading" className="py-7">
          <div className="mb-4 flex items-end justify-between gap-4">
            <div>
              <h3 id="prototype-status-heading" className="text-base font-semibold text-[#172033]">
                Study status
              </h3>
              <p className="mt-1 text-sm text-gray-500">Current activity across your workspace</p>
            </div>
            <p className="text-xs font-medium text-gray-500">Updated today</p>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            {statusSummary.map((item) => (
              <div
                key={item.label}
                className={`relative overflow-hidden rounded-md border px-5 py-5 ${item.panelClass}`}
              >
                <span className={`absolute inset-y-0 left-0 w-1.5 ${item.markerClass}`} />
                <div className="flex items-start justify-between gap-4 pl-1">
                  <div>
                    <p className="text-sm font-semibold text-gray-700">{item.label}</p>
                    <p className="mt-2 text-xs text-gray-600">{item.detail}</p>
                  </div>
                  <p className={`text-3xl font-semibold ${item.valueClass}`}>{item.value}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section
          aria-labelledby="prototype-recent-heading"
          className="overflow-hidden rounded-md border border-gray-200 bg-white shadow-sm"
        >
          <div className="flex items-center justify-between gap-4 border-b border-gray-200 px-5 py-4 md:px-6">
            <div>
              <h3 id="prototype-recent-heading" className="font-semibold text-[#172033]">
                Recent studies
              </h3>
              <p className="mt-1 text-sm text-gray-500">Select a row to continue your research</p>
            </div>
            <span className="text-sm font-semibold text-emerald-700">View all studies</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[40rem] border-collapse text-left">
              <thead>
                <tr className="border-b border-[#d9e2ec] bg-[#edf3f8] text-xs font-semibold uppercase text-[#52667d]">
                  <th className="px-6 py-3.5">Study</th>
                  <th className="px-5 py-3.5">Status</th>
                  <th className="px-5 py-3.5">Created</th>
                  <th className="px-6 py-3.5 text-right">Continue</th>
                </tr>
              </thead>
              <tbody>
                {recentStudies.map((study, index) => (
                  <tr
                    key={study.id}
                    className={`border-b border-gray-100 last:border-b-0 ${
                      index % 2 === 0 ? 'bg-white' : 'bg-[#fafcfd]'
                    }`}
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-start gap-3">
                        <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${study.dotClass}`} />
                        <div className="min-w-0">
                          <p className="font-semibold text-[#172033]">{study.title}</p>
                          <p className="mt-1 truncate font-mono text-xs text-gray-400">{study.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span className={`inline-flex rounded-sm border px-2.5 py-1 text-xs font-semibold ${study.statusClass}`}>
                        {study.status}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-5 py-4 text-sm text-gray-600">
                      {study.created}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <span className="text-sm font-semibold text-emerald-700">Open study</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  )
}

export default NewInterface
