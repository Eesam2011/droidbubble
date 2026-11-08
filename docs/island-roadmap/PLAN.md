# Droidbubble Island Roadmap — Master Plan (v2: polish scope)

*Created 2026-11-08. Revised the same day: the owner wants polish, not a redesign. Sources: six research and audit reports in `research/01–06`. Baseline: `main` at `8d33466` plus the staged island-polish work (the task files were verified against this).*

**Goal:** make the island feel as alive and refined as Apple's Dynamic Island, **while keeping Droidbubble's current design**. That means the squircle corners, black cards, current card layouts and current palette all stay.

What we polish:
- layout and alignment
- motion
- gestures
- **dynamic (animated) icons**
- the small Dynamic-Island-like behaviours that make it feel premium

We also fix what's broken.

The ready-to-run tasks live in [`tasks/`](tasks/README.md). To use this plan in a new chat, see [`NEW_CHAT_PROMPT.md`](NEW_CHAT_PROMPT.md).

---

## 0. Guiding principles

1. **Polish, don't redesign.** Keep:
   - `SquircleShape` at smoothing 0.6
   - the card designs and the layout structure of each card
   - the colors (we only merge duplicates)
   - the settings app's structure

   We improve *how* things sit, move, respond and animate, not what they are.
2. **Apple is the quality reference, not a clone target.** For each item, take the principle behind it and implement it the Android way. Examples of principles:
   - interruptible springs that keep their velocity
   - the shape leads and the content follows
   - concentric corners
   - digits that never jitter
   - icons that react to events
   - no hard cuts

   Android-native implementations include `AnimatedVectorDrawable`/path morphs, Android haptic primitives, predictive back and TalkBack.
3. **Changing icons and colors isn't the improvement.** The improvement is in motion, gestures, layout precision and liveliness.
4. **Ship small.** One task is one focused change. Every step stays shippable.

> **Optional, not planned unless the owner asks:** a new palette, bundling a new font, redesigning the media card or system HUDs, a settings-app restructure, renaming "Liquid glass". The research covers all of these (01 §5, 02 §5, 05 §6) in case they're wanted later.

---

## 1. What the research found (the parts in scope)

### Broken or risky now → Phase 1
- **Security:** an *exported* debug receiver ships in release builds. Any app can use it to inject fake cards, wipe the island's state, or force a fullscreen touch-blocking island. (06 P1-4)
- **Touch blocking:** a touchable area of about 260×52 dp sits over the status bar whenever an activity is showing. It stays touchable even when the card is hidden for the foreground app. This kills status-bar taps and pulling the shade down from the centre. `IslandTouchManager` is never instantiated. (06 P0-4; 05 P0-1)
- **Live activities are invisible on a fresh install.** Calls, timers, alarms, recorders and rides are gated on "Incoming Notifications", which defaults to off. (04 P0-1)
- **Single notification slot:** a chat message pushes an ongoing call or timer aside. (04 P0-2)
- **English-only heuristics** for call, timer and recorder controls and for screen-recording detection. (04 P0-3)
- **High contrast barely works:** the outline is 15% instead of 55%, and 9 hex text colors bypass the tokens. (02 R2)
- **Accidental actions:**
  - invented "OK / Sure / Thanks!" reply chips
  - a ringing call can be swiped away
  - swipe-right hides the island with no way back
  - long-press opens the app's home screen instead of the conversation (05 §2)
- **Fabricated weather and a hard-coded "5G" label.** (04 P1-11)

### Why motion and gestures can't feel like a Dynamic Island yet → Phases 2–3
- **The whole `IslandOverlayRoot` (about 1,950 lines) recomposes on every animation frame.** Each new animation makes it worse. (06 P0-1; 03 P0-2)
- **Expand and collapse use two bodies.** A card that starts at 0 width grows while the pill shrinks. The silhouette narrows before it grows, and collapse looks like "vanish and re-pop". (03 P0-1)
- **The window resize races the animation**, so the first frames get clipped. Hide uses a fixed 360 ms delay, but the spring needs about 580 ms. (03 P1-1)
- **Gestures don't follow the finger and drop their velocity.** Slow swipe-ups never collapse. A dismissed card springs back before it vanishes. (03 P1-2; 05)
- **Content hard-cuts** inside cards and the pill when an activity changes. (03 P1-4)
- **The curve editor doesn't control what it says it does.** The preview is 1.43× too fast. (03 P1-3)

