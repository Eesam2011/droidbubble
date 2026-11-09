# Phase 0 and Phase 1: Safety net and fixes

**Goal.** Phase 0 (`S`) builds the safety net every later task is judged against: characterization tests, a screenshot baseline, table tests for the window math, performance baselines and a notification fixture corpus. Nothing the user sees changes. Phase 1 (`B`) then fixes what is broken or risky today, in small PRs: the exported debug receiver, the touch-blocking strip over the status bar, live activities hidden on a fresh install, misfiring heuristics, high contrast, accidental actions, fabricated widget data, battery and main-thread waste, two typography bugs, main-thread parsing and crash loops.

**This is polish, not redesign (PLAN v2 §0).**
- Phase 0 adds tests and test seams only. Its screenshots and bounds become the diff gate for every later task.
- Phase 1 changes behaviour where something is broken. It keeps the squircle shapes, the card layouts and the palette. The only visual diffs allowed are the ones a task names (for example the high-contrast outline, or a placeholder that becomes readable).
- Apple is a quality reference. The fixes here follow Android conventions: heads-up grammar for tuck versus dismiss, the notification's own `contentIntent` for "open", `RECEIVER_NOT_EXPORTED`, and so on.

**Epics:** S1 characterization tests · S2 screenshot baseline · S3 window-geometry math · S4 performance baselines · S5 notification fixtures · B1 security · B2 touch pass-through · B3 live activities on by default · B4 heuristic hardening · B5 high contrast · B6 safe actions · B7 truthful widgets · B8 battery and main-thread wins · B9 typography fixes · B10 parsing off the main thread · B11 crash containment.

**Sources:** `research/06-performance-architecture.md` (§4 Phase 0 and Phase 1, P0-1…P1-9, R4, R5, R7, R8, R11), `research/04-functions.md` (§4 P0-1, P0-3, P1-4, P1-5, P1-11, P2-14, P2-17, §5 R1, R6, R10), `research/05-ux-interaction.md` (§2, §3 P0-1, P1-1, P1-4, P1-5, R1, R8, R9, R11), `research/02-visual-design.md` (R2), `research/01-typography.md` (findings 2 and 5, R4).

**Downstream contracts.** Later phase files rely on these tasks by name. Keep the names, or update those files.
- Phase 7: S5 fixture names (listed in S5.2), `showLiveActivities` (B3.1), hardened regexes (B4.1), the `contentIntent`-first open order (B6.3), the parser dispatcher and sequence numbers (B10.3), breadcrumbs and `runCatching` guards (B11.1, B11.2), and `BuildConfig.DEBUG` (B1.1).
- Phase 8: the unified settings mapper and debounced slider writes (B8.6, B8.7), the tuck rule for calls and alarms (B6.2), the placeholder contrast fix (B5.2), and the `exported="false"` service (B1).

## Conventions for every task in this phase

**Baseline.** These anchors were read on 2026-11-08 from the working tree. The repo is on branch **`main`** (HEAD `8d33466`) with the `island-polish` work **staged but not committed**, not on an `island-polish` branch at `77d266e` as PLAN.md says. Paths are relative to `app/src/main/java/com/droidbubble/app/` unless they start with `app/` or `docs/`. Anchors in `IslandView.kt` match the research reports. Anchors in `IslandOverlayService.kt` and `NotificationReceiverService.kt` have **drifted by about +20 lines** from the research; the numbers below are the current ones. Re-check every anchor before you edit, because earlier tasks in this file move lines.

**Step zero (PLAN §4).** Commit or checkpoint the staged tree first, so each task below is a clean diff. Phase 0 tasks assume a green `./gradlew :app:testDebugUnitTest` on that checkpoint.

**Tests.**
- Unit tests live in `app/src/test/java/com/droidbubble/app/<package>/`. Use plain JUnit when possible.
- Use Robolectric `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")` with `@GraphicsMode(NATIVE)`, matching `IslandOverlayCrashTest.kt:39-40`, whenever Compose, a `Notification` or a `Bundle` is involved.
- `IslandStateManager` is a process singleton. Every test calls `IslandStateManager.resetForTest()` (`island/IslandStateManager.kt:770`) in `@Before` and `@After`, and resets `IslandTokens.highContrast` (`island/IslandTokens.kt:22`).

**Gates once Phase 0 lands.** Every B task must:
- keep the S1 bounds matrix green, or list each intended diff in the PR
- show S2 screenshot diffs that are either zero or exactly the ones the task names
- keep the S4 recomposition count at or below the recorded baseline
- keep the S3 window table green, or flip only the rows the task names

**API levels.** `minSdk = 29`, `targetSdk`/`compileSdk = 36` (`app/build.gradle.kts:13-18`). Guard API 30+ and 33+ symbols. If D1 raises minSdk to 33, the guards go away.

**Privacy.** Breadcrumbs, logs and fixtures never contain message text, names or numbers from real notifications. Use the package name, the kind and the key hash only.

---

## S1 — Characterization tests (state × settings)

### S1.1 — Shared island test harness
- **Goal:** One reusable harness for rendering `IslandOverlayRoot`, settling it, drawing it and reading node bounds. Today these helpers are private to `IslandOverlayCrashTest`.
- **Why:** 06 §4 Phase 0 extends "the Robolectric harness" into a matrix. S1.2, S1.3, S2 and S4.1 all need the same setup, and copy-pasting it would make them drift.
- **Files:**
  - Source of the helpers: `app/src/test/java/com/droidbubble/app/island/IslandOverlayCrashTest.kt:43-170` (rule, `settle`, `draw`, `onState`, `exists`, swipe helpers, fixtures `song`, `message`, `incomingCall`, `recorder`, `timer` at `:48-102`)
  - New: `app/src/test/java/com/droidbubble/app/island/harness/IslandHarness.kt`, `harness/IslandFixtures.kt`
- **Steps:**
  1. Move the fixtures into `IslandFixtures` (an `object` with the same values, plus `progress(pct)`, `alarmRinging`, `timesUp`, `ride`, `score`, built the same way as `LiveActivityTester.kt:53-120`).
  2. Write `IslandHarness(rule)` with `setContent(settings: MutableState<IslandSettings>)`, `settle(ms)`, `draw()`, `onState {}`, `exists(tag)`, `bounds(tag): DpRect?` (via `onAllNodesWithTag(tag)[0].getBoundsInRoot()`), and the pill and card swipe helpers.
  3. Port `IslandOverlayCrashTest` to the harness without changing any test body or assertion.
- **Acceptance criteria:** `IslandOverlayCrashTest` has the same 9 tests, all green, and its file shrinks by the moved helpers. No production file changes.
- **Tests:** the ported `IslandOverlayCrashTest`.
- **Depends on:** step zero.
- **Effort:** S   **Risk:** low — test-only.
- **Ships as:** Test-only.

### S1.2 — Content probes: which activity is drawn where
- **Goal:** Tests can ask which activity is in the pill, the bubble and each card, without reading pixels.
- **Why:** PLAN S1 asks for "the content in each state". Today only three tags exist (`IslandTestTags`, `island/IslandView.kt:381-385`), so a test can't tell a media pill from a timer pill.
- **Files:**
  - `island/IslandView.kt:381-385` (tags), the pill content root inside `:1953-1960`, the bubble content at `:1743`, card 1 and card 2 content roots (around `:1388` and `:1562`)
- **Steps:**
  1. Add `IslandTestTags.content(slot: String, priority: IslandPriority) = "island_${slot}_${priority.name}"` with the slots `pill`, `bubble`, `card1`, `card2`.
  2. Put `Modifier.testTag(...)` on the four content roots. It is a semantics-only modifier, so layout and drawing are unchanged.
  3. Add `harness.contentOf(slot): IslandPriority?`, which finds the one matching tag.
- **Acceptance criteria:** S2 (once it exists) or a manual before/after screenshot shows no pixel change. `contentOf("pill")` returns `MEDIA` with media playing and `POP_NOTIFICATION` for a timer.
- **Tests:** `IslandContentProbeTest`: media → pill MEDIA; media + message → pill MEDIA, bubble POP_NOTIFICATION; expanded media → card1 MEDIA.
- **Depends on:** S1.1.
- **Effort:** S   **Risk:** low — `testTag` adds a semantics node only.
- **Ships as:** Test seam.

### S1.3 — Settled bounds and content matrix
- **Goal:** A recorded table of settled `PILL`, `BUBBLE` and `CARD` bounds and of the content in each slot, across states and settings. Later refactors (F1, M1, V-tasks) must reproduce it, or list their diffs.
- **Why:** 06 §4 Phase 0: "assert the bounds of `PILL`, `BUBBLE` and `CARD` at settle (`getBoundsInRoot`) and which content appears; include dual mode, notch, minimized and hidden, the foreground-app case, ringing call plus flash, and high contrast."
- **Files:** new `app/src/test/java/com/droidbubble/app/island/IslandCharacterizationTest.kt` and `.../island/golden/IslandBoundsGolden.kt` (a Kotlin table, so diffs are readable in review).
- **States (rows):**
  1. idle + `alwaysShowPill`
  2. media playing
  3. media paused
  4. message
  5. download 40 %
  6. voice recorder
  7. timer running
  8. incoming call
  9. incoming call + torch blinking (`IslandOverlayCrashTest.kt:256-269`)
  10. media + message (dual)
  11. media + recorder + download (third activity hidden today)
  12. minimized (`userMinimized`)
  13. hidden (`userHidden`)
  14. media with Spotify as `foregroundPackage`
  15. charging glance
  16. ringer glance
  17. expanded media
  18. expanded message
  19. expanded dual (media + message)
- **Settings (columns):** default; `STYLE_NOTCH`; `widthDp = 220, heightDp = 40`; `expandedScale = 1.0`; high contrast (`IslandTokens.highContrast = true` + `showOutline`); font scale 1.3 (Robolectric qualifier `fontScale`, or `LocalDensity` as `IslandOverlayService.kt:727` does).
- **Steps:**
  1. Parameterize over states × settings: 19 × 6 = 114 cases. Each case sets up the state, calls `settle(1500)`, then records `bounds(PILL|BUBBLE|CARD)` rounded to 0.5 dp, plus `contentOf(...)` for the four slots.
  2. Add a record mode (`-Disland.record=true`) that prints the table as Kotlin source to paste into `IslandBoundsGolden.kt`. The default mode asserts against the table.
  3. Mark the rows that are known bugs with a `knownIssue = "B2.1"`-style field, for example row 14 (the card is hidden but the window still takes touches). Phase 1 tasks flip those rows on purpose.
- **Acceptance criteria:** the table is recorded and committed. Two consecutive runs pass with no diff. The run takes under 2 min on the dev machine.
- **Result (2026-11-08):** recorded and stable, but the run takes about 4 min (231 s) on the 4-core dev machine: each case settles about 100 frames, and today the whole root recomposes every frame (06 P0-1). Expect it to drop after M2. Columns use `showNotifications = true` as their base, so the fresh-install gate rows live in S3.2 and S5.3, not here.
- **Tests:** the test itself.
- **Depends on:** S1.2.
- **Effort:** M   **Risk:** low — test-only. Watch for: `rememberMorphSpeed` uses `System.nanoTime()` (`island/IslandComponents.kt:630-638`), so assert only at settle, never mid-morph.
- **Ships as:** Test-only.

---

## S2 — Screenshot baseline (Roborazzi)

### S2.1 — Roborazzi set-up and a deterministic clock
- **Goal:** Record and verify screenshots of the real overlay on the JVM, with identical pixels run to run.
- **Why:** PLAN S2 makes screenshots "the diff gate for every polish task". 06 §4: "Roborazzi screenshots on Robolectric native graphics." The harness already runs native graphics (`IslandOverlayCrashTest.kt:40`).
- **Files:**
  - `gradle/libs.versions.toml`, `app/build.gradle.kts` (plugin `io.github.takahirom.roborazzi`, test deps `roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule`; output dir `app/src/test/screenshots`)
  - Wall-clock reads in island UI that make timers non-deterministic: `island/IslandComponents.kt:406, 410, 419, 752, 760`; `island/LiveActivities.kt:1232`
  - New: `island/IslandClock.kt`
- **Steps:**
  1. Add the plugin and dependencies at the latest stable 1.x. **Check compatibility with AGP 9.0.1** (`gradle/libs.versions.toml:2`) first; if the plugin fails, use the `captureRoboImage` library API without the Gradle plugin and verify with a JUnit comparison.
  2. Add `object IslandClock { @Volatile var wallMs: () -> Long = System::currentTimeMillis }` and replace the six reads above. Production behaviour is identical.
  3. In the harness, freeze `IslandClock.wallMs` and set `mainClock.autoAdvance = false` for screenshots. Ambient loops (the equalizer `IslandView.kt:4170-4218`, the recording dot `:4143-4161`, the waveform) then advance only when the test advances the clock.
  4. Record one smoke screenshot (media pill) twice and confirm a zero diff.
  5. Builds live outside OneDrive (`app/build.gradle.kts:9`); keep the **reference images inside the repo** so they are committed.
- **Acceptance criteria:** `./gradlew :app:recordRoborazziDebug` then `verifyRoborazziDebug` passes with 0 changed pixels, twice in a row. Timer text in the smoke image doesn't change between runs.
- **Tests:** `IslandScreenshotSmokeTest`.
- **Depends on:** S1.1.
- **Effort:** S   **Risk:** med — plugin and AGP 9 compatibility, and font rendering differences between machines. Mitigation: record and verify on the same machine and JDK; document that CI needs the same.
- **Ships as:** Tooling.

### S2.2 — Baseline of 15 key states
- **Goal:** Committed reference images for the states every polish task touches.
- **Why:** PLAN S2 (about 15 states). Phase 7 and Phase 8 tasks require "S2 identical" or "S2 diff reviewed".
- **Files:** new `app/src/test/java/com/droidbubble/app/island/IslandScreenshotTest.kt`; images in `app/src/test/screenshots/island/`.
- **States:**
  1. idle pill
  2. media pill
  3. message pill
  4. timer pill
  5. recorder pill
  6. download pill
  7. dual pill (media + message)
  8. minimized ring
  9. notch style, media
  10. charging glance
  11. expanded media
  12. expanded message with Reply open
  13. expanded incoming call
  14. expanded timer
  15. expanded dual
  16. high contrast, expanded message (16 in total, which is fine)
