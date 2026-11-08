# Phase 4 + Phase 6: Layout polish (V) and signature motion (D)

**Goal.** Make the island sit precisely and move like one living thing, **without changing what it is**. Phase 4 (V1–V7) is about precision: the pill hugs the real camera, inner corners are concentric with the card, the compact pill is balanced and only as wide as its content, cards are as tall as their content, colors read clearly, edges are clean and icons come from one family. Phase 6 (D1–D6) adds the signature motion on top of the Phase 3 one-body shell: a liquid split and merge, alert bumps, rolling numbers everywhere, shared artwork, an honest curve editor and an optional orbit ring.

**Owner direction (PLAN v2 §0), restated as hard rules for every task here:**
- Keep `SquircleShape` at smoothing **0.6** (`island/IslandShapes.kt:31`). Never change the smoothing.
- Keep the black cards, each card's layout structure, the current palette (only duplicates merge) and the settings app's structure.
- No new palette, no bundled font, no media card or HUD redesign, and "Liquid glass" keeps its name.
- Apple is a quality reference only. Each task takes a *principle* and implements it the Android way.
- **Dynamic icons belong to epic I (separate agent).** When a task needs an icon effect, it names the I1 effect or the I2 morph and stops there.

**Assumed inputs.** These are delivered by Phases 0–3 and referenced by ID:

| ID | What this phase uses from it |
|---|---|
| S2 | Roborazzi screenshot baseline: the diff gate for every task here |
| S3 | Window-geometry math as a pure function (`WindowSpec`-style), table-tested |
| B5 | High-contrast outline and text tokens fixed |
| B9 | `tnum` escapes fixed; bubble numeral ≥ 11 sp |
| B10 | Off-main sampled art decode |
| F1 | `deriveIslandScene(...)` |
| F2 | `IslandTokens` consolidated: dp, hex, sp and spring literals moved; duplicate colors merged; the CI grep that bans new literals |
| F3 | `IslandMotion` tokens (duration and bounce → `spring(1−b, (2π/d)²·speed²)`), `IslandHaptics`, `IslandTimings` |
| F4 | `IslandType` roles (`compactValue`, `compactLabel`, `compactUnit`, `bubbleValue`, `hero`, `title`…) and `NumericSlot(text, style, template)` |
| F5 | Window controller: grow before animating, shrink on settle |
| M1 | One body: a single `Animatable<IslandGeometry>` for the pill and card |
| M2 | Deferred reads (layout, graphicsLayer and draw phase); `geometry.velocity` |
| M3 | Content progress `p` as a pure function of geometry; `AnimatedContent` keyed per activity |
| M4 | `Modifier.islandGestures` |
| M5 | Motion tiers Full / Efficient / Reduced (`LocalMotionTier`) |
| I1, I2, I3 | `DynamicIcon` effect engine, morphing state icons, event wiring (separate agent) |
| C2, C5.1 | Live preview while tuning dimensions; optional `MainScreen.kt` file split |

**Name collisions resolved in this file.** The research uses `IslandGeometry` for three different things. Here:
- `IslandGeometry` means **only** the M1 animated shape (center x, top, width, height, corner).
- `CameraGeometry` and `CutoutCalibration` (V1) describe the hardware camera. Phase 8's C2.4 already uses the name `CutoutCalibration`.
- `CardGeometry` (V2) holds the margin and radius system and is exposed as `LocalCardGeometry`.
- "Decision D1" means PLAN §3's minSdk decision. "Epic D1" means the liquid shader.

## Verified code anchors

Line numbers are for the working tree as checked out on 2026-11-08: branch **`main`** at `8d33466`, with a staged, uncommitted working tree. PLAN v2 names `island-polish` at `77d266e` as the baseline, but that commit isn't an ancestor of `HEAD`; see the conflicts list at the end. **Re-verify each anchor before editing.** Phase 1–3 tasks will move most of them.

| Anchor | Location |
|---|---|
| `SquircleShape` (smoothing 0.6, capsule fade at `:43-46`) | `island/IslandShapes.kt:30-104` |
| `islandShape()` (API 33+ squircle, else `RoundedCornerShape`) | `island/IslandShapes.kt:111-112` |
| `expandedCornerRadiusFor` (unused) | `island/IslandShapes.kt:118` |
| `notchPillShape` (circular corners) | `island/IslandShapes.kt:140-145` |
| Tokens: spacing `:25-30`, radii `:33-36` (`RadiusCard` unused), `SurfaceSunken` `:42`, accents `:52-57`, `HighContrastOutline` `:66`, type `:69-73` | `island/IslandTokens.kt` |
| Settings: `widthDp`, `heightDp`, `cornerRadiusDp`, `topOffsetDp`, `horizontalOffsetDp` `:17-21`; `isLeftCutout`, `isTeardropCutout` `:24-25`; outline `:26-28`; `expandedScale` `:29`; `animationSpeed` `:46`; curves `:63-68` | `model/IslandSettings.kt` |
| Defaults and persisted keys (two mapping blocks) | `data/SettingsRepository.kt:21-74`, `:85-134`, `:184-206`, `:246-269` |
| `extractMediaAccentColor` (RGB average of 16×16) | `island/IslandView.kt:329-378` |
| Outline colors (15% alpha; recording red `FF3B30`) | `island/IslandView.kt:541-567` |
| `speed`, `speedSq`, `collapseStiffness = expandStiffness·1.4·speed²` | `island/IslandView.kt:569-573` |
| `swapSquash` (110 ms tween, then spring) | `island/IslandView.kt:578-588`; used at `:1815`, `:1834-1835`, `:2245` |
| Press springs (not speed-scaled) | `island/IslandView.kt:590-604` |
| Spec selection: "Appear" curve only for charging, unlock, Bluetooth and ringer | `island/IslandView.kt:607-657` |
| Compact pill width spec: the **split** curve drives appear, hide and minimise | `island/IslandView.kt:675-685` |
| Alert pill widths `widthDp+20`, `×0.82`, `×0.95` | `island/IslandView.kt:893-895` |
| `targetCompactWidth` (fixed `widthDp`) | `island/IslandView.kt:900-911` |
| Card width `340.dp × scale`, capped at screen − 16 dp | `island/IslandView.kt:916-917` |
| `card1ContentHeight` via `cardHeightFor` | `island/IslandView.kt:947` |
| Corner target: `liveActivityCornerRadius` expanded, `cornerRadiusDp` collapsed | `island/IslandView.kt:957-961` |
| Bubble: `bubbleSize = heightDp`, `compactMainHalf = widthDp/2`, gap 6 dp | `island/IslandView.kt:981-984` |
| Bubble spring (split curve out, 0.85/380 back) | `island/IslandView.kt:1004-1014` |
| Expanded top = `topOffsetDp + 52` | `island/IslandView.kt:1024-1028` |
| `CardMorph` animate + `rememberMorphSpeed` | `island/IslandView.kt:1036-1056` |
| Glass card brush (bottom alpha 0.3 with blur) | `island/IslandView.kt:1073-1082` |
| `cardContainerAlpha` | `island/IslandView.kt:1084` |
| Card 1 shell: stretch `:1316-1328`, clip and border `:1329-1339`, ambient tints `:1399-1437`, content alpha `:1446-1450` | `island/IslandView.kt` |
| Bubble box (`CircleShape`, fixed `bubbleSize`) | `island/IslandView.kt:1675-1756` |
| Liquid neck Canvas (snap gap 5 dp) | `island/IslandView.kt:1758-1796` |
| Notch bridge and pill shape | `island/IslandView.kt:1798-1820` |
| Compact pill box | `island/IslandView.kt:1822-1952` |
| Charging pill content (`13.sp` literals; W at `:2011`, % at `:2030`) | `island/IslandView.kt:1986-2040` |
| `Spacer(36.dp)` camera gaps | `island/IslandView.kt:2022, 2060, 2105, 2173, 2867` |
| Compact swap `AnimatedContent`; `horizontalInset = (H−20)/2` clamped 6–14 dp | `island/IslandView.kt:2211-2266` (inset at `:2261`) |
| Parenthesis brackets | `island/IslandView.kt:2270-2340` |
| `AuxiliaryBubbleContent` (ring `:2525-2531`, 8 sp numeral `:2532-2537`) | `island/IslandView.kt:2462-2615` |
| `CompactContactAvatar` (even ring) | `island/IslandView.kt:2628-2651` |
| `CompactContent`: leading `:2677-2863`, gap `:2867`, trailing `:2870+`; flashlight trailing empty `:2886`; progress % `:2917-2924`; FPS `:2940` | `island/IslandView.kt:2654` |
| Compact media art `RoundedCornerShape(5.dp)` | `island/IslandView.kt:2782` |
| `ExpandedMediaView`: time labels `:3383-3397`, sizes `:3410-3422`, `SpaceBetween` `:3428`, art radius `:3445` | `island/IslandView.kt:3282` |
| Notification card inner radii | `island/IslandView.kt:3842, 3862` |
| Progress tile `:4008`; progress spring `:3977-3980` | `island/IslandView.kt` |
| FPS, weather, network tiles `:4285, 4402, 4534`; panels `:4437, 4562, 4593`; `TextStyle` escapes `:4579, 4585, 4610, 4616` | `island/IslandView.kt` |
| `MediaArtBackdrop` (0.75 art, 0.38 scrim, no top scrim) | `island/IslandView.kt:4663-4711` |
| `IslandHeader` | `island/IslandComponents.kt:93-136` |
| `cardHeightFor` fixed table | `island/IslandComponents.kt:211-230` |
| `islandCardContentPadding` (20·s / 16·s; 20/18/12·s) | `island/IslandComponents.kt:237-249` |
| `NotificationCardMetrics` | `island/IslandComponents.kt:255-275` |
| `measureProgressCardHeight` / `measureNotificationCardHeight` | `island/IslandComponents.kt:313`, `:342` |
| `IslandActionButtonRow` (`RadiusButton` 14 dp outer, 4 dp inner) | `island/IslandComponents.kt:449-505` |
| `FlippingCoverArt` | `island/IslandComponents.kt:515-569` |
| `CardMorph` 4-D | `island/IslandComponents.kt:572-583` |
| `steppedBlurEffect` | `island/IslandComponents.kt:605-614` |
| `MorphSpeedMeter` | `island/IslandComponents.kt:629-663` |
| `LiveActivityColors` (`RecordingRed FF5A47`, `CallGreen 34C759`, `AlarmYellow FFCC00`) | `island/LiveActivities.kt:285-313` |
| `LiveLayout` (margin, control, `cornerRadius = control/2 + margin`) | `island/LiveActivities.kt:328-358` |
| `RollingNumberText` (draw-phase roll, `tnum`) | `island/LiveActivities.kt:398-492` |
| `liveActivityCardHeight` / `liveActivityCornerRadius` | `island/LiveActivities.kt:906`, `:912` |
| `TimerRingIcon` (already animated sweep) | `island/LiveActivities.kt:1227-1260` |
| `LiveActivityCompactIcon`, `liveCompactText` | `island/LiveActivities.kt:1262`, `:1304` |
| Overlay window flags and initial bounds (`totalWidthDp`) | `service/IslandOverlayService.kt:616-686` |
| `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` | `service/IslandOverlayService.kt:681` |
| High-contrast settings override (`outlineColorArgb = 0x8CFFFFFF`, which the island never reads) | `service/IslandOverlayService.kt:702-712` |
| `MotionScheme.expressive()`; `LocalTextStyle` and `Density` overrides | `service/IslandOverlayService.kt:714-728` |
| `updateWindowState` compact bounds (`widthDp + 80`; dual `widthDp/2 + 8 + H + 40`) | `service/IslandOverlayService.kt:916-1064` (`:986-1003`) |
| Glass panes (`GradientDrawable` circular corners) | `service/GlassPaneWindows.kt:43-160` (`:126-150`) |
| Dimensions sliders (Width 50–260, Height 12–56, Top 0–60, Horizontal ±50) | `ui/main/MainScreen.kt:1866-1951` |
| Curve editor: targets `:2654`, defaults `:2656`, preview `:2740-2777` (no speed²), Bounce % `:2779-2786`, Snap `:2787-2793`, Bold↔Medium label `:2729` | `ui/main/MainScreen.kt:2649-2796` |
| `springStep` | `ui/main/MainScreen.kt:2638-2647` |
| Speed formatter (`%.1f MB/s`, `KB/s`) | `service/PersistentWidgetTelemetryManager.kt:250-254` |
| `minSdk = 29` | `app/build.gradle.kts:16` |

## Conventions for every task in this phase

1. **The S2 screenshot diff gate is how polish is judged.** Refactor steps must be pixel-identical. Polish steps produce *intended* diffs that are listed in the PR and approved at a checkpoint (see "Design-review checkpoints").
2. **No literals.** New dp, sp, color and spring values go into `IslandTokens`, `CardGeometry` or `IslandMotion`, so the F2 CI grep stays green.
3. **Motion goes through `IslandMotion` and honors M5.** The Reduced tier always has a path: no bounce, no squash, no shader, 150 ms cross-fades.
4. **Haptics go through `IslandHaptics`** (the toggle, SDK guards and throttling all live there).
5. **API levels.** `minSdk = 29`. AGSL `RuntimeShader` needs API 33; `DisplayCutout.getCutoutPath()` needs 31; `WindowManager.getCurrentWindowMetrics()` needs 30; `VibrationEffect.Composition` primitives need 31. Guard each with `Build.VERSION.SDK_INT`. If decision D1 raises minSdk to 33, drop the guards.
6. **Tests** live in `app/src/test/java/com/droidbubble/app/island/` (layout and motion) and `.../ui/main/` (editor). Use pure JUnit for math, and Robolectric `@Config(sdk = [35])` plus Roborazzi for rendering, as in `IslandOverlayCrashTest.kt`.
7. **`IslandView.kt` contention.** Almost every task here edits it, as do Phase 7's A1.5–A1.8 and Phase 8's C3. Never run two writing sessions on it at once. If the 06 Phase-5 presentation file split has landed, edit the split files instead and re-point the anchors in the PR.
8. **What "Design check" means.** The line in each task confirms three things: (a) the current look is preserved, meaning squircles at 0.6, the black cards, each card's layout structure and the current palette; (b) the change is precision or liveliness, not a restyle; (c) nothing is a near-literal copy of an Apple asset, layout, number or name.
9. **Definition of done** is PLAN §6.

---

## VD0: Enabler

### VD0.1: Owner checkpoint screenshot set
- **Goal:** one command produces the before/after image set for each design-review checkpoint (CP1–CP6).
- **Why:** this phase is judged visually by the owner. S2 covers about 15 states; the checkpoints need about 40, including large text and light and dark backdrops.
- **Files:**
  - New: `app/src/test/java/com/droidbubble/app/island/CheckpointScreens.kt`
  - Reuse: the S2 Roborazzi harness and `LiveActivityTester.Demo` (`island/LiveActivityTester.kt:23-36`)
