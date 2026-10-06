// Dedicated Axios instance for the M5 participant flow.
//
// This intentionally does NOT reuse `src/services/api.ts`: that client
// auto-attaches the researcher JWT and redirects to /login on 401, neither
// of which is correct for an anonymous participant. See the M5 handoff doc
// section 2 ("三类 token 必须严格区分").

import axios from 'axios'

const participantHttpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

export default participantHttpClient