- **Steps:**
  1. One test per state, cropped to the island's union bounds plus 8 dp (from `harness.bounds`), so unrelated window size changes don't create noise.
  2. Name images `<state>__<variant>.png`. Variants: `default` for all, and `hc` for state 16.
  3. Record. Review every image by eye once. This is the baseline.
- **Acceptance criteria:** all 16 images are committed and `verify` is green. Each PR touching `island/` attaches its `compare` output.
- **Tests:** the test itself.
- **Depends on:** S2.1, S1.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Test-only.

---

## S3 — Window-geometry math as a pure function

### S3.1 — Extract the window math verbatim
- **Goal:** The size, flags and touchability of the overlay window come from one pure function that the service calls. It keeps today's behaviour exactly, including today's inconsistencies.
- **Why:** 06 P1-1: the window geometry is computed in three places that disagree. 06 §4 Phase 0: "Extract the service window math into a pure function as-is and table-test it." B2 and F5 change this math; they need a table to change it against.
- **Files:**
  - `service/IslandOverlayService.kt:633-655` (`initOverlay` initial size, which uses a different width formula: `mainHalf = widthDp*0.90`, the `chargingEdge` max)
  - `service/IslandOverlayService.kt:916-1088` (`updateWindowState`: disallowed 929-943, expanded 945-959, occupancy 961-984, widths 986-1003, compact bounds 1005-1040, collapse timing 1042-1086)
  - New: `service/OverlayWindowMath.kt`
- **API sketch:**
  ```kotlin
  data class WindowInputs(
      val settings: IslandSettings, val expanded: Boolean, val landscape: Boolean, val locked: Boolean,
      val call: Boolean, val media: Boolean, val notification: Boolean, val progress: Boolean, val recording: Boolean,
      val flashlight: Boolean, val charging: Boolean, val unlock: Boolean, val connection: Boolean, val ringer: Boolean,
      val userHidden: Boolean, val userMinimized: Boolean,
  )
  sealed interface WindowSpec {
      data object Gone : WindowSpec                       // 1×1, NOT_TOUCHABLE
      data object FullScreen : WindowSpec                 // MATCH_PARENT², touchable
      data class Compact(val widthDp: Float?, /* null = MATCH_PARENT (left cutout) */ val heightDp: Float, val touchable: Boolean) : WindowSpec
  }
  fun compactWindowSpec(i: WindowInputs): WindowSpec      // updateWindowState rules
  fun initialWindowSpec(i: WindowInputs): WindowSpec      // initOverlay rules, kept separate on purpose
  ```
- **Steps:**
  1. Move the arithmetic as-is. Keep the `isCollapsing`/`collapseJob` timing in the service; only the "what size" decision moves.
  2. The service builds `WindowInputs` from `IslandStateManager` and `currentSettings`, then converts dp to px and flags in one adapter.
  3. Add a KDoc note on `initialWindowSpec` pointing at the drift: it ignores the `show*` toggles and the unlock and ringer widths (06 P1-1). F5 merges the two.
- **Acceptance criteria:** the compiled behaviour is unchanged. S1.3 and the crash test are green.
- **Tests:** a parity test `OverlayWindowMathParityTest` with 5 hand-computed rows (idle, media, dual, charging, expanded).
- **Depends on:** step zero.
- **Effort:** S   **Risk:** low — mechanical. Mitigation: keep the old code in a private function for one commit and assert equality in the parity test, then delete it.
- **Ships as:** Refactor.

### S3.2 — Full table test, with the known-bug rows marked
- **Goal:** About 30 table rows cover every branch, so B2 and F5 diffs show up row by row.
- **Why:** PLAN S3 "table-test it". 05 §2 "Touch region" puts today's compact window at (width + 80 dp) × (top offset + height + 12 dp). That is about 260 × 60 dp at the defaults (the research's "52 dp" predates the `+12`).
- **Files:** new `app/src/test/java/com/droidbubble/app/service/OverlayWindowMathTest.kt`.
- **Rows:** idle; idle + `alwaysShowPill`; each single activity (call, media, message, progress, recording, flashlight); persistent widgets only; each glance (charging, ringer, unlock, connection); dual; minimized; hidden; `isLeftCutout`; `horizontalOffsetDp = ±20`; landscape with and without `showInLandscape`; locked + `hideOnLockScreen`; expanded; `showNotifications = false` with a timer (a B3 row); and `initialWindowSpec` versus `compactWindowSpec` for the same input (documents the drift).
- **Steps:**
  1. Write the rows as a list of `(name, inputs, expected)`.
  2. Rows that encode today's bugs get a `// BUG B2.1` or `// BUG B3.1` comment and today's value. The fixing task flips them.
- **Acceptance criteria:** all rows are green against S3.1. Every `when` branch of `compactWindowSpec` is hit; check with a coverage run or by reading.
- **Tests:** the test itself.
- **Depends on:** S3.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Test-only.

---

## S4 — Performance baselines

### S4.1 — Recomposition counter and an expand budget test
- **Goal:** A recorded number for how many times `IslandOverlayRoot` recomposes during a 500 ms expand. It becomes a regression gate.
- **Why:** 06 P0-1: the whole root recomposes on every animation frame. 06 §4 Phase 0: "a debug recomposition counter on the root, with a test asserting the count stays at or below N over a 500 ms expand. It starts as a recorded number and becomes a regression gate after R2."
- **Files:** `island/IslandView.kt:389-392` (root entry); new `island/RecompositionProbe.kt`; new test `app/src/test/java/com/droidbubble/app/island/RecompositionBudgetTest.kt`.
- **Steps:**
  1. Add `object RecompositionProbe { @JvmField var onRoot: (() -> Unit)? = null }`. At the top of `IslandOverlayRoot`, add `RecompositionProbe.onRoot?.invoke()`. In production it is a single null check. Don't use `SideEffect`, which also counts but adds a node.
  2. Test scenarios, with `autoAdvance = false` and 16 ms steps:
     - media expand over 500 ms
     - media collapse over 600 ms
     - 5 s of a playing media pill at rest (the equalizer)
     - 5 s of a running timer pill
     - 3 s of network speed at 1 Hz
  3. Record the counts into `PerfBaseline.kt` as `const val`s. Assert `count <= baseline * 1.1`.
- **Acceptance criteria:** the baseline is committed. The test fails if a change adds 10 % or more recompositions.
- **Tests:** the test itself.
- **Depends on:** S1.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Test seam.

### S4.2 — Macrobenchmark module: frame timing for expand and collapse
- **Goal:** `FrameTimingMetric` numbers (P50/P90/P99 frame duration, jank count) for expand and collapse on a real device, plus a baseline.
- **Why:** PLAN S4 and 06 R12. No macrobenchmark module exists (`settings.gradle.kts` includes only `:app`).
- **Files:** `settings.gradle.kts`; new module `macrobenchmark/` (`com.android.test`, `targetProjectPath = ":app"`); `app/build.gradle.kts` (new `benchmark` build type: `initWith(release)`, `matchingFallbacks += "release"`, debug signing as today, `profileable` in a `src/benchmark/AndroidManifest.xml`).
- **Driving the island without the debug receiver.** B1 removes the receiver from release-like builds, so don't use it here.
  - In `setupBlock`, grant access by shell: `appops set com.droidbubble.app SYSTEM_ALERT_WINDOW allow`, and enable the accessibility service through `settings put secure enabled_accessibility_services …`.
  - Launch `MainActivity` and tap the Test Live Activities panel's **"Download"** demo (`ui/main/LiveActivityTestPanel.kt:46-52`, `island/LiveActivityTester.kt:31`). Download is the demo that is visible on a fresh install; the call and timer demos need B3.
  - In `measureBlock`, `device.click()` the pill at (screenWidth/2, topOffset + height/2) to expand. Wait for idle. Tap outside to collapse.
- **Steps:**
  1. Create the module and the build type. Add `androidx.benchmark:benchmark-macro-junit4` to the module only.
  2. Write `IslandExpandBenchmark` with `FrameTimingMetric`, `CompilationMode.Partial()`, 10 iterations.
  3. Run on one 120 Hz device and paste the results into `macrobenchmark/BASELINE.txt` (device, build and date).
- **Acceptance criteria:** `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest` runs on a device and produces JSON output. The baseline file is committed.
- **Tests:** the benchmark.
- **Depends on:** none; it can run before B1. If B1 lands first, check that the benchmark build has no debug receiver.
- **Effort:** M   **Risk:** med — AGP 9 plus the `com.android.test` set-up, and tapping a system overlay through UiAutomator. Fallback: measure with `dumpsys gfxinfo com.droidbubble.app framestats` from the same script.
- **Ships as:** Tooling (device-only).
- **Done (2026-11-09):** baseline on a 90 Hz OnePlus CPH2631 (no 120 Hz phone at hand) in `macrobenchmark/BASELINE.txt`. Run through `adb install -r` plus `am instrument`, not the Gradle connected task, which uninstalls the app and its data afterwards. The accessibility service was already on, so the set-up doesn't touch secure settings. The pill is found from the island's overlay window, not from default sizes, and the list is swiped slowly, because a tap on a flinging list only stops it.

### S4.3 — Trace sections and a Perfetto recipe
- **Goal:** One command captures a trace in which island work is labelled: notification parsing, media refresh, window updates and the root composition.
- **Why:** PLAN S4 "a Perfetto recipe". 06 P0-2: main-thread parsing lands in the same frame as the pop-in; that is only visible with labelled sections.
- **Files:**
  - `service/NotificationReceiverService.kt:1035` (`onNotificationPosted`), `:343` (`refreshMedia`), `:484` (`updateMediaInfo`)
  - `service/IslandOverlayService.kt:887-914` (`applyLayoutParams`), `:863` (`boostRefreshRate`)
  - `island/IslandView.kt:389` (root)
  - New: `app/perfetto/island.pbtxt`
- **Steps:**
  1. Add `androidx.tracing:tracing-ktx` and wrap each site above in `trace("db.<name>") { }`.
  2. Write the config: `atrace` categories `gfx view wm am input sched freq`, `atrace_apps: "com.droidbubble.app"`, and a 15 s buffer.
  3. In the file's header comment, write the recipe: the `adb shell perfetto … -c - --txt` invocation, plus what to look for (`db.posted` overlapping `Choreographer#doFrame`).
- **Acceptance criteria:** a trace taken on a device shows the `db.*` slices. The overhead is about zero when not tracing (the `trace{}` calls are no-ops).
- **Tests:** none (device tooling). The build stays green.
- **Depends on:** none.
- **Effort:** S   **Risk:** low.
- **Ships as:** Tooling.

---

## S5 — Notification fixture corpus

### S5.1 — Fixture format and a `StatusBarNotification` builder
- **Goal:** Real-world notifications can be written as small JSON files and turned into `StatusBarNotification`s on the JVM.
- **Why:** PLAN S5. 04 §4 P0-3 and §5 R5: the heuristics are English-only, and fixing them needs per-locale fixtures. Phase 7 A3/A4 are tested against this corpus.
- **Files:** new `app/src/test/java/com/droidbubble/app/fixtures/NotificationFixtures.kt`; data in `app/src/test/resources/fixtures/notifications/<group>/<name>.json`.
- **Format:**
  ```json
  { "pkg": "com.google.android.deskclock", "appLabel": "Clock", "locale": "de-DE",
    "category": "stopwatch", "channelId": "Timers", "flags": ["ONGOING_EVENT","FOREGROUND_SERVICE"],
    "template": "android.app.Notification$DecoratedCustomViewStyle", "when": -31000, "postTimeOffset": 0,
    "importance": 3, "extras": { "android.title": "Timer", "android.text": "Pausiert", "android.showChronometer": false,
      "android.chronometerCountDown": true, "android.callType": 1 },
    "actions": [ { "title": "Fortsetzen", "semanticAction": 0, "icon": "ic_play" } ],
    "source": "Pixel 8, Android 16, dumpsys notification --noredact, 2026-11" }
  ```
  `when` and `postTimeOffset` are relative to a fixed test clock.
- **Steps:**
  1. Write the loader: parse the JSON, build the `Notification` with the real `Notification.Builder` where an API exists (so the extras match what the framework writes), overlay the raw `extras`, and wrap it in the public `StatusBarNotification(...)` constructor.
  2. Add a `Ranking` stub for `importance`, which `NotificationReceiverService.kt:1178-1185` reads.
  3. Write the capture recipe in the KDoc: `adb shell dumpsys notification --noredact`, then copy the fields by hand and **replace every real name, number and message with neutral text**.
- **Acceptance criteria:** a round-trip test loads each fixture and reads back the title, category, flags and action titles.
- **Tests:** `NotificationFixturesTest`.
- **Depends on:** step zero.
- **Effort:** S   **Risk:** low.
- **Ships as:** Test-only.

### S5.2 — The corpus
- **Goal:** Fixtures for every activity family in several locales, under the names Phase 7 already uses.
- **Why:** PLAN S5: "CallStyle, ProgressStyle, chronometer, Maps, clock and recorder apps, in multiple locales." Phase 7 convention 4 fixes these names.
- **Files:** `app/src/test/resources/fixtures/notifications/…`.
- **Fixtures (the Phase 7 names, verbatim):**
  - `call/{gphone_incoming_en, gphone_ongoing_mute_speaker_en, whatsapp_incoming_ar, whatsapp_video_es, gphone_screening_en, samsung_incoming_tr}`
  - `clock/{gclock_timer_running_en, gclock_timer_paused_de, gclock_timesup_fr, samsung_timer_ar, gclock_stopwatch_en, gclock_alarm_ringing_es}`
  - `recorder/{grecorder_en, samsung_voicenote_ru, miui_recorder_zh}`
  - `nav/{gmaps_turn_en, gmaps_turn_ar, gmaps_liveupdate_36_en, waze_en}`
  - `progress/{play_download_en, chrome_download_hi, uber_progressstyle_36_en, talabat_ongoing_ar, careem_ongoing_ar}`
  - `screenrec/{pixel_en, samsung_es, xiaomi_id}`
  - `message/{whatsapp_ar, telegram_en, gmail_private_en}`
