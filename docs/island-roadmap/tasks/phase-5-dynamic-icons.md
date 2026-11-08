# Phase 5 — Dynamic icons

**Goal.** Make the island's icons react to what is happening, the way Apple's symbol effects do. That means a bounce when the same app sends something new, a pulse while recording, a wiggle for a ringing call, a fill that rises with a download, a slash that draws itself across the bell, and a play glyph that melts into pause. Every effect is built the Android way: springs from `IslandMotion`, reads in the draw phase, `graphics-shapes` morphs and path trims, and a Reduced-motion fallback for each one.

**This phase is polish, not a redesign (PLAN v2 §0).**
- The glyphs stay the ones the island already shows (V7 moves them to one Rounded family). Colours stay F2 tokens. Squircles and card layouts don't change.
- We take Apple's *principles*: discrete vs indefinite vs transition effects, by-layer motion, "magic replace" that keeps the shared shape and animates only the difference. We don't copy SF Symbols assets, effect names in UI copy, or Apple's timings. No timings are published for these anyway.
- The only new visible element is a small bolt on the charging pill (I3.3). The plan's I3 table asks for it, and it needs an owner design check.

**Assumed inputs.** These tasks are referenced by ID:
- F2: tokens, plus the CI grep that bans literals
- F3: `IslandMotion` with (duration, bounce) springs and the speed² factor, plus `IslandHaptics`
- M5: motion tiers Full / Efficient / Reduced
- V7: one Rounded icon family, the `IslandIcon` type, and size tokens (`IconCompact` 18 dp, `IconBubble` 16 dp, `IconHeader` 24 dp; 02 R6)
- S2: screenshot harness
- S4: recomposition and frame baseline

Phase 7 already relies on this phase: `Slot.Icon(icon: IslandIcon, effect: IconEffect?)` (phase-7 A1.2), and A7.x name I3 effects for charging, ringer/DND and mic/camera. The API below keeps those names.

**Confidence labels** (same as `research/03`):
- **[Apple-doc]**: stated in an Apple WWDC session or on developer.apple.com, paraphrased.
- **[Android-doc]**: from developer.android.com or AndroidX release notes.
- **[Verified-local]**: checked in this repo or in the local Gradle cache.
- **[Community]**: a third-party write-up.
- **[Estimate]**: my own modelling, to be tuned on a device in I4.

---

## 1. Reference summary: Apple principle → Droidbubble implementation

### 1.1 What Apple's symbol-effect system teaches (paraphrased)

1. **Every effect has a behaviour class** [Apple-doc, WWDC23 10258]:
   - *discrete*: fires once on an event and needs no cleanup
   - *indefinite*: runs while a state holds and is removed when the state ends
   - *transition*: inserts or removes the symbol and may reflow layout
   - *content transition*: animates one symbol into another

   An effect's class decides how it is triggered. For Droidbubble that maps to `trigger: Any?` (discrete), `active: Boolean` (indefinite), visibility (transition) and a changing icon value (content transition).
2. **Pick the effect by the message, not the look** [Apple-doc, WWDC23 10197; WWDC24 10188]:
   - bounce: "something happened"
   - scale: focus or selection (stateful)
   - pulse: ongoing activity, opacity only
   - variable color: a level or a search/scan
   - wiggle: "look here" or a call to action
   - breathe: ongoing status that should feel present (recording, timer)
   - rotate: work in progress
   - replace: a change of state
3. **By-layer is the default** [Apple-doc, WWDC23 10197; WWDC25 337]. Layers animate one after another, which reads as more precise. Whole-symbol motion is for unified movement. Variable color depends on layer order:
   - *cumulative*: layers fill and stay filled (connecting, a level building up)
   - *iterative*: one layer at a time (searching)
   - *reversing*: loops back
4. **Magic Replace keeps what the two symbols share** and animates only the difference, for example drawing a slash on or off, or adding a badge. Unrelated symbols fall back to a directional replace [Apple-doc, WWDC24 10188]. SF Symbols 7 adds *Draw On / Draw Off*, a stroke-order reveal with by-layer default, and *Variable Draw*, where layers act as progress. Draw-capable symbols draw off and on during Magic Replace [Apple-doc, WWDC25 337 / sf-symbols page; detail from wwdcnotes, Community].
5. **Replace direction carries meaning** [Apple-doc, WWDC23 10197]:
   - down/up: a reaction to user input
   - up/up: forward progression (play → pause after a tap)
6. **Initial state does not animate.** A symbol that starts in a state is set without animation [Apple-doc, WWDC23 10258]. For us: no effect fires on first composition or when a card re-enters.
7. **Restraint.** Too much motion, or motion in the wrong context, distracts from content [Apple-doc, WWDC24 10188]. For us: one effect per icon at a time, discrete effects are rate-limited, and indefinite loops stop off-screen.
8. **Keyframes only for one-shot authored effects; anything interruptible is a spring** [Apple-doc, WWDC23 10157, via 03 §1]. We go one step further: bounce and wiggle are *velocity impulses on springs*, so a second event that arrives mid-effect adds to the motion instead of restarting it (03 R10).

### 1.2 Android building blocks (what exists, what it costs)

| Building block | Status in this repo | Use here | Confidence |
|---|---|---|---|
| `Modifier.graphicsLayer { }` with `Animatable` values | Already used (bell `IV:2156-2166`, cover flip `IC:549-553`) | Engine for bounce, wiggle, scale, pulse, breathe, rotate, appear, replace. Reads happen in the layer phase, so there's no recomposition. | [Verified-local] |
| `androidx.graphics:graphics-shapes` (`RoundedPolygon`, `CornerRounding(r, smoothing)`, `Morph`, `Morph.toPath(progress, path)`) | **Already on the compile classpath**, 1.0.1, pulled in transitively by `material3-android:1.5.0-alpha04` at compile scope (POM in `~/.gradle/caches`). Latest stable is 1.1.0 (Oct 2025). | Filled-shape morphs (play↔pause). `CornerRounding(…, smoothing = 0.6f)` matches `SquircleShape`'s smoothing, so the glyph corners belong to the same family as the island. | [Verified-local] / [Android-doc] |
| `PathMeasure.getSegment` (Compose `androidx.compose.ui.graphics.PathMeasure`) | Available (compose-ui) | Draw-on/off: checkmark, slash for bell/flashlight/mic/mute. | [Android-doc] |
| Layered `ImageVector` / Canvas layers | `CellularSignalBars` is already a 4-layer icon (`IV:4234-4256`), static | Variable color: signal, network arrows, download, flashlight beam, battery. | [Verified-local] |
| `AnimatedVectorDrawable` + `rememberAnimatedVectorPainter(AnimatedImageVector, atEnd)` | **Not available.** It needs `androidx.compose.animation:animation-graphics`, which is not in `app/build.gradle.kts` or the Gradle cache. It is `@ExperimentalAnimationGraphicsApi`. Animations are authored as XML `<objectAnimator>`. `atEnd` toggles are animated, but they are timeline-based, not interruptible springs, and don't go through `IslandMotion`. | Fallback only, for a glyph that can't be expressed as Morph or trim. It costs one BOM-managed dependency. | [Android-doc] / [Verified-local] |
| Material Symbols | Apache 2.0. Shipped as static vector drawables and a variable font (FILL, wght, GRAD, opsz). **There's no official animated asset set.** Google's guidance is to animate state changes on the FILL axis (web/CSS). | V7 exports the static Rounded glyphs. We animate them ourselves. FILL 0→1 ≈ our variable-color "fill" for active states. | [Android-doc / developers.google.com] |
| `material-icons-extended` | Still a dependency (`app/build.gradle.kts:89`, `libs.versions.toml`). Deprecated upstream. | V7 removes it. I tasks must not add new `Icons.*` uses; they take `IslandIcon`. | [Verified-local] / 02 R6 |
| Lottie (`lottie-compose`) | Not used | **Not recommended.** It adds a runtime dependency and a JSON asset pipeline (designer tooling). Its timeline playback can't hand velocity to `IslandMotion` springs or follow motion tiers, and it would be a second animation system next to Compose. Revisit only for a hero illustration, never for status glyphs. | [Community] / [Estimate] |

### 1.3 Effect table

Token values are at speed 1.0, converted by F3 as `spring(ζ = 1 − bounce, k = (2π/d)²·speed²)`. They are **[Estimate]** starting points, to tune in I4. "Reduced" is the M5 Reduced tier. "Efficient" (Battery Saver) drops blur and halves indefinite frame rates, as `EqualizerVisualizer` already does (`IV:4186`).

