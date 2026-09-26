import { useCallback, useEffect, useId, useRef, useState, type FormEvent } from 'react'
import axios from 'axios'
import { createStudy, type StudyResponse } from '../../services/studyApi'

export const studyTemplates = [
  {
    id: 'blank',
    name: 'Blank canvas',
    description: 'Start with an empty canvas and add only the components you need.',
    accent: 'bg-gray-400',
  },
  {
    id: 'facebook',
    name: 'Facebook',
    description: 'A familiar social feed with posts, reactions, and comments.',
    accent: 'bg-blue-600',
  },
  {
    id: 'instagram',
    name: 'Instagram',
    description: 'A visual feed suited to image-led interaction studies.',
    accent: 'bg-pink-600',
  },
  {
    id: 'tiktok',
    name: 'TikTok',
    description: 'A short-form content layout for rapid media browsing.',
    accent: 'bg-cyan-500',
  },
  {
    id: 'x',
    name: 'X',
    description: 'A compact text-first timeline for public conversations.',
    accent: 'bg-gray-950',
  },
  {
    id: 'threads',
    name: 'Threads',
    description: 'A conversational feed focused on posts and replies.',
    accent: 'bg-gray-700',
  },
  {
    id: 'bluesky',
    name: 'Bluesky',
    description: 'An open-network feed for social content experiments.',
    accent: 'bg-sky-500',
  },
  {
    id: 'truth-social',
    name: 'Truth Social',
    description: 'A platform-styled feed for comparative interface research.',
    accent: 'bg-blue-800',
  },
] as const

export type StudyTemplateId = (typeof studyTemplates)[number]['id']

type CreateStudyDialogProps = {
  onClose: () => void
  onCreated: (study: StudyResponse, templateId: StudyTemplateId) => void
}

function getCreateErrorMessage(error: unknown) {
  if (axios.isAxiosError<{ error?: string; message?: string }>(error)) {
    if (!error.response) {
      return 'Unable to reach the server. Check that the backend is running.'
    }

    return error.response.data?.error
      ?? error.response.data?.message
      ?? 'The study could not be created.'
  }

  return error instanceof Error ? error.message : 'The study could not be created.'
}

