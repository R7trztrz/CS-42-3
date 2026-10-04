// M4 (question bank + questionnaire) now calls the real backend directly -
// see researcher/questionBank/api/questionBankApi.ts and
// researcher/questionnaire/api/questionnaireApi.ts. M5 (participant
// session) has no backend yet (SurveyPlatformBackend/docs/m4-api-contract-v1.md
// section 6: "There is no public participant questionnaire/session
// endpoint in M4 v1"), so participant/session/api/sessionApi.ts stays on
// the in-memory mock below unconditionally until an M5 contract exists.

// Small helper so mock responses feel like a real network call (loading
// states, skeletons, etc. behave the same in mock mode as they will once
// wired to a real M5 backend).
export function mockDelay(ms = 300): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