### Layout precision → Phase 4
- **The camera is never measured.** Placement relies on hand-tuned sliders plus a fixed `Spacer(36.dp)` repeated 5 times. (02 R1)
- **Two margin systems, and inner radii aren't concentric.** Examples: album art 12 dp where about 21 dp would be concentric; 40 dp tiles at 10 dp where about 17 dp would be. (02 §2)
- **Every island `Text` inherits a 24 sp line height.** That makes the spacing tokens meaningless and may clip hero digits. There are 33 hard-coded sp values and four title scales, and `expandedScale` is applied unevenly. (01 findings 1–6)
- **The compact pill has a fixed width.** Its sides are unbalanced (the flashlight trailing slot is empty), the bubble is circle-only, and an 8 sp numeral sits in the bubble ring. (02 gap 8; 01 finding 5)
- **Cards are 30–40% taller than their content needs.** (02 gap 7)
- **Color drift:** three different "recording" reds and two greens. The album accent is a muddy RGB average. Scrims are too light behind text. (02 §2)
- **Icons mix Filled and Rounded at 6 sizes.** (02 R6)

### Liveliness and dynamic icons → Phases 5–6
- The only "symbol effect" vocabulary today is the bell shake and the checkmark. Icons don't react to events: no bounce on a new message, no pulse for recording, no animated play/pause morph, no variable-color fill for signal or volume. (03 P2)
- Rolling digits don't cover %, W, ° or network speed. The bubble's progress ring jumps. Album art isn't shared between compact and expanded. (03 P2)

### Already good: keep and build on
- `SquircleShape` with its capsule fade
- **`RollingNumberText`** (rolls in the draw phase)
- `LiveLayout`'s concentric rules
- fixed-width media time labels
- the cached `steppedBlurEffect`
- the expand default spring 0.8/380, which equals Material 3 Expressive's default spatial spring
- the liquid-neck split concept
- the charging fill
- the Robolectric render harness

### Facts that correct assumptions
- `minSdk = 29`, but the README says Android 13+. AGSL (for the liquid shader) needs API 33. Strong skipping is on. There's no baseline profile and no macrobenchmark. Release builds are signed with the debug key.

---

## 2. What "Dynamic-Island-quality" means for Droidbubble

These are the principles we take from Apple, then make our own:

| Quality | Principle (from the reference) | Droidbubble implementation |
|---|---|---|
| **One living shape** | The island is one body that grows, shrinks, buds and re-absorbs. | A single `Animatable<IslandGeometry>`. The squircle card *drips* out of the pill below the status bar and is re-absorbed; it is never two shapes. The liquid neck becomes a smooth SDF shader. |
| **Physics, not timelines** | Springs defined by duration and bounce; velocity carries through interruptions; gestures hand off their velocity. | `IslandMotion` tokens with the character "eager in, calm out": a small bounce on expand, critically damped collapse. The user's curve settings plug into the same tokens. |
| **Shape leads, content follows** | Content fades, scales and blurs in after the shape is mostly grown, and leaves early. Elements keep the same relative placement. | Content progress is a pure function of geometry, with the same curve in both directions. Album art is shared from compact to expanded. No hard cuts anywhere. |
| **Gestures feel physical** | Content follows the finger, flings carry momentum, rubber-banding at the limits. | One gesture detector. Swipe down expands, and pulling further opens the shade. Swipe up tucks. Sideways dismisses with a fling. Inward minimises. Back and predictive back collapse. Haptics fire at the thresholds. |
| **Icons are alive** | Symbol effects: bounce, pulse, variable color, replace, wiggle, breathe, rotate, appear and disappear. | A `DynamicIcon` effect engine plus path and AVD morphs (play↔pause, mute, bell↔silent, flashlight). Each effect is wired to an island event (§4, epic I). |
| **Numbers are alive** | Numeric content transitions; no width jitter. | `RollingNumberText` everywhere, plus `NumericSlot` fixed widths and `tnum`. |
| **Glanceable alerts** | A brief expand or "breath" on important updates, then back to rest. | An alert bump (width impulse), an icon bounce and a tier-aware haptic. Banners recede instead of covering live activities. |
| **Precise fit** | Hugs the camera; concentric corners; even margins; no wasted height. | Cutout calibration, one margin system, concentric inner squircles, content-hugging compact width, measured card heights. |

---

## 3. Decisions for the owner

| # | Decision | Recommendation |
|---|---|---|
| D1 | Raise `minSdk` from 29 to 33? | **Yes.** It matches the README and unlocks AGSL and RenderEffect without fallbacks. If not, the liquid shader (D1 epic) needs an API-33 guard plus a fallback to today's neck. |
| D2 | Bundle a new font? | **Not now.** Keep the system font and fix its metrics (line heights, `tnum`, one consistent scale). Revisit later; see 01 R3. |
| D3 | Change the palette? | **No.** Keep the current colors and only merge duplicates (one red, one green, one gray family). |
| D5 | Live activities on by default, separate from message banners? | **Yes** |
| D6 | Replace swipe-right-to-hide with swipe-inward-to-minimise? | **Yes.** Full hide moves to a custom action, the QS tile, or a notification action. |
| D7 | Window strategy | **Keep MATCH_PARENT on expand** with the F5 fixes. Trial an envelope window with a touchable region behind a flag. |

