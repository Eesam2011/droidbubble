# 03 — Animation & Motion (research + audit)

Scope: island motion only (`island/*`, `service/IslandOverlayService.kt`, `service/GlassPaneWindows.kt`, curve editor in `ui/main/MainScreen.kt`). Working tree of `island-polish`, read-only audit. Apple is used as a **quality benchmark, not a spec to clone**. See "Droidbubble's own take".

Confidence labels: **[Apple-doc]** = stated in Apple docs/WWDC. **[Android-doc]** = AndroidX source/docs. **[Community]** = open-source recreation or write-up. **[Estimate]** = my own modelling or observation, to verify on a device.

---

## 1. Apple reference (benchmark)

**Spring model** [Apple-doc, WWDC23 "Animate with springs" + `Spring` docs]
- Two perceptual parameters: **duration** (pace) and **bounce** (−1…1). Mass = 1, stiffness = (2π / duration)², damping coefficient = (1 − bounce)·4π / duration for bounce ≥ 0. Apple's own example: `Spring(duration: 0.5, bounce: 0.3)` gives stiffness 157.9 and damping 17.6.
- **Compose mapping:** `spring(dampingRatio = 1 − bounce, stiffness = (2π/duration)²)`. Compose uses unit mass, so legacy `response` = duration and `dampingFraction` = dampingRatio. Inverse: duration = 2π/√k, bounce = 1 − ζ.
- Guidance: bounce 0 is the default when unsure, ~0.15 feels brisk, 0.3 is noticeably bouncy, and be cautious above ~0.4.
- Retargeting keeps the current velocity. Gestures hand their velocity to the spring. Ease and linear curves cannot carry an initial velocity.
- Presets: `.snappy` 0.5 s / bounce 0.15, `.bouncy` 0.5 s / 0.3, `.smooth` no bounce. Legacy `spring()` is response 0.5 / dampingFraction 0.825. `interactiveSpring` is 0.15 / 0.86 (blend 0.25).
- WWDC23 "Wind your way…": keyframes are for one-shot, pre-authored effects (shake, reaction). They can't retarget gracefully, so anything interruptible should be a spring. Separate tracks layer squash/stretch and anticipation.

**Converted to Compose** [math from the formulas above]

| Reference | duration / bounce | Compose ζ / k | overshoot |
|---|---|---|---|
| `.smooth` | 0.5 / 0 | 1.00 / 158 | 0 % |
| `.snappy` | 0.5 / 0.15 | 0.85 / 158 | 0.6 % |
| `.bouncy` | 0.5 / 0.3 | 0.70 / 158 | 4.6 % |
| `interactiveSpring` | 0.15 / 0.14 | 0.86 / 1755 | 0.5 % |
| boring.notch open [Community] | 0.42 / 0.2 | 0.80 / 224 | 1.5 % |
| boring.notch close [Community] | 0.45 / 0 | 1.00 / 195 | 0 % |
| M3 Expressive defaultSpatial [Android-doc, via mirror] | 0.32 / 0.2 | 0.80 / 380 | 1.5 % |
| M3 Expressive fast / slow spatial | 0.22/0.4, 0.44/0.2 | 0.6/800, 0.8/200 | 9.5 %, 1.5 % |
| M3 effects (fast/default/slow) | — | 1.0 / 3800, 1600, 800 | 0 % |

**Island design intent** [Apple-doc, WWDC23 "Design dynamic Live Activities"]
- The island should feel like "a living organism" with deliberate elasticity.
- Built-in transitions are numeric text (counts up/down) and content replace. Custom transitions combine scale, opacity and position. Animate only the element that moves and fade the rest.
- Keep the relative placement of elements consistent between compact and expanded. Nest shapes concentrically. Expanded content hugs the sensor (no "forehead"). Avoid heights that sit between the compact and expanded sizes.
- Apple publishes **no spring numbers for the system island**. Any "Dynamic Island = X" value is an estimate.