- **Spec:**
  - States, each rendered at font scale 1.0 and 1.3:
    - **compact:** media, timer, call, recording, progress at 42%, flashlight, FPS, weather, network, charging, unlock, ringer, dual pill with bubble, minimised
    - **expanded:** media (with art backdrop on and off), notification with actions, progress, weather, FPS, network, flashlight, timer row, call row, alert
  - **Backdrops:** black, a mid-grey photo and a white photo, so keylines and scrims can be judged.
  - **Output:** `build/outputs/roborazzi/checkpoints/CP<n>/<state>@<fontScale>.png`. A small HTML contact sheet sets the images side by side against the recorded baseline.
- **Steps:**
  1. Add the state list as a table-driven test.
  2. Record the baseline on the commit before V1.1.
  3. Add a `-Pcheckpoint=CPn` filter.
- **Acceptance criteria:** the baseline set is recorded and committed under `app/src/test/snapshots/checkpoints/baseline/`. Running it twice with no code change produces zero diffs.
- **Tests:** the task is the test.
- **Depends on:** S2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Checkpoint screenshot set (test only)".
- **Design check:** test-only; no visual change.

---

## V1: Camera calibration from `DisplayCutout`

**Principle:** blend with the hardware. The island should be centred on and concentric with the real camera, not placed by hand-tuned sliders. Android-native: read `DisplayCutout`. Phones without a cutout keep today's defaults exactly.

### V1.1: `CutoutCalibration`: a pure reader and `CameraGeometry`
- **Goal:** a pure function turns cutout rects and screen metrics into a `CameraGeometry`. A thin Android adapter reads the real cutout. Nothing consumes it yet.
- **Why:** 02 R1 and gap 1 (P0). Nothing in the code reads cutout information today, and placement relies on `topOffsetDp = 12`, `widthDp = 180` and `heightDp = 36` (`model/IslandSettings.kt:17-21`).
- **Files:**
  - New: `island/layout/CameraGeometry.kt` (pure) and `service/CutoutReader.kt` (Android)
  - Read-only: `service/IslandOverlayService.kt:616-686`
- **Spec:**
  ```kotlin
  enum class CutoutKind { NONE, PUNCH_HOLE, WATERDROP, WIDE_NOTCH, UNKNOWN }
  enum class CameraSide { CENTER, START, END }

  @Immutable data class CameraGeometry(
      val kind: CutoutKind,
      val side: CameraSide,
      val centerXDp: Float,      // from the screen's left edge, current rotation
      val centerYDp: Float,      // from the screen's top edge
      val diameterDp: Float,     // camera width; 0 when NONE
      val rotation: Int,         // Surface.ROTATION_*
  ) {
      val isUsable get() = kind == CutoutKind.PUNCH_HOLE || kind == CutoutKind.WATERDROP
      companion object { val None = CameraGeometry(CutoutKind.NONE, CameraSide.CENTER, 0f, 0f, 0f, 0) }
  }

  object CutoutCalibration {
      /** Pure: rects in px in the current rotation; density and screen size from DisplayMetrics. */
      fun fromRects(top: RectF?, pathBounds: RectF?, screenWpx: Int, density: Float, rotation: Int): CameraGeometry
  }
  ```
- **Derivation rules:**
  - **Source rect:** use `pathBounds` when present (API 31+ `cutoutPath.computeBounds`), otherwise `boundingRectTop` (API 29+).
  - **Diameter.** Many OEMs extend the bounding rect up to y = 0, so trust the width and the bottom edge, not the top:
    - `diameterDp = rect.width / density`
    - `centerYDp = (rect.bottom / density) − diameterDp/2`
    - `centerXDp = rect.centerX / density`
  - **Kind** (aspect = width / height of the *corrected* box, i.e. height = diameter for holes):
    - `PUNCH_HOLE`: width ≤ 48 dp and the raw rect's aspect is 0.75–1.35, or its bottom-anchored box is square
    - `WATERDROP`: width ≤ 48 dp, raw aspect 1.35–2.2, top at y ≤ 1 dp
    - `WIDE_NOTCH`: width > 48 dp
    - `NONE`: no rect
    - `UNKNOWN`: anything else
  - **Side:** `|centerX − screenW/2| ≤ 0.12·screenW` gives CENTER; otherwise START or END, mirrored for RTL by the consumer, not here.
  - **Several cutouts:** pick the one closest to top-centre.
- **Android adapter (`CutoutReader`):**
  - On API 30+, read `windowManager.currentWindowMetrics.windowInsets.displayCutout`; on API 29, read `windowManager.defaultDisplay.cutout`.
  - On API 31+, read `cutoutPath?.computeBounds(rectF, true)`, which is deprecated but available on every level.
  - Wrap everything in `runCatching`. On failure, return `CameraGeometry.None`.
- **Steps:**
  1. Write the pure type and function.
  2. Write the reader.
  3. Expose `IslandStateManager.cameraGeometry: StateFlow<CameraGeometry>`, written by the service at `initOverlay` and in `onConfigurationChanged`, and by a `DisplayManager.DisplayListener.onDisplayChanged` for foldables.
- **Acceptance criteria:**
  - All fixture rects classify correctly.
  - On a Pixel-class phone, the logged geometry matches a screenshot of the camera within ±1 dp.
  - With no cutout, the flow is `None`.
- **Tests:** `CutoutCalibrationTest`, table-driven over these rects:
  - a centre punch hole with a top-extended rect
  - a centre punch hole with an exact rect
  - a top-left hole (Samsung A-series style)
  - a waterdrop
  - a wide notch
  - none
  - two cutouts
  - rotations 90 and 270, with the rect on the left edge
- **Depends on:** none (pure). It can start alongside Phase 2.
- **Effort/Risk:** S / low (pure, unused).
- **Ships as:** 1 PR, "Cutout calibration reader (no behaviour change)".
- **Design check:** no visual change. Reading the hardware is Android-native, not an Apple technique.

### V1.2: Settings become fine-tune deltas, with a migration that keeps every current pill where it is
- **Goal:** add `autoCalibrate` and delta fields, plus a one-time migration that keeps an existing user's pill exactly where it is today.
- **Why:** PLAN V1: "the sliders become fine-tune deltas". The owner likes the current look, so no existing user's pill may move on update.
- **Files:**
  - `model/IslandSettings.kt:15-72`
  - `data/SettingsRepository.kt` (keys `:21-74`; both mapping blocks `:85-134` and `:160-206`; writer `:220-270`)
  - New: `island/layout/EffectiveDimensions.kt`
- **Spec:**
  ```kotlin
  // IslandSettings additions
  val autoCalibrate: Boolean = true,
  val topOffsetDeltaDp: Int = 0,          // UI range ±12
  val horizontalOffsetDeltaDp: Int = 0,   // UI range ±24
  val heightDeltaDp: Int = 0,             // UI range ±8
  val calibrationMigrated: Boolean = false,

  data class EffectiveDimensions(val topDp: Float, val horizontalDp: Float, val heightDp: Float, val cameraZoneDp: Float)

  fun IslandSettings.effective(cam: CameraGeometry, screenWDp: Float): EffectiveDimensions
  ```
- **Formulas** (dp; `d` = `cam.diameterDp`):
  - `cameraZone = d + 2·6` for a punch hole, or `d + 2·4` for a waterdrop, then coerced to at least 24 dp. With no usable cutout, `cameraZone = 36`, which is today's `Spacer`.
  - `H_auto = max(36, d + 12)`. That gives a ring of at least 6 dp around the lens and keeps 36 dp on most phones. 02 R1's `d + 16` variant goes to CP1 as an alternative.
  - `top_auto = cam.centerYDp − H/2`, coerced to at least 0, so the pill is vertically centred on the lens.
  - `horizontal_auto = cam.centerXDp − screenWDp/2`.
  - When `autoCalibrate && cam.isUsable`:
    - `top = top_auto + topOffsetDeltaDp`
    - `horizontal = horizontal_auto + horizontalOffsetDeltaDp`
    - `height = H_auto + heightDeltaDp`
  - Otherwise, use the absolute fields `topOffsetDp`, `horizontalOffsetDp` and `heightDp` exactly as today.
  - `widthDp` stays absolute. After V3 it means "maximum width".
- **Migration.** It can't run in the repository, because it needs the display. It runs once in the service the first time a usable `CameraGeometry` arrives and `calibrationMigrated == false`:
  - **Fresh install** (no `TOP_OFFSET_DP`, `HEIGHT_DP` or `HORIZONTAL_OFFSET_DP` key stored): deltas are 0 and `autoCalibrate` is true.
  - **Existing install:**
    - `deltaTop = topOffsetDp − top_auto`, `deltaH = heightDp − H_auto`, `deltaX = horizontalOffsetDp − horizontal_auto`, each rounded.
    - If any delta falls outside its UI range, set `autoCalibrate = false`. The user then keeps the absolute values, and nothing moves.
  - In both cases set `calibrationMigrated = true`.
  - Keep the absolute fields; they are the manual mode and the rollback path.
- **Steps:**
  1. Add the fields, keys and both mapping blocks.
  2. Write the pure `effective()` and the pure `migrateCalibration(settings, cam, hasStoredDims)`.
  3. Have the service call the migration and persist its result.
  4. Update Phase 8 C2.3's `withDefaultDimensions()` so it zeroes the deltas (one place).
- **Acceptance criteria:**
  - An upgrade on a device with a cutout renders an S2-identical pill.
  - A fresh install centres the pill on the camera.
  - With no cutout, rendering is identical to today.
- **Tests:**
  - `EffectiveDimensionsTest`: none, centre hole, side hole, waterdrop, deltas at the range limits.
  - `CalibrationMigrationTest`: fresh install; upgrade with default dims; upgrade with extreme dims (expects `autoCalibrate = false`); idempotent on a second run.
- **Depends on:** V1.1.
- **Effort/Risk:** S / medium. Migration mistakes move users' pills; the tests above guard against that.
- **Ships as:** 1 PR, "Calibrated dimensions model + migration (no visual change for existing users)".
- **Design check:** the current look is preserved for every existing user by construction. New installs get the same pill, centred on the camera.

### V1.3: Apply the calibration: effective settings, `cameraZone` and window bounds
- **Goal:** the island draws and sizes its window from `EffectiveDimensions`, and the five `Spacer(36.dp)` become `cameraZone`.
- **Why:** 02 R1. Content can sit on the camera when the camera isn't exactly 36 dp at screen centre.
- **Files:**
  - `service/IslandOverlayService.kt:702-712`: where `liveSettings` is built. Add `.withCalibration(cam)` next to the high-contrast copy, so every `settings.topOffsetDp`, `heightDp` and `horizontalOffsetDp` read in the island gets the effective value.
  - `service/IslandOverlayService.kt:640-654` and `:986-1027`: window math. Use the calibrated `currentSettings`, or the S3 pure function if it has landed.
  - `island/IslandView.kt:2022, 2060, 2105, 2173, 2867`
  - `island/IslandTokens.kt`
- **Spec:**
  - `fun IslandSettings.withCalibration(cam, screenWDp): IslandSettings` returns a copy whose `topOffsetDp`, `heightDp` and `horizontalOffsetDp` hold the *effective* values. This is a single seam, and `IslandOverlayRoot` stays unaware of it.
  - `val LocalCameraZone = staticCompositionLocalOf { 36.dp }`, provided next to `LocalTextStyle` (`IslandOverlayService.kt:725-728`).
  - Replace the five Spacers with `CameraGap()`, which reads `LocalCameraZone`.
  - **Gap follows the lens:** when the user's horizontal delta shifts the pill away from the camera, the gap shifts back by the same amount, so it stays over the lens: `gapOffsetX = −horizontalOffsetDeltaDp`. Clamp it so neither slot drops below 18 dp. Implement it as a `Modifier.offset` on `CameraGap`, and make the leading and trailing weights asymmetric only through V3.1's layout. Until V3.1 lands, the offset is applied to the Spacer only.
  - Persist the migration result from V1.2 here.
- **Steps:**
  1. Write `withCalibration`.
  2. Provide `LocalCameraZone`.
  3. Swap the five Spacers.
  4. Re-point the window math.
  5. Add a debug-only "Show camera outline" toggle in `ui/main/LiveActivityTestPanel.kt`. It draws a 1 dp red ring at `CameraGeometry` in the island window and is used for CP1 photos.
- **Acceptance criteria:**
  - On 2+ devices (centre hole and side hole), the debug ring sits on the lens within ±1 dp, and the pill is concentric with it.
  - The gap never overlaps the lens.
  - With no cutout, S2 is identical.
  - The window still contains the pill at the extreme deltas.
- **Tests:**
  - `IslandOverlayCrashTest` still green.
  - New Robolectric `CameraZoneTest`: with `LocalCameraZone = 40.dp`, the leading slot's right edge plus 40 dp equals the trailing slot's left edge.
  - S3 window table extended with calibrated inputs.
- **Depends on:** V1.2; F2 (the token home). Coordinate with F5 if the window code has moved.
- **Effort/Risk:** M / medium (window bounds on OEMs).
- **Ships as:** 1 PR, "Island follows the real camera".
- **Design check:** same pill, same squircle and same content, just placed on the lens. The brackets "( )" stay. No Apple-style forehead wrapping.

### V1.4: Dimensions page: "Fit to camera" switch and fine-tune labels
- **Goal:** the Dimensions page exposes `autoCalibrate`, shows what was detected, and relabels the sliders as fine-tune deltas when calibration is on.
- **Why:** PLAN V1 and 05 §2 ("six raw sliders"). Pairs with Phase 8 C2.4, which owns the one-tap button and the live preview.
- **Files:** `ui/main/MainScreen.kt:1866-1951`. If C5.1 has landed, use `DimensionsPage.kt` instead.
- **Spec:**
  - A new top row: Switch "Fit to camera", with supporting text "Punch-hole camera, 24 dp, centre" or "No camera cutout found, so manual position is used".
  - **When on:**
    - "Top offset" becomes "Vertical fine-tune", −12…+12 dp, with the value chip showing a signed value ("+2 dp").
    - "Horizontal Offset" becomes "Horizontal fine-tune", −24…+24 dp.
    - "Height" becomes "Height fine-tune", −8…+8 dp.
    - "Width" stays absolute, labelled "Maximum width" once V3.1 lands.
  - **When off:** today's sliders and ranges, unchanged.
  - **Switching on** recomputes and zeroes no deltas, so the pill doesn't jump. A separate "Reset" (C2.3) zeroes them.
- **Steps:**
  1. Read `IslandStateManager.cameraGeometry` in the ViewModel.
  2. Add the switch row.
  3. Switch the slider bindings between the absolute and delta fields.
