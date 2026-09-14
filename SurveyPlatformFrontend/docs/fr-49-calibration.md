# FR-49: eye-tracking calibration logic

This module implements the calibration logic in UC-35/FR-49. The interaction team owns the page; M5 owns consent and participant sessions; FR-48/FR-51 own durable record delivery; FR-50 owns browsing gaze events and widget attribution.

It does not add a participant route, a WebGazer script loader, an HTTP endpoint, a database migration, or an export format. The current repository has none of the M5/FR-48/FR-50/FR-51 integration modules, so passing these tests is not a claim that the whole participant application is integrated.

## Validation

Use the version declared in `packageManager` (pnpm 12.4.1):

```sh
pnpm install --frozen-lockfile
pnpm build
pnpm lint
pnpm typecheck:eyetracking
pnpm test
pnpm exec playwright install chromium
pnpm test:browser
```

To use an installed Chrome instead of downloading Chromium:

```sh
# macOS / Linux
FR49_BROWSER_CHANNEL=chrome pnpm test:browser
```

The browser harness uses canvas-generated **real MediaStreamTracks** and a WebGazer contract double. It never requests the user's camera. These tests cover resource cleanup, initialization races, result handoff and stage separation. They do not measure real WebGazer accuracy or verify a real permission prompt, backend persistence or export.

`pnpm lint` applies the existing JS rules and additional TypeScript rules scoped to this module, its tests and test configuration. `typecheck:eyetracking` additionally enables strict TypeScript checks without changing unrelated application code. Vitest and Playwright are pinned development dependencies; the module adds no production dependency or bundle-time WebGazer import.

## Requirement mapping

| UC-35 / FR-49 behavior                                           | Implementation                                                                                          | Automated evidence                                                             |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Only after consent, with eye tracking enabled                    | Required immutable session context; start/probe reject before touching the engine otherwise             | Controller precondition tests                                                  |
| Nine-point calibration                                           | Nine viewport-relative targets, five clicks per target, optional held-out validation                    | Controller normal flow + browser complete-run test                             |
| Record quality with the session                                  | Immutable attempt with session ID, stable attempt UUID, ordinal, residuals, coverage and policy version | Result/recording tests; **durable session queue still belongs to FR-48/FR-51** |
| Poor/abandoned calibration still tracks with low-quality markers | Engine stays running; explicit quality/reasons; ownership transfers to FR-50                            | Low-quality unit/browser handoff tests                                         |
| Camera unavailable never blocks browsing                         | UNAVAILABLE result with reason, cancellable/bounded initialization; no camera on preflight denial       | Denied/missing API/timeout/cancellation tests                                  |
| Limited, optional retries                                        | Configurable total attempts; retry resets regression; quality never gates browsing                      | Retry/count tests                                                              |
| Calibration produces no browsing gaze events                     | Single prediction consumer; detach at completion; FR-50 explicitly installs its consumer after handoff  | Browser stage separation test with the same engine                             |

## Minimal integration

Import from `src/eyetracking/index.ts` rather than internal files. The following is a **host integration sketch**, not an existing route:

```ts
import {
  CalibrationController,
  WebGazerGazeEngine,
  isRetryWorthOffering,
  type CalibrationRecordSink,
} from '../eyetracking'

// M5 supplies a valid anonymous session and records consent before reaching here.
// Return to the appropriate M5 flow without constructing/starting an engine if
// consent is absent or the study did not enable eye tracking.
const sink: CalibrationRecordSink = {
  record: (attempt) => sessionEventQueue.durablyAccept(attempt),
}
const engine = new WebGazerGazeEngine({ webgazer })
const controller = new CalibrationController({
  engine,
  session: { sessionId, consentGiven: true, eyeTrackingEnabled: true },
  sink,
  maxAttempts: 2, // total attempts: first + one retry, not two retries
  previousAttempts: sessionMetadata.previousCalibrationAttempts,
  onIssue: (issue) => reportOperationalIssue(issue),
})

let attempt = await controller.start({ onTargetChange, onProgress })
// The UI renders targets from callbacks while awaiting start(). Each dot click
// calls controller.registerClick(). Skip calls controller.skip().
while (
  attempt.trackingAvailable &&
  isRetryWorthOffering(attempt.quality) &&
  controller.attemptsRemaining > 0 &&
  (await participantChoosesRetry())
) {
  attempt = await controller.retry({ onTargetChange, onProgress })
}

if (attempt.trackingAvailable) {
  const browsingEngine = controller.handoffToTracking()
  browsingEngine.setSampleListener((sample) => {
    // FR-50 adds the remaining event fields and attributes the widget.
    browsingCollector.accept(sample, {
      sessionId: attempt.sessionId,
      calibrationAttemptId: attempt.attemptId,
      calibrationQuality: attempt.quality,
      calibrationLowQualityReasons: attempt.lowQualityReasons,
    })
  })
  // The session owner must call browsingEngine.stop() at session end.
}
// All terminal calibration outcomes allow browsing/questionnaire progression.
goToBrowsing()
```