**Observed choreography** [Estimate, from watching the system island; not frame-measured]
- The shape leads. Content fades and scales in after the shape is mostly grown, and leaving content blurs and fades early.
- Expand is a single continuous growth from the pill, about 0.4–0.5 s with a small bounce. Collapse has about the same pace with less bounce.
- The minimal bubble buds off the pill and merges back with a gooey neck.
- The alert "breath" is a quick width impulse. A press gives a small squish.

**Gooey / metaball** [Community]
- The usual technique is to blur the shapes and then apply an alpha threshold. A React Native Skia island recreation uses a 37 pt-tall, 20 pt-radius pill.
- Compose AGSL metaball write-ups warn that the threshold distorts content drawn inside the blob, so draw content on a separate, unfiltered layer.
- For two or three shapes, a signed-distance-field smooth-union shader is cheaper and exact.

---

## 2. Droidbubble today: animation inventory

`IV` = `island/IslandView.kt`, `IC` = `IslandComponents.kt`, `LA` = `LiveActivities.kt`, `OS` = `service/IslandOverlayService.kt`.

**Global**
- `speed = animationSpeed` (default **0.7**, `model/IslandSettings.kt:39`, `data/SettingsRepository.kt:70-75`). Every spring uses stiffness × speed², which is a physically correct time scale (`IV:569-573`).
- Effective defaults: expand 0.46 s / bounce 0.20 (ζ 0.8, k 186), collapse 0.39 s / 0 (ζ 1, k 261), appear 0.54 s / 0.40, split 0.50 s / 0.37.

**Shape and size** (all `animate*AsState` read in composition)

| What | Where | Spec |
|---|---|---|
| Card geometry, 4-D (w, h, corner, top) | `IV:1036-1044`, spec `641-657`, type `IC:573-583` | expand / appear / collapse springs |
| expandProgress | `IV:687-691` (`623-637`) | same springs |
| Compact pill width | `IV:1032-1035`, spec `675-685` | 0.82 / 380 while expanding, **split curve** otherwise |
| Card 2 height / alpha / offset | `IV:1045-1047`, start delayed 45 ms `964-972` | size spring |
| Bubble X offset | `IV:1048`, spec `1004-1014`, emerge delayed 60 ms `988-997` | split curve; retract 0.85 / 380 |
| Horizontal offset, card spacing | `IV:1049, 1051` | expand / collapse |
| Card drag offset X | `IV:1089-1098` | 0.76 / 420 |

**Choreography**
- Expanded content: alpha and scale 0.94→1 are derived from expandProgress (in from 60 %, out from 45 %). Leaving content blurs up to 10 dp (`IV:1446-1467`).
- Compact content blurs out on expand (`IV:2215-2221`). Its alpha multiplier is ×3.0 while expanding and ×1.8 while collapsing (`IV:1954-1958`).
- "Liquid stretch": the card scales up to +3.5 % in X and −2 % in Y with measured morph speed (`IV:1316-1328`). Speed comes from `MorphSpeedMeter`, which takes `System.nanoTime()` in composition (`IC:623-663`).
- Activity swap: `AnimatedContent` with fade + scale 0.85, the leaving content blurred up to 12 dp (`IV:2226-2264`). A `swapSquash` tween of 110 ms is followed by a spring at ζ 0.5 (`IV:578-588`).
- Split: the bubble is a fixed circle sliding on X under the pill, joined by a concave quadratic "neck" that snaps at a 5 dp gap (`IV:1675-1796`). Merge is slide-under plus fade.
- Notch bridge (`IV:1805-1820`). Parentheses shoot out (initial velocity 240·speed, spring 0.65 / 260) (`IV:1155-1181, 2270-2340`).

