# M4 API Contract v1

Status: frozen for M4 frontend integration  
Scope: question bank, questionnaire authoring, FR38 branches, publication validation, and published snapshots  
Source of truth: the DTOs, controllers, and exception handlers in the commit containing this document

## 1. Scope and ownership

M4 backend code is under `com.cs_42_3.surveyplatformbackend.survey`, with
questionnaire composition and snapshots below `survey.questionnaire`.

This contract covers:

- FR32-FR35 reusable question-bank CRUD.
- FR36-FR37 one ordered questionnaire per Study.
- FR38 conditional branching for `SINGLE_CHOICE` and `SCALE`.
- M4 publication readiness checks and immutable snapshots.
- Owner-facing reads of live drafts and published snapshots.

This contract does not provide participant sessions, consent, answer submission,
progress restoration, or completion. Those remain M5. The snapshot branch
resolver is an internal backend service for reuse by M5 and is not a public REST
endpoint in v1.

## 2. Runtime conventions

| Item | Contract |
|---|---|
| Local backend base URL | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Authentication | `Authorization: Bearer <JWT>` |
| Required role | `RESEARCHER` |
| Content type | `application/json` |
| UUID representation | JSON string |
| Timestamps | UTC ISO-8601 strings |
| Frontend local origin allowed by CORS | `http://localhost:5173` |

The existing frontend Axios client reads the JWT from
`localStorage.researcherToken`. All M4 researcher endpoints require that token.

## 3. Stable enumerations

### 3.1 QuestionType

```text
SINGLE_CHOICE
MULTI_CHOICE
SCALE
TEXT
```

No additional question types are part of M4 v1.

### 3.2 QuestionnaireContentSource

```text
LIVE_DRAFT
PUBLISHED_SNAPSHOT
```

### 3.3 QuestionnaireItemReferenceStatus

```text
VALID
MISSING_QUESTION
```

## 4. Common error envelope

M4 business and validation errors use one envelope:

```json
{
  "code": "QUESTIONNAIRE_VALIDATION_ERROR",
  "message": "Questionnaire contains invalid branch rules",
  "timestamp": "2026-10-03T04:00:00Z",
  "path": "/api/studies/11111111-1111-1111-1111-111111111111/questionnaire",
  "details": [
    {
      "field": "items[0].branchRules[0]",
      "index": 0,
      "itemId": "22222222-2222-2222-2222-222222222222",
      "ruleIndex": 0,
      "code": "INVALID_OPTION_TRIGGER",
      "message": "Branch option does not belong to the source question"
    }
  ]
}
```

Frontend rules:

- Branch on `code`, never on the English `message`.
- `details` is always an array and may be empty.
- Use `details[].field`, `index`, `itemId`, and `ruleIndex` to attach an error to
  the corresponding editor row.
- HTTP 401 means the JWT is missing, invalid, or expired.
- HTTP 403 means the authenticated user lacks the required role.

## 5. Question-bank API

### 5.1 List questions

```http
GET /api/questions?page=0&size=20&type=SINGLE_CHOICE&search=consent
```

Query parameters:

| Name | Required | Contract |
|---|---|---|
| `page` | no | Zero-based; default `0`; must be nonnegative |
| `size` | no | Default `20`; range `1..100` |
| `type` | no | Exact `QuestionType` value |
| `search` | no | Case-insensitive literal substring of question text |

`search` is the field name. The old frontend field `keyword` is not accepted as
the M4 search contract.

Response `200`:

