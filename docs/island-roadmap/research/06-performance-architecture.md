# Droidbubble performance, architecture and reliability audit

Scope: I read the working tree on `island-polish` and changed nothing. No Gradle runs, so the perf claims come from reading the code. Confirm the frame-level ones with Perfetto or a macrobenchmark before and after each fix.

**Facts that correct the brief**
- `minSdk = 29` in `app/build.gradle.kts`, but the README says Android 13+.
- Strong skipping is on by default (Kotlin 2.3.20).
- There is no stability config, no app baseline profile and no macrobenchmark module.

## 1. Current architecture map

**Data flow:**
system sources → services (main thread) → `IslandStateManager` (a global object holding about 25 StateFlows) → `IslandOverlayRoot` (one ~1950-line composable) plus `IslandOverlayService.updateWindowState` (the window size).

**Sources, all on the main looper:**
- `NotificationReceiverService`:
  - notification parsing: `onNotificationPosted` L859+
  - media sessions: `refreshMedia` L341, `updateMediaInfo` L482–679
  - screen-recording 1 s ticker: L844–855
- `IslandAccessibilityService`: foreground package; it calls `refreshMedia()` on every window change (`IslandAccessibilityService.kt:37`).
- `IslandOverlayService`: broadcast receivers for charging, ringer, Bluetooth and unlock (L280–361), the torch callback (L363–448) and the Wi-Fi callback (L252–278).
- `PersistentWidgetTelemetryManager`: network speed at 1 Hz on the IO dispatcher (L162–198) and weather every 20 minutes. Both pause when the screen is off. This part is well done.

**State:** `IslandStateManager.kt:33–771`.
- About 25 independent `MutableStateFlow`s.
- Derived values are plain getters that nothing can observe: `isOccupied` L322, `currentPriority` L326.
- Global callback variables are wired by whichever service set them last: L178–198, L271, L283–284.
- Glass-pane rects are pushed every frame through `glassPaneListener` (L251–259).

**Overlay window** (`IslandOverlayService.kt`):
- One `ComposeView` with a hand-made always-RESUMED lifecycle owner (L464–467, L1210).
- Added as `TYPE_ACCESSIBILITY_OVERLAY` or `TYPE_APPLICATION_OVERLAY` with `NOT_FOCUSABLE | NOT_TOUCH_MODAL | LAYOUT_IN_SCREEN | NO_LIMITS`, TRANSLUCENT, at top centre (L606–671). It is hardware-accelerated through `WindowManagerGlobal`'s app default.
- **It is not resized every frame.** It is resized in steps when state changes (L899–1057):
  - Compact: (widthDp+80+2·|hOffset|) × (topOffset+heightDp+4) dp, touchable (L982–999).
  - Expanded: MATCH_PARENT × MATCH_PARENT so a tap outside collapses it (L928–937).
  - Idle: 1×1 and `FLAG_NOT_TOUCHABLE`.
  - Collapsing: stays MATCH_PARENT until `collapseSettled` fires, with a 1.5 s safety timeout (L1021–1034).
- Touch pass-through relies only on window size plus `FLAG_NOT_TOUCHABLE`. `IslandTouchManager.kt` (a reflection-based touchable region) is dead code: it is declared at service L573 and never attached.
- The refresh-rate boost re-sends the layout with `preferredRefreshRate` for 1.2 s divided by animation speed (L846–867).
- `GlassPaneWindows.kt`: two extra Dialog windows, each on its own `URGENT_DISPLAY` HandlerThread (L63). They are relayouted on **every frame** of a morph to follow the cards.

**Rendering** (`IslandView.kt`):
- `IslandOverlayRoot` L389–2343 does everything in one function:
  - collects 22 flows (L393–414)
  - derives scene and priority (L446–567)
  - builds about 25 `animate*AsState` (L687–1051)
  - lays out cards (L1266–1652), the pill (L1656–2268) and the parentheses (L2270–2340)
