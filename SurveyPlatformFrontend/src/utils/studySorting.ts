import type { StudyStatus, StudySummaryResponse } from '../services/studyApi'

const statusOrder: Record<StudyStatus, number> = {
  DRAFT: 0,
  COLLECTING: 1,
  CLOSED: 2,
}

export function sortStudies(studies: StudySummaryResponse[]) {
  return [...studies].sort((left, right) => {
    const statusDifference = statusOrder[left.status] - statusOrder[right.status]
    if (statusDifference !== 0) return statusDifference

    const leftUpdatedAt = new Date(left.updatedAt).getTime()
    const rightUpdatedAt = new Date(right.updatedAt).getTime()
    const updatedAtDifference = (Number.isNaN(rightUpdatedAt) ? 0 : rightUpdatedAt)
      - (Number.isNaN(leftUpdatedAt) ? 0 : leftUpdatedAt)

    return updatedAtDifference !== 0
      ? updatedAtDifference
      : left.id.localeCompare(right.id)
  })
}
