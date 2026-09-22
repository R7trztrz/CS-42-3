// TEMPLATE: Temporary FR-24 dialog used with frontend-only local persistence.

type UnsavedChangesDialogTemplateProps = {
  isSaving: boolean
  onStay: () => void
  onLeave: () => void
  onSaveAndLeave: () => void
}

function UnsavedChangesDialogTemplate({
  isSaving,
  onStay,
  onLeave,
  onSaveAndLeave,
}: UnsavedChangesDialogTemplateProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-950/60 px-4 py-6">
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="unsaved-changes-heading"
        aria-describedby="unsaved-changes-description"
        className="w-full max-w-lg bg-white shadow-2xl"
      >
        <div className="border-b border-gray-200 px-6 py-5">
          <p className="text-xs font-semibold uppercase text-amber-700">Unsaved work</p>
          <h2 id="unsaved-changes-heading" className="mt-1 text-xl font-semibold text-gray-950">
            Leave this editor?
          </h2>
        </div>

        <div className="px-6 py-5">
          <p id="unsaved-changes-description" className="text-sm leading-6 text-gray-600">
            Your latest interface changes have not been saved. You can stay and continue editing,
            leave without saving, or save this draft before leaving.
          </p>
        </div>

        <div className="flex flex-col-reverse gap-3 border-t border-gray-200 bg-gray-50 px-6 py-4 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onLeave}
            disabled={isSaving}
            className="border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-50 disabled:opacity-50"
          >
            Leave without saving
          </button>
          <button
            type="button"
            onClick={onStay}
            disabled={isSaving}
            className="border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-100 disabled:opacity-50"
          >
            Stay
          </button>
          <button
            type="button"
            onClick={onSaveAndLeave}
            disabled={isSaving}
            className="bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSaving ? 'Saving...' : 'Save and leave'}
          </button>
        </div>
      </section>
    </div>
  )
}

export default UnsavedChangesDialogTemplate