- Per-activity views: L2654–4711.
- Software box blur and accent-colour extraction: L228–378.
- Helpers live in `IslandComponents.kt`, `LiveActivities.kt` and `IslandShapes.kt`.
- `IslandTokens.kt` is a global `object`, only partly used: `IslandView.kt` still has 75 `Color(0x…)` literals, 40 inline `spring`/`tween` calls and 36 `.sp` literals.

**Settings:** the DataStore flow is collected separately by the overlay's `collectAsState` (service L681), the service job (L806), `NotificationReceiverService` and `MainScreen`. The mapping code is duplicated (`SettingsRepository.kt` L69–131 and L133–195).

## 2. Findings

### P0

**P0-1. The whole island recomposes on every animation frame.**
- `IslandOverlayRoot` reads all flows and all animated values during composition.
- It applies them through layout modifiers: `.width(animatedCard1Width).height(...)` at L1313–1314 and `.width(animatedCompactWidth)` at L1827.
- It also branches on them: L1229, L1250, L1267, L1308, L1675, L1823.
- `rememberMorphSpeed` (`IslandComponents.kt:623–638`) explicitly depends on per-frame recomposition and uses `System.nanoTime()` at composition time, not frame time.
- `SideEffect { publishGlass() }` (L1149) and `onGloballyPositioned { publishGlass() }` (L1315, L1497) run on every frame.
- Result: each frame re-executes about 1950 lines, creates new `islandShape(...)` instances, gradient `Brush` objects and `graphicsLayer` lambdas, and re-measures the pill content (`fillMaxSize` inside an animated width, L2211–2264).
- 1 Hz flows (network speed, recording seconds at NRS L850) also recompose everything.
- At 120 Hz there are only 8.3 ms per frame, so this is the main smoothness risk.

**P0-2. Main-thread parsing lands in the same frame as the pop-in animation.** `NotificationListenerService` callbacks run on the main looper. They do:
- RemoteViews inflation, `views.apply(...)` (NRS L1238–1266). For clock and recorder apps this runs on every repost, about once per second (L971–976).
- `Icon.loadDrawable` (L1032–1034, L1166, L1192–1195).
- A full-resolution `BitmapFactory.decodeStream` of the art URI with no sampling, which is also an OOM risk (L562–566).
- The `activeNotifications` binder call, which unparcels every notification including bitmaps, on every `updateMediaInfo` (L532, also L407).
- `refreshMedia` itself runs on every playback-state change, metadata change and app switch.

The island's pop-in, recomposition and window relayout start in that same main-thread slot.

**P0-3. The compact equalizer keeps the display at high refresh rates while music plays.**
- `EqualizerVisualizer` (L4170–4218) requests a frame on every vsync for as long as music plays. Compact media is the most common resting state.
- Visualizer data only arrives at 20 Hz or less (`AudioLevels.kt:138`), so most of those frames show nothing new.
- On adaptive displays this blocks the drop to idle refresh. The telemetry manager's own comment (L59–60) describes this exact effect.
- The pattern is right otherwise: state is read in the draw phase.

**P0-4 (bug). An invisible touch-blocking rectangle sits over the foreground app.**
- The composable hides cards whose app is in the foreground (L460–465) and then draws nothing (L1229).
- The service's `isOccupied` (L944–952) ignores the foreground package, and its `combine` (L751–771) doesn't even watch `foregroundPackage`.
- So with Spotify open and playing, a touchable, transparent strip of about (widthDp+80) × 52 dp at the top centre swallows taps and shade pulls.
- More generally, the compact window eats 40 dp on each side of the pill, from y=0 down (L993–994).
- Confirm on a device: tap the top centre of the music app while it's playing.

### P1

**P1-1. Window geometry is computed in three places and they disagree.** `initOverlay` L611–641, `updateWindowState` L939–980 and the composable L893–1030. They already drift: `initOverlay` ignores the `show*` toggles and the unlock/ringer widths.