**Micro-animations**
- Press: pill 0.95, bubble 0.9, spring 0.6 / 700, not speed-scaled (`IV:590-604`).
- Idle-tap bounce: tween → tween → spring (`IV:1896-1932`).
- Alert content: 130 ms tweens. The checkmark waits 110 ms, then springs at ζ 0.52 (`IV:693-794`).
- Bell and vibrate shakes use keyframes (1000 / 600 ms) (`IV:796-872`). Charging fill is a 750 ms tween (`IV:876-891`).
- `RollingNumberText`: per-character roll, 380 ms with CubicBezier(0.2, 0, 0, 1), animated in the draw phase. Very good (`LA:374-492`).
- Cover flip on track change (`IC:516-556`), timer hand (`IC:747-778`), countdown ring (`LA:1226-1244`), equalizer and waveform frame loops (`IV:4170-4218`, `LA:569-610`), recording dot pulse (`IV:4143-4162`), alert icon pulse (`LA:1030-1036`).
- Media slider: 260 ms linear glide (`IV:3330-3334`). Seek thumb and play/pause have springs and `AnimatedContent` (`IV:3399-3408, 3610-3617`). Progress springs (`IV:3977-3980`, `LA:1063-1066`).

**Blur**
- `steppedBlurEffect` caches one `RenderEffect` per 0.5 px step and turns off in Battery Saver (`IC:595-614`).
- Album-art box blur runs on the CPU in composition, on the main thread (`IV:451-458, 230-325`).
- A 32 dp live blur is the fallback when that bitmap isn't ready yet (`IV:4664-4681`). The flashlight beam always has `.blur(8dp)` (`IV:3109`).

**Window**
- The main overlay is **not resized every frame**. It is resized at state changes: compact bounds, `MATCH_PARENT` while expanded, 1×1 when idle (`OS:899-1057`).
- After a collapse, shrinking waits for the island's settle signal (`OS:1021-1034`, signal sent from `IV:1061-1070`). Hiding uses a fixed 360 ms delay (`OS:1035-1052`).
- The refresh-rate boost is a separate `updateViewLayout` (`OS:846-867`).
- `IslandTouchManager` (touchable region) is **never instantiated**: dead code (`OS:573`).
- Glass panes (opt-in) are window-resized every frame on their own threads (`GlassPaneWindows.kt:35-41, 126-150`).

**Curve editor** (`MainScreen.kt:2290-2448`)
- Shows Bounce % = (1 − ζ)/0.7 and Snap = raw stiffness 150–700.
- The preview plots 0–1 s **without** the speed² factor.

**Reduce motion**
- The app has no setting of its own.
- Compose links `ANIMATOR_DURATION_SCALE` to `MotionDurationScale` on the window recomposer [Android-doc]. So "Remove animations" makes every spring snap, but the `delay()` staggers (45/60/70/110 ms) and service timers keep running.

---

## 3. Gaps (ranked)

**P0-1: Expand and collapse are two shapes, not one morph.**
- Collapsed, the card is a **0-width** sliver at the pill (`IV:918-922`). It grows and drops 52 dp (`IV:1024-1028`) while the pill shrinks to 0 (`IV:902`).
- Modelled with the default settings [Estimate]: during expand the silhouette narrows 180 → **121 dp at about 80 ms** before growing.
- During collapse it narrows 340 → **120 dp at about 140 ms**, then the pill pops back out to 194 dp (7.8 % overshoot from the split curve).
- It reads as "vanish and re-pop", not as a living shape. Two different springs also mean an interrupted expand↔collapse isn't one continuous motion.

**P0-2: The whole `IslandOverlayRoot` recomposes on every animation frame.**
- About 40 animated values are read with `by` at the top of a ~1,950-line composable. `rememberMorphSpeed` depends on this explicitly (`IC:623-628`).
- Every frame also allocates a new `SquircleShape` (path rebuilt for clip, border and backdrop) and runs layout, because `.width/.height(animated)` is used.
- At 120 Hz (8.3 ms per frame) this is the main jank risk on mid-range devices. Quantify with a Perfetto trace.

