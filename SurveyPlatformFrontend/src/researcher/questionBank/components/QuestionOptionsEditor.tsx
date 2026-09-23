import type { QuestionOptionRequest } from '../../../shared/types/question'

interface QuestionOptionsEditorProps {
  options: QuestionOptionRequest[]
  onChange: (options: QuestionOptionRequest[]) => void
}

export default function QuestionOptionsEditor({
  options,
  onChange,
}: QuestionOptionsEditorProps) {
  function updateOption(index: number, text: string) {
    const next = options.slice()
    next[index] = { optionText: text }
    onChange(next)
  }

  function removeOption(index: number) {
    onChange(options.filter((_, i) => i !== index))
  }

  function addOption() {
    onChange([...options, { optionText: '' }])
  }

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-gray-700">Options</label>

      {options.map((option, index) => (
        <div key={index} className="flex items-center gap-2">
          <span className="w-5 text-sm text-gray-400">{index + 1}</span>
          <input
            type="text"
            value={option.optionText}
            onChange={(event) => updateOption(index, event.target.value)}
            placeholder={`Option ${index + 1}`}
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
          />
          <button
            type="button"
            onClick={() => removeOption(index)}
            disabled={options.length <= 1}
            className="rounded-lg px-2 py-1 text-sm text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Remove
          </button>
        </div>
      ))}

      <button
        type="button"
        onClick={addOption}
        className="rounded-lg border border-dashed border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:border-blue-400 hover:text-blue-600"
      >
        + Add option
      </button>

      {options.length < 2 && (
        <p className="text-xs text-amber-600">At least two options are required.</p>
      )}
    </div>
  )
}
