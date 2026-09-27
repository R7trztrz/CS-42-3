import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useNavigate } from 'react-router-dom'
import ResearcherHeader from '../../components/researcher/ResearcherHeader'
import {
  listStudies,
  type StudyStatus,
  type StudySummaryResponse,
} from '../../services/studyApi'
import { sortStudies } from '../../utils/studySorting'

const PAGE_SIZE = 100
const RECENT_STUDY_LIMIT = 5

const statusDetails: Array<{
  status: StudyStatus
  label: string
  detail: string
  panelClass: string
  markerClass: string
  valueClass: string
}> = [
  {
    status: 'DRAFT',
    label: 'Draft',
    detail: 'Studies being prepared',
    panelClass: 'border-amber-200 bg-amber-50',
    markerClass: 'bg-amber-500',
    valueClass: 'text-amber-950',
  },
  {
    status: 'COLLECTING',
    label: 'Collecting',
    detail: 'Studies accepting responses',
    panelClass: 'border-emerald-200 bg-emerald-50',
    markerClass: 'bg-emerald-600',
    valueClass: 'text-emerald-950',
  },
  {
    status: 'CLOSED',
    label: 'Closed',
    detail: 'Completed studies',
    panelClass: 'border-sky-200 bg-sky-50',
    markerClass: 'bg-sky-600',
    valueClass: 'text-sky-950',
  },
]

const statusStyles: Record<StudyStatus, string> = {
  DRAFT: 'border-amber-300 bg-amber-50 text-amber-800',
  COLLECTING: 'border-emerald-300 bg-emerald-50 text-emerald-800',
  CLOSED: 'border-sky-300 bg-sky-50 text-sky-800',
}

const statusDots: Record<StudyStatus, string> = {
  DRAFT: 'bg-amber-500',
  COLLECTING: 'bg-emerald-600',
  CLOSED: 'bg-sky-600',
}

const dateFormatter = new Intl.DateTimeFormat('en-AU', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  timeZone: 'Australia/Sydney',
})

function formatUpdatedAt(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Unknown' : dateFormatter.format(date)
}

function getDashboardErrorMessage(error: unknown) {
  if (axios.isAxiosError<{ error?: string; message?: string }>(error)) {
    if (!error.response) {
      return 'Unable to reach the server. Check that the backend is running.'
    }

    if (error.response.status === 401) {
      return 'Your session has expired. Please log in again.'
    }

    return error.response.data?.error
      ?? error.response.data?.message
      ?? 'The dashboard could not be loaded.'
  }

  return 'The dashboard could not be loaded.'
}

async function loadAllStudies() {
  const firstPage = await listStudies(0, PAGE_SIZE)

  if (firstPage.totalPages <= 1) {
    return firstPage.content
  }

  const remainingPages = await Promise.all(
    Array.from(
      { length: firstPage.totalPages - 1 },
      (_, index) => listStudies(index + 1, PAGE_SIZE),
    ),
  )

  return [firstPage, ...remainingPages].flatMap((page) => page.content)
}