**P1-1: The window resize races the animation.**
- `updateViewLayout` runs asynchronously from a service coroutine. By the project's own trace a window relayout takes about 28–80 ms.
- So the first frames of an expand can be clipped by the compact bounds, and an appear from 1×1 can lose frames.
- Hide shrinks the window after a fixed 360 ms, but the split spring needs about 580 ms at 0.7×.
- Expand start makes two relayouts (refresh boost, then resize).

**P1-2: Gestures don't track the finger and drop their velocity.**
- On swipe-dismiss the card springs back to 0 and then vanishes (`IV:1367, 1541`), and the threshold is distance only.
- Swipe-up collapse fires at −15 px with no velocity hand-off (`IV:1286-1304`).
- The pill hide/minimize swipes don't move until release (`IV:1850-1877`).

**P1-3: The curve editor doesn't control what it says it does.**
- **"Appear"** only drives the card morph while a charging/unlock/BT/ringer alert is showing (`IV:607-657`). The pill's real appear, widen, hide and minimize all ride **"Split"** (`IV:675-685`).
- The preview is 1.43× faster than reality at the default 0.7×.
- The Bounce % isn't Apple's bounce: 100 % = ζ 0.3, which is 37 % overshoot.
- The default appear/split curves (bounce 0.40 / 0.37) sit on Apple's caution line: 8–10 % overshoot, about 14–17 dp on a 180 dp pill.

**P1-4: Content hard-cuts.**
- In an expanded card, `ExpandedCardContent` swaps instantly when the primary activity changes (`IV:1469`), while the card height animates.
- In the compact pill, a new notification replacing one of the same priority hard-cuts. Titles without digits render as plain `Text` (`IV:2897-2903`).

**P1-5: No reduce-motion mode.** There's no gentle path: either full bounce, shake and squash, or the system's all-or-nothing snap.

**P2**
- Alpha thresholds differ by direction, so interrupting causes jumps (`IV:1446-1450, 1954-1958`), and the blur gates on `isExpanded`.
- Wall-clock staggers aren't interrupt-aware.
- Specs are tweens mixed with springs, and press and swap specs ignore `speed`.
- The album-art blur runs on the main thread during the cover flip.
- Rolling digits are missing on %, W, ° and network speed. The bubble's progress ring jumps (`IV:2525-2531`).
- The split never deforms the bubble.
- Glass-pane blur lags behind the card (opt-in).
- `cardContainerAlpha` sits on a layer that resizes every frame, so an offscreen buffer is reallocated each frame.
- No shared artwork compact→expanded, which goes against Apple's "consistent placement" guidance.
- The only "symbol effect" vocabulary is the bell shake and the checkmark.

---

## 4. Recommendations (principles adapted to Compose)

**R1. `IslandMotion` token object** (S, low risk, no dependencies)
- **What:** add `island/IslandMotion.kt`. Each token is defined as (duration, bounce) and converted once: `spring(1−b, (2π/d)²·speed²)`.
- **Why:** this is the single place where speed, reduced motion and battery tier get applied. It replaces about 30 inline specs.
- **Files:** IV, IC, LA.

**R2. One shape, one spring** (L, high risk, needs R1)
- **What:** an `IslandGeometry(centerX, top, width, height, corner)` held in **one** `Animatable` (5-D vector) for the main island. Compact and expanded are just targets. Pill and card become the same black surface, with the compact and expanded content layers inside it.
- **Why:** fixes P0-1. Interrupting goes through `animateTo(target, spec)`, and Animatable keeps its velocity by default, so expand↔collapse is always continuous.
- **How:** expand starts from the pill's *current* geometry, never from 0 width.
- **Files:** IV `897-1051`, `1266-1840`, plus `CardMorph`.
- **Testing:** keep `IslandOverlayCrashTest`. Add a test that the geometry never jumps by more than ε between frames when retargeted.