- **Fixtures for Phase 1 regressions (new names):**
  - `tracking/uber_arrive_time_en`: ongoing, title "Arrive at 10:45 PM" (B4.1)
  - `tracking/flight_boarding_en`: "Boarding at 10:30 Gate B" (B4.1)
  - `promo/uber_marketing_en`: not ongoing, importance LOW (B4.2)
  - `promo/google_app_news_en`: `com.google.android.googlequicksearchbox`, not ongoing (B4.1, B4.2)
  - `message/send_action_en`: a call-category notification with an action "Send" (B4.3)
  - `screenrec/thirdparty_idle_en`: ongoing "Screen Recorder" with text "Tap to start" (B4.4)
  - `sports/score_en`: "Arsenal 2 - 1 Chelsea" from `com.mobilefootball` (B4.1, keeps the true positive)
- **Steps:** capture what you can from real devices (S5.1 recipe). Hand-write the rest from AOSP sources and mark `"source": "synthetic"`. Keep each file under 2 KB.
- **Acceptance criteria:** all 38 files load through S5.1. Each has a `source` field.
- **Tests:** `NotificationFixturesTest` iterates the whole directory.
- **Depends on:** S5.1.
- **Effort:** M   **Risk:** low. Real captures for Samsung, Xiaomi and RTL locales may need borrowed devices; synthetic is acceptable when marked.
- **Ships as:** Test-only.

### S5.3 — Fixture characterization through the real listener
- **Goal:** Each fixture is posted into the real `NotificationReceiverService` under Robolectric, and the resulting island state is recorded. Phase 1 heuristic fixes then show up as table diffs.
- **Why:** the classification spans `NotificationReceiverService.kt:1035-1325` and `island/LiveActivities.kt:226-253`; only an end-to-end run catches gate ordering bugs such as B3's.
- **Files:**
  - `service/NotificationReceiverService.kt:69` (`currentSettings` is private): add `@VisibleForTesting internal fun setSettingsForTest(s: IslandSettings)`
  - `:1178-1180` (ranking lookup): needs the `Ranking` stub from S5.1
  - New: `app/src/test/java/com/droidbubble/app/service/FixtureCharacterizationTest.kt`, `.../service/golden/FixtureGolden.kt`
- **Steps:**
  1. `Robolectric.buildService(NotificationReceiverService::class.java).create()`, then `setSettingsForTest(IslandSettings(showNotifications = true))` and `onNotificationPosted(sbn)`.
  2. Record per fixture:
     - which flow received it: `activeNotification`, `activeProgress`, `activeCallState`, `screenRecordingState`, or none
     - `liveActivityKind`
     - `isIncomingCall`
     - whether answer/decline intents were found
     - `liveActivityControls` (`LiveActivities.kt:277`): pause and stop found or not
  3. Run a second column with default settings (`showNotifications = false`), which is today's fresh install.
  4. Use the same record and assert mode as S1.3. Mark known-wrong rows with `knownIssue` (for example `whatsapp_incoming_ar` loses Answer: A4; `uber_arrive_time_en` comes out as SPORTS: B4.1; `gclock_timer_running_en` is dropped with defaults: B3.1).
- **Acceptance criteria:** the table is committed and green. Every `knownIssue` names the task that fixes it.
- **Tests:** the test itself.
- **Depends on:** S5.2.
- **Effort:** M   **Risk:** med — the service's `onCreate` touches `MediaSessionManager` and DataStore. Mitigation: if `create()` is too heavy, call `onNotificationPosted` on an instance built with `Robolectric.buildService(...).get()` without `create()`, and set the settings through the seam.
- **Ships as:** Test-only.

---

## B1 — Security: debug commands out of release builds