- **Acceptance criteria:**
  - Toggling the switch never moves the pill.
  - The labels and ranges match the mode.
  - TalkBack reads "Vertical fine-tune, plus 2 dp".
- **Tests:** `MainScreenViewModelTest`: the binding writes the delta fields when on and the absolute fields when off.
- **Depends on:** V1.3. Coordinate with C2.3 and C2.4 (Phase 8).
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Dimensions: fit to camera".
- **Design check:** the current grouped M3 list is kept. One row is added and labels change; no restyle.

### V1.5: Landscape anchors to the camera
- **Goal:** with "Show in landscape" on, the pill is placed relative to where the camera actually is in the current rotation, instead of a portrait-tuned top-centre offset.
- **Why:** PLAN V1 "landscape anchors to the camera". Today landscape reuses the portrait top and horizontal offsets.
- **Files:**
  - `island/layout/EffectiveDimensions.kt`
  - `service/IslandOverlayService.kt:633-635` and `:921-927` (orientation checks)
  - `island/IslandView.kt:439-440`
- **Spec:**
  - **The camera is on the top edge in this rotation** (rotation 0, or 180 on phones whose camera sits on the bottom edge in portrait): use the V1.2 formulas.
  - **The camera is on a side edge** (rotation 90 or 270):
    - The pill stays on the top edge, so the island doesn't move into the content area. Its inner cap is placed `8 dp` from the cutout's inner edge, on the camera side: `horizontal = ±(screenW/2 − cutoutInnerEdgeDp − 8 − W/2)`, mirrored per side.
    - `top = 6 dp`.
    - `cameraZone = 8 dp`, because nothing physical sits under the pill.
    - The minimised state (`userMinimized`) becomes a camera ring at the cutout itself, centred on the lens. The window must include that rect.
  - With no cutout, or `WIDE_NOTCH`, keep today's behaviour.
- **Steps:**
  1. Write the pure landscape branch.
  2. Have window math include the minimised ring rect.
  3. Recompute geometry on rotation (V1.1 already listens).
- **Acceptance criteria:**
  - In landscape on a centre-hole phone, the pill sits at the top, near the camera side, and never covers the centre of the status bar.
  - The minimised ring is concentric with the lens.
  - Rotating back restores the portrait pill.
- **Tests:** `EffectiveDimensionsTest`: landscape cases for rotation 90 and 270, with and without a cutout.
- **Depends on:** V1.3.
- **Effort/Risk:** S / medium (OEM rotation behaviour; device-verify).
- **Ships as:** 1 PR, "Landscape: anchor to the camera".
- **Design check:** same pill and same minimised circle. Android-own behaviour (Apple has no rotating island).

---

## V2: Concentric geometry

**Principle:** nested shapes share a centre of curvature, with even margins, so corners never "fight". Android-native: a `CardGeometry` CompositionLocal plus a `Modifier.concentricClip`, the Compose counterpart of a container-relative shape. Everything is still drawn with `SquircleShape` at 0.6.

**Concentric radius math.** All values are in dp, at `expandedScale` 1.1 and font scale 1.0 unless stated.
- **From `LiveLayout`** (`LiveActivities.kt:336-344`):
  - `m = (14·s).coerceIn(12, 18)`, which is **15.4**
  - `control = (50·s).coerceIn(48, 58) · rowGrowth`, which is **55**
  - `R = control/2 + m`, which is **42.9**. This is the only card radius.
- **Insets (one system, two named steps):**
  - `inset.tight = m` (15.4): one-row live cards (unchanged) and the flashlight card.
  - `inset.roomy = R/2` (21.45): every tall card. Today's tall-card padding is `20·s = 22` (`IslandComponents.kt:243-249`). The change is −0.55 dp at s = 1.1, +2 dp at s = 0.8 (16 → 18) and −0.5 dp at s = 1.3 (24 → 23.5), so tall cards keep their current feel. A corner-nested element at `R/2` gets a radius of exactly `R/2`.
  - Why not a single `m` everywhere? At inset 15.4, the media art would need radius 27.5 on a 59 dp cover, which is almost a circle and amounts to a redesign. `R/2` keeps the art a rounded square.
- **Inner radius:**
  - `inner(inset) = max(R − inset, 6)`
  - For an element of size w × h, `radiusFor(inset, w, h) = min(inner(inset), cap)`, where `cap = 0.42·min(w, h)` when `max/min < 1.5` (square-ish tiles stay squircles, not circles), and `min(w, h)/2` otherwise (bars and buttons may become capsules).
  - **Inside a capsule container** (the pill or the bubble): `R_c = H/2`, and the element's radius is `min(R_c − inset, side/2)`. No cap applies; concentric with a round end means round.

| Element | Inset | Today | Concentric result |
|---|---|---|---|
| Media art 59.4 dp (`IslandView.kt:3445`) | roomy 21.45 | 12 (`RadiusChip`) | **21.45** (cap 24.9) |
| Header tiles 40 dp (`:4008, 4285, 4402, 4534`) | roomy | 10 (`RadiusIcon`) | **16.8** (capped; 44 dp after V4.2 gives 18.5) |
| Bottom-nested panels (`:4437, 4562, 4593`) | roomy | 10–12 | **21.45** (bars, capsule cap h/2) |
| Action buttons 44–50 dp, outer corners (`IslandComponents.kt:467-479`) | roomy | 14 | **21.45** (near-capsule; inner corners stay `SpaceXs` 4) |
| Compact media art 20 dp (`IslandView.kt:2782`) | (36 − 20)/2 = 8 in a capsule of 18 | 5 | **10** = circle |
| Reply input (`:3862`) | — | capsule | unchanged |
| Notification avatar | — | circle | unchanged (identity) |
| One-row live card | tight 15.4 | R = 42.9 = capsule | unchanged |

### V2.1: `CardGeometry`, `inner()`, `concentricClip`, a per-corner `SquircleShape` and a capsule fast path
- **Goal:** the geometry system and the shape primitives exist and are tested. Nothing renders differently yet.
- **Why:** 02 R3 and R9; PLAN V2.
- **Files:**
  - New: `island/layout/CardGeometry.kt`
  - Changed: `island/IslandShapes.kt:30-145`; `island/LiveActivities.kt:328-358`. `LiveLayout` delegates margin, control and radius to `CardGeometry`, with the same numbers.
- **Spec:**
  ```kotlin
  @Immutable class CardGeometry(scale: Float, fontScale: Float) {
      val m: Dp; val control: Dp; val R: Dp            // exactly LiveLayout's formulas
      val insetTight: Dp get() = m
      val insetRoomy: Dp get() = R / 2
      fun inner(inset: Dp): Dp = (R - inset).coerceAtLeast(6.dp)
      fun radiusFor(inset: Dp, w: Dp, h: Dp): Dp
      fun radiusInCapsule(containerH: Dp, inset: Dp, side: Dp): Dp = minOf(containerH / 2 - inset, side / 2)
  }
  val LocalCardGeometry = staticCompositionLocalOf<CardGeometry> { error("no CardGeometry") }
  fun Modifier.concentricClip(inset: Dp, w: Dp, h: Dp): Modifier  // clip(islandShape(LocalCardGeometry.radiusFor(...)))
  ```
- **Per-corner squircle:**
  - Add `SquircleShape(topStart, topEnd, bottomEnd, bottomStart, smoothing = 0.6f)`, mapping start and end through `LayoutDirection`.
  - The existing single-radius constructor delegates to it.
  - The per-corner budget is `min(w, h)/2`, and the smoothing fade per corner is exactly today's (`:41-46`).
  - Also add `islandShape(topStart, topEnd, bottomEnd, bottomStart)`.
- **Capsule fast path:**
  - When every radius is at least `budget − 0.5 px`, `createOutline` returns `Outline.Rounded(RoundRect(0, 0, w, h, CornerRadius(budget)))`.
  - At a capsule today's maths already gives `s = 0` and `arcMeasure = 90°`, a pure circle, so the output is pixel-identical up to AA.
  - `Outline.Rounded` lets RenderNode clip by outline instead of a path, which is cheaper. It applies to the pill, the bubble and capsule buttons.
- **Steps:**
  1. Write `CardGeometry` and refactor `LiveLayout` to use it.
  2. Write the per-corner `SquircleShape` and the fast path.
  3. Provide `LocalCardGeometry` in `IslandOverlayRoot`, as `remember(expandedScale, fontScale)`.
- **Acceptance criteria:**
  - S2 is pixel-identical.
  - `SquircleShape(r)` and `SquircleShape(r, r, r, r)` produce identical paths.
  - For a capsule, the fast-path outline matches the generic path within 0.5 px at 16 sample angles.
- **Tests:**
  - `CardGeometryTest`: m, control and R at scales 0.8, 1.0, 1.1 and 1.3 and font scale 1.0 and 1.5. These equal `LiveLayout`'s values, and the table above is reproduced.
  - `SquircleShapeTest`: per-corner equivalence, the capsule fast path, RTL mirroring, zero radius giving a rectangle.
- **Depends on:** F2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "CardGeometry + per-corner squircle (no visual change)".
- **Design check:** smoothing stays 0.6 and the shapes are identical. Infrastructure only.

### V2.2: One margin system: retire `islandCardContentPadding`
- **Goal:** every expanded card's content inset comes from `CardGeometry` (`roomy` for tall cards, `tight` for live rows and flashlight), and the measure functions change in lockstep.
- **Why:**
  - 02 §2 "Two margin systems". Live cards use 15.4 dp; other cards use 22 dp; FPS, weather and network add a further `padding(4.dp, 2.dp)` for 26 dp.
  - Cards look assembled from different kits (02 gap 3).
- **Files:**
  - `island/IslandComponents.kt:237-249` (delete after migration), `:255-275` (`NotificationCardMetrics` paddings), `:278+` (`ProgressCardMetrics`), `:313`, `:342`
  - `island/IslandView.kt` call sites: `:3063, 3427, 3762, 3993, 4112, 4273, 4390, 4522`, plus each extra inner `padding(4.dp, 2.dp)` in the FPS, weather and network views
- **Spec:**

  | Card | Today (s 1.1) | New |
  |---|---|---|
  | media, idle | 22 all sides | roomy 21.45 all sides |
  | notification, progress | 22 h / 19.8 v | roomy all sides (+1.65 v) |
  | FPS, weather, network | 22 + 4 h, 22 + 2 v | roomy all sides (the extra padding is removed) |
  | flashlight | 17.6 h / 13.2 v | tight 15.4 all sides |
  | live rows | 15.4 | unchanged |

  The `measure*CardHeight` functions read the same `CardGeometry`, so the measured height equals the laid-out height.
- **Steps:**
  1. Write `Modifier.cardInset(kind: CardInsetKind)`.
  2. Swap each call site.
  3. Update the metrics classes.
  4. Delete `islandCardContentPadding`, `islandCardHorizontalPadding` and `islandCardVerticalPadding`.
- **Acceptance criteria:**
  - The S2 diffs are limited to the inset shifts in the table (≤ 4.6 dp).
  - Notification and progress cards are still exactly as tall as their content (the existing measure tests still pass).
  - No text clips at font scale 1.5.
- **Tests:**
  - `CardInsetTest`: each priority maps to the expected inset.
  - Robolectric `MeasuredHeightParityTest` (notification and progress): the measured value equals the laid-out content height ± 0.5 dp.
- **Depends on:** V2.1; F4 (line heights, so the measures are exact).
- **Effort/Risk:** M / medium (lockstep measure changes).
- **Ships as:** 1 PR, "One margin system for cards".
- **Design check:** same cards and same layout structure, with margins unified within ≤ 4.6 dp. Not Apple's 14 pt rule copied: the inset derives from Droidbubble's own `R`.

### V2.3: Concentric inner radii in squircles
- **Goal:** art, tiles, panels, action rows, the compact art and the notch pill all use `CardGeometry` radii drawn with `islandShape`. Plain `RoundedCornerShape` is gone from the island.
- **Why:** 02 §2 (radius table), R3 and R9; gap 9 (mixed shape families).
- **Files:**
  - `island/IslandView.kt:2782, 3445, 3842, 4008, 4131, 4285, 4402, 4437, 4534, 4562, 4593`
  - `island/IslandComponents.kt:152, 188` (chips), `:466-481` (action row)
  - `island/IslandShapes.kt:140-145` (`notchPillShape` becomes a per-corner squircle)
  - `island/IslandTokens.kt:33-36`. Delete `RadiusIcon`, `RadiusButton` and the unused `RadiusCard`, or point them at `CardGeometry`.
- **Spec:**
  - Apply the radius table above.
  - **Chips** (`InfoChip`, about 24 dp tall) become capsules: `radiusFor(…, h = 24)` gives 12, the same as `RadiusChip` today, now as a squircle.
  - **Action row:** outer corners `radiusFor(roomy, w, h)`, inner corners `SpaceXs`, using per-corner `islandShape`.
  - **Notch pill:** `islandShape(0, 0, r, r)` with `r = H·0.45`, the same as today's value.
  - **Glass panes** keep circular `GradientDrawable` corners (`GlassPaneWindows.kt:138-139`); the mismatch is under 0.5 px at R = 42.9. Note this in the PR, but change nothing.
- **Steps:**
  1. Change one element family per commit.
  2. Re-record S2 for each commit, so the owner can approve each family.
- **Acceptance criteria:**
  - `grep RoundedCornerShape island/` returns 0 hits, except the API < 33 fallback in `islandShape`.
  - The S2 diffs match the radius table.
  - CP1 is approved.
- **Tests:** `ConcentricRadiusTest`: an element at inset `i` has radius `R − i`, or its cap. Run per card kind at scales 0.8, 1.1 and 1.3.
- **Depends on:** V2.2.
- **Effort/Risk:** M / medium (visible; owner-judged).
- **Ships as:** 1 PR with one commit per family: "Concentric inner corners".
- **Design check:** squircle family at 0.6 everywhere. Tiles stay rounded squares (capped at 0.42) and art stays a rounded square; nothing becomes an iOS-style widget clone. **Checkpoint CP1.**

---

## V3: Compact balance

**Principle:** no wasted width, balanced sides, large matched text, never an ellipsis. Android-native: a measured slot layout with system-font metrics from F4. The width axis is out of scope, because no font is bundled, so "squeeze" is a mild geometric condense.

### V3.1: Content-hugging compact width with `CompactSlotLayout`
- **Goal:** the pill is `cameraZone + 2·max(lead, trail) + 2·inset` wide, clamped to `[Wmin, widthDp]`, and the camera stays centred between balanced sides.
- **Why:** 02 gap 8 and R7. The pill is 180 dp whatever it shows (`IslandView.kt:900-911`); the flashlight's trailing side is empty (`:2886`).
- **Files:**
  - New: `island/layout/CompactSlots.kt` (pure width model plus the layout)
  - `island/IslandView.kt:900-911` (target width; after M1, the compact target of `IslandGeometry`), `:982-984` (bubble rest uses the measured half-width, not `widthDp/2`), `:2654-2870` (Row, weights and Spacer replaced by `CompactSlotLayout`)