**R3. Read animations in layout/draw, not composition** (M, needs R2)
- **What:** split the root into children that take `() -> IslandGeometry`. Size via `Modifier.layout {}` with fixed-size content. Draw the surface in `drawWithCache`, with the squircle `Path` rebuilt only when the size changes. Replace `MorphSpeedMeter` with `geometry.velocityVector` read inside `graphicsLayer`, which is exact with no `nanoTime` jitter.
- **Target:** about 0 recompositions per frame during a morph. Verify with Layout Inspector recomposition counts and JankStats.

**R4. Choreography as pure functions of progress** (S–M, needs R1)
- **What:** content progress p is derived from geometry (0 = compact, 1 = expanded), using the **same curve in both directions**.
- **Incoming:** alpha = smoothstep(0.45…0.9), scale 0.92→1 anchored top-centre, optional 4→0 dp blur. The leading element starts at p 0.45, trailing elements at p 0.55.
- **Leaving:** gone by p 0.35 (or 0.65 when collapsing), blur 0→8 dp, scale →0.96.
- **Inside cards:** wrap expanded content in `AnimatedContent(contentKey)` with SizeTransform off (geometry handles size). Key compact content on (priority, notification key / track key).
- **Fixes:** P1-4 and the P2 hysteresis jumps.

**R5. Gesture physics** (M)
- **What:** an `Animatable` drag offset plus a `VelocityTracker`.
- **Release:** dismiss if |x| > threshold **or** |v| > ~1000 dp/s, flinging off with `animateTo(offscreen, initialVelocity = v)` and then dismissing. Otherwise spring back with v.
- **Pill:** rubber-band the pill during hide/minimize drags, x′ = x / (1 + |x|/L). Feed swipe-up velocity into geometry top/height on collapse.
- **Files:** IV `1286-1385, 1515-1560, 1850-1877`.

**R6. Window: grow first, shrink on settle** (M)
- **Now (low risk):**
  - The expand animation waits until the root has actually resized: watch `onSizeChanged` on the root, at most 1–2 frames.
  - Replace the 360 ms hide delay with a settle signal, as collapse already does.
  - Merge the refresh-rate change into the same `updateViewLayout`.
- **Later (behind a flag):** a fixed-size window plus a touchable region. `IslandTouchManager` already exists, but it uses a hidden `ViewTreeObserver` API through reflection, so it is a non-SDK interface risk. Test on Android 14–16 first.
- **Files:** OS `846-1057`, IV root.

**R7. Liquid SDF shader for split, merge and drip** (L, needs R2/R3; minSdk 33 = AGSL is available)
- **What:** an AGSL `RuntimeShader` brush that draws the black body as `smin(sdRoundBox(pill), sdCircle(bubble), k)`. k ramps from about 10 dp to 0 as the gap grows. The neck snaps at a gap of about 5 dp.
- **Merge:** absorb the bubble, with a 3 dp bulge on the pill.
- **Details:** anti-alias with `smoothstep(±1px)`. Draw content above the shader layer, unfiltered. Only use it during transitions; at rest draw plain shapes. Capsules and circles have no squircle smoothing, so the SDF is exact.
- **Replaces:** the neck Canvas at IV `1758-1796`.

**R8. Reduced motion tier** (S, needs R1)
- **Trigger:** an in-app toggle, or the system animator scale = 0.
- **Behaviour:** geometry uses ζ = 1 at 0.3 s. No bounce, squash, shake, flip, parentheses shoot, stretch or blur. Content uses 150 ms cross-fades, digits cross-fade instead of rolling, and the equalizer bars are static.
- **"Efficient" tier:** Battery Saver also turns blur off, already does (`IC:607`), and halves frame loops.