### B1.1 — The debug receiver exists only in debug builds
- **Goal:** Release APKs contain no receiver that accepts `com.droidbubble.app.ACTION_*` commands. Debug builds keep the commands, registered `RECEIVER_NOT_EXPORTED`.
- **Why:** 06 P1-4 (confirmed): any installed app can post fake cards (phishing text), wipe the real state, or loop `ACTION_EXPAND` to block the screen. PLAN B1.
- **Files:**
  - `service/IslandOverlayService.kt:134-252` (`debugReceiver`, **13** actions, including `ACTION_COLLAPSE` at `:210`, which the research's count of 11 missed)
  - `:499-518` (filter and registration: `RECEIVER_EXPORTED` on 33+, and the plain `registerReceiver` below 33, which is also exported)
  - `:1138` (unregister)
  - `app/build.gradle.kts:42` (`buildConfig = false`)
  - New: `app/src/debug/java/com/droidbubble/app/service/DebugCommands.kt` and `app/src/release/java/com/droidbubble/app/service/DebugCommands.kt` (no-op). A `benchmark` source set, if S4.2 is in, uses the release no-op.
- **API sketch:**
  ```kotlin
  object DebugCommands {               // debug source set
      fun register(service: Service): BroadcastReceiver?   // ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)
      fun unregister(service: Service, receiver: BroadcastReceiver?)
  }
  object DebugCommands { fun register(service: Service): BroadcastReceiver? = null; fun unregister(service: Service, r: BroadcastReceiver?) {} } // release
  ```
- **Steps:**
  1. Set `buildConfig = true`. Phase 7 A1.3 also uses `BuildConfig.DEBUG`.
  2. Move the receiver body verbatim into the debug `DebugCommands`. Use `ContextCompat.registerReceiver(..., ContextCompat.RECEIVER_NOT_EXPORTED)` on every API level.
  3. In the service, replace `:499-518` with `debugReceiver = DebugCommands.register(this)`, and `:1138` with `DebugCommands.unregister(...)`.
  4. **Note for developers:** with `NOT_EXPORTED`, `adb shell am broadcast` no longer reaches the receiver, because the shell is another UID. The in-app Test Live Activities panel (`ui/main/LiveActivityTestPanel.kt`) covers manual testing. If adb control is wanted, it's a separate, explicit decision: register in debug only with `RECEIVER_EXPORTED` and `permission = android.permission.DUMP`, which the shell holds and third-party apps can't. Don't do this silently.
- **Acceptance criteria:**
  - `apkanalyzer dex packages` (or `unzip` plus a grep) on the release APK finds no `ACTION_SIMULATE_NOTIFICATION` string.
  - A debug build still responds to the commands sent from inside the app (a unit test sends the broadcast with `setPackage`).
  - On a device, a release build plus another app sending `ACTION_EXPAND` shows no reaction.
- **Tests:** `DebugCommandsTest` (Robolectric, debug variant): an in-app broadcast `ACTION_SIMULATE_MEDIA` sets `activeMedia`. A release-variant test (`src/testRelease/...`): `DebugCommands.register` returns `null`.
- **Depends on:** none. Land it first in Phase 1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix (security).
- **Done (2026-11-09):** `DebugCommands` in `src/debug` (not exported, all API levels, no adb control per the owner), no-ops in `src/release` and `src/benchmark` (a build type's `matchingFallbacks` covers dependencies only, not sources). `register` takes a `Context`. AGP 9 makes unit tests for debug only, so `app/build.gradle.kts` enables release's for `src/testRelease`. Release and benchmark dex have no `com.droidbubble.app.ACTION_*` string; the debug dex does. Device check passed (2026-11-09, OnePlus CPH2631): `adb shell am broadcast` of SIMULATE_NOTIFICATION, EXPAND and SIMULATE_MEDIA to the benchmark build did nothing.

### B1.2 — Runtime system receivers not exported, plus an exported-surface test
- **Goal:** The service's system-event receiver is registered `RECEIVER_NOT_EXPORTED`. A test pins the list of exported components, so a new exported one can't slip in.
- **Why:** 06 P1-4 fix list: "The `powerReceiver` only needs protected system broadcasts, so it can also be `RECEIVER_NOT_EXPORTED`." Protected system broadcasts are still delivered to non-exported receivers.
- **Files:**
  - `service/IslandOverlayService.kt:520-537` (power, screen, unlock, Bluetooth and ringer filter, registered `RECEIVER_EXPORTED`)
  - `app/src/main/AndroidManifest.xml:50, 61, 71, 79, 90, 102, 111, 122, 135` (the components' `exported` attributes)
- **Steps:**
  1. Switch `:533-537` to `ContextCompat.registerReceiver(this, powerReceiver, powerFilter, RECEIVER_NOT_EXPORTED)`.
  2. Device check: plug in a charger, toggle the ringer, connect Bluetooth, lock and unlock. Each glance still appears. `"android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"` (`:531`) and the Bluetooth profile actions (`:527-529`) are the ones to watch, because they are system-sent but less common.
  3. Add `ExportedSurfaceTest` (Robolectric): read `PackageManager.getPackageInfo(GET_ACTIVITIES|GET_SERVICES|GET_RECEIVERS|GET_PROVIDERS)` and assert that the exported set equals the allowlist:
     - `MainActivity`
     - the notification listener, guarded by `BIND_NOTIFICATION_LISTENER_SERVICE`
     - the accessibility service, guarded by `BIND_ACCESSIBILITY_SERVICE`
     - `BootReceiver`
     - (`PowerConnectionReceiver` and `ShizukuProvider` were removed in the pre-Phase-0 cleanup.)
- **Acceptance criteria:** the test is green. Every system glance still fires on a device.
- **Tests:** `ExportedSurfaceTest`; device checklist above.
- **Depends on:** B1.1 (same lines of `onCreate`).
- **Effort:** S   **Risk:** low — OEM ringer broadcasts are the only unknown; the device check covers them.
- **Ships as:** Fix (security).
- **Done in code (2026-11-09):** the filter is split. Power, charging, unlock, screen and both ringer actions are registered `RECEIVER_NOT_EXPORTED` (the system process sends them). The four Bluetooth actions stay `RECEIVER_EXPORTED`: the Bluetooth app sends them from its own UID, which a not-exported receiver drops (`checkComponentPermission` passes only root, system and the same app); they are protected broadcasts, so no other app can send them. `ExportedSurfaceTest` also lists two debug-only library activities (`PreviewActivity`, the ui-test-manifest `ComponentActivity`) that are absent from release. Device check passed (2026-11-09, OnePlus CPH2631): charger, ringer, Bluetooth and unlock glances all appear. Note: `adb install -r` of an update switched the accessibility service off on this phone; the owner re-enabled it.

---

## B2 — Touch pass-through

### B2.1 — Nothing is touchable when nothing is visible (foreground app)
- **Goal:** When the only activity belongs to the foreground app, so the island draws nothing, the window is 1×1 and `NOT_TOUCHABLE`, not a transparent touchable strip.
- **Why:** 06 P0-4 (bug): `IslandView.kt:460-465` hides the app's own cards, but the service's occupancy (`IslandOverlayService.kt:966-975`) ignores `foregroundPackage`, and its `combine` (`:765-787`) doesn't watch it. With Spotify open and playing, a touchable rectangle of about 260 × 60 dp over the status bar swallows taps and shade pulls. 05 P0-1.
- **Files:**
  - `service/OverlayWindowMath.kt` (from S3.1): add `foregroundPackage` and the package of each activity to `WindowInputs`
  - `service/IslandOverlayService.kt:765-787` (add `IslandStateManager.foregroundPackage` to the combine), `:961-984`
  - The view's rule to mirror: `island/IslandView.kt:459-465`. Media, progress and notification are hidden when their package is in the foreground, **except an incoming call**.
- **Steps:**
  1. In the pure function, `media`, `progress` and `notification` count as occupied only when `pkg != foregroundPackage`, with the ringing-call exception. This is a temporary duplicate of the view rule; F1's `deriveIslandScene` removes it (06 §4 Phase 1, "P0-4 via a temporary foreground check in the service").
  2. Flip the `// BUG B2.1` rows in S3.2. Add rows: a media app in the foreground → `Gone`; foreground media app + a message from another app → `Compact`, touchable.
  3. Flip S1.3 row 14's `knownIssue`.
- **Acceptance criteria:** S3.2 green with the flipped rows. On a device, with Spotify playing in the foreground, tapping the top centre of Spotify and pulling the shade from the centre both work. Leaving Spotify brings the media pill back.
- **Tests:** S3.2 rows; S1.3 row 14.
- **Depends on:** S3.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix.
- **Done in code (2026-11-09):** `WindowInputs` gains `foregroundPackage`, the media, notification and progress packages and `incomingCall`; `mediaShown`, `notificationShown` and `progressShown` mirror the view rule (an unknown foreground app hides nothing) and feed occupancy and the dual count. The service's combine watches `foregroundPackage`. S3.2 rows flipped and six added; S1.3 `media_app_in_foreground` no longer carries a knownIssue. Device check still open: Spotify in front, tap its top centre and pull the shade from the centre.

### B2.2 — Trim the compact window to the pill and bubble envelope
- **Goal:** The compact window is only as large as the drawn pill and bubble, plus the room their animations need, instead of `width + 80 dp` and `height + 12 dp`.
- **Why:** 06 P0-4: "the compact window eats 40 dp on each side of the pill, from y=0 down". 05 R1 fallback: "resizing the window to exactly the pill and enlarging it only while it morphs." This is the safe half of PLAN B2 and needs no hidden API.
- **Files:**
  - `service/OverlayWindowMath.kt` (the `+ 80f` margins at today's `IslandOverlayService.kt:988, 991, 994, 1000, 1002`; the `+ 12` height at `:1022` and `:653`)
  - The composable geometry these must cover: `island/IslandView.kt:893-1000` (`targetCompactWidth` at `:900`, charging width at `:893`, bubble offset at `:999`); the idle bounce scale up to 1.04 (`:1921-1923`); `swapSquash` (`:578`, `:1834`)
- **Steps:**
  1. Measure the widest settled and animating extent for each state from the composable constants, for example `chargingIslandWidth` and the dual `rightEdge`. Write it down as `envelopeMarginDp` per state, for example 8 dp for overshoot instead of a flat 40 dp per side.
  2. Height: `topOffset + heightDp + 4` (enough for the 1.04 bounce), not `+ 12`.
  3. Keep MATCH_PARENT for expanded and the collapse latch exactly as today.
  4. Update S3.2's expected rows; each diff is intended.
- **Acceptance criteria:**
  - The S1.3 and S2 diffs are zero: nothing visible is clipped at settle.
  - The crash test and `randomEventStorm_neverCrashes` are green.
  - On a device, with a media pill showing, the status-bar icons 30 dp beside the pill respond to taps.
  - Record a screen capture of charging, ringer and dual: no clipped edge mid-animation.
- **Tests:** S3.2 updated; S1.3; device recording.
- **Depends on:** B2.1.
- **Effort:** S   **Risk:** med — clipping an overshoot frame. Mitigation: the device recording at 0.5× animation speed.
- **Ships as:** Fix.

### B2.3 — Touches outside the pill pass through (touchable region, behind a probe)
- **Goal:** In compact mode only the pill and bubble rectangles take touches. Everything else in the window, including the strip above the pill, passes through to the status bar and the app.
- **Why:** PLAN B2: "the hit area hugs the pill and bubble". 05 R1: "hook up `IslandTouchManager` (rects from `onGloballyPositioned`)… If the hidden API it relies on is blocked on API 34–36, fall back" (to B2.2). `IslandTouchManager` is declared but never created (`IslandOverlayService.kt:595`).
- **Files:**
  - `service/IslandTouchManager.kt:17-111`. It uses reflection on the hidden `ViewTreeObserver.addOnComputeInternalInsetsListener` (`:53`) and `setTouchableInsets` (`:86`)
  - `service/IslandOverlayService.kt:595` (the field), `:688-748` (view creation and `addView`), `:1005-1040`
  - `island/IslandView.kt:1823-1829` (pill) and `:1692-1696` (bubble): publish their settled `boundsInWindow`
  - `island/IslandStateManager.kt`: add `touchRects: StateFlow<List<Rect>>`
- **Steps:**
  1. **Spike first (½ day, before writing the PR):** on API 34, 35 and 36 devices, attach the manager and log whether `getMethod` succeeds and whether `setTouchableInsets(3)` (REGION) takes effect. Hidden-API enforcement for `targetSdk 36` may block it.
  2. If the probe succeeds: publish the pill and bubble rects from `onGloballyPositioned`, **only when they change by at least 1 px and not every frame** (contrast `publishGlass`, 06 P0-1). Attach the manager after `addView`, and call `updateCompactBounds(rects, occupied, expanded)` from `updateWindowState`. Expanded keeps the full-window region (tap outside collapses).
  3. If the probe fails on a device: `IslandTouchManager.attach()` returns `false`, and the service keeps B2.2's envelope. Record the result in `CrashLog` breadcrumbs (B11.1) when it is available.
  4. Put it behind `IslandFlags.touchRegion` (a `const val` default `true` in debug, decided per the spike for release).
- **Acceptance criteria:**
  - On a device where the probe succeeds, with the media pill showing, a tap 2 dp above the pill reaches the status bar, and a shade pull starting 10 dp beside the pill opens the shade. The pill and bubble still respond to tap, long-press and horizontal swipe.
  - Where it fails, behaviour equals B2.2.
- **Tests:**
  - A unit test of the rect-to-region conversion.
  - `IslandOverlayCrashTest` green: rect publishing must not run per frame. Check with the S4.1 counter: no new recompositions.
  - The device matrix: Pixel on API 36, Samsung One UI and Xiaomi HyperOS.
- **Depends on:** B2.2.
- **Effort:** M   **Risk:** high — hidden API and OEM behaviour. Mitigation: the probe, the flag and the B2.2 fallback.
- **Ships as:** Fix (device-verified).

---

## B3 — Live activities visible by default (D5)

### B3.1 — A separate `showLiveActivities` gate, on by default
- **Goal:** Calls, timers, alarms, recorders, rides and scores show on a fresh install. "Incoming Notifications" governs only message banners.
- **Why:** 04 P0-1: the headline activities go through the "Incoming Notifications" toggle, which defaults to off. D5 = yes.
  - **Found while verifying:** the gate also hides **ringing calls**. A CallStyle incoming call isn't "ongoing", so it takes the `!hasProgress` branch and is dropped at `NotificationReceiverService.kt:1151` when `showNotifications` is off.
  - The view gates every notification on `showNotifications` (`IslandView.kt:463`), and so does the service's window occupancy (`IslandOverlayService.kt:968`).
  - So with defaults, the in-app Test panel's call, timer and alarm demos show nothing either.
- **Files:**
  - `model/IslandSettings.kt:31-34` (add `showLiveActivities: Boolean = true`)
  - `data/SettingsRepository.kt:38` (keys), `:85-145` (flow mapper), `:157-216` (edit mapper), `:220-272` (writer). All three must get the key; B8.6 later merges the mappers.
  - `service/NotificationReceiverService.kt:1038` (early return), `:1147-1152` (the gate)
  - `island/IslandView.kt:463-464`
  - `service/OverlayWindowMath.kt` (from S3.1; was `IslandOverlayService.kt:968`)
  - New: `model/NotificationGate.kt`
- **API sketch:**
  ```kotlin
  enum class NotificationGateKind { CALL, LIVE, PROGRESS, MESSAGE }
  fun gateKind(isCall: Boolean, isLive: Boolean, hasProgress: Boolean): NotificationGateKind
  fun IslandSettings.allows(kind: NotificationGateKind): Boolean = when (kind) {
      CALL -> showCalls; LIVE -> showLiveActivities; PROGRESS -> showLiveProgress; MESSAGE -> showNotifications
  }
  ```
  - In NRS: `isLive = looksLikeTimer || isLiveCandidate`. Note that `looksLikeTimer` is computed at `:1169`, after the gate, so move the gate below it.
  - In the view and the window math: `isLive = n.isLiveActivity || liveActivityKind(n) != NONE`, and `isCall = n.isCall`.
  - Progress with a live kind (rides with a bar) counts as LIVE in the view. The service keeps routing it through `showLiveProgress` until A1. Document this in the KDoc.
- **Steps:**
  1. Add the setting with default `true` and missing-key → `true` in both mappers.
  2. Move the NRS gate after `looksLikeTimer` and `isLiveCandidate` are known, and replace it with `currentSettings.allows(gateKind(...))`. Update the early return at `:1038` to include `showLiveActivities`.
  3. View `:463` and the window math use the same helper.
  4. Flip the `knownIssue` rows in S5.3 (the defaults column), S3.2 and S1.3.
- **Acceptance criteria:**
  - With fresh-install settings, each Test panel demo (call, timer, time's up, alarm, recorder, ride, live update, flight, score) appears.
  - A WhatsApp message does **not** appear until "Incoming Notifications" is on.
  - Turning "Phone Calls" off hides ringing calls.
- **Tests:** `NotificationGateTest` (a pure truth table); S5.3 defaults column; S3.2 row "`showNotifications = false` + timer → Compact"; a Robolectric repository round-trip, missing key → `true`.
- **Depends on:** S5.3, S3.2.
- **Effort:** M   **Risk:** med — users who turned notifications off on purpose now see timers and rides. That is intended (D5); the new toggle in B3.2 lets them turn it off.
- **Ships as:** Fix (behaviour, user-visible).

### B3.2 — A "Live Activities" toggle in the settings app
- **Goal:** Users can turn live activities off separately from message notifications, and the labels say which is which.
- **Why:** B3.1 adds the setting; without a switch it can't be turned off. 04 R1 lists `MainScreen.kt:715-835`.
- **Files:** `ui/main/MainScreen.kt:646-660` (`reorderableKeys`), `:712-736` (titles and checked state), `:849-855` (toggle write).
- **Spec:**
  - Don't add a `LIVE` key to `featurePriorities`. Phase 7 A2.2 adds the `LIVE` and `SYSTEM` buckets with a migration, and adding it here would collide.
  - Instead, add a plain switch row directly below the reorderable list: title "Live Activities", subtitle "Calls, timers, alarms, recorders, rides and scores". Reuse the existing row composable and `GroupPosition` styling.
  - Rename "Incoming Notifications" to "Message Notifications" (`:728`). The row and its position stay the same.
- **Steps:** add the row bound to `showLiveActivities`, then change the title string.
- **Acceptance criteria:** toggling the row changes the setting and live activities disappear and come back. Drag-to-reorder is unaffected.
- **Tests:** `MainScreenViewModelTest` (`app/src/test/java/com/droidbubble/app/ui/main/MainScreenViewModelTest.kt`) case: `updateSettings { it.copy(showLiveActivities = false) }` persists. Manual check.
- **Depends on:** B3.1.
- **Effort:** S   **Risk:** low. `MainScreen.kt` is shared with Phase 8 (C1, C5): one session at a time.
- **Ships as:** Fix (companion app).

---

## B4 — Heuristic hardening

### B4.1 — Strict score regex, TRACKING before SPORTS, no Google app in sports hints
- **Goal:** Times such as "Arrive at 10:45 PM" or "Boarding at 10:30 Gate B" are never parsed as scores, and ride or flight updates get the tracking design.
- **Why:** 04 P1-4: `SCORE_REGEX` accepts `:`, and SPORTS is checked before TRACKING. 04 R6. 04 P1-5: `googlequicksearchbox` in the sports hints pops every Google app notification.
- **Files:** `island/LiveActivities.kt:143-147` (`SPORTS_PACKAGE_HINTS`, ending in `"googlequicksearchbox"`), `:156` (`SCORE_REGEX`), `:182-192` (`parseLiveScore`), `:250-251` (order).
- **Spec:**
  - The separator is a hyphen, en dash or em dash, with optional spaces. A colon is accepted **only** when `isSportsPackage(pkg)`, and never when AM/PM, `a.m.`/`p.m.` or "h" follows the second number.
  - Both team strings must contain at least one letter and must not be one of `at`, `by`, `from`, `until`, `gate`.
  - Swap lines 250 and 251.
  - Drop `"googlequicksearchbox"` from the hints. Google live scores still match when they are promoted or ongoing and the strict regex passes.
- **Steps:** change the regex and the order. Keep `LiveScore` unchanged. Add the S5.2 fixtures `tracking/uber_arrive_time_en`, `tracking/flight_boarding_en` and `sports/score_en` to S5.3.
- **Acceptance criteria:** `uber_arrive_time_en` → TRACKING, `flight_boarding_en` → TRACKING or NONE (never SPORTS), `score_en` → SPORTS with home "Arsenal". The existing `LiveActivityKindTest.ridesLiveUpdatesAndScores` (`app/src/test/.../LiveActivityKindTest.kt:99-121`) stays green.
- **Tests:** new cases in `LiveActivityKindTest`; S5.3 diffs limited to those fixtures.
- **Depends on:** S5.3.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix.
- **Done (2026-11-09):** `matchScore` adds the colon, AM/PM/h, letter and time-word rules; the time words are checked at the edges next to the numbers ("Arriving by 10 - 11" fails), not only as whole team names. Order: a tracking app's ongoing or promoted update is TRACKING first, then scores, then any other promoted notification is TRACKING. A plain swap would have turned a promoted Google live score into TRACKING, which the spec says must stay SPORTS. S5.3: only `uber_arrive_time_en` and `flight_boarding_en` changed, both to TRACKING.

### B4.2 — Package hints bypass the filters only for ongoing notifications
- **Goal:** Silent or marketing pushes from Uber, Delta, United, sports apps or recorder apps no longer pop the island. Only their ongoing or foreground-service notifications count as live candidates.
- **Why:** 04 P1-5: `isLiveCandidate` includes `isTrackingPackage`, `isSportsPackage` and `isRecorderPackage` regardless of ongoing. That bypasses the ongoing filter, the silent-channel filter and the app-on-screen filter.
- **Files:** `service/NotificationReceiverService.kt:1139-1141` (`isLiveCandidate`), with the bypassed filters at `:1172`, `:1179-1185` and `:1203`.
- **Spec:**
  ```kotlin
  val ongoingLike = isOngoing || isForegroundServiceNotif      // move :1161 above :1139
  val isLiveCandidate = isAlert || isPromoted ||
      (ongoingLike && (isTrackingPackage(pkg) || isSportsPackage(pkg) || isRecorderPackage(pkg))) ||
      (isClockPackage(pkg) && ("alarm" in rawWords || "snooz" in rawWords))
  ```
  The clock-alarm clause stays as it is: an upcoming-alarm notification isn't always ongoing.
- **Acceptance criteria:** `promo/uber_marketing_en` (importance LOW) → dropped. `promo/google_app_news_en` → treated as a regular message, so it is gated by "Message Notifications" and importance. `progress/careem_ongoing_ar` and `talabat_ongoing_ar` → unchanged.
- **Tests:** S5.3 rows; a new `IslandStateBehaviorTest`-style unit test isn't needed.
- **Depends on:** B4.1 (same fixture table), S5.3.
- **Effort:** S   **Risk:** low — a non-ongoing ride update from a tracking app now needs importance ≥ default to pop. That's the intended heads-up rule.
- **Ships as:** Fix.

### B4.3 — Whole-word matching for "end" and friends in call and recorder actions
- **Goal:** Button words match whole words only, so "Send", "Weekend" or "Recommend" is never taken as hang-up or stop.
- **Why:** 04 §3 CALL: "`contains("end")` also matches "Send…"". `STOP_REGEX` in `LiveActivities.kt:150` already uses `\b`. **The bug lives in NRS**, not in `LiveActivities`:
  - `NotificationReceiverService.kt:769`: ongoing call, `aTitle.contains("end")`
  - `:947`: screen recording stop, `aTitle.contains("end")`
  - `:1236-1242`: `actionIntent(...)`, with `t.contains(it)` for "decline", "reject", "hang up", "end"
  - `:730` already uses `it == "end"`, which is fine
- **Files:** the lines above; new `service/ActionWords.kt`.
- **API sketch:** `fun String.hasWord(vararg words: String): Boolean`. Lowercase with `Locale.ROOT`, then match `(?<![\p{L}\p{N}])word(?![\p{L}\p{N}])`, which works for multi-word phrases and non-Latin text. Phase 7 A4 later replaces these English tables; keep the helper so A4 can delegate to it.
- **Steps:** replace the four `contains` chains with `hasWord`.
- **Acceptance criteria:** `message/send_action_en` doesn't get a decline or hang-up intent from "Send". `call/gphone_ongoing_mute_speaker_en` still finds "Hang up". `screenrec/pixel_en` still finds "Stop".
- **Tests:** `ActionWordsTest` (pure): "Send" ∌ end, "End call" ∋ end, "Hang up" ∋ "hang up", "Weekend" ∌ end. S5.3 rows.
- **Depends on:** S5.3.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix.

### B4.4 — Fewer screen-recording false positives
- **Goal:** An idle "Screen recorder: tap to start" notification from a third-party app doesn't put the island into recording mode.
- **Why:** 04 §3 "Screen recording" and P2-14: the last `||` clauses accept any ongoing notification whose title or text says "screen recorder".
- **Files:** `service/NotificationReceiverService.kt:883-933` (`isScreenRecordingNotification`; the bare title and text clauses are `:927-932`), `:866-881` (`isScreenRecordingSavedNotification`).
- **Spec:**
  - Recording = ongoing **and** (`isRecordingChannel && (isRecordingText || isRecordingPkg)`, **or** `isRecordingPkg && isRecordingText`, **or** a system-UI package with recording text, **or** the notification shows a chronometer (`EXTRA_SHOW_CHRONOMETER`) and has recording text).
  - Delete `:927-932`.
  - Add idle words to the saved check: "tap to start", "ready to record".
- **Acceptance criteria:** `screenrec/thirdparty_idle_en` → not recording. `screenrec/{pixel_en, samsung_es, xiaomi_id}` → same result as before. If the Spanish and Indonesian ones were missed before, they stay missed (A4 and A7.9 fix those).
- **Tests:** S5.3 rows; unit cases through the S5.1 builder.
- **Depends on:** S5.3.
- **Effort:** S   **Risk:** low — a third-party recorder that names its channel oddly may now be missed. A7.9 (the API 35 callback) is the real fix.
- **Ships as:** Fix.

### B4.5 — Dismissed cards return only on a real state change
- **Goal:** A ride or delivery card the user swiped away stays away while only its ETA or clock ticks. It comes back when the state changes, for example "Driver arriving" to "Driver has arrived".
- **Why:** 04 P2-17 and R12: "A dismissed card returns when its text changes (`IslandStateManager.kt:552`), so dismissed ride cards come back on every update."
- **Files:** `island/IslandStateManager.kt:559-562` (`dismissedNotificationText`), `:581-582` (the check), `:703` (where it is stored); new `island/DismissSignature.kt`.
- **Spec:**
  ```kotlin
  /** What must change for a dismissed card to return. Phase 7 A1.2 reuses this as LiveActivitySpec.stateSignature. */
  fun dismissSignature(n: NotificationInfo): Int =
      if (!n.isLiveActivity) n.text.hashCode()                          // messages: any new text is a new message (today's rule)
      else listOf(liveActivityKind(n), n.isIncomingCall, n.isAlert,
                  n.liveTimerBaseMs == 0L,                              // paused vs running
                  n.actions.map { it.title },
                  n.title.replace(Regex("""\d+"""), "#"),
                  n.text.replace(Regex("""\d+"""), "#")).hashCode()
  ```
- **Steps:** store the signature instead of the text, and compare signatures. `removeNotification` (`:663`) still clears the entry.
- **Acceptance criteria:**
  - A dismissed ride with text "Arrives in 4 min" stays dismissed on a repost saying "Arrives in 3 min", and returns on "Driver has arrived".
  - A dismissed WhatsApp message returns on new text, as today.
- **Tests:** add to `IslandStateBehaviorTest` (`app/src/test/.../IslandStateBehaviorTest.kt`): `dismissedRide_staysGoneOnEtaTick_returnsOnStatusChange` and `dismissedMessage_returnsOnNewText`.
- **Depends on:** B4.1 (`liveActivityKind` must already be the hardened version).
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix.

---

## B5 — High contrast

### B5.1 — High-contrast outline on the pill, bubble and both cards
- **Goal:** With high contrast on, from the app setting or Android's "High contrast text", every island edge is drawn with the 55 % white outline at 1.5 dp or more.
- **Why:** 02 R2 and gap 2: `IslandOverlayService.kt:704-709` sets `outlineColorArgb = 0x8CFFFFFF`, but the view never reads `outlineColorArgb`. It draws `primaryOutlineColor` at 15 % instead (`IslandView.kt:541-567`), so "the accessibility setting does almost nothing".
- **Files:**
  - `island/IslandView.kt:541-567` (outline colours)
  - The four borders: `:1332-1338` (card 1), `:1506-1508` (card 2), `:1705-1711` (bubble), `:1840-1846` (pill; skipped for notch)
  - `island/IslandTokens.kt:66` (`HighContrastOutline`, unused today)
- **Steps:**
  1. `val outlineFor: (Color) -> Color = { if (IslandTokens.highContrast) IslandTokens.HighContrastOutline else it }`, applied to `primaryOutlineColor` and `secondaryOutlineColor`.
  2. The thickness is already raised to ≥ 1.5 by the service (`:708`); keep that.
  3. Notch style keeps no pill outline (`:1840`, `!settings.isNotch`), because the notch is fused to the screen edge. Add a short code comment saying it's deliberate.
  4. Remove the dead `outlineColorArgb = 0x8CFFFFFF` override (`IslandOverlayService.kt:707`) or route it through. Pick one; don't keep both.
- **Acceptance criteria:**
  - The S2 `hc` screenshot shows the outline on the pill and the card. The default screenshots are unchanged.
  - The S1.3 high-contrast column changes only in pixels; bounds stay the same.
- **Tests:** S2 `hc` variant re-recorded (an intended diff); S1.3 unchanged.
- **Depends on:** S2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish (accessibility fix).

### B5.2 — Text tokens instead of literal greys, and a readable placeholder
- **Goal:** All secondary and tertiary island text reacts to high contrast, and the reply placeholder reaches at least 4.5:1.
- **Why:** 02 R2: nine `Color(0xFFA1A1AA)` texts bypass `TextSecondary`. 05 §2 Accessibility: the placeholder #71717A on #212121 is about 3.3:1. With #8E8E93 (`TextTertiary`) on #212121 it is about 4.9:1.
- **Files:**
  - `island/IslandView.kt` `0xFFA1A1AA` at `:2787, 4125, 4318, 4358, 4364, 4458, 4579, 4610, 4632` → `IslandTokens.TextSecondary`
  - `:3378, 3380` (`0xFF8E8E93`, media time-label colour) → `IslandTokens.TextTertiary`
  - `:3894` (placeholder) and `:3925` (Cancel icon), both `0xFF71717A` → `TextTertiary`
  - `:3910` (`0xFF52525B`, disabled Send) stays. Disabled controls are exempt; leave a short comment.
  - Tokens: `island/IslandTokens.kt:48-49`
- **Steps:** replace the literals. In normal mode the hex values are identical, except `71717A` → `8E8E93` (an intended diff). In high contrast they become `DADADF` and `C7C7CC`.
- **Acceptance criteria:**
  - Default screenshots: the only diff is the reply placeholder and Cancel icon in S2 state 12.
  - In the `hc` screenshots, the secondary text is brighter.
  - A grep finds `0xFFA1A1AA`, `0xFF8E8E93` and `0xFF71717A` nowhere in `IslandView.kt`.
- **Tests:** S2 (states 12 and 16 re-recorded); a unit test of the contrast ratio of `TextTertiary` on `SurfaceMuted` ≥ 4.5 (WCAG luminance helper in the test).
- **Depends on:** S2.2. **Serialize with B9.1**, which edits `:4579` and `:4610` too.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish (accessibility fix).

### B5.3 — High contrast reacts live to the system setting
- **Goal:** Turning Android's "High contrast text" on or off restyles the island at once, with no restart.
- **Why:** 05 §2 Accessibility and R11: the system flag is read once inside `remember {}` (`IslandOverlayService.kt:697-701`).
- **Files:** `service/IslandOverlayService.kt:695-703`.
- **Steps:**
  1. Write `fun highTextContrastFlow(resolver): Flow<Boolean>`: a `callbackFlow` with a `ContentObserver` on `Settings.Secure.getUriFor("high_text_contrast_enabled")`, emitting the current value first, then each change, with `distinctUntilChanged()`.
  2. In `setContent`, `val systemHighContrast by remember { highTextContrastFlow(contentResolver) }.collectAsState(initial = <current read>)`.
- **Acceptance criteria:** on a device, toggling "High contrast text" in Accessibility settings while a pill is showing switches the outline within 1 s.
- **Tests:** Robolectric: write the secure setting, notify the observer, and assert that the flow emits `true`.
- **Depends on:** B5.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix (accessibility).

---

## B6 — Safe actions

### B6.1 — No invented reply chips
- **Goal:** Smart-reply chips appear only when the app supplied them through `RemoteInput.choices`.
- **Why:** 05 §2 and P1-5: when the app sends no suggestions, the card invents "OK", "Sure" and "Thanks!", which send on one tap. 04 §6 keeps real smart replies as an Android-only strength.
- **Files:** `island/IslandView.kt:3830-3833` (`smartChoices`), and the chip row `:3835-3850`.
- **Steps:** `val smartChoices = replyAction?.remoteInputChoices.orEmpty()`. If it's empty, don't emit the `Row` at all, so no empty gap is left behind. Check `metrics.replyRowSpacing` above it.
- **Acceptance criteria:** the S2 state 12 fixture (`IslandOverlayCrashTest.message()`, a reply action without choices) shows no chips; this is an intended diff. A fixture with `remoteInputChoices = listOf("On my way")` shows one chip.
- **Tests:** S2 state 12 re-recorded, plus a new state "reply with app choices".
- **Depends on:** S2.2. Serialize with Phase 8 C3.6 (same reply row).
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix.

### B6.2 — Ringing calls and alarms tuck back instead of being dismissed
- **Goal:** A sideways swipe on a ringing call, a ringing alarm or a "Time's up" card collapses it to compact. The card stays, and the system notification is never cancelled.
- **Why:** 05 §2 gesture map and P1-5: ringing calls are not guarded, and a sideways swipe takes the Answer button away. Found while verifying: the swipe calls `clearCurrentNotification()` (`IslandStateManager.kt:226-230`), which **also cancels the notification in the shade**. 05 §6 spec: "Swipe sideways: dismiss the card (calls and alarms tuck instead)". Heads-up grammar.
- **Files:**
  - `island/IslandView.kt:1353-1358` (card 1, `POP_NOTIFICATION` branch), and the card 2 branch inside `:1512-1550` (at about `:1527-1531`)
  - `island/IslandStateManager.kt:226-230` (`clearCurrentNotification`), `:698-718` (`dismissCurrentNotification`)
- **Steps:**
  1. In both swipe handlers, if `currentNotif?.isIncomingCall == true || currentNotif?.isAlert == true`, call `triggerCollapseHaptic()` and `IslandStateManager.collapse()`, and reset the offset. Don't dismiss.
  2. Defence in depth: `clearCurrentNotification()` and `dismissCurrentNotification()` return `false` without changes while the active notification is ringing or an alert. Calls end through Decline; alarms end through Stop.
  3. Leave the card's follow-the-finger offset as it is (M4 owns fling and spring-back).
- **Acceptance criteria:** a swipe of 500 px on an incoming-call card leaves `activeNotification.isIncomingCall == true` and `isExpanded == false`. The shade notification is untouched (the `onCancelSystemNotification` callback isn't called).
- **Tests:**
  - `IslandStateBehaviorTest`: `clearCurrentNotification_refusesWhileRinging`, `clearCurrentNotification_refusesForAlert`
  - `IslandOverlayCrashTest`: a new `ringingCall_sideSwipe_tucksAndKeepsCard`
  - S1.3 unchanged
- **Depends on:** S1.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix.

### B6.3 — Open targets: the notification's own intent first
- **Goal:** Long-press, avatar and app-name taps open the exact conversation or detail (`contentIntent`), and media opens its `sessionActivity`. The launcher intent is only the fallback.
- **Why:** 05 §2 and P1-4: `launchAppOrIntent` tries the launcher first (`IslandView.kt:2350-2366`), so long-press lands on the app's home screen. The body tap already does the opposite (`:3710-3740`). 05 §5: "make long-press open the exact conversation". Phase 7 A1.8 encodes this order in `spec.open`.
- **Files:**
  - `island/IslandView.kt:2345-2386` (`launchAppOrIntent`), `:2388-2415` (`openAppForPriority`)
  - Call sites `:1722-1731` (bubble long-press), `:1939-1948` (pill long-press), `:3448` (media header), `:3787` (notification header), `:3984` (progress card)
- **API sketch:**
  ```kotlin
  /** contentIntent / sessionActivity first, then the launcher. Returns false if nothing could be opened. */
  fun openTarget(context: Context, primary: PendingIntent?, fallbackPackage: String?): Boolean
  ```
- **Steps:**
  1. Write `openTarget` with the order inverted: the PendingIntent first, keeping the existing `MODE_BACKGROUND_ACTIVITY_START_ALLOWED` options (`:2371-2375`), then the launcher.
  2. Point all call sites at it.
  3. Long-press on a notification no longer calls `dismissCurrentNotification()` (`:1729`, `:1946`). It calls `IslandStateManager.onNotificationOpened()` instead, the same as the body tap (`:3739`). Auto-cancel notifications clear; others keep their card, as tapping in the shade does.
  4. Fold the body tap's own copy (`:3710-3734`) into `openTarget` too.
- **Acceptance criteria:**
  - On a device, long-pressing a WhatsApp message pill opens that chat.
  - Long-pressing the media pill opens the player screen (`sessionActivity`).
  - A notification without a `contentIntent` still opens the app.
- **Tests:** `OpenTargetTest` (Robolectric): with a `PendingIntent` that records `send`, it is sent and the launcher is not used; with `null`, the launcher is used. `IslandStateBehaviorTest`: long-press on a non-auto-cancel card keeps the card.
- **Depends on:** none.
- **Effort:** S   **Risk:** low — the background-activity-start rules on Android 14+ are already handled by the options bundle the body tap uses.
- **Ships as:** Fix.
- **Done in code (2026-11-09):** `island/OpenTarget.kt` `openTarget(context, primary, fallbackPackage)` replaces `launchAppOrIntent` at all eight call sites (long-press on pill and bubble, media cover, notification avatar and body, progress card, and `LiveActivities.kt` `open`). Long-press calls `onNotificationOpened()`. Tests: `OpenTargetTest` (own intent sent and launcher not used; launcher when there's none or it's cancelled) and `IslandLongPressTest` (UI long-press keeps a non-auto-cancel card, clears an auto-cancel one; checked to fail with the old dismiss). Device check still open: WhatsApp chat, media player screen, and an app without a contentIntent.

### B6.4 — Swipe right no longer hides the island (D6)
- **Goal:** No horizontal swipe on the pill makes the island disappear. Left keeps minimising to the camera ring (as today). On the ring, a swipe right or a tap restores it.
- **Why:** 05 P1-1: swipe-right is a hide trap. The window turns not-touchable, and the island only comes back on the next event (`IslandStateManager.kt:96-100`, `IslandOverlayService.kt:977-978`). D6 = yes.
- **Scope note:** PLAN D6 says "swipe-inward-to-minimise". True inward and outward geometry, measured from where the swipe starts and mirrored for RTL, is part of M4's `Modifier.islandGestures` (PLAN M4: "Inward minimises", "RTL-aware"). B6.4 only removes the trap and keeps today's left-swipe minimise, so users don't get two gesture changes in a row.
- **Files:**
  - `island/IslandView.kt:1848-1877` (pill drag handler; hide at `:1866-1869`)
  - `island/IslandStateManager.kt:88-111`. Keep `hideIsland()` as an API: Phase 8 C3.5 (TalkBack "Hide until next update") and C4 (QS tile, "Pause island 1 h") call it.
  - Test: `app/src/test/.../IslandOverlayCrashTest.kt:185-194` (`swipeRightTwice_onMediaPill` asserts `userHidden`)
- **Steps:**
  1. Delete the `dragTotal > threshold -> hideIsland()` branch. A right swipe on a non-minimised pill does nothing; the `change.consume()` is kept so the touch doesn't leak.
  2. Update the comment at `:1848-1849`.
  3. Change `swipeRightTwice_onMediaPill` to assert `!userHidden && !userMinimized`, and rename it `swipeRightTwice_onMediaPill_doesNotHide`.
- **Acceptance criteria:** in the crash test, a right swipe never sets `userHidden`. Left-swipe minimise and ring restore behave as before (`minimizedPill_tapRestoresAndExpands` stays green).
- **Tests:** the updated crash test; S1.3 row 13 (hidden) is still reachable through `hideIsland()` directly.
- **Depends on:** S1.1.
- **Effort:** S   **Risk:** low — users lose the hide gesture until C3.5 or C4 add the replacement. State this in the release notes.
- **Ships as:** Fix (gesture).

---

## B7 — Truthful widgets and FPS dead code

### B7.1 — Weather: never show made-up data
- **Goal:** The weather widget shows real data, or an honest "Weather unavailable" state. There are no synthetic hourly entries and no fixed UV value, and the timezone-guessed city is labelled as approximate.
- **Why:** 04 P1-11 and R10. 06 P2 "Fabricated data". PLAN §1 "Fabricated weather".
- **Files:**
  - `service/PersistentWidgetTelemetryManager.kt:279-338` (`fetchWeather`: synthetic `hourly` list at `:309-315`, `uvIndex = 3` at `:324`), `:340-357` (`useFallbackWeather`, which publishes 22 °C "Partly Cloudy"), `:375+` (`approximateCoordsForTimeZone`)
  - `model/PersistentWidgetModels.kt:16-32`. The `WeatherInfo` **defaults** are themselves fabricated: 22 °C and a five-entry hourly list. They show before the first fetch.
  - `island/IslandStateManager.kt:162` (`updateWeather`) and its flow
  - The weather views in `island/IslandView.kt` (compact around `:2832`; expanded around `:4310-4370`)
  - Note: this file has **unstaged edits** (`git status`: `MM`). Commit or rebase onto them first.
- **Steps:**
  1. Make the weather flow `StateFlow<WeatherInfo?>`, initially `null`. Strip the model's fake defaults.
  2. On failure, publish `null`, or keep the last real value if it is under 3 h old, with `fetchedAtMs`.
  3. Fetch a real hourly forecast: add `&hourly=temperature_2m,weather_code&forecast_hours=5` to the open-meteo URL (`:284`), plus `uv_index_max` in `daily`. Or drop the hourly row when that data is absent; don't invent it.
  4. Add a `cityIsApproximate = true` field and render the city as "≈ Athens" in the expanded view. Leave the layout as is.
  5. For a `null` weather value: compact shows the condition icon only, and expanded shows "Weather unavailable" in `TextSecondary`, inside the same card.
- **Acceptance criteria:** in airplane mode, the widget never shows 22 °C. With network, the hourly temperatures match open-meteo's response.
- **Tests:** a parsing test with a recorded open-meteo JSON (`app/src/test/resources/weather/open_meteo_ok.json`) → real hourly entries; a failure path → `null`. S2: add one "weather unavailable" state.
- **Depends on:** S2.2.
- **Effort:** M   **Risk:** low.
- **Ships as:** Fix (trust).

### B7.2 — Network label: no hard-coded "5G"
- **Goal:** The network widget says "Wi-Fi", "Cellular" or "Network", which are true, instead of "5G".
- **Why:** 04 P1-11: cellular is hard-coded to "5G" (`IslandView.kt:2852`, telemetry `:194`).
- **Files:** `island/IslandView.kt:2852` (compact: `if (networkSpeedInfo.isWifi) "Wi-Fi" else "5G"`); `service/PersistentWidgetTelemetryManager.kt:192-196` (`"Cellular 5G"`).
- **Steps:**
  1. Telemetry: `isCellular -> "Cellular"`.
  2. Compact: `text = networkSpeedInfo.networkLabel`, with `"Cellular"` shortened to `"Cell"` only if the compact slot is too narrow. Check against the S2 screenshot; never use an ellipsis.
  3. Detecting the real generation needs `READ_PHONE_STATE`. Out of scope.
- **Acceptance criteria:** on mobile data, the pill and card read "Cellular" (or "Cell"), never "5G" on an LTE phone.
- **Tests:** a unit test of the label mapping; an S2 network state, if it exists.
- **Depends on:** none. Serialize with B7.1 (same telemetry file).
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix (trust).

### B7.3 — Remove the FPS remnants
- **Goal:** No FPS code path, enum value, setting row or flow is left.
- **Why:** 04 §3 "FPS: removed (`IslandView.kt:473`), but the enum and views remain"; R12. PLAN B7.
- **Files** (44 references; grep `showFps|SHOW_FPS|fpsInfo|updateFps|IslandPriority.FPS|FpsInfo|onRecalibrateFps`):
  - `island/IslandView.kt:407, 473, 476, 481, 2583, 2794-2805, 2939-2941, 3012, 4273-4299`
  - `model/IslandEvent.kt:168` (`FPS` enum value)
  - `model/PersistentWidgetModels.kt:3-8` (`FpsInfo`)
  - `model/IslandSettings.kt:10` (`"FPS"` in `DEFAULT_FEATURE_PRIORITIES`), `:54` (`showFps`)
  - `island/IslandStateManager.kt:153` (`updateFps`), `:217` (`onRecalibrateFps`)
  - `service/PersistentWidgetTelemetryManager.kt` (the FPS parts, including the unstaged `publishRefreshRate`)
  - `ui/main/MainScreen.kt:719, 732, 745`, and the `reorderableKeys` list at `:646-660`
  - `data/SettingsRepository.kt` (the `SHOW_FPS` key)
- **Steps:**
  1. Delete them all. Leave the DataStore key unread; it's harmless.
  2. In both settings mappers, filter `"FPS"` out of a stored `featurePriorities` (the append-missing-defaults logic around `SettingsRepository.kt:137-142` and `:209-214` must not re-add it).
- **Acceptance criteria:** the grep returns 0. A stored `"CALL,MEDIA,FPS,WEATHER"` loads as `CALL,MEDIA,WEATHER,…`. All tests and the S2 screenshots are unchanged.
- **Tests:** a repository round-trip test for the migration; the full suite.
- **Depends on:** B7.1 (same telemetry file); after B3.2 if run near it (`MainScreen.kt`).
- **Effort:** S   **Risk:** low.
- **Ships as:** Cleanup.

---

## B8 — Battery and main-thread quick wins

### B8.1 — Ambient frame budget: the equalizer and the recording dot
- **Goal:** The playing media pill no longer asks for a frame on every vsync, and the pulsing recording dot no longer recomposes.
- **Why:**
  - 06 P0-3: `EqualizerVisualizer` requests a frame every vsync for as long as music plays, but its data arrives at 20 Hz or less (`AudioLevels.kt:138`). That holds adaptive displays at their top refresh rate.
  - 06 P1-6: `PulsingRecordingDot` reads an infinite transition during composition.
  - 06 R5.
- **Files:** `island/IslandView.kt:4170-4218` (equalizer), `:4143-4161` (dot); `island/AudioLevels.kt:138` (the data rate).
- **Steps:**
  1. Equalizer: inside the frame loop, skip the update unless at least 33 ms have passed since the last applied frame. With a real signal, also skip when `OutputAudioBands.bands` haven't changed since the last applied frame (keep a copy). Then sleep with `delay(33)` between frames instead of chaining `withInfiniteAnimationFrameMillis` back to back. Keep the attack and release smoothing. Battery Saver (`AudioPowerPolicy.lowPower`) halves the rate again.
  2. Dot: read `alpha` inside `Modifier.graphicsLayer { scaleX = …; scaleY = …; alpha = … }` and draw a solid red circle with `drawBehind`. No `.scale()` or `.background(color.copy(alpha))` in composition.
- **Acceptance criteria:**
  - S4.1's "5 s playing media pill" count drops by at least 50 %.
  - On a device with "Show refresh rate" (developer options) on, the display drops below its top rate while music plays and the island rests.
  - S2: the pill looks the same (the equalizer frame at a fixed clock may differ by a few pixels; review it).
- **Tests:** S4.1 (update the baseline down); S2 review.
- **Depends on:** S4.1, S2.2.
- **Effort:** S   **Risk:** low — the equalizer looks slightly less fluid at 30 fps. That's intended (06 R5: ≤ 30 fps).
- **Ships as:** Perf.

### B8.2 — Media slider driven by the playback clock, no 250 ms ticker
- **Goal:** The expanded media card no longer recomposes on every frame while playing. The slider moves in the draw phase, and the time labels update once per second.
- **Why:** 06 P1-6: `ExpandedMediaView` has a 250 ms ticker plus an endless 260 ms tween (`IslandView.kt:3304-3334`), so the whole card recomposes every frame. 06 R5.
- **Files:** `island/IslandView.kt:3282-3340` (`livePosition` ticker at `:3303-3315`; `smoothSliderProgress` tween at `:3329-3333`); `model/IslandEvent.kt:29-35` (`computeCurrentPosition`).
- **Steps:**
  1. Replace `livePosition` and `smoothSliderProgress` with a `State<Long>` lambda, `positionNow = { stablePosition(...) }`. The slider's progress drawing reads it inside `drawBehind` or `graphicsLayer`, and a `LaunchedEffect` drives redraws with `withFrameMillis` only while expanded, playing and not seeking. That invalidates the draw only, not composition.
  2. The time labels read a `secondTick` state that changes once per second, aligned to the second boundary, as `IslandComponents.kt:752-760` already does.
  3. Keep the seek logic (`isSeeking`, `isSeekPending`, the `stablePosition` drift guard `:3298-3301`) as is.
- **Acceptance criteria:**
  - S4.1: a new scenario "expanded media, playing, 3 s" shows at most about 3 root and card recompositions per second (record it).
  - On a device, the slider moves smoothly, seeking works, and the time labels tick each second.
- **Tests:** S4.1 new scenario; S2 state 11 unchanged at the frozen clock.
- **Depends on:** S4.1, S2.2.
- **Effort:** M   **Risk:** med — seek edge cases. Mitigation: the device check with Spotify and YouTube Music, including seek and pause.
- **Ships as:** Perf.

### B8.3 — Glass pane windows only when "Liquid glass" is on, and never blocking
- **Goal:** No extra windows or urgent-priority threads exist unless `liquidGlassCards` is on, and creating them never blocks the main thread.
- **Why:** 06 P1-3: the panes are created unconditionally (the default is off). That is 2 windows plus 2 `URGENT_DISPLAY` threads, and `create()` blocks on a latch for up to 1.5 s. 06 R7.
- **Files:**
  - `service/IslandOverlayService.kt:743-745` (unconditional `GlassPaneWindows(...).create()`), `:757-762` (listener), `:823-838` (the settings collector, where on and off must be handled), `:1098-1102` (teardown)
  - `service/GlassPaneWindows.kt:63` (`HandlerThread`), `:175-195` (`create`, with `CountDownLatch.await(1500, ms)` at `:192`)
- **Steps:**
  1. Create the panes only when `currentSettings.liquidGlassCards` is on. In the settings collector, create them on false→true and dismiss them on true→false.
  2. `create()` returns at once. The panes report readiness through a callback, and `update()` calls before that are kept as "latest rects" and applied when ready. Ordering matters: the panes must sit beneath the island (`:743` comment). Use the readiness callback to add the island view after the panes, or re-add it, when glass is on.
- **Acceptance criteria:**
  - With glass off, `adb shell dumpsys window | grep GlassPane` shows nothing, and there are no `GlassPane*` threads (`ps -T`).
  - With glass on, the expanded card still blurs what's behind it, with no frame where the blur window sits above the island.
- **Tests:** a Robolectric service test: with glass off, `glassPaneWindows == null` after `initOverlay` (expose it with `@VisibleForTesting`). Device check for ordering.
- **Depends on:** B1.1 (same `onCreate` and `initOverlay` area; avoids conflicts).
- **Effort:** S   **Risk:** med — window z-order with glass on. Mitigation: the device check.
- **Ships as:** Perf.

### B8.4 — No `runBlocking` in `Service.onCreate`
- **Goal:** The overlay service starts without blocking the main thread on DataStore.
- **Why:** 06 P1-9; R7: "Replace `runBlocking` with 'start with the defaults, apply settings when they arrive'".
- **Files:** `service/IslandOverlayService.kt:471-477` (`runBlocking(Dispatchers.IO) { settingsFlow.first() }`), `:478-479`, `:494-497` (telemetry start uses settings), `:550-556` (`onStartCommand` → `initOverlay`), `:823-838` (settings collector).
- **Steps:**
  1. Keep `currentSettings = IslandSettings()` until the first emission, but **don't add the overlay or start telemetry before the first real settings arrive**. Otherwise a user with `isEnabled = false`, or a custom width, would see a wrong pill for a moment.
  2. `onStartCommand` still calls `startForegroundService()` at once (an FGS start-time requirement). `initOverlay()` moves into the first collection of `settingsFlow` (the collector at `:823` already calls `initOverlay()` when `composeView == null`).
  3. Telemetry `start(currentSettings)` moves to the first emission too.
- **Acceptance criteria:**
  - A StrictMode `detectDiskReads` debug policy, enabled in the debug `DebugCommands` init from B1.1, reports no read in `IslandOverlayService.onCreate`.
  - The island appears with the right size after reboot (`BootReceiver`) and after app start.
- **Tests:** a Robolectric service test: after `create()` the overlay isn't added; after the first emission it is. A device reboot check.
- **Depends on:** B1.1, B8.3 (same block).
- **Effort:** S   **Risk:** low.
- **Ships as:** Perf.

### B8.5 — Touches no longer recompose the root
- **Goal:** Dragging the media or flashlight slider doesn't recompose `IslandOverlayRoot` or restart effects at the input rate.
- **Why:** 06 P1-5: the root `pointerInput` writes `isTouching` and `lastTouchAt` on every pointer event, and `lastTouchAt` is a `LaunchedEffect` key (`IslandView.kt:1218-1245`).
- **Files:** `island/IslandView.kt:1216-1226` (auto-collapse effect), `:1238-1246` (observer).
- **Steps:**
  1. Replace both states with a remembered `Channel<Boolean>(CONFLATED)` (pressed / released), sent from the `PointerEventPass.Initial` observer only when the pressed state **changes**.
  2. One `LaunchedEffect(isExpanded, isKeyboardFocused, isCallRinging)` runs a loop: wait for "released", then `withTimeoutOrNull(3000) { channel.receive() }`. If it times out, collapse; if a press arrives first, keep waiting. Composition never reads the touch state.
  3. Keep the 3 s value. F3's `IslandTimings` replaces it later.
- **Acceptance criteria:**
  - S4.1: a new scenario "drag the flashlight slider for 1 s at 120 Hz input" records at most 2 root recompositions (today it is about 120).
  - Auto-collapse still fires 3 s after the last touch, and never while a finger is down (crash test `minimizedPill_tapRestoresAndExpands` green).
- **Tests:** S4.1 scenario; a new `IslandOverlayCrashTest` case: hold a press for 5 s with no collapse, release, then collapse 3 s later.
- **Depends on:** S4.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Perf.

### B8.6 — One settings mapper, distinct emissions
- **Goal:** Preferences map to `IslandSettings` in exactly one function, and the settings flow emits only real changes.
- **Why:** 06 §1 Settings: "The mapping code is duplicated (`SettingsRepository.kt` L69–131 and L133–195)", and is now at `:85-145` and `:157-216`. Each new key (B3.1, Phase 8 C1 and C4) must be added twice, and a missed line drops it silently. 06 R11. Phase 8 expects B8 to unify the mapper.
- **Files:** `data/SettingsRepository.kt:76-145` (flow mapper), `:147-218` (edit mapper), `:220-272` (writer).
- **Steps:**
  1. Write `internal fun Preferences.toIslandSettings(): IslandSettings` once, and use it in both `settingsFlow` and `updateSettings`.
  2. `settingsFlow = data.map { it.toIslandSettings() }.distinctUntilChanged()`.
  3. Pair the writer with a `MutablePreferences.write(s: IslandSettings)`.
  4. Add a round-trip test that **reflects over `IslandSettings`'s constructor parameters** and asserts that a non-default value survives the write and read for each one. That catches a forgotten key for good.
- **Acceptance criteria:** the round-trip test is green for every field. Behaviour is unchanged.
- **Tests:** `SettingsRepositoryRoundTripTest` (Robolectric DataStore in a temp dir).
- **Depends on:** B3.1 and B7.3 (they add and remove keys; land them first, or rebase).
- **Effort:** S   **Risk:** low.
- **Ships as:** Refactor.

### B8.7 — Debounced slider writes
- **Goal:** Dragging a Dimensions or Appearance slider updates a preview state in memory, and writes DataStore once, when the drag ends.
- **Why:** 06 P2 "Settings writes per slider tick": every tick writes DataStore, recomposes the overlay and relayouts the window (`MainScreen.kt:1856-1908` in the research, now `:1890-1950`). Phase 8 C2.2 (live preview) needs this.
- **Files:**
  - `ui/main/MainScreen.kt:1768-1805` (the slider row composable), the call sites `:1897, 1907, 1917, 1927, 1938, 1949` (width, height, top offset, horizontal offset, expanded scale, animation speed), `:2277` (outline thickness), `:2785, 2792` (curve editor)
  - `ui/main/MainScreenViewModel.kt:121-125` (`updateSettings`)
- **Steps:**
  1. The slider row keeps its own `var dragValue by remember(value) { mutableFloatStateOf(value) }`, calls `onValueChange` → `dragValue = it` locally, and calls `onCommit(dragValue)` from `onValueChangeFinished`.
  2. Add `MainScreenViewModel.previewSettings: MutableStateFlow<IslandSettings?>`. While a drag is active, the overlay reads the preview through `IslandStateManager.previewSettings`, and the service merges it over `currentSettings` without writing. This is what C2.2 uses. If that is too much for one PR, ship step 1 alone: the overlay then updates on release only, which is acceptable until C2.2.
  3. Commit with one `updateSettings` on release.
- **Acceptance criteria:**
  - A 2 s drag of the Width slider produces exactly 1 DataStore write (count it with a test double for the repository).
  - The value label next to the slider updates live.
- **Tests:** a `MainScreenViewModelTest` case; a Compose UI test of the slider row: drag, then exactly one commit.
- **Depends on:** B8.6.
- **Effort:** S   **Risk:** low. `MainScreen.kt` is shared with Phase 8: one session at a time.
- **Ships as:** Perf.

---

## B9 — Typography quick fixes

### B9.1 — Restore `tnum` on the network values
- **Goal:** The network speed digits keep tabular widths on every OEM font.
- **Why:** 01 finding 2 (P0): these calls use `style = TextStyle(...)`, which **replaces** the island-wide `LocalTextStyle` with `fontFeatureSettings = "tnum"` (`IslandOverlayService.kt:726`). These are the fastest-ticking numbers in the app, and they wobble each second on OEM fonts.
- **Files:** `island/IslandView.kt:4579, 4585, 4610, 4616` (the only four `style = TextStyle(` in `island/`).
- **Steps:** change each to `style = LocalTextStyle.current.merge(TextStyle(...))`, or pass `fontSize`, `fontWeight` and `color` as `Text` parameters directly, as the surrounding code does. The colour literal at `:4579` and `:4610` becomes `IslandTokens.TextSecondary` in B5.2; whichever lands second rebases.
- **Acceptance criteria:** a grep for `style = TextStyle(` in `island/` returns 0. The S2 network state is pixel-identical on Robolectric (Roboto is tabular already, 01 finding 2 [V]).
- **Tests:** S2. A unit test that the merged style has `fontFeatureSettings == "tnum"`, using a small composable test that reads the `TextLayoutResult` style.
- **Depends on:** S2.2. **Serialize with B5.2** (same lines).
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish (typography fix).

### B9.2 — A readable bubble numeral (at least 11 sp)
- **Goal:** The download percentage in the split bubble is 11 sp or larger, fits its ring at any supported font scale, and falls back to the ring alone when it can't fit.
- **Why:** 01 finding 5 (P0): an 8 sp numeral inside a 17 dp ring (`IslandView.kt:2525-2537`) is below Material's 11 sp floor, and at font scale 1.3 "100" is wider than the ring. 01 R4: "Bubble: show the ring alone, or 11–12 sp at weight 700 with condensed width in a ring of at least 22 dp. Nothing below 11 sp."
- **Files:** `island/IslandView.kt:2510-2539` (`AuxiliaryBubbleContent`, PROGRESS branch), with the bubble size `= settings.heightDp.dp` at `:981`.
- **Spec:**
  - Ring diameter `= (bubbleSize - 10.dp).coerceIn(22.dp, 28.dp)`, stroke 2 dp, and the same colours as today.
  - Numeral: 11 sp, `FontWeight.Bold`, `tnum` (inherited), no "%".
  - Measure `"${pct}"` with `rememberTextMeasurer()`. If the width is greater than ring diameter − 2 × stroke − 2 dp, draw the ring alone.
  - The indeterminate branch (`:2517-2523`) keeps its size.
- **Acceptance criteria:**
  - At font scale 1.0 and the default height of 36 dp, a 26 dp ring shows "42", and "100" fits.
  - At font scale 1.5, the ring shows alone with no overflow.
  - Bounds: the S1.3 bubble bounds are unchanged; only the inner content changes.
- **Tests:** S2: re-record a new "dual: media + download in bubble" state at 1.0 and 1.5 (an intended diff). A unit test of the fit rule.
- **Depends on:** S2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish (typography fix).

---

## B10 — Parsing off the main thread

### B10.1 — Sampled, off-main album-art decode
- **Goal:** Album art from a URI, or an oversized metadata bitmap, is decoded at a bounded size off the main thread, and the result is applied only if the track hasn't changed meanwhile.
- **Why:** 06 P0-2: a full-resolution `BitmapFactory.decodeStream` of the art URI with no sampling, which is also an OOM risk. 06 R4: "URI decode with `inSampleSize` capped at about 256 px".
- **Files:** `service/NotificationReceiverService.kt:547-585` (art resolution: metadata bitmaps `:547-549`, cache `:551-556`, URI decode `:559-571`, large-icon fallback `:574-580`); `:484` (`updateMediaInfo`).
- **Steps:**
  1. Write `ArtDecoder.decode(uri, maxPx = 512)` (512, because the expanded card's art and blur need more than the compact 256): read the bounds with `inJustDecodeBounds`, pick a power-of-two `inSampleSize`, then decode. Run it on `Dispatchers.IO`.
  2. `updateMediaInfo` publishes the media with the cached art, or `null` art, at once, then launches the decode keyed by `artKey`. When it finishes, it re-publishes only if `cachedArtKey`'s target still equals `artKey`.
  3. Metadata bitmaps larger than 1024 px are scaled down off the main thread the same way.
- **Acceptance criteria:**
  - A test with a 4000 × 4000 PNG URI decodes to at most 512 px, with no main-thread decode (assert with a `Looper` check in a test seam).
  - Skipping tracks quickly never shows the previous track's art on the new one.
- **Tests:** `ArtDecoderTest` (Robolectric, with a content URI from a test `ContentProvider`); an ordering test with two decodes completing out of order.
- **Depends on:** S4.3 (to see the improvement in a trace; not blocking).
- **Effort:** M   **Risk:** med — ordering. Mitigation: the `artKey` check.
- **Ships as:** Perf.

### B10.2 — Debounced `refreshMedia`, without `activeNotifications` binder calls
- **Goal:** Bursts of media callbacks and window events cause one media refresh. The media path never calls `activeNotifications`.
- **Why:**
  - 06 P0-2: the `activeNotifications` binder call unparcels every notification, including bitmaps, on every `updateMediaInfo`. `refreshMedia` runs on every playback change, metadata change and app switch.
  - 06 R4: "Debounce `refreshMedia` (about 100 ms, and skip it when the token and state are unchanged)."
  - Found while verifying: `getControllerTitle` (`:400-413`) also calls `activeNotifications`, and `findBestController` calls it once per controller, several times per refresh.
- **Files:**
  - `service/NotificationReceiverService.kt:343-355` (`refreshMedia`), `:371-384` (controller callbacks → `refreshMedia()`), `:409` and `:534` (`activeNotifications` lookups), `:170` and `:300-301` (other uses; check whether they are in the media path)
  - `:1114` (posted media notification → `refreshMedia(directController)`)
  - `service/IslandAccessibilityService.kt:47` (`refreshMedia()` on every `TYPE_WINDOW_STATE_CHANGED`)
- **Steps:**
  1. `refreshMedia(candidate)` stores `candidate` (if any) and posts a single coalesced run 100 ms later on the main handler (latest wins).
  2. In the run, compute `(token, playbackState.state, metadata title/artist/album, position bucket)`. If it equals the last applied value and there is no new candidate, skip the work.
  3. Keep a `mediaNotifications: MutableMap<String, StatusBarNotification>` updated in `onNotificationPosted` (from the media branch `:1100-1116`) and `onNotificationRemoved`. Read from it instead of `activeNotifications` at `:409` and `:534`.
  4. The accessibility service calls `refreshMedia()` only when the foreground package **changes**, not on every window event.
- **Acceptance criteria:**
  - A test fires 20 `onPlaybackStateChanged` callbacks in 50 ms and sees 1 `updateMediaInfo` run.
  - The media pill still updates within 150 ms of play or pause on a device.
  - `activeNotifications` is no longer referenced in the media path.
- **Tests:** `MediaRefreshDebounceTest` (Robolectric, main looper paused, fake controllers).
- **Depends on:** B10.1 (same function, so serialize).
- **Effort:** M   **Risk:** med — a missed final state after a burst. Mitigation: trailing-edge debounce, so the last event always runs.
- **Ships as:** Perf.

### B10.3 — Notification parsing on a parser dispatcher, applied in post order
- **Goal:** The heavy part of `onNotificationPosted` runs on a single background thread, and the results reach `IslandStateManager` on main in post order. A late result for a removed notification is dropped.
- **Why:**
  - 06 P0-2: RemoteViews inflation (`views.apply`), `Icon.loadDrawable` and package-manager lookups all run on the main looper, in the same slot as the pop-in animation.
  - 06 R4: "A single-thread `NotificationParser` dispatcher… Results are posted to the store… parse results must apply in post order, so use a sequence number."
  - **Phase 7 assumes this exists** (its conventions: "Parsing happens off the main thread on the B10 parser dispatcher", "B10 sequence number"). PLAN's B10 text names only the art decode and the debounce; this task closes that gap.
- **Files:**
  - `service/NotificationReceiverService.kt:1035-1325` (`onNotificationPosted`)
  - The heavy calls: `:1207-1213` (`getApplicationInfo`, `getApplicationLabel`), `:1215` (`extractNotificationBitmap` → `loadDrawable` `:1366`, `:1392-1395`), `:1224-1226` (call-person icon), `:1164-1168` → `readCustomViewTimer` → `views.apply` at `:1440`
  - Main-only parts that must stay on main: `:1056-1097` (recording and call state machines with jobs), `:1100-1116` (media → `refreshMedia`), and all `IslandStateManager.*` writes
  - `:1327+` (`onNotificationRemoved`)
- **API sketch:**
  ```kotlin
  private val parser = Executors.newSingleThreadExecutor { Thread(it, "db-parser").apply { priority = Thread.NORM_PRIORITY - 1 } }.asCoroutineDispatcher()
  private val seq = HashMap<String, Long>()           // main-confined: sbn.key -> latest sequence
  private var nextSeq = 0L
  // onNotificationPosted (main): cheap filters first (:1038-1049, screen rec, call, media) → val s = ++nextSeq; seq[key] = s
  //   serviceScope.launch(parser) { val info = buildNotificationInfo(sbn, settingsSnapshot) ; withContext(Main) { if (seq[key] == s) apply(info) } }
  // onNotificationRemoved (main): seq[key] = ++nextSeq  → any in-flight parse for the key is discarded
  ```
- **Steps:**
  1. Pull `:1118-1314` into a pure-ish `buildNotificationInfo(sbn, settings, ranking, fg): ParseResult`, where `ParseResult` is `Drop`, `Ignore`, `Progress(info)` or `Notification(info)`. It reads only its inputs, plus `packageManager` and the drawables (thread-safe for reads).
  2. `dropIfShown()` and the final routing (`:1318-1324`) are applied on main from the `ParseResult`.
  3. The `liveTimerBases.stabilize` call (`:1266`) mutates shared state: run it on main in `apply`, or confine it to the parser thread (it is only used there). Pick one and document it.
  4. Read `foregroundPackage` (`:1203`) at post time, on main, and pass it in.
- **Acceptance criteria:**
  - S5.3 table unchanged.
  - New test: post A (slow, an inflated custom view), then post B for the same key, then remove the key. The state ends empty, and A never overwrites B.
  - A trace (S4.3) shows `db.posted` work off the main thread.
- **Tests:** S5.3; `ParserOrderingTest` (Robolectric with a controllable parser dispatcher); `IslandStateBehaviorTest` unchanged.
- **Depends on:** S5.3, B4.2 and B4.3 (they edit the same function; land them first), B11.2 (the guard wraps the parse).
- **Effort:** M   **Risk:** med — ordering, and the call or recording state machines that must stay on main. Mitigation: steps 2–4, and S5.3 as the gate.
- **Ships as:** Perf.

### B10.4 — Don't re-inflate a running clock app's custom view on every repost
- **Goal:** A running Google Clock or AOSP timer, which reposts about once per second, inflates its RemoteViews once per state change, not once per second.
- **Why:** 06 P0-2: "For clock and recorder apps this runs on every repost, about once per second." 06 R4: "Cache by notification key and post time."
- **Files:** `service/NotificationReceiverService.kt:1164-1168` (the `customTimer` decision), `:1430-1470` (`readCustomViewTimer`, with `views.apply` at `:1440`).
- **Steps:**
  1. Cache `key → (actionsSignature, chronometer base, running, countDown, label, timeText, at)`.
  2. On a repost with the same action titles (pause versus resume flips them) and a cached running timer, reuse the cached base and skip inflation. Re-inflate when the actions change, or every 30 s as a safety net.
  3. Clear the entry in `onNotificationRemoved`.
- **Acceptance criteria:** with a running timer fixture reposted 10 times, `views.apply` runs once (count it through a test seam). Pausing the timer (the actions change) re-reads it.
- **Tests:** a unit test with `gclock_timer_running_en` reposted, and `gclock_timer_paused_de` as the state change.
- **Depends on:** B10.3.
- **Effort:** S   **Risk:** low.
- **Ships as:** Perf.

---

## B11 — Crash containment

### B11.1 — Breadcrumb ring buffer in `CrashLog`
- **Goal:** Every crash report carries the last 64 island events, and non-fatal errors are recorded too.
- **Why:** 06 P1-7: `CrashLog` saves only the last stack trace (`util/CrashLog.kt:21-43`). 06 R8: "a breadcrumb ring buffer in `CrashLog`". Phase 7 writes breadcrumbs on every `runCatching` failure.
- **Files:** `util/CrashLog.kt:17-52`. Call sites: `service/NotificationReceiverService.kt:1035` (posted), `:1327` (removed), `:343` (`refreshMedia`); `service/IslandOverlayService.kt:916` (`updateWindowState`, the spec only); `island/IslandStateManager.kt:571` (`updateNotification`, kind only), `:752` (`expand`), `:762` (`collapse`).
- **API sketch:**
  ```kotlin
  object CrashLog {
      fun breadcrumb(tag: String, msg: String)            // ring of 64, thread-safe (synchronized ArrayDeque); msg must not hold user text
      fun nonFatal(tag: String, error: Throwable)         // breadcrumb + append to nonfatal.txt (last 20, rotated)
      // write(): appends "Breadcrumbs:" (oldest first, with elapsedRealtime) to last_crash.txt
  }
  ```
- **Steps:**
  1. Implement the API. Message format: `posted pkg=com.whatsapp kind=NONE keyHash=1a2b`. Never include the title or text (see the Conventions "Privacy").
  2. Add the call sites listed.
  3. The crash report screen in `MainScreen` reads `last_crash.txt` as today, so it shows breadcrumbs with no UI change.
- **Acceptance criteria:** a forced crash in a debug build writes a report with up to 64 breadcrumbs. A grep of the report for the test message's text finds nothing.
- **Tests:** `CrashLogTest`: ring overflow; `nonFatal` rotation; the report format.
- **Depends on:** none.
- **Effort:** S   **Risk:** low.
- **Ships as:** Reliability.
- **Done (2026-11-09):** `CrashLog` keeps a synchronized ring of 64 breadcrumbs (`elapsedRealtime tag msg`), appends them to the crash report, and `nonFatal` writes `nonfatal.txt` (last 20) with the class and top 8 frames but not the message, which can quote content. `attach(context)` lets tests write without installing the crash handler; `clear` also clears `nonfatal.txt`. Breadcrumbs: posted, removed, refreshMedia (NRS), window spec changes only (service), notification kind with a key hash, expand, collapse (state). `CrashLogTest` covers overflow, rotation, format and that no title, text or raw key reaches the report.

### B11.2 — Guard each notification and each media refresh
- **Goal:** One malformed notification, or a misbehaving media session, can't crash the process. It is skipped and logged.
- **Why:** 06 P1-7: "`onNotificationPosted` has no guard around each notification… `START_STICKY` then restarts it, and the same notification can crash it again in a loop." 06 R8.
- **Files:** `service/NotificationReceiverService.kt:1035-1325` (`onNotificationPosted`), `:1327+` (`onNotificationRemoved`), `:343-355` (`refreshMedia`; it catches `Exception` but then calls `updateMediaInfo()` unguarded at `:353`), `:484` (`updateMediaInfo`).
- **Steps:**
  1. `private inline fun guarded(tag: String, block: () -> Unit) = try { block() } catch (t: Throwable) { if (t is CancellationException) throw t; CrashLog.nonFatal(tag, t) }`. Don't catch `OutOfMemoryError` silently: log it and rethrow, because the process state is unknown.
  2. Wrap the posted body as `guarded("posted:${sbn.packageName}")`, and the removed body, `refreshMedia` and `updateMediaInfo` the same way.
  3. After B10.3, the guard wraps both the parser block and the main-thread apply.
- **Acceptance criteria:** a fixture with an `extras` value of the wrong type (for example `android.progress` as a String) is skipped with a `nonfatal.txt` entry, and the next fixture is processed normally.
- **Tests:** `NotificationGuardTest`, using an S5.1 fixture with deliberately wrong types.
- **Depends on:** B11.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Reliability.
- **Done (2026-11-09):** `guarded(tag)` wraps the posted body, a new `handleRemoved`, `refreshMedia` and `updateMediaInfo`. Bundle's typed getters return defaults for wrong-typed extras rather than throw, so the wrong-types fixture alone doesn't reach the guard; `NotificationGuardTest` checks it doesn't stop the listener, and forces a throw through `importanceForTest` to check the skip, the `nonfatal.txt` entry and the next notification.

### B11.3 — Crash-loop guard and safe mode
- **Goal:** After 3 crashes within 10 minutes, the island restarts in safe mode: the activity kind and package that were active at the crash are paused for 1 hour, and the user is told, with a way to undo it.
- **Why:** 06 P1-7 ("There is no safe mode") and R8: "N crashes in M minutes boots into safe mode with the last-active activity type disabled."
- **Files:**
  - `util/CrashLog.kt:26-29` (the handler: also record the crash time, and the last "primary" breadcrumb, `kind` + `pkg`)
  - New: `util/SafeMode.kt`, backed by SharedPreferences (`commit()`, synchronous, safe inside the crash handler)
  - `service/IslandOverlayService.kt:466-470` (`onCreate`: check safe mode), `:572-582` (the FGS notification: an extra action)
  - `service/NotificationReceiverService.kt` (skip the paused kind or package), `island/IslandStateManager.kt:571` (refuse a paused kind as a second line of defence)
- **Spec:**
  - The crash handler appends `now` to `crash_times` (keeping the last 5) and stores `last_primary = "<kind>|<pkg>"` from the newest `primary=` breadcrumb. Add one in `IslandOverlayRoot`'s primary change, cheap and posted, not per frame.
  - On service start: if at least 3 crash times fall within 10 min → `SafeMode.pause(kind, pkg, until = now + 1 h)`, then clear `crash_times`.
  - While paused: notifications of that kind from that package are skipped. If the kind is unknown, the package's island cards are skipped.
  - The FGS notification shows "Paused <App> on the island after repeated crashes", with a "Resume" action (an explicit `PendingIntent.getService`, `FLAG_IMMUTABLE`, consistent with B1 and Phase 8 C4.2).
  - The "fall back to a generic pill" error boundary per presentation (06 R8) is **deferred to Phase 7 A1/A3**: Compose can't catch composition exceptions, so it needs the adapter's pre-computed data.
- **Acceptance criteria:** in a debug build, a `DebugCommands` action `ACTION_CRASH_TEST` (debug only) that throws inside the notification path → after 3 crashes, the service starts with that package paused and the notification shows "Resume". Tapping it clears the pause.
- **Tests:** `SafeModeTest` (pure): the time window, the pause and expiry; `SafeModeServiceTest` (Robolectric): a paused package's notification is skipped.
- **Depends on:** B11.1, B11.2, B1.1 (the debug-only test action).
- **Effort:** M   **Risk:** med — a safe mode that triggers wrongly hides real activities. Mitigation: the 1 h expiry and the visible "Resume".
- **Ships as:** Reliability.

---

## Execution order

`∥` marks tasks that can run in parallel with the previous item, as long as they don't share files. Two writing sessions must never edit `IslandView.kt` at the same time (PLAN §5), and the same goes for `NotificationReceiverService.kt`, `IslandOverlayService.kt` and `MainScreen.kt`.

0. **Step zero:** commit or checkpoint the staged tree (it is on `main`, not `island-polish`), and confirm the suite is green.
1. **S1.1** harness. ∥ **S3.1** window math extraction (service only) ∥ **S5.1** fixture format ∥ **B1.1** debug receiver out of release (service `onCreate` only; doesn't touch `updateWindowState`, so it can share the service with S3.1 if the two sessions merge carefully, otherwise run it right after S3.1) ∥ **B11.1** breadcrumbs (`CrashLog` only)
2. **S1.2** content probes (`IslandView` test tags). ∥ **S3.2** window table ∥ **S5.2** corpus ∥ **S4.3** trace sections (NRS and service: one line each; land after B1.1)
3. **S1.3** bounds matrix. ∥ **S2.1** Roborazzi plus `IslandClock` (touches `IslandComponents` and `LiveActivities` one line each) ∥ **S4.1** recomposition probe ∥ **S5.3** fixture characterization
4. **S2.2** screenshot baseline. ∥ **S4.2** macrobenchmark (new module; device) ∥ **B1.2** receivers not exported ∥ **B6.3** open targets (pure function plus call sites in `IslandView`: run it before the other `IslandView` B tasks, or serialize)
5. **B2.1** foreground-aware occupancy (service and window math). ∥ **B4.1** score regex (`LiveActivities` only) ∥ **B11.2** guards (NRS)
6. **B3.1** live activities by default (settings, NRS gate, one line in `IslandView`, window math). Then **B3.2** toggle (`MainScreen`).
7. **B4.2** → **B4.3** → **B4.4** (all in NRS, sequential). ∥ **B4.5** dismiss signature (`IslandStateManager`) ∥ **B5.3** live high contrast (service `setContent`)
8. The `IslandView` lane, **one at a time**, in this order: **B6.2** tuck → **B6.4** no hide → **B6.1** reply chips → **B5.1** outline → **B5.2** text tokens → **B9.1** `tnum` escapes → **B9.2** bubble numeral → **B8.5** touch → **B8.1** ambient → **B8.2** media slider. ∥ in parallel, the non-`IslandView` lane: **B2.2** envelope (window math) → **B8.3** lazy glass panes → **B8.4** no `runBlocking` (service, sequential)
9. **B7.1** weather → **B7.2** network label → **B7.3** remove FPS (telemetry, model, `IslandView`, `MainScreen`: run it after the step 8 `IslandView` lane, or slot it in between two of those tasks). ∥ **B10.1** art decode → **B10.2** media debounce (NRS media path)
10. **B8.6** one mapper (after B3.1 and B7.3) → **B8.7** debounced sliders (`MainScreen`). ∥ **B10.3** parser dispatcher (NRS; after B4.2–B4.4 and B11.2) → **B10.4** clock-view cache
11. **B2.3** touchable region: spike first; device matrix; after B2.2. ∥ **B11.3** safe mode (after B11.2)

**Fastest payoff and highest risk first:** B1.1 (security) and B2.1 (dead touch strip) ship in the first week, right behind the Phase 0 tasks they need. B2.3 is the only high-risk task here; its fallback (B2.2) is already shipped by the time it starts.

**Phase 0 tasks that the B tasks need:** B2.x need S3.2. B3.1 and B4.x need S5.3. B5.x, B6.1, B7.1, B8.1, B8.2 and B9.x need S2.2. B8.1, B8.2 and B8.5 need S4.1. Every `IslandView` task uses S1.3 as a gate.