`webgazer`, session metadata, the durable queue, UI callbacks, browser collector and navigation in the sketch are supplied by other modules. A returned record or a queue failure must never become a navigation gate. Programmer errors (invalid configuration, absent consent, reuse after disposal, duplicate start, attempt limit) reject; device/runtime failures resolve with a reasoned attempt.

The controller calls the engine's non-prompting probe as part of every start; callers do not need a separate `unavailableAttempt()` path. `probeAvailability()` is an optional advisory UI query; it does not consume an attempt. Empty/restricted pre-permission device lists are inconclusive. A denied permission or absent API is sufficient to skip initialization; `begin()` is authoritative for other device failures.

### Page and session ownership

- Create one controller/engine per participant session, not on every render. React StrictMode effects must not repeatedly reuse a disposed controller. Use explicit session ownership outside a transient page effect.
- `start()` enters STARTING synchronously, then returns a promise that completes only when the attempt ends. Do not wait for that promise before rendering a target.
- Targets and samples use **viewport CSS pixels**. Draw a fixed-position target around its centre. Pass the actual centre to `registerClick(x, y)` on the first click if necessary; do not pass cursor coordinates.
- A target's centre is fixed after its first counted click. Call `checkViewport()` on resize/zoom; clicks also check dimensions/DPR. A changed viewport ends the measurement with a low-quality marker while preserving a usable engine.
- `skip()` during RUNNING keeps the model and produces ABANDONED. `skip()` during STARTING cancels an unavailable initialization and produces UNAVAILABLE/INIT_CANCELLED; any late stream is released. It cannot promise tracking before camera initialization succeeds.
- `dispose()` is terminal and idempotent. Before handoff it stops the engine and resolves an outstanding attempt. After `handoffToTracking()` it disposes only the controller; FR-50 owns engine shutdown. Detach page resize/click listeners separately.
- `handoffToTracking()` requires FINISHED and a usable engine, transfers ownership once, and never resets training. It does not emit gaze events itself. Call `setSampleListener()` to switch consumers; do not call `begin()` again.
- On consent withdrawal/session exit, the current engine owner must stop the engine. Consent revocation is an M5 signal, not something this module can infer from navigation.

## Quality policy and data contract

Every attempt has `schemaVersion: 1`, `algorithmVersion: 'residual-v2'`, `sessionId`, a UUID `attemptId`, and a session-local `attemptNumber`. Use `(sessionId, attemptId)` for idempotent record acceptance. A delivery retry uses the exact same immutable object; a new calibration retry creates a new attempt ID.

The outcome, quality and camera availability are independent:

| Field                                   | Meaning                                                                                                                                                                 |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| outcome                                 | COMPLETED: all clicks completed; ABANDONED: skipped/invalidated; UNAVAILABLE: initialization did not become usable                                                      |
| trackingAvailable                       | Whether the finished attempt can hand off a usable model; the only flag callers should use to decide whether to start gaze collection                                   |
| unavailableReason                       | NO_CAMERA_API, NO_CAMERA_DEVICE, PERMISSION_DENIED, INIT_FAILED, INIT_CANCELLED or INIT_TIMEOUT; can accompany ABANDONED if the engine failed/was disposed during a run |
| quality                                 | GOOD, FAIR, POOR or NOT_MEASURED; a low/unmeasured result never disables tracking                                                                                       |
| qualityBasis                            | CALIBRATION, VALIDATION or NONE; requested validation is never silently replaced with a better training score                                                           |
| lowQualityReasons                       | ABANDONED, NOT_MEASURED, INSUFFICIENT_SAMPLES, HIGH_RESIDUAL, VIEWPORT_CHANGED and/or ENGINE_UNAVAILABLE                                                                |
| residualMedianPx                        | Median of measured training-target residuals, including partial targets; not a final independent accuracy estimate                                                      |
| validationMedianPx                      | Median of held-out validation residuals, or null                                                                                                                        |
| \*TargetsMeasured                       | Targets with at least `minSamplesPerTarget` independent predictions                                                                                                     |
| \*TargetsRequired                       | Number of targets in the applicable layout                                                                                                                              |
| qualityThresholds / minSamplesPerTarget | The actual policy parameters, recorded with the result                                                                                                                  |
| startedAt / finishedAt                  | UTC ISO timestamps; finishedAt is derived from start wall time + monotonic duration, so a wall-clock adjustment cannot reverse them                                     |

Each point retains kind/index, actual target centre, trimmed-mean predicted centre, Euclidean residual in CSS pixels and the **distinct pre-trim sample count**. A callback is consumed at most once. Sample values/times must be finite, not future-dated, produced after the target settling boundary and less than 700 ms old. Switching targets clears the previous sample. Early clicks do not train or advance while the default 200 ms settling interval is active.

For each coordinate separately, sort the samples and discard the minimum/maximum if there are at least three. Compute the distance from this aggregate prediction to the target, then the median across targets. This is not the average per-frame error and does not measure dispersion. Individual predictions are discarded when the attempt ends; they are not browsing gaze events.