function ResearcherDashboard() {
  const navigate = useNavigate()
  const [studies, setStudies] = useState<StudySummaryResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [requestVersion, setRequestVersion] = useState(0)

  useEffect(() => {
    let ignore = false

    const loadDashboard = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const response = await loadAllStudies()

        if (!ignore) setStudies(sortStudies(response))
      } catch (error) {
        if (!ignore) setErrorMessage(getDashboardErrorMessage(error))
      } finally {
        if (!ignore) setIsLoading(false)
      }
    }

    void loadDashboard()

    return () => {
      ignore = true
    }
  }, [requestVersion])

  const recentStudies = studies.slice(0, RECENT_STUDY_LIMIT)
  const statusCounts = studies.reduce<Record<StudyStatus, number>>(
    (counts, study) => {
      counts[study.status] += 1
      return counts
    },
    { DRAFT: 0, COLLECTING: 0, CLOSED: 0 },
  )

  const openStudy = (studyId: string) => {
    navigate(`/studies/${studyId}/edit`)
  }

  return (
    <div className="min-h-screen bg-[#f3f6f8] text-gray-950">
      <ResearcherHeader activePage="dashboard" />

      <main className="mx-auto w-full max-w-7xl px-5 py-8 md:px-8 md:py-10">
        <section className="flex flex-col justify-between gap-4 border-b border-gray-300 pb-7 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-semibold text-emerald-700">Overview</p>
            <h2 className="mt-1 text-3xl font-semibold text-[#172033]">
              Research dashboard
            </h2>
          </div>
          <div className="border-l-2 border-sky-500 pl-4 text-right">
            <p className="text-xs font-semibold uppercase text-gray-500">Workspace total</p>
            <p className="mt-1 text-2xl font-semibold text-[#172033]">
              {isLoading ? '-' : studies.length} {studies.length === 1 ? 'study' : 'studies'}
            </p>
          </div>
        </section>

        <section aria-labelledby="study-status-heading" className="py-7">
          <h3 id="study-status-heading" className="mb-4 text-base font-semibold text-[#172033]">
            Study status
          </h3>

          <div className="grid gap-4 sm:grid-cols-3">
            {statusDetails.map((item) => (
              <div
                key={item.status}
                className={`relative overflow-hidden rounded-md border px-5 py-5 ${item.panelClass}`}
              >
                <span className={`absolute inset-y-0 left-0 w-1.5 ${item.markerClass}`} />
                <div className="flex items-start justify-between gap-4 pl-1">
                  <div>
                    <p className="text-sm font-semibold text-gray-700">{item.label}</p>
                    <p className="mt-2 text-xs text-gray-600">{item.detail}</p>
                  </div>
                  <p className={`text-3xl font-semibold ${item.valueClass}`}>
                    {isLoading ? '-' : statusCounts[item.status]}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section
          aria-labelledby="recent-studies-heading"
          className="overflow-hidden rounded-md border border-gray-200 bg-white shadow-sm"
        >
          <div className="flex items-center justify-between gap-4 border-b border-gray-200 px-5 py-4 md:px-6">
            <div>
              <h3 id="recent-studies-heading" className="font-semibold text-[#172033]">
                Recent studies
              </h3>
            </div>
            <Link to="/studies" className="text-sm font-semibold text-emerald-700 hover:text-emerald-900">
              View all studies
            </Link>
          </div>

          {isLoading && (
            <div className="px-5 py-14 text-center text-sm text-gray-500" role="status">
              Loading studies...
            </div>
          )}

          {!isLoading && errorMessage && (
            <div className="px-5 py-12 text-center">
              <p className="text-sm text-red-700" role="alert">{errorMessage}</p>
              <button
                type="button"
                onClick={() => setRequestVersion((value) => value + 1)}
                className="mt-4 rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-800 hover:bg-gray-50"
              >
                Try again
              </button>
            </div>
          )}

          {!isLoading && !errorMessage && recentStudies.length === 0 && (
            <div className="px-5 py-14 text-center">
              <p className="font-semibold text-[#172033]">No studies yet</p>
              <p className="mt-1 text-sm text-gray-500">
                Your studies will appear here after they are created.
              </p>
            </div>
          )}

          {!isLoading && !errorMessage && recentStudies.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[40rem] border-collapse text-left">
                <thead>
                  <tr className="border-b border-[#d9e2ec] bg-[#edf3f8] text-xs font-semibold uppercase text-[#52667d]">
                    <th className="px-6 py-3.5">Study</th>
                    <th className="px-5 py-3.5">Status</th>
                    <th className="px-5 py-3.5">Last updated</th>
                    <th className="px-6 py-3.5 text-right">Continue</th>
                  </tr>
                </thead>
                <tbody>
                  {recentStudies.map((study, index) => (
                    <tr
                      key={study.id}
                      role="link"
                      tabIndex={0}
                      aria-label={`Open ${study.title}`}
                      onClick={() => openStudy(study.id)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault()
                          openStudy(study.id)
                        }
                      }}
                      className={`cursor-pointer border-b border-gray-100 outline-none last:border-b-0 hover:bg-emerald-50 focus-visible:bg-emerald-50 ${
                        index % 2 === 0 ? 'bg-white' : 'bg-[#fafcfd]'
                      }`}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-start gap-3">
                          <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${statusDots[study.status]}`} />
                          <div className="min-w-0">
                            <p className="font-semibold text-[#172033]">{study.title}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <span className={`inline-flex rounded-sm border px-2.5 py-1 text-xs font-semibold ${statusStyles[study.status]}`}>
                          {study.status}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-sm text-gray-600">
                        {formatUpdatedAt(study.updatedAt)}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <span className="inline-flex rounded-sm border border-emerald-700 bg-white px-3 py-2 text-sm font-semibold text-emerald-700">
                          Open study
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default ResearcherDashboard
