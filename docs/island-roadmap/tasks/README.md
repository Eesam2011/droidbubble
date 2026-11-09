# Task index

This index lists every task in the roadmap, in order. Each task's full entry (goal, files, steps, acceptance criteria, tests, dependencies) is in its phase file. Read [`../PLAN.md`](../PLAN.md) first; §0 and §6 apply to every task.

**Baseline:** branch `main` at `8d33466`, plus the staged island-polish work. The task files' `file:line` anchors were checked against that tree. **Commit the staged work before starting.** Anchors can drift, so re-check them before editing.

**Status:** `todo` → `doing` → `done`. Update the Status column here as you go.

| Phase | File | Tasks |
|---|---|---|
| 0–1 Safety net and fixes | [phase-0-1-safety-and-fixes.md](phase-0-1-safety-and-fixes.md) | 51 |
| 2–3 Foundations and one living shape | [phase-2-3-foundations-and-shape.md](phase-2-3-foundations-and-shape.md) | 52 |
| 4 + 6 Layout polish and signature motion | [phase-4-6-layout-and-motion-polish.md](phase-4-6-layout-and-motion-polish.md) | 41 |
| 5 Dynamic icons | [phase-5-dynamic-icons.md](phase-5-dynamic-icons.md) | 14 |
| 7 Live activities | [phase-7-live-activities.md](phase-7-live-activities.md) | 44 |
| 8 Accessibility and companion fixes | [phase-8-accessibility-and-companion.md](phase-8-accessibility-and-companion.md) | 30 |

---

## Owner decisions to settle first

