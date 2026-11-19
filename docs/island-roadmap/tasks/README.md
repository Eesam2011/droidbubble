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
| — | Margins: tall-card inset R/2 and tile radius cap (to keep today's look) | **Decided 2026-11-15 (CP1 questions): R/2 inset for every tall card; tiles and art keep the 0.42 cap (rounded squares)** | V2.2, V2.3 |
| — | CP3: keyline default, download header gap, screen gutter | **Decided 2026-11-17 (CP3 questions): keyline default stays OFF; download card header gap stays 13.2 dp; screen gutter stays 16 dp (cards 344 dp on a 360 dp phone)** | V6.1, V4.2 |
| — | Pill height: max(36, camera+12) or max(36, camera+16) | **Decided 2026-11-15: max(36, camera+12)** | V1.3 |

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
| B2.1 | Nothing touchable when the card is hidden for the foreground app | done (code and tests done; device check pending) |
| B2.2 | Trim the compact window envelope | done (settled sizes verified on device; 0.5x animation recording pending) |
| B2.3 | Touchable region via `IslandTouchManager` (probe, flag, fallback) | blocked (spike: touchableRegion denied on API 36; B2.2 is the fix) |
| B3.1 | Separate `showLiveActivities` gate, on by default | done (code and tests done; device check pending) |
| B3.2 | "Live Activities" toggle in settings | done (code and test done; manual check pending) |
| B4.1 | Strict score regex; TRACKING before SPORTS | done |
| B4.2 | Package hints only for ongoing notifications | done |
| B4.3 | Whole-word "end" matching | done |
| B4.4 | Fewer screen-recording false positives | done |
| B4.5 | Dismissed cards return only on a real state change | done |
| B5.1 | High-contrast outline on all four edges | done |
| B5.2 | Text tokens; placeholder at 4.9:1 | done |
| B5.3 | React live to system high contrast | done (code and test done; device check pending) |
| B6.1 | No invented reply chips | done |
| B6.2 | Ringing calls and alarms tuck instead of dismiss | done |
| B6.3 | Open targets: contentIntent first | done (code and tests done; device check pending) |
| B6.4 | Swipe right no longer hides | done |
| B7.1 | Weather: no fabricated data | dropped (weather widget removed) |
| B7.2 | Network label: no hard-coded "5G" | dropped (network speed widget removed) |
| B7.3 | Remove FPS remnants | done |
| B8.1 | Equalizer throttle and draw-phase recording dot | done (code and tests done; device refresh-rate check pending) |
| B8.2 | Media slider without the 250 ms ticker | done (code and tests done; device seek check pending) |
| B8.3 | Lazy, non-blocking glass panes | done (code and tests done; device z-order check pending) |
| B8.4 | No `runBlocking` in onCreate | done (code and tests done; device reboot check pending) |
| B8.5 | Touches don't recompose the root | done (code and tests done; device trace pending) |
| B8.6 | One settings mapper and distinctUntilChanged | done |
| B8.7 | Debounced slider writes | done |
| B9.1 | Restore tnum on network values | done |
| B9.2 | Bubble numeral at least 11 sp, or the ring alone | done |
| B10.1 | Sampled off-main art decode | done (code and tests done; device track-skip check pending) |
| B10.2 | Debounced refreshMedia without activeNotifications | done (code and tests done; device latency check pending) |
| B10.3 | Parser dispatcher and per-key sequence numbers | done (code and tests done; device trace pending) |
| B10.4 | Cache clock-app custom-view reads | done |
| B11.1 | Breadcrumb ring in CrashLog | done |
| B11.2 | Per-notification and media guards | done |
| B11.3 | Crash-loop guard and safe mode | done (code and tests done; device crash-loop check pending) |

### Phase 2–3: Foundations and one living shape ([file](phase-2-3-foundations-and-shape.md))
| ID | Title | Status |
|---|---|---|
| F1.1 | Lift arbitration into a pure `deriveIslandScene` | done |
| F1.2 | Composable consumes the scene | done |
| F1.3 | Target geometry as a pure function (`IslandGeometry`) | done |
| F1.4 | Window consumes the scene | done (code and tests pass; check later on the phone) |
| F1.5 | One `StateFlow<IslandScene>` per process | done (code and tests pass; check later on the phone: a burst of notifications resizes the window once, not per notification) |
| F2.1 | Literal ratchet test | done |
| F2.2 | Color literals into IslandTokens (exact) | done |
| F2.3 | Merge near-duplicate colors | done (owner kept the look; screenshots re-recorded, full suite passes) |
| F2.4 | Shell geometry tokens | done |
| F2.5 | Presentation dp hygiene, file by file | done (pixel-identical; full suite passes) |
| F3.1 | IslandMotion: formula, tokens, tiers, impulse and loop | done |
| F3.2 | Route every inline spec through IslandMotion | done |
| F3.3 | IslandHaptics (1:1, toggle respected) | done (code and tests pass; check later on the phone: each haptic fires once, and the haptics switch silences them) |
| F3.4 | Retune haptic meanings | done (Droid map is the default, Legacy kept; device feel check pending) |
| F3.5 | IslandTimings with recommended timeout | done (code and tests pass; check later on the phone: Android's "Time to take action" setting lengthens the timeouts) |
| F3.6 | Retune timings | done (open card 6 s, media 8 s, ringing call or alarm stays; tests pass; check later: how it feels on the phone) |
| F4.1 | IslandType roles reproducing today exactly | done |
| F4.2 | Explicit line heights and Trim | done (owner kept the look; screenshots and bounds golden re-recorded, full suite passes) |
| F4.3 | Harmonize the scales | done (owner kept the look; screenshots re-recorded, full suite passes) |
| F4.4 | NumericSlot | done |
| F5.1 | WindowSpec and OverlayWindowController | done (code and tests pass; check later: one window relayout at expand start in a Perfetto trace) |
| F5.2 | Grow the window before animating | done (code and tests pass; check later on the phone: no clipped first frames at 120 Hz) |
| F5.3 | Shrink on settle everywhere, including hide | done (code and tests pass; check later on the phone: hide and the last notification ending show the full shrink) |
| F5.4 | Envelope window trial behind a flag | done (behind a debug-only flag, default off; tests pass; check later: the device trial) |
| F6.1 | Apply Decision D1 (minSdk or guards) | done (D1 = no: API gates in `IslandApi`) |
| F6.2 | Compose compiler reports and stability config | done |
| F6.3 | Baseline profile | done (module and generator compile; check later: generate the profile with the phone connected, `bash tools/gradle-locked.sh :app:generateBaselineProfile`, see `baselineprofile/README.md`, then compare with `IslandFirstExpandBenchmark`) |
| F6.4 | Release signing | doing (wiring done and tested with a throwaway key; waiting on the owner to create the real keystore) |
| M1.1 | IslandShellState (Animatable geometry + x) | done |
| M1.2 | Continuity test harness | done |
| M1.3 | Expand starts from the pill | cancelled (was parked; owner: one-body morph felt less smooth on review; two bodies kept. Work saved in `parked/M1.3-one-body.patch`) |
| M1.4 | One surface for pill and card | cancelled (was parked; needs M1.3) |
| M1.5 | Notch style on the one body | cancelled (was parked; needs M1.4) |
| M2.1 | Extract the shell to `island/shell/` | done (one file, IslandShell.kt; finer splits land with M1.4 and M2.x) |
| M2.2 | Size and position read in the layout phase | done (recompositions within budget, not yet lower: the shell still reads the geometry in composition until M2.4–M2.7) |
| M2.3 | Cached squircle surface | done (exact-radius cache: 0.25 dp buckets would shift mid-animation pixels) |
| M2.4 | Stretch and blur from spring velocity | done (owner approved the mid-frame diff, 2026-11-12) |
| M2.5 | Glass publishing without SideEffect | done (device: OPPO CPH2631, Android 16, Liquid glass on; the blur follows the card on expand and collapse) |
| M2.6 | Pill content laid out at final width | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M2.7 | Narrow state reads and recomposition gate | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M3.1 | Content progress as a pure function of geometry | done (owner-approved mid-frames; device checked at 0.7×, 1×, 2× and 10×: content waits for the shape, leaves blurred) |
| M3.2 | Staggers in progress, not wall-clock time | done (device checked at 0.7×, 1×, 2× and 10×: card 2 after card 1, bubble after the pill, parentheses at 60%; no jank regression vs the pre-M2.5 build) |
| M3.3 | Expanded content keyed per activity | done (crossfade on a new key, same-key updates in place; new mid-frame motion_call_replaces_media__64ms; device check pending, phone not connected) |
| M3.4 | Compact content keyed per activity | done (pill keyed on ContentKey; tests pass; check later on the phone) |
| M4.1 | `Modifier.islandGestures` and release resolver | done |
| M4.2 | Card sideways fling-out or spring-back | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M4.3 | Card swipe-up tucks, with velocity hand-off | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M4.4 | Pill: down expands, further opens the shade, inward minimizes | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M4.5 | Back and predictive back collapse | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M5.1 | Resolve the motion tier | done (code and tests pass; check later on the phone: "Remove animations" and Battery Saver switch the tier live) |
| M5.2 | Apply Reduced and Efficient everywhere | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |
| M5.3 | "Eager in, calm out" retune | cancelled (was reverted; owner, after a device try: the swipes, motion feel, cover animation and pill position got worse; code kept in history, see commit 3cfb11c..3f88e06) |

**Checks left for later (phase 2–3).** The code and tests for these are done; they need the phone or the owner:
- On the phone: F1.4, F1.5 (one resize per notification burst), F3.3 (haptics), F3.5 ("Time to take action"), F3.6 (6 s / 8 s feel), F5.1 (Perfetto: one relayout), F5.2 (no clipped first frames at 120 Hz), F5.3 (full shrink on hide), F5.4 (envelope trial, debug flag), M3.3 and M3.4 (crossfades), M5.1 (tier switches live).
- With the phone connected: F6.3, generate the baseline profile and run `IslandFirstExpandBenchmark`.
- The owner: F6.4, create the release keystore.
- Cancelled by the owner (parked or reverted earlier), not to be redone without a new decision: M1.3–M1.5, M2.6, M2.7, M4.2–M4.5, M5.2, M5.3.

### Phase 4 + 6: Layout polish and signature motion ([file](phase-4-6-layout-and-motion-polish.md))
| ID | Title | Status |
|---|---|---|
| VD0.1 | Owner checkpoint screenshot set | done (89 states on a grey photo, plus black and white variants; FPS, weather and network left out because they were removed) |
| V1.1 | CutoutCalibration reader and CameraGeometry | done (pure maths in `island/layout/CameraGeometry.kt`, reader in `service/CutoutReader.kt`, `IslandStateManager.cameraGeometry`; 12 table cases pass; nothing reads it yet; checked on OPPO CPH2631, 2026-11-15: it reports a 38 × 32.5 dp rectangular cutout box, not the lens, so rectangular outlines now read as UNKNOWN and placement stays manual; still to check: a phone that reports a round hole) |
| V1.2 | Settings become fine-tune deltas, with migration | done (deltas are Float so a migrated pill never moves by rounding; calibration applies only after the migration has run; nothing renders from it until V1.3; C2.3 must zero the deltas in its reset; check later on the phone: upgrade keeps the pill where it was) |
| V1.3 | Apply calibration (cameraZone, window) | done (island and window draw from the calibrated settings; camera gap = measured zone; debug "Show camera outline" switch in the Test panel; the gap-follows-lens offset waits for V3.1, a Spacer offset has no visible effect; S2 unchanged; check later on the phone: the red ring sits on the lens, pill concentric, window holds the pill at extreme deltas) |
| V1.4 | "Fit to camera" switch and labels | done (one new row on Dimensions; sliders become fine-tune deltas with signed values; toggling keeps the pill in place; TalkBack reads the label and "plus 2 dp"; C2.4 is this same switch; check later on the phone: the detected camera line, toggling with the island on) |
| V1.5 | Landscape anchors to the camera | done, minus the minimised ring (pill on the top edge 8 dp from the side camera, top 6, gap 8; only the height fine-tune carries over; no migration while sideways; owner decided 2026-11-15: no ring at the side cutout, the minimised circle stays on top; check later on the phone with "Show in landscape" on) |
| V2.1 | CardGeometry, `inner()`, per-corner squircle | done (settled S2 pixel-identical; one mid-motion frame differs by anti-aliasing at a capsule's ends, from the new capsule fast path, re-recorded; tests pass) |
| V2.2 | One margin system | done (owner chose the even R/2 inset, CP1 questions 2026-11-15; S2 and bounds golden re-recorded: message card +3.5 dp at font 1.0, +8 at 1.3; no Robolectric parity test, the views and the measures share one cardInsetDp) |
| V2.3 | Concentric inner radii in squircles | done (all island corners in the squircle family; art and tiles rounded squares under the 0.42 cap, owner's CP1 answer; pill album art a rounded square too, not the table's circle; buttons near-capsules; one commit, not one per family; S2 re-recorded, 13 images; CP1 images in review/CP1; owner fixes after CP1: song cover capped at 0.27 (less round), download glyph not shifted, centred by its outline, checked on the phone) |
| V3.1 | Content-hugging compact width | turned off (owner, 2026-11-15, after seeing it: the pill stays exactly the Width setting, like the reference; content back at the outer ends; the width model stays behind CompactSlots.HUG_CONTENT for V3.3 and as the switch back) |
| V3.2 | Matched type; squeeze, abbreviate, drop | done (compact labels 13 sp, values 14 sp, units 11 sp, weight 600 both sides; no ellipsis in the pill: squeeze 92%, then a shorter form or first word, then drop; S2 re-recorded) |
| V3.3 | Oval bubble and readable ring | done (bubble in the squircle family, pixel-identical; width rule H to 1.25 H, but nothing the bubble shows today is wider than the circle, so it stays round; the ring readability came with B9.2) |
| V3.4 | Flashlight trailing value | done (level 25–100 % on the empty side, rolling; "On" without steps; 12 sp like the download pill at the owner's request) |
| V3.5 | System alert pills hug their content | done (owner's exact-width decision: glance pills keep today's widths and only grow to fit their widest content, never shrink; ringer fits "Vibrate", charging fits bolt + widest wattage + "100%", one width in every state; no ellipsis; unlock and connection pills hold icons only; checked on the owner's phone 2026-11-15) |
| V4.1 | Measured card heights | done (media card measured from its rows; flashlight and idle keep the table; spacing between info, bar and controls kept as before per owner) |
| V4.2 | One header, padding and button rhythm | done (CardRhythm from the notification card's metrics; notification, download and song cards read it; the song cover-to-title gap now scales (12 → 13.2 dp at 1.1); FPS, weather and network cards no longer exist; CP3: download header gap stays 13.2 dp, gutter stays 16 dp) |
| V5.1 | Semantic color roles | done (roles Recording, Success, Warning, Info, Timer, Stopwatch; LiveActivityColors aliases them; alarm yellow FFCC00 merged into FFD60A; SemanticColorTest) |
| V5.2 | Album accent via Palette with clamps | done (island/color/MediaAccent.kt: Palette vibrant, light vibrant, dominant; L* 70–80, chroma 20+, else no accent; adds androidx.palette 1.0.0; still on the main thread, cached; MediaAccentTest) |
| V5.3 | Legibility scrims | done (island/color/Scrim.kt: art scrim per top and bottom band from the brightest 8×8 cell, 0.38–0.85; glass with blur never clearer than 0.55 at the bottom; ScrimMathTest; the pixel-contrast VD0.1 states not added) |
| V6.1 | Adaptive keyline | done (keylineMode OFF/AUTO/ALWAYS replaces the switch, showOutline migrates to ALWAYS/OFF and is still written for rollback; AUTO follows WallpaperColors (lock screen's while locked) and night mode, resolved in the overlay service; Off/Auto/Always segmented row; one islandKeyline modifier, stroke inside the surface's own clip, for pill, bubble and both cards; today's tints and the thickness slider kept (spec's 1 dp / 20 % accent not applied: a look change, for CP3); default OFF, confirmed at CP3) |
| V6.2 | One outline rule, including notch | done (IslandOutline.resolve gives pill, notch, bubble and both cards one colour and width; high contrast 55 % white, at least 1.5 dp, floor moved out of the service; pill and notch fade with compactPillAlpha, the bubble's line fades in as the neck snaps (was a jump at 70 %), so no doubled edge; notch outlined: pill sides and bottom (open top) plus the bridge ears via notchEarsPath, never the top edge; OutlineResolverTest; HC card golden unchanged; CP3 images include hc and keyline notch/dual) |
| V7.1 | IslandIcons catalogue, Rounded only | done (owner kept Rounded; 7 glyphs changed, S2 re-recorded, full suite passes) |
| V7.2 | Icon size tokens and optical centering | done (CP4 approved: pill glyphs 18 dp, bubble 16 dp, six glyphs centred from measured centroids; full suite passes) |
| V7.3 | (optional) Material Symbols drawables | done (compiles; icon, optical-centre, crash and S2 screenshot tests pass; check later: the full suite, owner look at the new glyphs, APK size vs 3,655,255 bytes before) |
| D1.1 | Liquid SDF math and shader asset | removed (ponytail review, 2026-11-17: unused while D1.2 is on hold and D1.3 dropped; the shader and its tests are in commit 95b8721) |
| D1.2 | Split and merge with the shader | on hold (owner, 2026-11-15: not approved, risk without the one body; ask again) |
| D1.3 | (optional) Drip on expand | dropped (owner, 2026-11-15: needs the one body, which stays cancelled) |
| D2.1 | Alert bump replaces swapSquash | done (a change of activity kicks the pill's width spring (AlertBump: v0 = 0.06·W·ω / g(ζ), g(0.55) = 0.522), peak +6 %, on the pill's own width spring (the user's split curve) so width changes move as before; not on an appear; kicks add up, so two changes 100 ms apart are one motion; Reduce motion: none; no scaleX/scaleY on pill or notch bridge any more, so text isn't stretched; the leaving content's blur reads |width velocity| / kick; SWAP_SQUASH_IN removed; AlertBumpMathTest (numeric spring sim), AlertBumpTest (Robolectric: 6 % ± 1 %, none with Reduce motion); four goldens re-recorded (dual appear at 150 ms no longer shows the bubble above and below a squashed pill; the rest equalizer phase only)) |
| D2.2 | Banners peek, then recede | done (a new plain message opens its card as a peek and recedes after IslandTimings.peek (2.5 s, scaled by "Time to take action") through the existing auto-collapse loop, with the collapse curve; a touch turns it into a normal card; PeekPolicy: not for calls, alarms, live activities or ongoing, not while locked (no message text on the lock screen), not with TalkBack on (C3's focus signal pending), not over an open card, once per app per 5 s; after receding the auto-dismiss timer restarts; recedes to compact (A2.3 pending), fullscreen check pending (A6); "Peek at New Messages" switch in the notification settings, on by default (owner, 2026-11-17), applied by the overlay service; PeekStateMachineTest, BannerPeekTest) |
| D2.3 | "Same app, new event" signal | done (PulseDetector: the same app again with something new (isSameAppNudge: never a first appearance or a live-activity tick), at most once per 600 ms per app; it now drives the existing I3 sameAppNudge counter (icon bounce and NUDGE tick) and a D2.1 kick at 3 % on the pill (not while a card is open or with Reduce motion); no separate SharedFlow: I3 already reads the counter; IslandPulseDetectorTest) |
| D2.4 | Haptics at the neck snap and merge | done (CrossingLatch: NECK_SNAP once when a joined pair pulls past half the 5 dp snap gap, MERGE once when a split pair touches (gap ≤ 0); wobble can't repeat either, the first sample only sets state, 150 ms throttle; gap read in a snapshotFlow (no recomposition), off while a card is open and with Reduce motion; through IslandHaptics' existing Droid map (SEGMENT_FREQUENT_TICK on API 34+, CLOCK_TICK before) rather than raw VibrationEffect primitives, so the phone's touch-feedback setting and the haptics switch apply; expand keeps its click; CrossingLatchTest; device check pending) |
| D3.1 | Rolling numbers in compact and bubble | done (RollingNumberText gains slotTemplate + slotAlign (width from the template, so nothing shifts as it ticks), rolls down when the number shrinks unless told otherwise, and cross-fades in place under Reduce motion; download %, torch % and battery % in "100%" slots, the wattage rolls, the bubble numeral rolls centred in "100"; FPS, weather and network speed no longer exist; RollingSlotTest; four goldens re-recorded, only the shared −0.01 em number tracking differs) |
| D3.2 | Rolling numbers in expanded cards | done (FPS, weather and network cards no longer exist; the live-activity heroes already roll (verified: timers, recorder, call, ride ETA, scores); the download card's detail line ("12 MB of 40 MB") now rolls in its own tracking, up to 40 characters, and falls back to the old cut-off Text when it doesn't fit; expanded_progress pixel-identical at rest at font 1.0 and 1.3; RollingSlotTest +1) |
| D3.3 | Spring on progress rings | done (IslandMotion.progress = d 0.5 s, b 0.1 (ζ 0.9, k 158 at speed 1, speed-scaled) replaces the download bar's 0.9/140; rememberProgressAnimatable: springs forward, snaps on a drop of more than 0.2 (a new download); bubble ring and expanded bar read it in the draw phase; spinner ↔ value cross-fade 150 ms in the pill and the bubble; ProgressAnimationPolicyTest; S2 unchanged) |
| D4.1 | Shared art, avatar and icon between compact and expanded | on hold (owner, 2026-11-15: not approved, risk without the one body; ask again) |
| D4.2 | Consistent placement audit | done (audit table in review/CP5/d4.2-placement-audit.md with the compact-vs-card sheet; media and message OK; download %, call, timer and torch deferred: each would need an added or removed element or a different card structure, which the task doesn't allow; recorder, screen recording and ride to check on the phone; no code changes; owner call at CP5) |
| D5.1 | Duration and bounce model, with migration | done (curves stored as duration and bounce in schema 2; old spring keys read once to migrate and left for rollback; collapse is a stored curve; the old editor bridges through the conversion until D5.2; S2 unchanged, tests pass) |
| D5.2 | Curve editor v2 with honest preview | done (Expand/Collapse/Appear/Split, Duration + Bounce sliders, preview of k·speed², exact 1% settle time and overshoot in dp; SpringReadout + tests; not compiled or tested: no builds per owner) |
| D5.3 | Calm, Droid and Lively presets | done (MotionPreset table in model/MotionCurves.kt, preset chips plus Custom and Try on island in the curve editor, PresetTableTest; Lively collapse is 0.862 of expand vs the stated 0.85, kept as is (owner, 2026-11-15); not compiled or tested: no builds per owner) |
| D6.1 | (optional) Orbit ring | dropped (owner, 2026-11-15: not wanted) |

### Phase 5: Dynamic icons ([file](phase-5-dynamic-icons.md))
| ID | Title | Status |
|---|---|---|
| I0.1 | Symbol-effect tokens and motion-tier shim | done (tokens in `IslandMotion.Symbol`; the tier comes from M5.1, so no shim; graphics-shapes pin not added, waiting on owner sign-off) |
| I1.1 | Engine core: bounce and wiggle | done (`island/icon/`; tests pass; no callers yet; device feel check in I4.1) |
| I1.2 | Pulse, breathe, rotate; draw-phase pulses | done (recording dot and alarm icon on the engine; S2 unchanged, tests pass; check later on the phone: the dot pulse reads as before) |
| I1.3 | Appear, disappear and replace | done (`DynamicIconSwitch`, `DynamicIconVisibility`; tests pass; no callers until I2.2/I3.2) |
| I1.4 | Variable color (LayeredIcon) | done (bands cut the real V7 glyph, so no hand-drawn layer paths; signal-bars step dropped, that view was removed earlier; tests pass; device check in I4.1) |
| I2.1 | Morph toolkit and play↔pause | done (morph rebuilt in I4.1: graphics-shapes folded the triangle mid-way, now interpolated convex quads, no dependency; media button and recorder/timer pause buttons morph; at rest the real Material glyphs draw, so S2 is unchanged; check later on the phone: 10 taps never show a broken shape, crisp at 1–1.6× card scale) |
| I2.2 | Checkmark draw-on and slash overlay | done (unlock and connection checks draw on; ringer Ring↔Silent draws the slash, to or from Vibrate replaces; settled glyphs unchanged, S2 unchanged; check later on the phone: lock/unlock, earbuds, ringer switch) |
| I2.3 | Flashlight and mic morphs | done (torch off from the card draws the slash first; bubble mic slashed while paused; `MuteGlyph` ready for A3.2; tests pass; check later on the phone: torch off from the card and from QS) |
| I3.1 | Icon event counters | done (`IslandStateManager.iconSignals`; tests pass) |
| I3.2 | Wire bounce, wiggle, pulse, appear and replace | done (same-app bounce + NUDGE tick, ringing call wiggle + answer bounce, alarm wiggle then breathe, recorder mic pulse, connection glyph appear, ringer arrival wiggles replace the keyframe shakes; tests pass; check later on the phone: a WhatsApp burst, a real incoming call, Time's up, earbuds, the ringer switch) |
| I3.3 | Wire charging, download, network and flashlight | done (owner approved the bolt, the download fill and the torch level, 2026-11-15; charging fill read in the draw phase; network arrows dropped with the network widget; 4 S2 screenshots re-recorded as intended; check later on the phone: plug in, a Play Store download, torch brightness) |
| I3.4 | Media: cover flip and play-state morph | done (flip follows the island speed, turn-in on Symbol.replace; Reduced motion crossfades the covers; play-state morph landed in I2.1; tests pass; check later on the phone: skip tracks 5× fast) |
| I4.1 | Effect catalogue in the Test panel | done (catalogue in the Test panel and offline clips in `review/icon-clips/`; waiting on the owner to tune the feel on the phone) |
| I4.2 | Buttons that fire real events on the island | done (same app ×3, Bluetooth, ringer cycle, charging 40 %, download 0→100, torch levels; through the public IslandStateManager calls; tests pass; check later on the phone: run each with the island on. `clear()` leaves the signal counters alone, since resetting one would itself fire an effect) |

### Phase 7: Live activities ([file](phase-7-live-activities.md))
| ID | Title | Status |
|---|---|---|
| A1.1 | Classify each notification once | done (`NotificationInfo.kind`, lazy, outside equals; NRS classifies on the parser thread; 15 call sites read it; NotificationInfoKindTest counts 1 classifier run for 60 reads; S2 verify unchanged; check later on the phone: nothing, no visible change) |
| A1.2 | LiveActivitySpec, Slot and legacy mapper | done (`island/activity/`: LiveActivitySpec, Slot (plus ClockFace for TimerClockIcon, and FLASHLIGHT_LEVEL), IslandIcon and AccentRole as plain enums so the spec has no Compose types, LegacySpecMapper for notifications, progress, media, screen recording and torch; stateSignature reuses B4.5's dismissSignature; the Test lab demos are now a pure `demoNotification()`; nothing reads the specs yet; LegacySpecMapperTest over all 12 demos and 18 call, clock, recorder and progress fixtures; check later on the phone: nothing visible) |
| A1.3 | ActivityStore in shadow mode | done, minus the Test lab "Store" row (`island/activity/ActivityStore.kt`; IslandStateManager writes every notification, download, media, torch and screen-recording change to it as well as to the old slots; dismissal memory by stateSignature, remove forgets it; debug thread check logs a warning instead of crashing (shadow mode); ActivityStoreTest 12 cases incl. call + timer + message = 3 entries while the slot shows 1; the Test lab dump row adds visible text to the settings app, waiting on the owner; check later on the phone: nothing visible) |
| A1.4 | Old slots become store projections; delete the stash | done (`island/activity/LegacyProjection.kt`: the two old slots are picked from the store after each change (ringing call or alarm not covered; a live repost waits behind a one-off card; when the shown card goes, the newest live entry left returns; downloads: the one just updated, else the newest left); stash, dismissedSignatures and dismissedProgressKeys deleted (the store remembers dismissals; a dismissed download stays gone until removed); `isIncomingCallRinging` scans the store; IslandStateBehaviorTest unchanged and green; S1 and S2 unchanged; LegacyProjectionTest 9 cases incl. call → timer → message and two downloads; check later on the phone: a Clock timer, then a WhatsApp message, then a second timer, dismiss the message: both timers still reachable) |
| A1.5 | Generic slot renderers | todo |
| A1.6 | Compact and bubble render from slots | todo |
| A1.7 | Media, flashlight and recording become store entries | todo |
| A1.8 | Expanded card, height, open target and outline from the spec | todo |
| A1.9 | Delete the legacy single-slot state | todo |
| A2.1 | Pure `arbitrate()` with the tier table | done (`island/activity/Arbiter.kt`, `TierTable.kt`: tiers, peek into the secondary place, newest T4 glance unless a T0 shows, foreground hide except T0, fullscreen gate and lock SECRET placeholders for A6/A5, per-app overrides (never into or out of T0), priority apps head T2; "why" reasons per activity; the 6 intended differences from today's order are listed in the KDoc; nothing uses it until A2.2; ArbiterTest 21 cases, branch coverage not measured) |
| A2.2 | The scene uses the arbiter | todo |
| A2.3 | Banners recede | todo |
| A2.4 | "+N" overflow | todo |
| A2.5 | (optional) "Why is this primary?" and overrides | todo |
| A2.6 | Test lab: stack demo activities | done, minus the panel UI (`LiveActivityTester.stack` keeps earlier demos, each stacked demo has its own ticker, the existing Clear removes them all; test keys keep the `droidbubble.test.` prefix; TesterStackTest: 3 stacked demos = 3 store entries; the "Stack" chip and the "On the island now" list (A1.3's store row) add visible UI to the Test lab, waiting on the owner, see review/CP7/a1.3-a2.6-testlab.png) |
| A3.1 | NotificationSnapshot and adapter chain | done (`island/activity/adapters/`: SnapshotReader reads each field in its own runCatching (a wrong-type extra gives its default and a breadcrumb), AdapterRegistry runs the chain with crash containment, LegacyAdapter holds the old parse rules verbatim; the custom-view inflation and the app label / bitmaps are read only when the adapter asks (no extra work for dropped notifications); adapters return an AdapterOutcome (Drop / Ignore / Post with info + spec) instead of a bare spec, since the listener still needs Drop vs Ignore until A1.9; FixtureCharacterizationTest unchanged; AdapterChainTest 9 cases; check later on the phone: nothing visible) |
| A3.2 | CallStyle adapter | todo |
| A3.3 | Live Updates: promotion, shortCriticalText, countdown | todo |
| A3.4 | ProgressStyle parser | todo |
| A3.5 | Journey bar | todo |
| A3.6 | Navigation adapter | todo |
| A3.7 | Chronometer and timer reader | done, minus the overtime display (`island/activity/adapters/ChronometerReader.kt`: the custom-view gate (with `CATEGORY_STOPWATCH`), the custom-view running rule and the base rules, verbatim, plus `isOvertime`; NRS calls it; fixture golden, LiveTimerBaseStabilizerTest and CustomTimerCacheTest unchanged; the "−0:12" overtime text after Time's up is a visible change, waiting on the owner; ChronometerAdapter registers with A3.1; check later on the phone: nothing visible) |
| A4.1 | ControlRole resolver | done (`island/activity/controls/`: structured CallStyle intents and RemoteInput, then `getSemanticAction()`, then the icon resource name (cached per package and resId), then an English title table until A4.2; NRS fills `role`, `semanticAction`, `iconResName` on every `NotificationAction`; nothing reads the role yet (A4.3); no S5 fixture carries a semantic action or a resource icon, so ControlResolverTest covers those with synthetic actions; FixtureCharacterizationTest unchanged; check later on the phone: nothing visible) |
| A4.2 | Per-locale tables and digit normalization | done (`controls/LocaleTables.kt`: button words and state words for ar, es, tr, de, fr, ru, zh, id, hi, every non-English token backed by a fixture (named in a comment), no guessed translations; "رد" and "Responder" are Answer on a call, Reply elsewhere; `text/Digits.kt`: Arabic-Indic and Persian digits to ASCII, tashkeel stripped, alef forms unified, distances in m / km / م / كم; the old English regexes and the timer regex use explicit Unicode boundaries and [0-9] after digit normalisation; ControlResolver step (d) reads the tables; LocaleTablesTest (JVM) and LocaleTablesFixtureTest (Robolectric) agree; fixture golden unchanged; pt has no fixture, so no Portuguese words; check later on the phone: nothing visible until A4.3) |
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
| C1.1 | Permission catalogue: levels and honest copy | done (allPermissions(sdkInt) holds levels, grant kinds and the honest copy; pending(context) checks grants; ids unchanged; notification access opens our own toggle on API 30+; 'Nearby devices' is in the catalogue but not listed until C1.5; rows still show titles until C1.2; PermissionCatalogTest 6 cases) |
| C1.2 | Permission rows: descriptions, levels, "Not now" | done (PermissionSection in its own file: level overline, what + why under the title, red only for Required, Allow (labelled 'Allow <title>') and 'Not now' on non-required rows; skipped items persist in skipped_permissions and fold into 'N optional items skipped · Review'; header 'Required setup / N required' or 'Finish setup / N optional'; one runtime launcher for notifications / mic / nearby devices with the app-settings fallback; new AppIcons.Info; the explanation isn't capped at 3 lines so the mic copy isn't cut; PermissionSectionTest 6 cases, round-trip test covers the key; check later on the phone: API 33+ notifications open the system dialog) |
| C1.3 | Accessibility-service disclosure | todo |
| C1.4 | Restricted-settings helper | todo |
| C1.5 | Bluetooth permission only when needed | todo |
| C1.6 | Light guided first run | todo |
| C2.1 | Preview sample activity | todo |
| C2.2 | Live preview on Dimensions and Appearance | todo |
| C2.3 | Reset buttons | todo |
| C2.4 | "Fit to camera" on Dimensions (with V1.4) | todo |
| C3.1 | A11y test harness and device spike | done (MainScreenTest rebuilt on the real MainScreen and ViewModel, Enable Island switch tagged; IslandSemanticsTest + A11yMatchers pin today: the pill is tappable with no label; androidTest compiles; TalkBack device spike pending, phone not connected: C3.4 waits on it) |
| C3.2 | IslandA11y environment | done (IslandA11y.screenReaderOn: touch exploration or another package's spoken-feedback service, never our own status bar layer; follows touch-exploration, accessibility-state and (API 33+) services-state listeners, registered in the overlay service; recommendedTimeoutMs goes through IslandTimings; IslandA11yTest 5 cases; check later on the phone: TalkBack on/off flips it within a second) |
| C3.3 | IslandSpeech label builder | done (IslandSpeech: headline, value, click / long-click labels, availableActions per node with the Dismiss exclusions, pane title, announcement politeness, ICU durations and locale percentages; SpeechSubject + subjectFor(NotificationInfo) so the wiring is mechanical; 45 spoken strings in strings.xml; IslandSpeechTest 18 cases incl. es; no call sites yet) |
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
- **Weather and network speed removed (2026-11-09, owner's decision).** The weather widget and the "Wi-Fi & Network Speed" widget are gone: `WeatherInfo`, `NetworkSpeedInfo`, `PersistentWidgetTelemetryManager`, `showWeather` / `showNetworkSpeed`, `IslandPriority.WEATHER` / `NETWORK_SPEED`, the expanded weather and network cards, the "persistent widgets" in the view and the window math, and their settings rows. Stored `"WEATHER"` / `"NETWORK_SPEED"` priorities are dropped by `parseFeaturePriorities`. B7.1 and B7.2 are dropped; B9.1's network-speed fix and its test went with the card. The "Wi-Fi connecting" glance in System Alerts (`showWifi`) stays. Skip weather and network-speed items in later phases (2–3, 4–6, 7, 8 and PLAN still mention them).
- **Pre-Phase-0 cleanup (2026-11-08, owner-approved).** These are gone; skip any task text that still mentions them:
  - FPS everywhere (`IslandPriority.FPS`, `FpsInfo`, `showFps`, the settings row, telemetry). Stored `"FPS"`/`"TIMER"` priorities are dropped by `parseFeaturePriorities` (`model/IslandSettings.kt`). This was B7.3.
  - The settings `isTeardropCutout` (and the Tear-drop picker option), `mediaWaveAnimation`, `wavyProgressBar`, `spinningAlbumArt` and `cornerRadiusDp` (now the fixed `IslandTokens.CompactCornerRadius` = 22 dp).
  - The "Persistent Island Views · Coming soon" section (this was C5.5).
  - `ScreenRecordingService` and `FOREGROUND_SERVICE_MEDIA_PROJECTION`; the manifest `PowerConnectionReceiver` (the A9.2 receiver half); Shizuku (`ShizukuHelper`, both dependencies, the exported `ShizukuProvider`, `WRITE_SECURE_SETTINGS`).
  - Dead code: `morphBlurEffect`, `expandedCornerRadiusFor`, `CellularSignalBars`, `toggleExpand`, `activeExpandedPriority`, `IslandOverlayRoot`'s `onBoundsChange` parameter.
  - Kept on purpose: the unused tokens `SpaceLg`, `SpaceXl`, `RadiusCard`, `SurfaceSunken`, `AccentPurple`, `AccentYellow` (their values still appear as literals, so F2.2/F2.5 route those literals into them), `HighContrastOutline` (B5.1), `IslandTouchManager` (B2.3).
- **Owner decision (2026-11-15): one-body-dependent D tasks.** One body (M1.3–M1.5) stays cancelled, so the D tasks built on it are adapted to today's two bodies. Approved (low risk): D2.1 (alert bump on the pill's own width), D2.2 (peek then recede, with today's expand and collapse), D2.3 (same-app pulse), D2.4 (haptics at today's neck), D4.2 (placement audit, without waiting for D4.1). Not approved (risk): D1.2 (liquid shader, per-frame cost without M2.6/M2.7) and D4.1 (shared art travel); D1.3 (drip) dropped, it needs the one body. Ask again before any of these three.
- **Owner decision (2026-11-12):** the island's Reduced motion tier turns on automatically only at a system animator scale of 0 ("Remove animations"), not at 0.5x (people set that for speed). M5.1 implements it; 05 R11 and the M5.1 entry still say "below 1".
- **Owner decisions (2026-11-11):** Decision D1 = **no**: `minSdk` stays 29. Features that need a newer Android are gated in `island/IslandApi.kt` and keep a fallback (squircle corners and AGSL need API 33, blur needs API 31). The liquid shader (epic D1) must check `IslandApi.hasRuntimeShader` and fall back to the current neck. Decision D7 = **yes**: MATCH_PARENT on expand, with the envelope window as a flag-gated trial (F5.4).
- **Owner decisions (2026-11-08):** D5 = yes, D6 = yes, adb debug control = **no** (B1.1 registers `RECEIVER_NOT_EXPORTED` only; the Test panel covers manual testing).
- The `MainScreen.kt` line ranges in the phase-8 file are stale; the curve editor is now around `:2649–2796`. Re-check them before C5.1.
- `IslandGeometry` means the animated shape (M1). The cutout data is `CameraGeometry` (V1), and margins are `CardGeometry` (V2).
- Overlaps:
  - F2.3 and V5.1 both merge duplicate colors. Whichever lands first does the merge; the other only names the roles.
  - D2.3 and I3: D2.3 detects the event, I3 owns the icon effect.
  - C2.4 and V1.4 are the same "Fit to camera" switch. Do them together.
