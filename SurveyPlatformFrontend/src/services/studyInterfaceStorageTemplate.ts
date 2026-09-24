// TEMPLATE: Frontend-only persistence for the FR-24 demonstration.
// Replace these functions with authenticated API calls when the backend contract is ready.

export type StudyInterfaceTemplateRecord = {
  studyId: string
  templateType: string
  editorState: string
  schemaVersion: 1
  savedAt: string
}

const STORAGE_PREFIX = 'survey-platform-template:study-interface:'

function getStorageKey(studyId: string) {
  return `${STORAGE_PREFIX}${studyId}`
}

export function loadStudyInterfaceTemplate(studyId: string) {
  const storedValue = localStorage.getItem(getStorageKey(studyId))

  if (!storedValue) return null

  try {
    const record = JSON.parse(storedValue) as StudyInterfaceTemplateRecord

    if (record.studyId !== studyId || typeof record.editorState !== 'string') {
      return null
    }

    return record
  } catch {
    return null
  }
}

export function saveStudyInterfaceTemplate(
  studyId: string,
  templateType: string,
  editorState: string,
) {
  const record: StudyInterfaceTemplateRecord = {
    studyId,
    templateType,
    editorState,
    schemaVersion: 1,
    savedAt: new Date().toISOString(),
  }

  localStorage.setItem(getStorageKey(studyId), JSON.stringify(record))
  return record
}