By default, the score is the training residual median. If validation targets are configured, use their median instead; validation clicks do not train. GOOD is at most 5% of the viewport short edge, FAIR is at most 10%, otherwise POOR. Abandoned/invalidated/insufficiently measured attempts cannot be GOOD or FAIR, even if their partial residual is zero. No usable scoring samples means NOT_MEASURED. Requested validation with no valid samples remains NOT_MEASURED.

Defaults (five clicks, three required independent samples per target, 200 ms settling, 5%/10%, total two attempts, no extra validation targets) are **provisional product/measurement choices**, not numeric values mandated by FR-49 or empirically validated scientific thresholds. Confirm them with the interaction/research team. Changing them should preserve policy/version metadata and corresponding tests. The controller bounds attempts to 1–10 and optional validation targets to 0–9 to keep per-session memory bounded; initialization defaults to 30 seconds and can be configured up to 120 seconds.

## Record acceptance and recovery boundary

`controller.attempts` retains all immutable records in the current controller. `currentAttempt` is the **last**, not the historically best, because retries replace the trained model. `pendingRecords` retains results until a sink accepts them. `issues` is a bounded diagnostic history (last 50); it is not a collection of participant events.

`CalibrationRecordSink.record(attempt)` may resolve synchronously or asynchronously. Resolve only after durable acceptance into the shared FR-48/FR-51 queue, not merely after starting a fetch. Failures are observable via `issues`/`onIssue`, retain the same pending result and do not reject the participant's calibration promise. `retryPendingRecords()` retries only failed/not-accepted records and does not duplicate an in-flight call. A hung sink remains pending and does not block browsing; the sink owns its timeout/retry policy.

The supplied `collectCalibrationAttempts(sessionId)` is explicitly **memory-only**, checks session identity and deduplicates attempt IDs. It is suitable for tests/development, not a production persistence substitute. The controller also retains pending results only in memory. M5/FR-48/FR-51 must:

1. Bind and verify the actual session/authentication at the server boundary.
2. Implement durable queue acceptance and restore the previous attempt count after refresh.
3. Preserve pending records and attempt IDs across navigation, offline periods and refresh.
4. Persist/export quality and camera availability alongside session data.
5. Ensure only the browsing phase feeds gaze events into the shared event pipeline.

Do not use the old standalone `/api/participant/sessions/{id}/calibration` API or old calibration SQL tables as an implied dependency of this module. That historical design is not part of this change.

## WebGazer adapter contract

The adapter accepts an injected WebGazer instance, currently designed against the locally inspected WebGazer **3.5.3** public API and ridge regression. Loading/bundling the package is the host's responsibility; this feature does not silently download a script or model. Before choosing another version, rerun the adapter contract checks and real-camera acceptance.

Key behavior:

- Only one session engine may own a given WebGazer singleton at a time.
- `saveDataAcrossSessions(false)` disables model persistence/reload. Automatic mouse training is mandatory to disable; it is removed during initialization and before handoff. Prediction listener inputs are finite-checked and use the same monotonic clock as the controller.
- **Do not call `clearData()` to reset calibration.** In WebGazer 3.5.3 it calls origin-wide `localforage.clear()`, potentially deleting unrelated offline session data. The adapter selects ridge and calls `getRegression().init()` to reset in-memory regression/Kalman state without touching shared storage. These methods are validated as required dependencies.
- `stop()` stops **all tracks** found on WebGazer's owned video element, calls `stopVideo()` as a fallback for partially initialized streams, clears prediction/mouse listeners, and removes WebGazer's UI via `end()`. In 3.5.3, `end()` alone only pauses/removes DOM and does not stop the stream.
- A scoped initialization observer captures a late video stream even if a pending model initialization never finishes. Cancellation marks the adapter terminal; subsequent completion cleans up again and cannot reactivate predictions. A cancelled browser permission request itself cannot be forcibly dismissed by this adapter.
- `params.videoElementId`, or the 3.5.3 default `webgazerVideoFeed`, identifies the owned element. The host must not change it mid-initialization or share the singleton across sessions.

The browser tests exercise the lifecycle contract using a double and actual media tracks; they do not certify every implementation detail of the WebGazer package. Real device/model accuracy, camera permission UX, backend persistence, long-session tracking and export remain integration acceptance items.

## Source map

| File                                   | Responsibility                                                                                 |
| -------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `types/calibration.ts`                 | Result, session, point, quality and diagnostic contracts                                       |
| `calibration/calibrationController.ts` | Consent guards, state machine, targets, sample attribution, quality result and handoff         |
| `calibration/residuals.ts`             | Pure coordinate aggregation and residual arithmetic                                            |
| `calibration/quality.ts`               | Numeric policy validation and threshold grading                                                |
| `calibration/calibrationRecordSink.ts` | Shared queue port and development collector                                                    |
| `engine/gazeEngine.ts`                 | Session engine and prediction-consumer contract                                                |
| `engine/webgazerGazeEngine.ts`         | WebGazer lifecycle/permission/resource adapter                                                 |
| `index.ts`                             | Public exports                                                                                 |
| `tests/eyetracking`                    | Unit/regression tests                                                                          |
| `tests/browser`                        | Isolated development harness and browser lifecycle tests, outside the application routes/build |
