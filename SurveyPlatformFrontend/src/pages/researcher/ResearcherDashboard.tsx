import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import CreateStudyDialog, {
  type StudyTemplateId,
} from '../../components/studies/CreateStudyDialog'
import type { StudyResponse } from '../../services/studyApi'

const navigationItems = ['Dashboard', 'Interface List', 'Study List']

const studySummary = [
  { label: 'Draft', value: 4, accent: 'border-amber-400' },
  { label: 'Collecting', value: 2, accent: 'border-emerald-500' },
  { label: 'Closed', value: 7, accent: 'border-gray-400' },
]

const recentStudies = [
  {
    title: 'Social Media Browsing Study',
    description: 'Compare attention patterns across platform-style feeds.',
    status: 'DRAFT',
    updatedAt: 'Today, 10:42 AM',
  },
  {
    title: 'Short-form Video Interaction',
    description: 'Measure participant engagement with video content.',
    status: 'COLLECTING',
    updatedAt: 'Yesterday, 4:18 PM',
  },
  {
    title: 'News Feed Trust Signals',
    description: 'Evaluate how interface cues affect content credibility.',
    status: 'CLOSED',
    updatedAt: '18 Sep 2026',
  },
]

const statusStyles: Record<string, string> = {
  DRAFT: 'border-amber-200 bg-amber-50 text-amber-800',
  COLLECTING: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  CLOSED: 'border-gray-200 bg-gray-100 text-gray-700',
}

function ResearcherDashboard() {
  const navigate = useNavigate()
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)

  const handleStudyCreated = (study: StudyResponse, templateId: StudyTemplateId) => {
    const query = new URLSearchParams({
      template: templateId,
    })

    navigate(`/studies/${study.id}/edit?${query.toString()}`)
  }

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
          {navigationItems.map((item, index) => {
            const className = `border-b-2 pb-3 text-sm font-medium ${
              index === 0
                ? 'border-emerald-400 text-white'
                : 'border-transparent text-gray-300'
            }`

            if (item === 'Study List') {
              return (
                <Link key={item} to="/studies" className={`${className} hover:text-white`}>
                  {item}
                </Link>
              )
            }

            return <span key={item} className={className}>{item}</span>
          })}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-7xl px-5 py-7 md:px-8 md:py-9">
        <section className="flex flex-col justify-between gap-4 border-b border-gray-300 pb-6 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-medium text-emerald-700">Overview</p>
            <h2 className="mt-1 text-2xl font-semibold">Research dashboard</h2>
            <p className="mt-2 max-w-2xl text-sm text-gray-600">
              Continue recent work and manage your research studies.
            </p>
          </div>

          <button
            type="button"
            onClick={() => setIsCreateDialogOpen(true)}
            className="self-start bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800 sm:self-auto"
          >
            Create Study
          </button>
        </section>

        <section aria-labelledby="study-status-heading" className="py-6">
          <div className="mb-3 flex items-center justify-between">
            <h3 id="study-status-heading" className="text-sm font-semibold text-gray-700">
              Study status
            </h3>
            <p className="text-xs text-gray-500">13 studies total</p>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            {studySummary.map((item) => (
              <div
                key={item.label}
                className={`border border-gray-200 border-l-4 bg-white px-5 py-4 ${item.accent}`}
              >
                <p className="text-sm text-gray-600">{item.label}</p>
                <p className="mt-1 text-2xl font-semibold text-gray-950">{item.value}</p>
              </div>
            ))}
          </div>
        </section>

        <section aria-labelledby="recent-studies-heading" className="min-w-0 bg-white">
            <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
              <div>
                <h3 id="recent-studies-heading" className="font-semibold">
                  Recent studies
                </h3>
                <p className="mt-1 text-xs text-gray-500">Last updated across your workspace</p>
              </div>
              <button type="button" className="text-sm font-medium text-emerald-700 hover:text-emerald-900">
                View all
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[42rem] border-collapse text-left">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50 text-xs font-semibold uppercase text-gray-500">
                    <th className="px-5 py-3">Study</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Last updated</th>
                    <th className="px-5 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {recentStudies.map((study) => (
                    <tr key={study.title} className="border-b border-gray-100 last:border-b-0">
                      <td className="px-5 py-4">
                        <p className="font-medium text-gray-950">{study.title}</p>
                        <p className="mt-1 text-sm text-gray-500">{study.description}</p>
                      </td>
                      <td className="px-4 py-4">
                        <span
                          className={`inline-flex border px-2 py-1 text-xs font-semibold ${statusStyles[study.status]}`}
                        >
                          {study.status}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-4 py-4 text-sm text-gray-600">
                        {study.updatedAt}
                      </td>
                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          className="text-sm font-semibold text-emerald-700 hover:text-emerald-900"
                        >
                          Open
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
        </section>
      </main>

      {isCreateDialogOpen && (
        <CreateStudyDialog
          onClose={() => setIsCreateDialogOpen(false)}
          onCreated={handleStudyCreated}
        />
      )}
    </div>
  )
}

export default ResearcherDashboard
