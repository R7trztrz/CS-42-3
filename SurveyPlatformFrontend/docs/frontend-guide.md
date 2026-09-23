# M4/M5 Frontend Guide

Covers the researcher UI (question bank + questionnaire editor, FR-32~39) and
the participant UI (session flow, M5/UC-28~33) added on `feat/m4-m5-frontend`.
Not detailed by design - enough to run the app, find your way around the
code, and know which endpoint each screen expects once the real backend
lands.

## 1. Running it locally

```bash
cd SurveyPlatformFrontend
npm install
npm run dev
```

Open the URL Vite prints (usually `http://localhost:5173/`). Other useful
commands: `npm run build` (type-checks via `tsc -b` then bundles),
`npm run lint`, `npm run preview` (serve the production build locally).

**No backend or database is required.** Every screen in this doc runs off
an in-memory mock (see section 3) until the real endpoints exist, so
`npm run dev` alone is enough to click through the whole flow.

Entry points, from the home page nav bar:

| Link | Route | What it is |
|---|---|---|
| Question bank | `/researcher/questions` | list/create/edit/delete questions (FR-32~35) |
| Questionnaire editor | `/researcher/studies/demo-study/questionnaire` | select/order/branch questions into a questionnaire (FR-36~39) |
| Preview participant flow (M5) | `/participate/demo-study` | consent → feed → questionnaire (with branching) → complete |

Both researcher routes above use `demo-study` as a stand-in `studyId`
because there's no study-list UI yet (M2). The questionnaire editor and the
participant flow read/write the *same* mock questionnaire, so configuring
branch rules in the editor and then walking through `/participate/demo-study`
is the fastest way to sanity-check a jump rule.

## 2. Project layout

```
src/
  shared/
    types/           DTOs shared across modules, mirroring backend records
    api/mockConfig.ts   the USE_MOCK_API switch (see section 3)
  researcher/
    questionBank/      FR-32~35
    questionnaire/      FR-36~39
  participant/
    session/            M5 (UC-28~33)
  layouts/, pages/, routes/   pre-existing app shell (not part of this work)
```

Each `researcher/*` and `participant/*` module follows the same internal
shape: `api/` (the facade + mock), `pages/`, and `components/` where needed.

## 3. How the API layer is designed

Every screen calls a plain async function from an `api/*Api.ts` file - never
`axios` or `fetch` directly, and never a mock function directly. Each of
those functions branches on one flag:

```ts
// shared/api/mockConfig.ts
export const USE_MOCK_API = true
```

```ts
// e.g. researcher/questionBank/api/questionBankApi.ts
export async function listQuestions(type, keyword) {
  if (USE_MOCK_API) return mockListQuestions(type, keyword)
  const response = await api.get('/api/questions', { params: { type, keyword } })
  return response.data
}
```

`api` is the shared axios instance in `src/services/api.ts`
(`baseURL` from `VITE_API_BASE_URL`, defaults to `http://localhost:8080`).

**To connect a module to its real backend**, once it exists: flip
`USE_MOCK_API` to `false`. No component or page code needs to change - they
only ever call the `api/*Api.ts` functions, and those keep the same
signature on both sides of the flag. The mock files
(`mock*.ts`) can be deleted once every module is wired up for real.

## 4. Question bank API (FR-32~35)

File: `researcher/questionBank/api/questionBankApi.ts`. Backend:
`com.cs_42_3.surveyplatformbackend.survey.api.QuestionController`.

| Function | Real endpoint | Notes |
|---|---|---|
| `listQuestions(type?, keyword?)` | `GET /api/questions` | `type`/`keyword` sent as query params when set |
| `getQuestion(id)` | `GET /api/questions/{id}` | |
| `createQuestion(values)` | `POST /api/questions` | `values: QuestionFormValues` |
| `updateQuestion(id, values)` | `PUT /api/questions/{id}` | |
| `deleteQuestion(id)` | `DELETE /api/questions/{id}` | backend returns 409 `QUESTION_IN_USE` if a questionnaire still references it - surfaced to the user as-is |

