# Phase 2 and 3: Foundations (F1–F6) and One living shape (M1–M5)

**Goal.** Give the island the architecture that Dynamic-Island-quality motion needs, then build the motion on it:
- **Phase 2 (F):** one scene model, one token set, one place for motion, haptics and timing, one type system on the system font, and a window that never clips the animation. The look stays the same.
- **Phase 3 (M):** the pill and the card become **one body** with one 4-D spring, read in the layout and draw phases (about 0 recompositions per frame). Content follows the shape, gestures follow the finger, and motion tiers cover Reduced and Battery Saver.

**This is polish, not a redesign (PLAN v2 §0).**
- `SquircleShape` at smoothing 0.6 stays the only surface shape (`island/IslandShapes.kt:31`). M1 still draws every surface with it.
- The palette stays. F2 only moves colors into `IslandTokens` and merges duplicates.
- No theme or preset system. 06 R9 (`IslandTheme`, `IslandStyle` presets) is **out of scope**. `IslandTokens` stays a plain `object`.
- No font bundling (D2). F4 fixes the metrics of the system font.
- Apple is the quality reference only. The spring formula and the principles (velocity hand-off, shape leads and content follows) are adopted. The asymmetry "eager in, calm out", the drop below the status bar, Android haptic meanings, swipe-down-to-shade and predictive back are Droidbubble's own (03 §5, 05 §5).

**Assumed inputs.** These tasks are referenced by ID:
- **S1:** characterization tests. They assert the settled `PILL`, `BUBBLE` and `CARD` bounds (`IslandTestTags`, `island/IslandView.kt:381-385`) across a state × settings matrix.
- **S2:** the Roborazzi screenshot baseline of about 15 states.
- **S3:** the window-geometry math extracted from `IslandOverlayService` into a pure, table-tested function.
- **S4:** a recomposition counter on the root, a macrobenchmark `FrameTimingMetric` for expand and collapse, and a Perfetto recipe.
- **B2:** touch pass-through. The hit area hugs the pill and bubble, and nothing is touchable when the card is hidden for the foreground app.
- **B6:** safe actions (D6). Swipe-right no longer hides, and ringing calls and alarms tuck instead of dismissing.
- **B8:** main-thread quick wins, including "touch no longer recomposes the root" (06 P1-5) and "no `runBlocking`".
- **B9:** the `tnum` escapes and an 11 sp minimum for the bubble numeral.
- **B11:** crash guards and breadcrumbs.

Phase 7 already references `deriveIslandScene` (F1), F2 tokens, `IslandTimings` and `IslandHaptics` (F3) and `WindowSpec` (F5). The names below match.

---

## Ground truth (verified against the working tree, 2026-11-08)

The research line numbers have drifted. Use these. `IV` = `island/IslandView.kt`, `IC` = `island/IslandComponents.kt`, `LA` = `island/LiveActivities.kt`, `OS` = `service/IslandOverlayService.kt`, `SM` = `island/IslandStateManager.kt`.

| What | Where |
|---|---|
| `IslandOverlayRoot` (one composable, about 1,950 lines) | `IV:389-2343` |
| 22 `collectAsState` flows | `IV:393-414` |
| Arbitration: visibility flags, dual, primary and secondary, outlines | `IV:446-567` |
| Speed and inline springs (`speedSq`, collapse = expandStiffness × 1.4) | `IV:569-685` |
| `expandProgress` | `IV:687-691` |
| Target geometry (alert widths, compact width, card size, corner, bubble, offsets) | `IV:893-1030` |
| Animated values, including `CardMorph` via `animateValueAsState` | `IV:1032-1051` |
| `rememberMorphSpeed` call sites | `IV:1055-1056` |
| Collapse-settled signal | `IV:1061-1070` |
| `cardContainerAlpha` | `IV:1084` |
| Card drag offsets (spring 0.76 / 420) | `IV:1086-1105` |
| Glass publishing, including `SideEffect { publishGlass() }` | `IV:1107-1152` |
| Parentheses shoot | `IV:1154-1181` |
| Haptic helpers | `IV:1183-1214` |
| Ringer raw waveform | `IV:801-828` |
| Auto-collapse after 3 s | `IV:1216-1226` |
| Idle early return | `IV:1228-1231` |
| Root touch observer (recomposes on every pointer event) | `IV:1238-1246` |
| Outside-tap backdrop | `IV:1250-1262` |
| Swipe-up collapse at −15 px | `IV:1286-1304` |
| Card 1: `.width/.height(animated)`, stretch, clip, border, drag | `IV:1308-1387` |
| Expanded content alpha thresholds (direction-dependent) | `IV:1446-1450` |
| `ExpandedCardContent` hard swap | `IV:1469` |
| Card 2 drag | `IV:1515-1556` |
| Pill container, bubble, liquid neck, notch bridge | `IV:1656-1820` |
| Pill body `.width(animatedCompactWidth)` | `IV:1822-1838` |
| Pill hide and minimise swipes (no motion until release) | `IV:1850-1877` |
| Idle-tap tween chain | `IV:1896-1932` |
| Compact alpha (×3.0 / ×1.8 by direction) | `IV:1954-1958` |
| Compact `AnimatedContent` keyed on priority only | `IV:2211-2264` |
| Parentheses | `IV:2270-2340` |
| `CardMorph` (4-D) | `IC:571-583` |
| `steppedBlurEffect` (cached) | `IC:605-614` |
| `rememberMorphSpeed` and `MorphSpeedMeter` (`System.nanoTime()` in composition) | `IC:629-663` |
| `NotificationCardMetrics`, `ProgressCardMetrics` | `IC:255-293` |
| `cardHeightFor` | `IC:211-230` |
| `LiveLayout` type sizes | `LA:346-351` |
| `RollingNumberText` | `LA:399` |
| `RollingNumberText` roll clips | `LA:470-475` |
| `SquircleShape` (always `Outline.Generic`) | `IslandShapes.kt:31-104` |
| `islandShape()` API-33 switch | `IslandShapes.kt:111-112` |
| `notchPillShape` | `IslandShapes.kt:140-145` |
| `runBlocking` settings read | `OS:471-477` |
| `touchManager` declared, never attached | `OS:595` |
| `disableMoveAnimation` (reflection on `privateFlags`) | `OS:597-603` |
| `initOverlay` geometry, which drifts from `updateWindowState` (0.90 × width, +44) | `OS:633-655` |
| Base text style `bodyLarge` + `tnum` (the source of the 24 sp line height) | `OS:726` |
| Font-scale clamp | `OS:727` |
| Glass panes created unconditionally | `OS:744-745` |
| State `combine` + `shapeKey` → `boostRefreshRate()` + `updateWindowState()` | `OS:765-803` |
| `boostRefreshRate` (a separate `updateViewLayout`) | `OS:863-884` |
| `applyLayoutParams` | `OS:887-914` |
| `updateWindowState` | `OS:916-1088` |
| Service occupancy (ignores `foregroundPackage`) | `OS:961-984` |
| Compact widths | `OS:986-1003` |
| Compact height = topOffset + height + 12 dp | `OS:1022` |
| `collapseDelayMs` computed, never used | `OS:1049-1050` |
| Collapse waits for `collapseSettled` (1.5 s timeout) | `OS:1052-1065` |
| Idle shrink after `(420 / speed).coerceIn(320, 750)` ms | `OS:1066-1083` |
| `collapseSettled` | `SM:143-148` |
| `glassPaneListener` | `SM:277-284` |
| Derived getters, `isOccupied` | `SM:311-352` |
| `currentPriority` (has CALL; the composable does not) | `SM:354-366` |
| Timers: unlock 2.2 s, connection 3 s, ringer 2.5 s, charging 4 s, auto-dismiss 3 s, pop-in 4.5 s | `SM:450, 468, 480, 492, 636, 693` |
| `expand` / `collapse` | `SM:752-766` |
| Touchable-region manager (hidden `OnComputeInternalInsetsListener` via reflection), never instantiated | `service/IslandTouchManager.kt:17-111` |
| User curves: `animationSpeed` 0.7, expand 0.8 / 380, appear 0.6 / 280, split 0.63 / 320 | `model/IslandSettings.kt:46, 63-68` |
| Settings mapping (duplicated) | `data/SettingsRepository.kt:76-146` and the copy inside `updateSettings` at `:147-215`; curves at `:129-134` and `:201-206` |
| Speed slider 0.5–2.0 | `ui/main/MainScreen.kt:1942-1951` |
| `AnimationCurveEditor` | `ui/main/MainScreen.kt:2650` |
| `minSdk = 29`, release signed with the debug key, `buildConfig = false` | `app/build.gradle.kts:16, 29, 44` |
| Battery Saver flag | `island/AudioLevels.kt:40-42` (`AudioPowerPolicy.lowPower`) |

Literal counts (with `grep -o`) feed the F2 ratchet:

| File | `Color(0x` | `N.sp` | `spring(` | `tween(` | `N.dp` | haptic calls |
|---|---|---|---|---|---|---|
| `IslandView.kt` | 79 | 33 | 20 | 23 | 250 | 23 |
| `LiveActivities.kt` | 9 | 0 | 1 | 3 | 27 | 0 |
| `IslandComponents.kt` | 0 | 1 | 1 | 2 | 85 | 0 |

There is **no CI in the repo** (no `.github/`). So "CI grep" means a JVM unit test that runs with `./gradlew test` (F2.1).

## Conventions for every task in this phase

