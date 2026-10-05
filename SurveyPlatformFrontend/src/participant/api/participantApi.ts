import axios from 'axios'

import participantHttpClient from './participantHttpClient'
import {
  PARTICIPANT_SESSION_TOKEN_HEADER,
  type CreateParticipantSessionResponse,
  type ParticipantDeviceInfo,
  type ParticipantErrorResponse,
  type ParticipantQuestionnaireState,
  type ParticipantSessionResponse,
  type SubmitAnswerPayload,
} from '../model/participantSession'

function sessionHeaders(sessionToken: string) {
  return { headers: { [PARTICIPANT_SESSION_TOKEN_HEADER]: sessionToken } }
}

export async function createParticipantSession(
  studyToken: string,
  deviceInfo: ParticipantDeviceInfo | null = null,
): Promise<CreateParticipantSessionResponse> {
  const response = await participantHttpClient.post<CreateParticipantSessionResponse>(
    `/api/participation/${studyToken}/sessions`,
    { deviceInfo },
  )

  return response.data
}

export async function getCurrentParticipantSession(
  sessionToken: string,
): Promise<ParticipantSessionResponse> {
  const response = await participantHttpClient.get<ParticipantSessionResponse>(
    '/api/participant-session',
    sessionHeaders(sessionToken),
  )

  return response.data
}

export async function decideParticipantConsent(
  sessionToken: string,
  accepted: boolean,
): Promise<ParticipantSessionResponse> {
  const response = await participantHttpClient.put<ParticipantSessionResponse>(
    '/api/participant-session/consent',
    { accepted },
    sessionHeaders(sessionToken),
  )

  return response.data
}

export async function completeParticipantBrowsing(
  sessionToken: string,
): Promise<ParticipantSessionResponse> {
  const response = await participantHttpClient.post<ParticipantSessionResponse>(
    '/api/participant-session/browsing-completion',
    undefined,
    sessionHeaders(sessionToken),
  )

  return response.data
}

export async function getCurrentParticipantQuestion(
  sessionToken: string,
): Promise<ParticipantQuestionnaireState> {
  const response = await participantHttpClient.get<ParticipantQuestionnaireState>(
    '/api/participant-session/questionnaire/current',
    sessionHeaders(sessionToken),
  )

  return response.data
}

export async function submitParticipantAnswer(
  sessionToken: string,
  itemId: string,
  idempotencyKey: string,
  answer: SubmitAnswerPayload,
): Promise<ParticipantQuestionnaireState> {
  const response = await participantHttpClient.put<ParticipantQuestionnaireState>(
    `/api/participant-session/questionnaire/answers/${itemId}`,
    answer,
    {
      headers: {
        [PARTICIPANT_SESSION_TOKEN_HEADER]: sessionToken,
        'Idempotency-Key': idempotencyKey,
      },
    },
  )

  return response.data
}

export async function submitParticipantQuestionnaire(
  sessionToken: string,
): Promise<ParticipantSessionResponse> {
  const response = await participantHttpClient.post<ParticipantSessionResponse>(
    '/api/participant-session/questionnaire/submission',
    undefined,
    sessionHeaders(sessionToken),
  )

  return response.data
}

export async function abandonParticipantSession(
  sessionToken: string,
): Promise<ParticipantSessionResponse> {
  const response = await participantHttpClient.post<ParticipantSessionResponse>(
    '/api/participant-session/abandonment',
    undefined,
    sessionHeaders(sessionToken),
  )

  return response.data
}

// Extracts the stable `code` from the M5 error envelope. Returns null for
// network failures or any shape that isn't the expected envelope, so
// callers can fall back to a generic message instead of matching on the
// English `error` text.
export function getParticipantErrorCode(error: unknown): string | null {
  if (axios.isAxiosError<ParticipantErrorResponse>(error)) {
    return error.response?.data?.code ?? null
  }

  return null
}

export function getParticipantErrorStatus(error: unknown): number | null {
  if (axios.isAxiosError(error)) {
    return error.response?.status ?? null
  }

  return null
}
