import { Link } from 'react-router-dom'

const studies = [
  {
    id: 'study-1',
    title: 'Social Media Browsing Study',
    description: 'Compare attention patterns across platform-style feeds.',
    status: 'DRAFT',
    template: 'Facebook',
    updatedAt: 'Today, 10:42 AM',
  },
  {
    id: 'study-2',
    title: 'Short-form Video Interaction',
    description: 'Measure participant engagement with short-form video content.',
    status: 'COLLECTING',
    template: 'TikTok',
    updatedAt: 'Yesterday, 4:18 PM',
  },
  {
    id: 'study-3',
    title: 'News Feed Trust Signals',
    description: 'Evaluate how interface cues affect perceived content credibility.',
    status: 'CLOSED',
    template: 'X',
    updatedAt: '18 Sep 2026',
  },
  {
    id: 'study-4',
    title: 'Visual Content Recall',
    description: 'Explore how visual feed structure influences content recall.',
    status: 'DRAFT',
    template: 'Instagram',
    updatedAt: '16 Sep 2026',
  },
  {
    id: 'study-5',
    title: 'Open Network Discussion',
    description: 'Study interaction patterns in an open-network conversation feed.',
    status: 'CLOSED',
    template: 'Bluesky',
    updatedAt: '11 Sep 2026',
  },
]

const statusStyles: Record<string, string> = {
  DRAFT: 'border-amber-200 bg-amber-50 text-amber-800',
  COLLECTING: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  CLOSED: 'border-gray-200 bg-gray-100 text-gray-700',
}

function StudyList() {
  return (
    <div className="min-h-screen bg-gray-100 text-gray-900">
      <header className="text-white">
        <div className="flex items-center justify-between gap-6 bg-gray-950 px-6 py-5 md:px-10">
          <div>
            <p className="text-sm font-medium text-emerald-400">
              Researcher workspace
            </p>
            <h1 className="mt-1 text-2xl font-semibold">Hello Researcher!</h1>
          </div>

          <div className="flex items-center gap-4 text-sm">
            <button type="button" className="text-gray-300 hover:text-white">
              Account
            </button>
            <button
              type="button"
              className="border border-gray-600 px-3 py-2 font-medium text-gray-200 hover:border-gray-400 hover:text-white"
            >
              Logout
            </button>
          </div>
        </div>

        <nav
          aria-label="Researcher pages"
          className="flex flex-wrap gap-7 border-y border-gray-700 bg-gray-800 px-6 pt-3 md:px-10"
        >
          <Link
            to="/researcher-dashboard"
            className="border-b-2 border-transparent pb-3 text-sm font-medium text-gray-300 hover:text-white"
          >
            Dashboard
          </Link>
          <span className="border-b-2 border-transparent pb-3 text-sm font-medium text-gray-300">
            Interface List
          </span>
          <span className="border-b-2 border-emerald-400 pb-3 text-sm font-medium text-white">
            Study List
          </span>
        </nav>
      </header>

      <main className="mx-auto w-full max-w-7xl px-5 py-7 md:px-8 md:py-9">
        <section className="flex flex-col justify-between gap-4 border-b border-gray-300 pb-6 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-medium text-emerald-700">Studies</p>
            <h2 className="mt-1 text-2xl font-semibold">All research studies</h2>
            <p className="mt-2 text-sm text-gray-600">
              View and manage studies owned by the current researcher.
            </p>
          </div>

          <Link
            to="/researcher-dashboard"
            className="self-start bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800 sm:self-auto"
          >
            Create Study
          </Link>
        </section>

        <section aria-labelledby="study-list-heading" className="mt-6 bg-white">
          <div className="flex flex-col justify-between gap-2 border-b border-gray-200 px-5 py-4 sm:flex-row sm:items-center">
            <div>
              <h3 id="study-list-heading" className="font-semibold text-gray-950">
                Your studies
              </h3>
              <p className="mt-1 text-xs text-gray-500">
                Most recently updated studies appear first.
              </p>
            </div>
            <p className="text-sm font-medium text-gray-600">{studies.length} studies</p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[58rem] border-collapse text-left">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50 text-xs font-semibold uppercase text-gray-500">
                  <th className="px-5 py-3">Study</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Template</th>
                  <th className="px-4 py-3">Last updated</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {studies.map((study) => (
                  <tr key={study.id} className="border-b border-gray-100 last:border-b-0 hover:bg-gray-50">
                    <td className="px-5 py-4">
                      <p className="font-medium text-gray-950">{study.title}</p>
                      <p className="mt-1 max-w-xl text-sm text-gray-500">{study.description}</p>
                    </td>
                    <td className="px-4 py-4">
                      <span className={`inline-flex border px-2 py-1 text-xs font-semibold ${statusStyles[study.status]}`}>
                        {study.status}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-4 text-sm text-gray-700">
                      {study.template}
                    </td>
                    <td className="whitespace-nowrap px-4 py-4 text-sm text-gray-600">
                      {study.updatedAt}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-4">
                        <button type="button" className="text-sm font-medium text-gray-600 hover:text-gray-950">
                          View
                        </button>
                        <button type="button" className="text-sm font-semibold text-emerald-700 hover:text-emerald-900">
                          Edit
                        </button>
                      </div>
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

export default StudyList
