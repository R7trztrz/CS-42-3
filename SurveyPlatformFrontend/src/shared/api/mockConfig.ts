// The M4/M5 backend endpoints aren't wired up yet (teammates are still
// building them), so every api/*Api.ts file in researcher/ and
// participant/ branches on USE_MOCK_API: same function signatures, but the
// mock branch returns in-memory fixtures instead of calling the network.
//
// To switch a module over once its real endpoint exists, delete that
// module's mock branch and mock data file; nothing else needs to change,
// since callers only ever import the *Api.ts functions, never the mocks
// directly.
export const USE_MOCK_API = true

// Small helper so mock responses feel like a real network call (loading
// states, skeletons, etc. behave the same in mock mode as they will once
// wired to the backend).
export function mockDelay(ms = 300): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