---

## 4. Phases and epics

Epic IDs are stable. Tasks in `tasks/*.md` use `<EPIC>.<n>`.

> **Before Phase 0:** commit the staged island-polish work on `main`, so each task below is a clean diff.

### Phase 0: Safety net (`S`). No user-visible change.
- **S1** Characterization tests across a state × settings matrix. Cover the settled pill, bubble and card bounds, and the content in each state.
- **S2** Screenshot baseline (Roborazzi) of about 15 key states. This is the diff gate for every polish task.
- **S3** Pull the window-geometry math out into a pure function and table-test it.
- **S4** Performance baselines:
  - a recomposition counter, with a test on expand
  - a macrobenchmark `FrameTimingMetric` for expand and collapse
  - a Perfetto recipe
- **S5** Notification fixture corpus: CallStyle, ProgressStyle, chronometer, Maps, clock and recorder apps, in multiple locales.

### Phase 1: Fix what's broken (`B`). Small PRs.
- **B1** Security: the debug receiver becomes debug-only and `RECEIVER_NOT_EXPORTED`.
- **B2** Touch pass-through: the hit area hugs the pill and bubble, and nothing is touchable when the card is hidden.
- **B3** Live activities visible by default (D5).
- **B4** Heuristic hardening:
  - the score regex
  - TRACKING checked before SPORTS
  - package hints apply to ongoing notifications only
  - a word-boundary match for "end"
  - fewer screen-recording false positives
  - dismissed cards return only on a real state change
- **B5** High contrast: outline and text tokens, reacting live, placeholder contrast of at least 4.5:1.
- **B6** Safe actions:
  - no invented reply chips
  - ringing calls and alarms tuck back instead of being dismissed
  - correct open targets
  - swipe-right no longer hides (D6)
- **B7** Truthful widgets, and removal of FPS dead code.
- **B8** Battery and main-thread quick wins:
  - throttle the equalizer
  - draw-phase recording dot
  - media slider without a ticker
  - lazy glass panes
  - no `runBlocking`
  - touch no longer recomposes the root
  - debounced settings writes
- **B9** Typography quick fixes: the `tnum` escapes on network values, and a bubble numeral of at least 11 sp.
- **B10** Parsing off the main thread: sampled art decode, debounced `refreshMedia`.
- **B11** Crash containment: guards, breadcrumbs, a crash-loop safe mode.

### Phase 2: Foundations (`F`). Must land before Phases 3–6. The look stays pixel-identical.
- **F1** Scene model: a pure `deriveIslandScene(...)` consumed by both the composable and the window. It becomes the single arbitration source.
- **F2** Token consolidation: move the about 250 dp, 79 hex, 33 sp and about 40 inline spring literals into `IslandTokens`. Merge duplicate colors (D3). Add a CI grep that bans new literals. **No visual change, and no theming system overhaul.**
- **F3** Behaviour tokens:
  - `IslandMotion`: duration and bounce → `spring(1−b, (2π/d)²·speed²)`, with tiers, using the user's curves.
  - `IslandHaptics`: an SDK-guarded map that respects the toggle, with no raw waveforms.
  - `IslandTimings`: scaled by `getRecommendedTimeoutMillis`.
- **F4** Type roles on the system font: an `IslandType` with named roles, explicit line heights and `LineHeightStyle.Trim`. This replaces the 24 sp `LocalTextStyle` inheritance and merges the four scale systems. Sizes stay close to today's; we harmonize them rather than restyle. Plus `NumericSlot`.
- **F5** Window controller: grow before animating, shrink on settle (including hide), and fold the refresh boost and resize into one `updateViewLayout`. The envelope window goes behind a flag (D7).
- **F6** Build and release: the minSdk decision (D1), baseline profile, release signing, Compose compiler reports.