- **Spec:**
  ```kotlin
  sealed interface CompactSlot {
      data class Icon(val size: Dp) : CompactSlot                        // V7.2 size tokens
      data class Image(val size: Dp) : CompactSlot                       // compact art 20 dp
      data class Avatar(val ringFill: Boolean) : CompactSlot             // H − 2·ring
      data class Value(val text: String, val template: String) : CompactSlot   // NumericSlot width, never per-tick
      data class Label(val text: String) : CompactSlot
      data class IconValue(val icon: Dp, val gap: Dp, val value: Value) : CompactSlot
      data class Fixed(val width: Dp) : CompactSlot                      // equalizer, waveform, dot
      data object Empty : CompactSlot
  }
  fun compactWidth(lead: Dp, trail: Dp, cameraZone: Dp, inset: Dp, wMin: Dp, wMax: Dp): Dp =
      (cameraZone + 2 * maxOf(lead, trail) + 2 * inset).coerceIn(wMin, wMax)
  ```
- **Widths and tokens:**
  - Slot widths are computed **purely**, with `TextMeasurer` and F4 roles. Numeric values use the `NumericSlot` template width ("00:00", "100%", "000"), so the pill never jitters per tick.
  - `inset = (H − 20)/2` coerced to 6–14 dp, today's `:2261`.
  - `Wmin = 120 dp`, a token. `Wmax = settings.widthDp`, labelled "Maximum width" in V1.4.
- **Gap placement:**
  - The camera gap sits at the lens, as V1.3's `gapOffsetX`.
  - Each side is laid out in a box of `max(lead, trail)` width. The leading side is end-aligned toward the camera, and the trailing side is start-aligned, so content sits snug to the lens.
  - That flips today's `CenterStart` and `CenterEnd`, which pin content to the outer edges. **This is a visible change** that goes to CP2.
  - The alternative (keep outer pinning, hug width only) is a one-flag switch, `CompactSlotLayout(snugToCamera = true)`.
- **Dual mode:** `isCompactDual` keeps the `SpaceSm` padding and icon-only sides, also width-hugging.
- **Transitions:** a width change rides the existing compact width spring. After M1, it's the geometry's compact target, so an activity swap animates width and content together (M3).
- **Steps:**
  1. Write the pure width model and its tests.
  2. Map each priority to its `CompactSlot`s, mirroring today's content.
  3. Write `CompactSlotLayout(lead, trail, cameraZone, snugToCamera)`.
  4. Point the target width at the model.
  5. Fix the bubble rest X.
- **Acceptance criteria:**
  - The S2 compact states are narrower, and symmetric about the lens within ±0.5 dp.
  - Width is constant while a timer, recording time or percentage ticks: a 10 s capture shows 0 width changes.
  - The dual bubble rests 6 dp from the measured pill edge.
  - The window still contains everything (window width uses `widthDp`, an upper bound).
- **Tests:**
  - `CompactWidthTest`: each priority at font scales 1.0 and 1.3 against a golden width table, plus clamping at Wmin and Wmax.
  - `CompactTickStabilityTest`: values "9:59" → "10:00" keep the same slot width.
  - `IslandOverlayCrashTest` still green.
