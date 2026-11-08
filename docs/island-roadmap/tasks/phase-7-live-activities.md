# Phase 7 — Live activities & functions

**Goal.** Replace the single notification slot with a store of many live activities, a tiered arbiter you can test, and adapters that read Android's structured signals (CallStyle, Live Updates / ProgressStyle, navigation, chronometer) before falling back to heuristics. Then add the functions that make the island useful: locale-independent controls, lock-screen privacy, a fullscreen policy, new system sources, "Pin as activity" and a media session switcher.

**This phase is behaviour, not visuals (PLAN v2 §0).**
- Every new presentation reuses an existing card or compact design (`LiveLayout`, `LiveTrackingRow`, `LiveTimerRow`, the glance pills, `SquircleShape`), the F2 tokens and the V7 icon family.
- When a new activity needs an animated icon, it names an epic I effect (I1 engine, I3 event wiring). It never designs its own.
- Apple is the quality reference only. Tiers, overflow and receding banners are Droidbubble's own rules (04 §6).

**Assumed inputs.** These tasks are referenced by ID:
- S5: the notification fixture corpus
- B3: live activities on by default (`showLiveActivities`)
- B4: hardened heuristics
- B6: safe actions
- B10: off-main parsing with sequence numbers
- B11: per-notification guards and breadcrumbs
- F1: `deriveIslandScene`
- F2: tokens
- F3: `IslandTimings` and `IslandHaptics`
- F5: `WindowSpec`

Some tasks also use M3 (content keyed per activity), M4 (gestures), V3 (compact balance), V4 (measured heights), V7 (icons), I1/I3 (dynamic icons), C1 (permission UX) and C3 (TalkBack).

## Migration strategy: the app stays shippable at every merge

| Stage | Tasks | What changes | What stays |
|---|---|---|---|
| 1. Shadow | A1.1–A1.3 | Kind is computed once. `LiveActivitySpec` and `ActivityStore` exist, and every update is also written into the store. | Nothing reads the store, so the visible behaviour is identical. |
| 2. Projection | A1.4 | `activeNotification` and `activeProgress` become *derived views* of the store. The stash is deleted. | `IslandView` code is unchanged. The existing `IslandStateBehaviorTest` passes unchanged. |
| 3. Presentation | A1.5–A1.8 | Kinds render from slots, one family per PR. | The S2 screenshots are **pixel-identical**. |
| 4. Arbitration | A2.1–A2.2, A1.9 | `deriveIslandScene` (F1) calls the tiered arbiter. `currentPriority`, the view's ordering code and the legacy flows are deleted. | The S1 characterization tests pass, except for documented, intended diffs. |
| 5. Features | A2.3+, A3–A9 | Each new adapter or source is one file, plus slots and a test. | — |

**Parallelism.**
- A2.1, A3.7 and A4.1 are pure and can start as soon as F1 lands, alongside A1.1–A1.3.
- A4–A7 can run alongside A1 when they don't touch the store files (`island/activity/*`, `IslandStateManager.kt`).
- **Contention:** `IslandView.kt` and `LiveActivities.kt` are touched by A1.5–A1.8, A2.3, A2.4, A3.2, A3.5, A3.6, A5.2 and A8.3. Never run two of those at once, and never alongside an M, V or I task on the same file. Ideally the 06 Phase-5 file split lands first (PLAN §5).

## Conventions for every task in this phase

**API levels.** `minSdk = 29`, `compileSdk = 36` (`app/build.gradle.kts:13,16`), so API-36 symbols compile, but each one needs a runtime guard. If D1 raises minSdk to 33, drop the guards for API 30–33.

