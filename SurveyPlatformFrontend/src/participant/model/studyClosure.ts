import axios from 'axios'

const listeners = new Map<string, Set<() => void>>()

export function isStudyClosedError(error: unknown): boolean {
  return axios.isAxiosError<{ code?: string }>(error)
    && (error.response?.status === 410 || error.response?.data?.code === 'STUDY_CLOSED')
}

// Tokens scope notifications to one anonymous session, including late uploads from an old study.
export function notifyStudyClosed(sessionToken: string): void {
  for (const listener of [...(listeners.get(sessionToken) ?? [])]) listener()
}

export function subscribeToStudyClosure(sessionToken: string, listener: () => void): () => void {
  const subscribers = listeners.get(sessionToken) ?? new Set<() => void>()
  subscribers.add(listener)
  listeners.set(sessionToken, subscribers)
  return () => {
    subscribers.delete(listener)
    if (subscribers.size === 0) listeners.delete(sessionToken)
  }
}
