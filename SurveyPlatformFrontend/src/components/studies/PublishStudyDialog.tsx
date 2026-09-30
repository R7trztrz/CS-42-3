type PublishStudyDialogProps = {
  studyTitle: string
  isPublishing: boolean
  errorMessage: string
  onCancel: () => void
  onConfirm: () => void
}

function PublishStudyDialog({
  studyTitle,
  isPublishing,
  errorMessage,
  onCancel,
  onConfirm,
}: PublishStudyDialogProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#101b2b]/55 px-4">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="publish-study-heading"
        className="w-full max-w-lg rounded-md border border-gray-200 bg-white p-6 shadow-xl"
      >
        <p className="text-xs font-semibold uppercase text-emerald-700">Publish study</p>
        <h2 id="publish-study-heading" className="mt-1 text-xl font-semibold text-[#172033]">
          Start collecting responses?
        </h2>
        <p className="mt-4 text-sm leading-6 text-gray-600">
          <span className="font-semibold text-[#172033]">{studyTitle}</span> will move to
          Collecting. Its test interface will be locked and can no longer be edited.
        </p>

        {errorMessage && (
          <p className="mt-4 rounded-sm border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {errorMessage}
          </p>
        )}

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={isPublishing}
            className="rounded-sm border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPublishing}
            className="rounded-sm bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isPublishing ? 'Publishing...' : 'Publish study'}
          </button>
        </div>
      </section>
    </div>
  )
}

export default PublishStudyDialog