1. **Pixel-identical by default.** Unless a task is marked **Intended diff**, the S2 screenshots must be byte-identical (or within Roborazzi's 0 % threshold) and the S1 settled bounds unchanged. An intended-diff task lists the exact screenshots it expects to change, and the PR shows before and after.
2. **Recomposition.** Every task reports the S4 counter for a 500 ms expand and a 500 ms collapse. F tasks: no worse than the S4 baseline. After M2.7: at most 2 recompositions of the shell per morph (start and settle), not one per frame.
3. **Device check** is required for anything touching the window, gestures, haptics or OEM behaviour (PLAN §6). The minimum is one Pixel on Android 15 or 16 plus one Samsung or Xiaomi, at 120 Hz where available, at speed 0.7 and 2.0.
4. **No new literals** in presentation code after F2.1 lands. The ratchet test fails the build.
5. **Tests** live next to the code they cover:
   - `app/src/test/java/com/droidbubble/app/island/…` for the island
   - `…/service/…` for the window
   - Pure JUnit by default. Use Robolectric `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")` with `@GraphicsMode(NATIVE)`, matching `IslandOverlayCrashTest.kt:38-40`, only when rendering.
6. **`IslandView.kt` contention:** almost every task here edits it. Never run two of these at once, and never alongside a V, I, A or C task on the same file. M2.1 (shell extraction) reduces this.
7. **Kotlin API sketches are contracts, not final code.** Names may change in review, but the shape (pure function, deferred read, single owner) may not.

---

## F1: Scene model, the single arbitration source

Today the composable (`IV:446-567`, `IV:893-1030`), the service (`OS:961-1003`) and `initOverlay` (`OS:633-655`) each decide what is showing, and they disagree:
- The service ignores `foregroundPackage`, which causes 06 P0-4.
- The service counts `isCallActive`; the composable doesn't.
- `hideOnLockScreen` is checked only by the service.
- `initOverlay` uses 0.90 × width and +44 dp, while `updateWindowState` uses +40 dp.
- `SM.currentPriority` has a CALL tier that the composable folds into `POP_NOTIFICATION`.

### F1.1 Lift arbitration into a pure `deriveIslandScene`
- **Goal:** One pure function returns what the island shows: mode, primary, secondary, dual, the visibility flags and occupancy. It is lifted **verbatim** from `IV:446-567`. Nothing consumes it yet.
- **Why:** 06 R1 / P1-1 / P1-8. It is the prerequisite for F1.2–F1.5, A2 and the window controller.
- **Files:**
  - New: `island/scene/IslandScene.kt`, `island/scene/DeriveIslandScene.kt`
  - Read-only source: `IV:419-422` (flashlight solo), `IV:438-567`
- **API sketch:**
  ```kotlin
  package com.droidbubble.app.island.scene

  /** Everything arbitration reads. A plain snapshot: no flows, no Compose. */
  @Immutable
  data class IslandInputs(
      val isExpanded: Boolean,
      val expandedPriority: IslandPriority?,
      val isPopIn: Boolean,
      val media: MediaInfo?,
      val notification: NotificationInfo?,
      val progress: NotificationInfo?,
      val recording: ScreenRecordingState,
      val charging: ChargingInfo?,            // whatever activeCharging holds today
      val isUnlocked: Boolean,
      val isBluetoothConnected: Boolean,
      val connectionAlert: IslandStateManager.ConnectionAlert?,
      val ringerMode: RingerMode?,
      val isFlashlightOn: Boolean,
      val isCallActive: Boolean,              // SM.isCallActive; used by the window today (OS:966)
      val foregroundPackage: String?,
      val userHidden: Boolean,
      val userMinimized: Boolean,
      val isDeviceLocked: Boolean,
      val flashlightSolo: Boolean,            // latched UI state (IV:419-422), passed in
  )

  @Immutable
  data class IslandEnv(val isLandscape: Boolean, val screenWidthDp: Int, val fontScale: Float)

  enum class IslandMode { OFF, IDLE, IDLE_PILL, MINIMAL, COMPACT, DUAL, SYSTEM_ALERT, EXPANDED }
  enum class SystemAlert { CHARGING, UNLOCK, CONNECTION, RINGER }

  @Immutable
  data class IslandScene(
      val mode: IslandMode,
      val primary: IslandPriority,             // IDLE when nothing
      val secondary: IslandPriority?,          // non-null only in DUAL (and in EXPANDED with card 2)
      val alert: SystemAlert?,                 // the "overshadowing" alert, if any
      val visible: VisibleActivities,          // isMedia, isNotification, … exactly as IV:459-476
      val activeCount: Int,                    // ephemeral count, IV:489-493
      val isDual: Boolean,
      val isOccupied: Boolean,
      val hasPersistent: Boolean,
      val showSecondaryCard: Boolean,          // IV:974
      /** True when something touchable is on screen. The window uses only this (fixes 06 P0-4 in F1.4). */
      val wantsTouch: Boolean,
  )

  fun deriveIslandScene(inputs: IslandInputs, settings: IslandSettings, env: IslandEnv): IslandScene
  ```
- **Steps:**
  1. Copy `IV:438-567` into `deriveIslandScene`. Replace each `by` read with an `inputs.` read. Keep every expression, including the `isFps = false` dead branch. A later task removes it; this one must stay verbatim.
  2. Add `mode` and `wantsTouch` as **derived** fields. They are computed from the copied flags and change nothing. `wantsTouch` mirrors the composable's view of occupancy, including the foreground-package rule.
  3. Write table tests.
- **Acceptance criteria:**
  - The function is pure (no `IslandStateManager`, no Android `Context`).
  - Every row of the S1 state × settings matrix produces the primary and secondary that S1 observes on screen.
  - S2 untouched, because nothing is wired.
- **Tests:** New `island/scene/DeriveIslandSceneTest.kt`, pure JUnit, at least 30 rows:
  - every single activity
  - every pair, for dual
  - each system alert overshadowing dual
  - the foreground app hides its card, except an incoming call
  - `priorityApps` promotion
  - `featurePriorities` reordering, with FLASHLIGHT missing from the list
  - flashlight solo
  - `isPopIn` swapping the secondary
  - persistent-only dual (weather + network)
  - landscape with `showInLandscape` off
- **Depends on:** S1.
- **Effort:** M   **Risk:** low (pure, unwired).
- **Ships as:** Plumbing.

### F1.2 The composable consumes the scene
- **Goal:** `IslandOverlayRoot` calls `deriveIslandScene` once per state change. It deletes its own copy of `IV:446-567`.
- **Why:** This makes the scene the single source on the UI side, with no behaviour change.
- **Files:** `IV:389-567`.
- **API sketch:**
  ```kotlin
  val inputs = IslandInputs(isExpanded, expandedPriority, isPopIn, media, notification, /* … */ flashlightSolo = flashlightSolo)
  val scene = remember(inputs, settings, env) { deriveIslandScene(inputs, settings, env) }
  val primaryPriority = scene.primary   // local vals kept as aliases so the diff stays small
  ```
- **Steps:**
  1. Build `IslandInputs` from the existing `by` values.
  2. Replace the derivations with `scene.*` aliases. Keep the local names (`isMedia`, `isDual`…) as `val x = scene.visible.x`, so the 1,500 lines below don't change.
  3. Keep `allowInOrientation` and the early return (`IV:440-444`).
- **Acceptance criteria:** S1 bounds unchanged. S2 pixel-identical. S4 no worse. `IslandOverlayCrashTest` and `IslandStateBehaviorTest` green.
- **Tests:** existing suites only.
- **Depends on:** F1.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Refactor.

### F1.3 Target geometry as a pure function
- **Goal:** Move `IV:893-1030` into a pure `deriveIslandTargets(scene, settings, env, measured)` that returns target sizes in dp:
  - the alert widths
  - the compact width
  - card 1 width, height, corner and top
  - card 2 height
  - the bubble resting X
  - the horizontal offset
  - the card spacing
- **Why:** The window (F5.1) and the one-body shell (M1) need the same targets. Text measurement stays in the composable and is passed in.
- **Files:**
  - New: `island/scene/IslandTargets.kt`
  - Edit: `IV:893-1030`
- **API sketch:**
  ```kotlin
  /** Heights that need a TextMeasurer: measured in composition, passed in. */
  @Immutable data class MeasuredHeights(val notification: Dp, val progress: Dp?)

  @Immutable
  data class IslandTargets(
      val compactWidth: Dp,              // IV:900-911
      val card1: IslandGeometry,         // width/height/corner/top: IV:916-961, 1024-1028
      val card2Height: Dp,               // needs card2Expanded: passed as a flag until M3.2 removes the delay
      val bubbleRestX: Dp,               // IV:981-984
      val horizontalOffset: Dp,          // IV:1016-1022
      val cardSpacing: Dp,               // IV:1030
      val alertWidth: Dp?,               // charging / unlock / ringer, IV:893-895
  )

  fun deriveIslandTargets(
      scene: IslandScene, settings: IslandSettings, env: IslandEnv,
      measured: MeasuredHeights, card2Expanded: Boolean, bubbleEmerged: Boolean,
  ): IslandTargets
  ```
  `IslandGeometry` is introduced here as a rename of `CardMorph` (`IC:571-583`), with `typealias CardMorph = IslandGeometry` kept for one release. M1.1 builds on it.
- **Steps:**
  1. Rename `CardMorph` to `IslandGeometry` and add the typealias.
  2. Copy the target expressions verbatim. The `LaunchedEffect` delays (`IV:963-972`, `IV:988-997`) stay in the composable and pass their booleans in.
  3. Table-test it.
- **Acceptance criteria:** S1 bounds unchanged. S2 pixel-identical.
- **Tests:** New `DeriveIslandTargetsTest`:
  - default 180 × 36 dp
  - `expandedScale` 0.8 and 1.4
  - a narrow screen, 320 dp, so `expandedCardWidth` is clamped to the screen minus 16
  - each alert width with its `coerceAtLeast` floor
  - minimised (width = height)
  - hidden (0)
- **Depends on:** F1.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Refactor.

### F1.4 The window consumes the scene
- **Goal:** `updateWindowState` and `initOverlay` stop computing occupancy and widths themselves. They call S3's pure window function with the **scene** and the **targets**.
- **Why:** 06 P1-1 (three disagreeing copies) and P0-4 (touchable strip over the foreground app).
- **Files:**
  - `OS:633-655` (`initOverlay` geometry)
  - `OS:961-1003` (occupancy and widths)
  - S3's extracted function, for example `service/window/WindowGeometry.kt`
- **API sketch:**
  ```kotlin
  // S3 extracted: compactWindowSize(settings, flags…) — change its input to the scene
  fun compactWindowSizeDp(scene: IslandScene, targets: IslandTargets, settings: IslandSettings): DpSize
  ```
  The service builds `IslandInputs` from `IslandStateManager`'s current values and gets the same scene the composable gets.
- **Steps:**
  1. Add `IslandStateManager.snapshotInputs(flashlightSolo = false)`. A main-thread getter that reads the flows' `.value`s. It is temporary; F1.5 replaces it.
  2. `updateWindowState` derives the scene and targets, then calls the S3 function. Delete `OS:961-1003`.
  3. `initOverlay` uses the same path. Delete `OS:640-643`.
  4. Update S3's table tests. **Each changed row is a documented, intended behaviour diff:**
     - foreground-app card → not touchable, 1×1
     - `initOverlay` matches `updateWindowState`
     - an ongoing call counts the same in both
- **Acceptance criteria:**
  - S1 bounds unchanged.
  - S3 diffs limited to the rows listed in the PR.
  - On a device: with Spotify playing **in the foreground**, tapping the top centre of the screen reaches Spotify, and the shade pulls down from the centre (06 P0-4).
  - With B2 already merged, B2's temporary foreground check is deleted, not duplicated.
- **Tests:** S3 tables, plus new rows: foreground app, ongoing call only, `hideOnLockScreen` while locked.
- **Depends on:** F1.3, S3. Coordinate with B2: whichever lands second deletes the other's duplicate logic.
- **Effort:** M   **Risk:** medium (window behaviour, OEM).
- **Ships as:** Fix (touch pass-through, window drift).

### F1.5 One `StateFlow<IslandScene>` per process
- **Goal:** One `IslandSceneHolder` combines the state flows with one shared settings `StateFlow` (06 R11) into `scene: StateFlow<IslandScene>`. The window collects it. The overlay composable collects it instead of 22 flows.
- **Why:**
  - It removes the service's hand-written 5-way `combine` and the `shapeKey` list (`OS:765-803`), which already miss `foregroundPackage`.
  - The composable stops recomposing on flows that don't change the scene. Leaf data (1 Hz network speed, recording seconds) is still read at the leaves (M2.7).
- **Files:**
  - New: `island/scene/IslandSceneHolder.kt`
  - Edit: `OS:694-735` (the overlay `setContent`), `OS:765-803`, `IV:393-414`
- **API sketch:**
  ```kotlin
  class IslandSceneHolder(scope: CoroutineScope, settings: StateFlow<IslandSettings>, env: StateFlow<IslandEnv>) {
      /** Main-confined. distinctUntilChanged: only arbitration-relevant changes emit. */
      val scene: StateFlow<IslandScene>
      /** For the window: scene + whether it changed "shape" (replaces shapeKey). */
      val shapeChanges: Flow<IslandScene>
  }
  ```
  `flashlightSolo` moves into `IslandStateManager`, so it is state and not composable-local. The scene can then be fully derived outside Compose.
- **Steps:**
  1. Move `flashlightSolo` into `IslandStateManager`, with the latch semantics of `IV:419-422` and the clear at `IV:1068`.
  2. Build the holder and start it in the service (`OS:466`).
  3. Swap `OS:765-803` to `holder.shapeChanges.collectLatest { … }`.
  4. Make the composable read `holder.scene` and only the leaf flows it renders.
- **Acceptance criteria:**
  - S1 bounds unchanged. S2 pixel-identical.
  - S4: recompositions during an idle 10 s with network speed on drop by at least 9 (the 1 Hz ticks no longer reach the root). Record the number.
  - On a device: rapid notification bursts still resize the window correctly.
- **Tests:**
  - `IslandSceneHolderTest` with `kotlinx-coroutines-test`: a flow change emits once; irrelevant changes such as `fpsInfo` don't emit.
  - `IslandStateBehaviorTest` green through the facade.
- **Depends on:** F1.4, and 06 R11 (one shared settings flow). If B8 didn't do that, do it here as step 0.
- **Effort:** M   **Risk:** medium (ordering of the main-thread collectors).
- **Ships as:** Refactor + perf.

---

## F2: Token consolidation into `IslandTokens` (pixel-identical)

No theming, no presets, no `CompositionLocal` for colors. `IslandTokens` (`island/IslandTokens.kt:16-74`) stays a global `object`, and its existing names keep working. New sections are added beside them.

### F2.1 Literal ratchet test (the "CI grep")
- **Goal:** A unit test that counts literals per file in `island/` and fails when a count goes **up**. Each F2/F4 PR lowers the stored baseline.
- **Why:** PLAN §6 "no new literals". There is no CI workflow, so `./gradlew test` is the gate.
- **Files:**
  - New: `app/src/test/java/com/droidbubble/app/island/IslandLiteralRatchetTest.kt`
  - New: `app/src/test/resources/island-literal-baseline.properties`
- **API sketch:**
  ```kotlin
  class IslandLiteralRatchetTest {
      private val src = File("src/main/java/com/droidbubble/app/island")   // unit tests run from the module dir
      private val rules = mapOf(
          "hex"    to Regex("""Color\(0x"""),
          "sp"     to Regex("""\b\d+(\.\d+)?\.sp\b|\(\s*\d+(\.\d+)?f?\s*\*\s*scale\s*\)[^\n]*\.sp"""),
          "spring" to Regex("""\bspring\s*(<[^>]*>)?\("""),
          "tween"  to Regex("""\btween\s*\("""),
          "haptic" to Regex("""performHapticFeedback|VibrationEffect|Vibrator"""),
      )
      private val exempt = setOf("IslandTokens.kt", "IslandMotion.kt", "IslandType.kt", "IslandHaptics.kt", "IslandTimings.kt")
      @Test fun countsNeverGrow() { /* compare per-file counts with the baseline; print the new numbers on failure */ }
  }
  ```
  `dp` is **not** ratcheted. Layout dp inside presentations is legitimate. F2.4 and F2.5 move the *shell* geometry only.
- **Steps:**
  1. Write the test.
  2. Record today's counts (table above) as the baseline.
  3. Document "lower the baseline in the same PR" in the test's KDoc.
- **Acceptance criteria:** Green on the current tree. Adding one `Color(0xFF000000)` to `IV` fails with a message naming the file and rule.
- **Tests:** The test itself, plus a self-test on a temp dir fixture.
- **Depends on:** —
- **Effort:** S   **Risk:** low.
- **Ships as:** Tooling.

### F2.2 Move color literals into `IslandTokens` (exact values)
- **Goal:** Every `Color(0x…)` in `IV` (79) and `LA` (9) becomes a named token **with the same ARGB value**. Exact duplicates collapse to one token. Near-duplicates get separate tokens for now.
- **Why:** 02 R11. Prerequisite for B5 (high contrast), V5 and F2.3.
- **Files:** `IslandTokens.kt`, `IV`, `LA`.
- **API sketch:**
  ```kotlin
  object IslandTokens {
      // existing sections unchanged …
      // ---- Semantic roles (values copied, not changed) ----
      val RecordingRed = Color(0xFFFF453A)          // == AccentRed
      val RecordingRedSystem = Color(0xFFFF3B30)    // IV:542 outline; merged in F2.3
      val ChargingGreen = AccentGreen               // IV:553
      val ChargingFillDark = Color(0xFF04190B)      // IV:1966
      val FlashlightTint = Color(0xFFD2D6FF)        // IV:1410
      val ControlLight = Color(0xFFE5E5EA)          // == ActionPrimaryContainer (EqualizerVisualizer default, IV:4170)
      // ---- Alpha roles: the .copy(alpha = …) multipliers used for outlines and ambient gradients ----
      const val OutlineAlphaTask = 0.15f            // IV:542-545
      const val OutlineAlphaFlashlight = 0.25f      // IV:549
      const val OutlineAlphaCharging = 0.30f        // IV:553
  }
  ```
- **Steps:**
  1. Grep `Color(0x` in `IV` and `LA`, and list them with their usages.
  2. Map each one to an existing token if the value is identical, otherwise add a token named for its **role**.
  3. Replace the usages.
  4. Lower the F2.1 baseline (hex → 0 for `IV` and `LA`).
- **Acceptance criteria:**
  - S2 pixel-identical. If one pixel changes, a value was mis-copied.
  - The F2.1 hex count is 0 outside `IslandTokens.kt`.
- **Tests:** F2.1. Add `IslandTokensTest` asserting that the documented duplicates are `==`.
- **Depends on:** F2.1, and B5 if it lands first (rebase onto its tokens).
- **Effort:** M   **Risk:** low.
- **Ships as:** Refactor.

### F2.3 Merge near-duplicate colors (D3) — **Intended diff (tiny)**
- **Goal:** One red, one green and one gray family, keeping today's hues. For example, `RecordingRedSystem` (#FF3B30) → `RecordingRed` (#FF453A). The two greens and the zinc/Apple gray mix (#A1A1AA vs #8E8E93) are resolved to one family per role.
- **Why:** PLAN D3 and §1 "three different recording reds and two greens". This is the only F2 step that changes pixels, so it is isolated.
- **Files:** `IslandTokens.kt`.
- **Steps:**
  1. List each merge with its ΔE2000 in the PR.
  2. Only merge pairs with ΔE < 5. Anything larger goes to V5.
  3. Repoint the deprecated tokens with `@Deprecated(replaceWith = …)`, then delete them in the same PR.
- **Acceptance criteria:**
  - S2 diffs only in the states that show the merged colors (recording outline, charging, secondary text), each reviewed.
  - High-contrast states (B5) unchanged.
- **Tests:** `IslandTokensTest` updated.
- **Depends on:** F2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

### F2.4 Shell geometry tokens
- **Goal:** Move the numbers that define the island's *body*, not its card content, into `IslandTokens.Shell`. Keep the values.
- **Why:** M1/M4/F5 compute with them, and the window and the composable must agree.
- **Files:** `IslandTokens.kt`, `IV:893-1030`, `IV:1286-1287`, `IV:1345`, `IV:1761-1771`, `IV:1859`, `IV:2285-2289`, `OS:986-1022`.
- **API sketch:**
  ```kotlin
  object IslandTokens {
      object Shell {
          val ExpandedDrop = 52.dp            // IV:1025: card top = topOffset + 52
          val ExpandedCardWidth = 340.dp      // IV:917, × expandedScale
          val ScreenGutter = 16.dp            // IV:917
          val BubbleGap = 6.dp                // IV:983
          val CardSpacing = 10.dp             // IV:1030
          val Card2Rise = 14.dp               // IV:979 (negative offset)
          val NeckSnapGap = 5.dp              // IV:1765
          val PillZoneExtra = 20.dp           // IV:1287
          val CameraZone = 36.dp              // the Spacer(36.dp) ×5, IV:2022, 2060, 2105, 2173, 2867 (V1 replaces it)
          val WindowSideMargin = 40.dp        // OS:1002 "+80" = 2 × 40
          val WindowBottomMargin = 12.dp      // OS:1022
          const val ChargingExtraWidthDp = 20  // IV:893 / OS:987
          const val UnlockWidthFactor = 0.82f  // IV:894 / OS:993
          const val RingerWidthFactor = 0.95f  // IV:895 / OS:990
      }
      object Gesture {                        // M4 replaces these with dp-based thresholds
          const val LegacySwipeDismissPx = 160f    // IV:1345
          val LegacyPillSwipe = 32.dp              // IV:1859
      }
  }
  ```
- **Steps:**
  1. Replace each occurrence in `IV` and `OS` with the token.
  2. The service's `Int`/`Float` arithmetic uses `.value`.
- **Acceptance criteria:** S1 bounds and S3 tables unchanged. S2 pixel-identical.
- **Tests:** S3, `DeriveIslandTargetsTest`.
- **Depends on:** F1.3 (targets live in one function, so most replacements happen there).
- **Effort:** S   **Risk:** low.
- **Ships as:** Refactor.

### F2.5 Presentation dp hygiene, file by file
- **Goal:** Inside presentations (compact, bubble and expanded views, `IV:2462-4711`), replace repeated paddings, gaps and icon sizes with the existing `Space*` and `Radius*` tokens **only where the value is identical**. Leave one-off layout dp in place.
- **Why:** 02 R11 snap to the 4 dp grid. **Snapping itself is a V-phase change (V2, V4)**, so it is not done here.
- **Files:** `IV:2462-4711`, `LA`, `IC`.
- **Steps:** Run one PR per presentation group (compact, bubble, media, notification, progress, flashlight, widgets), ideally while 06 Phase 5 moves each group to its own file.
- **Acceptance criteria:** S2 pixel-identical per PR.
- **Tests:** existing.
- **Depends on:** F2.2.
- **Effort:** M (spread over several S PRs)   **Risk:** low.
- **Ships as:** Refactor.

---

## F3: Behaviour tokens: `IslandMotion`, `IslandHaptics`, `IslandTimings`

### How the user's curves plug in (read this first)

The owner can already tune three curves plus a speed. F3 must keep their meaning **exactly** until D5 (curve editor v2) changes the editor.

| User setting (`IslandSettings.kt`) | Default | Today drives (verified) | `IslandMotion` token in F3 | After M1 (one body) |
|---|---|---|---|---|
| `animationSpeed` (`:46`, slider 0.5–2.0 at `MainScreen.kt:1947`) | 0.7 | × speed² on every speed-scaled spring (`IV:569-570`); tween ms ÷ speed | `speed` on `IslandMotion`; `Physical.speedScaled` | unchanged |
| `expandDamping` / `expandStiffness` (`:63-64`) | 0.8 / 380 | expand: card morph, `expandProgress`, size, position (`IV:612-614, 628-630, 647-650, 659-663`) | `expand` = `User(expand)` | body geometry compact→expanded |
| *(derived)* collapse | ζ 1, k = expandStiffness × 1.4 (`IV:573`) | every "not expanded" branch (`IV:616-620, 632-636, 652-656, 665-669`) | `collapse` = `Physical(1f, expandK × 1.4)` | body geometry expanded→compact |
| `appearDamping` / `appearStiffness` (`:65-66`) | 0.6 / 280 | card morph and sizes **only while a system alert shows** (`IV:608-610, 624-626, 642-645`) | `appear` = `User(appear)` | alert widen (charging, unlock, BT, ringer) on the body |
| `splitDamping` / `splitStiffness` (`:67-68`) | 0.63 / 320 | pill width when not expanding, which includes the pill's real appear, hide and minimise (`IV:681-684`), and the bubble emerging (`IV:1005-1008`) | `split` = `User(split)` | pill appear/hide/minimise + bubble split |

The editor's labels ("Expand / Appear / Split", `MainScreen.kt:2654`) don't match the right-hand columns (03 P1-3). **Renaming them is D5, not F3.** F3 only routes the same numbers through one object, so D5 can later swap the editor to Duration/Bounce without touching the call sites.

### F3.1 `IslandMotion`: formula, tokens, tiers, impulses and loops
- **Goal:** One immutable object turns a token into an `AnimationSpec`. It applies speed and motion tier, and serves the shell (M), the choreography (M3) and the dynamic-icon engine (I1).
- **Why:** 03 R1 and R8, and PLAN F3. It replaces the inline specs in `IV:569-685` and about 40 others. Epic I needs impulse springs, loop specs and tier awareness from day one.
- **Files:**
  - New: `island/motion/IslandMotion.kt`, `island/motion/MotionTier.kt`
  - Edit: `IV:569-585` (only to build and provide the object; call sites move in F3.2)
- **API sketch:**
  ```kotlin
  package com.droidbubble.app.island.motion

  enum class MotionTier { FULL, EFFICIENT, REDUCED }    // M5.1 resolves it; F3 always passes FULL

  /** A spring described two ways. Perceptual is the design language; Physical is today's stored form. */
  sealed interface MotionToken {
      /** Apple-style perceptual spring: [duration] s at speed 1, [bounce] −1…1. */
      data class Perceptual(val duration: Float, val bounce: Float) : MotionToken
      /** Raw Compose spring, for user curves and legacy values that must stay bit-exact. */
      data class Physical(val dampingRatio: Float, val stiffness: Float, val speedScaled: Boolean = true) : MotionToken
  }

  /** duration/bounce → Compose: ζ = 1 − b (b ≥ 0), ζ = 1/(1 + b) (b < 0); k = (2π/d)² · speed². */
  fun MotionToken.Perceptual.toPhysical(): MotionToken.Physical {
      val zeta = if (bounce >= 0f) 1f - bounce else 1f / (1f + bounce)
      val k = (2f * PI.toFloat() / duration).pow(2)
      return MotionToken.Physical(zeta.coerceIn(0.05f, 4f), k)
  }
  /** Inverse, used by D5's migration: d = 2π/√k, b = 1 − ζ. */
  fun MotionToken.Physical.toPerceptual() = MotionToken.Perceptual(2f * PI.toFloat() / sqrt(stiffness), 1f - dampingRatio)

  /** A velocity kick on a spring that rests at its target: bounce, wiggle, alert bump. */
  @Immutable data class Impulse(val spec: SpringSpec<Float>, val initialVelocity: Float)

  /** A repeating ambient effect. Null from [IslandMotion.loop] means "draw the static frame". */
  @Immutable data class LoopSpec(val periodMillis: Int, val frameIntervalMillis: Int /* 0 = vsync */, val easing: Easing)

  enum class LoopToken(val periodMillis: Int) { PULSE(1_200), BREATHE(2_400), ROTATE(1_000), EQUALIZER(0) }

  @Immutable
  class IslandMotion(
      val speed: Float,                    // settings.animationSpeed.coerceIn(0.5f, 2f): same clamp as IV:569
      val tier: MotionTier,
      val expand: MotionToken.Physical,    // user curve
      val appear: MotionToken.Physical,    // user curve
      val split: MotionToken.Physical,     // user curve
      val profile: Profile = Profile.Legacy,
  ) {
      val collapse  get() = MotionToken.Physical(1f, expand.stiffness * 1.4f)
      fun <T> spring(token: MotionToken, visibilityThreshold: T? = null): SpringSpec<T>
      /** ms ÷ speed; REDUCED → [REDUCED_FADE_MS] crossfade. */
      fun <T> tween(millis: Int, easing: Easing = FastOutSlowInEasing, delayMillis: Int = 0): TweenSpec<T>
      /** For I1 bounce/wiggle and D2 alert bump. Null in REDUCED: no overshoot effects. */
      fun impulse(token: MotionToken, velocity: Float): Impulse?
      /** For I1 pulse/breathe/rotate and the equalizer. REDUCED → null, EFFICIENT → frameInterval ≥ 33 ms. */
      fun loop(token: LoopToken): LoopSpec?
      val allowsStretch get() = tier == MotionTier.FULL
      val allowsBlur get() = tier == MotionTier.FULL      // steppedBlurEffect keeps its own Battery Saver check
      val allowsStaggerDelays get() = tier != MotionTier.REDUCED

      enum class Profile { Legacy /* F3 → M5.2 */, Droid /* M5.3: 03 §6 table */ }

      companion object {
          const val REDUCED_FADE_MS = 150
          val REDUCED_GEOMETRY = MotionToken.Perceptual(0.30f, 0f)   // 03 R8: ζ 1 at 0.3 s
          fun from(settings: IslandSettings, tier: MotionTier = MotionTier.FULL) = IslandMotion(
              speed = settings.animationSpeed.coerceIn(0.5f, 2f), tier = tier,
              expand = MotionToken.Physical(settings.expandDamping, settings.expandStiffness),
              appear = MotionToken.Physical(settings.appearDamping, settings.appearStiffness),
              split  = MotionToken.Physical(settings.splitDamping, settings.splitStiffness),
          )
      }
  }

  val LocalIslandMotion = staticCompositionLocalOf<IslandMotion> { error("IslandMotion not provided") }
  ```
  `spring(token)` computes stiffness as `k × speed²` when `speedScaled`, otherwise `k`. In REDUCED it returns `REDUCED_GEOMETRY` for geometry tokens and `spring(1f, StiffnessMedium)` otherwise. It is `staticCompositionLocalOf` because it changes only when settings change, and that invalidating the subtree is acceptable then.

- **Token values: the `Legacy` profile.** These are bit-exact today's values, so F3 is pixel-identical:

  | Token | Value | Source |
  |---|---|---|
  | `expand`, `appear`, `split`, `collapse` | user / derived | see the table above |
  | `pillShrinkOnExpand` | Physical(0.82, 380) | `IV:676-679` |
  | `bubbleRetract` | Physical(0.85, 380) | `IV:1010-1013` |
  | `cardDrag` | Physical(0.76, 420) | `IV:1091, 1096` |
  | `press` | Physical(0.6, 700, **speedScaled = false**) | `IV:595, 602` |
  | `swapSquashIn` / `swapSquashOut` | tween 110 FastOutSlowIn / Physical(0.5, 380) | `IV:586-587` |
  | `parenthesisShoot` | Physical(0.65, 260), v₀ = 240 · speed | `IV:1170-1176` |
  | `contentFade` | tween 130 ms ÷ speed | `IV:695, 701, 2276` |
  | `compactSwapIn` | fade tween 200 delay 50 + scale Physical(0.75, 400, false) | `IV:2229-2230` |
  | `compactSwapOut` | tween 120 | `IV:2231` |
  | `idleTap` | tweens 70 / 90 + spring MediumBouncy / StiffnessLow | `IV:1899-1931` |
  | `chargingFill` | tween 750 ÷ speed, delay 60 ÷ speed / 150 ÷ speed | `IV:880-888` |
  | `roll` | tween 380, CubicBezier(0.2, 0, 0, 1) | `LA:375` (`ROLL_EASING`), `LA:399` |

- **Token values: the `Droid` profile** (03 §6, at speed 1.0, applied in M5.3, never to the three user curves):

  | Token | d / b | ζ / k |
  |---|---|---|
  | `collapse` (default only when the user hasn't customised expand) | 0.38 / 0 | 1.00 / 273 |
  | `dismiss` | 0.32 / 0 | 1.00 / 386 |
  | `merge` | 0.35 / 0.05 | 0.95 / 322 |
  | `alertBump` | 0.30 / 0.45 | 0.55 / 439 |
  | `press` | 0.20 / 0.30 | 0.70 / 987 |
  | `interactive` | 0.18 / 0.10 | 0.90 / 1218 |
  | `effects` | ≈0.16 / 0 | 1.00 / 1600 |
  | `iconBounce` (I1) | 0.30 / 0.45 | 0.55 / 439 |
  | `iconWiggle` (I1) | 0.25 / 0.35 | 0.65 / 632 |

  The tokens that exist only in `Droid` have a `Legacy` stand-in, so call sites written before M5.3 still compile and stay pixel-identical:

  | Droid-only token | Legacy stand-in |
  |---|---|
  | `dismiss` | `collapse` |
  | `merge` | `bubbleRetract` |
  | `interactive` | `cardDrag` |
  | `alertBump` | `swapSquashOut` |
  | `effects` | `contentFade` |
  | `iconBounce`, `iconWiggle` | their `Droid` values, because nothing uses them before epic I |

- **Steps:**
  1. Write the types and the conversion functions.
  2. Provide `LocalIslandMotion` from `IslandOverlayRoot` via `remember(settings) { IslandMotion.from(settings) }`.
  3. Leave every call site alone (F3.2 moves them).
- **Acceptance criteria:**
  - `Perceptual(0.5f, 0.3f).toPhysical()` = ζ 0.70, k 157.9 (Apple's own example, 03 §1).
  - `Physical(0.8, 380).toPerceptual()` round-trips within 1e-4.
  - S2 untouched.
- **Tests:** New `island/motion/IslandMotionTest.kt`:
  - every row of 03 §1's table
  - speed² scaling at 0.5 / 0.7 / 2.0
  - `press` not scaled
  - REDUCED returns `REDUCED_GEOMETRY`, `impulse == null`, `loop == null`
  - EFFICIENT `loop(EQUALIZER).frameIntervalMillis >= 33`
- **Depends on:** —
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.

### F3.2 Route every inline spec through `IslandMotion`
- **Goal:** All `spring(`/`tween(` calls in `IV`, `IC` and `LA` (20 + 23, 1 + 2, 1 + 3) go through `LocalIslandMotion.current`, using `Profile.Legacy` tokens. Values are unchanged.
- **Why:** This makes M1, M3 and M5 one-line changes, and lets F2.1 ratchet `spring` and `tween` to 0.
- **Files:** `IV:569-1181` (shell specs), `IV:2226-2276`, `IV:2654-4711` (presentations), `IC`, `LA`.
- **Steps:**
  1. Do the shell specs `IV:569-685` first, mapped 1:1 to `motion.spring(motion.expand)` and so on.
  2. Then the presentations.
  3. Keyframes (bell and vibrate shakes, `IV:832` and `IV:856`) stay keyframes. Wrap them in `motion.keyframes { }` so REDUCED can skip them in M5.2. WWDC23 rule: keyframes only for one-shot effects.
  4. Lower the F2.1 `spring` and `tween` baselines to 0 for `IV`, `IC` and `LA`.
- **Acceptance criteria:**
  - S2 pixel-identical. S4 no worse.
  - S2 also captures mid-animation frames at 100 ms and 250 ms of an expand and a collapse, using the Robolectric paused clock. Those must match too, because they prove the springs are bit-exact.
- **Tests:** F2.1. `IslandOverlayCrashTest` green.
- **Depends on:** F3.1, F2.1.
- **Effort:** M   **Risk:** low (mechanical; mid-frame screenshots catch mistakes).
- **Ships as:** Refactor.

### F3.3 `IslandHaptics`: one SDK-guarded map that respects the toggle
- **Goal:** Every island haptic goes through `IslandHaptics.perform(event)`. It is a no-op when `settings.hapticsEnabled` is false. In this task the events map 1:1 to **today's** constants.
- **Why:** 05 R5:
  - today 23 call sites in `IV`
  - the flashlight slider and off button (`IV:3081`, `3096`, `3265`) ignore the toggle
  - the ringer uses a raw `createWaveform` (`IV:801-828`)
  - `CONTEXT_CLICK` stands for five meanings (`IV:1197-1214`)
- **Files:**
  - New: `island/IslandHaptics.kt`
  - Edit: `IV:801-828`, `1183-1214`, `1889-1894`, `3081`, `3096`, `3265`, and the callers of `triggerHaptic` / `triggerCollapseHaptic` / `triggerLongPressHaptic`
- **API sketch:**
  ```kotlin
  enum class IslandHapticEvent {
      TAP_EXPAND, COLLAPSE, LONG_PRESS, IDLE_TAP, DISMISS, MINIMISE, RESTORE, HIDE,
      THRESHOLD_CROSSED, GESTURE_COMMIT, TOGGLE_ON, TOGGLE_OFF, SEGMENT_TICK, SEGMENT_FREQUENT_TICK,
      CONFIRM, REJECT, NECK_SNAP, MERGE, SYSTEM_ALERT_CHARGING, SYSTEM_ALERT_RINGER
  }

  class IslandHaptics(private val view: View, private val enabled: () -> Boolean, private val map: HapticMap = HapticMap.Legacy) {
      fun perform(event: IslandHapticEvent) {
          if (!enabled()) return
          val constant = map.constantFor(event, Build.VERSION.SDK_INT) ?: return   // null = no haptic
          view.performHapticFeedback(constant)    // never FLAG_IGNORE_GLOBAL_SETTING: the system touch setting wins
      }
  }

  /** event → HapticFeedbackConstants, with an SDK fallback chain. No Vibrator, no waveforms. */
  fun interface HapticMap {
      fun constantFor(event: IslandHapticEvent, sdk: Int): Int?
      companion object {
          val Legacy: HapticMap   // TAP_EXPAND/DISMISS/MINIMISE/RESTORE/HIDE → CONTEXT_CLICK; COLLAPSE → CLOCK_TICK;
                                  // LONG_PRESS and SYSTEM_ALERT_CHARGING → LONG_PRESS; IDLE_TAP → CONFIRM (30+) / KEYBOARD_TAP;
                                  // flashlight SEGMENT_TICK → CLOCK_TICK; flashlight off → CONFIRM;
                                  // SYSTEM_ALERT_RINGER → CLOCK_TICK (the waveform's fallback path today)
          val Droid: HapticMap    // F3.4
      }
  }
  val LocalIslandHaptics = staticCompositionLocalOf<IslandHaptics> { error("not provided") }
  ```
- **Steps:**
  1. Write the class and the `Legacy` map.
  2. Replace every call site.
  3. The flashlight sites now respect the toggle. **This is the one intended behaviour fix.**
  4. The ringer waveform becomes the `Legacy` fallback constant. The raw `Vibrator` code goes away (05 R5: no raw waveforms).
  5. Ratchet `haptic` to 0 in `IV`.
- **Acceptance criteria:**
  - S2 pixel-identical.
  - On a device, with the in-app toggle off, *nothing* vibrates, including the flashlight slider.
  - With it on, every gesture feels as before, except the ringer switch, which is now a tick instead of a buzz pattern.
- **Tests:** `IslandHapticsTest` (Robolectric, `ShadowView` last haptic constant): toggle off → no call; each event's mapped constant at SDK 29, 30 and 34.
- **Depends on:** F2.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Fix + plumbing.

### F3.4 Haptic meanings retune (05 §6) — **Intended diff (haptics)**
- **Goal:** Switch to `HapticMap.Droid`, following 05 §6's Android haptic meanings.
- **Why:** 05 R5 and §5: use Android's haptic meanings, and stay quiet for passive events.
- **Files:** `island/IslandHaptics.kt`.
- **API sketch:** the `Droid` map:

  | Event | API 34+ | API 30–33 | API 29 |
  |---|---|---|---|
  | TAP_EXPAND, RESTORE | VIRTUAL_KEY | VIRTUAL_KEY | VIRTUAL_KEY |
  | COLLAPSE | CLOCK_TICK | CLOCK_TICK | CLOCK_TICK |
  | LONG_PRESS | LONG_PRESS | LONG_PRESS | LONG_PRESS |
  | THRESHOLD_CROSSED | GESTURE_THRESHOLD_ACTIVATE | CLOCK_TICK | CLOCK_TICK |
  | GESTURE_COMMIT (dismiss, minimise, shade) | GESTURE_END | GESTURE_END | CONTEXT_CLICK |
  | TOGGLE_ON / TOGGLE_OFF | TOGGLE_ON / TOGGLE_OFF | CONTEXT_CLICK | CONTEXT_CLICK |
  | SEGMENT_TICK / SEGMENT_FREQUENT_TICK | SEGMENT_TICK / SEGMENT_FREQUENT_TICK | CLOCK_TICK / null | CLOCK_TICK / null |
  | CONFIRM / REJECT | CONFIRM / REJECT | CONFIRM / REJECT | KEYBOARD_TAP / LONG_PRESS |
  | NECK_SNAP, MERGE (D1/D2) | SEGMENT_FREQUENT_TICK | CLOCK_TICK | CLOCK_TICK |
  | SYSTEM_ALERT_CHARGING, SYSTEM_ALERT_RINGER | **null** | null | null |
  | IDLE_TAP | CONFIRM | CONFIRM | KEYBOARD_TAP |

- **Steps:**
  1. Flip the default map.
  2. Make `Legacy` reachable by a hidden debug setting for A/B testing on devices.
- **Acceptance criteria:** Device check on Pixel and Samsung, API 34+ and one API 30–33 device: each row feels distinct, and charging and ringer are silent.
- **Tests:** `IslandHapticsTest` table for `Droid`.
- **Depends on:** F3.3.
- **Effort:** S   **Risk:** low (taste; reversible).
- **Ships as:** Polish.

### F3.5 `IslandTimings`: one place for timeouts, scaled for accessibility
- **Goal:** Every island timeout comes from one object. Each is passed through `AccessibilityManager.getRecommendedTimeoutMillis` (API 29, which is minSdk). Values are unchanged.
- **Why:** 05 R6. Today the timeouts are spread over `IV:1223` and `SM:450, 468, 480, 492, 636, 693`, and none respect the user's "Time to take action" setting.
- **Files:**
  - New: `island/IslandTimings.kt`
  - Edit: `IV:1216-1226`, `SM:440-500`, `SM:630-640`, `SM:685-695`
- **API sketch:**
  ```kotlin
  class IslandTimings(private val recommend: (originalMs: Int, flags: Int) -> Int) {
      // Today's values (ms). F3.6 retunes them.
      val autoCollapse      get() = scaled(3_000, CONTROLS or TEXT)     // IV:1223
      val notificationAuto  get() = scaled(3_000, TEXT)                 // SM:636
      val popIn             get() = scaled(4_500, TEXT)                 // SM:693
      val unlock            get() = scaled(2_200, ICONS)                // SM:450
      val connection        get() = scaled(3_000, ICONS or TEXT)        // SM:468
      val ringer            get() = scaled(2_500, ICONS)                // SM:480
      val charging          get() = scaled(4_000, ICONS or TEXT)        // SM:492
      private fun scaled(ms: Int, flags: Int) = recommend(ms, flags).toLong()
      companion object {
          private const val ICONS = AccessibilityManager.FLAG_CONTENT_ICONS
          private const val TEXT = AccessibilityManager.FLAG_CONTENT_TEXT
          private const val CONTROLS = AccessibilityManager.FLAG_CONTENT_CONTROLS
          fun from(context: Context): IslandTimings {
              val am = context.getSystemService(AccessibilityManager::class.java)
              return IslandTimings { ms, f -> am?.getRecommendedTimeoutMillis(ms, f) ?: ms }
          }
          val Unscaled = IslandTimings { ms, _ -> ms }   // tests
      }
  }
  ```
  `IslandStateManager` is a global `object`, so it gets `var timings: IslandTimings = IslandTimings.Unscaled`, set by the service in `onCreate`. That follows the existing pattern of `dismissMediaOnPause` (`OS:478`).
- **Steps:**
  1. Write the class.
  2. Replace the 7 literals.
  3. The service sets `IslandStateManager.timings = IslandTimings.from(this)`.
- **Acceptance criteria:**
  - With the accessibility timeout at the system default, behaviour is identical (`getRecommendedTimeoutMillis` returns the original).
  - On a device with "Time to take action" set to 1 minute, an expanded card stays open about 1 minute.
- **Tests:**
  - `IslandTimingsTest` with a fake `recommend`
  - `IslandStateBehaviorTest` with `Unscaled`, unchanged
- **Depends on:** —
- **Effort:** S   **Risk:** low.
- **Ships as:** Accessibility fix.

### F3.6 Timing retune (05 R6) — **Intended diff (behaviour)**
- **Goal:**
  - A user-expanded card stays 6 s.
  - Expanded media stays 8 s.
  - Ringing calls and alarms have no timeout (already true for calls, `IV:1220-1222`).
  - Transient system alerts stay at 2–3 s. Pop-in keeps 4.5 s.
- **Why:** 05 R6: "media and a 10-line notification get the same 3 s".
- **Files:** `island/IslandTimings.kt`, `IV:1216-1226` (which needs the expanded priority).
- **API sketch:** `fun autoCollapseFor(priority: IslandPriority, isAlarm: Boolean): Long?`, where `null` means no timeout.
- **Acceptance criteria:**
  - Device: media stays 8 s and a notification 6 s.
  - Touch still resets the timer. After B8 it observes touches without recomposing the root.
- **Tests:** `IslandTimingsTest`.
- **Depends on:** F3.5, B8.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

---

## F4: `IslandType` roles on the system font

### F4.1 `IslandType` with roles that reproduce today exactly
- **Goal:** One `IslandType` object exposes named roles. **Step 1 is pixel-identical:** each role's size, weight and `lineHeight` equal what the text renders with today. The `lineHeight` is the inherited 24 sp, written out explicitly. All 33 `N.sp` literals in `IV`, the four metric classes (`IC:255-293`, `LA:346-351`) and the media inline sizes move to roles.
- **Why:** 01 R1. It separates "where the numbers live" from "what the numbers are", so F4.2 and F4.3 become one-file diffs.
- **Files:**
  - New: `island/IslandType.kt`
  - Edit: `IV`, `IC:255-293`, `LA:328-351`, `IslandTokens.kt:68-73` (the `Type*` tokens become `@Deprecated` aliases)
- **API sketch:**
  ```kotlin
  @Immutable
  class IslandType(val expandedScale: Float, val fontScale: Float, val mode: Mode = Mode.Legacy) {
      enum class Mode { Legacy /* F4.1: inherits 24 sp boxes */, Trimmed /* F4.2 */ }

      // Compact and bubble: never multiplied by expandedScale (01 R1)
      val compactValue: TextStyle; val compactLabel: TextStyle; val compactUnit: TextStyle; val bubbleValue: TextStyle
      // Expanded: size and line height scale together
      val hero: TextStyle; val heroSmall: TextStyle; val statValue: TextStyle
      val title: TextStyle; val body: TextStyle; val secondary: TextStyle
      val eyebrow: TextStyle; val caption: TextStyle; val button: TextStyle

      /** Base for any Text that names no role: replaces bodyLarge at OS:726. */
      val base: TextStyle
  }
  val LocalIslandType = staticCompositionLocalOf<IslandType> { error("IslandType not provided") }

  // Every role starts from the same base, so tnum and tracking can't escape again (01 finding 2):
  private val Base = TextStyle(fontFeatureSettings = "tnum", letterSpacing = 0.sp, platformStyle = PlatformTextStyle(includeFontPadding = false))
  ```
  In `Legacy`, each role's `lineHeight = 24.sp` (bodyLarge's line height, which `OS:726` copies today), with `lineHeightStyle` left at bodyLarge's. That gives an identical layout.
- **Steps:**
  1. Write the roles. In `Legacy`, `title` keeps today's **per-card** sizes through role parameters: `title(card = NOTIFICATION | PROGRESS | MEDIA | LIVE)`, because the sizes differ today (17 / 17.6 / 18.7 sp, 01 finding 6). F4.3 merges them.
  2. Provide `LocalIslandType` next to `LocalIslandMotion`.
  3. Replace the literals and metric fields with roles. `measureNotificationCardHeight` and `measureProgressCardHeight` (`IC:313`, `IC:342`) use the same roles, so the measured heights still match the views.
  4. Lower the F2.1 `sp` baseline to 0.
- **Acceptance criteria:** S2 pixel-identical. S1 bounds unchanged, including measured notification card heights.
- **Tests:** `IslandTypeTest`: `Legacy` roles equal the old metric-class values for `expandedScale` 0.8, 1.0, 1.1 and 1.4. F2.1.
- **Depends on:** F2.1, B9 (the `tnum` escapes at `IV:4579, 4585, 4610, 4616`, and the 8 sp bubble numeral).
- **Effort:** M   **Risk:** low.
- **Ships as:** Refactor.

### F4.2 Explicit line heights and `LineHeightStyle.Trim` — **Intended diff**
- **Goal:** Switch to `Mode.Trimmed`. Every role gets the line height from 01 §6 and `LineHeightStyle(Alignment.Center, Trim.Both)`. The base style at `OS:726` becomes `LocalTextStyle provides islandType.base` (body 15/20), which ends the 24 sp inheritance.
- **Why:** 01 finding 1 (P0):
  - the spacing tokens are a fiction
  - small labels float in tall boxes
  - a possible hero clip during the roll (`LA:470-475` clips to the line box)
- **Files:** `island/IslandType.kt`, `OS:726`, and any fixed-height containers that relied on the slack (re-check `cardHeightFor`, `IC:211-230`).
- **API sketch:** the `Trimmed` line heights from 01 §6 at `expandedScale` 1.0:

  | Role | Line height |
  |---|---|
  | compactValue | 16 |
  | compactLabel | 16 |
  | compactUnit | 16 |
  | bubbleValue | 14 |
  | hero | 44, plus `LineHeightStyle.Mode.Minimum` if the Compose BOM 2026.03.01 has it; otherwise `Trim.None` for heroes |
  | heroSmall | 32 |
  | statValue | 24 |
  | title | 20 |
  | body | 20 |
  | secondary | 16 |
  | eyebrow | 16 |
  | caption | 14 |
  | button | 18 |

  **Sizes stay today's.** Only the line boxes change.
- **Steps:**
  1. Flip the mode.
  2. Fixed-height cards gain slack. Do **not** shrink them here (that is V4). Re-centre content only where it is visibly off-centre.
  3. Check `RollingNumberText`'s travel (`0.72 × box height`, 01 R1) with the new boxes. Heroes must not clip during a roll.
- **Acceptance criteria:**
  - S2 diffs reviewed state by state: text sits 2–5 dp tighter, nothing clips, and measured notification and progress cards get shorter.
  - S1: card bounds change **only** for the measured cards (notification, progress), as listed.
  - Device check at font scale 1.0 and 1.5 on an OEM font (One UI or MIUI).
- **Tests:**
  - S1 updated for the listed rows
  - `IslandTypeTest` for `Trimmed`
  - Robolectric render of a hero mid-roll: no glyph pixels clipped, by comparing the bitmap's top rows with the settled frame
- **Depends on:** F4.1, S2.
- **Effort:** M   **Risk:** medium (touches every card; reviewed via S2).
- **Ships as:** Polish (typography).

### F4.3 Harmonise the scales — **Intended diff (small)**
- **Goal:**
  - One `title` for every card. The default is 17.6 sp (16 × 1.1), within 0.6 sp of the notification and progress cards today.
  - `expandedScale` applied evenly to all expanded roles, including FPS, weather, network, idle and the media time labels, which ignore it today (01 finding 6).
  - Compact roles never scaled.
- **Why:** The title jumps 10 % between cards today. PLAN F4: "harmonize them rather than restyle".
- **Files:** `island/IslandType.kt`, and the views at `IV:3386`, `IV:4119`, `IV:4319-4366`, `IV:4419-4507`, `IV:4579-4640` named in 01 finding 6.
- **Steps:**
  1. Remove the `card` parameter from `title`.
  2. Route the unscaled views through scaled roles.
  3. List every size that moves by more than 0.5 sp in the PR.
- **Acceptance criteria:** S2 diffs limited to title size on media and LiveLayout cards, and to the five previously unscaled views. No layout overflow at `expandedScale` 1.4 and font scale 1.5.
- **Tests:** `IslandTypeTest`.
- **Depends on:** F4.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

### F4.4 `NumericSlot`
- **Goal:** A composable that reserves the width of a template string with each digit mapped to "0". It generalises the media time labels (`IV:3383-3396`). It is used first only to replace that code, which is pixel-identical. V3 and D3 adopt it for %, battery, FPS and speed.
- **Why:** 01 R2: no width jitter.
- **Files:** New `island/NumericSlot.kt`; `IV:3383-3396`.
- **API sketch:**
  ```kotlin
  @Composable
  fun NumericSlot(
      text: String,
      style: TextStyle,
      template: String,                   // e.g. "100%", "00:00", "000 KB/s"
      modifier: Modifier = Modifier,
      align: Alignment.Horizontal = Alignment.End,
      content: @Composable (String) -> Unit = { Text(it, style = style, maxLines = 1) }, // or RollingNumberText
  )
  internal fun slotTemplate(template: String) = template.map { if (it.isDigit()) '0' else it }.joinToString("")
  ```
- **Acceptance criteria:** S2 pixel-identical. Media time labels keep their width.
- **Tests:** `NumericSlotTest`: the template mapping, and a Robolectric width test that "1:05" and "10:59" share a width.
- **Depends on:** F4.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.

---

## F5: Window controller

### F5.1 `WindowSpec` and `OverlayWindowController`: one `updateViewLayout` per change
- **Goal:** The service describes the window it wants as a `WindowSpec` (size, touchable, focusable, refresh rate). One controller diffs it against the current params and sends **one** `updateViewLayout`. This replaces the separate refresh-boost call and the resize call (`OS:800-802`, `OS:863-884`, `OS:887-914`).
- **Why:** 06 P1-2 and 03 P1-1 (two relayouts at expand start). The controller is also the place where F5.2, F5.3 and F5.4 plug in.
- **Files:**
  - New: `service/window/WindowSpec.kt`, `service/window/OverlayWindowController.kt`
  - Edit: `OS:595-603`, `OS:745-803`, `OS:806-819`, `OS:841-1088`
- **API sketch:**
  ```kotlin
  @Immutable
  data class WindowSpec(
      val width: Int,                    // px or MATCH_PARENT
      val height: Int,
      val touchable: Boolean,
      val focusable: Boolean,            // reply (OS:806-819) and, later, back (M4.5)
      val preferredRefreshRate: Float,   // 0 = system default
      val gravity: Int = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
      val phase: Phase,
  ) {
      enum class Phase { HIDDEN, COMPACT, GROWING, EXPANDED, SHRINKING }
  }

  /** Pure: S3's function, now returning a WindowSpec. */
  fun windowSpecFor(scene: IslandScene, targets: IslandTargets, settings: IslandSettings, density: Float,
                    keyboardFocused: Boolean, boosting: Boolean, maxRefreshRate: Float): WindowSpec

  class OverlayWindowController(private val wm: WindowManager, private val view: View, private val params: WindowManager.LayoutParams) {
      /** Applies only the fields that differ; one updateViewLayout at most. */
      fun apply(spec: WindowSpec)
      /** API 34+: params.setCanPlayMoveAnimation(false); below: today's reflection (OS:597-603). */
      private fun disableMoveAnimation()
  }
  ```
- **Steps:**
  1. Build the controller with `apply()`.
  2. Fold the boost into the spec: `boosting` is true for `1200 / speed` ms after a shape change (`OS:876`).
  3. Fold the keyboard focus job (`OS:806-819`) into the spec.
  4. Leave the collapse logic (`OS:1042-1086`) in place. F5.3 replaces it.
  5. Delete the unused `collapseDelayMs` (`OS:1049-1050`) and the dead `touchManager` field (`OS:595`).
- **Acceptance criteria:**
  - S3 tables unchanged (same sizes and flags).
  - A `ShadowWindowManager` counter shows **1** `updateViewLayout` per expand start, down from 2.
  - Device: expand and collapse look the same. A Perfetto trace shows one relayout at expand start.
- **Tests:** `OverlayWindowControllerTest` (Robolectric): an unchanged spec makes no call; size + refresh in one call; a focus change alone makes one call.
- **Depends on:** F1.4, S3.
- **Effort:** M   **Risk:** medium (window).
- **Ships as:** Perf.

### F5.2 Grow before animating
- **Goal:** On expand (and when appearing from 1×1), the window grows first. The geometry animation starts only once the root has actually been laid out at the new size, so the first frames aren't clipped.
- **Why:** 06 P1-2 and 03 P1-1. A WM relayout takes about 28–80 ms; at 120 Hz that clips 3–9 frames.
- **Files:** `IV:1234-1247` (root `Box`), `IV:1036-1041` (morph start), and `IslandStateManager` (a new `windowEnvelope` state).
- **API sketch:**
  ```kotlin
  // Composable side: the root is fillMaxSize, so its size IS the window size.
  var rootSize by remember { mutableStateOf(IntSize.Zero) }
  Modifier.onSizeChanged { rootSize = it }

  /** The animation target switches to "expanded" only when the window can show it, or after 100 ms. */
  @Composable
  fun rememberWindowReady(wantExpanded: Boolean, rootSize: IntSize, neededPx: IntSize): Boolean
  ```
  The shell (M1) animates toward `targets.card1` only when `windowReady`. Until then it holds the compact target, so it starts from the pill: there is no jump, only at most 1–2 frames of latency.
- **Steps:**
  1. Compute `neededPx` from `targets.card1` (top + height, card width).
  2. Gate the morph's target on readiness. Fall back after 100 ms so an OEM that never resizes doesn't block the expand.
  3. Do the same for appear-from-idle (1×1 → compact).
- **Acceptance criteria:**
  - Device at 120 Hz: a frame capture (`adb shell screenrecord` at 120 fps, or a Perfetto frame timeline) shows no clipped card on the first frames of expand. The latency to the first moving frame is ≤ 2 frames.
  - S2 pixel-identical, because Robolectric resizes synchronously and the gate is immediately true.
  - S4 no worse.
- **Tests:** `WindowReadyTest`: the gate's state machine (ready, not ready, timeout) with a test clock.
- **Depends on:** F5.1. Ideally after M1.1 (the gate is easier on an `Animatable`), but it works with `animateValueAsState` by switching the target.
- **Effort:** S   **Risk:** medium (OEM resize timing; covered by the timeout).
- **Ships as:** Fix (clipped first frames).

### F5.3 Shrink on settle, everywhere (including hide)
- **Goal:** Every shrink waits for the shape to report it has settled. Today only the collapse does (`OS:1052-1065`). This covers:
  - collapse
  - hide
  - minimise
  - becoming idle
  - system alert ending

  It replaces the speed-scaled guess delay (`OS:1066-1083`) and fixes **hide**. Hide currently goes straight to 1×1 through `applyCompactBounds()` (`OS:978`, `OS:1028-1038`), so the shrink animation is never visible.
- **Why:** 03 P1-1 and 06 R6 "shrink on settle".
- **Files:** `SM:143-148` (generalise `collapseSettled`), `IV:1061-1070`, `OS:1042-1086`.
- **API sketch:**
  ```kotlin
  // IslandStateManager
  /** The geometry the island has settled into; published by the composable when every shape animation is at rest. */
  val settledShape: StateFlow<SettledShape>
  @Immutable data class SettledShape(val mode: IslandMode, val compactWidthDp: Float, val expanded: Boolean, val generation: Long)

  // Controller
  suspend fun shrinkWhenSettled(target: WindowSpec, settled: StateFlow<SettledShape>, expectMode: IslandMode, timeoutMs: Long = 1_500)
  ```
  `collapseSettled` stays as a derived alias (`settledShape.map { !it.expanded }`) for one release, for `IslandStateBehaviorTest`.
- **Steps:**
  1. The composable publishes `SettledShape` from a `snapshotFlow` over the animatables' `isRunning` plus the scene mode. This extends `IV:1061-1070`.
  2. The controller shrinks only when `settled.mode == expectMode`.
  3. Delete `OS:1066-1083`.
- **Acceptance criteria:**
  - Device: swipe-to-hide (or, after B6, the hide action) shows the full shrink into the camera.
  - The last notification ending shows the full shrink.
  - Expanded → collapse unchanged.
  - S3 tables unchanged in final sizes.
  - The window never stays large for more than 1.5 s after settling (timeout).
- **Tests:** `OverlayWindowControllerTest`: hide waits for settle; the timeout path; a new state arriving mid-shrink re-applies the grow.
- **Depends on:** F5.1.
- **Effort:** S   **Risk:** medium.
- **Ships as:** Fix.

### F5.4 Envelope window trial behind a flag (D7)
- **Goal:** An opt-in debug flag replaces MATCH_PARENT on expand with an **envelope** window: the expanded cards' union plus a 24 dp margin, plus `FLAG_WATCH_OUTSIDE_TOUCH`. A tap outside collapses through `ACTION_OUTSIDE`. Default stays MATCH_PARENT (D7).
- **Why:** 06 R6. Less fill and compositing, and the app below stays touchable while a card is open. D7 says trial only.
- **Files:** `service/window/*`, `IV:1250-1262` (the backdrop is not needed in envelope mode), and a hidden setting in `IslandSettings` and `SettingsRepository`.
- **API sketch:**
  ```kotlin
  enum class ExpandWindowStrategy { MATCH_PARENT, ENVELOPE }   // settings.expandWindowStrategy, debug-only UI
  fun envelopeFor(targets: IslandTargets, scene: IslandScene, density: Float): Rect
  ```
  Envelope mode also needs `onTouchEvent(ACTION_OUTSIDE)` on a wrapper `FrameLayout` around the `ComposeView` that calls `IslandStateManager.collapse()`.
- **Steps:**
  1. Add the strategy and envelope math.
  2. Add the wrapper view.
  3. Disable the backdrop when ENVELOPE.
  4. Run the device matrix.
- **Acceptance criteria:** On Pixel, Samsung and Xiaomi:
  - tapping outside collapses
  - the app below is *not* touched by that tap (ACTION_OUTSIDE delivers to the overlay only)
  - the card's shadow and stretch are not clipped by the envelope
  - With the flag off, there is no change.
- **Tests:** envelope math table tests.
- **Depends on:** F5.3. Better after M1, so the envelope matches the one body.
- **Effort:** M   **Risk:** high (OEM behaviour; flag-gated).
- **Ships as:** Experiment.

---

## F6: Build and release

### F6.1 Decide and apply D1 (`minSdk` 29 → 33)
- **Goal:** Record the owner's D1 answer. If it is yes:
  - set `minSdk = 33` (`app/build.gradle.kts:16`)
  - delete the legacy branches: `islandShape`'s `RoundedCornerShape` fallback (`IslandShapes.kt:111-112`), `SDK_INT < S` in `steppedBlurEffect` (`IC:606`), the `SDK_INT >= TIRAMISU` receiver branches (`OS:514-518`, `OS:533-537`) and other `< 33` checks
  - update the README

  If it is no: add an `@RequiresApi(33)` lint baseline and a `Build.VERSION.SDK_INT >= 33` gate helper for the D1 shader.
- **Why:** PLAN D1 and 06 P2 (fallbacks for RenderEffect 31, AGSL 33, anti-aliased path clip 33). M1 draws everything with `SquircleShape`, which on API < 33 falls back to `RoundedCornerShape` today. Under minSdk 29, M1 must keep that fallback.
- **Files:** `app/build.gradle.kts`, `IslandShapes.kt`, `IC`, `OS`, `README.md`.
- **Acceptance criteria:** Builds. S2 pixel-identical, because Robolectric runs at sdk 35.
- **Tests:** existing.
- **Depends on:** the owner's decision.
- **Effort:** S   **Risk:** low (drops Android 10–12 users; the owner decides).
- **Ships as:** Build.

### F6.2 Compose compiler reports and stability config
- **Goal:** Compiler metrics and reports behind a Gradle property, plus a `compose_stability.conf` that marks the model classes stable:
  - `IslandSettings`
  - `MediaInfo`
  - `NotificationInfo`
  - the scene types (immutable by construction)
- **Why:** 06 P2 and R12. M2.7 needs to see which composables are restartable and skippable.
- **Files:** `app/build.gradle.kts`, new `app/compose_stability.conf`.
- **API sketch:**
  ```kotlin
  composeCompiler {
      if (providers.gradleProperty("composeReports").isPresent) {
          reportsDestination = layout.buildDirectory.dir("compose_reports")
          metricsDestination = layout.buildDirectory.dir("compose_metrics")
      }
      stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("app/compose_stability.conf"))
  }
  ```
  `MediaInfo` and `NotificationInfo` hold `Bitmap`s. Mark them stable only if they are never mutated after construction. Verify in `NotificationReceiverService` first; if they are mutated, leave them unstable.
- **Acceptance criteria:** `./gradlew :app:assembleRelease -PcomposeReports` writes reports. S4 no worse.
- **Depends on:** —
- **Effort:** S   **Risk:** low.
- **Ships as:** Build.

### F6.3 Baseline profile
- **Goal:** A `:baselineprofile` module that generates an app baseline profile from S4's macrobenchmark journeys: service start, compact appear, expand, collapse, a media card, and dual split. `profileinstaller` is already a dependency (`app/build.gradle.kts`, "Installs the baseline profiles bundled with Compose").
- **Why:** 06 R12. The first expands stutter while Compose code warms up (JIT).
- **Files:** new `baselineprofile/` module, `settings.gradle.kts`, `app/build.gradle.kts` (`androidx.baselineprofile` plugin), `gradle/libs.versions.toml`.
- **Acceptance criteria:** The S4 macrobenchmark shows lower P90 frame duration for the **first** expand after install with the profile, compared with `CompilationMode.None`.
- **Depends on:** S4 (its benchmark module and journeys).
- **Effort:** M   **Risk:** low.
- **Ships as:** Perf.

### F6.4 Release signing
- **Goal:** The release build is signed with a real key, read from `~/.gradle/gradle.properties` or environment variables. It is never committed. It falls back to debug signing **only** when the properties are absent, with a build warning.
- **Why:** 06 P2: release is signed with the debug key (`app/build.gradle.kts:29`). That blocks Play distribution and lets anyone with the debug key ship an update.
- **Files:** `app/build.gradle.kts`, `.gitignore` (`*.jks`, `keystore.properties`).
- **Acceptance criteria:**
  - `apksigner verify --print-certs` shows the release certificate.
  - No secrets in git.
  - **The owner creates the keystore**; the task only wires it.
- **Depends on:** —
- **Effort:** S   **Risk:** low. Existing installs signed with the debug key can't update in place: the owner must decide and communicate.
- **Ships as:** Build.

---

## M1: One body, a single `Animatable<IslandGeometry>`

**4-D, not 5-D.** 03 R2 proposes `IslandGeometry(centerX, top, width, height, corner)` in one `Animatable`. Compose's `AnimationVector` is a sealed class with 1D–4D only, so a 5-D converter is impossible. The body is the existing 4-D `CardMorph`, renamed `IslandGeometry` (width, height, corner, top). Horizontal position is a second `Animatable<Float>`, always launched in the **same** coroutine with the **same** spec, so both stay in step and keep their velocities.

**Notch style.** The notch pill has square top corners (`notchPillShape`, `IslandShapes.kt:140-145`). One `corner` value can't morph it. M1.3 and M1.4 apply to `STYLE_PILL`. Notch keeps today's two-body path until M1.5.

### M1.1 `IslandShellState`: the card morph on an `Animatable`
- **Goal:** Replace `animateValueAsState(CardMorph)` (`IV:1036-1041`) and the horizontal-offset `animateDpAsState` (`IV:1049`) with one state holder that owns `Animatable<IslandGeometry>` plus `Animatable<Float>` for X. It is driven by `LaunchedEffect(target, spec)` → `animateTo`. **Pixel-identical**: `animateValueAsState` is itself an `Animatable` retargeted with the current velocity.
- **Why:** M1.3, M2 and M4 need direct `animateTo(target, spec, initialVelocity)`, `snapTo`, `velocity` and `isRunning`. `animate*AsState` exposes none of these.
- **Files:**
  - New: `island/shell/IslandShellState.kt`
  - Edit: `IV:1036-1051`, `IC:571-583` (`IslandGeometry`, from F1.3)
- **API sketch:**
  ```kotlin
  @Immutable
  data class IslandGeometry(val width: Dp, val height: Dp, val corner: Dp, val top: Dp) {
      companion object {
          val VectorConverter = TwoWayConverter<IslandGeometry, AnimationVector4D>(
              convertToVector = { AnimationVector4D(it.width.value, it.height.value, it.corner.value, it.top.value) },
              convertFromVector = { IslandGeometry(it.v1.dp, it.v2.dp, it.v3.dp, it.v4.dp) },
          )
          val VisibilityThreshold = IslandGeometry(0.1.dp, 0.1.dp, 0.1.dp, 0.1.dp)   // same as CardMorph today
      }
  }

  @Stable
  class IslandShellState(initial: IslandGeometry, initialX: Dp) {
      val body = Animatable(initial, IslandGeometry.VectorConverter, IslandGeometry.VisibilityThreshold)
      val x = Animatable(initialX.value)                           // dp as Float

      /** Retarget both together; velocity is kept unless [initialVelocity] overrides it (gestures, M4). */
      suspend fun animateTo(
          target: IslandGeometry, targetX: Dp, spec: AnimationSpec<IslandGeometry>, xSpec: AnimationSpec<Float>,
          initialVelocity: IslandGeometry = body.velocity,
      ) = coroutineScope {
          launch { x.animateTo(targetX.value, xSpec) }
          body.animateTo(target, spec, initialVelocity)
      }
      /** Deferred-read accessors (M2): call only inside layout/draw lambdas. */
      val geometry: IslandGeometry get() = body.value
      val velocity: IslandGeometry get() = body.velocity
      val isSettled: Boolean get() = !body.isRunning && !x.isRunning
  }

  @Composable fun rememberIslandShellState(initial: IslandGeometry, initialX: Dp): IslandShellState
  ```
- **Steps:**
  1. Add the holder.
  2. `LaunchedEffect(targets.card1, targets.horizontalOffset, spec) { shell.animateTo(…) }`.
  3. Local `animatedCard1Width`… become `shell.geometry.width` reads (still in composition for now; M2 defers them).
  4. The settle signal (`IV:1061-1070`, generalised in F5.3) uses `shell.isSettled`.
- **Acceptance criteria:** S1 bounds unchanged. S2 pixel-identical, including the F3.2 mid-frame captures. S4 no worse.
- **Tests:**
  - New `IslandShellStateTest` with `MonotonicFrameClock` from `kotlinx-coroutines-test`: animate to A, retarget to B mid-flight, and assert the frame-to-frame value change never exceeds the spring's own step (no jump).
  - Velocity at retarget equals the velocity before it.
- **Depends on:** F1.3, F3.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Refactor.

### M1.2 Continuity harness
- **Goal:** A reusable test that drives the real `IslandOverlayRoot` frame by frame through interrupted transitions and records the body's on-screen bounds every frame:
  - expand → collapse at 30 % / 60 % / 90 %
  - collapse → expand
  - a system alert arriving mid-expand
  - dual split mid-collapse

  The rule it asserts: **the silhouette never narrows during an expand, and no frame-to-frame jump exceeds ε**.
- **Why:** It is the regression gate for M1.3 and M1.4. It encodes 03 P0-1: "narrows 180 → 121 dp at about 80 ms before growing".
- **Files:** New `app/src/test/java/com/droidbubble/app/island/IslandContinuityTest.kt`.
- **API sketch:**
  ```kotlin
  /** Union of PILL and CARD bounds per frame (testTags IV:381-385), via composeTestRule.mainClock.advanceTimeByFrame(). */
  fun ComposeContentTestRule.recordSilhouette(frames: Int, step: () -> Unit = {}): List<Rect>
  fun List<Rect>.assertMonotonicWidth(fromFrame: Int, toFrame: Int)
  fun List<Rect>.assertNoJumpOver(epsilonDp: Float, density: Density)
  ```
- **Steps:**
  1. Write the recorder.
  2. Record **today's** behaviour as `@Ignore("fails until M1.3: two-body morph")` cases plus passing cases (pure collapse, pure expand ε).
- **Acceptance criteria:**
  - Runs in under 20 s.
  - Today's narrowing is reproduced in the ignored test's printed output (≈121 dp minimum width).
- **Depends on:** M1.1, S1 (harness).
- **Effort:** S   **Risk:** low.
- **Ships as:** Test.

### M1.3 Expand starts from the pill — **Intended diff (the core one)**
- **Goal:**
  - The card body's **collapsed target is the pill's rect**, not 0 width: width = the compact width target, height = `settings.heightDp`, corner = height / 2 (the pill's capsule corner, `IV:1803`), top = `settings.topOffsetDp`. That replaces `IV:918-922` and `IV:957-961`.
  - Expand therefore starts from the pill's *current* geometry, and collapse lands on it.
  - The card container's alpha stays 1: delete `cardContainerAlpha`, `IV:1084`, and its use at `IV:1317`.
  - The pill body hides (alpha 0, not removed) while the card body is wider than the pill, so there is exactly one black shape on screen.
  - Content still fades by `expandProgress` (M3 changes that).
- **Why:** 03 P0-1, 02 R12 and PLAN §2 "one living shape". It fixes "vanish and re-pop".
- **Files:** `IV:900-961`, `IV:1084`, `IV:1308-1340`, `IV:1822-1838`, and `deriveIslandTargets` (F1.3).
- **API sketch:**
  ```kotlin
  // deriveIslandTargets
  val pillRect = IslandGeometry(width = compactWidth, height = settings.heightDp.dp,
                                corner = settings.heightDp.dp / 2, top = settings.topOffsetDp.dp)
  val card1 = if (scene.mode == IslandMode.EXPANDED) expandedGeometry else pillRect
  ```
  The squircle budget caps the corner at half the short side (`IslandShapes.kt:36-37`), so a 36 × 36 → 340 × H morph is a continuous capsule → squircle card. **Still drawn with `islandShape()` → `SquircleShape(…, 0.6f)`.**
- **Steps:**
  1. Change the collapsed target.
  2. Remove the container alpha.
  3. Hand off between pill and body: while `shell.geometry.width > compactWidthTarget + 0.5.dp` **or** `scene.mode == EXPANDED`, the pill draws its content layer only (no black body). The body draws the black surface.
  4. On settle in compact, the body hides and the pill shows. Both rects are identical at that moment, so the swap is invisible.
  5. The stretch (`IV:1316-1328`) stays as is (M2.4 moves it to velocity).
  6. Un-ignore the M1.2 cases.
- **Acceptance criteria:**
  - M1.2: the silhouette width is monotonic during expand and during collapse, and no jump exceeds 1 dp per frame at 120 Hz.
  - S1 settled bounds **unchanged**: the end states are identical, only the path differs.
  - S2 settled states pixel-identical. Mid-frame captures are the intended diff; attach them.
  - Device at 0.7× and 2.0×, Pixel plus one OEM: expand reads as one shape growing down out of the pill, and collapse as the shape returning into it. Interrupting by double tap is continuous.
  - **Design check:** squircle corners at every frame, same black, same card.
- **Tests:** M1.2 enabled. `IslandOverlayCrashTest` green, especially "swipes mid-animation".
- **Depends on:** M1.2, F5.2 (otherwise the first grown frames are clipped by the compact window, which is more visible now that the body starts at the pill).
- **Effort:** M   **Risk:** high (most visible change; review on devices).
- **Ships as:** Polish (signature motion).

### M1.4 One surface for pill and card
- **Goal:** The black pill body and the card body become **one surface** owned by the shell. In compact the shell's geometry *is* the pill (width = compact width), so the separate pill-width spring (`IV:1032-1035`) stops driving a second black shape. It drives only the compact content's layout width (until M2.6). The compact content and the expanded content are two layers inside the same clipped surface.
- **Why:** M1.3 still hands off between two black shapes at settle. One surface removes the hand-off and makes alert widen (charging, unlock), appear and hide the same morph:
  - widen = width change on the body using `appear`
  - appear and hide = width to and from the camera circle using `split`
- **Files:** `IV:1266-1340`, `IV:1654-1838`, `IV:1954-2266`, `island/shell/*`.
- **API sketch:**
  ```kotlin
  @Composable
  fun IslandBody(
      shell: IslandShellState,
      shape: (IslandGeometry) -> Shape = { islandShape(it.corner) },     // SquircleShape, smoothing 0.6
      background: Brush, outline: BorderStroke?,
      compactLayer: @Composable BoxScope.() -> Unit,
      expandedLayer: @Composable BoxScope.() -> Unit,
  )
  ```
  Spec choice for the one body uses the user's curves:

  | Transition | Spec |
  |---|---|
  | → EXPANDED | `motion.expand` (user) |
  | EXPANDED → compact | `motion.collapse` |
  | compact ↔ alert width | `motion.appear` (user) |
  | width ↔ camera circle (appear, hide, minimise) | `motion.split` (user) |
  | while expanding, the old pill-shrink token | `pillShrinkOnExpand`, retired |

- **Steps:**
  1. Introduce `IslandBody`.
  2. Move the pill's `graphicsLayer` effects onto the body's compact layer: press scale, idle bounce, swap squash (`IV:1829-1836`).
  3. Keep the bubble, the neck and the parentheses as siblings. D1 replaces the neck.
  4. Keep `IslandTestTags.PILL` on the body while compact and `CARD` while expanded, so S1 keeps working.
- **Acceptance criteria:**
  - S1 settled bounds unchanged.
  - S2 settled states pixel-identical. Alert widen and hide mid-frames are the intended diff.
  - M1.2 green, plus a new case: the charging alert interrupting an expand stays continuous.
  - Device check, including notch (unchanged path) and high contrast (the outline follows the body).
- **Tests:** M1.2 extended. `IslandOverlayCrashTest`.
- **Depends on:** M1.3, M2.1 (do it inside the extracted shell, not in `IV`).
- **Effort:** M   **Risk:** high.
- **Ships as:** Polish.

### M1.5 Notch style on the one body
- **Goal:** Bring `STYLE_NOTCH` onto the one body. The body gets a `topCornerFactor` (0 = square, 1 = same as bottom), animated on the X `Animatable`'s sibling, or fixed by mode:
  - notch compact: top 0, bottom 0.45 h
  - expanded: all corners squircle

  The notch bridge (`IV:1798-1820`) follows the body's width.
- **Why:** M1.3 and M1.4 leave notch users on the two-body path.
- **Files:** `IslandShapes.kt` (a `SquircleShape` variant with separate top and bottom radii, same smoothing 0.6), `island/shell/*`.
- **API sketch:**
  ```kotlin
  class SquircleShape(radius: Dp, smoothing: Float = 0.6f, topRadius: Dp = radius)   // backwards compatible
  ```
- **Acceptance criteria:**
  - Notch S2 settled states pixel-identical. At a 0 top radius the notch path must equal `notchPillShape`'s `RoundedCornerShape`, so the squircle's bottom must match at smoothing 0.6. **If not, keep `RoundedCornerShape` for the notch compact and document the exception.**
  - The M1.2 notch case is green.
- **Tests:** an `IslandShapesTest` path-equality check at the settled sizes, plus M1.2.
- **Depends on:** M1.4.
- **Effort:** M   **Risk:** medium.
- **Ships as:** Polish.

---

## M2: Deferred reads (target: about 0 recompositions per frame)

### M2.1 Extract the shell into `island/shell/` (mechanical move)
- **Goal:** Move the card stack, pill body, bubble, neck, notch bridge and parentheses out of `IslandOverlayRoot` (`IV:1266-2341`) into `island/shell/IslandShell.kt` as composables with explicit parameters. **No logic change.**
- **Why:** 06 §4 target architecture. Every following M task edits the shell, not the 4,711-line file, which also eases the contention rule.
- **Files:** `IV:1233-2342` → `island/shell/IslandShell.kt`, `island/shell/CardStack.kt`, `island/shell/Pill.kt`, `island/shell/SplitNeck.kt`, `island/shell/Parentheses.kt`.
- **Steps:**
  1. Move one region per commit.
  2. Pass values as parameters exactly as they are read today.
  3. Keep the presentations (`CompactContent`, `ExpandedCardContent`…) in `IV`.
- **Acceptance criteria:** S1 unchanged. S2 pixel-identical. S4 unchanged (± noise).
- **Tests:** existing.
- **Depends on:** M1.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Refactor.

### M2.2 Size and position read in the layout phase
- **Goal:** The body's size comes from `Modifier.layout {}` reading `shell.geometry` inside the measure block. Its position comes from `offset {}`, which is already a lambda at `IV:1270-1275`. This replaces `.width(animatedCard1Width).height(…)` (`IV:1313-1314`) and `.width(animatedCompactWidth)` (`IV:1827`). The branches on animated values (`IV:1229`, `1250`, `1267`, `1308`, `1675`, `1823`) are replaced by **target-based** booleans: "is the shell expanded or animating", read from `scene` plus `shell.isSettled` through `derivedStateOf`.
- **Why:** 06 P0-1 and 03 R3: reading an animated value in composition recomposes the root every frame.
- **Files:** `island/shell/*`.
- **API sketch:**
  ```kotlin
  fun Modifier.islandBodySize(shell: IslandShellState): Modifier = layout { measurable, _ ->
      val g = shell.geometry                                 // read here, not in composition
      val w = g.width.coerceAtLeast(0.dp).roundToPx(); val h = g.height.coerceAtLeast(0.dp).roundToPx()
      val p = measurable.measure(Constraints.fixed(w, h))
      layout(w, h) { p.place(0, 0) }
  }
  /** Coarse, composition-safe flags: change a handful of times per morph, not per frame. */
  @Composable fun IslandShellState.rememberPhase(): State<ShellPhase>   // derivedStateOf { HIDDEN / COMPACT / MORPHING / EXPANDED }
  ```
- **Acceptance criteria:**
  - S1 unchanged. S2 pixel-identical, including mid-frames.
  - S4: recompositions of `IslandShell` during a 500 ms expand drop from about one per frame to ≤ 10. Record the number; M2.7 finishes the job.
- **Tests:** S4 counter test with a tightened threshold.
- **Depends on:** M2.1.
- **Effort:** M   **Risk:** medium (layout semantics, clip bounds).
- **Ships as:** Perf.

### M2.3 Cached squircle surface
- **Goal:** Background, outline and clip of the body draw through `drawWithCache`. The `SquircleShape` outline is rebuilt only when the size or the corner (in 0.25 dp buckets) changes. The content clip uses `graphicsLayer { shape = cached; clip = true }` with the cached instance. Today a new `islandShape(animatedCornerRadius)` is allocated per frame for both clip and border (`IV:1329`, `IV:1336`).
- **Why:** 06 P2 "Shape cost" and 03 P0-2. **The shape stays `SquircleShape` at 0.6.** Only the caching changes.
- **Files:** `island/shell/IslandBody.kt` (new or in `IslandShell.kt`), `IslandShapes.kt` (a small cache helper).
- **API sketch:**
  ```kotlin
  /** One SquircleShape per 0.25 dp corner bucket; SquircleShape is @Immutable with value equality (IslandShapes.kt:100-103). */
  object SquircleCache { fun get(corner: Dp): Shape }
  fun Modifier.islandSurface(shell: IslandShellState, brush: Brush, outline: BorderStroke?): Modifier =
      drawWithCache {
          val corner = shell.geometry.corner
          val outlinePath = (SquircleCache.get(corner).createOutline(size, layoutDirection, this) as? Outline.Generic)?.path
          onDrawBehind { /* drawPath(outlinePath, brush); outline?.let { drawPath(outlinePath, it.brush, style = Stroke(it.width.toPx())) } */ }
      }
  ```
  `islandShape()`'s API < 33 `RoundedCornerShape` fallback stays (or goes, per F6.1).
- **Acceptance criteria:**
  - S2 pixel-identical: the 0.25 dp bucketing is below a pixel at xxhdpi. Verify, and use 0.1 dp if any diff appears.
  - An allocation tracker (Android Studio memory profiler or a Perfetto heap sample) shows no `Path` allocation per frame during a morph.
- **Tests:** `SquircleCacheTest`: bucket identity and equality.
- **Depends on:** M2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Perf.

### M2.4 Stretch and blur from the spring's real velocity
- **Goal:** The card stretch (`IV:1316-1328`) and the morph blurs (`IV:1464-1466`, `IV:2218-2220`, `IV:2244-2246`) read `shell.velocity` inside `graphicsLayer {}`. Delete `rememberMorphSpeed` and `MorphSpeedMeter` (`IC:616-663`) and `MorphSpeedMeterTest`.
- **Why:** 06 P0-1 and 03 R3. `MorphSpeedMeter` takes `System.nanoTime()` in composition and depends on per-frame recomposition (`IC:623-628`). Velocity is exact and frame-timed.
- **Files:** `island/shell/*`, `IC:616-663`, `app/src/test/java/com/droidbubble/app/island/MorphSpeedMeterTest.kt` (delete).
- **API sketch:**
  ```kotlin
  /** 0…1 morph speed from the body's velocity, the same scale as MorphSpeed.speed (fullSpeed 1800 dp/s, IC:630). */
  fun IslandShellState.morphSpeed(fullSpeedDpPerSec: Float = 1800f): Float =
      ((abs(velocity.width.value) + abs(velocity.height.value)) / fullSpeedDpPerSec).coerceIn(0f, 1f)
  fun IslandShellState.isGrowing(target: IslandGeometry): Boolean =
      (target.width + target.height) > (geometry.width + geometry.height)

  Modifier.graphicsLayer {
      val s = if (motion.allowsStretch) shell.morphSpeed() else 0f
      if (s > 0f && shell.isGrowing(target) && !glass) { transformOrigin = TransformOrigin(0.5f, 0f); scaleX = 1f + 0.035f * s; scaleY = 1f - 0.02f * s }
  }
  ```
- **Acceptance criteria:**
  - S2 settled pixel-identical.
  - Mid-frame stretch within 0.5 % of the old value. The old value was smoothed half-and-half (`IC:660`); a small diff is expected and reviewed.
  - S4 counter: no composition reads of the velocity (it is read only in `graphicsLayer`).
- **Tests:** `IslandShellStateTest`: `morphSpeed()` is 0 at rest and peaks mid-morph.
- **Depends on:** M2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Perf.

### M2.5 Glass publishing without `SideEffect`
- **Goal:** Publish glass-pane rects from one `onPlaced` per card. The strengths are read from the shell inside that callback. Remove `SideEffect { publishGlass() }` (`IV:1149`), which runs on every recomposition.
- **Why:** 06 P0-1. With M2.2 there is no per-frame recomposition, so the `SideEffect` would no longer fire per frame. The blur windows must follow the body from the layout callback, which still runs each frame because the size changes.
- **Files:** `island/shell/CardStack.kt`, `IV:1107-1152` (moved by M2.1).
- **API sketch:**
  ```kotlin
  Modifier.onPlaced { glass.publish(index = 0, boundsInWindow = it.boundsInWindow(), strength = shell.contentProgress(), cornerPx = shell.geometry.corner.toPx()) }
  ```
- **Acceptance criteria:** Device with liquid glass on: the panes track the card on expand and collapse with no lag beyond today's (`GlassPaneWindows.kt` header notes about 1 frame). S2 pixel-identical with glass off.
- **Tests:** existing glass Robolectric checks, if any; otherwise a `GlassPublisherTest` for the strength rounding (`IV:1141`).
- **Depends on:** M2.2.
- **Effort:** S   **Risk:** low (opt-in feature).
- **Ships as:** Perf.

### M2.6 Pill content laid out at its final width
- **Goal:**
  - Compact content is measured once at the **target** compact width and revealed by the body's clip. That is how the cards already work (`IV:1455-1456`), replacing `fillMaxSize` inside the animated width (`IV:2211-2264`).
  - Compact alpha and blur (`IV:1954-1958`, `IV:2215-2221`) are read in `graphicsLayer` from the shell.
  - The emergence alpha (`IV:1669-1671`) and the bubble alpha (`IV:1677-1690`) move into `graphicsLayer` too.
- **Why:** 06 P0-1 "re-measures the pill content".
- **Files:** `island/shell/Pill.kt`.
- **Acceptance criteria:**
  - S2 settled pixel-identical.
  - Mid-frames of appear and hide: content no longer reflows; it is revealed by the clip. Intended, reviewed.
  - S4: no `CompactContent` recomposition during a width morph.
- **Tests:** S4 counter on a compact appear.
- **Depends on:** M2.2, M1.4 (one surface: the reveal is the body's clip).
- **Effort:** S   **Risk:** medium (text that used to squeeze now clips during the morph; check RTL).
- **Ships as:** Perf + polish.

### M2.7 Narrow state reads and the recomposition gate
- **Goal:**
  - Leaf data reaches the leaves as state or lambdas: network speed (1 Hz), recording seconds, flashlight level, media position, FPS and weather. The shell and root read only `scene`, `targets` and settings.
  - Use `collectAsStateWithLifecycle` with the overlay lifecycle paused when the screen is off or the island hidden (06 R3).
  - **Turn the S4 counter into a hard gate:** ≤ 2 shell recompositions per expand or collapse.
- **Why:** 06 P0-1 ("1 Hz flows recompose everything") and R3.
- **Files:** `IV` (presentations' parameters), `island/shell/*`, `OS:688-692` (lifecycle owner: drive `ON_PAUSE`/`ON_RESUME` from `SCREEN_OFF`/`SCREEN_ON`, already received at `OS:524-525`).
- **API sketch:**
  ```kotlin
  @Composable fun CompactContent(priority: IslandPriority, networkSpeed: () -> NetworkSpeedInfo, recordingSeconds: () -> Int, /* … */)
  ```
- **Acceptance criteria:**
  - S4 gate: a 500 ms expand gives ≤ 2 shell recompositions. 10 s idle with network speed on gives 0 root recompositions; only the speed text recomposes, about 10 times.
  - S2 pixel-identical. Device check: screen off → on resumes animations.
- **Tests:** the S4 counter test as a hard assertion.
- **Depends on:** M2.6, F1.5.
- **Effort:** M   **Risk:** medium.
- **Ships as:** Perf (battery).

---

## M3: Choreography: content follows the shape

### M3.1 Content progress as a pure function of geometry
- **Goal:** `p` = the body's progress from the compact rect to the expanded rect, from geometry, not a separate `expandProgress` spring (`IV:687-691`). Every content alpha, scale and blur is a pure function of `p`, the **same curve in both directions**. This replaces the direction-dependent thresholds (`IV:1446-1450`, `IV:1954-1958`).
- **Why:**
  - 03 R4: no hysteresis jumps when interrupted (03 P2)
  - PLAN §2: shape leads, content follows
- **Files:**
  - New: `island/shell/IslandChoreography.kt`
  - Edit: `island/shell/*`
- **API sketch:**
  ```kotlin
  /** 0 = compact rect, 1 = expanded rect; height-weighted so the drop leads. Clamped. */
  fun contentProgress(g: IslandGeometry, compact: IslandGeometry, expanded: IslandGeometry): Float

  object IslandChoreography {
      fun incomingAlpha(p: Float, start: Float = 0.45f) = smoothstep(start, 0.90f, p)          // leading items 0.45, trailing 0.55
      fun incomingScale(p: Float) = lerp(0.92f, 1f, incomingAlpha(p))                           // anchored top-centre
      fun compactAlpha(p: Float) = 1f - smoothstep(0f, 0.35f, p)                               // compact gone by p 0.35
      fun leavingBlurFraction(p: Float, contracting: Boolean) = if (contracting) 1f - incomingAlpha(p) else 0f // blur only on leave
      const val LEAVING_BLUR_MAX_DP = 8f; const val INCOMING_BLUR_MAX_DP = 4f; const val LEAVING_SCALE = 0.96f
  }
  private fun smoothstep(e0: Float, e1: Float, x: Float): Float { val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f); return t * t * (3 - 2 * t) }
  ```
  `contracting` comes from the sign of `shell.velocity.height` and is read in `graphicsLayer`. It only toggles the blur, never alpha, so there are no jumps. Values come from 03 §6; the blur obeys `motion.allowsBlur`.
- **Acceptance criteria:**
  - S2 settled pixel-identical. Mid-frames are the intended diff.
  - M1.2 extended: content alpha is continuous across a 50 % interruption (max frame-to-frame Δα ≤ 0.15 at 120 Hz).
  - S4 gate holds.
- **Tests:** `IslandChoreographyTest`: symmetry (f(p) is the same whichever direction p was reached from), endpoints, monotonicity.
- **Depends on:** M2.4.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

### M3.2 Staggers in progress, not wall-clock delays
- **Goal:** Replace the wall-clock `delay`s with progress or geometry conditions:
  - card 2 at 45 ms (`IV:963-972`) → card 2 starts at `p ≥ 0.1`
  - bubble emerge at 60 ms (`IV:988-997`) → when the body width reaches 95 % of its compact target
  - parentheses shoot at 70 ms (`IV:1167-1168`) → when the body width reaches 60 % of its target

  These conditions survive interruptions and speed changes, and REDUCED.
- **Why:** 03 P2 ("wall-clock staggers aren't interrupt-aware") and 03 §6 ("card 2 offset by p + 0.1").
- **Files:** `island/shell/*`, `deriveIslandTargets` (drop the `card2Expanded` and `bubbleEmerged` inputs, F1.3).
- **API sketch:** `val card2Started by remember { derivedStateOf { contentProgress(shell.geometry, …) >= 0.1f } }`. It flips once per morph, so it costs one recomposition (allowed).
- **Acceptance criteria:**
  - S1 settled bounds unchanged.
  - Mid-frames are the intended diff: card 2 starts slightly later at slow speeds and earlier at fast speeds.
  - M1.2 case: collapse during the card 2 lead is continuous.
- **Tests:** M1.2.
- **Depends on:** M3.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

### M3.3 Expanded content keyed per activity
- **Goal:** Wrap `ExpandedCardContent` (`IV:1469`) in `AnimatedContent(targetState = contentKey)`:
  - `contentKey` = (priority, notification key or media track key)
  - `SizeTransform` off, because geometry handles the size
  - Incoming: fade and scale from `IslandChoreography` with `motion.effects`
  - Leaving: fade with blur

  When the primary activity changes while expanded, the old content crossfades out as the card height springs to the new one. No hard cut.
- **Why:** 03 P1-4 and PLAN M3.
- **Files:** `island/shell/CardStack.kt` (the call site), `IV:2973` (`ExpandedCardContent` stays in `IV`).
- **API sketch:**
  ```kotlin
  @Immutable data class ContentKey(val priority: IslandPriority, val id: String?)   // n:<key>, media:<pkg>:<title>, …
  fun contentKeyFor(scene: IslandScene, inputs: IslandInputs, slot: Slot): ContentKey
  ```
  Phase 7's `ActivityId` (A1.2) becomes the `id` later.
- **Acceptance criteria:**
  - S2: no change to settled states.
  - A new S2 mid-frame shows the crossfade when a notification replaces media while expanded.
  - REDUCED (M5.2): a plain 150 ms crossfade.
- **Tests:** `contentKeyFor` table test. Robolectric: switching primary while expanded never shows a frame with neither content (alpha sum ≥ 0.9).
- **Depends on:** M3.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

### M3.4 Compact content keyed per activity
- **Goal:** The compact `AnimatedContent` (`IV:2226-2237`) keys on `ContentKey`, not just `primaryPriority`. A new notification from another app at the same priority now transitions instead of hard-cutting. Titles without digits (01 / 03 P1-4) crossfade instead of swapping. The transition specs come from `motion` (F3.2). The `swapSquash` stays: D2 replaces it with `alertBump`.
- **Why:** 03 P1-4.
- **Files:** `island/shell/Pill.kt`, `IV:2654` (`CompactContent`).
- **Acceptance criteria:**
  - S2 settled pixel-identical.
  - The new mid-frame "message from app B replaces app A" shows the transition.
  - Same-key updates (a timer tick, a new message in the same conversation) do **not** animate the container. That is `RollingNumberText`'s job, and later I1's bounce.
- **Tests:** `contentKeyFor` table rows for same-key and new-key.
- **Depends on:** M3.3.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.

---

## M4: Gestures, `Modifier.islandGestures`

Interaction spec: 05 §6 (verified against code). After B6, swipe-right no longer hides.

### M4.1 `Modifier.islandGestures`: one detector with a pure release resolver
- **Goal:** One pointer-input modifier:
  - `awaitEachGesture` with touch slop
  - axis lock after slop
  - a `VelocityTracker`
  - thresholds in dp
  - RTL-aware inward and outward
  - a threshold-crossed callback
  - children keep first claim: only unconsumed changes are claimed, at `PointerEventPass.Main`

  The decision on release is a **pure function**. Not wired yet.
- **Why:** 05 R2 and 03 R5. Today three detectors at `IV:1286-1304`, `1342-1383`, `1515-1556` and `1850-1877` use pixel thresholds (−15 px, 160 px), and none uses velocity.
- **Files:** New `island/gesture/IslandGestures.kt`, `island/gesture/GestureResolver.kt`.
- **API sketch:**
  ```kotlin
  enum class GestureSurface { COMPACT, MINIMAL, EXPANDED_CARD }
  enum class Axis { HORIZONTAL, VERTICAL }

  @Immutable
  data class GestureThresholds(
      val commit: Dp = 48.dp,                 // 05 R2
      val expandMin: Dp = 24.dp,              // 05 §6: swipe down 24–64 dp expands
      val shadeAt: Dp = 64.dp,                // past this, open the shade
      val flingVelocity: Dp = 1000.dp,        // per second; ViewConfiguration.scaledMinimumFlingVelocity is the floor
      val rubberBandLength: Dp = 120.dp,      // x' = x / (1 + |x|/L)
  )

  sealed interface IslandGestureResult {
      data class Expand(val velocityY: Float) : IslandGestureResult
      data object OpenShade : IslandGestureResult
      data class Tuck(val velocityY: Float) : IslandGestureResult                 // expanded → compact
      data class Dismiss(val direction: Int, val velocityX: Float) : IslandGestureResult
      data class TuckInstead(val velocityX: Float) : IslandGestureResult         // calls/alarms (B6)
      data object Minimise : IslandGestureResult
      data object Restore : IslandGestureResult
      data class Cancel(val velocity: Velocity) : IslandGestureResult            // spring back with this velocity
  }

  /** Pure: fully table-testable. "Inward" = toward the pill's centre from where the drag started. */
  fun resolveRelease(
      surface: GestureSurface, axis: Axis, offset: Offset, velocity: Velocity, startX: Float, centreX: Float,
      thresholds: GestureThresholdsPx, isRtl: Boolean, canDismiss: Boolean,
  ): IslandGestureResult

  fun rubberBand(x: Float, lengthPx: Float): Float = x / (1f + abs(x) / lengthPx)

  fun Modifier.islandGestures(
      surface: GestureSurface,
      thresholds: GestureThresholds = GestureThresholds(),
      canDismiss: () -> Boolean,
      onDrag: (axis: Axis, offset: Offset) -> Unit,              // already rubber-banded; write into an Animatable via snapTo
      onThresholdCrossed: (axis: Axis, crossed: Boolean) -> Unit, // → haptics.perform(THRESHOLD_CROSSED)
      onRelease: (IslandGestureResult) -> Unit,
  ): Modifier
  ```
  Inward and outward are measured from the drag's start relative to the pill's centre, so they don't depend on the layout direction. **When the drag starts within ±8 dp of the centre**, "inward" falls back to the layout direction's end→start (LTR: leftward). That is the only place `isRtl` matters, and it keeps today's swipe-left-minimises for LTR.
- **Acceptance criteria:** No wiring, so no visible change. The resolver table covers every row of 05 §6.
- **Tests:** `GestureResolverTest` with at least 25 rows:
  - slow drag past 48 dp → commit
  - fast short fling → commit
  - slow short drag → Cancel with its velocity
  - swipe down 40 dp → Expand; 80 dp → OpenShade
  - inward from the right half
  - inward from the left half
  - RTL near the centre
  - a call card sideways → TuckInstead
  - diagonal → axis lock
  - plus `rubberBand` monotonic and bounded
- **Depends on:** F3.3 (haptic events), B6.
- **Effort:** M   **Risk:** low.
- **Ships as:** Plumbing.

### M4.2 Expanded card, sideways: follows the finger, flings out or springs back
- **Goal:** Card 1 and card 2 use `islandGestures(EXPANDED_CARD)`. While dragging, the card's X `Animatable` follows the finger: `snapTo`, unbanded for dismissable cards and rubber-banded for non-dismissable ones. The screen-recording card keeps its 0.18 resistance (`IV:1375`, `IV:1550`). On release:
  - **Dismiss:** `animateTo(±offscreen, motion.spring(motion.dismiss), initialVelocity = v)`, then the store dismisses. No spring-back before vanishing (`IV:1367`, `IV:1541`).
  - **TuckInstead** (calls and alarms): collapse with `v`.
  - **Cancel:** spring back with `v` using `motion.interactive`.

  The haptic fires when crossing the threshold, not on release.
- **Why:** 03 P1-2 and 05 R2.
- **Files:** `island/shell/CardStack.kt` (replacing the moved `IV:1342-1383`, `IV:1515-1556`, and the offset state `IV:1086-1105`).
- **Acceptance criteria:**
  - Device:
    - the card tracks the finger 1:1
    - a quick flick dismisses from a short distance
    - a slow release under the threshold springs back carrying the release velocity
    - a dismissed card leaves the screen in the fling direction and never returns first
    - a ringing call tucks
  - S2 settled pixel-identical.
  - `IslandOverlayCrashTest` swipe cases green.
- **Tests:** a Robolectric gesture test with `performTouchInput { swipeLeft(durationMillis = 80) }` → dismissed. Slow 30 dp → offset returns to 0.
- **Depends on:** M4.1, M1.1.
- **Effort:** S   **Risk:** medium.
- **Ships as:** Polish (gestures).

### M4.3 Expanded card, vertical: swipe-up tucks with velocity hand-off
- **Goal:**
  - The swipe-up detector (`IV:1286-1304`: pill zone only, −15 px, no motion) becomes `islandGestures` on the same pill zone (`PillZoneExtra`, F2.4).
  - While dragging up, the body's `top`/`height` follow the finger, rubber-banded.
  - Release past the threshold or with an upward fling → `Tuck(v)` → collapse with `initialVelocity = IslandGeometry(height = v, top = v…)`.
  - Release before → spring back with `v`.
  - Dragging down on the expanded card rubber-bands and springs back (no action).
- **Why:** 03 P1-2: "slow swipe-ups never collapse" and "no velocity hand-off".
- **Files:** `island/shell/CardStack.kt`, `IslandShellState` (add `dragTo(offset)` that `snapTo`s a drag-adjusted geometry without losing the target).
- **API sketch:**
  ```kotlin
  /** Drag displaces the body from its target; release hands the finger velocity to the spring. */
  suspend fun IslandShellState.dragBy(target: IslandGeometry, dy: Float)          // snapTo(target.copy(top = …, height = …))
  suspend fun IslandShellState.release(target: IslandGeometry, spec: AnimationSpec<IslandGeometry>, velocityY: Dp)
  ```
- **Acceptance criteria:**
  - Device: a slow 60 dp swipe up collapses. A flick collapses faster than a tap-outside collapse. A short pull springs back.
  - Sliders and buttons inside the card are unaffected: the media seek bar still seeks.
  - M1.2: continuity during drag → release.
- **Tests:** Robolectric: `swipeUp` from the pill zone collapses; `swipeUp` starting on the media slider doesn't.
- **Depends on:** M4.1, M1.3.
- **Effort:** S   **Risk:** medium.
- **Ships as:** Polish.

### M4.4 Compact pill: down to expand, further for the shade, inward to minimise
- **Goal:** Replace the pill's horizontal detector (`IV:1850-1877`) with `islandGestures(COMPACT)` and `(MINIMAL)`:
  - **Down 24–64 dp → Expand(v).** The body follows the finger downward, rubber-banded, and expand starts with the finger's velocity.
  - **Down past 64 dp → OpenShade.** `IslandAccessibilityService.instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)`. Without the accessibility service, fall back to Expand.
  - **Inward → Minimise.** **Outward on the ring → Restore.**
  - Horizontal drags move the pill body (rubber-banded) before release.
  - Tap and long-press stay on `combinedClickable` (`IV:1878-1951`).
- **Why:** 05 §5 ("a downward swipe on the pill expands it, and pulling further opens the system shade") and D6.
- **Files:** `island/shell/Pill.kt`.
- **Acceptance criteria:**
  - Device (Pixel, Samsung):
    - a downward pull expands
    - a long pull opens the shade
    - inward minimises to the ring
    - outward or a tap restores
    - the status bar outside the pill stays tappable (B2)
    - RTL locale: inward and outward behave the same relative to the centre
  - S2 settled pixel-identical.
- **Tests:**
  - Robolectric swipes for each branch
  - `IslandOverlayCrashTest` "repeated swipes / mid-animation" green
  - the shade path verified with a fake accessibility instance
- **Depends on:** M4.1, M1.4, B6, B2.
- **Effort:** M   **Risk:** medium (shade gesture conflicts with the system's own pull-down on some OEMs).
- **Ships as:** Polish (gestures).

### M4.5 Back and predictive back collapse
- **Goal:** While expanded, Back collapses the island. On API 34+, the predictive-back progress drives a shrink preview: the body lerps toward the compact rect by up to 10 % and its corner grows. Commit collapses with the gesture's velocity; cancel springs back. The window must be **focusable** while expanded:
  - `WindowSpec.focusable = expanded && settings.backCollapses`
  - drop `FLAG_NOT_FOCUSABLE` the same way the reply does (`OS:806-819`)
- **Why:** 05 R7 and §5. iOS has no back gesture; this is Droidbubble's own.
- **Files:** `service/window/*` (focusable), the `ComposeView` wrapper (`OS:688`), `island/shell/IslandShell.kt`, `IslandSettings` and `SettingsRepository` (`backCollapses`, default on after the device check).
- **API sketch:**
  ```kotlin
  // API 33+: view.findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(PRIORITY_OVERLAY, callback)
  // API 34+: OnBackAnimationCallback { onBackProgressed(e) → shell.previewBack(e.progress); onBackInvoked → collapse; onBackCancelled → springBack }
  // API 29–32: wrapper FrameLayout.dispatchKeyEvent(KEYCODE_BACK, ACTION_UP) → collapse
  suspend fun IslandShellState.previewBack(progress: Float, compact: IslandGeometry, expanded: IslandGeometry)
  ```
  The overlay has a hand-made lifecycle owner (`OS:688-692`) and no `OnBackPressedDispatcherOwner`. So use the platform dispatcher directly rather than Compose's `PredictiveBackHandler`, or provide `LocalOnBackPressedDispatcherOwner` explicitly.
- **Acceptance criteria:**
  - Device on API 34+ (Pixel) and an API 30–33 device:
    - Back collapses
    - the predictive gesture previews and cancels cleanly
    - the app below does **not** receive that Back
    - after collapse the window is non-focusable again, and the app's keyboard focus returns
  - Document the known trade-off: an IME open in the app below closes when the island takes focus.
- **Tests:** Robolectric: the `KEYCODE_BACK` path collapses; `WindowSpec` focusable only while expanded.
- **Depends on:** F5.1, M1.1, M4.3 (shared release path).
- **Effort:** M   **Risk:** high (focus and IME interplay; OEM back handling; flag-gated).
- **Ships as:** Feature.

---

## M5: Motion tiers: Full, Efficient and Reduced

### M5.1 Resolve the tier
- **Goal:** One pure function picks the tier:
  - **REDUCED:** the in-app "Reduce motion" setting is on, or the system animator scale is 0 or below 1 (05 R11)
  - **EFFICIENT:** Battery Saver is on (`AudioPowerPolicy.lowPower`, `AudioLevels.kt:40-42`)
  - otherwise **FULL**

  The tier is fed into `IslandMotion` (F3.1) and updates live.
- **Why:** 03 R8 and P1-5, PLAN M5. Compose already snaps springs when the animator scale is 0, but `delay` staggers and service timers keep running (03 §2).
- **Files:**
  - New: `island/motion/MotionTierSource.kt`
  - Edit: `IslandSettings` (`reduceMotion: Boolean = false`), `SettingsRepository` (both mappers, `:76-146` and `:147-215`, plus the writer around `:246-269`), `OS` (provide the tier), and one switch on the existing Accessibility settings page (C-phase owns the wider settings work)
- **API sketch:**
  ```kotlin
  fun resolveMotionTier(reduceMotionSetting: Boolean, animatorScale: Float, batterySaver: Boolean): MotionTier = when {
      reduceMotionSetting || animatorScale < 1f -> MotionTier.REDUCED
      batterySaver -> MotionTier.EFFICIENT
      else -> MotionTier.FULL
  }
  /** Watches Settings.Global.ANIMATOR_DURATION_SCALE via ContentObserver and AudioPowerPolicy. */
  class MotionTierSource(context: Context, settings: StateFlow<IslandSettings>) { val tier: StateFlow<MotionTier> }
  ```
- **Acceptance criteria:**
  - Toggling the system "Remove animations" or Battery Saver changes the tier without a restart.
  - Default FULL: S2 pixel-identical.
- **Tests:** `MotionTierTest` table; a `SettingsRepository` round-trip for `reduceMotion`.
- **Depends on:** F3.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Accessibility.

### M5.2 Apply REDUCED and EFFICIENT everywhere
- **Goal:**
  - **REDUCED:**
    - geometry ζ 1 at 0.3 s
    - no stretch, squash, bounce, shake, cover flip, parentheses shoot or idle bounce
    - no blur
    - content uses 150 ms crossfades
    - `RollingNumberText` crossfades instead of rolling
    - the equalizer and waveform draw a static frame
    - progress staggers are instant (p-based, so they simply run with the shorter geometry)
  - **EFFICIENT:**
    - blur off (already, `IC:607`)
    - stretch off
    - frame loops capped at `loop().frameIntervalMillis` (≥ 33 ms)
- **Why:** 03 R8 and 05 R11.
- **Files:** `island/shell/*`, `LA:399+` (`RollingNumberText` crossfade path), `IV:4170-4218` (equalizer), `IV:832-872` (shakes, via `motion.keyframes`), `IV:1896-1932` (idle bounce, via `motion.impulse`).
- **Acceptance criteria:**
  - S2: a new REDUCED set of 5 states; settled frames identical to FULL.
  - Device with "Remove animations": nothing bounces, content fades, and the island never looks broken (no frozen half-states).
  - Device in Battery Saver: the equalizer's frame rate is ≤ 30 fps (Perfetto).
- **Tests:** Robolectric render with tier REDUCED: no `renderEffect` set on any layer; the `RollingNumberText` crossfade path.
- **Depends on:** M5.1, M3.1, M2.4.
- **Effort:** S–M   **Risk:** low.
- **Ships as:** Accessibility + battery.

### M5.3 "Eager in, calm out": retune the non-user tokens — **Intended diff**
- **Goal:** Switch `IslandMotion.profile` to `Droid` (F3.1 table) for the tokens the user can't edit: collapse (only while expand is at its default), dismiss, merge, press, interactive and effects. **The three user curves and `animationSpeed` are untouched.**
- **Why:** PLAN §2 / F3 and 03 §5: a small bounce on the way in, critically damped and about 15 % quicker on the way out.
- **Files:** `island/motion/IslandMotion.kt`.
- **Steps:**
  1. Flip the profile.
  2. Compare settle times at 0.7× in the PR: the collapse settle is shorter, so F5.3's shrink-on-settle happens sooner.
  3. Keep `Legacy` selectable from a hidden debug setting for one release.
- **Acceptance criteria:**
  - S2 settled pixel-identical. Mid-frames are the intended diff.
  - Device check at 0.7× and 2.0×. The owner signs off on feel.
  - **The 03 §6 speed re-base ("new 1.0 ≈ today's 0.7×") is *not* done here.** It changes the meaning of the stored `animationSpeed`, so it belongs with D5's migration.
- **Tests:** `IslandMotionTest` for `Droid`.
- **Depends on:** M1.4, M3.1, M4.2.
- **Effort:** S   **Risk:** low (reversible).
- **Ships as:** Polish (signature motion).

---

## Execution order

Phase 0 (S1–S5) and the Phase 1 items named in **Assumed inputs** land first. Then:

```
Wave 1 (parallel, different files):
  F2.1 ratchet ─┐            F1.1 scene (pure) ──> F1.2 ──> F1.3
  F3.1 motion   ├─ no IV    F3.5 timings (SM)
  F3.3 haptics  │            F6.2 reports, F6.4 signing, F6.1 (after the owner answers D1)
  F4.4 slot ────┘ (small IV edit; serialize with F1.2)

Wave 2 (IV.kt: one at a time, in this order):
  F1.2 → F1.3 → F2.2 → F2.4 → F3.2 → F4.1 → M1.1 → M2.1 (shell extracted; IV pressure drops)
  In parallel with the IV chain (service files): F1.4 → F5.1 → F5.2 → F5.3 → F1.5

Wave 3 (shell files):
  M1.2 harness → M2.2 → M2.3 → M2.4 → M1.3 one body → M1.4 one surface → M2.5 → M2.6 → M2.7 gate
  Interleaved where they don't collide: F3.4 haptic retune, F3.6 timing retune, F2.3 color merge, F4.2 → F4.3 (presentations in IV)

Wave 4:
  M3.1 → M3.2 → M3.3 → M3.4
  M4.1 (pure; can start in Wave 2) → M4.2 → M4.3 → M4.4 → M4.5
  M5.1 (any time after F3.1) → M5.2 → M5.3
  M1.5 notch, F5.4 envelope trial, F6.3 baseline profile (after S4): any time in Wave 4
```

**Deliberate deviation from the PLAN's M1 → M2 numbering:** M1.1 and M2.1–M2.4 run *before* M1.3 and M1.4. The one-body change then happens once, inside the extracted shell with deferred reads, instead of twice: once in composition in `IV`, then again to defer it. The PLAN's intent ("M1 then M2") is kept at the level of outcomes.

**Unblocks:**
- After F1.3: Phase 7 A2.1 and A4.1.
- After F3.1: epic I (I1 can start on `impulse()` and `loop()`) and M5.1.
- After F2.2 and F4.1: epic V.
- After M1.4 + M3.1: D1, D2 and D4.
- After M4.4: C3.

**Fastest visible payoff:** M1.3 (one-body morph), then M4.2–M4.4 (gestures).

## Risk register

| # | Risk | Likelihood / impact | Mitigation | Owner task |
|---|---|---|---|---|
| R1 | "Pixel-identical" refactors silently change pixels (mis-copied color, spring rounding, a reordered modifier). | M / M | Byte-exact S2, including F3.2's mid-frame captures. Physical tokens store raw ζ/k, never round-tripped through d/b. | F2.2, F3.2, M1.1, M2.x |
| R2 | The one-body morph (M1.3/M1.4) looks wrong on some settings: tiny pills, large `expandedScale`, left cutout (`isLeftCutout` makes the compact window MATCH_PARENT, `OS:1013-1014`), dual mode. | M / H | The M1.2 continuity harness over the S1 settings matrix. Device check at 0.7× and 2.0×. Ship M1.3 behind a hidden debug toggle for one release if the owner wants an A/B. | M1.2–M1.4 |
| R3 | A 5-D geometry isn't possible (Compose `AnimationVector` is 1D–4D, sealed). X drifts out of sync with the body. | H / L | 4-D body plus X `Animatable` launched in the same coroutine with the same spec (M1.1). Test: they settle within one frame of each other. | M1.1 |
| R4 | Notch style can't be expressed with one corner value. | H / M | Notch stays on the two-body path until M1.5. If squircle-with-square-top ≠ `notchPillShape` at settle, keep `RoundedCornerShape` for notch compact and document the exception. | M1.5 |
| R5 | Window resize timing differs by OEM: grow-first latency, envelope-window touch routing. | M / H | F5.2's 100 ms fallback. F5.4 flag-gated. 2–3 OEM device matrix (Pixel, Samsung, Xiaomi). | F5.2, F5.4 |
| R6 | The F4.2 line-height change breaks fixed-height layouts or clips heroes on OEM fonts. | M / M | Intended-diff review per state. Hero roll clip test. One UI / MIUI at font scale 1.5. V4 (measured heights) follows. | F4.2 |
| R7 | Deferred reads change clip and placement semantics (`layout {}` vs `.width()`), or glass panes lag. | M / M | M2.2 is pixel-identical with mid-frames. M2.5 device check with glass on. | M2.2, M2.5 |
| R8 | Predictive back needs a focusable overlay. Taking focus can close the underlying app's IME, and OEM back routing to overlays varies. | H / M | `backCollapses` setting. Focusable only while expanded. API 29–32 key fallback. Documented trade-off. | M4.5 |
| R9 | Swipe-down-past-64-dp → shade needs the optional accessibility service, and may conflict with the system's own top-edge pull. | M / M | Fall back to Expand when the service is missing. Thresholds tunable in `GestureThresholds`. OEM check. | M4.4 |
| R10 | Behaviour changes hidden inside "refactors": F1.4 alters touchability (foreground app, call counting, `hideOnLockScreen`), F3.3 makes the flashlight respect the haptic toggle and drops the ringer waveform. | H / L | Each is listed as an intended diff in its task, and S3 row changes are enumerated in the PR. | F1.4, F3.3 |
| R11 | Stability config wrongly marks mutable models stable, so the UI shows stale data. | L / H | Verify that `MediaInfo` and `NotificationInfo` are never mutated after construction before listing them (F6.2). | F6.2 |
| R12 | Moving the D1 decision or the release signing breaks existing installs: dropping API 29–32 users; existing installs signed with the debug key can't update in place. | M / M | Owner decision (D1). Release notes. For signing, a one-time reinstall notice. | F6.1, F6.4 |
| R13 | Contention: nearly every task touches `IslandView.kt` until M2.1 lands. | H / M | Wave 2's strict serial chain. M2.1 early. Never two writing sessions on `IV`. | all |
| R14 | `animationSpeed` re-base (03 §6) and the curve editor's semantics drift if M5.3 is mistaken for D5. | M / L | M5.3 explicitly excludes the user curves and the speed re-base. D5 owns the migration. | M5.3 |
