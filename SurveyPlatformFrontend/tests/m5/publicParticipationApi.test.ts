import { beforeEach, describe, expect, it, vi } from 'vitest'

const { participantClientMock, researcherClientMock } = vi.hoisted(() => ({
  participantClientMock: {
    get: vi.fn(),
  },
  researcherClientMock: {
    get: vi.fn(),
  },
}))

vi.mock('../../src/participant/api/participantHttpClient', () => ({
  default: participantClientMock,
}))

vi.mock('../../src/services/api', () => ({
  default: researcherClientMock,
}))

import { getParticipation, type ParticipationResponse } from '../../src/services/studyApi'

describe('public participation API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads public study content through the anonymous participant client', async () => {
    const participation: ParticipationResponse = {
      title: 'Public study',
      description: 'Anonymous participant view',
      eyeTrackingEnabled: false,
      questionnaireEnabled: true,
      theme: null,
      content: { ROOT: {} },
    }
    participantClientMock.get.mockResolvedValue({ data: participation })

    await expect(getParticipation('study-token')).resolves.toBe(participation)

    expect(participantClientMock.get).toHaveBeenCalledWith(
      '/api/participation/study-token',
    )
    expect(researcherClientMock.get).not.toHaveBeenCalled()
  })
})