**P1-2. Expand animations can start before the window has grown.**
- On expand, MATCH_PARENT is applied asynchronously (the authors' own trace puts a WM relayout at about 28 ms, see `GlassPaneWindows.kt` header) while the animation starts on the next frame. The first 2–3 frames at 120 Hz are probably clipped to the compact window.
- Each state change sends two `updateViewLayout` calls back to back: the boost, then the resize (L783–785).

**P1-3. Glass panes are always created, even with the feature off.**
- L730–731 creates them unconditionally (the default is `liquidGlassCards=false`).
- That means 2 windows and 2 urgent-priority threads per session.
- `create()` blocks the main thread on a latch for up to 1.5 s (`GlassPaneWindows.kt:192`).

**P1-4 (security). The exported debug receiver is confirmed: any app can inject fake island cards.**

The functions agent's flag is correct.

How it's registered:
- `debugReceiver` (`IslandOverlayService.kt` L133–243) gets an `IntentFilter` with 11 `com.droidbubble.app.ACTION_*` actions (L479–491).
- On API 33+ it is registered with `Context.RECEIVER_EXPORTED` (L492–494). Below 33 it uses plain `registerReceiver` (L495), which is also exported by default.
- No permission argument, and no debug-build gate (`buildConfig = false`), so it ships in release.

What another installed app can do, with no permission:
- **Fake cards:** `ACTION_SIMULATE_NOTIFICATION` posts a card with any `title`, `appName` and `text` it chooses, plus button labels (L150–172), all drawn over whatever app is open. That's a phishing surface ("Bank: verify your account"). The fake buttons have `null` PendingIntents, so tapping them does nothing directly, but the text is fully under the attacker's control.
- **Spoofed media and progress:** `ACTION_SIMULATE_MEDIA` and `ACTION_SIMULATE_PROGRESS`.
- **Fake system alerts:** `ACTION_SIMULATE_CHARGING`, `_RINGER`, `_UNLOCK` and `_BLUETOOTH`.
- **Wiping real state:** `ACTION_RESET` clears the real media, notification, recording and flashlight state (L205–211).
- **Blocking the screen:** `ACTION_EXPAND` turns the window full screen (MATCH_PARENT, touchable) and eats taps. Sent in a loop it acts as a denial of service.

Fix:
- Register it only in debug builds (or move these commands to a debug-only source set) and use `RECEIVER_NOT_EXPORTED`.
- If test tooling needs it in release-like builds, protect it with a custom `signature`-level permission.
- The `powerReceiver` only needs protected system broadcasts, so it can also be `RECEIVER_NOT_EXPORTED`.

**P1-5. Every touch recomposes the root.**
- The root `pointerInput` writes `isTouching` and `lastTouchAt` on every pointer event (L1238–1245).
- `lastTouchAt` is a `LaunchedEffect` key (L1221).
- Dragging the media or flashlight slider therefore recomposes and restarts at the input rate.

**P1-6. Continuous recomposition in the expanded media card and the recording dot.**
- `ExpandedMediaView` has a 250 ms ticker plus an endless 260 ms tween (L3304–3334), so the whole card recomposes every frame while expanded and playing.
- `PulsingRecordingDot` reads an infinite transition during composition through `.scale` and `.background` (L4143–4161).

**P1-7. Crashes aren't contained.**
- `CrashLog` only saves the last stack trace (`CrashLog.kt:21`).
- `onNotificationPosted` has no guard around each notification.
- A Compose exception kills the process; `START_STICKY` then restarts it, and the same notification can crash it again in a loop.
- There is no safe mode.

**P1-8. State has no single owner.**
- `IslandStateManager` is a process singleton: main-thread-only by convention, not enforced.
- Its derived state can't be observed, and its callbacks are global variables.
- The composable re-derives occupancy and priority with different rules from the getters (`currentPriority` vs `featurePriorities` at L500–516).

**P1-9. The service blocks the main thread at startup.** `runBlocking(Dispatchers.IO)` reads DataStore inside `Service.onCreate` (L455–461).

### P2

- **Shape cost:** `SquircleShape` always returns `Outline.Generic`, a path clip, even for capsules where the smoothing collapses to 0 (`IslandShapes.kt:31–104`). Clip and border each rebuild the path, and the shape instance is new every frame.
- **Bitmap work during composition:**
  - box blur at L451–458
  - accent-colour extraction at L450, L2930 and L3339
- **Settings writes per slider tick:** every tick writes DataStore, then recomposes the overlay and relayouts the window (`MainScreen.kt:1856–1908`).
- **Lifecycle never pauses:** the overlay stays RESUMED with the screen off or the island hidden.
- **Hidden API:** `disableMoveAnimation` writes the hidden `privateFlags` by reflection (service L575–581). The public `setCanPlayMoveAnimation` exists on API 34+, and `GlassPaneWindows` already uses it.
- **Dead code:** `onBoundsChange` is unused (L391, service L717).
- **Build and release:**
  - release builds are signed with the debug key
  - material3 is an alpha (`1.5.0-alpha04`)
  - no baseline profile and no Compose compiler metrics
  - `minSdk 29` keeps fallbacks alive for RenderEffect (31), AGSL (33) and anti-aliased path clipping (33)
- **Fabricated data:** weather shows synthetic data on failure, including a fake hourly forecast (telemetry L290–296, L321–338). This affects user trust; it belongs to the functions workstream.
- **Boot:** `BootReceiver` launches a coroutine without `goAsync()`.

**What's already done well and should become house patterns:**
- `RollingNumberText` and `LiveWaveform` read state in the draw phase (`LiveActivities.kt:399–618`).
- Card content is laid out once at its final size (L1455–1456).
- `steppedBlurEffect` caches its effects (`IslandComponents.kt:605–614`).
- Telemetry pauses when the screen is off.
- The Robolectric harness renders and draws the real root (`IslandOverlayCrashTest.kt:104–134`).

## 3. Recommendations

| # | What / how | Files | Effort | Risk | Unblocks |
|---|---|---|---|---|---|
| R1 | **Single scene model.** Extract a pure `deriveIslandScene(state, settings, env)` returning primary/secondary, mode (hidden/minimal/compact/alert/expanded) and target geometry in dp. Lift it verbatim from L446–567 and L893–1030. The composable and the window controller both consume it. That fixes P0-4 and P1-1. | IslandView, IslandOverlayService | M | Low (pure function, table-tested) | functions, UX, motion |
| R2 | **Deferred-read shell.** Move the morph into an `IslandShell` that owns `Animatable`s, including a 4D `Animatable<CardMorph>`. Read them only inside `Modifier.layout {}`, `graphicsLayer {}` and `drawBehind {}`. Replace `rememberMorphSpeed` with `Animatable.velocityVector`, which is exact and frame-timed. Publish glass rects from a single `onPlaced`/`onGloballyPositioned` callback, without `SideEffect`. Lay the pill content out at its final width and clip it, the same way the cards already work. | new `island/shell/*`, IslandComponents | L | Medium (behaviour-preserving, needs the Phase 0 tests) | **motion (prerequisite)**, visual |
| R3 | **Narrow state reads.** Pass state holders or lambdas instead of values: `() -> NetworkSpeedInfo`, and recording seconds read only in `RollingNumberText`. Collect each flow at the leaf that renders it. Use `collectAsStateWithLifecycle` with the overlay lifecycle driven by screen on/off and visibility. | IslandView, service lifecycle | M | Low | battery |
| R4 | **Off-main parsing.** A single-thread `NotificationParser` dispatcher handles RemoteViews, drawables, URI decode with `inSampleSize` capped at about 256 px, accent colour and blur. Results are posted to the store. Cache by notification key and post time. Drop `activeNotifications` from the media path; use the posted notification for that package, kept in a map updated by `onNotificationPosted`. Debounce `refreshMedia` (about 100 ms, and skip it when the token and state are unchanged). | NotificationReceiverService, IslandAccessibilityService | M | Medium (ordering: parse results must apply in post order, so use a sequence number) | functions, visual (accent and blur available free) |
| R5 | **Frame budget for ambient motion.** Throttle the equalizer to the data rate (≤30 fps): wait on a frame only once 33 ms have passed and the data has changed, and skip entirely when the bands haven't moved. Move `PulsingRecordingDot` to `graphicsLayer`. Drive the media slider from `drawBehind` with the playback clock (no 250 ms ticker, no endless tween). Investigate a frame-rate vote that doesn't relayout the window (View requested frame rate, API 35); otherwise fold the boost into the same `updateViewLayout` as the resize. | IslandView L4143–4218, L3304–3334; service L846 | S–M | Low | battery, motion |
| R6 | **Window controller.** Add `OverlayWindowController(spec: WindowSpec)`, with `WindowSpec` derived from the scene (R1). Expand: resize to the expanded envelope, wait one `OnPreDraw`, then start the animation. Collapse: keep the current animate-then-shrink. Consider an envelope-sized window plus `FLAG_WATCH_OUTSIDE_TOUCH` instead of MATCH_PARENT (less fill and compositing, and apps stay touchable). Optionally revive `IslandTouchManager` behind a runtime capability probe, with fallback. Tighten the compact hit area to the pill and bubble. | service, IslandTouchManager | M | Medium (OEM quirks, so feature-flag it) | motion (no clipped first frames), UX |
| R7 | **Security and lifecycle.** Debug receiver to debug builds only and `RECEIVER_NOT_EXPORTED` (enable `buildConfig`); see P1-4. Create glass panes lazily, only when the setting is on, and never block the main thread. Replace `runBlocking` with "start with the defaults, apply settings when they arrive". | service, GlassPaneWindows | S | Low | — |
| R8 | **Crash containment.** Guard each notification and each adapter with `runCatching` plus a breadcrumb ring buffer in `CrashLog`. Add a crash-loop guard: N crashes in M minutes boots into safe mode with the last-active activity type disabled. Add an error boundary per presentation: fall back to a generic pill if an adapter throws during its data mapping (pre-compute in the adapter, since Compose can't catch errors during composition). | CrashLog, NRS, service | S–M | Low | functions |
| R9 | **Themeable design system, as the user asked.** `IslandTheme` provides `LocalIslandColors`, `LocalIslandTypography`, `LocalIslandShapes` (squircle, rounded or user corner radius) and `LocalIslandMotion`, a factory that builds springs from the user's curves: `expand*`, `appear*`, `split*`, `animationSpeed`. `IslandStyle` presets (pill, notch, future ones) map to a `ShellGeometry` strategy plus a token set. The current `IslandTokens` values become the *default Droidbubble* preset, not hard-coded constants, so iOS stays a quality reference rather than the only look. Rule: no literal colour, sp or spring in presentation code; a lint/grep check in CI. | new `island/design/*`, IslandTokens, IslandShapes | M (incremental) | Low | **visual and typography (prerequisite)**, motion |
| R10 | **Shape fast path.** `SquircleShape` returns `Outline.Rounded` when the smoothing collapses (capsules and circles). Remember shape instances per radius bucket. | IslandShapes | S | Low (pixel-identical for capsules) | visual, motion |
| R11 | **Settings.** One mapper function; `distinctUntilChanged`; a debounced editor for sliders (in-memory preview state, written about 250 ms after drag end); one shared `StateFlow<IslandSettings>` per process. | SettingsRepository, MainScreen | S | Low | UX |
| R12 | **Build and release.** `baselineprofile` and `macrobenchmark` modules (`FrameTimingMetric` on expand/collapse, triggered from a debug/benchmark-only command). Compose compiler reports. Release signing key. Raise `minSdk` to 33 to match the README and delete the legacy branches. Plan a move off material3 alpha in the overlay: only `MaterialExpressiveTheme` needs it. | gradle | M | Low | all |

## 4. Target architecture and refactor sequence

```
island/
  state/        IslandStore (single StateFlow<IslandState>, main-confined, intents), IslandScene.kt (pure derive)
  activities/   ActivityAdapter { kind; priority; isActive(state); compact(); minimal(); expanded(); measureExpanded(); onDismiss() }
                media/, notification/, live/ (call, timer, recorder, tracking, score), progress/, system/ (charging, unlock, bt, ringer), flashlight/, widgets/
  shell/        IslandShell (Animatables, deferred reads), CardStack, Pill, SplitNeck, Parentheses, GlassPublisher
  presentations/  per-activity Compact/Minimal/Expanded composables (moved from IslandView L2654–4711, LiveActivities)
  design/       IslandTheme, Colors/Type/Shapes/Motion tokens, IslandStyle presets, SquircleShape
overlay/        IslandOverlayService (thin), OverlayWindowController, WindowSpec, GlassPaneWindows, OverlayLifecycle, SystemEventReceivers, DebugCommands (debug only)
sources/        NotificationParser (off-main), MediaSessionTracker, Telemetry, Torch
```

Sources (background) → `IslandStore` (single writer) → `IslandScene` → both the `OverlayWindowController` and `IslandShell`. Actions go back through an `IslandActions` interface instead of the global callback variables. Adding a new activity type means writing one `ActivityAdapter` plus its presentations; the root no longer needs editing.

**Phase 0 – safety net (do this first, about 2–3 days):**
- Extend the Robolectric harness with characterization tests for a state × settings matrix:
  - assert the bounds of `PILL`, `BUBBLE` and `CARD` at settle (`getBoundsInRoot`) and which content appears
  - include dual mode, notch, minimized and hidden, the foreground-app case, ringing call plus flash, and high contrast
- Add Roborazzi screenshots on Robolectric native graphics for about 15 key states. These are the visual and typography teams' diff baseline.
- Add a debug recomposition counter on the root, with a test asserting the count stays at or below N over a 500 ms expand. It starts as a recorded number and becomes a regression gate after R2.
- Extract the service window math into a pure function as-is and table-test it.
- Add the macrobenchmark for expand/collapse frame timings.

**Phase 1 – safe fixes with no visual change:**
- P0-4 via a temporary foreground check in the service
- R7, including the P1-4 debug-receiver fix
- R5, R10
- the root-touch fix (P1-5)
- R4 parsing off the main thread
- R11

**Phase 2 – R1 scene model:** service and composable switch to `deriveIslandScene`; the Phase 0 bounds tests must pass unchanged.

**Phase 3 – R2 shell plus R3 narrow reads.** **This must land before any motion work**: otherwise new curves, morphs or ambient animation multiply the per-frame recomposition cost, and `rememberMorphSpeed` blocks velocity-based effects.

**Phase 4 – R9 design tokens and style presets.** **This must land before visual and typography work**: otherwise they'll edit about 150 literals in place inside a 4711-line file, and the user-chosen-style requirement can't be met. Can run in parallel with Phase 3: migrate each file's literals as it moves.

**Phase 5 – split presentations and adapters:**
- Move per-activity composables out of `IslandView.kt` file by file. Each move is mechanical, followed by a screenshot diff.
- Introduce the `ActivityAdapter` registry.
- Split `IslandStateManager` into store plus per-domain reducers; existing `IslandStateBehaviorTest` keeps passing through a facade.

The functions workstream can start new activity types once Phase 2 exists, ideally targeting the adapter interface.

**Phase 6 – window upgrades (R6) and crash containment (R8), behind flags.** Validate on 2–3 OEMs: Samsung, Xiaomi, Pixel.

Every phase should keep the app shippable: run the full Robolectric suite and the screenshots, and compare benchmark frame timings against the Phase 0 baseline.