```json
{
  "content": [
    {
      "questionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "type": "SINGLE_CHOICE",
      "questionText": "Do you consent?",
      "updatedAt": "2026-10-03T04:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Ordering is `updatedAt DESC`, then `questionId DESC`.

### 5.2 Get one question

```http
GET /api/questions/{questionId}
```

Response `200`:

```json
{
  "questionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "type": "SINGLE_CHOICE",
  "questionText": "Choose one option",
  "required": true,
  "options": [
    {
      "optionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      "optionText": "Yes",
      "optionOrder": 0
    },
    {
      "optionId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
      "optionText": "No",
      "optionOrder": 1
    }
  ],
  "scaleMin": null,
  "scaleMax": null,
  "scaleMinLabel": null,
  "scaleMaxLabel": null,
  "createdAt": "2026-10-03T03:00:00Z",
  "updatedAt": "2026-10-03T04:00:00Z"
}
```

Missing or foreign-owned IDs return `404 QUESTION_NOT_FOUND`.

### 5.3 Create a question

```http
POST /api/questions
```

Choice example:

```json
{
  "type": "SINGLE_CHOICE",
  "questionText": "Choose one option",
  "required": true,
  "options": [
    { "optionText": "Yes" },
    { "optionText": "No" }
  ],
  "scaleMin": null,
  "scaleMax": null,
  "scaleMinLabel": null,
  "scaleMaxLabel": null
}
```

Scale example:

```json
{
  "type": "SCALE",
  "questionText": "How satisfied are you?",
  "required": false,
  "options": [],
  "scaleMin": 1,
  "scaleMax": 5,
  "scaleMinLabel": "Not satisfied",
  "scaleMaxLabel": "Very satisfied"
}
```

Type rules:

| Type | Options | Scale fields | Conditional branches |
|---|---|---|---|
| `SINGLE_CHOICE` | At least two nonblank options | ignored/returned null | allowed by `optionId` |
| `MULTI_CHOICE` | At least two nonblank options | ignored/returned null | forbidden |
| `SCALE` | returned as empty array | `scaleMin` and `scaleMax` required; min < max | allowed by integer value |
| `TEXT` | returned as empty array | returned null | forbidden |

Create requests must omit `optionId` or send it as `null`; option identities are
server-generated. Success returns `201`, a `Location` header, and the complete
`QuestionResponse`.

### 5.4 Update a question

```http
PUT /api/questions/{questionId}
```

The update replaces all editable question fields. Example:

```json
{
  "type": "SINGLE_CHOICE",
  "questionText": "Choose one updated option",
  "required": true,
  "options": [
    {
      "optionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      "optionText": "Yes, updated"
    },
    {
      "optionId": null,
      "optionText": "Maybe"
    }
  ],
  "scaleMin": null,
  "scaleMax": null,
  "scaleMinLabel": null,
  "scaleMaxLabel": null,
  "replaceAllOptions": false
}
```

Rules:

- Retain the `optionId` of every unchanged option.
- `optionId: null` adds a new option.
- An option ID must belong to the question being updated.
- Duplicate option IDs are rejected.
- If no existing option identity is retained, explicitly send
  `replaceAllOptions: true`.
- Draft questionnaires immediately read the latest question content.
- Published questionnaires remain unchanged because they read their snapshot.

Success returns `200` and the complete `QuestionResponse`.

### 5.5 Delete a question

```http
DELETE /api/questions/{questionId}
```

Success returns `204` with no body.

Deleting a question is allowed even when a draft or published questionnaire once
used it:

- Published snapshots remain unchanged.
- A draft keeps its stable item but returns `missing: true`,
  `referenceStatus: MISSING_QUESTION`, and `question: null` until repaired.
- The draft cannot be published while the missing reference remains.

## 6. Questionnaire API

There is at most one questionnaire per Study.

### 6.1 Get a questionnaire

```http
GET /api/studies/{studyId}/questionnaire
```

Behavior by Study state:

| Study state | Response source |
|---|---|
| `DRAFT` | Current questionnaire plus live question-bank data |
| `COLLECTING` | Immutable publication snapshot |
| `CLOSED` | Immutable publication snapshot |

An owned DRAFT with no saved questionnaire returns:

```text
404 QUESTIONNAIRE_NOT_FOUND
```

The frontend may translate only that specific error code into an unsaved empty
editor. It must not convert every 404 into an empty questionnaire.

Draft response example:

```json
{
  "questionnaireId": "10000000-0000-0000-0000-000000000001",
  "snapshotId": null,
  "studyId": "10000000-0000-0000-0000-000000000002",
  "contentSource": "LIVE_DRAFT",
  "items": [
    {
      "itemId": "10000000-0000-0000-0000-000000000003",
      "position": 0,
      "missing": false,
      "question": {
        "questionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "type": "SINGLE_CHOICE",
        "questionText": "Choose one option",
        "required": true,
        "options": [
          {
            "optionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            "optionText": "Yes",
            "optionOrder": 0
          },
          {
            "optionId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
            "optionText": "No",
            "optionOrder": 1
          }
        ],
        "scaleMin": null,
        "scaleMax": null,
        "scaleMinLabel": null,
        "scaleMaxLabel": null,
        "createdAt": "2026-10-03T03:00:00Z",
        "updatedAt": "2026-10-03T04:00:00Z"
      },
      "referenceStatus": "VALID",
      "branchRules": [
        {
          "id": "10000000-0000-0000-0000-000000000004",
          "sourceOptionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
          "sourceScaleValue": null,
          "targetItemId": "10000000-0000-0000-0000-000000000005",
          "targetPosition": 1
        }
      ],
      "defaultNextItemId": "10000000-0000-0000-0000-000000000005"
    },
    {
      "itemId": "10000000-0000-0000-0000-000000000005",
      "position": 1,
      "missing": false,
      "question": {
        "questionId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
        "type": "TEXT",
        "questionText": "Please explain your choice",
        "required": false,
        "options": [],
        "scaleMin": null,
        "scaleMax": null,
        "scaleMinLabel": null,
        "scaleMaxLabel": null,
        "createdAt": "2026-10-03T03:00:00Z",
        "updatedAt": "2026-10-03T04:00:00Z"
      },
      "referenceStatus": "VALID",
      "branchRules": [],
      "defaultNextItemId": null
    }
  ],
  "version": 3,
  "updatedAt": "2026-10-03T04:00:00Z",
  "publishedAt": null,
  "valid": true,
  "validationIssues": []
}
```

`validationIssues` uses:

```json
{
  "code": "MISSING_QUESTION",
  "itemIndex": 0,
  "itemId": "10000000-0000-0000-0000-000000000003",
  "ruleIndex": null,
  "message": "Questionnaire item no longer resolves to an owned question"
}
```

### 6.2 Save the complete draft

```http
PUT /api/studies/{studyId}/questionnaire
```

First save:

```json
{
  "expectedVersion": null,
  "items": [
    {
      "itemId": null,
      "questionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "branchRules": [
        {
          "sourceOptionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
          "sourceScaleValue": null,
          "targetPosition": 1
        }
      ]
    },
    {
      "itemId": null,
      "questionId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
      "branchRules": []
    }
  ]
}
```

Subsequent save:

```json
{
  "expectedVersion": 3,
  "items": [
    {
      "itemId": "10000000-0000-0000-0000-000000000003",
      "questionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "branchRules": []
    }
  ]
}
```

Save semantics:

- `items` is the entire desired final questionnaire, not a delta.
- The array order is the display order.
- An empty array is a valid saved draft, but cannot be published.
- Include the `expectedVersion` key on every request.
- Use `null` only for first creation or its exact replay.
- Later saves use the `version` returned by the latest successful GET/PUT.
- Preserve existing `itemId` values while reordering or retaining items.
- A null `itemId` adds an item.
- Each question may appear only once in a questionnaire.
- Exact retries are idempotent and do not advance `version`.
- A stale version with different content returns
  `409 QUESTIONNAIRE_VERSION_CONFLICT`; reload before retrying.
- Only an owned `DRAFT` Study can be changed. Other states return
  `409 QUESTIONNAIRE_LOCKED`.

The first creation returns `201` and a `Location` header. Updates and exact
replays return `200`. Both return `QuestionnaireResponse`.

## 7. FR38 branch contract

### 7.1 Supported source types

| Source type | Trigger field |
|---|---|
| `SINGLE_CHOICE` | `sourceOptionId` |
| `SCALE` | `sourceScaleValue` |
| `MULTI_CHOICE` | branches forbidden |
| `TEXT` | branches forbidden |

Exactly one trigger field must be non-null.

### 7.2 Target representation

Save requests use `targetPosition`, the zero-based index in the complete final
`items` array. Responses include both the stable `targetItemId` and current
`targetPosition`.

The frontend should keep a stable client-side row key and convert it to
`targetPosition` only immediately before saving. Never keep a branch target only
as an array index while the user is reordering rows.

### 7.3 Default transition

- If no conditional rule matches, flow continues to the next item.
- The last item's default transition is END.
- `defaultNextItemId` exposes this implicit path in GET/PUT responses.
- An optional branching question always preserves an unanswered default path.

### 7.4 Rejected graphs

The backend rejects:

- Branches on `MULTI_CHOICE` or `TEXT`.
- Missing or multiple trigger fields.
- Options not belonging to the source question.
- Scale values outside the source range.
- Duplicate triggers from one source item.
- Target positions outside the final array.
- Self-loops.
- Cycles.
- Unreachable items.
- Required items without a valid terminating transition.

## 8. Publication and snapshot contract

```http
POST /api/studies/{studyId}/publish
```

The `version` below is the Study version, not the questionnaire version:

```json
{
  "version": 4
}
```

When `questionnaireEnabled` is false, publication does not require or create a
questionnaire snapshot.

When it is true, publication requires:

- A saved, nonempty questionnaire.
- No missing question references.
- Valid data for all four question types.
- Valid branch triggers and targets.
- Acyclic, reachable, terminating flow.

Questionnaire validation, snapshot creation, and the Study transition to
`COLLECTING` occur in one transaction. Failure rolls everything back.

Success returns `200 StudyResponse`, including the refreshed Study `version`,
`status: COLLECTING`, `publishedAt`, and `participationUrl`.

After publication, the owner-facing questionnaire GET returns:

- `contentSource: PUBLISHED_SNAPSHOT`
- non-null `snapshotId`
- non-null `publishedAt`
- the same stable item, option, and branch identifiers captured at publication
- content unaffected by later question-bank edits or deletions

There is no public participant questionnaire/session endpoint in M4 v1.

## 9. Error codes the frontend must handle

### 9.1 Top-level codes

| HTTP | Code | Frontend behavior |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | Show field/request validation feedback |
| 400 | `MALFORMED_REQUEST` | Report unsupported enum, malformed JSON, path, or query value |
| 400 | `INVALID_PAGINATION` | Correct page/size |
| 400 | `QUESTION_VALIDATION_ERROR` | Show question type/data error |
| 400 | `CREATE_OPTION_ID_NOT_ALLOWED` | Remove create-time option IDs |
| 400 | `DUPLICATE_OPTION_ID` | Fix duplicated option rows |
| 400 | `INVALID_OPTION_REFERENCE` | Reload question; option ID is not owned by it |
| 400 | `OPTION_IDENTITIES_REQUIRED` | Retain IDs or confirm `replaceAllOptions` |
| 400 | `QUESTIONNAIRE_VALIDATION_ERROR` | Render `details` against items/rules |
| 404 | `QUESTION_NOT_FOUND` | Resource is absent or not owned |
| 404 | `STUDY_NOT_FOUND` | Resource is absent or not owned |
| 404 | `QUESTIONNAIRE_NOT_FOUND` | For a DRAFT editor only, initialize unsaved local state |
| 409 | `QUESTIONNAIRE_LOCKED` | Switch editor to read-only/reload Study status |
| 409 | `QUESTIONNAIRE_VERSION_CONFLICT` | Reload before retrying |
| 409 | `CONCURRENT_QUESTIONNAIRE_CREATION` | Reload the newly created questionnaire |
| 409 | `QUESTION_REFERENCE_CHANGED` | Reload questions and questionnaire |
| 409 | `STUDY_NOT_PUBLISHABLE` | Refresh Study lifecycle state |
| 409 | `STUDY_VERSION_CONFLICT` | Reload Study and retry intentionally |
| 409 | `FEED_NOT_READY` | Complete/fix the Study feed |
| 409 | `QUESTIONNAIRE_PUBLICATION_VALIDATION_ERROR` | Render publication `details` in the editor |

### 9.2 Common detail codes

```text
ITEM_REQUIRED
QUESTION_ID_REQUIRED
DUPLICATE_ITEM_ID
DUPLICATE_QUESTION_REFERENCE
INVALID_QUESTION_REFERENCE
INVALID_ITEM_REFERENCE
BRANCH_RULE_REQUIRED
BRANCH_TRIGGER_REQUIRED
OPTION_TRIGGER_REQUIRED
SCALE_TRIGGER_REQUIRED
BRANCH_TYPE_UNSUPPORTED
INVALID_OPTION_TRIGGER
SCALE_TRIGGER_OUT_OF_RANGE
DUPLICATE_BRANCH_TRIGGER
BRANCH_TARGET_OUT_OF_RANGE
INVALID_BRANCH_TARGET
BRANCH_SELF_LOOP
BRANCH_CYCLE
UNREACHABLE_ITEM
NON_TERMINATING_ITEM
MISSING_QUESTION
INVALID_QUESTION_DATA
QUESTIONNAIRE_REQUIRED
QUESTIONNAIRE_EMPTY
```

## 10. Required changes from the old frontend mock contract

The old `feat/m4-m5-frontend` guide and mocks are not the source of truth. Update
the frontend as follows:

| Old assumption | M4 v1 contract |
|---|---|
| `keyword` query parameter | `search` |
| Question `id` | `questionId` |
| Option without stable identity | retain `optionId` on update |
| Questionnaire response `id` | `questionnaireId` |
| Missing questionnaire returns an empty object | `404 QUESTIONNAIRE_NOT_FOUND` |
| Save without version | always include `expectedVersion` |
| Rows can be recreated during reorder | preserve `itemId` |
| Any referenced question cannot be deleted | deletion is allowed; draft exposes missing reference; snapshot stays stable |
| Client resolves participant flow from live draft | M5 must eventually use backend session logic over a published snapshot |
| Draft and publication are indistinguishable | use `contentSource` and `snapshotId` |

## 11. Recommended frontend integration order

1. Reuse the existing Axios bearer-token interceptor.
2. Replace question-bank mocks with list/detail/create/update/delete calls.
3. Map the stable `QuestionPageResponse` rather than Spring Page internals.
4. Replace hard-coded `demo-study` with the selected real Study UUID.
5. Implement questionnaire GET, treating only `QUESTIONNAIRE_NOT_FOUND` as an
   unsaved draft.
6. Implement full-state PUT with `expectedVersion` and stable item IDs.
7. Integrate single-choice branches.
8. Integrate scale branches.
9. Display `validationIssues` and `SurveyErrorResponse.details` at item/rule level.
10. Integrate publication and switch the editor to published read-only state.
11. Keep M5 participant session functions on mocks until an M5 contract exists.

## 12. Minimum joint acceptance scenarios

- List, search, filter, create, edit, and delete all four question types.
- Preserve option IDs through edits; deliberately replace them only with
  `replaceAllOptions: true`.
- Create an empty draft, then add and reorder items while preserving item IDs.
- Save valid single-choice and scale branches.
- Confirm multi-choice/text branches are rejected.
- Confirm invalid triggers, self-loops, cycles, unreachable items, and stale
  versions are rendered from structured error codes.
- Delete a question used by a draft and show the missing placeholder.
- Reject publication for missing, empty, or invalid questionnaires.
- Publish a valid Study and read `PUBLISHED_SNAPSHOT`.
- Edit/delete source questions after publication and confirm the snapshot does not
  change.

## 13. Verified backend gate

The M4 backend was verified with Docker-backed PostgreSQL Testcontainers:

```text
Tests run: 194, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
PostgreSQL 17.11
Flyway V1-V13 applied successfully from an empty schema
```

The standard build and performance profile commands are:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pperformance test
```
