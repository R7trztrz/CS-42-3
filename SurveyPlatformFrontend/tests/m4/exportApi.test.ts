// @vitest-environment happy-dom
import { afterEach, expect, it, vi } from 'vitest'
const { api } = vi.hoisted(() => ({ api: { post: vi.fn(), get: vi.fn() } }))
vi.mock('../../src/services/api', () => ({ default: api }))
import { createSessionExport, createStudyExport, downloadStudyExport, getStudyExport } from '../../src/services/exportApi'
afterEach(() => { vi.restoreAllMocks(); vi.clearAllMocks(); vi.useRealTimers() })

it('uses the authenticated study routes and preserves selected categories', async () => {
  const job = { id: 'export-1', status: 'PENDING' }
  api.post.mockResolvedValue({ data: job }); api.get.mockResolvedValue({ data: job })
  expect(await createStudyExport('study-1', ['GAZE', 'BEHAVIOR'])).toBe(job)
  expect(api.post).toHaveBeenCalledWith('/api/studies/study-1/exports', { dataTypes: ['GAZE', 'BEHAVIOR'] })
  expect(await createSessionExport('study-1', 'session-2', ['QUESTIONNAIRE'])).toBe(job)
  expect(api.post).toHaveBeenCalledWith('/api/studies/study-1/participants/session-2/exports', { dataTypes: ['QUESTIONNAIRE'] })
  const controller = new AbortController()
  expect(await getStudyExport('study-1', 'export-1', controller.signal)).toBe(job)
  expect(api.get).toHaveBeenCalledWith('/api/studies/study-1/exports/export-1', { signal: controller.signal })
})

it.each(['STUDY', 'SESSION'] as const)('downloads a %s ZIP through the authenticated client and releases its object URL', async scope => {
  vi.useFakeTimers()
  const blob = new Blob(['zip'], { type: 'application/zip' })
  api.get.mockResolvedValue({ data: blob })
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-export')
  const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function(this: HTMLAnchorElement) { expect(this.download).toBe(`${scope === 'SESSION' ? 'session' : 'study'}-export-export-1.zip`) })
  await downloadStudyExport('study-1', 'export-1', scope)
  expect(api.get).toHaveBeenCalledWith('/api/studies/study-1/exports/export-1/download', { responseType: 'blob' })
  expect(click).toHaveBeenCalledTimes(1)
  expect(document.querySelector('a')).toBeNull()
  vi.advanceTimersByTime(60_000)
  expect(revoke).toHaveBeenCalledWith('blob:test-export')
})