- **Depends on:** F4 (roles, `NumericSlot`), V1.3 (`cameraZone`), V7.2 (icon size tokens, or use today's sizes until then).
- **Effort/Risk:** M / medium (visible; bubble and bracket maths).
- **Ships as:** 1 PR, "Compact pill hugs its content".
- **Design check:** same capsule, brackets and content per activity, with less empty black. Balanced sides is a principle, not Apple's 52.33 pt slot sizes.

### V3.2: Matched type on both sides, and the fit order (squeeze, abbreviate, drop; never an ellipsis)
- **Goal:** leading and trailing text both use F4 compact roles, and overflowing text follows a deterministic fit order with no `TextOverflow.Ellipsis` anywhere in compact.
- **Why:**
  - 01 finding 4 and R4: leading labels are 10–10.5 sp at 85% white with ellipsis (`IslandView.kt:2764-2770, 2805-2809, 2830-2836, 2852-2856`); trailing values are 10.5–12 sp.
  - 01 §5 "never ellipsize".
- **Files:**
  - `island/IslandView.kt:2654-2970`
  - New: `island/layout/CompactFit.kt`
- **Spec:**
  - **Roles:** `compactLabel` 13 / 16 sp weight 600 for labels in the primary color; `compactValue` 14 / 16 sp weight 600 in the accent; `compactUnit` 11 / 16 sp weight 500 for %, MB/s and ° in the secondary color.
  - **Fit order** for a slot of available width `A = (Wmax − cameraZone)/2 − inset`:
    1. **squeeze:** `TextGeometricTransform(scaleX = 0.92f)` plus `letterSpacing = −0.01.em`. Never go below 0.92; it's synthetic and any further would distort strokes.
    2. **abbreviate:** a `CompactAbbreviations` table, for example "Partly cloudy" → "Cloudy", "Thunderstorm" → "Storm", the progress `appName` → dropped, and "Download" → dropped.
    3. **drop:** remove the text and keep the icon.
  - **Speed:** use 3 significant digits ("870 KB/s", "1.23 MB/s", "12.3 MB/s"), changing `PersistentWidgetTelemetryManager.kt:250-254`. In compact, show the number as `compactValue` and the unit as `compactUnit`.
  - **Network label:** the hard-coded "5G" (`IslandView.kt:2852`) is B7's job. Here, render whatever B7 provides.
- **Steps:**
  1. Write the pure `fitCompactText(text, role, available, measurer): FitResult(text, scaleX, dropped)` and its tests.
  2. Apply it to every compact label.
  3. Remove every `overflow = TextOverflow.Ellipsis` from compact code.
- **Acceptance criteria:**
  - `grep Ellipsis` inside `CompactContent` returns 0 hits.
  - Both sides use the same role family.
  - At font scale 1.3, nothing clips; text squeezes, abbreviates or drops in that order.
- **Tests:** `CompactFitTest`, table-driven: fits; squeeze fits; abbreviation fits; dropped; numeric never abbreviated; RTL text.
- **Depends on:** V3.1, F4.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Compact text: matched and never truncated".
- **Design check:** system font only, with today's colors. Larger, steadier type is a precision fix, not a new type scale. No SF tracking table.

### V3.3: Oval bubble for short text, and a readable bubble ring
- **Goal:** the split bubble is `H … 1.25·H` wide to fit its content, still a capsule squircle, and the progress numeral is readable.
- **Why:**
  - 02 gap 8: the bubble is always a 36 dp circle (`IslandView.kt:1692-1703`).
  - 01 finding 5: an 8 sp numeral in a 17 dp ring (`:2525-2537`).
  - B9 already raised the numeral to ≥ 11 sp; this task makes it fit.
- **Files:**
  - `island/IslandView.kt:981-984` (`bubbleSize` → `bubbleWidth`, `bubbleHeight`), `:1675-1756`, `:1761-1796` (neck uses the bubble's half-width), `:2305-2310` (bracket edge), `:2462-2615`
  - `service/IslandOverlayService.kt:997-1000` (the dual window width already covers 1.25 H; verify)
- **Spec:**
  - `bubbleW = (contentW + 2·6dp).coerceIn(H, 1.25·H)`, using the V3.1 pure width model with the `bubbleValue` role (12 / 14 sp, min 11).
  - The shape is `islandShape(H/2)` (capsule fast path), not `CircleShape`.
  - **Progress ring:** 24 dp diameter, 2 dp stroke, with the numeral in `bubbleValue` and `scaleX 0.88` for 3 digits.
  - **Above font scale 1.15**, show the ring alone, with `contentDescription = "42 percent"`. The numeral must never exceed the ring's inner diameter: test `textW ≤ 24 − 2·2 − 2`.
  - The FPS bubble's `FontWeight.Black` becomes `bubbleValue` (weight 700).
  - **Rest position:** `compactMainHalf + 6dp + bubbleW/2`.
- **Steps:**
  1. Add the width model for the bubble.
  2. Swap the shape.
  3. Fix the neck, bracket and rest maths.
  4. Rework the ring.
- **Acceptance criteria:**
  - "100", "23°" and "120" bubbles stay circles at font scale 1.0 and become ovals of at most 45 dp at 1.3.
  - The neck still joins and snaps cleanly at every width.
  - The ring numeral never overflows.
- **Tests:**
  - `BubbleWidthTest`.
  - The Robolectric bubble state in VD0.1 at font scales 1.0 and 1.3.
  - `IslandOverlayCrashTest` (dual states).
- **Depends on:** V3.1, B9, F4.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Oval bubble for short values".
- **Design check:** same black capsule family, still budding from the pill. "Circular or oval" is a principle; the sizes are Droidbubble's own (H to 1.25 H).

### V3.4: A trailing value for the flashlight
- **Goal:** the flashlight pill shows its level on the trailing side, so it's balanced.
- **Why:** 02 gap 8 (`IslandView.kt:2886` is `Unit`).
- **Files:** `island/IslandView.kt:2687-2713` (leading) and `:2886`
- **Spec:**
  - When `maxFlashlightLevel > 1`, show `NumericSlot("${pct}%", template = "100%")` in `compactValue`, white, where `pct = round(100·level/max)`.
  - Otherwise, show `compactLabel` "On", in the secondary color.
  - Rolls via `RollingNumberText` when the level changes.
  - The leading glow and icon are unchanged.
- **Steps:** map the trailing slot; wire `flashlightLevel` and `maxFlashlightLevel`, which are already parameters at `:2660-2661`.
- **Acceptance criteria:** the flashlight pill is symmetric. Changing the strength rolls the digits, and the width doesn't change.
- **Tests:** a VD0.1 state with level 3 of 5, and one with max 1.
- **Depends on:** V3.1.
- **Effort/Risk:** S / low.
- **Ships as:** part of the V3 series PR, or alone: "Flashlight level in the pill".
- **Design check:** existing glow and icon, plus one value in the existing compact style.

### V3.5: System alert pills hug their content
- **Goal:** the charging, unlock, Bluetooth/Wi-Fi and ringer pills use the V3.1 width model instead of `widthDp+20`, `×0.82` and `×0.95`.
- **Why:** the same principle as V3.1, applied to the pills at `IslandView.kt:893-895` and `:1986-2210`, and to the window maths at `IslandOverlayService.kt:986-995`.
- **Files:** `island/IslandView.kt:893-895, 1986-2210`; `service/IslandOverlayService.kt:986-995` (keep upper bounds)
- **Spec:**
  - Width is `compactWidth(...)` with `Wmin = 120` and `Wmax = widthDp + 20`, today's charging width, kept as the maximum.
  - The charging wattage and percentage use `compactValue` with `NumericSlot` templates "00W" / "00.0W" and "100%".
  - The 13 sp literals (`:2014`, `:2032`) and 12 sp (`:2203`) move to roles.
- **Steps:** map each alert to slots, then swap the widths.
- **Acceptance criteria:** the alert pills are symmetric about the lens. The charging fill (`:1960-1984`) still spans the whole pill. No ellipsis.
- **Tests:** VD0.1 alert states; `CompactWidthTest` cases.
- **Depends on:** V3.1, V3.2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Alert pills hug their content".
- **Design check:** the charging tank fill, colors and icons are all kept.

---

## V4: Expanded spacing and heights

**Principle:** take no unnecessary height, and keep one rhythm across cards. **The current card designs are kept; only empty space goes.**

### V4.1: Measured heights instead of the fixed table
- **Goal:** the media, weather, FPS, network, flashlight and idle cards are measured from their content, as notification and progress cards already are. `cardHeightFor`'s fixed table (`IslandComponents.kt:218-229`) is deleted.
- **Why:** 02 gap 7 and R8. Cards are 30–40% taller than their content (media and weather 214.5 dp, flashlight 225.5, FPS and network 203.5 at s 1.1), and `SpaceBetween` (`IslandView.kt:3428`) hides the slack.
- **Files:**
  - `island/IslandComponents.kt:211-230, 255-342`
  - `island/IslandView.kt:941-947, 976`, plus each card view (`:3031, 3282, 4108, 4260, 4373, 4514`)
- **Spec:**
  - One `XCardMetrics(scale, fontScale, geometry)` class per card, plus `measureXCardHeight(...)`: a pure sum of rows from F4 line heights and `CardGeometry` insets, the pattern of `NotificationCardMetrics`.
  - The views switch from `Arrangement.SpaceBetween` to explicit V4.2 rhythm gaps, so layout equals the measured sum.
  - **Worked example, media at s 1.1:** `21.45·2 + art 59.4 + 8.8 + slider row 18 + 4.4 + controls 55 = 188.5 dp`, down from 214.5 (−12%).
  - **Ceiling:** `min(176·s·textGrowth, 0.45·screenH)`. Beyond that, the card content scrolls; C3.10 builds on it at font scale 2.0.
  - **Floor:** `R·2`, so a card is never shorter than a capsule.
- **Steps:**
  1. Do one card per commit: media, weather, FPS, network, flashlight, idle.
  2. Delete the table.
  3. Make `cardHeightFor` a dispatcher over the measure functions.
- **Acceptance criteria:**
  - Each card's settled height equals its laid-out content height ± 0.5 dp, at font scales 1.0, 1.3 and 1.5.
  - Heights fall by about 10–15% versus baseline, recorded in a golden table.
  - No clipping. Card 2 uses the same functions.
- **Tests:**
  - `MeasuredHeightParityTest`, extended to every card: Robolectric lays out each card with `requiredWidth(w)` and `wrapContentHeight`, and the result is compared with its `measure*` value.
  - `CardHeightGoldenTest`.
- **Depends on:** V2.2, F4.
- **Effort/Risk:** M / medium (many views; visible).
- **Ships as:** 1 PR, one commit per card: "Cards as tall as their content".
- **Design check:** every card keeps its rows, elements and order; only the empty space between them goes.

### V4.2: One header, padding and button rhythm across cards
- **Goal:** every tall card uses the same `CardRhythm`: icon/art to text, header to body, body to actions, button height and row gap. FPS, weather and network scale with `expandedScale` like the others.
- **Why:**
  - 01 finding 6: FPS, weather, network, idle and the media time labels don't scale (`IslandView.kt:4119, 4319-4366, 4419-4507, 4579-4640, 3386`).
  - Header tiles are 40/22 dp fixed in three cards but `40·s` elsewhere.
- **Files:** `island/IslandComponents.kt:93-136` (`IslandHeader` takes the rhythm), `:255-275`; the six card views; `island/IslandView.kt:917` (side margin)
- **Spec.** `CardRhythm(s)` codifies the notification card's existing metrics, the best-tuned card:

  | Role | Rule |
  |---|---|
  | `iconGap` | `12·s`, coerced to 8–16 |
  | `titleToSubtitle` | `SpaceXxs·s`, 1–3 (real now that F4 trims line boxes) |
  | `headerToBody` | `SpaceSm·s`, 6–10 |
  | `bodyToActions` | `SpaceMd·s`, 10–14 |
  | `buttonHeight` | `42·s`, 44–50 |
  | `rowGap` | `CardGeometry` gap |
  | `tile` | `40·s`, 32–48; glyph = 0.55·tile (V7.2) |

  **Card side margin:** keep `340·s` but cap at `screenW − 24 dp` instead of − 16, so it's at least 12 dp per side. The expanded top (`topOffset + 52`, `:1024-1028`) is unchanged, so the card stays detached below the status bar.
- **Steps:**
  1. Write `CardRhythm`.
  2. Have `IslandHeader` and `IslandActionButtonRow` read it.
  3. Swap each card's literals.
  4. Re-run V4.1's parity tests.
- **Acceptance criteria:**
  - Title sizes are identical across cards at each scale; this is shared with F4.
  - Header-to-body and body-to-actions gaps are equal across cards ± 0.5 dp.
  - The FPS, weather and network cards scale.
- **Tests:** `CardRhythmTest`; S2 and VD0.1 diffs.
- **Depends on:** V4.1, F4.
- **Effort/Risk:** M / low.
- **Ships as:** 1 PR, "One rhythm for all cards".
- **Design check:** same header pattern and button rows, just spaced consistently. **Checkpoint CP3** (together with V5 and V6).

---

## V5: Color consolidation and legibility

**Principle:** a black base with sparing, meaningful accents, and text that always reads. Same hues: decision D3 is No.

### V5.1: One semantic color per meaning
- **Goal:** colors are referenced by meaning: `Recording`, `Success`, `Warning`, `Info`, `Timer`, `MediaAccent`, plus the text roles. Every remaining duplicate is gone.
- **Why:**
  - 02 §2 palette drift: three reds (`FF3B30` `IslandView.kt:542`, `FF453A` `:1420`, `FF5A47` `LiveActivities.kt:286`), two greens (`30D158`, `34C759` `:289`) and two yellows (`FFD60A`, `FFCC00` `:293`).
  - **Overlap note:** F2 already merges duplicates. If F2 has done so, this task shrinks to the role aliases and the usage-by-meaning pass.
- **Files:** `island/IslandTokens.kt:51-57`; `island/LiveActivities.kt:285-313`; `island/IslandView.kt:541-567, 1409-1428` and the compact and bubble accents
- **Spec:**

  | Role | Value kept (existing) | Replaces |
  |---|---|---|
  | `Recording` | `FF453A` (`AccentRed`) | `FF3B30`, `FF5A47` |
  | `Success` | `30D158` (`AccentGreen`) | `34C759` |
  | `Warning` | `FFD60A` (`AccentYellow`) | `FFCC00` |
  | `Info` | `64D2FF` (`AccentBlue`) | — |
  | `Timer` | `5B9BFF` (`TimerBlue`) | — (its own meaning) |
  | `Stopwatch` | `FF9F0A` (`AccentOrange`) | — |

  `LiveActivityColors` keeps its API, and its members alias the roles. Outline colors use `role.copy(alpha = …)`.
- **Steps:** add the roles, repoint the usages, then delete the literals.
- **Acceptance criteria:** the F2 grep finds 0 `Color(0x` outside the token files. The S2 diffs are limited to the recording red (slightly darker), the call green and the alarm yellow.
- **Tests:** `SemanticColorTest`: each `LiveActivityKind` maps to its role. Each role's contrast on black is ≥ 4.5:1, computed in the test.
- **Depends on:** F2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Semantic island colors".
- **Design check:** same hues, merged. The palette's iOS resemblance (02 §5) is acknowledged and out of scope per decision D3.

### V5.2: Album accent from Palette, with L\* and chroma clamps
- **Goal:** the media accent is vivid, readable on black, and never muddy or near-black.
- **Why:** 02 §2 and R4. It's a plain RGB average of a 16×16 downscale (`IslandView.kt:329-378`).
- **Files:**
  - `island/IslandView.kt:327-378` (move it to `island/color/MediaAccent.kt`)
  - `gradle/libs.versions.toml` and `app/build.gradle.kts`: add `androidx.palette:palette-ktx`
- **Spec:**
  - Run `Palette.from(bitmap).maximumColorCount(16).generate()`. Pick the swatch in this order: `vibrant`, `lightVibrant`, `dominant`.
  - Convert it to LAB with `ColorUtils.colorToLAB`.
  - **Neutral fallback:** if chroma `C* = √(a² + b²) < 20`, return `Color.Transparent`. Callers already treat Transparent as "no accent" (`:544`, `:1400`, `:2931`, `:3377`).
  - **Clamp L\*** to 70–80, keeping a and b. If the clamped color falls out of gamut, scale a and b toward 0 until it fits.
  - **Threading:** run off main on the B10 decoder, keeping the `WeakHashMap` cache.
- **Steps:**
  1. Write the pure `clampAccent(lab)`.
  2. Write the extractor.
  3. Swap the call sites.
- **Acceptance criteria:** on fixture covers (dark, pastel, saturated, greyscale, two-tone), the accent has L\* in [70, 80] and C\* ≥ 20, or is Transparent. Every accent is at least 4.5:1 on black.
- **Tests:** `MediaAccentTest`, with fixture bitmaps generated in-test.
- **Depends on:** B10 (threading), V5.1.
- **Effort/Risk:** S / low (adds one small dependency).
- **Ships as:** 1 PR, "Brighter, cleaner media accent".
- **Design check:** the same accent *uses*: outline, top gradient, equalizer and slider tint. The extraction is better; there's no new color system.

### V5.3: Legibility scrims (4.5:1 or better behind text)
- **Goal:** white text stays at 4.5:1 or better on bright album art and on glass cards over bright wallpaper.
- **Why:**
  - 02 R5. Glass cards fade to 30% black with blur on (`IslandView.kt:1076`).
  - The art backdrop is 0.75 art under a 0.38 scrim, with no top scrim under the title (`:4680-4688`).
- **Files:** `island/IslandView.kt:1073-1082, 4663-4711`
- **Spec:**
  - **Glass:** bottom alpha `max(0.55, current)` behind text regions. The gradient becomes `0.92 → 0.74 → 0.55` with blur, and `0.92 → 0.82 → 0.72` without it.
  - **Art backdrop:**
    - On the blurred bitmap, which is already computed, measure *linear* relative luminance over the top 40% and the bottom 40%, as `Y_top` and `Y_bot` (max over the 8×8 sampled cells, not the mean).
    - Art is drawn at α = 0.75 over black, so `Y1 = 0.75·Y`.
    - The scrim alpha needed for a composite at or below 0.18 is `a = (1 − 0.18/Y1).coerceIn(0.38, 0.85)`, applied per region as a vertical gradient (top a, bottom a).
    - At `Y2 ≤ 0.18`, white text has `(1.05)/(0.18 + 0.05) = 4.57:1`.
    - Glass mode uses the same formula, with `0.42·Y` for `Y1`.
  - **Top title scrim:** a black gradient over the top 40%, at the computed top alpha.
- **Steps:**
  1. Write the pure `scrimAlphaFor(Y, artAlpha)`.
  2. Compute the luminance with the blur (off main, B10).
  3. Swap the brushes.
- **Acceptance criteria:**
  - On a white cover and on a white wallpaper with glass, the contrast measured from a Roborazzi pixel sample under the title and the artist is ≥ 4.5:1.
  - Dark covers look unchanged (scrim stays at its minimum, 0.38).
- **Tests:** `ScrimMathTest`, plus VD0.1 white-cover and white-wallpaper states with a pixel-contrast assertion.
- **Depends on:** V5.2, B10.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Readable text on bright art and glass".
- **Design check:** same backdrop and glass look on typical content; only bright content gets darker. "Liquid glass" keeps its name.

---

## V6: Edge polish

### V6.1: Adaptive keyline
- **Goal:** an optional 1 dp keyline, tinted with the activity accent, separates the black island from dark backgrounds. It turns on automatically on dark wallpapers or in night mode when the user picks "Auto".
- **Why:** 02 R10. The outline today is opt-in, 1.5 dp, the same on light and dark wallpapers. `outlineColorArgb` is never read by the island; a grep of `island/` for `outlineColorArgb` returns 0 hits.
- **Files:**
  - New: `island/layout/Keyline.kt`
  - `island/IslandView.kt:1332-1339` (card 1), the card 2 shell (`:1490-1650`), `:1704-1712` (bubble), `:1839-1847` (pill)
  - `model/IslandSettings.kt:26-28`; `data/SettingsRepository.kt`
  - New: `service/WallpaperTone.kt`
- **Spec:**
  ```kotlin
  fun Modifier.islandKeyline(shape: Shape, color: Color, width: Dp = 1.dp): Modifier =
      drawWithCache {
          val inset = width.toPx() / 2f
          val outline = shape.createOutline(size - Size(inset * 2, inset * 2), layoutDirection, this)
          val path = Path().apply { addOutline(outline); translate(Offset(inset, inset)) }
          onDrawWithContent { drawContent(); drawPath(path, color, style = Stroke(width.toPx())) }
      }
  ```
  - **Color:**
    - Normally `accent.copy(alpha = 0.20f)`, where accent is the activity's semantic role or the media accent, and `White.copy(0.12f)` when there's none.
    - With high contrast, `White.copy(0.55f)` at 1.5 dp. This is the B5 token.
    - In manual mode, `Color(outlineColorArgb)`, which finally wires up the setting.
  - **Setting:** `keylineMode: OFF | AUTO | ALWAYS`.
    - Migrate `showOutline = true` to `ALWAYS` and `false` to `OFF`, so every user's look is preserved.
    - New installs default to `OFF`. The CP3 question is whether the default becomes `AUTO`.
  - **Auto:**
    - On API 31+: `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` (or `FLAG_LOCK` while locked) with `colorHints and HINT_SUPPORTS_DARK_TEXT == 0`.
    - On API 29–30: `primaryColor.luminance() < 0.3`.
    - Also on whenever `UI_MODE_NIGHT_YES`.
    - Listen with `addOnColorsChangedListener`. No permission is needed.
  - Replaces the four `Modifier.border(...)` calls. The stroke sits fully inside the clip, so its width is now even all round.
- **Steps:**
  1. Write the modifier.
  2. Write the `WallpaperTone` flow.
  3. Add the setting, with migration and a three-way segmented control in the Appearance page next to the outline row.
  4. Swap the borders.
- **Acceptance criteria:**
  - `OFF` is pixel-identical to today's no-outline look.
  - `ALWAYS` draws an even 1 dp line, with no half-clipped stroke at squircle corners.
  - `AUTO` toggles within 1 s of a wallpaper change.
  - High contrast always shows the 55% line.
- **Tests:** `KeylineModeMigrationTest`; `WallpaperToneTest` (hint logic); VD0.1 dark-backdrop states with each mode.
- **Depends on:** V2.1 (shapes), V5.1 (roles), B5.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Adaptive keyline".
- **Design check:** off by default, so the borderless black island stays the default look. The tint follows Droidbubble's own accents; this isn't a copy of `keylineTint`.

### V6.2: Consistent high-contrast outlines everywhere, including the notch
- **Goal:** the pill, bubble, card 1, card 2 and notch bridge all draw the same outline from one resolver, at the same width, with the same fade-in rule.
- **Why:**
  - 02 R2 and B5 follow-up. The bubble border only appears past 70% emergence (`IslandView.kt:1705`).
  - The notch pill has no outline at all (`!settings.isNotch`, `:1840`).
  - Each surface computes its own alpha.
- **Files:** `island/IslandView.kt:541-567, 1704-1712, 1798-1847`; card 2 shell
- **Spec:**
  - `IslandOutline.resolve(surface, priority, keylineMode, highContrast): OutlineSpec(color, width, visibleFrom)`.
  - The alpha fades with each surface's emergence (`compactPillAlpha` for the pill, bubble emergence for the bubble).
  - **Notch:** stroke the pill's bottom and sides plus the bridge's ears only, never the top edge, using a path variant of `notchBridgePath`.
- **Steps:** write the resolver, replace the four inline color blocks, then add the notch path.
- **Acceptance criteria:** with high contrast on, every visible surface shows a 1.5 dp line at 55% white, including the notch style. There are no doubled lines where the bubble overlaps the pill.
- **Tests:** `OutlineResolverTest`; VD0.1 high-contrast states for pill, notch and dual.
- **Depends on:** V6.1.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "One outline rule".
- **Design check:** the same thin line, now consistent. Notch style keeps its shape.

---

## V7: Icon consistency (the base for epic I)

**Principle:** one icon family and weight, sized to a grid and optically centred. Epic I builds its effects on this; V7 never animates.

### V7.1: `IslandIcons` catalogue in one family (Rounded)
- **Goal:** every island glyph comes from one `IslandIcons` object using `Icons.Rounded.*`. All 34 `Icons.Default.*` (Filled) uses in the island are gone.
- **Why:** 02 R6. The island mixes Filled (34) and Rounded (21); for example, MusicNote is Filled in compact (`IslandView.kt:2785`) and Rounded in expanded (`:3453`).
- **Files:**
  - New: `island/IslandIcons.kt`
  - `island/IslandView.kt` (every `Icons.Default.` hit: `:2052, 2069, 2097, 2114, 2142-2144, 2503, 2576, 2606, 2707, 2735, 2755, 2785, 2797, 2816-2818, 2844, 3271, 3793, 3908, 3923, 4021, 4290, 4379-4383, 4539, 4571, 4602`)
  - `island/LiveActivities.kt` (already Rounded; route through the catalogue)
- **Spec:**
  - `object IslandIcons { val Notification = Icons.Rounded.Notifications; val Flashlight = Icons.Rounded.FlashlightOn; ... }`, with semantic names (`WeatherSun`, `NetDown` and so on). The weather mapping moves here too (`:2815-2819`, `:4378-4384`).
  - The catalogue is the seam epic I replaces with `DynamicIcon`. Phase 7's `Slot.Icon(icon: IslandIcon)` maps onto it.
- **Steps:**
  1. Write the catalogue.
  2. Replace the uses mechanically.
  3. Review every glyph that differs in the Rounded set.
- **Acceptance criteria:** `grep "Icons.Default" island/` returns 0 hits. The S2 diffs show only the Filled → Rounded glyph changes.
- **Tests:** `IslandIconsTest`: no catalogue entry is from `Icons.Filled` (a reflection check on the `ImageVector.name` prefix).
- **Depends on:** none (F2 recommended).
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "One icon family".
- **Design check:** the same Material symbols, now in one style. No SF Symbols look-alikes are introduced.

### V7.2: Icon size tokens and optical centering
- **Goal:** compact uses 2 sizes, not 6. Header and control glyphs follow a ratio, and asymmetric glyphs are optically centred.
- **Why:** 02 R6. Compact uses 14, 15, 16, 17, 18 and 19 dp glyphs, plus 20 dp art (`IslandView.kt:2710, 2723, 2738, 2750, 2788, 2800, 2825, 2847`).
- **Files:** `island/IslandTokens.kt`; `island/IslandIcons.kt`; the compact, bubble and header call sites
- **Spec:**

  | Token | Size |
  |---|---|
  | `IconCompact` | 18 dp (glyph) / 20 dp (art, avatar cap) |
  | `IconBubble` | 16 dp |
  | `IconHeader` | 0.55 × tile (22 at a 40 tile) |
  | `IconControl` | 0.46 × control (the existing rule) |

  **`Modifier.opticalCenter(glyph)`** applies a per-glyph offset table in fractions of glyph size:

  | Glyph | Offset |
  |---|---|
  | play | x +0.06 |
  | next / previous | x ±0.03 |
  | download arrow | y +0.03 |
  | bell | y +0.02 |
  | mic | y −0.02 |
  | others | 0 |

  Values come from the blur test (blur the glyph at 4 dp and compare its centroid to the box centre), recorded in `IslandIcons`.
- **Steps:** add the tokens, swap the sizes, then add the offsets one glyph at a time with before/after crops.
- **Acceptance criteria:** compact glyphs come only in `IconCompact` and `IconBubble` sizes. The play glyph's centroid is within 0.5 dp of its circle's centre.
- **Tests:** `OpticalCenterTest`: render each glyph in Robolectric, compute the alpha centroid and assert ≤ 0.5 dp error after the offset.
- **Depends on:** V7.1.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Icon sizes and optical centering".
- **Design check:** same glyphs, sized consistently. **Checkpoint CP4.**

### V7.3 *(optional; decide with the epic I agent and the owner)*: Material Symbols Rounded drawables
- **Goal:** replace `material-icons-extended`, which Google no longer publishes, with exported Material Symbols Rounded vector drawables: weight 500, grade −25, optical size 20 for compact and 24 for expanded, FILL 1 for active states.
- **Why:** 02 R6. It cuts APK size, and I2's AVD morphs need path-compatible drawables.
- **Files:** `res/drawable/ic_island_*.xml`; `island/IslandIcons.kt` (now `ImageVector.vectorResource`); `app/build.gradle.kts:89` (remove the dependency)
- **Spec:** one drawable per catalogue entry. The names follow the semantic catalogue. Keep the V7.2 optical offsets, re-measured.
- **Steps:**
  1. Export the drawables.
  2. Swap the catalogue backing.
  3. Remove the dependency.
  4. Measure the APK.
- **Acceptance criteria:** visual parity within the expected weight change. The APK is smaller (record before and after). No other code changes, because the catalogue is the seam.
- **Tests:** `OpticalCenterTest` re-run; S2.
- **Depends on:** V7.2; agreement with epic I (I2's morph sources).
- **Effort/Risk:** M / medium (glyph and name differences).
- **Ships as:** 1 PR, "Material Symbols drawables".
- **Design check:** Android's own icon system at the weight the research recommends. Do it only if the owner agrees that the slightly different stroke is fine.

---

## D1: Liquid split, merge and drip (AGSL SDF shader)

**Principle:** the island is one body that buds, stretches and re-absorbs. Android-native: an AGSL `RuntimeShader` signed-distance smooth union. It runs only during transitions, is API 33-guarded, and content is always drawn above it, unfiltered.

**Shader outline** (`island/liquid/LiquidBody.agsl`, loaded as a `String` constant):
```glsl
uniform float2 uSize;          // layer size, px
uniform float4 uPill;          // center.x, center.y, halfW, halfH (px)
uniform float  uPillR;         // corner radius px (= halfH: capsule, exact match with SquircleShape at a capsule)
uniform float4 uBubble;        // center.x, center.y, halfW, halfH (oval bubble from V3.3)
uniform float  uBubbleR;       // = bubble halfH
uniform float  uK;             // smooth-min radius px; 0 = hard union
uniform float  uBulge;         // merge bulge px added to the pill's halfW
uniform float  uAA;            // anti-alias half-width px (~0.75)
uniform float  uOutlineW;      // keyline width px (0 = none)
layout(color) uniform half4 uColor;        // island black
layout(color) uniform half4 uOutlineColor; // V6 keyline color

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
float smin(float a, float b, float k) {           // polynomial smooth-min (quadratic)
    if (k <= 0.0) return min(a, b);
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}
half4 main(float2 xy) {
    float dP = sdRoundBox(xy - uPill.xy,   uPill.zw + float2(uBulge, 0.0), uPillR);
    float dB = sdRoundBox(xy - uBubble.xy, uBubble.zw,                      uBubbleR);
    float d  = smin(dP, dB, uK);
    float fill = 1.0 - smoothstep(-uAA, uAA, d);
    float ring = (uOutlineW > 0.0) ? 1.0 - smoothstep(-uAA, uAA, abs(d + uOutlineW * 0.5) - uOutlineW * 0.5) : 0.0;
    return mix(uColor * fill, uOutlineColor, ring * uOutlineColor.a);
}
```

**Neck maths:**
- Halfway between the facing edges, `a ≈ b ≈ gap/2` and `h = 1`, so `d = gap/2 − k/4`.
- A bridge exists exactly while **`gap < k/2`**. With `kMax = 10 dp`, the neck snaps at **gap = 5 dp**, which equals today's `snapGap` (`IslandView.kt:1765`), so the timing feel is preserved.
- **After the snap**, ramp `k` down so the facing sides don't stay inflated: `k = kMax · (1 − smoothstep(kMax/2, 1.5·kMax, gap))`. Because `k` only falls once `gap > k/2`, the bridge can never re-form.
- **Merge:**
  - `uK` ramps 0 → `kMax` as the gap closes from `1.5·kMax` to 0.
  - `uBulge = 3 dp · bump(t)`, where `bump` is a `merge`-token spring impulse that peaks once and settles.
  - The bubble's halfW shrinks into the pill.

### D1.1: Liquid SDF: pure Kotlin mirror plus the shader asset
- **Goal:** the shader source plus a pure Kotlin mirror of `sdRoundBox`, `smin` and the k schedule, so the maths is unit-tested without a GPU.
- **Why:** 03 R7. Today's neck is a quadratic Canvas approximation (`IslandView.kt:1758-1796`) that never deforms the bubble (03 P2).
- **Files:**
  - New: `island/liquid/LiquidSdf.kt` (pure)
  - New: `island/liquid/LiquidBodyShader.kt` (`@RequiresApi(33)` wrapper: `RuntimeShader(SRC)`, `setFloatUniform`, `setColorUniform`, `ShaderBrush`)
- **Spec:**
  - The uniforms are as above.
  - `LiquidSdf.neckPresent(gapPx, kPx)`, `LiquidSdf.k(gapPx, kMaxPx)`, `LiquidSdf.d(x, y, params)`.
  - The wrapper caches one `RuntimeShader` per window. It only sets uniforms per frame, with no allocations.
- **Steps:** write the pure maths and tests, the shader string and the wrapper. No wiring yet.
- **Acceptance criteria:**
  - The pure maths reproduces "bridge iff gap < k/2".
  - The shader compiles on an API 33+ device (a debug-panel smoke button renders a static split frame).
- **Tests:** `LiquidSdfTest`:
  - capsule SDF sign at sample points
  - the snap at 5 dp
  - k monotonic after the snap
  - no re-bridge for any decreasing-k sequence
  - bulge symmetric
- **Depends on:** none (pure). The shader needs API 33; see decision D1.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Liquid SDF maths + shader (unused)".
- **Design check:** no visual change yet. SDF metaballs are a general graphics technique, not an Apple asset.

### D1.2: Split and merge drawn with the shader
- **Goal:** during split and merge, one shader layer draws the pill, bubble and neck as one black body. At rest, the plain squircle shapes draw as today.
- **Why:** PLAN §2 "One living shape"; 03 §6 Split and Merge timelines.
- **Files:** `island/IslandView.kt:1666-1796` (bubble and neck), `:1822-1847` (pill background); the M1/M2 geometry readers
- **Spec:**
  - **Active window:** `transitioning = isDual && !isExpanded && (gap in −bubbleW..1.5·kMax || mergeAnimating)`.
  - **While active:**
    - The pill and bubble Boxes draw a transparent background.
    - One `Spacer(Modifier.drawWithCache { onDrawBehind { drawRect(brush) } })` sized to the union bounds plus kMax sits **below** both content layers.
    - Pill and bubble content stay in their own unfiltered layers.
  - **Uniforms** are read in the draw phase from M2's geometry, giving 0 recompositions per frame.
  - **Merge:** the bubble content fades out over 60 ms (the `effects` token) before contact, and the old fade-under is removed (`:1677-1686`).
  - **Fallbacks:** on API < 33, the notch style, the Reduced tier, or Battery Saver (`AudioPowerPolicy.lowPower`), keep today's neck Canvas, unchanged.
  - **Outline:** with a keyline, `uOutlineW` and `uOutlineColor` come from V6, so the neck carries the line continuously.
- **Steps:**
  1. Wire the brush behind the content.
  2. Gate it by `transitioning`.
  3. Write the merge choreography.
  4. Delete the Canvas neck from the Full tier path only.
- **Acceptance criteria:**
  - In a 120 fps screen recording, the neck thins smoothly and snaps at about 5 dp, the bubble shows slight deformation at the snap, and the merge bulges about 3 dp and settles.
  - There are no seams between the shader and the squircle at rest (switch frames identical within AA).
  - JankStats over 20 splits shows no frames above budget compared with S4.
- **Tests:**
  - Robolectric: the gate is off at rest and in the Reduced tier, and the fallback path is used on API 32 (`@Config(sdk = [32])`).
  - `LiquidSdfTest` covers the maths.
  - Macrobenchmark split/merge compared with S4.
- **Depends on:** D1.1, M1, M2, M5, V3.3 (oval bubble widths).
- **Effort/Risk:** M / high (GPU, OEM drivers). Mitigate with the fallback, plus a debug toggle "Liquid shader" in the Test panel.
- **Ships as:** 1 PR, "Liquid split and merge".
- **Design check:** the liquid-neck concept Droidbubble already has (PLAN §1 "keep"), made smooth. The shapes at rest are unchanged squircle capsules. **Checkpoint CP5** (video).

### D1.3 *(optional, after CP5)*: Drip, so the card buds out of the pill on expand
- **Goal:** for the first part of an expand (p < 0.35), a short liquid stem joins the pill at the camera to the card dropping below the status bar. It snaps, and the stem retracts into the camera.
- **Why:** 03 §5 "it drips into a card below the status bar". This keeps the detached card (02 §5) while reading as one body.
- **Files:** M1's geometry renderer; `island/liquid/*`
- **Spec:**
  - Only the **neck band** uses the shader: a clip band from the pill bottom to the card top, with `smin(capsule(pill), roundBox(cardTopStrip))`. The card itself stays `SquircleShape`, because the SDF round box is circular-cornered and would mismatch at the corners.
  - Use `kMax = 14 dp`. The stem snaps when the card top is more than 7 dp below the pill bottom (at p ≈ 0.25 at default speed).
  - **Collapse** reverses this, with the stem re-forming at p < 0.2.
  - Off in Reduced, Efficient, notch style and on API < 33.
- **Steps:** add the band shader in the M1 geometry draw, behind a flag; capture video; review.
- **Acceptance criteria:** no visible corner mismatch at the band edges in frame strips; the snap is ≤ 1 frame; it's interrupt-safe (expand → collapse mid-drip is continuous).
- **Tests:** a geometry test that the band is inside the union bounds for all p; Robolectric gate tests.
- **Depends on:** D1.2, M1–M3.
- **Effort/Risk:** M / high.
- **Ships as:** 1 PR behind a "Drip (experimental)" debug flag, enabled after owner approval.
- **Design check:** the detached squircle card is unchanged. The drip is Droidbubble's own idea (Apple's island doesn't detach).

---

## D2: Alert behaviours

### D2.1: The alert bump (a width impulse) replaces `swapSquash`
- **Goal:** when the compact activity changes, the pill gets a short width impulse on a spring with velocity, instead of the 110 ms tween plus spring that scales the whole pill and its content.
- **Why:**
  - 03 P2 and R10: wall-clock tweens aren't interruptible, and `scaleX` stretches the text.
  - `IslandView.kt:578-588` triggers it; it's used at `:1815`, `:1834-1835` and `:2245` (the blur).
- **Files:** `island/IslandView.kt:575-588, 1815, 1834-1835, 2245`; the M1 geometry
- **Spec:**
  - The token is `IslandMotion.alertBump = (d 0.30 s, b 0.45)`, so ζ = 0.55 and ω = 2π/0.30 = 20.9 rad/s at speed 1.
  - The peak should be `+6%` of the width W. For an impulse response the peak is `x_peak = (v0/ω)·exp(−ζ/√(1−ζ²) · atan(√(1−ζ²)/ζ)) = (v0/ω)·0.522`, so **`v0 = 0.06·W·ω/0.522 ≈ 2.40·W per second`** (about 432 dp/s for W = 180), scaled by `speed`.
  - Implementation: `geometry.animateTo(target, alertBump, initialVelocity = geometry.velocity + IslandGeometry(dWidth = v0))`. Because the shape widens in layout, content isn't stretched.
  - The leaving-content blur reads `|widthVelocity|/v0` instead of `swapSquash`.
  - **Reduced:** no bump; the content cross-fade only.
- **Steps:** add the impulse; remove `swapSquash` and its three readers; retune the blur input.
- **Acceptance criteria:**
  - The peak widening is 6% ± 1% (frame capture).
  - The text isn't stretched.
  - Two activity changes 100 ms apart produce one continuous motion, not a restart.
- **Tests:** `AlertBumpMathTest` (v0 formula against a numeric spring sim); Robolectric: Reduced gives no velocity applied.
- **Depends on:** M1, M2, F3.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Alert bump on a real spring".
- **Design check:** the same "breath" Droidbubble already has, with better physics; the numbers are its own.

### D2.2: Banners briefly expand, then recede
- **Goal:** an alerting update (a new message banner, or a T0/T3 alerting spec) peeks open into its card for `IslandTimings.peek`, then recedes. It recedes to compact, or into the bubble when a live activity holds the pill.
- **Why:** PLAN D2; 03 §6 Alert. Banners should never cover a live activity (A2.3).
- **Files:** `island/IslandStateManager.kt` (a peek state); `island/IslandView.kt` (geometry target); `service/NotificationReceiverService.kt` (alerting flag)
- **Spec:**
  - **Peek:** `expandPeek(id)` sets the geometry target to the card, with the content progress capped by M3.
  - **Duration:** `IslandTimings.peek = AccessibilityManager.getRecommendedTimeoutMillis(2500, FLAG_CONTENT_TEXT)`.
  - Then `collapse` with the **collapse** token, or, if A2's arbiter says a T1 activity is primary, the split token into the bubble.
  - **Cancelling:** any touch on the island cancels the recede, and it becomes a normal expand.
  - **TalkBack:** no auto-recede while the island has accessibility focus (C3 owns the focus signal).
  - No peek while the screen is off, in fullscreen (A6 policy), or within 5 s of the previous peek from the same app.
- **Steps:** add the peek state machine (pure, testable), wire the NRS alerting flag, then add the geometry targets.
- **Acceptance criteria:**
  - A message peeks and then recedes without a hard cut.
  - With a running timer, the banner recedes into the bubble and the timer keeps the pill.
  - A touch during the peek keeps it open.
- **Tests:** `PeekStateMachineTest` (timings, cancel, cooldown, focus-hold); `IslandStateBehaviorTest` additions.
- **Depends on:** M1, M3, F3 (`IslandTimings`); A2.3 for recede-into-bubble (until then it recedes to compact).
- **Effort/Risk:** M / medium (behaviour change; owner check at CP5).
- **Ships as:** 1 PR, "Banners peek, then recede".
- **Design check:** existing cards and pill only. The timing is accessibility-scaled (Android); not Apple's choreography numbers.

### D2.3: "Same app, new event" signal (the bounce trigger)
- **Goal:** detect when a new event arrives from the activity already shown (a new message in the same chat, the next turn) and emit an `IslandPulse(id)`. Epic I (I3) turns it into `symbolBounce`, and this task adds a small shape bump.
- **Why:**
  - PLAN D2 "`symbolBounce` on same app, new event".
  - **Overlap with I3** ("New message from the same app → bounce"): I3 owns the icon effect; D2.3 owns detection and the shape coupling only.
- **Files:** `island/IslandStateManager.kt:542-614` (`updateNotification`); new `island/IslandPulse.kt`
- **Spec:**
  - Pulse when `sameKey || samePackage && sameConversation` **and** the text or `postTime` changed **and** it isn't a clock tick. Reuse Phase 7's `stateSignature` when it exists; until then, compare `text` (today's rule at `IslandStateManager.kt:552`).
  - **Throttle:** at most one pulse per 600 ms per id.
  - **Shape coupling:** a D2.1 impulse at 3% instead of 6%.
- **Steps:** write the pure detector; expose a `SharedFlow<IslandPulse>`; apply the impulse; hand the flow to I3.
- **Acceptance criteria:** three WhatsApp messages give three pulses; timer ticks give zero; no pulse on the first appearance (that's an appear, not a bounce).
- **Tests:** `IslandPulseDetectorTest`, using S5 message fixtures.
- **Depends on:** D2.1; I1/I3 for the icon effect (the signal ships alone).
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Same-app event pulse".
- **Design check:** no new visuals; the icon effect is epic I's.

### D2.4: Haptics at physical moments
- **Goal:** a low tick when the neck snaps, a tick when the bubble merges, and a click on expand (exists). All go through `IslandHaptics`.
- **Why:** 03 §5 point 3; PLAN D2.
- **Files:** `island/IslandView.kt:1197-1215` (`triggerHaptic` family); D1.2 snap and merge callbacks; F3 `IslandHaptics`
- **Spec:**

  | Moment | Primitive (API 31+) | Fallback (29–30) |
  |---|---|---|
  | neck snap | `PRIMITIVE_LOW_TICK` at 0.4 | `HapticFeedbackConstants.CLOCK_TICK` |
  | merge contact | `PRIMITIVE_TICK` at 0.5 | `CLOCK_TICK` |
  | expand | existing click | — |

  - The snap fires once per split, when the gap crosses k/2; the merge fires when the gap reaches 0. Detect both in the draw-phase reader with a state latch, and post the haptic with `View.post`.
  - It respects `hapticsEnabled`, the Reduced tier, and a 150 ms throttle.
- **Steps:** add the two events in `IslandHaptics`; latch the crossings; wire the calls.
- **Acceptance criteria:** one haptic per snap and per merge on a device, with no repeats on spring oscillation.
- **Tests:** `CrossingLatchTest` (oscillating gap sequences give exactly one event).
- **Depends on:** D1.2 (or today's neck, which has the same gap values), F3.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Haptics for the liquid moments".
- **Design check:** no visual change. Android haptic primitives.

---

## D3: Numbers everywhere

### D3.1: Rolling digits and `NumericSlot` in compact and the bubble
- **Goal:** every live number in compact and the bubble rolls with `RollingNumberText` inside a fixed `NumericSlot`: %, W, °, FPS, speed and ETA.
- **Why:** 03 P2 (rolling is missing on %, W, ° and network speed); 01 finding 3.
- **Files:** `island/IslandView.kt:2011-2018` (W), `:2029-2032` (battery %), `:2917-2924` (progress %), `:2940-2946` (FPS), the network and weather trailing slots, and the bubble at `:2532-2537, 2585-2601`
- **Spec:**

  | Value | Template | Role | Direction |
  |---|---|---|---|
  | progress % | "100%" | compactValue + compactUnit | up |
  | battery % | "100%" | compactValue | up while charging |
  | wattage | "00.0W" | compactValue | — |
  | FPS | "000" | compactValue | — |
  | speed | "00.0" + unit slot "MB/s" | compactValue + compactUnit | — |
  | temperature | "−00°" | bubbleValue | — |

  - `RollingNumberText` gains an optional `slotTemplate`, so width comes from the template, not the current value. The existing digit-free path (`LiveActivities.kt:416-418`) stays.
  - **Reduced:** the roll becomes a 150 ms cross-fade (M5 flag inside `RollingNumberText`).
- **Steps:** extend `RollingNumberText`; swap the plain `Text`s; add the Reduced path.
- **Acceptance criteria:** none of these values changes width when it ticks (with V3.1's stability test extended). Each digit rolls in the counting direction.
- **Tests:** `RollingSlotTest` (template width ≥ every value's width); VD0.1 states.
- **Depends on:** F4 (`NumericSlot`), V3.1, M5.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Rolling numbers in the pill and bubble".
- **Design check:** `RollingNumberText` is Droidbubble's house signature (01 §5), not Apple's blur-morph.

### D3.2: Rolling digits on expanded live values
- **Goal:** the same treatment for expanded cards: network speeds, FPS, weather temperatures, battery and charging values, ETAs.
- **Why:** 03 P2; 01 R2. The `TextStyle` escapes are at `IslandView.kt:4579, 4585, 4610, 4616`; B9 restored `tnum`, and this task adds the roll.
- **Files:** `island/IslandView.kt:4260-4650` (FPS, weather and network views); `island/LiveActivities.kt` rows whose hero is already rolling (verify only)
- **Spec:** heroes use `hero` or `statValue` roles plus templates ("000" FPS, "0000 KB/s" → V3.2's 3-significant-digit format, "−00°"). Weather updates roll only on a real change.
- **Steps:** swap each value; verify the LiveLayout heroes already roll.
- **Acceptance criteria:** no expanded value changes width when it ticks; S2 is unchanged at rest.
- **Tests:** VD0.1 expanded states before and after a value change (two frames).
- **Depends on:** D3.1, V4.2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Rolling numbers in cards".
- **Design check:** same cards and same type roles.

### D3.3: Spring on progress rings and bars
- **Goal:** the bubble progress ring and the compact indeterminate-to-determinate switch animate on a spring, instead of jumping.
- **Why:** 03 P2; the bubble ring is at `IslandView.kt:2525-2531`.
- **Files:** `island/IslandView.kt:2510-2540` (bubble), `:2906-2925` (compact); `:3977-3980` (expanded, already `spring(0.9, 140)`, which moves to the token)
- **Spec:**
  - Use a new `IslandMotion.progress = (d 0.5 s, b 0.1)`, so ζ = 0.9 and k = 158 at speed 1. This replaces the literal `0.9/140` too.
  - **Backward jumps** (a new download, progress decreasing by more than 0.2) snap instead of spring.
  - Indeterminate to determinate: the ring cross-fades over 150 ms.
  - The sweep is read in the draw phase (`drawArc` with `animatable.value`), not in composition.
- **Steps:** add the token; animate the bubble ring; move the expanded literal.
- **Acceptance criteria:** 42% → 57% glides with no overshoot visible above 1%; a new download snaps to 0.
- **Tests:** `ProgressAnimationPolicyTest` (snap versus spring decision).
- **Depends on:** F3, M2 (draw-phase reads).
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Smooth progress rings".
- **Design check:** same rings.

---

## D4: Shared artwork and consistent placement

### D4.1: Shared art, avatar and app icon between compact and expanded
- **Goal:** during an expand or collapse, the media art, call avatar or app icon travels from its compact slot to its expanded header position as one image. Its rect and radius interpolate with `p`. It doesn't fade out and back in.
- **Why:** 03 P2 ("No shared artwork compact → expanded"); PLAN §2 "Album art is shared from compact to expanded".
- **Files:**
  - `island/IslandView.kt:2775-2792` (compact art), `:3438-3460` (expanded art), the avatar paths at `:2726-2732` and in the LiveLayout call row
  - New: `island/layout/SharedSlot.kt`
- **Spec:**
  - Both endpoints report their rects with `Modifier.onPlaced` into the root's coordinate space, as `compactRect` and `expandedRect`, captured at the *settled* layouts. The expanded rect comes from the fixed-size content box (`requiredSize(expandedCardWidth, card1ContentHeight)`, `:1456`), so it's known before the morph.
  - **One overlay image** above the shell draws at `lerpRect(compactRect, expandedRect, ease(p))`, where `ease` is the identity because `p` is already spring-driven. Its corner is `lerp(compactRadius = 10 (circle), expandedRadius = V2.3's 21.45)` as an `islandShape`.
  - While `0 < p < 1`, both endpoint images are hidden (alpha 0 via a `LocalSharedSlotActive` flag). At rest, each endpoint draws itself.
  - Interrupts are safe because `p` comes from M1's single geometry.
  - **The cover flip** (`FlippingCoverArt`) runs on the overlay when the track changes mid-morph.
  - **Reduced:** the art cross-fades in place, with no travel.
- **Steps:** write the rect capture, the overlay, the endpoint hiding and the Reduced path.
- **Acceptance criteria:** in a 120 fps capture, the art never disappears or doubles during expand or collapse. Interrupting at p = 0.5 reverses smoothly.
- **Tests:** `SharedSlotMathTest` (lerp endpoints, radius at p 0 and 1); Robolectric: endpoints are hidden only while `0 < p < 1`.
- **Depends on:** M1, M2, M3, V2.3 (radii), V3.1 (compact rects).
- **Effort/Risk:** M / medium.
- **Ships as:** 1 PR, "Shared artwork across compact and expanded".
- **Design check:** the same art in the same two places, now continuous. Implemented with Droidbubble's own geometry progress, not a transition API clone.

### D4.2: Consistent placement audit
- **Goal:** for every activity, the compact leading element maps to the expanded leading (top-left) element and the trailing value maps to the expanded top-right. Fix only mismatches that can be solved by *swapping existing elements*. Document the rest.
- **Why:** 02 §1 and 03 §1: keep elements in the same relative place.
- **Files:** `island/IslandView.kt` (compact and expanded views); `island/LiveActivities.kt` rows
- **Spec:** an audit table in the PR (activity → compact L/R → expanded TL/TR → OK/fix). Examples:
  - media: art L, equalizer R → art TL, equalizer TR (OK)
  - progress: icon L, % R → tile TL, % chip TR (verify)
  - FPS, weather, network: icon L, value R → tile TL, value TR (verify)
  - timer and call rows: ring/avatar L, time R (verify in LiveLayout)
  
  Fixes may move an existing element within its card's existing slots, never add or remove one.
- **Steps:** build the table from VD0.1 screenshots; apply the swap fixes, one commit each.
- **Acceptance criteria:** the table is complete; every "fix" row is resolved or explicitly deferred with the owner's sign-off at CP5.
- **Tests:** the VD0.1 diffs for the changed cards.
- **Depends on:** D4.1, V4.2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Consistent placement".
- **Design check:** no card is redesigned; at most an existing element moves within its card.

---

## D5: Curve editor v2

**Principle:** what you preview is what you get. The model speaks Duration and Bounce, a perceptual pair. Android-native: Compose `spring(dampingRatio, stiffness)` underneath, with F3's conversion.

**Conversion (single source, F3):**
- Forward:
  - `k = (2π/d)²`
  - `ζ = 1 − b` for `b ≥ 0`
  - `ζ = 1/(1 + b)` for `b < 0` (overdamped)
  - The effective spring at run time is `spring(ζ, k·speed²)`.
- **Inverse (the migration):**
  - **`d = 2π/√k`**
  - **`b = 1 − ζ`** for `ζ ≤ 1`, or `b = 1/ζ − 1` for `ζ > 1`
- Today's slider writes `ζ = 1 − 0.7·bounce%` (`MainScreen.kt:2785`), so the stored `ζ` lies in [0.3, 1] and `b` in [0, 0.7]. Clamp `b` to the new range [−0.2, 0.5]. Stored `k` lies in 150–700, so `d` lies in [0.237, 0.513] s.

**What each user curve drives today vs. v2:**

| Today's label | Actually drives (verified) | v2 name | v2 drives (F3 tokens) |
|---|---|---|---|
| Expand | card morph and progress while expanding (`IslandView.kt:612-615, 647-651`) | **Expand** | `expand` |
| *(none; derived 1.4·k, ζ 1 at `:573`)* | collapse | **Collapse** (new) | `collapse` |
| Appear | only the charging, unlock, Bluetooth and ringer morph (`:607-611, 642-646`) | merged into **Appear** | `appear` (pill out of the camera and alert widen); `dismiss` derived |
| Split | pill appear, widen, hide and minimise (`:675-685`) **and** bubble emerge (`:1004-1008`) | **Appear** and **Split** | `split` (bubble buds); `merge` derived |

**Migration values** (defaults, at speed 1; effective = d/speed, so at today's default 0.7×: expand 0.46 s, collapse 0.39 s, appear 0.50 s):

| v2 curve | Source | d (s) | b |
|---|---|---|---|
| Expand | expand 0.80 / 380 | 2π/√380 = **0.322** | **0.20** |
| Collapse | derived k = 532, ζ 1 | 2π/√532 = **0.272** | **0** |
| Appear | old *split* 0.63 / 320, because that's what users see when the pill appears | 2π/√320 = **0.351** | **0.37** |
| Split | old split 0.63 / 320 | **0.351** | **0.37** |
| *(old Appear 0.60 / 280: d 0.376, b 0.40)* | dropped; alert pills now ride Appear (0.351 / 0.37), a difference under 7% | — | — |

- **Derived:**
  - `dismiss = (0.76·d_appear, b = 0)`
  - `merge = (0.78·d_split, b = min(0.05, b_split))`
  - These are 03 §6's ratios (0.32/0.42 and 0.35/0.45).

### D5.1: Duration/Bounce settings model and migration
- **Goal:** settings store `(duration, bounce)` for Expand, Collapse, Appear and Split. A one-time migration converts the stored ζ/k pairs, and F3 reads the new fields.
- **Why:** 03 P1-3 and R9; PLAN D5 "migration of stored values".
- **Files:** `model/IslandSettings.kt:63-68`; `data/SettingsRepository.kt:66-71, 129-134, 201-206, 264-269`; F3's `IslandMotion` user-curve inputs
- **Spec:**
  - New fields: `expandDuration`, `expandBounce`, `collapseDuration`, `collapseBounce`, `appearDuration`, `appearBounce`, `splitDuration`, `splitBounce`, plus `motionSchema: Int = 2`.
  - The migration runs on read when `motionSchema < 2`, using the conversion and table above.
  - The old six keys are **kept, read-only, for one release** so a rollback is possible. They're deleted in a later cleanup.
  - Values outside the range are clamped, and a B11 breadcrumb is written.
  - **Speed:** `animationSpeed` semantics are untouched here. If F3 re-based speed (03 §6: "new 1.0 ≈ old 0.7×"), F3's migration owns that factor. D5.1 must not apply it twice. Assert this with a test that runs both migrations.
- **Steps:**
  1. Add the fields and keys.
  2. Write the pure `migrateCurves(old): NewCurves`.
  3. Repoint F3's inputs.
  4. Write the double-migration guard test.
- **Acceptance criteria:** after the upgrade, the effective `(ζ, k·speed²)` of every running spring equals the pre-upgrade value within 1% (expand, collapse and split exactly; alert pills within the documented 7%). Fresh installs get the Droid preset.
- **Tests:** `CurveMigrationTest`:
  - defaults
  - edge values (k 150 and 700; ζ 0.3 and 1.0)
  - idempotent
  - combined with F3's speed migration
  - inverse ∘ forward = identity
- **Depends on:** F3.
- **Effort/Risk:** S / medium (silent feel changes if wrong; guarded by the 1% test).
- **Ships as:** 1 PR, "Curves stored as duration and bounce".
- **Design check:** no visual change; the same motion.

### D5.2: Editor UI: Duration and Bounce sliders with an honest preview
- **Goal:** the editor speaks Duration and Bounce, previews the exact spring that will run (speed² included), and prints its settle time and overshoot in dp.
- **Why:**
  - 03 P1-3. The preview omits speed², so at the 0.7× default it's 1.43× faster than reality (`MainScreen.kt:2763`).
  - "Bounce %" isn't bounce (`:2779`).
  - The labels name the wrong transitions (`:2654`).
- **Files:** `ui/main/MainScreen.kt:2638-2796`. If C5.1 has landed, use `AnimationCurveEditor.kt`.
- **Spec:**
  - **Segments:** Expand / Collapse / Appear / Split. The selected label changes only its color and fill, with the same font weight, which fixes the width shift at `:2729` (flagged by Phase 8 C5.6).
  - **Sliders:**
    - **Duration** 0.20–0.80 s. The chip shows the *effective* value: "0.46 s at 0.7× speed".
    - **Bounce** −0.2…0.5. The label reads "None" at 0, "Gentle" up to 0.15, "Bouncy" up to 0.3, "Very bouncy" above 0.3, and there's a haptic tick at the 0.4 soft cap.
  - **Preview:**
    - Plot `springStep(ζ, k·speed², t)` over `t ∈ [0, max(1.2·t_settle, 0.3)]`, with the axis labelled in seconds.
    - **Settle time** (1% band): `t_s = ln(100/√(1−ζ²))/(ζ·ω)` for ζ < 1, with `ω = √(k·speed²)`; `t_s = 6.64/ω` for ζ = 1; `ζ > 1` is solved numerically.
    - **Overshoot:** `OS = exp(−πζ/√(1−ζ²))`, printed as dp over the transition's travel: Expand uses `expandedCardWidth − widthDp` (≈ 194 dp → 2.9 dp at the default), Appear `widthDp − H`, Split `bubbleW + 6`, Collapse 0. For example: "Settles in 0.52 s · overshoot 2.9 dp".
  - **Reduced tier active:** a banner "Reduced motion is on, so the island uses gentle fades".
  - Reset uses the selected curve's Droid preset.
- **Steps:**
  1. Write the pure `SpringReadout(d, b, speed, travelDp)` with tests.
  2. Rebuild the editor body in the same `SettingsCardItem` shell.
  3. Wire it to D5.1.
- **Acceptance criteria:**
  - A slow-motion capture of the island matches the preview curve within ±10 ms at 50% and 90% of travel.
  - TalkBack reads "Duration, 0.46 seconds at current speed".
- **Tests:** `SpringReadoutTest` (settle and overshoot against a numeric simulation within 2%); `MainScreenViewModelTest` (writes the new fields).
- **Depends on:** D5.1, M5. Coordinate with Phase 8 C5.1 and C5.6.
- **Effort/Risk:** M / low.
- **Ships as:** 1 PR, "Curve editor v2".
- **Design check:** the same card shell, segmented control style and slider rows (`SliderSettingItemPct`); only the semantics and preview change. The perceptual pair is a principle borrowed from the reference, labelled in Droidbubble's words.

### D5.3: Presets: Calm, Droid and Lively
- **Goal:** three one-tap presets for all four curves. Droid is the default, today's feel.
- **Why:** PLAN D5; 03 §5 point 4 (bounded, honest personality).
- **Files:** the editor file; `IslandMotion` (preset table)
- **Spec** (d s / b at speed 1):

  | Preset | Expand | Collapse | Appear | Split |
  |---|---|---|---|---|
  | **Calm** | 0.37 / 0.05 | 0.31 / 0 | 0.40 / 0.12 | 0.40 / 0.15 |
  | **Droid** (default, migrated) | 0.322 / 0.20 | 0.272 / 0 | 0.351 / 0.37 | 0.351 / 0.37 |
  | **Lively** | 0.29 / 0.28 | 0.25 / 0.05 | 0.32 / 0.38 | 0.32 / 0.40 |

  - "Eager in, calm out" holds in every preset: Collapse has b ≤ 0.05 and d ≤ 0.85·d_expand.
  - Any manual edit shows "Custom".
  - A "Try on island" button fires `LiveActivityTester`'s demo expand, then collapses after 1.5 s.
- **Steps:** add the table, a chip row above the segments, and the tester hook.
- **Acceptance criteria:** applying a preset writes all eight fields in one write. "Custom" detection is exact. The tester demo plays.
- **Tests:** `PresetTableTest` (eager-in/calm-out invariant; Droid equals the migration defaults).
- **Depends on:** D5.2.
- **Effort/Risk:** S / low.
- **Ships as:** 1 PR, "Motion presets".
- **Design check:** Droidbubble's own names and values (not `.snappy` or `.bouncy`). **Checkpoint CP6.**

---

## D6 *(optional)*: Orbit ring

### D6.1: Thin progress ring around the round punch-hole
- **Goal:** in the minimised state, or when the user enables "Orbit" for timers, downloads and charging, a thin accent ring hugs the circular punch-hole and shows progress.
- **Why:** 02 §5 "Orbit ring signature". It only works with round Android cutouts, which makes it a natural Android identity.
- **Files:**
  - New: `island/OrbitRing.kt`
  - `island/IslandView.kt` minimised path (`:906-908` targets; the pill box)
  - `model/IslandSettings.kt` (`orbitRing: Boolean = false`)
- **Spec:**
  - Shown only when `CameraGeometry.kind == PUNCH_HOLE`, the island style is pill (not notch), and the tier isn't Reduced. Otherwise it's hidden.
  - **Geometry:** centre at the lens, radius `d/2 + 3 dp`, stroke 2 dp with a round cap. Start at 12 o'clock and sweep clockwise. The track is `White.copy(0.12f)`, and the sweep is the activity's semantic color (V5.1).
  - **Progress:** the sweep uses the D3.3 progress token. Timers sweep continuously from the clock in the draw phase, with no spring.
  - **Minimised state:** the existing camera circle (`userMinimized`, `heightDp` wide) keeps its black fill, and the ring is drawn inside it. When the ring is shown, the circle diameter becomes `max(H, d + 2·(3 + 2 + 3))`.
  - **Window:** it's already covered by the compact bounds, so no change is needed.
- **Steps:** add the setting and the Appearance toggle (off by default); write the composable; wire progress sources.
- **Acceptance criteria:** on a centre punch-hole phone, the ring is concentric with the lens within ±1 dp. It's hidden on notch style and waterdrop phones. It costs nothing when off.
- **Tests:** `OrbitGeometryTest`; Robolectric hidden-gate tests.
- **Depends on:** V1.3, D3.3, M5, V5.1.
- **Effort/Risk:** S / medium (OEM cutout precision).
- **Ships as:** 1 PR behind the setting, "Orbit ring (optional)".
- **Design check:** an Android-only signature (Apple's island has no punch-hole ring). It's off by default, so the current look is unchanged.

---

## Design-review checkpoints (owner)

Each checkpoint is a short owner review of the VD0.1 contact sheet (baseline next to the new build), plus device photos or videos where named. For each item the owner answers **Keep / Tweak / Revert**. Record the answers in the PR that closes the checkpoint, and don't start the next track step on a "Revert".

| CP | After | What the owner sees | Questions |
|---|---|---|---|
| **CP1**: Fit and corners | V1.3, V2.3 | (a) Device photos on at least 2 phones (centre hole and side hole) with the debug camera ring on, before and after. (b) VD0.1 expanded states: media, notification with actions, weather, FPS, network, progress, flashlight, at s 0.8, 1.1 and 1.3. (c) Crops of the media art, tiles, panels and buttons. | Is the pill now on the lens? Height `max(36, d+12)` or the research's `d+16`? Do the 21 dp art corners and the about 17 dp tiles still look like Droidbubble? Should tiles keep the 0.42 cap or go concentric-round? |
| **CP2**: Compact balance | V3.1–V3.5 | VD0.1 compact states at font scale 1.0 and 1.3, dual and bubble ovals, the flashlight value, the alert pills; a 10 s ticking-timer capture showing a stable width. | Hugging width with `Wmin = 120`: OK? Content snug to the camera, or pinned to the outer edges (one flag)? Is the bubble oval OK? |
| **CP3**: Cards and color | V4.2, V5.3, V6.2 | Expanded states before and after (heights), the media card on white, mid and dark covers (scrims), glass on a white wallpaper, keyline modes OFF/AUTO/ALWAYS on dark and light backdrops, high contrast. | Are the shorter cards OK? Recording red unified to `FF453A`: OK? Should the keyline default stay OFF or become AUTO? |
| **CP4**: Icons | V7.2 (V7.3 if done) | An icon sheet: every catalogue glyph at the compact, bubble, header and control sizes, Filled before and Rounded after; optical-centre crops. | Is Rounded everywhere OK? Is any glyph worse? Go ahead with V7.3 drawables? |
| **CP5**: Signature motion | D1.2, D2.1, D2.2, D4.1 (D1.3 if trialled) | **Videos**, not screenshots: 120 fps screen recordings of split/merge (shader vs. old neck), the alert bump, a banner peek/recede, expand/collapse with shared art; 16 ms frame strips of the neck snap and the art travel; the D4.2 placement table. | Does the liquid read as one body? Is the bump size (6%) right? Is the peek duration right? Enable the drip? Any placement fixes deferred? |
| **CP6**: Curve editor | D5.3 | Editor screenshots per segment and preset, plus side-by-side video of the preview vs. the island for the Droid preset. | Are the labels clear? Do the presets feel distinct? Is Droid identical to today? |

---

## Execution order

`∥` marks tasks that can run in parallel with the previous item, as long as they don't share files. **[IV]** marks a task that edits `IslandView.kt`; run those one at a time, and serialise them with Phase 7 (A1.5–A1.8, A2.3, A2.4) and Phase 8 (C3.x). **[MS]** marks a task that edits `MainScreen.kt`; serialise those with Phase 8's C tasks, or land C5.1 first.

1. **VD0.1** checkpoint set (after S2; record the baseline before any V task)
2. **Pure starts**, no shared files: **V1.1** cutout reader ∥ **V2.1** CardGeometry and squircle (`IslandShapes`, `LiveActivities` only) ∥ **D1.1** liquid SDF maths ∥ **D5.1** curve model and migration (needs F3)
3. **V1.2** calibrated settings and migration → **V1.3** apply calibration [IV, service] → **V1.4** Dimensions UI [MS] → **V1.5** landscape
4. **V7.1** icon catalogue [IV] → **V7.2** sizes and optical centre [IV]. These come early because epic I builds on them (PLAN §5 icons track: V7 → I1).
5. **V2.2** one margin system [IV] → **V2.3** concentric radii [IV] → **CP1**
6. **V3.1** hugging width [IV] → **V3.2** fit order [IV] → **V3.3** oval bubble [IV] → **V3.4** flashlight value [IV] → **V3.5** alert pills [IV, service] → **CP2**. Serialise V3.1 with Phase 7 A1.6, or rebase onto its slots.
7. **V4.1** measured heights [IV] → **V4.2** rhythm [IV]. ∥ **V5.1** semantic colors (if F2 merged the colors, this touches mostly `LiveActivities` and tokens). Then **V5.2** media accent (new file, with a small IV call-site swap) → **V5.3** scrims [IV] → **V6.1** keyline [IV] → **V6.2** outlines [IV] → **CP3**
8. **D3.1** compact numbers [IV] → **D3.2** card numbers [IV] → **D3.3** progress springs [IV]. These can start after V3.1 and V4.2 even if Phase 3 isn't done; D3.3's draw-phase read wants M2.
9. **After M1–M3 land:** **D2.1** alert bump [IV] → **D1.2** shader split/merge [IV] → **D2.4** haptics. ∥ **D2.3** pulse signal (`IslandStateManager` only; hand to I3). Then **D2.2** banner peek (wants A2.3) → **D4.1** shared art [IV] → **D4.2** placement audit [IV] → **CP4** (icons, if not done after step 4) and **CP5**
10. **D5.2** editor v2 [MS] (after D5.1 and M5) → **D5.3** presets [MS] → **CP6**
11. **Optional**, each after its checkpoint: **D1.3** drip (after CP5) ∥ **D6.1** orbit ring (after V1.3 and D3.3) ∥ **V7.3** Material Symbols drawables (after CP4, agreed with epic I)