| Effect | Apple principle | Android implementation | Motion token (d / bounce → ζ / k) | Reduced-motion fallback |
|---|---|---|---|---|
| **Bounce** (discrete) | "Something happened." Fast and elastic; by-layer by default. [Apple-doc 10197, 10258] | `scale` and `translationY` on `graphicsLayer`, driven by `Animatable.animateTo(rest, spec, initialVelocity = v₀)`. This is a velocity impulse, so repeated events stack. Direction up or down. Layered icons stagger the impulse per layer by 40 ms. | `symbolBounce` 0.35 s / 0.40 → 0.60 / 322. v₀ ≈ −3 scale/s (dip ≈ 12 %, then overshoot) [Estimate] | One 150 ms opacity dip 1→0.6→1 (`effects` token). No transform. |
| **Scale** (indefinite, stateful) | Focus or selection; holds until removed. [Apple-doc 10197] | `scale` target 1.0 ↔ 1.12 with an `animateTo` spring. Use case: the selected icon in the I4 catalogue only. The island has no focus state, and press squish already exists (`IV:590-604`). | `symbolScale` 0.30 / 0.15 → 0.85 / 439 | Snap to the target. |
| **Pulse** (indefinite or discrete) | Opacity only, for ongoing activity. Whole symbol or one layer. [Apple-doc 10197] | `alpha` from an `InfiniteTransition` value read in `graphicsLayer`, 1.0 ↔ 0.40, period 1.3 s. That keeps today's recording-dot pace of 650 ms reverse (`IV:4148-4151`). Optional `layer` index for layered icons. | `symbolPulse`: period 1.3 s, sine-like `FastOutSlowIn` (a loop, not a spring) | **Kept**, since opacity isn't motion, but slowed to a 2.0 s period. "Remove animations" (scale 0) shows a static 0.7 alpha. |
| **Breathe** (indefinite) | Opacity *and* size, "more present" than pulse; for recording or timer. [Apple-doc 10188] | Pulse plus `scale` 1.0 ↔ 1.08, period 2.0 s. | `symbolBreathe`: period 2.0 s | Falls back to Pulse (opacity only). |
| **Rotate** (indefinite) | Work in progress; whole symbol or chosen layers. [Apple-doc 10188] | `rotationZ` from `InfiniteTransition`, linear, 1 rev / 1.2 s. Per-layer rotation draws that layer inside `rotate(angle, pivot)` in `drawWithCache`. Use: indeterminate download, sync. | `symbolRotate`: 1.2 s/rev, linear | Static glyph, plus Pulse on the rotating layer. |
| **Wiggle** (discrete, repeatable) | "Look here" or a call to action; each symbol has a preferred direction. [Apple-doc 10188] | A damped spring with an initial velocity on `rotationZ` (bell, phone) or `translationX` (vibrate). It oscillates and decays on its own: no keyframes, interruptible, speed-scaled. `transformOrigin` sets the pivot (bell = top centre, as `IV:2158` does today). | `symbolWiggle` 0.50 s / 0.70 → 0.30 / 158. Peak ≈ 14° (v₀ ≈ 250 °/s); horizontal ≈ 3 dp [Estimate] | One 150 ms opacity dip. |
| **Variable color** (indefinite level, or indefinite cumulative/iterative loop) | A level, or a state changing over time; layer order drives it. Cumulative = build up, iterative = scan, reversing = loop. [Apple-doc 10197] | `LayeredIcon`: N layer paths drawn in `drawWithCache`. Each layer's alpha is `inactive(0.25) … active(1.0)`, from a level (0..1 → layers lit) or from a loop phase. Level changes animate per layer with the `effects` spring, staggered by 40 ms. | Level: `effects` (≈0.16 s / 0 → 1.0 / 1600). Loop step 150 ms per layer [Estimate] | Level shown statically, with a 150 ms crossfade when it changes. Loops become a static "all lit at 0.6". |
| **Appear / Disappear** (transition) | Insert or remove; the indefinite variant keeps layout, the transition variant reflows. [Apple-doc 10258] | `DynamicIconVisibility(visible)`: scale 0.5→1 plus alpha, plus optional blur 4→0 dp using the cached `steppedBlurEffect` (`IC:595-614`). Layout is kept by default (no reflow); the island's geometry handles size. | In: `symbolAppear` 0.35 / 0.25 → 0.75 / 322. Out: `effects` | 150 ms fade, no scale, no blur. |
| **Replace** (content transition) | Swap symbols to show a state change. Down/up = reaction to input; up/up = progression. [Apple-doc 10197] | `DynamicIconSwitch(target)`: a two-slot layer. The outgoing glyph goes to scale 0.6, alpha 0, offset ±4 dp in 120 ms; the incoming one springs from scale 0.6 with offset ∓4 dp. Keyed on the icon value. It is interruptible: a third value retargets from the current frame. | In: `symbolReplace` 0.30 / 0.15 → 0.85 / 439. Out: `effects` 120 ms | 150 ms crossfade, no scale or offset. |
| **Magic replace** (content transition between related glyphs) | Keep the shared outline and animate only the difference (slash, badge). Falls back to plain replace. [Apple-doc 10188; WWDC25 337] | `SlashOverlay(progress)`: draws the base glyph, then the slash *trimmed* with `PathMeasure.getSegment(0, L·p)`, plus a clear "gap" stroke (`BlendMode.Clear` in a `CompositingStrategy.Offscreen` 16–24 dp layer) so the slash cuts the glyph. Bell↔bell-slash, flashlight on↔off, mic↔mic-off, mute↔unmute. Unrelated pairs (bell → vibrate) use Replace. | `symbolDraw` 0.35 / 0 → 1.0 / 322 | Crossfade between the two end states. |
| **Morph** (content transition, Android-native) | Same principle as magic replace, but for filled shapes. | `graphics-shapes` `Morph(start, end)` between `RoundedPolygon`s with `CornerRounding(r, 0.6f)`. `toPath(progress)` goes into a cached `Path` inside `drawWithCache`; progress is an `Animatable` read in draw. Play↔pause = two convex halves of the triangle morphing into two bars. | `symbolMorph` 0.30 / 0.15 → 0.85 / 439 (gives a small overshoot at the shape level) | Crossfade between the end polygons. |
| **Draw on / off** (transition) | A stroke-order reveal, by layer by default. [Apple-doc WWDC25 337] | `PathMeasure` trim over the glyph's stroke path in `drawWithCache`, layers in order. Checkmark draw-on replaces today's scale pop (`IV:716-760`). | `symbolDraw` 0.35 / 0 → 1.0 / 322. Layer stagger 60 ms | Fade in, fully drawn. |

**Sources**
- Apple, WWDC23 *Animate symbols in your app* (10258): https://developer.apple.com/videos/play/wwdc2023/10258/
- Apple, WWDC23 *What's new in SF Symbols 5* (10197): https://developer.apple.com/videos/play/wwdc2023/10197/
- Apple, WWDC24 *What's new in SF Symbols 6* (10188): https://developer.apple.com/videos/play/wwdc2024/10188/
- Apple, WWDC25 *What's new in SF Symbols 7* (337): https://developer.apple.com/videos/play/wwdc2025/337/
- Apple, SF Symbols page (Draw, Variable Draw, Magic Replace enclosures): https://developer.apple.com/sf-symbols/
- WWDC Notes, SF Symbols 7 summary [Community]: https://wwdcnotes.com/documentation/wwdc25-337-whats-new-in-sf-symbols-7/
- Apple, WWDC23 *Wind your way through advanced animations* (10157, keyframes vs springs): https://developer.apple.com/videos/play/wwdc2023/10157/
- Android, *Animated vector images in Compose*: https://developer.android.com/develop/ui/compose/animation/vectors
- Android, `rememberAnimatedVectorPainter`: https://developer.android.com/reference/kotlin/androidx/compose/animation/graphics/res/rememberAnimatedVectorPainter.composable
- Android, graphics-shapes releases: https://developer.android.com/jetpack/androidx/releases/graphics
- Android Developers blog, *The Shape of Things to Come* (RoundedPolygon / Morph): https://medium.com/androiddevelopers/the-shape-of-things-to-come-1c7663d9dbc0
- Android Developers blog, *Making Jellyfish move in Compose* (animating ImageVector paths): https://medium.com/androiddevelopers/making-jellyfish-move-in-compose-animating-imagevectors-and-applying-agsl-rendereffects-3666596a8888
- Google Fonts, *Material Symbols guide* (Apache 2.0, FILL axis for state changes, vector drawables): https://developers.google.com/fonts/docs/material_symbols
- Lottie: https://airbnb.design/lottie/

---

## 2. Audit summary (working tree, read-only)

`IV` = `island/IslandView.kt`, `IC` = `island/IslandComponents.kt`, `LA` = `island/LiveActivities.kt`, `ISM` = `island/IslandStateManager.kt`, `IOS` = `service/IslandOverlayService.kt`, `NRS` = `service/NotificationReceiverService.kt`. Line numbers were checked against the current working tree.

### 2.1 Icon animations that exist today

