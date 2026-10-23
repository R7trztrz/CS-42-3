// Dedicated Axios instance for the M5 participant flow.
//
// This intentionally does NOT reuse `src/services/api.ts`: that client
// auto-attaches the researcher JWT and redirects to /login on 401, neither
// of which is correct for an anonymous participant. See the M5 handoff doc
// section 2 ("三类 token 必须严格区分").

import axios from 'axios'
import { isStudyClosedError, notifyStudyClosed } from '../model/studyClosure'
import { PARTICIPANT_SESSION_TOKEN_HEADER } from '../model/participantSession'

const participantHttpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

participantHttpClient.interceptors.response.use(response => response, error => {
  if (isStudyClosedError(error)) {
    const token = error.config?.headers?.get?.(PARTICIPANT_SESSION_TOKEN_HEADER)
      ?? error.config?.headers?.[PARTICIPANT_SESSION_TOKEN_HEADER]
    if (typeof token === 'string') notifyStudyClosed(token)
  }
  return Promise.reject(error)
})

export default participantHttpClient