### Phase 3: One living shape (`M`). This is the core motion work and needs F1, F3 and F5.
- **M1** One body: a single `Animatable<IslandGeometry>`. Expand starts from the pill's current shape, the container alpha stays at 1, velocity is preserved, and everything is still drawn with `SquircleShape`.
- **M2** Deferred reads: layout, graphicsLayer and draw-phase reads, with stretch driven by the velocity vector. Target about 0 recompositions per frame during a morph.
- **M3** Choreography: content follows progress, with the same curve both ways. `AnimatedContent` is keyed per activity in both the cards and the pill, so there are no hard cuts.
- **M4** Gestures: `Modifier.islandGestures`.
  - Velocity, axis lock, thresholds in dp.
  - Fling-out or spring-back, and rubber-banding.
  - Swipe down expands, and further opens the shade. Swipe up tucks. Inward minimises.
  - RTL-aware.
  - Threshold haptics.
  - Back and predictive back collapse.
- **M5** Motion tiers: Full / Efficient / Reduced, from the in-app setting, the animator scale and Battery Saver.

### Phase 4: Layout polish (`V`). Needs F2 and F4, and can run alongside Phase 3. **The design is kept; this phase is about precision.**
- **V1** Camera calibration from `DisplayCutout`:
  - the sliders become fine-tune deltas
  - `cameraZone` replaces `Spacer(36.dp)`
  - landscape anchors to the camera
  - phones without a cutout fall back to today's defaults
- **V2** Concentric geometry:
  - one margin system
  - an `inner(inset)` radius helper and `Modifier.concentricClip`
  - inner art, tiles and buttons use concentric **squircle** radii
  - a `SquircleShape` fast path for capsules
- **V3** Compact balance:
  - content-hugging width
  - matched type on the leading and trailing sides
  - fit order: squeeze, then abbreviate, then drop; never an ellipsis
  - an oval bubble for short text
  - a trailing value for the flashlight
- **V4** Expanded spacing and heights:
  - measured heights instead of fixed ones
  - consistent header, padding and button rhythm across cards
  - **current card designs kept**, with empty space removed
- **V5** Color consolidation and legibility:
  - one semantic color per meaning (same hues)
  - album accent from Palette with L* and chroma clamps
  - scrims that keep text at 4.5:1 or better on bright art or wallpaper
- **V6** Edge polish: an adaptive keyline (1 dp, tinted with the accent) and consistent high-contrast outlines.
- **V7** Icon consistency: one family (Rounded), size tokens and optical centering. This is the base for epic I.

### Phase 5: Dynamic icons (`I`). Needs F3. Can start right after Phase 2, alongside Phase 3.
- **I1** `DynamicIcon` effect engine. Effects:
  - bounce (scale and offset impulse)
  - pulse (opacity)
  - breathe (scale loop)
  - wiggle (rotation impulse)
  - rotate (continuous, for sync)
  - variable color (layered fill for signal, volume, battery and Wi-Fi)
  - appear and disappear (scale and blur)
  - replace (crossfade with scale down and up)

  Every effect is driven by `IslandMotion` tokens, respects motion tiers, and does its reads in the draw phase.
- **I2** Morphing state icons: play↔pause, mute↔unmute, bell↔bell-slash, flashlight on↔off, checkmark draw-on, mic and waveform. These use `AnimatedVectorDrawable` or `graphics-shapes` `Morph`.
- **I3** Wiring icons to events:

  | Event | Icon effect |
  |---|---|
  | New message from the same app | bounce |
  | Recording in progress | pulse |
  | Charging | bolt bounce plus fill |
  | Download | progress icon fills (variable color) |
  | Incoming call | wiggle |
  | Timer done | shake |
  | Bluetooth connects | appear |
  | Ringer mode changes | replace |
  | Network speed | variable-color arrows |
  | Media track changes | art flip plus a play-state morph |

- **I4** An icon-effect catalogue screen in the existing Test Live Activities panel, to preview and tune each effect.

### Phase 6: Signature motion and delight (`D`). Needs M1–M3.
- **D1** Liquid split, merge and drip. An AGSL SDF `smin` shader for the pill, bubble and neck. Content is drawn above it unfiltered, and the shader only runs during transitions. It's guarded for API 33 (D1).
- **D2** Alert behaviours:
  - an alert bump (width impulse) instead of `swapSquash`
  - banners briefly expand, then recede
  - `symbolBounce` on "same app, new event"
  - haptics at physical moments (the neck snapping, the merge)
- **D3** Numbers everywhere: rolling digits for %, W, °, speed and ETA; a spring on the progress ring; `NumericSlot` on all live values.
- **D4** Shared artwork and consistent placement between compact and expanded, for media, call avatars and app icons.
- **D5** Curve editor v2:
  - Duration and Bounce sliders
  - an honest preview that includes speed²
  - transitions renamed to what they actually drive
  - Calm / Droid / Lively presets
  - migration of stored values
- **D6** *(Optional)* An orbit ring: a thin progress ring around the round punch-hole in the minimal state. Needs V1.