| What | Where | Mechanism | Phase of reads | Problems |
|---|---|---|---|---|
| Bell shake (ringer = Ring) | `IV:796-849`; applied `IV:2156-2161` | `keyframes`, 1000 ms/speed, ±24°→0, plus a raw `VibrationEffect.createWaveform` (`IV:812-814`) | `rotationZ` read in `graphicsLayer` ✔ | Not interruptible (a new trigger `snapTo(0)` restarts it). Raw waveform haptic (should be F3 `IslandHaptics`). No Reduced path. |
| Vibrate shake (ringer = Vibrate) | `IV:851-872`; applied `IV:2162-2164` | `keyframes`, 600 ms/speed, ±3 dp | `translationX` in `graphicsLayer` ✔ | Same as the bell. |
| Unlock / Bluetooth checkmark | `IV:705-760`; drawn `IV:2062-2080`, `2107-2125` | `delay(110/speed)`, then alpha tween 150 ms plus scale spring ζ 0.52 / k 320·speed² (0.4→1) | **Composition:** `if (unlockCheckmarkAlpha > 0.01f)` at `IV:2067` and `2112` reads every frame | A scale pop, not a draw-on. Wall-clock stagger. Duplicated for unlock and Bluetooth. |
| Ringer label pop | `IV:762-794`; `IV:2183-2211` | Same pattern as the checkmark | Composition (`IV:2183`) | Icon swap Silent/Vibrate/Ring is a **hard cut** (`IV:2140-2144`) when the mode changes while the pill is showing. |
| Recording dot pulse | `IV:4143-4162` (used `IV:2475`, `2684`) | `rememberInfiniteTransition`, 650 ms reverse, alpha 0.35↔1 plus scale +12 % | **Composition:** `alpha` delegate read in `.scale()` and `.background()` | Recomposes every frame while recording. Hard-coded red `0xFFFF3B30`, the third "recording red" (02 §2). |
| Alert icon pulse (Time's up / alarm, expanded) | `LA:1028-1050` (used `LA:949-953`) | `InfiniteTransition` scale 0.85↔1, 520 ms reverse | **Composition** (`.scale(pulse)`) | Never stops for Reduced. The compact alert icon (`LA:1281-1286`) is static. |
| Cover flip on track change | `IC:507-556`; used `IV:2544`, `2776`, `3440` | `tween(150)` to 90°, then swap, then `spring(0.7, 380)` from −90° | `rotationY` in `graphicsLayer` ✔ | Literal specs, not speed-scaled, no Reduced fallback. The album-art blur runs on the main thread during the flip (03 P2). |
| Play/pause | `IV:3610-3624` | `AnimatedContent`: fade plus `scaleIn(0.6)` spring 0.6/500, out `tween(100)` | Composition (AnimatedContent) | A crossfade-swap, not a morph. Literal specs. The recorder/timer pause buttons (`LA:735`, `LA:816`) **hard-swap** `PlayArrow`↔`Pause`. |
| Equalizer | `IV:4170-4220` | Frame loop, `frame` counter read inside `Canvas` | Draw ✔ | Good. Halves its frame rate in Battery Saver (`IV:4186`). Keep as is. |
| Recorder waveform | `LA:569-615` (compact `LA:1266-1271`) | Frame loop or mic level, read in `Canvas` | Draw ✔ | Good. Keep as the "alive" recorder glyph. |
| Timer clock hand / countdown ring | `IC:747-778`; `LA:1226-1258` | `Animatable` stepped per second | Draw ✔ | Good. Out of scope. |
| Charging fill | `IV:875-891`; drawn `IV:1961-1985` | `tween(750, delay 60)` on fraction, used in `fillMaxWidth(fraction)` | **Layout** (fraction in a modifier argument; recomposes) | There is **no bolt glyph** in the charging pill: it shows wattage text (`IV:2006-2018`) and % (`IV:2024-2034`). |
| Compact activity swap | `IV:2226-2264` | `AnimatedContent` keyed on `primaryPriority` | Composition | A new notification of the same priority (for example, the same app again) is a **hard cut**: the key doesn't change. That's exactly where "bounce" belongs. |

### 2.2 Static icons that should come alive

| Icon | Where | Note |
|---|---|---|
| Incoming-call phone (compact) | `LA:1265` | Static `Icons.Rounded.Call`. Answer button `LA:684-692` is static too. |
| Alert timer/alarm (compact) | `LA:1281-1286` | Static. |
| Notification leading avatar or app icon | `IV:2715-2737`; bubble `IV:2477-2507` | Static. Same-app updates don't register. |
| Download glyph | `IV:2740-2757` (leading), `IV:2913-2934` (trailing %), bubble `IV:2509-2538` | Static `Icons.Default.Download`. The determinate bubble shows an 8 sp numeral (B9 fixes the size). |
| Network speed glyph | `IV:2841-2858`; bubble `IV:2603-2611`; expanded `IV:4569-4615` | Static Wi-Fi or `ArrowDownward`. Hard-coded "5G" at `IV:2852` (B-phase fix). |
| Flashlight | compact `IV:2686-2711`, bubble `IV:2560-2580`, expanded `IV:3255-3276` | Always `FlashlightOn`. `FlashlightOff` is imported but unused (`IV:94`). Level changes have no visual. |
| Bluetooth / Wi-Fi connect glyph | `IV:2081-2105` | Static. Only the checkmark animates. |
| Unlock padlock | `IV:2037-2061` | Static `LockOpen`. Optional lock→unlock replace (not in the plan table). |
| Cellular bars | `IV:4234-4256` | Already layered. A natural variable-color host. |

### 2.3 Dependencies (`app/build.gradle.kts`, `gradle/libs.versions.toml`)

- Compose BOM `2026.03.01`, `material3 1.5.0-alpha04`, `material-icons-extended` (BOM, deprecated), `profileinstaller 1.4.1`, Robolectric 4.16.1, Kotlin 2.3.20, AGP 9.0.1. `minSdk 29`, `compileSdk`/`targetSdk 36`. [Verified-local]
- `androidx.graphics:graphics-shapes-android:1.0.1` is a **transitive compile dependency of material3** (verified in the cached POM). I2 can use `Morph` without adding a line. I0.1 recommends declaring it explicitly so it doesn't vanish if material3 drops it. [Verified-local]
- `androidx.compose.animation:animation-graphics` (AVD painter) is **absent**. [Verified-local]
- No Lottie. No `res/animator`. `res/drawable` holds only launcher and notification icons. [Verified-local]

### 2.4 Cross-cutting findings
- **Motion literals everywhere.** Every icon spec above is a literal: tween 130/150/650/520/750 ms, springs 0.52/320, 0.6/500 and 0.7/380. F2 bans these, and F3 gives them names.
- **Composition-phase reads** in the checkmark, ringer label, recording dot, alert pulse and charging fill. Each one recomposes the 4,700-line root or its children per frame (06 P0-1). I tasks move them to `graphicsLayer` or draw reads.
- **No Reduced path anywhere** (03 P1-5). "Remove animations" snaps springs, but the `InfiniteTransition` pulses and `delay()` staggers keep running.
- **Haptics are raw waveforms** (`IV:812-814`). F3 `IslandHaptics` owns them, and I tasks only call it.

---

## Conventions for every task in this phase

1. **Engine location.** New code lives in `island/icon/` (package `com.droidbubble.app.island.icon`). Presentation files only *call* `DynamicIcon`, `Modifier.iconEffect`, `DynamicIconSwitch`, `LayeredIcon` and `MorphIcon`. They never build an effect inline.
2. **Draw-phase rule.** Animated values are read only inside `graphicsLayer {}`, `drawWithCache {}`/`onDrawWithContent`, or `Canvas {}` lambdas, never in a composable body or a modifier argument. During any effect the hosting composable recomposes **0 times per frame**. The test hook is `IconRecompositionProbe`, a debug-only `SideEffect` counter (I1.1).
3. **Tier rule.** Every effect resolves its spec through `IconEffectSpecs.resolve(effect, tier)`, which returns a `ResolvedEffect`: either the Full motion, the Efficient variant (no blur, half-rate loops) or the Reduced fallback from §1.3. Before M5 lands, the tier comes from the I0.1 shim: Reduced if `ANIMATOR_DURATION_SCALE == 0`, Efficient if `AudioPowerPolicy.lowPower`, otherwise Full.
4. **First-composition rule.** Discrete effects never fire on first composition, on recomposition with an unchanged trigger, or when a card re-enters composition. The trigger is remembered by key (Apple's "initial state doesn't animate").
5. **Rate limit.** A discrete effect re-fired within `IslandTimings.iconRefire` (default 600 ms) adds velocity to the running spring instead of starting a new one. A burst of 10 messages gives one lively bounce, not 10.
6. **Off-screen rule.** Indefinite loops request frames only while the icon is attached and the island is visible. Use `withInfiniteAnimationFrameMillis`, which stops when composition leaves, as `EqualizerVisualizer` already does.
7. **Haptics** go through F3 `IslandHaptics` (a tier-aware, toggle-respecting map), never a raw `Vibrator`.
8. **Tests** go in `app/src/test/java/com/droidbubble/app/island/icon/`. Pure JUnit is the default (spec resolution, trigger logic, morph geometry). Use Robolectric `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")`, matching `IslandOverlayCrashTest.kt:39`, with `mainClock.autoAdvance = false` to step frames.
9. **Definition of done** is PLAN §6. A **Design check** means: same glyph (after V7), same size token, same F2 colour role, no new layout unless the task says so, and no Apple-asset look-alike. SF Symbols paths are never traced or copied. Glyphs come from Material Symbols (Apache 2.0) or are drawn from primitives.
10. **Contention.** `IslandView.kt` and `LiveActivities.kt` are shared with the M3, V3, V7 and A1.5–A1.8 tasks. Never run two writing sessions on the same file. Engine tasks (I0, I1.x, I2.1 toolkit) touch only `island/icon/*` and can run in parallel with anything.

---

## I0 — Prerequisites

### I0.1 — Symbol-effect tokens and a motion-tier shim
- **Goal:** Add the `IslandMotion.Symbol` tokens and an `LocalMotionTier` the engine can read today, before M5 exists, without changing any visuals.
- **Why:** F3 defines the spring factory, but the plan's token table (03 §6) has no symbol tokens. Every I task needs them. M5 is in Phase 3 and would otherwise block Phase 5, though the plan says Phase 5 runs "alongside Phase 3".
- **Files:**
  - `island/IslandMotion.kt` (F3 file; add a `Symbol` section)
  - New: `island/icon/MotionTierShim.kt`
  - `gradle/libs.versions.toml` and `app/build.gradle.kts`: declare `androidx.graphics:graphics-shapes` explicitly at **1.0.1** (the version material3 already resolves, so nothing changes on the classpath). The only Gradle edit in this phase, and it needs owner sign-off.
- **API sketch:**
  ```kotlin
  // island/IslandMotion.kt (F3), new section
  object Symbol {
      val bounce  = MotionToken(duration = 0.35f, bounce = 0.40f)   // ζ 0.60, k 322
      val wiggle  = MotionToken(duration = 0.50f, bounce = 0.70f)   // ζ 0.30, k 158
      val replace = MotionToken(duration = 0.30f, bounce = 0.15f)   // ζ 0.85, k 439
      val morph   = replace
      val appear  = MotionToken(duration = 0.35f, bounce = 0.25f)   // ζ 0.75, k 322
      val draw    = MotionToken(duration = 0.35f, bounce = 0.00f)   // ζ 1.00, k 322
      val scale   = MotionToken(duration = 0.30f, bounce = 0.15f)
      const val PULSE_PERIOD_MS = 1300; const val BREATHE_PERIOD_MS = 2000
      const val ROTATE_PERIOD_MS = 1200; const val LAYER_STAGGER_MS = 40
      const val REDUCED_FADE_MS = 150
  }

  // island/icon/MotionTierShim.kt
  enum class MotionTier { Full, Efficient, Reduced }   // moves to M5's file when M5 lands (typealias kept)
  val LocalMotionTier = staticCompositionLocalOf { MotionTier.Full }
  @Composable fun rememberMotionTierShim(): MotionTier   // animator scale 0 → Reduced; AudioPowerPolicy.lowPower → Efficient
  ```
- **Steps:**
  1. Add the tokens. If F3 hasn't merged, put them in a private `SymbolTokens` object in `island/icon/` and move them later.
  2. Add the shim, and provide it at the island root (`IslandOverlayRoot`), one line.
  3. Pin the graphics-shapes version, and document why in a comment.
- **Acceptance criteria:**
  - No visual change; S2 screenshots are identical.
  - `IconTokensTest`: each token converts to the ζ/k in the comment (±1 %) at speed 1.0, and scales k by speed².
  - The shim returns Reduced on Robolectric with `Settings.Global.ANIMATOR_DURATION_SCALE = 0`, and Efficient with power save on.
  - **Draw-phase:** n/a. **Reduced tier:** the shim exists. **Device check:** n/a (no visible change).
- **Tests:** `IconTokensTest` (JUnit), `MotionTierShimTest` (Robolectric).
- **Depends on:** F3 (soft: it can ship with a local token object first).
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.
- **Design check:** n/a.

---

## I1 — `DynamicIcon` effect engine

### I1.1 — Engine core with discrete effects: bounce and wiggle
- **Goal:** One composable and one modifier that apply any `IconEffect` to an icon *or to any content* (avatar, cover art). Discrete effects are spring velocity impulses read in `graphicsLayer`.
- **Why:** PLAN §2 "Icons are alive" and 03 R10 (`symbolBounce`, "tweens into velocity impulses on springs"). Today each effect is hand-built in `IslandOverlayRoot` (`IV:705-872`) and recomposes the root.
- **Files:**
  - New: `island/icon/IconEffect.kt`, `island/icon/IconEffectState.kt`, `island/icon/DynamicIcon.kt`, `island/icon/IconEffectSpecs.kt`
  - No presentation changes yet.
- **API sketch:**
  ```kotlin
  package com.droidbubble.app.island.icon

  sealed interface IconEffect {
      /** Fires once each time `trigger` changes (never on first composition). */
      sealed interface Discrete : IconEffect
      /** Runs while `active` is true; stops cleanly when it turns false. */
      sealed interface Indefinite : IconEffect

      data class Bounce(val direction: Direction = Direction.Up, val layered: Boolean = true) : Discrete
      data class Wiggle(val axis: WiggleAxis = WiggleAxis.Rotate, val repeatEveryMs: Long? = null) : Discrete
      // I1.2
      data class Pulse(val layer: Int? = null) : Indefinite, Discrete
      data object Breathe : Indefinite
      data class Rotate(val layer: Int? = null) : Indefinite
      // I1.4
      data class VariableColor(val mode: VariableMode, val reversing: Boolean = false) : Indefinite
      data object Scale : Indefinite
      enum class Direction { Up, Down }
      enum class WiggleAxis { Rotate, Horizontal }          // bell / phone vs vibrate
      enum class VariableMode { Level, Cumulative, Iterative }
  }

  @Stable class IconEffectState internal constructor(/* Animatable scale, offsetY, rotation, offsetX, alpha */) {
      suspend fun fire(effect: IconEffect.Discrete, resolved: ResolvedEffect)   // adds velocity if already moving
      fun DrawScope/GraphicsLayerScope.apply()                                  // reads values: layer phase only
  }

  @Composable fun rememberIconEffectState(): IconEffectState

  /** Works on any content: icons, avatars, cover art. */
  fun Modifier.iconEffect(
      effect: IconEffect?,
      trigger: Any? = null,          // Discrete: fires when this changes
      active: Boolean = true,        // Indefinite: runs while true
      pivot: TransformOrigin = TransformOrigin.Center,
  ): Modifier

  @Composable fun DynamicIcon(
      icon: IslandIcon,              // V7; until V7 lands: ImageVector overload
      contentDescription: String?,
      tint: Color,
      modifier: Modifier = Modifier,
      size: Dp = IslandTokens.IconCompact,
      effect: IconEffect? = null,
      trigger: Any? = null,
      active: Boolean = true,
  )

  object IconEffectSpecs { fun resolve(effect: IconEffect, tier: MotionTier, speed: Float): ResolvedEffect }
  ```
- **Steps:**
  1. Build `IconEffectState` with five `Animatable<Float>`s at rest. `fire(Bounce)` calls `scale.animateTo(1f, Symbol.bounce.spring(speed), initialVelocity = scale.velocity + V0)`, and in parallel does the same for `offsetY` (≈ −2 dp impulse for `Up`). `fire(Wiggle)` does the same on `rotation` (`Rotate`) or `offsetX` (`Horizontal`). The velocity is additive (Convention 5).
  2. `Modifier.iconEffect` remembers the last trigger with `remember(key)` and skips the first value (Convention 4). It launches `fire` in a `LaunchedEffect(trigger)` and applies values in `graphicsLayer { state.apply() }`.
  3. `Wiggle(repeatEveryMs)` re-fires on a coroutine while `active`. This is what the ringing call uses (I3.2).
  4. Reduced: `resolve` returns `OpacityDip(150 ms)`. Efficient: same motion, no blur. Under "Remove animations", the shim's Reduced also applies.
  5. Add `IconRecompositionProbe` (debug-only counter) for tests.
- **Acceptance criteria:**
  - **Draw-phase:** firing 5 bounces in 1 s on a `DynamicIcon` gives ≤ 1 recomposition of the host (the trigger change itself), checked with the probe.
  - Interrupting a bounce mid-flight continues from the current scale and velocity: the max frame-to-frame scale delta stays < 0.05 (no snap).
  - **Reduced tier:** no `scale`/`rotation`/`translation` change at all, only an alpha dip that returns to 1 within 150 ms ±1 frame.
  - First composition with a non-null trigger fires nothing.
  - **Device check:** the I4 catalogue on a 120 Hz phone and a 60 Hz mid-range phone: bounce and wiggle look elastic and settle in ≤ 0.5 s at speed 0.7 (the default `animationSpeed`, `IslandSettings.kt:46`), and `dumpsys gfxinfo` shows no janky frames added versus S4.
- **Tests:** `IconEffectSpecsTest` (JUnit: tier mapping, speed²), `IconEffectStateTest` (JUnit with a test `MonotonicFrameClock`: additive velocity, settle, no first-fire), `DynamicIconRecompositionTest` (Robolectric, probe count).
- **Depends on:** I0.1 (F3 tokens; M5 soft, via the shim).
- **Effort:** M   **Risk:** low. It's new code with no callers yet.
- **Ships as:** Plumbing (no visible change until I3).
- **Design check:** the engine adds no glyphs or colours.

### I1.2 — Indefinite effects: pulse, breathe, rotate; migrate the two existing pulses
- **Goal:** Add Pulse, Breathe and Rotate to the engine, and move `PulsingRecordingDot` (`IV:4143-4162`) and `PulsingAlertIcon` (`LA:1028-1050`) onto it with **draw-phase** reads.
- **Why:** Both existing pulses recompose every frame (§2.1) and ignore Reduced. Apple's guidance is pulse for ongoing activity and breathe for "present" status such as recording or a timer [Apple-doc 10188].
- **Files:** `island/icon/IconEffectState.kt`, `island/icon/IconEffectSpecs.kt`; `IV:4143-4162` (dot body only); `LA:1028-1050` (alert icon body only).
- **API sketch:**
  ```kotlin
  // PulsingRecordingDot keeps its signature; body becomes:
  Box(modifier.size(11.dp).iconEffect(IconEffect.Pulse(), active = true).clip(CircleShape).background(IslandColors.recording)) // F2 role
  // PulsingAlertIcon: Icon(..., Modifier.size(size * 0.5f).iconEffect(IconEffect.Breathe, active = ringing))
  ```
- **Steps:**
  1. Implement the loops with `rememberInfiniteTransition` values read *only* in `graphicsLayer` (pass the `State`, not the value). Or write a `withInfiniteAnimationFrameMillis` loop that writes a `FloatState` read in draw.
  2. Pulse alpha 1→0.40 at 1.3 s. Breathe adds scale 1→1.08 at 2.0 s. Rotate is linear at 1.2 s/rev, with an optional `layer` for `LayeredIcon` (I1.4).
  3. Recording dot: keep the current pace and the +12 % scale feel by mapping it to **Breathe** with a scale amplitude of 0.12. Use the F2 recording colour instead of `0xFFFF3B30`.
  4. Alert icon: Breathe while ringing (keep today's 0.85↔1 character as amplitude 0.15).
  5. Reduced: Pulse is slowed to 2.0 s; Breathe becomes Pulse; Rotate stops. Efficient: loops at half frame rate.
- **Acceptance criteria:**
  - **Draw-phase:** during 3 s of recording, `PulsingRecordingDot`'s host recomposes 0 times (probe). Today it recomposes about 180 times at 60 Hz.
  - The S2 screenshot at t=0 is identical for both views (same size, same colour after F2).
  - **Reduced tier:** the dot keeps a slow opacity pulse with no scale; the alert icon has no scale.
  - Loops stop requesting frames when the card leaves composition (no frame callbacks after removal, checked by the test).
  - **Device check:** screen recording on device: the dot pulse reads the same as before, and a 60 s Perfetto trace shows no main-thread recomposition slices per frame from the dot.
- **Tests:** `IndefiniteEffectTest` (Robolectric: probe = 0 over 120 frames; stops on removal), and the existing `IslandOverlayCrashTest.recorderTimerAndCallCards_renderAndSwipe` stays green.
- **Depends on:** I1.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish. It's invisible except for the battery/recomposition win and the Reduced path.
- **Design check:** same dot, same alert glyph, same pace.

### I1.3 — Transitions: appear/disappear and replace
- **Goal:** `DynamicIconVisibility` and `DynamicIconSwitch`, so a glyph change is never a hard cut.
- **Why:** The ringer icon hard-cuts between Ring, Vibrate and Silent (`IV:2140-2144`). Play/pause uses an ad-hoc `AnimatedContent` (`IV:3610-3624`). Apple models both as content transitions [Apple-doc 10258].
- **Files:** New: `island/icon/DynamicIconTransitions.kt`. No presentation changes (I3 wires them).
- **API sketch:**
  ```kotlin
  @Composable fun <T> DynamicIconSwitch(
      target: T,
      modifier: Modifier = Modifier,
      direction: ReplaceDirection = ReplaceDirection.DownUp,   // DownUp = reaction to input; UpUp = progression
      content: @Composable (T) -> Unit,
  )
  @Composable fun DynamicIconVisibility(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit)
  ```
- **Steps:**
  1. Build a two-slot layer, outgoing and incoming. Outgoing: `effects` spec, alpha→0, scale→0.6, offset ±4 dp. Incoming: `Symbol.replace` spring from scale 0.6 and offset ∓4 dp. All values are read in `graphicsLayer`. A third target retargets from the current frame; no queueing.
  2. Visibility: scale 0.5→1, alpha, plus an optional 4→0 dp blur via `steppedBlurEffect` (Full only).
  3. Reduced: a 150 ms crossfade, no scale or offset. Efficient: no blur.
  4. Layout stays fixed: the slot measures the max of both children. Apple's "indefinite appear keeps layout" principle applies, and the island's geometry owns size changes (M1).
- **Acceptance criteria:**
  - **Draw-phase:** the switch recomposes once per *target change*, not per frame.
  - Toggling A→B→A within 100 ms ends on A with no jump of more than 0.05 in scale between frames.
  - **Reduced tier:** crossfade only (alpha changes; scale and offset stay constant).
  - **Device check:** ringer toggling on device (Volume key → Vibrate → Silent → Ring quickly). The glyph never flashes blank and never pops.
- **Tests:** `DynamicIconSwitchTest` (Robolectric: frame stepping, retarget continuity, Reduced constants).
- **Depends on:** I1.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.
- **Design check:** no glyph or colour changes.

### I1.4 — Variable color: `LayeredIcon`
- **Goal:** Draw an icon as ordered layers whose emphasis follows a level (0..1) or a cumulative/iterative loop. Used for signal, network arrows, download, flashlight beam and battery.
- **Why:** PLAN I1 "variable color (layered fill for signal, volume, battery and Wi-Fi)". Apple: layer order drives variable color; cumulative builds up, iterative scans [Apple-doc 10197]. Material's equivalent principle is the FILL axis for state [developers.google.com].
- **Files:** New: `island/icon/LayeredIcon.kt`, `island/icon/IconLayers.kt` (layer path definitions for the needed glyphs: 3-arc Wi-Fi, 4-bar signal, 2-arrow transfer, 3-segment download fill, 3-ray beam).
- **API sketch:**
  ```kotlin
  @Immutable class IconLayers(val viewport: Size, val base: List<PathNode>?, val layers: List<List<PathNode>>)

  @Composable fun LayeredIcon(
      layers: IconLayers,
      tint: Color,
      modifier: Modifier = Modifier,
      size: Dp = IslandTokens.IconCompact,
      level: () -> Float? = { null },          // lambda: read in draw, so level ticks don't recompose
      mode: IconEffect.VariableMode = IconEffect.VariableMode.Level,
      active: Boolean = true,
      inactiveAlpha: Float = 0.25f,
  )
  ```
- **Steps:**
  1. `drawWithCache` builds one `Path` per layer when the size changes. `onDrawWithContent` draws each layer with its alpha from a per-layer `Animatable`.
  2. Level mode: `lit = ceil(level · N)`. Each layer animates to its target alpha with the `effects` spec, staggered by 40 ms from the first layer (by-layer).
  3. Cumulative and Iterative modes run a 150 ms/step loop while `active`; `reversing` loops back.
  4. Reduced: level shown with a 150 ms crossfade; loops show a static 0.6 alpha. Efficient: loops at half rate.
  5. `CellularSignalBars` (`IV:4234-4256`) becomes a thin wrapper over `LayeredIcon`. It should be pixel-identical at rest.
- **Acceptance criteria:**
  - **Draw-phase:** changing `level` 10×/s for 3 s gives 0 recompositions (the lambda is read in draw).
  - S2: `CellularSignalBars` is pixel-identical at levels 0–4.
  - **Reduced tier:** no loop frames are requested; level changes crossfade.
  - **Device check:** the I4 catalogue level slider on device: layers light up in order with a visible by-layer stagger, and nothing flickers at 120 Hz.
- **Tests:** `LayeredIconTest` (JUnit: level→lit mapping, including 0 and 1 edges; Robolectric: probe = 0; Reduced no frames).
- **Depends on:** I1.1. V7 for the glyph geometry; until then use layer paths drawn from primitives.
- **Effort:** M   **Risk:** medium. Hand-made layer paths must match V7's glyphs optically (designer review).
- **Ships as:** Plumbing, plus an invisible refactor of the signal bars.
- **Design check:** layer paths follow the V7 Material Symbols Rounded geometry (Apache 2.0), not SF Symbols. Inactive alpha comes from an F2 token.

---

## I2 — Morphing state icons

### I2.1 — Morph toolkit and play↔pause
- **Goal:** `MorphIcon`, built on `graphics-shapes` `Morph`, and the first user: a play↔pause that *melts* instead of swapping. It applies to the media card button (`IV:3610-3624`) and the recorder/timer pause buttons (`LA:735`, `LA:816`).
- **Why:** PLAN I2. Play/pause is the most-touched control. Today it's a crossfade in one place and a hard swap in two others (§2.1).
- **Files:** New: `island/icon/MorphIcon.kt`, `island/icon/MorphShapes.kt`; `IV:3610-3624`; `LA:735-737`, `LA:816-818` (icon argument → `MorphIcon` content in `LiveRoundButton`, `LA:499-527`).
- **API sketch:**
  ```kotlin
  /** Two-part morph: each part is a RoundedPolygon pair; corners use squircle smoothing 0.6. */
  @Immutable class MorphGlyph(val parts: List<Pair<RoundedPolygon, RoundedPolygon>>)

  object MorphShapes {
      // Play = triangle split into left quad + right triangle-ish quad; Pause = two bars.
      val PlayPause: MorphGlyph   // CornerRounding(radius = 0.08f * unit, smoothing = 0.6f)
  }

  @Composable fun MorphIcon(
      glyph: MorphGlyph,
      atEnd: Boolean,                 // false = start shape (play), true = end shape (pause)
      tint: Color,
      contentDescription: String?,
      modifier: Modifier = Modifier,
      size: Dp = IslandTokens.IconHeader,
  )
  ```
- **Steps:**
  1. **Route decision (documented in the file header).** Morph (already on the classpath) is the default. AVD via `animation-graphics` is only allowed for a glyph that can't be built from convex polygons plus strokes. That would be one BOM-managed dependency with owner sign-off, and none of the I2 glyphs needs it.
  2. Build the `RoundedPolygon` vertex lists in unit space, with an optical centre: the play triangle shifts +4 % to the right, the standard optical correction that V7 also applies. `Morph(start, end)` is created once in `remember`.
  3. Progress is an `Animatable` driven by `Symbol.morph`. In `drawWithCache`, reuse one `android.graphics.Path`. Call `morph.toPath(progress, path)` (or the cubic list into a Compose `Path`) and draw it scaled to the size.
  4. Direction is up/up (progression, per Apple's play→pause example): add a 1 dp upward drift on change.
  5. Reduced: crossfade the two end paths in 150 ms.
  6. Replace the three call sites, and keep the `contentDescription` semantics ("Play/Pause", "Pause"/"Resume").
- **Acceptance criteria:**
  - **Draw-phase:** a toggle recomposes once (the `atEnd` change); morph frames don't recompose (probe).
  - At rest, play and pause are visually equivalent to today's Rounded glyphs at `playBtnIcon` size. S2 shows an intended diff only in corner rounding (squircle smoothing), signed off by the owner.
  - Rapid double-tap mid-morph reverses smoothly from the current progress.
  - **Reduced tier:** crossfade, no shape interpolation.
  - **Device check:** Spotify/YouTube Music on device: tap play/pause on the island card 10×. The morph never shows self-intersecting shapes and stays crisp at 1×–1.6× `expandedScale`.
- **Tests:** `MorphShapesTest` (JUnit: polygons are convex; `Morph` at 0 and 1 matches the start and end bounds within 0.5 %); `MorphIconTest` (Robolectric: probe, Reduced crossfade).
- **Depends on:** I1.1 (tokens and tier plumbing), I0.1. V7 soft (size tokens).
- **Effort:** M   **Risk:** medium. Splitting the triangle into two morphable halves needs tuning. Verify `toPath` on API 29.
- **Ships as:** Polish (visible).
- **Design check:** same button container, same tint, and glyph proportions matched to Material Symbols Rounded `play_arrow`/`pause`. No SF Symbols geometry.

### I2.2 — Draw-on/off: checkmark draw-on and the slash family (bell↔bell-slash)
- **Goal:** `DrawOnIcon`, a stroke reveal, and `SlashOverlay`, Android's take on Apple's magic replace: the base glyph stays and only the slash draws on or off. Apply them to the unlock/Bluetooth checkmark (`IV:2062-2080`, `2107-2125`) and the ringer Ring↔Silent change.
- **Why:** PLAN I2 "bell↔bell-slash, checkmark draw-on". Today the checkmark is a scale pop with a composition-phase alpha read and a wall-clock `delay` (`IV:705-760`). Ring↔Silent is a hard cut.
- **Files:** New: `island/icon/DrawOnIcon.kt`, `island/icon/SlashOverlay.kt`; `IV:705-760` (state removed), `IV:2062-2125` (call sites), `IV:2131-2171` (ringer icon block).
- **API sketch:**
  ```kotlin
  @Composable fun DrawOnIcon(stroke: IconStrokePath, drawn: Boolean, tint: Color, modifier: Modifier = Modifier,
                             size: Dp = IslandTokens.IconCompact, delayFraction: Float = 0f)
  @Composable fun SlashOverlay(slashed: Boolean, tint: Color, slashTint: Color = tint,
                               modifier: Modifier = Modifier, content: @Composable () -> Unit)  // content = base glyph
  ```
- **Steps:**
  1. `DrawOnIcon`: in `drawWithCache`, build the full path and a `PathMeasure` once, and keep one `dst` path. On each draw, `dst.reset(); measure.getSegment(0f, len * p, dst, true)`, then stroke it with a round cap. `p` is an `Animatable` on `Symbol.draw`. Replace the 110 ms wall-clock stagger with `delayFraction` measured against the pill's appear progress (03 R4 principle). Until M3, use a 0.3 delay on the same spring.
  2. `SlashOverlay`: `graphicsLayer { compositingStrategy = Offscreen }`. Draw `content`, then a `BlendMode.Clear` gap stroke 1.5× wider, then the slash, both trimmed by the same `p`. The slash direction is top-left → bottom-right, matching Material `notifications_off`.
  3. Ringer: Ring↔Silent uses `SlashOverlay` over the bell (magic replace: the shared bell stays). Any change to or from Vibrate uses `DynamicIconSwitch` (I1.3), because the glyphs are unrelated. That's the same fallback Apple describes.
  4. Delete the six checkmark and ringer-label state blocks from `IslandOverlayRoot` (`IV:705-794`), which removes per-frame root recomposition. Keep the ringer label scale/alpha, but move its reads into `graphicsLayer`.
  5. Reduced: draw-on becomes a 150 ms fade-in of the full stroke; the slash crossfades.
- **Acceptance criteria:**
  - **Draw-phase:** an unlock or Bluetooth alert causes 0 per-frame recompositions of `IslandOverlayRoot` from the checkmark. Today `IV:2067` reads in composition.
  - The checkmark at rest is pixel-close to today's `Icons.Default.Check` at 19 dp (S2, intended diff only during the animation).
  - The Silent glyph at rest matches `NotificationsOff` (Rounded after V7) within 1 px.
  - **Reduced tier:** no trim animation, fade only.
  - **Device check:** lock/unlock 5× and connect earbuds. The check visibly *draws* left→right after the pill appears. Flip the ringer switch Ring→Silent→Ring: the slash draws across and retracts with no blink.
- **Tests:** `DrawOnIconTest` (JUnit: segment length at p = 0, 0.5, 1; Robolectric: probe); `IslandOverlayCrashTest` stays green.
- **Depends on:** I1.1, I1.3.
- **Effort:** M   **Risk:** low–medium. Offscreen compositing on a 19 dp layer is cheap, but confirm it on API 29.
- **Ships as:** Polish (visible).
- **Design check:** the same check, bell and slashed-bell glyphs; the silent red becomes the F2 role (02 §2 "three reds").

### I2.3 — Flashlight on↔off and mic↔mic-off (slash family); mute asset ready
- **Goal:** Reuse `SlashOverlay` for the flashlight (compact, bubble, expanded tap-to-off) and the recorder mic, and provide `MuteGlyph` ready for the Phase 7 call controls.
- **Why:** PLAN I2 "flashlight on↔off, mute↔unmute, mic and waveform". `FlashlightOff` is imported but never used (`IV:94`). Turning the torch off from the expanded card (`IV:3255-3276`) collapses with no feedback. **There's no mute control on the island today:** `ActiveCallState.isMuted` (`model/IslandEvent.kt:156`) is never rendered, and phase-7 A3.2 adds the mute/speaker buttons.
- **Files:** `IV:2686-2711`, `IV:2560-2580`, `IV:3255-3276`; `IV:2480` (bubble mic); new `island/icon/SlashGlyphs.kt` (flashlight, mic and mic-off base paths, or V7 `IslandIcon`s).
- **API sketch:** `SlashOverlay(slashed = !on) { DynamicIcon(IslandIcon.Flashlight, …) }` and `SlashOverlay(slashed = paused) { DynamicIcon(IslandIcon.Mic, …) }`.
- **Steps:**
  1. Expanded flashlight tap: draw the slash on (≈ 200 ms), then call `onTurnOff()`. The collapse is driven by state, so the slash completes as the card leaves: `onTurnOff` is called at p ≥ 0.6, not after a `delay`.
  2. Bubble mic (`IV:2480`): slash while the recorder is paused (`LiveActivityControls.isPaused`, `LA:273-282`).
  3. Mute: add `MuteGlyph` and a catalogue entry only (I4). Phase 7 A3.2 consumes it.
  4. Reduced: crossfade.
- **Acceptance criteria:**
  - **Draw-phase:** probe = 0 during the slash.
  - Torch off from the expanded card shows the slash before the card is gone (device-visible), and the collapse timing isn't lengthened by more than 1 frame.
  - **Reduced tier:** crossfade.
  - **Device check:** toggle the torch from QS and from the island: the compact glyph appears (I3.3) and the off state is never shown as a stale "on".
- **Tests:** a Robolectric render of each slashed or unslashed glyph; `IslandOverlayCrashTest` green.
- **Depends on:** I2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.
- **Design check:** the same flashlight and mic glyphs, no new colours.

---

## I3 — Wiring icons to events

### I3.1 — Icon event signals in `IslandStateManager`
- **Goal:** Pure, testable *event counters* for the discrete effects, so views don't guess from data diffs. Examples: "same app posted again", "call started ringing", "alert started", "connection appeared", "charging started".
- **Why:** Discrete effects need a trigger that changes exactly once per real event (Convention 4). Today the info is implicit: `isNewNotification` (`ISM:605`) is false for a same-key repost with new text, which is how chat apps update a conversation. And timers repost every second (`isLiveActivity`, `IslandEvent.kt:92-93`), so naïve "text changed" would bounce every tick.
- **Files:** `ISM` (new `IconSignals` flow), `model/IslandEvent.kt` (none). Phase 7 note: when A1.3 lands, this moves to `LiveActivitySpec.lastSignificantAt` and `stateSignature` (phase-7 A1.2). Keep the signal names.
- **API sketch:**
  ```kotlin
  @Immutable data class IconSignals(
      val sameAppNudge: Int = 0,      // non-live notification from the SAME package as the one showing (new key, or same key + new text)
      val ringStart: Int = 0,         // incoming call began ringing
      val alertStart: Int = 0,        // alarm / Time's up began
      val connectionAppear: Int = 0,  // BT / Wi-Fi alert shown
      val chargeStart: Int = 0,       // charging glance shown
  )
  val iconSignals: StateFlow<IconSignals>
  ```
- **Steps:**
  1. In `updateNotification`, around `ISM:603-607`: if `previous != null && notification != null && !notification.isLiveActivity && previous.packageName == notification.packageName && (previous.key != notification.key || previous.text != notification.text)`, then `sameAppNudge++`.
  2. Bump `ringStart` and `alertStart` inside the existing branches at `ISM:609-618` (the same conditions that call `expand`).
  3. Bump `connectionAppear` at `ISM:466` and `chargeStart` at `ISM:490`.
  4. Reset everything in `resetForTest()` (`ISM:770`).
- **Acceptance criteria:**
  - A Google Clock timer reposting every second for 10 s gives `sameAppNudge` = 0.
  - Two WhatsApp messages from the same chat (same key, new text) give +1. A message from a different app gives +0 (that's a swap, owned by M3).
  - **Draw-phase / Reduced / device:** n/a (pure state). **Device check:** none needed; it's covered by tests with the S5 fixtures.
- **Tests:** `IconSignalsTest` (JUnit, `IslandStateBehaviorTest` style; uses S5 fixtures `message/whatsapp_ar`, `clock/gclock_timer_running_en` if present).
- **Depends on:** none (S5 fixtures optional). Can start immediately.
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.
- **Design check:** n/a.

### I3.2 — Wire the attention effects: bounce, wiggle, pulse, appear, replace
- **Goal:** Connect I3.1's signals and existing state to the compact pill, bubble and card icons, following the §4 table rows 1, 2, 5, 6, 7 and 8.
- **Why:** PLAN I3. This is the visible payoff.
- **Files:** `IV:2683-2737` (compact leading), `IV:2474-2507` (bubble), `IV:2081-2171` (connection and ringer pills; the keyframe shakes `IV:796-872` are removed), `LA:1265`, `LA:1281-1286` (compact call and alert icons), `LA:684-692` (answer button), `LA:949-953` (alert header).
- **API sketch:**
  ```kotlin
  val signals by IslandStateManager.iconSignals.collectAsState()
  // compact notification leading slot:
  CompactContactAvatar(..., modifier = Modifier.iconEffect(IconEffect.Bounce(), trigger = signals.sameAppNudge))
  // ringing call:
  Icon(..., Modifier.iconEffect(IconEffect.Wiggle(repeatEveryMs = 2400), active = notification.isIncomingCall))
  ```
- **Steps:**
  1. **Same-app bounce:** on the leading avatar or app icon in the compact pill and bubble. Pair it with an `IslandHaptics.tick` (F3), gated by the haptics toggle. If the island is expanded, bounce the card header icon instead.
  2. **Incoming call:** the compact phone glyph wiggles (rotate, pivot at the handset centre), repeating every 2.4 s while ringing. The expanded answer button does a gentle **bounce** every 2.4 s, out of phase with the wiggle. It stops the moment the call is answered.
  3. **Timer done / alarm:** one `Wiggle` on `alertStart`, then `Breathe` (I1.2) while ringing, in compact and expanded. This resolves the plan's "shake": shake = wiggle, rotate axis.
  4. **Recording:** the screen-record dot is already Breathe (I1.2). The bubble mic for voice recorders (`IV:2480`) gets Pulse while not paused.
  5. **Bluetooth / Wi-Fi connect:** the glyph uses `DynamicIconVisibility` (appear) at pill appear, and the checkmark draw-on (I2.2) follows.
  6. **Ringer:** Ring↔Silent uses the slash (I2.2). To or from Vibrate uses `DynamicIconSwitch`. On *arrival* in Ring, Wiggle(Rotate) replaces the bell keyframes (`IV:796-849`); on arrival in Vibrate, Wiggle(Horizontal) replaces `IV:851-872`. The raw waveform haptic (`IV:812-814`) becomes `IslandHaptics`.
- **Acceptance criteria:**
  - Each row of §4 rows 1, 2, 5–8 is demonstrable from the I4.2 island demos.
  - **Draw-phase:** with a ringing call for 10 s, the root recomposes ≤ 1×/s (the timer text), not per frame. The probe on the wiggle host reads 0 per frame.
  - **Reduced tier:** no transform anywhere. The bounce and wiggle become opacity dips; Breathe becomes Pulse; replace becomes a crossfade. The bell keyframes are gone in every tier.
  - A burst of 5 same-app messages in 2 s gives one continuous lively bounce (additive velocity), with no stutter.
  - **Device check:** a real WhatsApp burst, a real incoming call (Google Dialer plus one OEM dialer), a Google Clock Time's up, earbuds connect, and the ringer switch. Recorded at 60 fps, it's reviewed by the owner against the "alive but restrained" bar.
- **Tests:** a Robolectric `IconWiringTest` drives `IslandStateManager` and asserts the effect state on tagged nodes (semantics property `IconEffectActive` exposed by `iconEffect` in debug). `IslandOverlayCrashTest.incomingCall_withBlinkingFlash_staysOnCallCard` stays green.
- **Depends on:** I1.1, I1.2, I1.3, I2.2, I3.1; F3 `IslandHaptics`. Coordinate with M3 (it touches the same compact block `IV:2226-2264`).
- **Effort:** M   **Risk:** medium. This is the most-edited part of `IslandView.kt`.
- **Ships as:** Polish (visible).
- **Design check:** no new glyphs, the same sizes and colours, and haptics only on the bounce and the arrival wiggle.

### I3.3 — Wire the level effects: charging bolt plus fill, download, network arrows, flashlight level
- **Goal:** §4 rows 3, 4, 9 and 11, the variable-color family.
- **Why:** PLAN I3: "Charging: bolt bounce plus fill", "Download: progress icon fills", "Network speed: variable-color arrows". Plus flashlight level, which complements V3's trailing value.
- **Files:**
  - `IV:1961-2036` (charging pill)
  - `IV:2740-2757`, `IV:2509-2538` (download leading and bubble)
  - `IV:2841-2858`, `IV:2603-2611`, `IV:4569-4615` (network)
  - `IV:2686-2711`, `IV:2560-2580` (flashlight)
  - `IV:875-891` (charging fraction moves to a draw read)
- **API sketch:**
  ```kotlin
  LayeredIcon(IconLayers.TransferArrows, tint, level = { netLevel(info.downloadBytesPerSec) }, mode = Level)
  fun netLevel(bps: Long): Float  // log buckets: <50 KB/s → 0.33, <2 MB/s → 0.66, else 1.0  [Estimate, tune in I4]
  ```
- **Steps:**
  1. **Charging:** add a bolt glyph (V7 `IslandIcon.Bolt`, `IconCompact` minus 4 dp) before the wattage text in the *existing* leading box. It bounces on `chargeStart`, and the fill rises inside the bolt with `LayeredIcon` Level = %. The pill-wide green fill stays, but its fraction is read in draw (`drawBehind` width = fraction), not `fillMaxWidth(fraction)`. **Owner design check required.** If it doesn't fit (V3 fit order), the bolt is dropped first.
  2. **Download:** the leading glyph is `LayeredIcon(DownloadFill)` with Level = `progressFraction`. Indeterminate uses `Iterative` mode. The bubble ring animates with a ζ 0.9 spring (03 R10).
  3. **Network:** compact, bubble and expanded use `LayeredIcon(TransferArrows)`: down-arrow layer level from download bps, up-arrow from upload bps. Wi-Fi uses the 3-arc layered glyph with the same level.
  4. **Flashlight:** a 3-ray beam layer over the torch glyph, Level = `flashlightLevel / maxFlashlightLevel`. Appear on torch on (via `DynamicIconVisibility`).
  5. Reduced: static levels with a 150 ms crossfade on change.
- **Acceptance criteria:**
  - **Draw-phase:** network updates (≈1 Hz, `PersistentWidgetTelemetryManager.kt:211`) and download progress cause 0 per-frame recompositions. Level changes recompose only the text, not the icon (lambda read).
  - The charging fill no longer recomposes per frame during its 750 ms rise.
  - **Reduced tier:** no loops (indeterminate download shows static 0.6).
  - **Device check:** plug in a charger (the bolt bounces once, the fill matches %), a Play Store download (the glyph fills in sync with %), a speed test (the arrows step up), and a torch brightness change on an API 33+ device with strength levels.
- **Tests:** `NetLevelTest` (JUnit buckets), Robolectric probe tests per wired view, S2 diffs for the charging pill (intended: the bolt).
- **Depends on:** I1.1, I1.3, I1.4, I3.1; V3 soft (flashlight trailing slot, compact width); V7 soft (bolt glyph).
- **Effort:** M   **Risk:** medium. The charging layout change needs owner sign-off.
- **Ships as:** Polish (visible).
- **Design check:** the bolt is the only new element. Colours: charging green and network blue (F2 roles). No new layouts beyond the bolt.

### I3.4 — Media: tokenised cover flip and play-state morph wiring
- **Goal:** §4 row 10. `FlippingCoverArt` uses `IslandMotion` and tiers, and play-state changes use `MorphIcon` (I2.1) everywhere play state is shown.
- **Why:** The flip (`IC:516-556`) uses literal specs and has no Reduced path. Track change and play-state change are separate events: the plan's "track change → art flip plus play-state morph" really means the flip on `trackKey` and the morph on `isPlaying`.
- **Files:** `IC:516-556`; call sites `IV:2544`, `IV:2776`, `IV:3440` (unchanged signatures).
- **API sketch:** `FlippingCoverArt(art, trackKey, …)` stays the same; internally the turn-away half uses the `effects` token and the turn-in half uses `IslandMotion.Symbol.replace`.
- **Steps:**
  1. Replace `tween(150)` and `spring(0.7, 380)` with tokens scaled by speed.
  2. Reduced: crossfade old→new art in 150 ms, no rotation.
  3. Efficient: the flip is kept (it's cheap), and the album blur is untouched (03 R10 moves it off-main separately).
  4. Optional: a bounce on the compact equalizer on track change is not added. One effect per event (restraint).
- **Acceptance criteria:**
  - **Draw-phase:** rotation is still read in `graphicsLayer` (unchanged), and the flip causes no extra recompositions beyond the art swap.
  - **Reduced tier:** no rotation.
  - **Device check:** skip tracks 5× quickly. Each flip completes or retargets without a blank frame, and late art still swaps silently.
- **Tests:** a Robolectric `FlippingCoverArtTest` with a Reduced shim and frame stepping.
- **Depends on:** I0.1, I2.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Polish.
- **Design check:** the same flip, the same art shapes.

---

## I4 — Icon-effect catalogue

### I4.1 — Effect catalogue inside the Test Live Activities panel
- **Goal:** A section in the existing `LiveActivityTestPanel` (`ui/main/LiveActivityTestPanel.kt:27`, shown at `MainScreen.kt:1004`) that renders every effect and morph on a black squircle tile. It has trigger buttons, an active toggle, a level slider, a tier override (Full/Efficient/Reduced) and a speed readout, so each effect can be previewed and tuned.
- **Why:** PLAN I4. The token values in §1.3 are [Estimate]s and must be tuned by eye on a device.
- **Files:** `ui/main/LiveActivityTestPanel.kt` (add a collapsible "Icon effects" section); new `ui/main/IconEffectCatalog.kt`.
- **API sketch:**
  ```kotlin
  @Composable fun IconEffectCatalog(tierOverride: MotionTier?, speed: Float)
  // rows: Bounce, Wiggle(rotate/horizontal), Pulse, Breathe, Rotate, VariableColor(level/cumulative/iterative),
  //       Appear/Disappear, Replace(down-up/up-up), Slash (bell/flashlight/mic/mute), Draw-on (check), Morph (play↔pause)
  ```
- **Steps:**
  1. Host each effect in a 56 dp black `SquircleShape` tile, with the icon at the real compact size, plus a 2× zoom tile for inspection.
  2. Provide `LocalMotionTier` from the override, and read `settings.animationSpeed`, so it previews what the island will run (03 R9 honesty).
  3. Show each token's d/bounce and its ζ/k, read-only. Tuning edits the token file; no runtime editor (keeps scope S/M).
- **Acceptance criteria:**
  - Every `IconEffect` subtype and every `MorphGlyph`/slash glyph appears.
  - The tier override visibly switches to the §1.3 fallbacks.
  - **Draw-phase:** scrolling the panel while 5 loops run stays at frame rate (no recomposition per frame, probe).
  - **Reduced tier:** the override honours it, and so does the system "Remove animations".
  - **Device check:** run on a phone; the owner signs off each effect's feel. The tuned values go back into `IslandMotion.Symbol`.
- **Tests:** a Robolectric render test (all rows compose without crashing in each tier).
- **Depends on:** I1.1–I1.4, I2.1–I2.3. It can land incrementally: the first version renders whatever exists.
- **Effort:** M   **Risk:** low.
- **Ships as:** Debug/tuning UI inside the existing settings card.
- **Design check:** uses the settings app's existing card and button styles (`SettingsCardItem`, `FilledTonalButton`), so no restructure.

### I4.2 — Island event demos
- **Goal:** Buttons in the same panel that fire the *real* events on the island: same-app message ×3, connect BT, ringer cycle, charging at 40 %, download 0→100, network ramp, torch levels. Each §4 row is device-testable without real hardware.
- **Why:** I3.2 and I3.3 device checks. Some events (ringer, BT) are awkward to reproduce on demand.
- **Files:** `island/LiveActivityTester.kt` (new `Demo` entries; reuse `post`); `ui/main/LiveActivityTestPanel.kt`.
- **Steps:**
  1. Add the demos through the public `IslandStateManager` calls (`triggerBluetoothEvent`, `triggerRingerModeEvent`, `triggerChargingEvent`, `updateNetworkSpeed`, `updateFlashlightState`) and through `updateNotification` with `droidbubble.test.*` keys.
  2. Never use the debug broadcast receiver (06 P1-4 removes it from release).
- **Acceptance criteria:** each §4 row can be triggered from the panel. `clear()` resets all signals. **Reduced/draw-phase:** inherited. **Device check:** run the full §4 table on device.
- **Tests:** extend `LiveActivityTester` coverage in `IslandStateBehaviorTest` (demo → expected signal counters).
- **Depends on:** I3.1, and I3.2/I3.3 for visible results.
- **Effort:** S   **Risk:** low.
- **Ships as:** Debug UI.
- **Design check:** existing panel styles only.

---

## 4. Event → effect wiring table

The *trigger* is where the state changes (the line that makes the event true). The *render* is where the icon is drawn and where I3 attaches the effect.

| # | Event | Effect (tier Full) | Trigger: state line | Trigger: upstream source | Render site(s) | Task |
|---|---|---|---|---|---|---|
| 1 | New notification from the **same app** | **Bounce** (up) on the leading avatar or app icon, plus a haptic tick | `ISM:603-607` (`previous`, `isNewNotification`, `_activeNotification.value = notification`), new `sameAppNudge` from I3.1 | `NRS:1323` (`updateNotification(notifInfo)`) | Compact `IV:2715-2737`; bubble `IV:2477-2507`; expanded header (notification view) | I3.1, I3.2 |
| 2 | Recording in progress (screen) | **Breathe** on the dot (Pulse in Reduced) | `ISM:735-738` (`_screenRecordingState.value = state`) | `ScreenRecordingService.kt:191, 209, 226, 257`; `NRS:1017-1026` | `IV:2683-2685`, `IV:2474-2476` → `PulsingRecordingDot` `IV:4143-4162` | I1.2 |
| 2b | Recording in progress (voice recorder) | Waveform stays (already alive, `LA:569-615`); bubble mic **Pulse**; slash on pause | `ISM:607` (live notification of `RECORDING` kind) | `NRS:1323` | Compact `LA:1266-1271`; bubble mic `IV:2480` | I2.3, I3.2 |
| 3 | Charging starts | **Bounce** on a new bolt, plus a **variable-color** fill = % | `ISM:485-495` (`_activeCharging.value = …` at `:490`) | `IOS:361` (power broadcast), `service/PowerConnectionReceiver.kt:30`; wattage `IOS:371` | Pill `IV:1961-2036` (fill `IV:1969-1983`, label box `IV:2006-2018`); fraction `IV:875-891` | I3.3 |
| 4 | Download progress | **Variable color** (Level) on the download glyph; **Iterative** when indeterminate | `ISM:645-649` (`_activeProgress.value = progress`) | `NRS:1320` (`updateProgress(notifInfo)`) | Leading `IV:2740-2757`; trailing % `IV:2913-2934`; bubble `IV:2509-2538` | I1.4, I3.3 |
| 5 | Incoming call | **Wiggle** (rotate), repeated every 2.4 s while ringing; answer button **Bounce** off-phase | `ISM:607-613` (`isIncomingCall` → `expand` at `:612`); `isIncomingCallRinging` `ISM:254`; `ringStart` from I3.1 | `NRS:1323` (CallStyle / `CATEGORY_CALL` notification) | Compact `LA:1265`; answer button `LA:684-692` | I3.2 |
| 6 | Timer done / alarm ringing | **Wiggle** once on arrival, then **Breathe** while ringing (resolves the plan's "shake") | `ISM:614-618` (`isAlert` → `expand` at `:617`); `alertStart` from I3.1 | `NRS:1323` | Compact `LA:1281-1286`; expanded `LA:949-953` → `PulsingAlertIcon` `LA:1028-1050` | I1.2, I3.2 |
| 7 | Bluetooth (or Wi-Fi) connects | **Appear** on the BT/Wi-Fi glyph, then the checkmark **draw-on** | `ISM:460-471` (`_isBluetoothConnected.value = true` at `:466`) | BT `IOS:335`, `IOS:345`; Wi-Fi `IOS:272`; debug `IOS:225` | `IV:2081-2105` (glyph), `IV:2107-2125` (check); state `IV:737-760` (removed) | I2.2, I3.2 |
| 8 | Ringer mode changes | **Magic replace** (slash draws on/off) for Ring↔Silent; **Replace** to/from Vibrate; arrival **Wiggle** (rotate for Ring, horizontal for Vibrate) | `ISM:473-483` (`_activeRingerMode.value = mode` at `:478`) | `IOS:303-305` (`RINGER_MODE_CHANGED`); debug `IOS:248` | Icon `IV:2131-2171`; shakes `IV:796-872` (replaced); label `IV:2176-2211` | I2.2, I3.2 |
| 9 | Network speed | **Variable color** (Level) on the down/up arrows (or the Wi-Fi arcs) from bytes/s | `ISM:171-175` (`_networkSpeedInfo.value = info` at `:173`) | `service/PersistentWidgetTelemetryManager.kt:84, 211` | Compact `IV:2841-2858`; bubble `IV:2603-2611`; expanded `IV:4569-4615` | I1.4, I3.3 |
| 10 | Media track changes / play state | Cover **flip** (exists, tokenised) on `trackKey`; play↔pause **morph** on `isPlaying` | `ISM:528-533` (`trackKey` vs `lastTrackKey`); `ISM:523` (`_activeMedia.value = media`) | `NRS:658` (`updateMedia`), `NRS:242` | Flip `IC:516-556` (used `IV:2544`, `2776`, `3440`); play/pause `IV:3610-3624`; recorder/timer buttons `LA:735`, `LA:816` | I2.1, I3.4 |
| 11 | Flashlight on / level / off | **Appear** on torch on; **variable-color** beam = level; **slash** draw-on when turned off from the card | `ISM:370-373` (`_isFlashlightOn.value = enabled`); level `ISM:425-427`; filter `ISM:395-413` | `IOS:441` (`TorchCallback.onTorchModeChanged`); debug `IOS:226-230` (`ACTION_SIMULATE_FLASHLIGHT`) | Compact `IV:2686-2711`; bubble `IV:2560-2580`; expanded `IV:3255-3276` | I2.3, I3.3 |
| 12 | *(optional, not in the plan table)* Unlock | Checkmark **draw-on**; optional lock→unlock **replace** | `ISM:443-454` (`_isUnlocked.value = true` at `:448`) | `IOS:311`, `IOS:329` | `IV:2037-2080`; state `IV:705-730` (removed) | I2.2 |

Phase 7 adds rows later. Each one names an effect from this table and never designs its own (phase-7 header):
- A7.1/A7.2: low battery → Pulse
- A7.6: DND → magic replace
- A7.8: mic/camera in use → Pulse
- A3.2: mute → `MuteGlyph` slash

---

## 5. Execution order

```
I0.1 tokens + tier shim ──┬─> I1.1 engine core ──┬─> I1.2 indefinite (migrates 2 pulses)
                          │                      ├─> I1.3 transitions
                          │                      └─> I1.4 variable color
I3.1 signals (pure; start anytime) ─────────────────────────────────────┐
                          └─> I2.1 morph toolkit + play/pause (after I1.1)│
                               I2.2 draw-on + slash (after I1.1, I1.3) ──> I2.3 flashlight/mic/mute
I3.2 attention wiring  (after I1.1–I1.3, I2.2, I3.1)
I3.3 level wiring      (after I1.4, I3.1; bolt needs owner OK)
I3.4 media             (after I2.1)
I4.1 catalogue         (incremental, from I1.1 on) ──> I4.2 island demos (after I3.1)
```

**Can start before Phase 3 (M1–M5):** all of them. Phase 5 needs F3 (or the I0.1 local token fallback), and M5 is shimmed by I0.1.
- **Free to start now, no contention:** I0.1, I1.1, I1.2 engine parts, I1.3, I1.4, I2.1 toolkit, I3.1 and I4.1. They touch only `island/icon/*`, `IslandStateManager.kt` or `ui/main/*`.
- **Before Phase 3 but serialised against M tasks on `IslandView.kt`:** I2.2, I2.3, I3.2, I3.3 and I3.4 edit `IslandView.kt`/`LiveActivities.kt`. Run them **before M3** (which re-keys the compact `AnimatedContent`, `IV:2226-2264`) or **after M3**, never at the same time. Prefer them after the 06 Phase-5 file split, if it lands first (PLAN §5).
- **After M5 lands:** delete the shim's tier logic. `LocalMotionTier` becomes M5's provider (one-line change), and no I task needs rework.
- **V7 interplay:** the engine is glyph-agnostic. I1.x and I3.1 don't need V7. I1.4 layer paths, the I2 glyph geometry and the I3.3 bolt should follow V7's Rounded family. If V7 is late, ship with today's glyphs and re-skin later at no cost to the engine.

**Recommended sequence** (one writing session per file at a time):
1. **I0.1** ∥ **I3.1**
2. **I1.1**
3. **I1.2** ∥ **I1.3** ∥ **I1.4** ∥ **I2.1** (engine files only, except I1.2's two pulse bodies)
4. **I4.1** (first cut, to tune tokens on device)
5. **I2.2** → **I2.3**
6. **I3.2**
7. **I3.3** (after the owner OKs the bolt)
8. **I3.4**
9. **I4.2**, then tune `IslandMotion.Symbol` from the catalogue and close the phase.