function CreateStudyDialog({ onClose, onCreated }: CreateStudyDialogProps) {
  const titleInputRef = useRef<HTMLInputElement>(null)
  const titleId = useId()
  const descriptionId = useId()
  const [step, setStep] = useState<1 | 2>(1)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [selectedTemplate, setSelectedTemplate] = useState<StudyTemplateId | null>(null)
  const [validationError, setValidationError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const hasEnteredData = title.trim().length > 0 || description.trim().length > 0 || selectedTemplate !== null

  const requestClose = useCallback(() => {
    if (isSubmitting) return

    if (hasEnteredData && !window.confirm('Discard this new study?')) {
      return
    }

    onClose()
  }, [hasEnteredData, isSubmitting, onClose])

  useEffect(() => {
    titleInputRef.current?.focus()
  }, [])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') requestClose()
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', handleKeyDown)

    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [requestClose])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setValidationError('')
    setSubmitError('')

    if (step === 1) {
      if (!title.trim()) {
        setValidationError('Enter a study title to continue.')
        titleInputRef.current?.focus()
        return
      }

      setStep(2)
      return
    }

    if (!selectedTemplate) {
      setValidationError('Select a template to continue.')
      return
    }

    try {
      setIsSubmitting(true)
      const study = await createStudy({
        title: title.trim(),
        description: description.trim() || undefined,
        templateCode: selectedTemplate,
      })
      onCreated(study, selectedTemplate)
    } catch (error) {
      setSubmitError(getCreateErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#101b2b]/70 px-4 py-6">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-study-heading"
        className="flex max-h-full w-full max-w-3xl flex-col overflow-hidden rounded-md border border-[#cbd6e2] bg-white shadow-2xl"
      >
        <div className="flex items-start justify-between border-b border-[#d9e2ec] bg-[#edf3f8] px-6 py-5">
          <div>
            <p className="text-xs font-semibold uppercase text-emerald-700">Step {step} of 2</p>
            <h2 id="create-study-heading" className="mt-1 text-xl font-semibold text-[#172033]">
              {step === 1 ? 'Study information' : 'Choose a template'}
            </h2>
          </div>
          <button
            type="button"
            aria-label="Close create study dialog"
            title="Close"
            onClick={requestClose}
            disabled={isSubmitting}
            className="h-9 w-9 rounded-sm border border-[#b8c6d4] bg-white text-lg text-[#52667d] hover:border-emerald-500 hover:bg-emerald-50 disabled:opacity-50"
          >
            X
          </button>
        </div>

        <div className="h-1 bg-gray-200" aria-hidden="true">
          <div className={`h-full bg-emerald-600 ${step === 1 ? 'w-1/2' : 'w-full'}`} />
        </div>

        <form onSubmit={handleSubmit} className="flex min-h-0 flex-1 flex-col">
          <div className="overflow-y-auto px-6 py-6">
            {step === 1 ? (
              <div className="space-y-5">
                <p className="text-sm text-gray-600">
                  Add the details researchers will use to identify this study.
                </p>

                <div>
                  <label htmlFor={titleId} className="mb-2 block text-sm font-semibold text-gray-800">
                    Study title <span className="text-red-600">*</span>
                  </label>
                  <input
                    ref={titleInputRef}
                    id={titleId}
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    maxLength={255}
                    autoComplete="off"
                    className="w-full rounded-sm border border-[#cbd6e2] px-3 py-2.5 outline-none focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600"
                    placeholder="e.g. Social media browsing study"
                  />
                  <p className="mt-1 text-right text-xs text-gray-500">{title.length}/255</p>
                </div>

                <div>
                  <label htmlFor={descriptionId} className="mb-2 block text-sm font-semibold text-gray-800">
                    Description <span className="font-normal text-gray-500">(optional)</span>
                  </label>
                  <textarea
                    id={descriptionId}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    rows={5}
                    className="w-full resize-y rounded-sm border border-[#cbd6e2] px-3 py-2.5 outline-none focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600"
                    placeholder="Describe the purpose of this study."
                  />
                </div>
              </div>
            ) : (
              <div>
                <p className="mb-5 text-sm text-gray-600">
                  Select the interface style to use as the starting point for {title.trim()}.
                </p>
                <div className="grid gap-3 sm:grid-cols-2">
                  {studyTemplates.map((template) => {
                    const isSelected = selectedTemplate === template.id

                    return (
                      <button
                        key={template.id}
                        type="button"
                        aria-pressed={isSelected}
                        onClick={() => {
                          setSelectedTemplate(template.id)
                          setValidationError('')
                        }}
                        className={`flex min-h-28 items-start gap-4 rounded-md border p-4 text-left transition-colors ${
                          isSelected
                            ? 'border-emerald-600 bg-emerald-50 ring-1 ring-emerald-600'
                            : 'border-gray-200 hover:border-gray-400 hover:bg-gray-50'
                        }`}
                      >
                        <span className={`mt-1 h-8 w-2 shrink-0 ${template.accent}`} aria-hidden="true" />
                        <span>
                          <span className="block font-semibold text-gray-950">{template.name}</span>
                          <span className="mt-1 block text-sm leading-5 text-gray-600">
                            {template.description}
                          </span>
                        </span>
                      </button>
                    )
                  })}
                </div>
              </div>
            )}

            {(validationError || submitError) && (
              <div role="alert" className="mt-5 rounded-sm border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {validationError || submitError}
              </div>
            )}
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#d9e2ec] bg-[#f8fafc] px-6 py-4">
            <button
              type="button"
              onClick={() => setStep(1)}
              disabled={step === 1 || isSubmitting}
              className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-100 disabled:invisible"
            >
              Back
            </button>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={requestClose}
                disabled={isSubmitting}
                className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-100 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {step === 1 ? 'Next' : isSubmitting ? 'Creating...' : 'Create and continue'}
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  )
}

export default CreateStudyDialog