### Phase 7: Live activities and functions (`A`). Needs F1. Behaviour, not visuals.
- **A1** `LiveActivitySpec` with slot-based presentations and an `ActivityStore`, replacing the single slot. Migrate one kind at a time.
- **A2** Tiered arbiter T0–T5:
  - primary, secondary and a "+N" overflow
  - banners recede and never cover T1
- **A3** Structured adapters:
  - CallStyle: mute and speaker, video, screening
  - Live Updates / ProgressStyle: segments, points, `shortCriticalText`
  - Navigation: maneuver icon and distance
  - chronometer
- **A4** Locale-independent control mapping, tested against the S5 fixtures.
- **A5** Lock-screen privacy: honor `visibility` and `publicVersion`, and hide actions while locked.
- **A6** Fullscreen policy: Hide / Calls & alarms only (the default) / Always.
- **A7** New system sources:
  - battery low, full and time to full
  - Bluetooth name, type and battery
  - VPN, Do Not Disturb and Modes
  - the mic and camera privacy pill
  - the screen-recording callback
- **A8** Android-only extras: "Pin as activity" and a media session switcher.
- **A9** Housekeeping: stop the waveform's own `AudioRecord`, and remove the dead receiver.

### Phase 8: Accessibility and small companion fixes (`C`). Not a redesign of the settings app.
- **C1** Permission and onboarding fixes, within the current layout:
  - show the permission descriptions
  - mark Required vs Optional
  - an accessibility disclosure
  - help for Android 13+ restricted settings
  - ask for Bluetooth only when it's needed
- **C2** A live preview while tuning Dimensions and Appearance, using a sample activity from `LiveActivityTester`, plus reset buttons. This pairs with V1.
- **C3** Island TalkBack:
  - labels and click labels
  - custom actions (minimise, hide, dismiss)
  - live regions
  - auto-collapse pauses while accessibility focus is on the island
  - TTS skipped while TalkBack is on
  - expanded font scale up to 2.0
- **C4** A Quick Settings tile and a "Pause island 1 h" action, plus Do Not Disturb awareness.
- **C5** Small settings fixes:
  - make the per-app filter reachable
  - a quieter update banner
  - `tnum` on value chips
  - remove "Coming soon"
  - accessibility actions for the reorder list

---

## 5. Recommended order and parallel tracks

```
S1–S5 ──> B1…B11 (start right away)
F1, F2, F3, F4, F5 (foundations, look unchanged)
   ├─> Motion track:   M1 → M2 → M3 → M4 → M5 → D1, D2, D4, D5
   ├─> Icons track:    V7 → I1 → I2 → I3 → I4      (visible wins early)
   ├─> Layout track:   V1 → V2 → V3 → V4 → V5 → V6 → D3, D6
   ├─> Functions track: A1 → A2 → A3 … (after F1)
   └─> A11y track:     C1, C2 (any time after F2); C3 after M4; C4, C5 any time
```

- **The fastest visible payoff:** B2 (touch), M1 (one-body morph), M4 (gestures) and I1 to I3 (dynamic icons).
- **`IslandView.kt` is where the tracks collide.** Split presentations out of it file by file (06 Phase 5) before running tracks in parallel, and never run two writing sessions on the same file.

## 6. Definition of done for every task

- Robolectric suite green. S2 screenshot diffs reviewed: unchanged for refactors, intended for polish tasks.
- No new literal colors, sp values or springs in presentation code (the F2 check).
- Recomposition and frame timing no worse than the S4 baseline. After M2, morphs run at about 0 recompositions per frame.
- The Reduced-motion path is handled, and TalkBack works for new interactive elements.
- **Design check:** the current look is kept (squircles, cards, palette), and nothing introduces a near-literal clone of an Apple asset, layout or name.
- Device-verified for any change touching the window, gestures, haptics or OEM behaviour.

## 7. Research index

| File | Dimension |
|---|---|
| [`research/01-typography.md`](research/01-typography.md) | Type metrics, numerals, fonts (font change is optional) |
| [`research/02-visual-design.md`](research/02-visual-design.md) | Geometry, cutout, concentricity, color, icons |
| [`research/03-motion.md`](research/03-motion.md) | Springs, one-body morph, choreography, gestures, curve editor, motion tokens and timelines |
| [`research/04-functions.md`](research/04-functions.md) | Activity catalogue, Android hooks, arbitration |
| [`research/05-ux-interaction.md`](research/05-ux-interaction.md) | Gestures, haptics, timing, context, accessibility, companion app |
| [`research/06-performance-architecture.md`](research/06-performance-architecture.md) | Window, recomposition, battery, architecture, security |