**R9. Curve editor v2** (S–M, needs R1)
- **Sliders:** speak **Duration** and **Bounce** (Apple's perceptual model, −0.2…0.5, with a soft cap at 0.4).
- **Preview:** plot the curve that will actually run (including speed²), and print the settle time and overshoot in dp.
- **Transitions:** rename them to what they drive: Expand, Collapse, Appear/Dismiss, Split/Merge. Add presets (Calm / Droid / Lively).
- **Migration:** convert stored values with d = 2π/√k, b = 1 − ζ.

**R10. Micro-animation polish** (S each)
- Use `RollingNumberText` for %, W, ° and network speed.
- Animate the bubble's progress ring (ζ 0.9 spring).
- Add a `Modifier.symbolBounce(trigger)` with a scale impulse spring for "same app, new event".
- Move the album blur to `produceState` on `Dispatchers.Default`.
- Use `CompositingStrategy.ModulateAlpha` on the card shell.
- Read the recording-dot alpha in the draw phase.
- Turn the tween chains (idle bounce, swap squash) into velocity impulses on springs. Per WWDC23, keyframes are only for one-shot effects such as the bell shake.

---

## 5. Droidbubble's own take

**Signature character: "liquid ink, Android-native physics."** The island behaves like a drop of ink:
- it **buds** (split),
- it **drips** into a card below the status bar (expand),
- it is **re-absorbed** (collapse/merge),
- and it squashes and stretches with its **real velocity**.

It sits in Android's motion family: the current expand default 0.8 / 380 is *exactly* Material 3 Expressive defaultSpatial. The app already sets `MotionScheme.expressive()` (`OS:700-703`) but the island never reads it.

Deliberate differences from Apple:
1. **Keep the drop below the status bar.** Android status icons matter more, and the camera is a punch-hole, not a pill. Render the drop as one liquid body with a neck (R7), not as two shapes.
2. **Asymmetric temperament: "eager in, calm out."** Expand gets a small bounce (≤ 0.15–0.2). Collapse and dismiss are critically damped and about 15 % quicker.
3. **Haptics on physical moments.** `VibrationEffect.Composition` low-tick when the neck snaps, tick when the bubble merges back, click on expand.
4. **User-tunable personality.** Something Apple doesn't offer, but it must be bounded and honest (R9): what you preview is what you get.
5. **Colour in motion.** Content transitions carry the album or app accent (Material You), for example the leaving blur tinted toward the accent.
6. **Battery-aware motion tiers** (Full / Efficient / Reduced) as a visible feature.

Where we *should* match Apple's quality bar: interruptibility, velocity hand-off, the shape leading and content following, no hard cuts, numeric rolls, consistent element placement.

---

## 6. Motion tokens and choreography

Defaults are at speed 1.0. Re-base so the new 1.0 roughly equals today's effective 0.7× feel.

| Token | Drives | duration / bounce | Compose ζ / k |
|---|---|---|---|
| `expand` | geometry compact→expanded | 0.45 s / 0.15 | 0.85 / 195 |
| `collapse` | geometry expanded→compact | 0.38 s / 0 | 1.00 / 273 |
| `appear` | pill out of the camera, alert widen | 0.42 s / 0.20 | 0.80 / 224 |
| `dismiss` | pill into the camera, hide | 0.32 s / 0 | 1.00 / 386 |
| `split` | bubble buds off | 0.45 s / 0.25 | 0.75 / 195 |
| `merge` | bubble absorbed | 0.35 s / 0.05 | 0.95 / 322 |
| `alertBump` | width impulse (v₀ ≈ +6 % width) | 0.30 s / 0.45 | 0.55 / 439 |
| `press` | squish 0.96 pill / 0.92 bubble | 0.20 s / 0.30 | 0.70 / 987 |
| `interactive` | finger release, rubber band | 0.18 s / 0.10 | 0.90 / 1218 |
| `effects` | alpha, blur, colour | ≈0.16 s / 0 | 1.00 / 1600 (M3 defaultEffects) |
| `roll` | digits | tween 380 ms, (0.2, 0, 0, 1) | keep as is |

Values: content scale in 0.92→1 (top-centre) and out 1→0.96. Blur out up to 8 dp, blur in up to 4 dp (optional). Stretch up to +3 % X and −2 % Y, from |v|/vₘₐₓ. Card 2 is offset by p + 0.1 (no wall-clock delay). Reduced tier: ζ = 1, 0.3 s, fades of 150 ms.

**Timelines** (ms are approximate at 1.0×; staggers are expressed in p so they survive interrupts and speed changes)
- **Expand:** at 0, haptic and geometry starts from the current pill. 0–90 compact content fades and blurs out (p < 0.35). About 110–300 expanded content comes in (p 0.45–0.9, leading items then trailing). Card 2 starts at p ≥ 0.1. About 450 settled. Glass blur fades in after settling.
- **Collapse:** at 0 the geometry springs back to the pill with gesture velocity. 0–100 expanded content leaves with blur. Card 2 leads card 1 by p 0.1. About 230+ compact content arrives sharp (p < 0.4). Window shrink happens on settle.
- **Appear:** the pill grows from the camera circle. Alpha is a smoothstep over the first 30 % of width. Content comes in after 60 % of width. Parentheses shoot at about 70 ms (signature, optional).
- **Dismiss:** content out in 80 ms. Geometry `dismiss` to the camera circle, fading over the last 30 % of width. Window shrinks on settle.
- **Split:** the bubble buds from the right cap (smin k 10→0 dp). The neck snaps at a 5 dp gap with a low-tick haptic, and the pill recoils by −2 dp. Bubble overshoot stays ≤ 4 dp. Bubble content comes in after the snap.
- **Merge:** bubble content out over 60 ms. The bubble travels on `merge`. At contact k ramps up, the pill bulges +3 dp and settles. No fade-under.
- **Alert bump:** the old content leaves (120 ms, blur). The new content springs in from scale 0.85. A width velocity impulse on `alertBump` replaces `swapSquash`. Optional `symbolBounce` on the leading icon, plus a haptic.

---

### Sources
- Apple, WWDC23 *Animate with springs*: https://developer.apple.com/videos/play/wwdc2023/10158/
- Apple, `Spring`: https://developer.apple.com/documentation/swiftui/spring
- Apple, `snappy(duration:extraBounce:)`: https://developer.apple.com/documentation/swiftui/animation/snappy(duration:extrabounce:)
- Apple, `bouncy(duration:extraBounce:)`: https://developer.apple.com/documentation/swiftui/animation/bouncy(duration:extrabounce:)
- Apple, `spring(response:dampingFraction:blendDuration:)`: https://developer.apple.com/documentation/swiftui/animation/spring(response:dampingfraction:blendduration:)
- Apple, `interactiveSpring`: https://developer.apple.com/documentation/swiftui/animation/interactivespring(response:dampingfraction:blendduration:)
- Apple, WWDC23 *Design dynamic Live Activities*: https://developer.apple.com/videos/play/wwdc2023/10194/
- Apple, WWDC23 *Wind your way through advanced animations in SwiftUI*: https://developer.apple.com/videos/play/wwdc2023/10157/
- boring.notch `ContentView.swift` (community notch springs): https://github.com/TheBoredTeam/boring.notch
- React Native gooey island recreation: https://github.com/AbijahKaj/react-native-SwiftScrollAnim
- Compose metaball / AGSL series: https://proandroiddev.com/jetpack-compose-metaball-edge-effect-final-part-ac8a4cc7a425
- M3 motion token mirror (AndroidX Material3 1.5): https://docs.rs/ferroui-themes-material/0.4.0/src/ferroui_themes_material/motion.rs.html
- AndroidX `WindowRecomposer.android.kt` (animator scale → MotionDurationScale): https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/WindowRecomposer.android.kt
- Smooth-min SDF reference (not fetched in this pass): https://iquilezles.org/articles/smin/