| # | Decision | Recommendation | Blocks |
|---|---|---|---|
| Decision D1 | Raise `minSdk` from 29 to 33 | Yes | F6.1, D1.x (liquid shader; otherwise needs an API 33 guard plus the current neck as fallback) |
| Decision D5 | Live activities on by default | Yes | B3.1 |
| Decision D6 | Swipe right no longer hides | Yes | B6.4 |
| Decision D7 | Keep MATCH_PARENT on expand; trial an envelope window behind a flag | Yes | F5.4, B2.3 |
| — | Keep `adb shell am broadcast` debug control? If yes, keep it in debug builds only, guarded by `DUMP` | Your call | B1.1 |
| — | Add a small bolt icon to the charging pill | Design check | I3.3 |
| — | Margins: tall-card inset R/2 and tile radius cap (to keep today's look) | Review at CP1 | V2.2, V2.3 |
| — | Pill height: max(36, camera+12) or max(36, camera+16) | Review at CP1 | V1.3 |

"Decision D1" (minSdk) is a different thing from epic **D1** (the liquid shader). They only share the name.

---

## Recommended global order

1. **Checkpoint:** commit the staged work.
2. **Phase 0:** S1 → S2 → S3 → S4 → S5. These are the regression net; don't skip them.
3. **Phase 1:** B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, following the execution order at the end of the phase-0-1 file. Use one-at-a-time lanes for `IslandView.kt`, `NotificationReceiverService.kt`, `IslandOverlayService.kt` and `MainScreen.kt`.
4. **Phase 2:** F1 → F2 → F3 → F4 → F5 → F6. Each first ships an exact copy of today's behaviour, then an intended change.
5. **Parallel tracks** once Phase 2 is merged. Use one worktree per track, and never have two writers on `IslandView.kt` at once.

   | Track | Order |
   |---|---|
   | Motion | M1.1 → M2.1–M2.4 → M1.3 → M1.4 → M2.5–M2.7 → M3 → M4 → M5 → D1, D2, D4, D5 |
   | Icons | I0.1 → I1 → I2 → I3 → I4. Can start before Phase 3, but tasks touching `IslandView.kt` go before or after M3. |
   | Layout | V7 → V1 → V2 → V3 → V4 → V5 → V6 → D3, D6. Owner checkpoints CP1–CP6. |
   | Functions | A1 → A2 → A3 → A4 → A5 → A6 → A7 → A8 → A9 (see that file's execution order) |
   | A11y and companion | C1, C2, C4, C5 any time after F2; C3 after M4 |

The fastest visible wins are B2 (touch pass-through), M1.3 and M1.4 (one-body morph), M4 (gestures), and I1–I3 (dynamic icons).

---

## All tasks

### Phase 0–1: Safety net and fixes ([file](phase-0-1-safety-and-fixes.md))
| ID | Title | Status |
|---|---|---|
| S1.1 | Shared island test harness | done |
| S1.2 | Content probes (testTags per slot) | done |
| S1.3 | Settled bounds and content matrix | done |
| S2.1 | Roborazzi setup and `IslandClock` seam | done |
| S2.2 | Screenshot baseline (16 states) | done |
| S3.1 | Extract window math verbatim | done |
| S3.2 | Window table test with known-bug rows | done |
| S4.1 | Recomposition counter and budget test | done |
| S4.2 | Macrobenchmark for expand and collapse | done |
| S4.3 | Trace sections and Perfetto recipe | done |
| S5.1 | Fixture JSON format and SBN builder | done |
| S5.2 | Fixture corpus | done |
| S5.3 | Fixture characterization through the real NRS | done |
| B1.1 | Debug receiver only in debug builds | done |
| B1.2 | System receiver NOT_EXPORTED and exported-surface test | done |
| B2.1 | Nothing touchable when the card is hidden for the foreground app | doing (code and tests done; device check pending) |
| B2.2 | Trim the compact window envelope | todo |
| B2.3 | Touchable region via `IslandTouchManager` (probe, flag, fallback) | todo |
| B3.1 | Separate `showLiveActivities` gate, on by default | doing (code and tests done; device check pending) |
| B3.2 | "Live Activities" toggle in settings | doing (code and test done; manual check pending) |
| B4.1 | Strict score regex; TRACKING before SPORTS | done |
| B4.2 | Package hints only for ongoing notifications | done |
| B4.3 | Whole-word "end" matching | done |
| B4.4 | Fewer screen-recording false positives | done |
| B4.5 | Dismissed cards return only on a real state change | done |
| B5.1 | High-contrast outline on all four edges | todo |
| B5.2 | Text tokens; placeholder at 4.9:1 | todo |
| B5.3 | React live to system high contrast | todo |
| B6.1 | No invented reply chips | todo |
| B6.2 | Ringing calls and alarms tuck instead of dismiss | done |
| B6.3 | Open targets: contentIntent first | doing (code and tests done; device check pending) |
| B6.4 | Swipe right no longer hides | done |
| B7.1 | Weather: no fabricated data | todo |
| B7.2 | Network label: no hard-coded "5G" | todo |
| B7.3 | Remove FPS remnants | done |
| B8.1 | Equalizer throttle and draw-phase recording dot | doing (code and tests done; device refresh-rate check pending) |
| B8.2 | Media slider without the 250 ms ticker | doing (code and tests done; device seek check pending) |
| B8.3 | Lazy, non-blocking glass panes | todo |
| B8.4 | No `runBlocking` in onCreate | todo |
| B8.5 | Touches don't recompose the root | doing (code and tests done; device trace pending) |
| B8.6 | One settings mapper and distinctUntilChanged | todo |
| B8.7 | Debounced slider writes | todo |
| B9.1 | Restore tnum on network values | done |
| B9.2 | Bubble numeral at least 11 sp, or the ring alone | todo |
| B10.1 | Sampled off-main art decode | todo |
| B10.2 | Debounced refreshMedia without activeNotifications | todo |
| B10.3 | Parser dispatcher and per-key sequence numbers | todo |
| B10.4 | Cache clock-app custom-view reads | todo |
| B11.1 | Breadcrumb ring in CrashLog | done |
| B11.2 | Per-notification and media guards | done |
| B11.3 | Crash-loop guard and safe mode | todo |

### Phase 2–3: Foundations and one living shape ([file](phase-2-3-foundations-and-shape.md))
| ID | Title | Status |
|---|---|---|
| F1.1 | Lift arbitration into a pure `deriveIslandScene` | todo |
| F1.2 | Composable consumes the scene | todo |
| F1.3 | Target geometry as a pure function (`IslandGeometry`) | todo |
| F1.4 | Window consumes the scene | todo |
| F1.5 | One `StateFlow<IslandScene>` per process | todo |
| F2.1 | Literal ratchet test | todo |
| F2.2 | Color literals into IslandTokens (exact) | todo |
| F2.3 | Merge near-duplicate colors | todo |
| F2.4 | Shell geometry tokens | todo |
| F2.5 | Presentation dp hygiene, file by file | todo |
| F3.1 | IslandMotion: formula, tokens, tiers, impulse and loop | todo |
| F3.2 | Route every inline spec through IslandMotion | todo |
| F3.3 | IslandHaptics (1:1, toggle respected) | todo |
| F3.4 | Retune haptic meanings | todo |
| F3.5 | IslandTimings with recommended timeout | todo |
| F3.6 | Retune timings | todo |
| F4.1 | IslandType roles reproducing today exactly | todo |
| F4.2 | Explicit line heights and Trim | todo |
| F4.3 | Harmonize the scales | todo |
| F4.4 | NumericSlot | todo |
| F5.1 | WindowSpec and OverlayWindowController | todo |
| F5.2 | Grow the window before animating | todo |
| F5.3 | Shrink on settle everywhere, including hide | todo |
| F5.4 | Envelope window trial behind a flag | todo |
| F6.1 | Apply Decision D1 (minSdk or guards) | todo |
| F6.2 | Compose compiler reports and stability config | todo |
| F6.3 | Baseline profile | todo |
| F6.4 | Release signing | todo |
| M1.1 | IslandShellState (Animatable geometry + x) | todo |
| M1.2 | Continuity test harness | todo |
| M1.3 | Expand starts from the pill | todo |
| M1.4 | One surface for pill and card | todo |
| M1.5 | Notch style on the one body | todo |
| M2.1 | Extract the shell to `island/shell/` | todo |
| M2.2 | Size and position read in the layout phase | todo |
| M2.3 | Cached squircle surface | todo |
| M2.4 | Stretch and blur from spring velocity | todo |
| M2.5 | Glass publishing without SideEffect | todo |
| M2.6 | Pill content laid out at final width | todo |
| M2.7 | Narrow state reads and recomposition gate | todo |
| M3.1 | Content progress as a pure function of geometry | todo |
| M3.2 | Staggers in progress, not wall-clock time | todo |
| M3.3 | Expanded content keyed per activity | todo |
| M3.4 | Compact content keyed per activity | todo |
| M4.1 | `Modifier.islandGestures` and release resolver | todo |
| M4.2 | Card sideways fling-out or spring-back | todo |
| M4.3 | Card swipe-up tucks, with velocity hand-off | todo |
| M4.4 | Pill: down expands, further opens the shade, inward minimizes | todo |
| M4.5 | Back and predictive back collapse | todo |
| M5.1 | Resolve the motion tier | todo |
| M5.2 | Apply Reduced and Efficient everywhere | todo |
| M5.3 | "Eager in, calm out" retune | todo |

### Phase 4 + 6: Layout polish and signature motion ([file](phase-4-6-layout-and-motion-polish.md))
| ID | Title | Status |
|---|---|---|
| VD0.1 | Owner checkpoint screenshot set | todo |
| V1.1 | CutoutCalibration reader and CameraGeometry | todo |
| V1.2 | Settings become fine-tune deltas, with migration | todo |
| V1.3 | Apply calibration (cameraZone, window) | todo |
| V1.4 | "Fit to camera" switch and labels | todo |
| V1.5 | Landscape anchors to the camera | todo |
| V2.1 | CardGeometry, `inner()`, per-corner squircle | todo |
| V2.2 | One margin system | todo |
| V2.3 | Concentric inner radii in squircles | todo |
| V3.1 | Content-hugging compact width | todo |
| V3.2 | Matched type; squeeze, abbreviate, drop | todo |
| V3.3 | Oval bubble and readable ring | todo |
| V3.4 | Flashlight trailing value | todo |
| V3.5 | System alert pills hug their content | todo |
| V4.1 | Measured card heights | todo |
| V4.2 | One header, padding and button rhythm | todo |
| V5.1 | Semantic color roles | todo |
| V5.2 | Album accent via Palette with clamps | todo |
| V5.3 | Legibility scrims | todo |
| V6.1 | Adaptive keyline | todo |
| V6.2 | One outline rule, including notch | todo |
| V7.1 | IslandIcons catalogue, Rounded only | todo |
| V7.2 | Icon size tokens and optical centering | todo |
| V7.3 | (optional) Material Symbols drawables | todo |
| D1.1 | Liquid SDF math and shader asset | todo |
| D1.2 | Split and merge with the shader | todo |
| D1.3 | (optional) Drip on expand | todo |
| D2.1 | Alert bump replaces swapSquash | todo |
| D2.2 | Banners peek, then recede | todo |
| D2.3 | "Same app, new event" signal | todo |
| D2.4 | Haptics at the neck snap and merge | todo |
| D3.1 | Rolling numbers in compact and bubble | todo |
| D3.2 | Rolling numbers in expanded cards | todo |
| D3.3 | Spring on progress rings | todo |
| D4.1 | Shared art, avatar and icon between compact and expanded | todo |
| D4.2 | Consistent placement audit | todo |
| D5.1 | Duration and bounce model, with migration | todo |
| D5.2 | Curve editor v2 with honest preview | todo |
| D5.3 | Calm, Droid and Lively presets | todo |
| D6.1 | (optional) Orbit ring | todo |

### Phase 5: Dynamic icons ([file](phase-5-dynamic-icons.md))
| ID | Title | Status |
|---|---|---|
| I0.1 | Symbol-effect tokens and motion-tier shim | todo |
| I1.1 | Engine core: bounce and wiggle | todo |
| I1.2 | Pulse, breathe, rotate; draw-phase pulses | todo |
| I1.3 | Appear, disappear and replace | todo |
| I1.4 | Variable color (LayeredIcon) | todo |
| I2.1 | Morph toolkit and play↔pause | todo |
| I2.2 | Checkmark draw-on and slash overlay | todo |
| I2.3 | Flashlight and mic morphs | todo |
| I3.1 | Icon event counters | todo |
| I3.2 | Wire bounce, wiggle, pulse, appear and replace | todo |
| I3.3 | Wire charging, download, network and flashlight | todo |
| I3.4 | Media: cover flip and play-state morph | todo |
| I4.1 | Effect catalogue in the Test panel | todo |
| I4.2 | Buttons that fire real events on the island | todo |

### Phase 7: Live activities ([file](phase-7-live-activities.md))
| ID | Title | Status |
|---|---|---|
| A1.1 | Classify each notification once | todo |
| A1.2 | LiveActivitySpec, Slot and legacy mapper | todo |
| A1.3 | ActivityStore in shadow mode | todo |
| A1.4 | Old slots become store projections; delete the stash | todo |
| A1.5 | Generic slot renderers | todo |
| A1.6 | Compact and bubble render from slots | todo |
| A1.7 | Media, flashlight and recording become store entries | todo |
| A1.8 | Expanded card, height, open target and outline from the spec | todo |
| A1.9 | Delete the legacy single-slot state | todo |
| A2.1 | Pure `arbitrate()` with the tier table | todo |
| A2.2 | The scene uses the arbiter | todo |
| A2.3 | Banners recede | todo |
| A2.4 | "+N" overflow | todo |
| A2.5 | (optional) "Why is this primary?" and overrides | todo |
| A2.6 | Test lab: stack demo activities | todo |
| A3.1 | NotificationSnapshot and adapter chain | todo |
| A3.2 | CallStyle adapter | todo |
| A3.3 | Live Updates: promotion, shortCriticalText, countdown | todo |
| A3.4 | ProgressStyle parser | todo |
| A3.5 | Journey bar | todo |
| A3.6 | Navigation adapter | todo |
| A3.7 | Chronometer and timer reader | todo |
| A4.1 | ControlRole resolver | todo |
| A4.2 | Per-locale tables and digit normalization | todo |
| A4.3 | Wire roles into every control consumer | todo |
| A4.4 | Structured signals for kind and state | todo |
| A5.1 | Lock context and visibility in the model | todo |
| A5.2 | Redaction while locked | todo |
| A6.1 | Fullscreen detection | todo |
| A6.2 | "In fullscreen apps" setting | todo |
| A7.1 | SystemActivitySource | todo |
| A7.2 | Battery: low, full, time to full | todo |
| A7.3 | Bluetooth glance with cooldown | todo |
| A7.4 | Battery for every Bluetooth device | todo |
| A7.5 | VPN (and hotspot, best effort) | todo |
| A7.6 | DND glance | todo |
| A7.7 | (optional) Next alarm glance | todo |
| A7.8 | Mic and camera privacy pill | todo |
| A7.9 | Screen-recording callback on API 35+ | todo |
| A8.1 | "Pin as activity" | todo |
| A8.2 | Track every media session | todo |
| A8.3 | Media session switcher | todo |
| A9.1 | Recorder waveform without AudioRecord | todo |
| A9.2 | Remove dead receivers and legacy heuristics | todo |

### Phase 8: Accessibility and companion fixes ([file](phase-8-accessibility-and-companion.md))
| ID | Title | Status |
|---|---|---|
| C1.1 | Permission catalogue: levels and honest copy | todo |
| C1.2 | Permission rows: descriptions, levels, "Not now" | todo |
| C1.3 | Accessibility-service disclosure | todo |
| C1.4 | Restricted-settings helper | todo |
| C1.5 | Bluetooth permission only when needed | todo |
| C1.6 | Light guided first run | todo |
| C2.1 | Preview sample activity | todo |
| C2.2 | Live preview on Dimensions and Appearance | todo |
| C2.3 | Reset buttons | todo |
| C2.4 | "Fit to camera" on Dimensions (with V1.4) | todo |
| C3.1 | A11y test harness and device spike | todo |
| C3.2 | IslandA11y environment | todo |
| C3.3 | IslandSpeech label builder | todo |
| C3.4 | Pill, bubble and ring semantics | todo |
| C3.5 | Custom actions | todo |
| C3.6 | Expanded card, backdrop and control labels | todo |
| C3.7 | Live-region announcements | todo |
| C3.8 | Auto-collapse pauses under accessibility focus | todo |
| C3.9 | Spoken Alerts defers to TalkBack | todo |
| C3.10 | Font scale: non-linear, compact cap, expanded up to 2.0 | todo |
| C4.1 | Pause model | todo |
| C4.2 | Pause surfaces | todo |
| C4.3 | Quick Settings tile | todo |
| C4.4 | DND awareness | todo |
| C5.1 | (optional) Split MainScreen.kt into files | todo |
| C5.2 | Per-app filter reachable | todo |
| C5.3 | Quieter update row | todo |
| C5.4 | Steady value chips (tnum) | todo |
| C5.5 | Remove "Coming soon" | done |
| C5.6 | Reorder list accessibility | todo |

---

## Known cross-file notes
- **Pre-Phase-0 cleanup (2026-11-08, owner-approved).** These are gone; skip any task text that still mentions them:
  - FPS everywhere (`IslandPriority.FPS`, `FpsInfo`, `showFps`, the settings row, telemetry). Stored `"FPS"`/`"TIMER"` priorities are dropped by `parseFeaturePriorities` (`model/IslandSettings.kt`). This was B7.3.
  - The settings `isTeardropCutout` (and the Tear-drop picker option), `mediaWaveAnimation`, `wavyProgressBar`, `spinningAlbumArt` and `cornerRadiusDp` (now the fixed `IslandTokens.CompactCornerRadius` = 22 dp).
  - The "Persistent Island Views · Coming soon" section (this was C5.5).
  - `ScreenRecordingService` and `FOREGROUND_SERVICE_MEDIA_PROJECTION`; the manifest `PowerConnectionReceiver` (the A9.2 receiver half); Shizuku (`ShizukuHelper`, both dependencies, the exported `ShizukuProvider`, `WRITE_SECURE_SETTINGS`).
  - Dead code: `morphBlurEffect`, `expandedCornerRadiusFor`, `CellularSignalBars`, `toggleExpand`, `activeExpandedPriority`, `IslandOverlayRoot`'s `onBoundsChange` parameter.
  - Kept on purpose: the unused tokens `SpaceLg`, `SpaceXl`, `RadiusCard`, `SurfaceSunken`, `AccentPurple`, `AccentYellow` (their values still appear as literals, so F2.2/F2.5 route those literals into them), `HighContrastOutline` (B5.1), `IslandTouchManager` (B2.3).
- **Owner decisions (2026-11-08):** D5 = yes, D6 = yes, adb debug control = **no** (B1.1 registers `RECEIVER_NOT_EXPORTED` only; the Test panel covers manual testing).
- The `MainScreen.kt` line ranges in the phase-8 file are stale; the curve editor is now around `:2649–2796`. Re-check them before C5.1.
- `IslandGeometry` means the animated shape (M1). The cutout data is `CameraGeometry` (V1), and margins are `CardGeometry` (V2).
- Overlaps:
  - F2.3 and V5.1 both merge duplicate colors. Whichever lands first does the merge; the other only names the roles.
  - D2.3 and I3: D2.3 detects the event, I3 owns the icon effect.
  - C2.4 and V1.4 are the same "Fit to camera" switch. Do them together.