| API | Symbols used here | Guard |
|---|---|---|
| 28 | `Notification.Action.getSemanticAction()`; `Icon.getType()/getResId()/getResPackage()`; `BatteryManager.computeChargeTimeRemaining()` | none needed (≥ minSdk) |
| 30 | `WindowInsets.isVisible(Type.statusBars())`; `BluetoothDevice.getAlias()` | `SDK_INT >= R` |
| 31 | CallStyle extras by string key (`android.callType`, `android.callIsVideo`, `android.verificationText`, `android.hangUpIntent`); `BLUETOOTH_CONNECT` runtime; `CATEGORY_STOPWATCH`, `CATEGORY_MISSED_CALL`, `CATEGORY_WORKOUT` | Read the keys as strings on every level (they're absent below 31), like `NotificationReceiverService.kt:962`. |
| 33 | typed `Bundle.getParcelable(key, Class)` | the existing pattern at `NotificationReceiverService.kt:1037-1043` |
| 35 | `WindowManager.addScreenRecordingCallback`, `DETECT_SCREEN_RECORDING` | `SDK_INT >= VANILLA_ICE_CREAM` |
| 36 | `Notification.FLAG_PROMOTED_ONGOING`, `hasPromotableCharacteristics()`, `EXTRA_REQUEST_PROMOTED_ONGOING`, `Notification.ProgressStyle` getters, `android.shortCriticalText` | `SDK_INT >= 36` **and** `runCatching` |

**Rules.**
1. Every extra, hidden key or reflective call is wrapped in `runCatching`. On failure the field is `null` and a B11 breadcrumb is written. A raw or `@hide` key is marked **[M]** and is never load-bearing on its own.
2. Parsing happens off the main thread on the B10 parser dispatcher. Adapters are pure Kotlin. Store writes happen on main and are applied in post order (B10 sequence number).
3. No new English-only string matching. Text heuristics go through the A4 resolver.
4. **Fixture naming** (S5 must provide these; map the names if S5 chose others): `call/{gphone_incoming_en, gphone_ongoing_mute_speaker_en, whatsapp_incoming_ar, whatsapp_video_es, gphone_screening_en, samsung_incoming_tr}`, `clock/{gclock_timer_running_en, gclock_timer_paused_de, gclock_timesup_fr, samsung_timer_ar, gclock_stopwatch_en, gclock_alarm_ringing_es}`, `recorder/{grecorder_en, samsung_voicenote_ru, miui_recorder_zh}`, `nav/{gmaps_turn_en, gmaps_turn_ar, gmaps_liveupdate_36_en, waze_en}`, `progress/{play_download_en, chrome_download_hi, uber_progressstyle_36_en, talabat_ongoing_ar, careem_ongoing_ar}`, `screenrec/{pixel_en, samsung_es, xiaomi_id}`, `message/{whatsapp_ar, telegram_en, gmail_private_en}`.
5. Tests live in `app/src/test/java/com/droidbubble/app/island/activity/`. Use pure JUnit by default. Use Robolectric `@Config(sdk = [35])`, matching `IslandOverlayCrashTest.kt:39`, only when a `Bundle` or `Notification` is needed.
6. The definition of done is PLAN §6. A **Design check** in a task means: same card, compact layout and squircle; no new hues outside F2; motion via M or I epics.

---

## A1 — `LiveActivitySpec`, slot presentations and the `ActivityStore`

### A1.1 — Classify each notification once
- **Goal:** `liveActivityKind()` runs once per `NotificationInfo` instance. Today it runs on every recomposition and every timer tick.
- **Why:** 04 §3 "Data model": it is "recomputed on every recomposition, including each timer tick". Its regexes also run inside height measurement and TTS.
- **Files:**
  - Classifier: `island/LiveActivities.kt:226-253`
  - Model: `model/IslandEvent.kt:49-94`
  - Call sites: `island/IslandView.kt:2478, 2716, 2889, 3666`; `island/IslandComponents.kt:355`; `island/IslandStateManager.kt:545`; `service/NotificationReceiverService.kt:1126`; `island/LiveActivityTester.kt:134`; `service/SpokenAlerts.kt:54, 77`
- **API sketch:**
  ```kotlin
  data class NotificationInfo(/* unchanged */) {
      /** Classified once per instance; copy() yields a new instance and re-classifies. Not in equals(). */
      val kind: LiveActivityKind by lazy(LazyThreadSafetyMode.PUBLICATION) { liveActivityKind(this) }
  }
  ```
  For now the model imports the island classifier. This is temporary: A3.1 makes `kind` an adapter output.
- **Steps:**
  1. Add the lazy `kind`.
  2. In NRS, touch `notifInfo.kind` right after construction (`NotificationReceiverService.kt:1093-1122`) so the regex runs off-main once B10 is in.
  3. Replace the 9 call sites with `n.kind`.
  4. Keep `liveActivityKind()` public for `LiveActivityKindTest`.
- **Acceptance criteria:**
  - All tests are green, and the S2 screenshots are identical.
  - With a running Google Clock timer, a debug counter in `liveActivityKind` shows 1 call per repost, not 1 per second per composable.
- **Tests:**
  - `LiveActivityKindTest` unchanged.
  - New `NotificationInfoKindTest`: `copy(title = "Time's up")` changes the kind, and `equals` ignores `kind`.
- **Depends on:** B4 (the regexes it caches must already be the hardened ones).
- **Effort:** S   **Risk:** low — no behaviour change.
- **Ships as:** Plumbing.

### A1.2 — Define `LiveActivitySpec`, `Slot` and the legacy mapper
- **Goal:** One immutable description of any activity, plus a pure mapper from today's `NotificationInfo`, `MediaInfo`, `ScreenRecordingState` and torch level to it. Nothing consumes it yet.
- **Why:** 04 §5.2. Today kinds come from three switch dimensions (`IslandPriority`, `LiveActivityKind`, ~25 flows), and adding a type touches about 10 places.
- **Files:**
  - New: `island/activity/LiveActivitySpec.kt`, `island/activity/Slot.kt`, `island/activity/LegacySpecMapper.kt`
  - Read-only references: `model/IslandEvent.kt:6-161`, `island/LiveActivities.kt:285-313` (accents), `:1262-1310` (compact icon and text rules)
- **API sketch:**
  ```kotlin
  package com.droidbubble.app.island.activity

  @JvmInline value class ActivityId(val value: String) // "n:<sbn.key>", "media:<pkg>", "rec:screen", "torch", "sys:<glance>", "bt:<addr>"

  enum class Tier { T0_INTERRUPT, T1_LIVE, T2_ONGOING, T3_BANNER, T4_GLANCE, T5_AMBIENT }
  enum class ActivitySource { NOTIFICATION, MEDIA_SESSION, SCREEN_RECORDING, TORCH, SYSTEM, WIDGET, PINNED, TEST }
  enum class ActivityKind {
      CALL_RINGING, CALL_ONGOING, ALERT, NAVIGATION, VOICE_RECORDING, SCREEN_RECORDING, TIMER, STOPWATCH, ALARM,
      TRACKING, SPORTS, MEDIA, PROGRESS, FLASHLIGHT, VPN, PRIVACY, PINNED, MESSAGE, GLANCE, WIDGET
  }
  /** User drag-order bucket (settings.featurePriorities); A2.2 adds LIVE and SYSTEM. */
  enum class OrderBucket { MEDIA, PROGRESS, RECORDING, NOTIFICATION, FLASHLIGHT, LIVE, SYSTEM }

  data class LiveActivitySpec(
      val id: ActivityId,
      val kind: ActivityKind,
      val source: ActivitySource,
      val packageName: String?,              // null for system sources
      val tier: Tier,                        // adapter default; arbiter may override per app (A2.5)
      val bucket: OrderBucket,
      val relevance: Float = 0f,             // tiebreak inside a tier, 0..1
      val startedAt: Long,                   // first seen, wall ms
      val lastSignificantAt: Long,           // state change, NOT a clock tick
      val staleAt: Long? = null,             // e.g. paused media + 10 min; null = never (no 8 h cap)
      val expiresAt: Long? = null,           // glances (A7.1)
      val alerting: Boolean = false,         // this update may peek-expand (T0 always)
      val accent: AccentRole,                // F2 semantic colour role, not a Color literal
      val compactLeading: Slot,
      val compactTrailing: Slot,
      val minimal: Slot,                     // split bubble
      val expanded: ExpandedSpec,
      val open: OpenTarget,                  // contentIntent first, then the launcher (B6)
      val privacy: PrivacySpec = PrivacySpec.Public,  // A5
      val stateSignature: Int,               // what must change for a dismissed activity to return
      val payload: ActivityPayload,          // back-reference for Custom renderers while migrating
  )

  sealed interface Slot {
      data object Empty : Slot
      data class Icon(val icon: IslandIcon, val effect: IconEffect? = null) : Slot      // V7 icon, I1 effect
      data class Image(val ref: ImageRef, val shape: SlotShape = SlotShape.Circle) : Slot // avatar, app icon, maneuver arrow
      data class Text(val text: String, val numeric: Boolean = false) : Slot               // numeric -> RollingNumberText
      data class Timer(val baseWallMs: Long, val countDown: Boolean, val pausedText: String?) : Slot
      data class Ring(val baseWallMs: Long, val key: String) : Slot                        // TimerRingIcon
      data class Progress(val fraction: Float?) : Slot                                     // null = indeterminate
      data object Waveform : Slot
      data object RecordingDot : Slot
      data class Score(val home: String, val away: String) : Slot
      data class Custom(val key: CustomSlot) : Slot   // escape hatch to today's bespoke composables
  }
  enum class CustomSlot { MEDIA_ART, EQUALIZER, FLASHLIGHT_GLOW, CONTACT_AVATAR, LEGACY }

  enum class ExpandedPresentation {
      CALL, ALERT, TIMER_ROW, RECORDING_ROW, TRACKING_ROW, NAVIGATION_ROW, SCORE_ROW,
      PROGRESS_CARD, NOTIFICATION_CARD, MEDIA_CARD, FLASHLIGHT_CARD, WEATHER, NETWORK, NONE
  }
  data class ExpandedSpec(
      val presentation: ExpandedPresentation,
      val leading: Slot = Slot.Empty, val trailing: Slot = Slot.Empty,
      val center: Slot = Slot.Empty, val bottom: Slot = Slot.Empty,      // bottom: ProgressJourney (A3.5)
      val controls: List<ActivityControl> = emptyList(),
  )
  data class ActivityControl(val role: ControlRole, val label: String, val intent: PendingIntent?, val weight: LiveButtonStyle)

  sealed interface ActivityPayload {
      data class Notification(val info: NotificationInfo) : ActivityPayload
      data class Media(val info: MediaInfo) : ActivityPayload
      data class ScreenRecording(val state: ScreenRecordingState) : ActivityPayload
      data class Torch(val level: Int, val max: Int) : ActivityPayload
      data class System(val glance: GlanceInfo) : ActivityPayload
  }
  ```
- **Steps:**
  1. Add the types.
  2. Write `LegacySpecMapper.fromNotification(info)`, `fromProgress`, `fromMedia`, `fromRecording` and `fromTorch`. Encode today's rules: slots from `LiveActivityCompactIcon` and `liveCompactText` (`LiveActivities.kt:1262-1310`), and the tiers from the A2.1 table.
  3. Pick `stateSignature` per kind:
     - call: `isIncomingCall`
     - timer: running or paused, plus `countDown`
     - tracking: title and status text, ignoring ETA digits
     - message: `text`, which matches today's rule at `IslandStateManager.kt:552`
- **Acceptance criteria:**
  - Every `LiveActivityTester.Demo` (`LiveActivityTester.kt:23-36`) maps to a spec with the expected kind, tier and slots.
  - No production code reads the specs yet.
- **Tests:** `LegacySpecMapperTest`, table-driven over the Tester demos plus the S5 fixtures for call, clock, recorder and progress.
- **Depends on:** A1.1, F1 (`AccentRole` and `IslandIcon` come from F2/V7; use placeholder enums if they haven't landed yet).
- **Effort:** M   **Risk:** low — pure.
- **Ships as:** Plumbing.

### A1.3 — `ActivityStore` in shadow mode
- **Goal:** A main-confined `Map<ActivityId, LiveActivitySpec>` with dismissal memory. `IslandStateManager` writes every update to it as well as to the old slots.
- **Why:** 04 P0-2. The store is the precondition for "never lose an activity".
- **Files:**
  - New: `island/activity/ActivityStore.kt`
  - Changed: `island/IslandStateManager.kt` (`updateNotification` :542-614, `updateProgress` :616-630, `removeNotification` :632-653, `dismissCurrentNotification` :669-689, `dismissProgress` :691-704, `updateMedia` :473-508, `updateRecordingState` :706-713, `updateFlashlightState` :341-349, `resetForTest` :733-762)
- **API sketch:**
  ```kotlin
  class ActivityStore(private val now: () -> Long = System::currentTimeMillis) {
      private val _activities = MutableStateFlow<Map<ActivityId, LiveActivitySpec>>(emptyMap()) // LinkedHashMap
      val activities: StateFlow<Map<ActivityId, LiveActivitySpec>> = _activities.asStateFlow()
      private val dismissed = HashMap<ActivityId, Int>()      // id -> stateSignature at dismissal

      /** Main thread only. Keeps startedAt from the previous version; ignored while dismissed with an equal signature. */
      fun upsert(spec: LiveActivitySpec): Boolean
      fun remove(id: ActivityId)                     // source ended: also forgets the dismissal
      fun dismiss(id: ActivityId)                    // user swipe: remember the signature, drop from the map
      fun get(id: ActivityId): LiveActivitySpec?
      fun expire(atMs: Long = now())                 // drops expiresAt/staleAt <= atMs
      @VisibleForTesting fun clear()
  }
  // IslandStateManager gains: val store = ActivityStore()
  ```
- **Steps:**
  1. Implement the store. Add a debug `check(Looper.myLooper() == Looper.getMainLooper())` under the B1 `BuildConfig.DEBUG`.
  2. Mirror every existing mutation into it through `LegacySpecMapper`.
  3. Add a read-only "Store" dump row to the Test lab (`ui/main/LiveActivityTestPanel.kt`) listing id, kind and tier.
- **Acceptance criteria:**
  - With a call, a timer and a message active, the store holds 3 entries while the old slot shows 1.
  - Removing a notification removes its entry.
  - A swipe dismissal survives an identical repost and is lifted by a changed signature.
- **Tests:** `ActivityStoreTest`: upsert keeps `startedAt`; dismissal versus signature; `remove` clears the dismissal; `expire`; insertion order is stable.
- **Depends on:** A1.2.
- **Effort:** S   **Risk:** low — it's write-only.
- **Ships as:** Plumbing, plus a dev-only Test lab row.

### A1.4 — Old slots become projections of the store; delete the stash
- **Goal:** `activeNotification` and `activeProgress` are computed from the store by a pure function, so a third activity can no longer be lost. `stashedLiveNotification` is deleted.
- **Why:**
  - 04 §3: "A second live activity overwrites the stash, so the earlier one is lost. Countdowns are rarely reposted, so that loss is permanent."
  - With two downloads, only the last one is shown.
- **Files:** `island/IslandStateManager.kt`:
  - flows :113-117
  - `isIncomingCallRinging` :229-230
  - stash :526-540, :550, :563-572, :636, :678
  - new `island/activity/LegacyProjection.kt`
- **API sketch:**
  ```kotlin
  data class LegacySlots(val notification: LiveActivitySpec?, val progress: LiveActivitySpec?)
  /** Reproduces today's single-slot choice; A2.2 replaces it with arbitrate(). */
  fun projectLegacySlots(all: Collection<LiveActivitySpec>, previous: LegacySlots): LegacySlots
  ```
  Projection rules, which keep today's behaviour:
  1. A ringing call or alert holds the notification slot against non-call, non-alert newcomers (`:555-562`).
  2. Otherwise the newest posted notification-sourced spec wins. A one-off alert covers a live activity (today's behaviour, reversed later by A2.3).
  3. When the shown entry is removed or dismissed, the newest remaining *live* entry returns. This replaces `restoreStashedLive` and is the bug fix.
  4. The progress slot is the most recently updated `PROGRESS` spec.
- **Steps:**
  1. Keep the old `MutableStateFlow`s, but assign them only from the projection after each store change.
  2. Move the dismissed-text check (`:552`) into `ActivityStore.dismiss` and the signature.
  3. Delete the stash field and its 6 uses.
  4. `isIncomingCallRinging` scans the store for `CALL_RINGING`.
- **Acceptance criteria:**
  - `IslandStateBehaviorTest` passes **unchanged**.
  - Ongoing call → timer → message, then dismiss the message: the timer shows, then the call once the timer ends. Neither is lost.
  - Two Play Store downloads: removing the newer one shows the older.
  - Device: start a Google Clock timer, then receive a WhatsApp message, then get a second timer; after dismissing the message, both timers are still reachable.
- **Tests:** `LegacyProjectionTest`, with the 4 rules plus the "third activity" and "two downloads" cases.
- **Depends on:** A1.3.
- **Effort:** M   **Risk:** med — every card path flows through here. Mitigation: the existing behaviour test is the gate, S1 runs before and after, and the change ships alone.
- **Ships as:** **Feature (bug fix)** — activities are no longer lost. The rest is unchanged.

### A1.5 — Generic slot renderers (parity only)
- **Goal:** A `SlotView` composable draws any `Slot` for the compact leading, compact trailing and minimal places using **today's** components, pixel-identical.
- **Why:** 04 §5.2, "renderers draw a slot generically and keep the current bespoke rows as custom slot implementations".
- **Files:**
  - New: `island/activity/SlotViews.kt`
  - Reused:
    - `RollingNumberText` (`LiveActivities.kt:399`), `TimerRingIcon` (`:1227`), `LiveWaveform` (`:569`)
    - `TimerClockIcon` (`IslandComponents.kt:748`), `NotificationAvatar` (`:670`), `FlippingCoverArt` (`:516`), `rememberLiveTimerText` (`:403`)
    - `PulsingRecordingDot` (`IslandView.kt:4143`), `EqualizerVisualizer` (`:4170`), `CompactContactAvatar` (`:2629`; make it `internal`)
- **API sketch:**
  ```kotlin
  enum class SlotPlace { COMPACT_LEADING, COMPACT_TRAILING, MINIMAL }
  @Composable fun SlotView(slot: Slot, spec: LiveActivitySpec, place: SlotPlace, isCompactDual: Boolean, modifier: Modifier = Modifier)
  // Slot.Custom(key) -> customSlotRenderers[key](spec.payload, place)  (MEDIA_ART -> FlippingCoverArt, EQUALIZER -> EqualizerVisualizer …)
  // Slot.Icon(icon, effect) -> V7 icon; when I1 exists: DynamicIcon(icon, effect) (no effect design here)
  ```
- **Steps:**
  1. Add a `rememberLiveTimerText(baseMs, countDown)` overload next to `IslandComponents.kt:403`.
  2. Implement each `Slot` case with the sizes used today (for example 18 dp icons at `LiveActivities.kt:1265-1296`, `TypeCaption` text at `IslandView.kt:2881`). Read sizes from F2 tokens where they exist.
  3. Add a Robolectric screenshot (S2 harness) per slot type.
- **Acceptance criteria:** S2 diff = 0 for each slot type rendered through `SlotView` compared with the legacy branch it replaces.
- **Tests:** `SlotViewScreenshotTest` (S2 harness), one case per `Slot` subtype × place.
- **Depends on:** A1.2, S2, F2.
- **Effort:** M   **Risk:** low.
- **Ships as:** Plumbing.

### A1.6 — Compact and bubble for notification and progress kinds render from slots
- **Goal:** The `POP_NOTIFICATION` and `PROGRESS` branches of the pill and the split bubble draw `spec.compactLeading`, `compactTrailing` and `minimal` through `SlotView`.
- **Why:** This retires 4 hand-written `when`s (04 §3 data model). The stable `spec.id` becomes M3's `AnimatedContent` content key (03 §183).
- **Files:** `island/IslandView.kt`:
  - `AuxiliaryBubbleContent` notification branch :2477-2509, progress branch :2510-2540
  - `CompactContent` leading notification :2715-2741, leading progress :2743-2773, trailing notification :2888-2904, trailing progress :2906-2926
  - the call sites that pass `notification` into those functions
- **Steps:**
  1. Pass the projected `LiveActivitySpec` alongside the existing `NotificationInfo`.
  2. Replace each branch body with `SlotView(spec.compactLeading …)` and so on.
  3. Keep `rememberLastNonNull` semantics (`:2669`, `:2472`) on the spec.
  4. Expose `spec.id.value` as the content key that M3 will consume.
- **Acceptance criteria:**
  - S2 is identical for: call, timer, stopwatch, recorder, alert, alarm, tracking, score, message with avatar, message with icon only, download determinate and indeterminate, and each of those in the split bubble.
  - Device: Google Clock timer ring, WhatsApp avatar and a Play download look unchanged.
- **Tests:** S2 screenshots, plus S1 content assertions.
- **Depends on:** A1.4, A1.5.
- **Effort:** M   **Risk:** med (contention with the M3 and V3 tracks). Mitigation: land it before V3, or rebase V3 onto the slots.
- **Ships as:** Plumbing.

### A1.7 — Media, flashlight and screen recording become store entries
- **Goal:** Non-notification sources are specs too, so A2 can rank everything in one place.
- **Why:** Arbitration needs a single list (04 §5.3). Today these are separate flows ranked by hand at `IslandView.kt:489-539`.
- **Files:**
  - `island/IslandStateManager.kt`: `updateMedia` :473-508 (the dismissed-package logic :481-493 maps to `store.dismiss` with signature = `isPlaying`), `dismissMedia` :510-518, flashlight :341-349 and :406-412, recording :706-713, `isMediaActive` :296-302
  - `island/IslandView.kt`: compact media :2775-2792 and :2928-2937, flashlight :2687-2713, recording :2683-2685 and :2875-2884; bubble :2474-2476, :2541-2582
- **Steps:**
  1. Map each source with `LegacySpecMapper`: `MEDIA_ART` and `EQUALIZER` as `Slot.Custom`; flashlight `FLASHLIGHT_GLOW`; recording `RecordingDot` plus `Timer`.
  2. Keep media staleness (`NotificationReceiverService.kt:511`, 10 min) as `staleAt`.
  3. Render these branches through `SlotView`.
- **Acceptance criteria:**
  - S2 is identical for media playing and paused, flashlight, and screen recording, both as primary and in the bubble.
  - "Dismiss media on pause" still works.
  - A swiped-away paused session returns only when it plays again (`IslandStateManager.kt:481-493` behaviour).
- **Tests:** extend `ActivityStoreTest` with media dismissal versus resume; S2.
- **Depends on:** A1.6.
- **Effort:** M   **Risk:** med — media is the most common state. Mitigation: S1 media cases and a device check with Spotify and YouTube.
- **Ships as:** Plumbing.

### A1.8 — Expanded card, height, open target and outline from the spec
- **Goal:** The expanded content, card height, tap and long-press target, and outline accent are chosen from `spec.expanded.presentation`, `spec.open` and `spec.accent`. The `IslandPriority` switch is gone from these paths.
- **Why:** These are the other hand-written `when`s (04 §3): `ExpandedCardContent`, `cardHeightFor`, `openAppForPriority` and the outline colours.
- **Files:**
  - `island/IslandView.kt`: `ExpandedCardContent` :2973-3028, `ExpandedNotificationView` live dispatch :3666-3674, `openAppForPriority` :2388-2415, outline colours :541-567
  - `island/IslandComponents.kt`: `cardHeightFor` :211-230, `measureNotificationCardHeight` :342-355, `measureProgressCardHeight` :313-328
  - `island/LiveActivities.kt`: `LiveActivityNotificationView` :835-903, `liveActivityCardHeight` :906-909
- **Steps:**
  1. Map `ExpandedPresentation` to the existing composables:
     - CALL → `LiveCallView`
     - ALERT → `LiveAlertView`
     - TIMER_ROW → `LiveTimerRow`
     - RECORDING_ROW → `LiveRecordingRow` / `ExpandedRecordingView`
     - TRACKING_ROW → `LiveTrackingRow`
     - SCORE_ROW → `LiveScoreRow`
     - PROGRESS_CARD → `ExpandedProgressView`
     - NOTIFICATION_CARD → `ExpandedNotificationView`
     - MEDIA_CARD → `ExpandedMediaView`
     - FLASHLIGHT_CARD → `ExpandedFlashlightView`
     - WEATHER / NETWORK → the existing widget views
  2. Write a `heightFor(spec, …)` that delegates to the existing measure functions. V4 later swaps in measured heights.
  3. `spec.open` encodes the B6 order: the notification's own intent first, the launcher second.
- **Acceptance criteria:**
  - S2 is identical for every expanded state.
  - S1 card bounds are unchanged.
  - Long-press opens the same target as after B6.
- **Tests:** S1 bounds matrix; `OpenTargetTest`, pure.
- **Depends on:** A1.7, B6.
- **Effort:** M   **Risk:** med — the height code feeds the window. Mitigation: S3 window-math tests.
- **Ships as:** Plumbing.

### A1.9 — Delete the legacy single-slot state
- **Goal:** Remove these:
  - `_activeNotification` and `_activeProgress`
  - the projection
  - `isNotificationActive`, `isProgressActive` and `isOccupied` getters (`IslandStateManager.kt:289-323`)
  - `LegacySpecMapper.fromNotification`, which A3.1's adapters replace
  - `IslandPriority` in presentation code

  Keep `IslandPriority` only if F1's scene still needs a bucket name.
- **Why:** Two sources of truth were 04 P1 #12 and 06 P1-8.
- **Files:**
  - `island/IslandStateManager.kt` :113-117, :289-339
  - `service/IslandOverlayService.kt`: combine :751-787, `updateWindowState` :944-960 (reads the scene, via F1/F5), :618, :1088 (SpokenAlerts takes the arbitration's T0 instead)
  - `service/NotificationReceiverService.kt` :260 (reply looks up `store.get(ActivityId("n:$key"))`), :1124-1132
  - `island/LiveActivityTester.kt:133-139`
- **Acceptance criteria:**
  - `grep _activeNotification|activeProgress|stashedLive|currentPriority` is empty.
  - The full suite and S1/S2 are green.
  - Inline reply still works on WhatsApp (device).
- **Tests:** existing suites; `IslandStateBehaviorTest` rewritten to assert on `store` and `arbitration` (same scenarios).
- **Depends on:** A2.2, A3.1.
- **Effort:** M   **Risk:** med. Mitigation: a pure deletion PR, with no behaviour change allowed.
- **Ships as:** Plumbing.

---

## A2 — Tiered relevance arbiter (T0–T5)

### A2.1 — Pure `arbitrate()` with the tier table
- **Goal:** A side-effect-free function from the store (plus context) to primary, secondary, overflow and reasons. It is fully table-tested before anything uses it.
- **Why:**
  - 04 §5.3.
  - Today's order lives in the view (`IslandView.kt:489-539`), and a second, contradicting order sits in `IslandStateManager.currentPriority` (`:326-336`), which `expand()` uses at `:716`.
  - Ringing alarms are not top tier (04 P1 #12).
- **Files:** new `island/activity/Arbiter.kt`, `island/activity/TierTable.kt`.
- **Tier table:**

  | Tier | Members | Rule | Today |
  |---|---|---|---|
  | **T0** Interrupt | ringing call (CallStyle type 1, or `CATEGORY_CALL` + answer), ringing alarm, timer "Time's up" | Always primary. Auto-expands. Shows over the foreground app and in fullscreen (A6). Blocks T4. A sideways swipe tucks it instead of dismissing (B6). | `isIncomingCallRinging` `IslandStateManager.kt:229`; protection at `:555-562` |
  | **T1** Live | ongoing call, navigation, voice or screen recording, timer and stopwatch (**running or paused**, so placement doesn't jump), Live Update with ETA or chip, user-pinned "Live" | Never covered by T3. Primary when there is no T0. | mixed into `_activeNotification` |
  | **T2** Ongoing | media, downloads, live score, flashlight, VPN/hotspot (opt-in), privacy pill, snoozed alarm, Live Update without ETA, pinned (default) | Ordered by the user's drag order | `featurePriorities` |
  | **T3** Banner | messages (importance ≥ DEFAULT), missed call | Peeks for `IslandTimings.peek` (F3, about 4.5 s): primary only if nothing ≥ T2 exists, otherwise secondary. Then recedes to its rank, possibly into overflow (A2.3). | pop-in at `IslandView.kt:527` |
  | **T4** Glance (overlay lane, not a rank) | charging/low/full, Bluetooth/Wi-Fi connect, ringer, DND, unlock, VPN on/off, next alarm | Overlays the compact pill for its TTL and suppresses the split. Never over T0. A newer glance replaces one of the same kind. | `trigger*` at `IslandStateManager.kt:414-466` |
  | **T5** Ambient | weather, network speed | Only when no T0–T3 activity is visible | `persistentList` at `IslandView.kt:480-486` |

  Order within a tier:
  1. per-app override (A2.5)
  2. user bucket order
  3. `lastSignificantAt`, newest first
  4. `startedAt`, oldest first
  5. `id`

  Visibility filters:
  - Hidden while its app is in the foreground, except T0 (`IslandView.kt:460-465`).
  - Fullscreen policy (A6).
  - Lock-screen `SECRET` (A5).
  - User-minimised or hidden.
- **API sketch:**
  ```kotlin
  data class ArbiterInput(
      val activities: Collection<LiveActivitySpec>,
      val now: Long,
      val bucketOrder: List<OrderBucket>,          // from settings.featurePriorities
      val priorityApps: Set<String>,               // promoted to the head of T2, never above T1
      val tierOverrides: Map<String, Tier> = emptyMap(),  // A2.5
      val foregroundPackage: String?,
      val fullscreen: FullscreenGate = FullscreenGate.NotFullscreen,   // A6
      val lock: LockContext = LockContext.Unlocked,                    // A5
      val peek: Peek? = null,                      // (id, untilMs) for the current T3 pop-in
  )
  data class Arbitration(
      val primary: LiveActivitySpec?,
      val secondary: LiveActivitySpec?,
      val overflow: List<LiveActivitySpec>,        // "+N" = overflow.size (A2.4)
      val glance: LiveActivitySpec?,               // T4 overlay currently on top
      val autoExpand: ActivityId?,                 // new T0, or alerting update
      val reasons: Map<ActivityId, Reason>,        // A2.5 "why primary?"
  )
  data class Reason(val tier: Tier, val rule: String)  // e.g. "T1 · live · started 10:42", "hidden · app on screen"
  fun arbitrate(input: ArbiterInput): Arbitration
  ```
- **Steps:**
  1. Implement the function.
  2. Write the table-driven tests.
  3. Write down every case where it differs from today's `IslandView` order. Ringing alarm over media is the intended one.
- **Acceptance criteria:**
  - Pure: no Android imports beyond `PendingIntent` types.
  - 100% branch coverage of the tier rules.
  - Deterministic for equal inputs.
- **Tests:** `ArbiterTest`:
  - call + media
  - timer + message (the message never becomes primary)
  - 2 timers (newest first)
  - 2 downloads
  - ringing alarm + media
  - glance over T1 but not over T0
  - foreground hide except T0
  - priority app head-of-T2
  - 5 activities give primary + secondary + overflow 3
  - persistent widgets only when empty
- **Depends on:** A1.2.
- **Effort:** M   **Risk:** low — pure.
- **Ships as:** Plumbing.

### A2.2 — The scene uses the arbiter; delete the two old orderings
- **Goal:** F1's `deriveIslandScene` calls `arbitrate()` for primary, secondary and glance. `IslandStateManager.currentPriority` and the view's ordering block are deleted, and `expand()` targets `arbitration.primary`.
- **Why:** One source of arbitration truth (PLAN F1; 04 P1 #12).
- **Files:**
  - F1's scene file
  - `island/IslandView.kt:489-539`
  - `island/IslandStateManager.kt:326-339`, `:715-723`
  - `model/IslandSettings.kt:3-9` (add the `LIVE` and `SYSTEM` buckets)
  - `data/SettingsRepository.kt:122-128` and `:187-193` (the merge appends new keys)
  - `ui/main/MainScreen.kt:642`, `:656` (the hard-coded `valid` lists), plus labels and icons at `:715-735`, `:835`
- **Steps:**
  1. Map the stored bucket keys to `OrderBucket`, and append `LIVE` and `SYSTEM` for existing users.
  2. Show the two new rows in the existing drag list. This adds rows, not a new layout.
  3. Replace the scene's ordering with `arbitrate()`.
  4. Delete `currentPriority` and `activeExpandedPriority`, unless F1 already did.
- **Acceptance criteria:**
  - S1 diffs are only the documented intended changes from A2.1.
  - Device: a Google Clock alarm ringing while Spotify plays → the alarm is primary and expanded.
  - Device: Maps navigation + Spotify → navigation primary, media in the bubble.
- **Tests:** S1 matrix; `SettingsMigrationTest` (an old 5-key list gains `LIVE` and `SYSTEM` at the end).
- **Depends on:** A2.1, A1.8, F1.
- **Effort:** M   **Risk:** med — users see a different order in edge cases. Mitigation: a dev flag `IslandFlags.tieredArbiter` for one release, plus the S1 diff list in the PR.
- **Ships as:** **Feature**: consistent priority, and ringing alarms always come first.

### A2.3 — Message banners recede and never cover a live activity
- **Goal:**
  - A new T3 message peeks, then settles to its rank.
  - It never takes primary while a T1 exists. While T1 is primary, the message pops in as the secondary bubble.
  - "Priority apps" promote a message to the head of T2 but not above T1.
- **Why:**
  - 04 §3: "a one-off message pushes an ongoing call, timer or recorder aside", and because auto-dismiss defaults off, "the message stays until it is swiped away".
  - PLAN §2 "Glanceable alerts".
- **Files:**
  - `island/IslandStateManager.kt`: `triggerPopIn` :660-667 (uses `IslandTimings.peek`), auto-dismiss :604-612
  - the arbiter's `peek` input
  - `app/src/test/java/com/droidbubble/app/island/IslandStateBehaviorTest.kt:137-148` (`answeredCall_letsAlertsThroughAgain` encodes today's "message covers the call" behaviour and **must be rewritten deliberately**)
- **Steps:**
  1. Feed the peek into `ArbiterInput`.
  2. Expose `Arbitration.peek` for D2's alert bump. Motion is not part of this task.
  3. Update the test to assert that the call stays primary and the message is secondary or overflow.
- **Acceptance criteria:**
  - Device: an ongoing WhatsApp call plus a Telegram message → the call stays primary, and the message shows in the bubble for about 4.5 s, then sits as secondary or "+N".
  - Device: with no other activity, the message is primary, as today.
  - Ringing calls are still never covered.
- **Tests:** `ArbiterTest` peek cases; the rewritten `IslandStateBehaviorTest` case.
- **Depends on:** A2.2, F3 (`IslandTimings`).
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** no new visuals. It reuses the split bubble, and D2 owns the bump.

### A2.4 — "+N" overflow instead of silently dropping activities
- **Goal:**
  - When `overflow` isn't empty, the split bubble shows a small "+N" counter.
  - Tapping the counter cycles which activity is secondary.
  - After M4, a swipe on the bubble does the same.
  - TalkBack hears "N more activities".
- **Why:** 04 P2 #13: "A 3rd, 4th or 5th activity is hidden with no indication"; 04 §6 "+N instead of silently dropping".
- **Files:**
  - `island/IslandView.kt` (the bubble container that hosts `AuxiliaryBubbleContent`, `:2462`)
  - the arbiter
  - F5 `WindowSpec` (the counter's hit area for B2)
- **Steps:**
  1. Render the counter with `RollingNumberText` and the existing bubble surface tokens, in a `SquircleShape` capsule on the bubble's outer edge. This is the only new element; it uses F2 tokens and no new hue.
  2. Keep a `secondaryRotation` index in the store's UI state.
  3. Add semantics: a label and an "Next activity" custom action (C3).
- **Acceptance criteria:**
  - Device: a timer + recorder + download + Spotify shows primary + bubble + "+2"; tapping "+2" rotates the bubble.
  - The counter rolls when N changes.
  - No layout shift of the pill.
  - S2 has a new baseline for the overflow state only.
- **Tests:** `ArbiterTest` rotation; an S2 screenshot of the overflow state; a TalkBack sanity check.
- **Depends on:** A2.2, A1.6; M4 for the swipe follow-up.
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** this is Droidbubble's own rule (iOS drops activities into "minimal"). It reuses the bubble and squircle and adds no new icon.

### A2.5 — "Why is this primary?" and per-app tier overrides *(optional; v2 PLAN dropped the explicit mention)*
- **Goal:**
  - In the Test lab and in the existing Priority Apps sheet, show `Reason.rule` for each active activity.
  - Let a user set an app to "Live (T1)", "Normal" or "Quiet (never pops)".
- **Why:** 04 §6 "Transparent arbitration … with per-app tier overrides".
- **Files:**
  - `model/IslandSettings.kt` (`tierOverrides: Map<String,String>` stored as a string set of `pkg=T1`)
  - `data/SettingsRepository.kt` (both mappers, `:69-131` and `:133-195`)
  - `ui/main/MainScreen.kt` (the Priority Apps sheet, `~:1133-1160`)
  - `ui/main/LiveActivityTestPanel.kt`
- **Acceptance criteria:**
  - An app set to Live outranks media.
  - A Quiet app's messages never peek.
  - The reason text matches the arbiter's.
- **Tests:** `ArbiterTest` overrides; `SettingsMigrationTest`.
- **Depends on:** A2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature** (power users).

### A2.6 — Test lab: stack several demo activities
- **Goal:** `LiveActivityTester` can post several demos at once (call + timer + message + download + media), so A2 and A3 can be checked on a device without real apps.
- **Why:** `show()` calls `clear()` first (`island/LiveActivityTester.kt:54`, `:156-160`), so today only one demo can exist and arbitration is untestable by hand.
- **Files:** `island/LiveActivityTester.kt`, `ui/main/LiveActivityTestPanel.kt`.
- **Acceptance criteria:**
  - A "Stack" toggle keeps the earlier demos.
  - A "Clear all" button.
  - Test keys stay prefixed `droidbubble.test.`.
- **Tests:** a unit test that 3 stacked demos give 3 store entries.
- **Depends on:** A1.4.
- **Effort:** S   **Risk:** low.
- **Ships as:** Dev tool, in the existing Test lab.

---

## A3 — Structured Android adapters

### A3.1 — `NotificationSnapshot`, the adapter chain and the legacy fallback
- **Goal:**
  - NRS turns each `StatusBarNotification` into a pure `NotificationSnapshot` (off-main, B10).
  - An ordered adapter chain turns the snapshot into a `LiveActivitySpec`.
  - Today's mapping (`NotificationReceiverService.kt:926-1122` plus `liveActivityKind`) becomes `LegacyAdapter`, the last in the chain, so behaviour is unchanged.
- **Why:**
  - 06 §4 target architecture ("Adding a new activity type means writing one `ActivityAdapter`").
  - It makes S5 fixtures testable on the JVM.
  - **A deliberate deviation from 06 §4:** adapters produce *data* (slots) and contain no composables, so they stay pure. The composables live in the presentations.
- **Files:**
  - New: `island/activity/adapters/{NotificationSnapshot.kt, SnapshotReader.kt, AdapterRegistry.kt, LegacyAdapter.kt}`
  - Changed: `service/NotificationReceiverService.kt`:
    - `onNotificationPosted` :859-1133 (gates :862-1013 stay in NRS for now)
    - `extractAvatarBitmap` :1187-1197
    - `progressStyleMax` :1217-1221
    - `readCustomViewTimer` :1238-1266
- **API sketch:**
  ```kotlin
  data class NotificationSnapshot(
      val key: String, val packageName: String, val appName: String, val postTime: Long, val sdk: Int,
      val whenMs: Long, val showWhen: Boolean, val usesChronometer: Boolean, val chronometerCountDown: Boolean,
      val category: String?, val template: String?, val channelId: String?, val flags: Int, val importance: Int?,
      val title: String?, val text: String?, val subText: String?, val bigText: String?, val shortCriticalText: String?,
      val progress: Int, val progressMax: Int, val indeterminate: Boolean, val journey: ProgressJourney?,   // A3.4
      val call: CallExtras?, val actions: List<ActionSnapshot>,
      val largeIcon: ImageRef?, val avatar: ImageRef?, val appIcon: ImageRef?, val contentIntent: PendingIntent?,
      val isPromoted: Boolean, val promotable: Boolean?, val requestPromoted: Boolean,
      val isColorized: Boolean, val hasCustomView: Boolean, val customViewTimer: CustomViewTimer?,
      val visibility: Int, val publicTitle: String?, val publicText: String?, val lockscreenOverride: Int?,   // A5.1
  )
  data class ActionSnapshot(val title: String, val intent: PendingIntent?, val semanticAction: Int,
                            val iconResName: String?, val remoteInput: RemoteInputSnapshot?, val role: ControlRole) // A4
  data class CallExtras(val type: Int, val isVideo: Boolean, val person: PersonSnapshot?, val verificationText: String?,
                        val answer: PendingIntent?, val decline: PendingIntent?, val hangUp: PendingIntent?)

  interface NotificationAdapter {
      val name: String                                     // breadcrumbs (B11) + "why" (A2.5)
      fun accepts(n: NotificationSnapshot): Boolean
      fun toSpec(n: NotificationSnapshot, previous: LiveActivitySpec?, ctx: AdapterContext): LiveActivitySpec?
  }
  object AdapterRegistry {
      val chain: List<NotificationAdapter> = listOf(
          CallStyleAdapter, /* A3.2 */ AlertAdapter, NavigationAdapter, /* A3.6 */ LiveUpdateAdapter, /* A3.3 */
          ChronometerAdapter, /* A3.7 */ PinnedAdapter, /* A8.1 */ LegacyAdapter /* today's rules, always last */)
      fun adapt(n: NotificationSnapshot, prev: LiveActivitySpec?, ctx: AdapterContext): LiveActivitySpec? =
          chain.firstNotNullOfOrNull { a ->
              if (!a.accepts(n)) null
              else runCatching { a.toSpec(n, prev, ctx) }.onFailure { CrashLog.breadcrumb("adapter ${a.name}", it) }.getOrNull()
          }
  }
  ```
- **Steps:**
  1. Write `SnapshotReader.read(sbn, ranking, pm)`, with every field in its own `runCatching`.
  2. Make `LegacyAdapter` wrap today's mapping.
  3. Make NRS call `AdapterRegistry.adapt` and `store.upsert`.
  4. Start with only `LegacyAdapter` registered.
- **Acceptance criteria:**
  - Behaviour is identical: S1 and S2 are green.
  - A malformed extra in a fixture (for example `android.progressSegments` holding a String) produces a breadcrumb and a fallback card, not a crash.
- **Tests:** `SnapshotReaderTest` (Robolectric, built from S5 fixtures); `AdapterRegistryTest` (order, crash containment); `LegacyAdapterTest`, which is equivalent to `LegacySpecMapperTest`.
- **Depends on:** A1.3, B10, B11, S5.
- **Effort:** M   **Risk:** med — the NRS refactor. Mitigation: it's the same code moved, and the fixture parity test is the gate.
- **Ships as:** Plumbing.

### A3.2 — CallStyle adapter: screening, video, verification, mute and speaker
- **Goal:**
  - Calls are read from CallStyle extras first.
  - The ongoing-call card gains the app's own mute and speaker buttons.
  - Screening and video calls are right.
  - The ongoing timer works even when `when` is missing.
- **Why:**
  - 04 P1 #7: "no mute/speaker (CallStyle's custom actions), no video badge or verification text, screening calls are wrong".
  - `LiveCallView` ignores `notification.actions` (`island/LiveActivities.kt:625-695`), although NRS keeps up to 3 of them (`NotificationReceiverService.kt:1102`).
- **Files:**
  - New: `island/activity/adapters/CallStyleAdapter.kt`
  - Changed: `island/LiveActivities.kt:625-695`; NRS `:962-964` and `:1036-1053` (move into the adapter)
- **Android hooks (string keys, present from API 31; absent means fallback):**
  - `android.callType` (0 unknown, 1 incoming, 2 ongoing, 3 screening)
  - `android.callIsVideo`
  - `android.callPerson` (`Person`)
  - `android.verificationText`, `android.verificationIcon`
  - `android.answerIntent`, `android.declineIntent`, `android.hangUpIntent`
  - Template `android.app.Notification$CallStyle`
  - Custom actions are `notification.actions`. The system-generated answer, decline and hang-up are *not* in that array; they come from the extras.
  - Roles come from A4.1: `SEMANTIC_ACTION_MUTE` (6) / `SEMANTIC_ACTION_UNMUTE` (7) via `getSemanticAction()` (API 28); speaker by icon name (`*speaker*`, `*volume*`) or the A4.2 table.
- **Steps:**
  1. Type 1 → `CALL_RINGING` (T0). Type 2 → `CALL_ONGOING` (T1). Type 3 → `CALL_ONGOING` with an eyebrow "Screening", keeping hang-up and, if present, the answer intent.
  2. Video: show a video icon in place of the call icon in the compact leading slot (an existing Material symbol at the V7 size).
  3. Verification text goes into the eyebrow line (`LiveEyebrow`).
  4. The ongoing call shows up to 2 extra `LiveRoundButton(style = GLASS)` controls before hang-up, inside the same row and using the `LiveLayout` button weights. The row stays one row. If the width is too small, drop speaker before mute.
  5. Duration: use `when` plus the chronometer if present. Otherwise use `spec.startedAt` captured at the type 1→2 transition.
  6. Mute toggles go through the I2 morph when it exists.
- **Acceptance criteria:**
  - Fixture tests pass for all `call/*` fixtures, including `whatsapp_incoming_ar` (no English titles) and `gphone_screening_en` (Answer still reachable).
  - Device: a Google Phone call shows mute and speaker, and tapping them toggles in the dialer.
  - Device: a WhatsApp video call shows the video icon.
  - Device: a Samsung call in Turkish shows Answer and Decline.
- **Tests:** `CallStyleAdapterTest` (parameterized over the fixtures); S2 baselines for the ongoing call with buttons.
- **Depends on:** A3.1, A4.1.
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** same call row and button weights; no new layout.

### A3.3 — Live Updates: promotion, `shortCriticalText` and the `when` countdown
- **Goal:**
  - Promoted ongoing notifications are recognised from the platform signals.
  - Their compact trailing slot shows `shortCriticalText`, else a `when` countdown ("5 min"), else the progress percentage.
  - Their tier is T1 when they carry an ETA or chip, T2 otherwise.
- **Why:**
  - 04 P1 #8: "`when` countdown … `hasPromotableCharacteristics()` are all ignored".
  - Today, ProgressStyle + ongoing counts as "promoted" (`NotificationReceiverService.kt:936-937`), and `trackingChipText` reads English "N min" (`LiveActivities.kt:199-203`).
- **Files:** new `island/activity/adapters/LiveUpdateAdapter.kt`; NRS `:55-57`, `:935-938`; `island/LiveActivities.kt:199-203`.
- **Android hooks:**
  - `Notification.FLAG_PROMOTED_ONGOING` (0x40000, API 36; the system sets it)
  - `Notification.hasPromotableCharacteristics()` (API 36, `runCatching`)
  - `Notification.EXTRA_REQUEST_PROMOTED_ONGOING` (`android.requestPromotedOngoing`)
  - `android.shortCriticalText` **[M]** for the raw key, which is already used
  - `when` + `EXTRA_SHOW_WHEN`: a `when` in the future gives a countdown
  - Eligibility (developer.android.com live-update): exclude colorized, custom `RemoteViews`, group summaries and `IMPORTANCE_MIN`.
  - Below API 36, accept the B4-hardened ongoing + tracking-package heuristic.
- **Steps:**
  1. Write the adapter, ahead of `LegacyAdapter`.
  2. Add a `Slot.Text(numeric = true)` countdown formatter. It formats minutes with `MeasureFormat` / `RelativeDateTimeFormatter` (locale-aware, API 24) instead of a hard-coded "min".
  3. Pick the `stateSignature` from the status text, excluding the ETA digits, so a dismissed ride doesn't return on every ETA update (04 P2 #17).
- **Acceptance criteria:**
  - `progress/uber_progressstyle_36_en` gives T1 and the chip text.
  - `talabat_ongoing_ar` on API 35 gives a TRACKING spec with an Arabic ETA chip.
  - A dismissed Uber card stays dismissed through ETA ticks and returns on "Driver arrived".
  - Device: a real Android 16 Live Update (Uber or a delivery app; or the AOSP live-update sample) shows the chip that matches the status-bar chip.
- **Tests:** `LiveUpdateAdapterTest` (fixtures; `when` countdown at now+90 s, +5 min and in the past).
- **Depends on:** A3.1, B4.
- **Effort:** M   **Risk:** med — the raw keys [M]. Mitigation: `runCatching` with a heuristic fallback.
- **Ships as:** **Feature**.

### A3.4 — ProgressStyle parser → `ProgressJourney`
- **Goal:** Parse segments (length and colour), points (position and colour), the tracker, start and end icons, and styled-by-progress into a data class. Downloads and trackers use it for max and progress.
- **Why:** 04 §2 ProgressStyle row; 04 P1 #8 ("ProgressStyle points, segment colours, tracker icon … ignored"). Today only the segment-length sum is read (`NotificationReceiverService.kt:1217-1221`).
- **Files:** new `island/activity/adapters/ProgressJourney.kt`; NRS `:926-933`, `:1217-1221`.
- **Android hooks:**
  - Preferred (API 36): `Notification.Builder.recoverBuilder(ctx, n).style as? Notification.ProgressStyle`, then the public getters `progressSegments`, `progressPoints`, `progressTrackerIcon`, `progressStartIcon`, `progressEndIcon`, `isStyledByProgress`, `progress`, `isProgressIndeterminate`. `recoverBuilder` creates a package context, so run it off-main (B10) and inside `runCatching`.
  - Fallback **[M]**, raw keys: `android.progressSegments` (bundles with `length`, `color`, `id`), `android.progressPoints` (bundles with `position`, `color`, `id`), `android.progressTrackerIcon`, `android.progressStartIcon`, `android.progressEndIcon`, `android.styledByProgress`.
- **API sketch:**
  ```kotlin
  data class ProgressJourney(
      val segments: List<Segment>, val points: List<Point>, val progress: Int, val max: Int,
      val indeterminate: Boolean, val styledByProgress: Boolean,
      val tracker: ImageRef?, val start: ImageRef?, val end: ImageRef?,
  ) { data class Segment(val length: Int, val argb: Int?); data class Point(val position: Int, val argb: Int?) }
  ```
- **Acceptance criteria:**
  - The `uber_progressstyle_36_en` fixture parses 3 segments and 2 points.
  - A malformed bundle gives `null` and a breadcrumb.
  - Downloads keep their percentage.
- **Tests:** `ProgressJourneyParserTest` (Robolectric Bundles on the raw-key path; the `recoverBuilder` path is checked on a device).
- **Depends on:** A3.1.
- **Effort:** S   **Risk:** med — the [M] keys. Mitigation: the public-API path first, with guards.
- **Ships as:** Plumbing.

### A3.5 — Journey bar in the existing tracking row and progress card
- **Goal:** Draw a `ProgressJourney` (coloured segments, milestone dots, tracker icon riding the bar) in place of the single 4 dp bar in `LiveTrackingRow` and the bar in `ExpandedProgressView`. The position and height stay the same.
- **Why:**
  - 04 §6: "A full renderer for Live Updates … the whole ProgressStyle journey … on every app screen, on non-Samsung phones".
  - v1 had this in A8; v2 folds it into A3 ("segments, points").
- **Files:**
  - `island/LiveActivities.kt:1108-1125` (the bar inside `LiveTrackingRow`, `:1058-1139`)
  - `island/IslandView.kt:4051-4072` (the `ExpandedProgressView` bar)
  - New: `island/activity/JourneyBar.kt`
- **Steps:**
  1. One `Canvas` with `drawWithCache`.
  2. Segment colours come from the app, clamped for contrast on black by V5's rule. The track uses `LiveActivityColors.Glass`. Points are drawn as `margin`-concentric dots.
  3. The tracker icon is ≤ 14 dp, centred on the progress position.
  4. Progress uses the existing spring (`:1063-1066`).
  5. With no journey, render the old bar exactly.
- **Acceptance criteria:**
  - S2 is unchanged for cards without a journey.
  - The new baseline shows Uber-style 3 segments + 2 points + a car tracker.
  - Device: Android 16 Uber or Talabat (or the AOSP ProgressStyle sample) matches the segment colours of the system notification.
  - The card height grows by ≤ 8 dp, measured (V4).
- **Tests:** S2 cases (no journey / segments / points / tracker); `JourneyBarGeometryTest` (pure position math, RTL mirrored).
- **Depends on:** A3.4, A1.8; V5 for the colour clamp (temporary clamp until then).
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** same row, same bar slot, no new card. Droidbubble's own: the system chip can't show a journey.

### A3.6 — Navigation adapter (maneuver arrow, distance, instruction)
- **Goal:**
  - Navigation is its own T1 kind.
  - Compact: maneuver arrow on the leading side, distance (or `shortCriticalText`) on the trailing side.
  - Expanded: the instruction, with the ETA as a sub-line.
  - It is no longer forced into an indeterminate "Downloading" PROGRESS card.
- **Why:**
  - 04 P1 #6 "Navigation looks like a download".
  - NRS forces it to progress (`:950-953`, `:1106`).
  - The card title defaults to "Downloading" (`IslandView.kt:3996`).
- **Correction to 04 §3:** the turn arrow is *not* lost. The large icon is already captured as `avatarBitmap` (`NotificationReceiverService.kt:1034`, `:1194-1196`); only the PROGRESS presentation never draws it (`IslandView.kt:2745-2760`, `:4013-4026`).
- **Files:**
  - New: `island/activity/adapters/NavigationAdapter.kt`
  - NRS `:950-953`, `:1106`
  - `island/LiveActivities.kt` (reuse the `LiveTrackingRow` layout as `NAVIGATION_ROW`: avatar slot = arrow bitmap, title = instruction, chip = distance)
- **Android hooks:**
  - `CATEGORY_NAVIGATION`, or package `com.google.android.apps.maps` / `com.waze`, plus ongoing.
  - Before 16: title = distance, text = instruction, large icon = arrow, sub-text or `when` = ETA **[M]**.
  - On 16 QPR: a promoted Live Update with `shortCriticalText` (time-to-turn) and ProgressStyle **[M]** (staged rollout).
  - Distance strings are locale-formatted (for example "٢٠٠ م"), so normalise digits via A4.2.
- **Acceptance criteria:**
  - `nav/gmaps_turn_en` and `nav/gmaps_turn_ar` → arrow + "200 m" + "Turn left onto …".
  - `gmaps_liveupdate_36_en` → chip from `shortCriticalText`.
  - Device: Google Maps driving nav (Pixel + Samsung) shows the arrow updating at each turn, and the T1 tier keeps it above Spotify.
  - Device: Waze shows the same.
- **Tests:** `NavigationAdapterTest` (fixtures, RTL).
- **Depends on:** A3.1, A3.3, A1.8.
- **Effort:** M   **Risk:** med — Maps formats vary by version and OEM. Mitigation: fixtures per version, and fall back to the tracking row.
- **Ships as:** **Feature**.
- **Design check:** reuses the tracking row; the arrow is the app's own bitmap.

### A3.7 — Chronometer and timer reader extraction
- **Goal:** Move the timer-base logic into a pure `ChronometerReader` used by `ChronometerAdapter`, and add countdown overtime (a `when` in the past with count-down set). The logic is today in:
  - the chronometer, custom-view and title-minus-postTime paths (`NotificationReceiverService.kt:1056-1074`)
  - the custom-view inflation (`:1238-1266`)
  - the custom-timer gate (`:970-979`)
- **Why:** 04 §5.4 "Chronometer: move the existing logic out of NRS". It's a precondition for A4.3's locale-independent pause detection (`:1061`, `:1258-1262`).
- **Files:** NRS lines above; `model/IslandEvent.kt:96-143` (`parseTimerTextMillis`, `LiveTimerBaseStabilizer`, which stay); new `island/activity/adapters/ChronometerReader.kt`.
- **Android hooks:**
  - `EXTRA_SHOW_CHRONOMETER` and `EXTRA_CHRONOMETER_COUNT_DOWN` (API 24), `when`
  - `RemoteViews.apply` → `Chronometer.isCountDown` (API 24), `base` (off-main per B10; catch `RuntimeException` on inflation)
  - `CATEGORY_STOPWATCH` (API 31) as a structured stopwatch signal ahead of the word "stopwatch" (`LiveActivities.kt:240`)
- **Acceptance criteria:**
  - Identical bases for all `clock/*` and `recorder/*` fixtures.
  - `LiveTimerBaseStabilizerTest` is unchanged and green.
  - Overtime shows "−0:12" style elapsed time after "Time's up", if the app keeps posting.
- **Tests:** `ChronometerReaderTest` (fixtures, including Arabic-Indic digits after A4.2 normalisation).
- **Depends on:** A3.1 (it can be written against today's NRS first, and only registered after A3.1).
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.

---

## A4 — Locale-independent control mapping

### A4.1 — `ControlRole` resolver (structured → semantic → icon name)
- **Goal:** Each `NotificationAction` gets a `role`, resolved in this order:
  - (a) structured extras (CallStyle intents, alert category)
  - (b) `Action.getSemanticAction()`
  - (c) the action icon's resource entry name in the posting package
  - (d) the A4.2 locale tables
- **Why:** 04 P0-3, 04 §5.5.
- **Files:** new `island/activity/controls/{ControlRole.kt, ControlResolver.kt}`; `model/IslandEvent.kt:39-47` (add `role`, `semanticAction`, `iconResName` to `NotificationAction`); NRS `:1076-1091` (fill them in).
- **API sketch:**
  ```kotlin
  enum class ControlRole { ANSWER, DECLINE, HANG_UP, MUTE, UNMUTE, SPEAKER, PAUSE, RESUME, STOP, CANCEL,
                           SNOOZE, ADD_TIME, DISMISS, REPLY, MARK_READ, OPEN, UNKNOWN }
  object ControlResolver {
      fun resolve(a: ActionSnapshot, ctx: ResolveContext /* kind hint, locale tables */): ControlRole
  }
  // (b) SEMANTIC_ACTION_REPLY=1, MARK_AS_READ=2, DELETE=4, MUTE=6, UNMUTE=7, CALL=10 (API 28+)
  // (c) icon: a.icon?.takeIf { it.type == Icon.TYPE_RESOURCE }?.let { pm.getResourcesForApplication(it.resPackage).getResourceEntryName(it.resId) }
  //     tokens: pause, play, resume, stop, close|cancel|clear, snooze|alarm_off, add|plus, mic_off|mute, volume|speaker, call_end|hangup, reply
  ```
- **Steps:**
  1. Write the resolver as a pure function over `ActionSnapshot`.
  2. Read icon names in `SnapshotReader` (off-main, `runCatching`, cached per package and resId).
- **Acceptance criteria:** Every action in the S5 `call`, `clock` and `recorder` fixtures resolves to the right role without using the action title, wherever the fixture has a semantic action or a resource icon.
- **Tests:** `ControlResolverTest`, parameterized.
- **Depends on:** S5 (and A3.1 for wiring the snapshot; the resolver itself is independent).
- **Effort:** M   **Risk:** med — OEM icon names vary [M]. Mitigation: it's only step (c) of four, and the titles in (d) remain a fallback.
- **Ships as:** Plumbing.

### A4.2 — Per-locale tables and digit normalisation
- **Goal:**
  - Regex tables for en, ar, es, pt, fr, de, tr, ru, hi, id and zh for each `ControlRole`, plus state words ("paused", "time's up", "screen recording", "stopwatch", "alarm", "snooze").
  - Unicode-safe boundaries.
  - Arabic-Indic (U+0660–0669) and Persian (U+06F0–06F9) digits normalised to ASCII before any time, ETA or score parsing.
- **Why:**
  - 04 P0-3; the package hints suggest many MENA users (`LiveActivities.kt:131-135`).
  - `\b` and `\d` behave differently on the JVM (ASCII) and on Android's ICU regex (Unicode) **[M]**, so test results today don't predict device behaviour for non-Latin text. The affected regexes are at `LiveActivities.kt:148-156` and `model/IslandEvent.kt:96`.
- **Files:** new `island/activity/controls/LocaleTables.kt`, `island/activity/text/Digits.kt`; `model/IslandEvent.kt:96-104`; `island/LiveActivities.kt:148-156`.
- **Steps:**
  1. Use explicit boundaries `(?<![\p{L}\p{N}])…(?![\p{L}\p{N}])` instead of `\b`, and `[0-9]` after normalisation instead of `\d`.
  2. Normalise Arabic before matching: strip tashkeel and unify alef forms.
  3. CJK tokens are matched as exact substrings.
  4. **Every token must be backed by a captured S5 fixture**; don't add guessed translations. Seed list, to verify against fixtures:
     - pause: pausar, pause/pausieren, duraklat, пауза, إيقاف مؤقت, jeda, 暂停
     - resume: reanudar, retomar, reprendre, fortsetzen, devam, продолжить, استئناف, lanjutkan, 继续
     - stop: detener, parar, arrêter, beenden, durdur, остановить, إيقاف, hentikan, 停止
     - answer: contestar, atender, répondre, annehmen, cevapla, ответить, رد, jawab, 接听
     - decline: rechazar, recusar, refuser, ablehnen, reddet, отклонить, رفض, tolak, 拒绝
     - hang up: colgar, desligar, raccrocher, auflegen, завершить, إنهاء, akhiri, 挂断
     - snooze: posponer, adiar, répéter, schlummern, ertele, отложить, غفوة, tunda, 稍后提醒
- **Acceptance criteria:** All `*_ar`, `*_es`, `*_de`, `*_fr`, `*_tr`, `*_ru`, `*_zh` and `*_id` fixtures resolve their roles. JVM and Robolectric give the same results.
- **Tests:** `LocaleTablesTest` (parameterized by locale × role); `DigitsTest` ("٠٠:٤٢" → 42 000 ms; "٢٠٠ م" → 200 m).
- **Depends on:** A4.1, S5 (multi-locale fixtures).
- **Effort:** M   **Risk:** med — false positives. Mitigation: the tables are only step (d), and the fixtures include negative cases ("Send", "Weekend", "Ende").
- **Ships as:** Plumbing.

### A4.3 — Wire roles into every control consumer
- **Goal:** Controls are picked by role everywhere. The English regexes stay only as the `en` table. The consumers are:
  - `liveActivityControls` (`LiveActivities.kt:277-283`)
  - `alertControls` (`:214-219`)
  - call intents (NRS `:1044-1051`)
  - screen-recording actions (NRS `:768-781`)
  - custom-view running detection (NRS `:1258-1262`)
  - `isTimerPaused` (NRS `:1061`)
  - the "paused" text check (`LiveActivities.kt:280-281`)
- **Why:** On non-English phones, "incoming calls without CallStyle lose Answer, recorders and timers lose their controls" (04 P0-3).
- **Files:** the lines above; `island/LiveActivityTester.kt:163-190` (fake actions get roles).
- **Acceptance criteria:**
  - Device with the system language set to Arabic, then Spanish:
    - Google Clock timer shows Pause and Cancel working
    - Samsung Voice Recorder shows Pause and Stop
    - an alarm shows Snooze and Stop
    - a non-CallStyle VoIP call shows Answer
  - The English fixtures are unchanged.
- **Tests:** `LiveActivityKindTest` extended with locale fixtures; `ControlsMappingTest`.
- **Depends on:** A4.2, A3.7.
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature** (non-English users).

### A4.4 — Structured signals for kind and state, ahead of words
- **Goal:** Kind and state detection prefers categories and flags, then falls back to A4.2 words:
  - `CATEGORY_ALARM` + `fullScreenIntent` for alerts
  - `CATEGORY_STOPWATCH`, `CATEGORY_CALL`, `CATEGORY_NAVIGATION`, `CATEGORY_PROGRESS`, `CATEGORY_WORKOUT`
  - the chronometer count-down flag
  - the call type
  - `CATEGORY_MISSED_CALL` (missed call → T3, not a call)

  This covers "time's up" (NRS `:941-944`; `LiveActivities.kt:206-209`), screen-recording text (NRS `:728-736`, `:749-756`) and the kind words (`LiveActivities.kt:238-249`).
- **Why:** 04 P0-3 and P2 #14. The bare title clauses at NRS `:751-756` accept any ongoing notification titled "screen recorder".
- **Files:** the lines above; `island/activity/adapters/AlertAdapter.kt`.
- **Acceptance criteria:**
  - `screenrec/samsung_es` and `xiaomi_id` are detected.
  - An idle third-party recorder ("Ready to record") is not.
  - `clock/gclock_timesup_fr` gives ALERT.
- **Tests:** fixture tests per locale.
- **Depends on:** A4.2, B4.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature**.

---

## A5 — Lock-screen privacy

### A5.1 — Lock context and visibility in the model
- **Goal:** Every spec carries a `PrivacySpec`, and the arbiter receives a `LockContext`. `PrivacySpec` contains:
  - the notification's `visibility`
  - its `publicVersion` title and text
  - the channel's lock-screen visibility
  - the ranking's `lockscreenVisibilityOverride`
- **Why:** 04 P1 #9; 05 §2 "Full text and reply controls appear over the keyguard".
- **Files:** `SnapshotReader` (A3.1); new `island/activity/privacy/LockContext.kt`; `service/IslandOverlayService.kt:300-318` (the screen on/off and user-present receivers feed the keyguard state).
- **Android hooks:**
  - `Notification.visibility` (`VISIBILITY_PUBLIC` 1, `PRIVATE` 0, `SECRET` −1) and `Notification.publicVersion`
  - `NotificationChannel.getLockscreenVisibility()` (API 26)
  - `Ranking.getLockscreenVisibilityOverride()`
  - `KeyguardManager.isKeyguardLocked` / `isDeviceLocked`, polled on `ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF` and `ACTION_USER_PRESENT`. Don't use `addKeyguardLockedStateListener`: its permission isn't available to us.
  - `Settings.Secure` `lock_screen_show_notifications` and `lock_screen_allow_private_notifications` **[M]** (hidden key names, read with `runCatching`, through a `ContentObserver`)
- **Acceptance criteria:** The `gmail_private_en` fixture yields `PRIVATE` with public text. The lock state flips correctly through screen-off, screen-on and unlock.
- **Tests:** `LockContextTest` (state machine); a snapshot fixture test.
- **Depends on:** A3.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.

### A5.2 — Redaction policy and rendering while locked
- **Goal:** While locked:
  - `SECRET` and "hide all notifications" → hidden.
  - `PRIVATE` or "hide sensitive content" → `publicVersion`, or "⟨App⟩ · New notification".
  - Reply, smart-reply chips and action buttons are hidden for T3 banners.
  - T0 and T1 live activities keep showing, following their own visibility (calls, timers and navigation are usually public), with controls kept only for calls and alarms.
  - A tap sends the content intent, and the system asks for unlock.
- **Why:** 04 §5.7; PLAN §4 A5; 05 R8 ("hide reply and actions").
- **Files:** new `island/activity/privacy/Redaction.kt` (pure `fun redact(spec, lock): LiveActivitySpec?`), applied in the arbiter's input; `island/IslandView.kt` `ExpandedNotificationView` reply and action rows (`:3706` reply action and the action row below it).
- **Acceptance criteria:**
  - Device (Pixel and Samsung), with the lock screen set to "Show sensitive content only when unlocked": a WhatsApp message on the locked screen shows "WhatsApp · New notification" with no reply. After unlock it shows the full text.
  - A Google Clock timer still shows and works while locked.
  - TalkBack reads the redacted text only.
- **Tests:** `RedactionTest` (visibility × lock-screen setting × tier); S2 locked-message baseline.
- **Depends on:** A5.1, A2.2.
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** same card; only the content changes.

---

## A6 — Fullscreen and immersive policy

### A6.1 — Fullscreen detection
- **Goal:** `env.fullscreen` in F1's scene is true when the status bar is hidden, or the foreground app is a game or the default camera.
- **Why:** 05 P0-2 "The island covers portrait video, games and camera apps and stays touchable"; 05 R3.
- **Files:** `service/IslandOverlayService.kt` (the root view of the overlay window, near the `ComposeView` setup `~:674-724`); `service/IslandAccessibilityService.kt:26-38` (foreground package); `AndroidManifest.xml:25-37` (`<queries>`).
- **Android hooks:**
  - `View.setOnApplyWindowInsetsListener` → `WindowInsets.isVisible(WindowInsets.Type.statusBars())` (API 30). Below 30, use the deprecated `View.setOnSystemUiVisibilityChangeListener`, guarded. **[M]** whether overlay windows receive status-bar visibility on all OEMs.
  - `ApplicationInfo.category == CATEGORY_GAME` (API 26).
  - Default camera: `pm.resolveActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), 0)`.
  - **Manifest:** add `<queries>` for `<intent><action MAIN/><category LAUNCHER/></intent>` and `<intent><action android.media.action.STILL_IMAGE_CAMERA/></intent>` (API 30 package visibility). Avoid `QUERY_ALL_PACKAGES`.
  - Don't switch to `AccessibilityService.getWindows()`: the config has `canRetrieveWindowContent="false"` and only `typeWindowStateChanged` (`res/xml/accessibility_service_config.xml`), so it would need a disclosure change.
- **Acceptance criteria:** Device: the flag is true in YouTube fullscreen, a game (any `CATEGORY_GAME` title) and the camera app, and false on the home screen and in Chrome. It doesn't flicker in split-screen.
- **Tests:** `FullscreenDetectorTest` (pure combiner of the inset, category and camera signals).
- **Depends on:** F1.
- **Effort:** S   **Risk:** med (OEM insets). Mitigation: the game and camera signals are independent of insets.
- **Ships as:** Plumbing.

### A6.2 — "In fullscreen apps" setting: Hide / Calls & alarms only / Always
- **Goal:** A new setting, defaulting to *Calls & alarms only*. The arbiter filters to T0 plus ongoing calls in that mode, to nothing in Hide, and doesn't filter in Always. Nothing is touchable when nothing is shown (B2/F5).
- **Why:** PLAN §4 A6; 05 R3.
- **Files:** `model/IslandSettings.kt` (`fullscreenPolicy`); `data/SettingsRepository.kt` (both mappers); `ui/main/MainScreen.kt` (a row beside "Show in landscape", `~:2082-2095`); the arbiter's `FullscreenGate`.
- **Acceptance criteria:**
  - Default: YouTube fullscreen + Spotify → no island. An incoming WhatsApp call → the call card shows.
  - "Always" behaves as today.
  - The window is non-touchable while hidden (device: tap the status-bar area during a game).
- **Tests:** `ArbiterTest` fullscreen cases; `SettingsMigrationTest`.
- **Depends on:** A6.1, A2.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature**.

---

## A7 — New system sources

### A7.1 — `SystemActivitySource`: glances become store entries
- **Goal:**
  - Charging, unlock, Bluetooth/Wi-Fi and ringer become T4 specs with `expiresAt`. This replaces 4 separate flows and jobs (`IslandStateManager.kt:45-61`, `:100-108`, `:414-466`).
  - The receivers move out of the service into `sources/SystemEventReceivers.kt`, as in the 06 §4 target.
- **Why:** The arbiter can then apply "never over T0" in one place instead of the per-trigger `isIncomingCallRinging` checks (`:416`, `:432`, `:446`, `:457`). It's also the base for A7.2–A7.8.
- **Files:**
  - `service/IslandOverlayService.kt:252-278` (Wi-Fi), `:280-361` (`powerReceiver`), `:498-515` (filters)
  - `island/IslandStateManager.kt:414-471`
  - `island/IslandView.kt:467-471` (the glance gates)
  - New: `app/src/main/java/com/droidbubble/app/sources/SystemActivitySource.kt`
- **Steps:**
  1. Register the receivers with `RECEIVER_NOT_EXPORTED` (B1).
  2. Read the glance TTLs from `IslandTimings` (F3).
  3. Keep the glance compact designs exactly as they are.
- **Acceptance criteria:**
  - `IslandStateBehaviorTest`'s ringing-call cases pass.
  - S2 is identical for every glance.
  - Device: plug in a charger during a ringing call → no glance. Plug in afterwards → the glance shows.
- **Tests:** `SystemActivitySourceTest` (TTL expiry, same-kind replacement).
- **Depends on:** A1.3, A2.2, B1, F3.
- **Effort:** M   **Risk:** low.
- **Ships as:** Plumbing.

### A7.2 — Battery: low, full and time to full
- **Goal:**
  - A low-battery glance at 20% and 10% (or `ACTION_BATTERY_LOW`).
  - A "Charged" glance once per plug session at `BATTERY_STATUS_FULL`.
  - The charging glance's trailing side shows the ETA ("1h 20m") from `computeChargeTimeRemaining()` when it's ≥ 0.
- **Why:** 04 P2 #19; 04 §7 "Charging / low / full".
- **Files:** `sources/SystemActivitySource.kt`; `service/BatteryUtils.kt`; `model/IslandSettings.kt` (`showBatteryAlerts`); `ui/main/MainScreen.kt` (System Alerts section, `:891-980`, adding one row).
- **Android hooks:** `ACTION_BATTERY_LOW` / `ACTION_BATTERY_OKAY` / `ACTION_POWER_DISCONNECTED` (runtime only; they can't be manifest-registered since API 26); the `ACTION_BATTERY_CHANGED` sticky read on those events only (no continuous receiver); `BatteryManager.computeChargeTimeRemaining()` (API 28). No permission.
- **Acceptance criteria:**
  - Device: draining past 20% shows one glance, not repeated until okay or plugged.
  - A charge to full shows "Charged" once.
  - The ETA appears on a Pixel and is hidden when it returns −1.
- **Tests:** `BatteryGlanceRulesTest` (hysteresis, once-per-session).
- **Depends on:** A7.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** the existing charging glance. The low-battery icon pulses and the bolt bounces with a fill, via I3 (the I3 table already lists "Charging: bolt bounce plus fill").

### A7.3 — Bluetooth glance: device name, type and per-device cooldown
- **Goal:**
  - The connect glance shows the device's alias and a type icon (headphones, watch, speaker, car, gamepad or keyboard).
  - The ACL and A2DP/headset events are deduplicated into one glance.
  - A device that reconnects within 10 minutes stays silent, so watches don't spam it.
- **Why:** 04 P1 #10: "no device name, type or battery, and watches that reconnect often keep re-triggering it". Today every event fires (`IslandOverlayService.kt:319-334`).
- **Files:** new `sources/BluetoothSource.kt`; `service/IslandOverlayService.kt:319-334`; `island/IslandStateManager.kt:427-442`; `MainActivity.kt:50-54` (the cold-launch permission request moves to the toggle, per C1).
- **Android hooks:**
  - `BluetoothDevice.ACTION_ACL_CONNECTED` + `EXTRA_DEVICE`
  - `getAlias()` (API 30), else `name`. Both need `BLUETOOTH_CONNECT` at runtime on API 31+; already declared at `AndroidManifest.xml:18`.
  - `device.bluetoothClass.majorDeviceClass` / `deviceClass` (`AUDIO_VIDEO_*`, `WEARABLE_WRIST_WATCH`, `PERIPHERAL`, `AUDIO_VIDEO_CAR_AUDIO`)
  - Every call is in `runCatching`, because a `SecurityException` without the permission falls back to today's generic glance.
- **Acceptance criteria:**
  - Device: Galaxy Buds or Pixel Buds show "Buds · ⟨alias⟩" with the headphone icon.
  - A Pixel Watch reconnecting 5 times in 10 min → 1 glance.
  - With the permission denied → the generic glance, no crash.
- **Tests:** `BluetoothCooldownTest`, `BluetoothClassMappingTest`.
- **Depends on:** A7.1; C1 (permission on demand).
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** the existing connection glance with a type icon (V7); "appear" via I3.

### A7.4 — Battery for every Bluetooth device
- **Goal:**
  - The connect glance shows the device's battery when it's known.
  - A device crossing 20% or 10% gets a low-battery glance (earbuds, watch, controller).
- **Why:** PLAN v1 §2 functions row "Battery for every Bluetooth device"; 04 §6.
- **Files:** `sources/BluetoothSource.kt`.
- **Android hooks:**
  - `android.bluetooth.device.action.BATTERY_LEVEL_CHANGED` + `android.bluetooth.device.extra.BATTERY_LEVEL` **[M]** (historically `@hide` and device-dependent)
  - reflective `BluetoothDevice.getBatteryLevel()` **[M]**, in `runCatching`, where −1 means unknown
  - Requires `BLUETOOTH_CONNECT`.
- **Acceptance criteria:**
  - Device: the Pixel Buds level appears within the connect glance on a Pixel, and on Samsung where it's reported.
  - Devices without reporting show no battery and no error.
  - A low warning fires once per discharge.
- **Tests:** `BtBatteryRulesTest`.
- **Depends on:** A7.3.
- **Effort:** S   **Risk:** med (hidden API). Mitigation: the feature degrades to nothing.
- **Ships as:** **Feature**.

### A7.5 — VPN on/off (+ hotspot, best effort)
- **Goal:**
  - A T4 glance when a VPN connects or disconnects.
  - An opt-in "Keep VPN as an activity" setting (T2, shield icon, "VPN").
  - A hotspot glance only where the hidden broadcast works.
- **Why:** 04 P2 #19; 04 §7 VPN row.
- **Files:** new `sources/VpnSource.kt`; settings (both mappers); the System Alerts row.
- **Android hooks:**
  - `ConnectivityManager.registerNetworkCallback(NetworkRequest.Builder().addTransportType(TRANSPORT_VPN).removeCapability(NET_CAPABILITY_NOT_VPN).build(), cb)`. The default request includes `NOT_VPN`, so it **must be removed**, otherwise VPNs are never reported.
  - `ACCESS_NETWORK_STATE` is already declared (`AndroidManifest.xml:22`). The VPN owner app isn't exposed to us.
  - Hotspot: `android.net.wifi.WIFI_AP_STATE_CHANGED` **[L-M]**, guarded.
- **Acceptance criteria:**
  - Device: WireGuard or Proton VPN on/off → one glance each.
  - With the setting on, the VPN sits at T2 below media.
  - Registration doesn't report the existing VPN as "new" (the 2 s guard, same as Wi-Fi at `IslandOverlayService.kt:261`).
- **Tests:** `VpnSourceTest` (callback sequencing).
- **Depends on:** A7.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature**.

### A7.6 — Do Not Disturb glance (Modes: filter level only)
- **Goal:**
  - A glance when the interruption filter changes ("Do Not Disturb", "Priority only", "Alarms only", "Off").
  - The filter is published into the scene's env, so C4's DND awareness can use it.
- **Why:** 04 P2 #19; PLAN A7 "Do Not Disturb and Modes".
- **Correction:** on Android 15, a Mode's name (`AutomaticZenRule`) is only readable by the app that owns the rule, so "Modes" here means the filter level. **[M]**
- **Files:** `service/NotificationReceiverService.kt` (add `onInterruptionFilterChanged`; the listener is already bound, so no receiver is needed); `sources/SystemActivitySource.kt`.
- **Android hooks:** `NotificationListenerService.onInterruptionFilterChanged(int)` (API 21) and `getCurrentInterruptionFilter()`. No permission.
- **Acceptance criteria:**
  - Device: toggling DND from the QS tile → one glance.
  - A Bedtime mode start → a glance.
  - No glance at listener connect.
- **Tests:** `DndGlanceTest`.
- **Depends on:** A7.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** the ringer glance; the bell→moon change uses I3 "replace".

### A7.7 — Next alarm glance *(optional; dropped from the v2 A7 text)*
- **Goal:** In the evening (after 21:00), when an alarm is set or changed, show one glance: "Alarm 7:00".
- **Why:** 04 §2 Alarms row; 04 P2 #19.
- **Files:** `sources/SystemActivitySource.kt`.
- **Android hooks:** `AlarmManager.getNextAlarmClock()` + `ACTION_NEXT_ALARM_CLOCK_CHANGED` (API 21). The time is formatted with `DateFormat.getTimeFormat(ctx)`, which is locale- and 24h-aware.
- **Acceptance criteria:** Setting an alarm at 22:00 → one glance. Daytime changes → none.
- **Tests:** a rules test.
- **Depends on:** A7.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature** (optional).

### A7.8 — Mic and camera privacy pill
- **Goal:**
  - A T2 activity "Mic in use" or "Camera in use" while another app records or uses the camera.
  - Tap → the privacy dashboard.
  - No permission is needed.
- **Why:** 04 §6 "Privacy pill … Original and needs no permission"; 04 §7.
- **Files:** new `sources/PrivacySource.kt`; the arbiter (`SYSTEM` bucket).
- **Android hooks:**
  - `AudioManager.registerAudioRecordingCallback(cb, handler)` (API 24). `AudioRecordingConfiguration` client packages are redacted for third parties, so the pill says "Mic in use" without an app name.
  - **Exclude our own sessions:** the `MicLevelMeter` `AudioRecord` (`island/AudioLevels.kt:272-279`, gone after A9.1) and the `Visualizer` (`:140`). Match by `clientAudioSessionId`.
  - `isClientSilenced` (API 29) → no pill.
  - `CameraManager.registerAvailabilityCallback` → `onCameraUnavailable` / `onCameraAvailable` (API 21). Ignore while the keyguard is locked (face unlock) and for the first 1.5 s. The torch (`setTorchMode`) doesn't open the camera.
  - Tap: `Intent("android.intent.action.REVIEW_PERMISSION_HISTORY")` **[M]** (API 31), falling back to `Settings.ACTION_PRIVACY_SETTINGS`.
- **Acceptance criteria:**
  - Device: a WhatsApp voice note → the mic pill for its duration.
  - The Google Camera app → the camera pill, unless the A6 fullscreen policy hides it.
  - Face unlock → no pill.
  - Droidbubble's own visualizer → no pill.
- **Tests:** `PrivacySourceTest` (own-session filter, debounce, keyguard gate).
- **Depends on:** A7.1, A2.2.
- **Effort:** M   **Risk:** med — OEM camera-availability quirks. Mitigation: debounce, and a setting to disable it.
- **Ships as:** **Feature**.
- **Design check:** an existing compact layout with a mic or camera icon (V7) and the recording-dot treatment in a F2 semantic colour. The pulse uses I3 "Recording in progress → pulse". It is Droidbubble's own (Android's corner dot stays as well).

### A7.9 — Screen-recording callback on API 35+
- **Goal:** On API 35+, detect screen recording with the platform callback. Keep notification text as the fallback (A4.4).
- **Why:** 04 §5.8; 04 P2 #14 (false positives from text).
- **Files:** `service/IslandOverlayService.kt` (register against the overlay window's `WindowManager`); `service/NotificationReceiverService.kt:707-757` (fallback only); `AndroidManifest.xml` (add `<uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING" />`, a normal permission).
- **Android hooks:** `WindowManager.addScreenRecordingCallback(executor, Consumer<Int>)` → `SCREEN_RECORDING_STATE_VISIBLE` / `NOT_VISIBLE` (API 35). It reports whether *our* windows are being captured, so it's true for full-screen recordings while the island is shown and false for single-app recordings. **[M]** for `TYPE_ACCESSIBILITY_OVERLAY` windows. The start time is the callback time; the stop controls still come from the recorder's notification.
- **Acceptance criteria:**
  - Device (Pixel, Android 15): the system screen recorder starts → the recording activity appears within 1 s, even if the notification text is in Indonesian.
  - An idle "ready" recorder notification → nothing.
- **Tests:** `ScreenRecordingDetectorTest` (merge of callback + notification signals).
- **Depends on:** A7.1, A4.4.
- **Effort:** S   **Risk:** med (whether overlay windows count). Mitigation: the fallback remains.
- **Ships as:** **Feature**.

---

## A8 — Android-only extras

### A8.1 — "Pin as activity"
- **Goal:**
  - Any ongoing notification (Pomodoro, workout, VPN timer, build runner) can be pinned. It then shows as a generic activity: app icon leading; trailing = `shortCriticalText`, the chronometer or a short title; expanded = the existing notification card with its actions.
  - Pins persist per `packageName|channelId`, so future posts are pinned automatically.
- **Why:** 04 §6 "A user-defined promotion iOS cannot offer; it reuses the generic Slot renderer". Today these are dropped at NRS `:980-983`.
- **Files:**
  - New: `island/activity/adapters/PinnedAdapter.kt`
  - NRS `:966-983` (pinned bypass)
  - `model/IslandSettings.kt` (`pinnedChannels: Set<String>`), `data/SettingsRepository.kt` (both mappers)
  - Entry points:
    - a "Pin as activity" action on the expanded notification card's action row (`IslandView.kt` `ExpandedNotificationView`), shown only for ongoing notifications
    - a per-app list inside the existing Priority Apps sheet (`MainScreen.kt ~:1133`)
    - a long-press menu is *not* used (long-press opens the app, PLAN UX)
- **Acceptance criteria:**
  - Device: a Forest or Pomodoro ongoing timer → Pin → appears as T2 with the chronometer.
  - After a reboot, the next session is pinned automatically.
  - Unpinning restores today's filtering.
- **Tests:** `PinnedAdapterTest` (fixture: an ongoing FGS notification with a chronometer).
- **Depends on:** A3.1, A2.2.
- **Effort:** M   **Risk:** low.
- **Ships as:** **Feature**.
- **Design check:** existing card and compact layouts only.

### A8.2 — Track every media session in the store
- **Goal:** Every eligible session (playing, or paused within 10 min, with a title) is its own `MEDIA` spec, instead of the single "best controller". The transport callbacks target a session id.
- **Why:** 04 §3 "Only one session is shown and there is no switcher"; it's the precondition for A8.3.
- **Files:**
  - NRS `:341-353` (`refreshMedia`), `:355-388` (`syncControllers`), `:413-468` (`findBestController`, which becomes `rank()` for the default), `:470-480`, `:482+` (`updateMediaInfo` per controller)
  - NRS `:91-112` (callbacks take a session id)
  - `island/IslandStateManager.kt:177-185` (callback vars)
  - New: `sources/MediaSessionTracker.kt`, following B10's debounce
- **Acceptance criteria:**
  - Device: Spotify plays while a YouTube video is paused → 2 media specs; the arbiter shows Spotify.
  - Play/pause acts on the shown session.
  - No extra binder calls per update (B10 removed `activeNotifications` from this path).
- **Tests:** `MediaSessionRankTest` (the 6 rules of `findBestController`, ported as-is).
- **Depends on:** A1.7, B10.
- **Effort:** M   **Risk:** med (media is the most common state). Mitigation: same default choice as today, checked by the S1 media cases.
- **Ships as:** Plumbing.

### A8.3 — Media session switcher
- **Goal:**
  - In the expanded media card, a horizontal swipe on the artwork switches to the next session, using the existing art flip (`FlippingCoverArt`).
  - The existing tokens draw a small page indicator, only when there are ≥ 2 sessions.
  - TalkBack gets "Next media source" and "Previous media source" actions.
- **Why:** 04 §6 "Multi-session media switcher: swipe between active MediaSessions (e.g. podcast and YouTube). iOS shows only one."
- **Files:** `island/IslandView.kt` `ExpandedMediaView` (`:3282`), the M4 gesture modifier, `MediaSessionTracker`.
- **Acceptance criteria:**
  - Device: Spotify + Pocket Casts → swipe swaps the title, art and controls. The primary-pill media follows the chosen session until another one starts playing.
  - The swipe doesn't conflict with card-dismiss (M4 axis lock: the art area claims the horizontal gesture).
- **Tests:** S2 baseline with the indicator; gesture test via M4's harness.
- **Depends on:** A8.2, M4, I2 (play-state morph, optional).
- **Effort:** M   **Risk:** med — gesture conflicts. Mitigation: the art area only.
- **Ships as:** **Feature**.
- **Design check:** the current media card is unchanged except for the indicator. No redesign (the PLAN v2 non-goal).

---

## A9 — Housekeeping

### A9.1 — Recorder waveform without our own `AudioRecord`
- **Goal:** `LiveWaveform` stops opening the microphone. It animates from the "is another app recording and not silenced" signal (A7.8's `AudioRecordingCallback`) with the existing synthetic motion.
- **Why:** 04 P2 #18: "That attributes the mic indicator to Droidbubble, and the audio is probably silenced: the service is a `specialUse` FGS".
- **Files:** `island/AudioLevels.kt:196-335` (`MicLevelMeter`, `AudioRecord` at `:272-279`), `:354-361` (`MicLevelEffect`); `island/LiveActivities.kt:569-618` (`LiveWaveform`, `:570`).
- **Note:** `RECORD_AUDIO` (`AndroidManifest.xml:14`) must **stay**, because the output `Visualizer` (`AudioLevels.kt:140`) needs it. Only the mic capture goes.
- **Acceptance criteria:**
  - Device: during a Google Recorder session, Android's mic indicator names only Recorder, never Droidbubble.
  - The waveform still moves while recording and stops when paused.
  - S2 is unchanged (motion only).
- **Tests:** `MicLevelMeter` usage removed; a unit test for the activity-driven level source.
- **Depends on:** A7.8 (shares the callback); it can ship earlier with the static wobble.
- **Effort:** S   **Risk:** low.
- **Ships as:** **Feature** (privacy fix).

### A9.2 — Remove dead receivers and legacy heuristics
- **Goal:** Remove:
  - the manifest `PowerConnectionReceiver` (`AndroidManifest.xml:108-116`, `service/PowerConnectionReceiver.kt`; apps can't receive `ACTION_POWER_CONNECTED` from the manifest since API 26, and the service already registers it at runtime, `IslandOverlayService.kt:498-515`)
  - the English-only regex remnants left after A4.3
  - `LiveActivityKind` / `liveActivityKind()`, once every adapter emits `ActivityKind`
  - `isLiveActivity` (`IslandEvent.kt:92-93`)
- **Already done (pre-Phase-0 cleanup, 2026-11-08):** the manifest `PowerConnectionReceiver` and `ScreenRecordingService` are deleted. Only the legacy heuristics part of this task remains.
- **Also flag:** `service/ScreenRecordingService.kt` and its manifest entry (`:68-72`) plus `FOREGROUND_SERVICE_MEDIA_PROJECTION` (`:9`). **Nothing in the code starts this service** (grep finds no caller). Ask the owner before deleting it.
- **Files:** as listed.
- **Acceptance criteria:**
  - Builds and tests are green.
  - Charging still pops up after a cold start (device).
  - `grep -n "PowerConnectionReceiver\|liveActivityKind"` is empty in `main/`.
- **Tests:** existing suites.
- **Depends on:** A1.9, A4.3.
- **Effort:** S   **Risk:** low.
- **Ships as:** Plumbing.

---

## Execution order

`∥` marks tasks that can run in parallel with the previous item, as long as they don't share files.

1. **A1.1** classify once. ∥ **A2.1** pure arbiter ∥ **A4.1** control resolver ∥ **A3.7** chronometer reader (written against today's NRS)
2. **A1.2** spec types and legacy mapper
3. **A1.3** store, shadow mode
4. **A1.4** legacy projection, stash deleted (the first user-visible fix). ∥ **A3.1** snapshot and adapter chain (NRS only)
5. **A2.6** Test lab stacks. ∥ **A4.2** locale tables
6. **A1.5** slot renderers
7. **A1.6** compact and bubble from slots (`IslandView`: serialize with V3 and M3)
8. **A1.7** media, torch and recording in the store
9. **A1.8** expanded, heights and open target from the spec
10. **A2.2** scene uses the arbiter (needs F1)
11. **A1.9** delete the legacy slots
12. **A2.3** banners recede. Then **A2.4** "+N" overflow (`IslandView`). ∥ **A4.3** wire roles (NRS + `LiveActivities`; serialize with A3.2)
13. **A3.2** CallStyle → **A3.3** Live Updates → **A3.4** ProgressStyle parse → **A3.5** journey bar → **A3.6** navigation. These are sequential because they share `LiveActivities.kt` and NRS. ∥ **A4.4**
14. **A5.1** → **A5.2** lock-screen privacy. ∥ **A6.1** → **A6.2** fullscreen policy
15. **A7.1** system source, then in parallel (one source file each; they collide only on receiver registration):
    - **A7.2** battery
    - **A7.3** → **A7.4** Bluetooth
    - **A7.5** VPN
    - **A7.6** DND
    - **A7.7** next alarm (optional)
    - **A7.8** privacy pill
    - **A7.9** screen-recording callback
16. **A8.1** pin as activity. ∥ **A8.2** media tracker → **A8.3** switcher (after M4)
17. **A2.5** why-primary and overrides (optional)
18. **A9.1** waveform (any time after A7.8; or earlier with the static wobble) → **A9.2** cleanup (last)

## Activity catalogue checklist (04 §7 → delivering tasks)

| Activity | Detection | Presentation | Tier |
|---|---|---|---|
| Incoming call | A3.2 (CallStyle type 1), A4.3 (Answer/Decline in any locale) | A1.6, A1.8 (existing call card) | A2.1 T0 |
| Ringing alarm / Time's up | A4.4 (category + fullscreen intent), A3.7 | A1.6, A1.8 (`LiveAlertView`) | A2.1/A2.2 T0 |
| Ongoing call | A3.2 (type 2, `when`, startedAt fallback) | A3.2 (mute/speaker buttons) | T1 |
| Navigation | A3.6 | A3.6 (tracking-row layout with arrow) | T1 |
| Voice / screen recording | A1.7, A4.4, A7.9 | A1.6, A9.1 (waveform without mic) | T1 |
| Timer / stopwatch | A3.7, A4.3 | A1.6 (ring and clock slots) | T1 |
| Ride / delivery / flight | A3.3, A3.4 | A3.5 (journey bar) | T1 with ETA, else T2 |
| Media | A1.7, A8.2 | A8.3 (session switcher) | T2 |
| Download / progress | A1.4 (multi-download), A3.4 | A1.6, A3.5 | T2 |
| Live score | B4 (strict regex), A1.2 mapper | A1.6, A1.8 (`LiveScoreRow`) | T2 |
| Flashlight | A1.7 | A1.7 (existing; V3 adds the trailing value) | T2 |
| VPN / hotspot | A7.5 | glance; opt-in T2 | T4 (opt-in T2) |
| Mic / camera in use | A7.8 | A7.8 (existing compact + I3 pulse) | T2 |
| Message banner | A3.1 (legacy adapter), A5.1 | A2.3 (recede), A5.2 (redaction) | T3 |
| Charging / low / full | A7.1, A7.2 | existing charging glance + I3 | T4 |
| Bluetooth connect | A7.3, A7.4 | existing connection glance + type icon | T4 |
| Ringer / DND / Mode | A7.1, A7.6 | existing ringer glance + I3 replace | T4 |
| Unlock | A7.1 | existing unlock glance | T4 |
| Weather / network speed | B7 (truthful data) | unchanged | A2.1 T5 |
| *(04 §6)* Pin as activity | A8.1 | A1.5 generic slots | T2 (user can raise to T1 via A2.5) |
| *(04 §6)* "+N" overflow | A2.1 | A2.4 | — |
| *(04 §2)* Next alarm | A7.7 (optional) | glance | T4 |
