import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useNavigate } from 'react-router-dom'
import ResearcherHeader from '../../components/researcher/ResearcherHeader'
import CreateStudyDialog, {
  type StudyTemplateId,
} from '../../components/studies/CreateStudyDialog'
import {
  listStudies,
  type StudyPageResponse,
  type StudyResponse,
  type StudyStatus,
} from '../../services/studyApi'

const PAGE_SIZE = 20

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

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

function formatCreatedAt(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Unknown' : dateFormatter.format(date)
}

function getListErrorMessage(error: unknown) {
  if (axios.isAxiosError<{ error?: string; message?: string }>(error)) {
    if (!error.response) {
      return 'Unable to reach the server. Check that the backend is running.'
    }

    if (error.response.status === 401) {
      return 'Your session has expired. Please log in again.'
    }

    return error.response.data?.error
      ?? error.response.data?.message
      ?? 'The study list could not be loaded.'
  }

  return 'The study list could not be loaded.'
}

function StudyList() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [studyPage, setStudyPage] = useState<StudyPageResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [requestVersion, setRequestVersion] = useState(0)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)

  useEffect(() => {
    let ignore = false

    const loadStudies = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const response = await listStudies(page, PAGE_SIZE)

        if (!ignore) setStudyPage(response)
      } catch (error) {
        if (!ignore) setErrorMessage(getListErrorMessage(error))
      } finally {
        if (!ignore) setIsLoading(false)
      }
    }

    void loadStudies()

    return () => {
      ignore = true
    }
  }, [page, requestVersion])

  const studies = studyPage?.content ?? []
  const totalStudies = studyPage?.totalElements ?? 0
  const totalPages = studyPage?.totalPages ?? 0

  const handleStudyCreated = (study: StudyResponse, templateId: StudyTemplateId) => {
    const query = new URLSearchParams({ template: templateId })
    navigate(`/studies/${study.id}/edit?${query.toString()}`)
  }

  return (
    <div className="min-h-screen bg-[#f3f6f8] text-gray-950">
      <ResearcherHeader activePage="studies" />

      <main className="mx-auto w-full max-w-7xl px-5 py-8 md:px-8 md:py-10">
        <section className="flex flex-col justify-between gap-4 border-b border-gray-300 pb-7 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-semibold text-emerald-700">Studies</p>
            <h2 className="mt-1 text-3xl font-semibold text-[#172033]">
              All research studies
            </h2>
            <p className="mt-2 text-sm leading-6 text-gray-600">
              View and manage studies owned by the current researcher.
            </p>
          </div>

          <button
            type="button"
            onClick={() => setIsCreateDialogOpen(true)}
            className="self-start rounded-sm bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800 sm:self-auto"
          >
            Create Study
          </button>
        </section>

        <section
          aria-labelledby="study-list-heading"
          className="mt-7 overflow-hidden rounded-md border border-gray-200 bg-white shadow-sm"
        >
          <div className="flex flex-col justify-between gap-2 border-b border-gray-200 px-5 py-4 md:px-6 sm:flex-row sm:items-center">
            <div>
              <h3 id="study-list-heading" className="font-semibold text-[#172033]">
                Your studies
              </h3>
              <p className="mt-1 text-sm text-gray-500">
                Most recently created studies appear first.
              </p>
            </div>
            <p className="border-l-2 border-sky-500 pl-3 text-sm font-semibold text-[#52667d]">
              {isLoading ? 'Loading...' : `${totalStudies} ${totalStudies === 1 ? 'study' : 'studies'}`}
            </p>
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

          {!isLoading && !errorMessage && studies.length === 0 && (
            <div className="px-5 py-14 text-center">
              <p className="font-semibold text-[#172033]">No studies yet</p>
              <p className="mt-1 text-sm text-gray-500">
                Use Create Study to begin your first research project.
              </p>
            </div>
          )}

          {!isLoading && !errorMessage && studies.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[44rem] border-collapse text-left">
                  <thead>
                    <tr className="border-b border-[#d9e2ec] bg-[#edf3f8] text-xs font-semibold uppercase text-[#52667d]">
                      <th className="px-6 py-3.5">Study</th>
                      <th className="px-5 py-3.5">Status</th>
                      <th className="px-5 py-3.5">Created</th>
                      <th className="px-6 py-3.5 text-right">Continue</th>
                    </tr>
                  </thead>
                  <tbody>
                    {studies.map((study, index) => (
                      <tr
                        key={study.id}
                        className={`border-b border-gray-100 last:border-b-0 hover:bg-emerald-50 ${
                          index % 2 === 0 ? 'bg-white' : 'bg-[#fafcfd]'
                        }`}
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-start gap-3">
                            <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${statusDots[study.status]}`} />
                            <div className="min-w-0">
                              <p className="font-semibold text-[#172033]">{study.title}</p>
                              <p className="mt-1 truncate font-mono text-xs text-gray-400">{study.id}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-5 py-4">
                          <span className={`inline-flex rounded-sm border px-2.5 py-1 text-xs font-semibold ${statusStyles[study.status]}`}>
                            {study.status}
                          </span>
                        </td>
                        <td className="whitespace-nowrap px-5 py-4 text-sm text-gray-600">
                          {formatCreatedAt(study.createdAt)}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <Link
                            to={`/studies/${study.id}/edit`}
                            className="text-sm font-semibold text-emerald-700 hover:text-emerald-900"
                          >
                            Open study
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="flex items-center justify-between border-t border-gray-200 bg-[#fafcfd] px-5 py-4 md:px-6">
                  <button
                    type="button"
                    onClick={() => setPage((value) => Math.max(0, value - 1))}
                    disabled={page === 0}
                    className="rounded-sm border border-gray-300 bg-white px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Previous
                  </button>
                  <p className="text-sm font-medium text-[#52667d]">
                    Page {page + 1} of {totalPages}
                  </p>
                  <button
                    type="button"
                    onClick={() => setPage((value) => value + 1)}
                    disabled={page + 1 >= totalPages}
                    className="rounded-sm border border-gray-300 bg-white px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
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

export default StudyList