Types (`shared/types/question.ts`) mirror the backend's
`survey.api.dto` records field-for-field: `QuestionResponse`,
`QuestionOptionResponse`, `QuestionSummaryResponse`. `QuestionFormValues` is
the frontend-only shape the create/edit form works with (request-shaped, no
server-generated fields).

## 5. Questionnaire API (FR-36~39)

File: `researcher/questionnaire/api/questionnaireApi.ts`. Backend:
`com.cs_42_3.surveyplatformbackend.questionnaire.api.QuestionnaireController`.
Design background: `SurveyPlatformBackend/docs/questionnaire-module.md` and
`fr38-branch-rules-design.md`.

| Function | Real endpoint | Notes |
|---|---|---|
| `getQuestionnaire(studyId)` | `GET /api/studies/{studyId}/questionnaire` | returns an empty structure (`id: null`) if nothing saved yet |
| `saveQuestionnaire(studyId, request)` | `PUT /api/studies/{studyId}/questionnaire` | full replace - always sends the *entire* item list, not a delta |

`SaveQuestionnaireRequest`'s branch rules reference their jump target by
**array index** into the `items` list being saved, matching the backend
contract exactly. Reordering items client-side would silently break that,
so the editor never works with array indices directly - it keeps a stable
per-row `clientId` (`QuestionnaireEditorItem`/`QuestionnaireEditorBranchRule`
in `shared/types/questionnaire.ts`) and only resolves `clientId → index`
right before calling `saveQuestionnaire`. See
`QuestionnaireEditorPage.tsx` for both conversions (load: index → clientId;
save: clientId → index).

Branch rules are only offered for `SINGLE_CHOICE`/`SCALE` questions,
matching the backend's FR-38 eligibility rule (see `BranchRuleEditor.tsx`).

## 6. Participant session API (M5, UC-28~33) - proposed contract

File: `participant/session/api/sessionApi.ts` and
`participant/session/types.ts`. **No backend exists for this yet** (FR-41~46
are unimplemented), so there is no real-endpoint column here - every
function only has a mock branch, and calling with `USE_MOCK_API = false`
throws `"Real participant-session API is not implemented yet."` on purpose.

| Function | Proposed purpose |
|---|---|
| `createSession(studyId)` | UC-28: start a session |
| `giveConsent(sessionId)` | UC-29 |
| `getFeed()` | UC-30 - placeholder content only, real feed is M3's job |
| `getQuestionnaireForSession(studyId)` | fetch the questionnaire to run |
| `submitAnswer(sessionId, answer)` | record one answer |
| `completeSession(sessionId)` | UC-33 |

Treat `types.ts`'s shapes (`ParticipantSession`, `AnswerSubmission`,
`SessionProgress`) as a starting point to negotiate with whoever builds the
M5 backend, not a confirmed contract.

Two things worth knowing when that backend lands:

- **Branch resolution should move server-side.** Right now
  `participant/session/logic/resolveNextItem.ts` runs the FR-38 lookup
  (`questionnaire_branch_rules`) client-side, reading the *live draft*
  questionnaire, purely so this UI could be demoed end-to-end. The real
  flow should have the backend resolve the next item (ideally against a
  frozen publish snapshot, which doesn't exist yet either - see
  `questionnaire-module.md` section 5) and return it, at which point this
  function becomes redundant or, at most, a client-side preview helper.
- **Progress resume (UC-32) is client-only.** `SessionContext.tsx`
  persists progress to `localStorage` (`loadProgress`/`saveProgress`) as a
  stand-in for a real FR-45 endpoint. Swap those two functions for real API
  calls once one exists; everything else in the context stays the same.

## 7. Known gaps / things to check before relying on this further

- Question bank and questionnaire mock data live in memory
  (`mockQuestionBank.ts`, `mockQuestionnaire.ts`) and reset on every page
  reload - fine for manual testing, not for anything you need to persist.
- The questionnaire editor can only *select* questions that already exist
  in the bank; creating a new question still means leaving the page for
  `/researcher/questions/new` first (no inline "new question" flow yet).
- No study-management UI exists (M2), so every researcher route hardcodes
  or falls back to a `demo-study` id instead of a real one.
- Cycle detection across multiple branch rules (A → B → C → A) isn't
  implemented on either side - see `fr38-branch-rules-design.md` section 2.4.
