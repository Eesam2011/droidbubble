# Functionality audit: activities, detection, prioritization

Scope: read-only review of the `island-polish` working tree, plus web research. Apple is used as a reference, not a spec. Confidence labels: **[H]** high, **[M]** medium, **[L]** low.

## 1. Apple reference (for ideas, not to copy)

- **Presentations [H]:**
  - *Compact*: leading and trailing views around the camera, used when one activity is running.
  - *Minimal*: used when several apps have activities. On most devices both go minimal, one attached to the camera and one detached.
  - *Expanded*: leading, trailing, center and bottom regions. Opens on long-press, and briefly on an alerting update.
  - The Lock Screen has its own presentation. Newer hardware can show two reduced-width compact activities **[M]**. Source: Apple, "Displaying live data with Live Activities" (developer.apple.com/documentation/activitykit/displaying-live-data-with-live-activities).
- **Priority [H]:** `relevanceScore` decides which of an app's activities appears in the island. A tie goes to the one that started first, and the score also orders the Lock Screen. How the system picks between different apps is not documented **[M]**. Source: developer.apple.com/documentation/activitykit/activitycontent/relevancescore.
- **Lifecycle [H]:**
  - Up to 8 h in the island, then up to 4 more hours on the Lock Screen (12 h max).
  - `staleDate` sets `isStale`.
  - `AlertConfiguration` on an update briefly expands the island.
  - An ended activity stays up to 4 h by default; a dismissal policy is passed to `end`.
  - Payload limit is 4 KB.
- **System catalogue [M]:** Apple publishes no list; this is compiled from press and community pages (9to5mac.com/2022/09/14/dynamic-island-supported-apps, macworld.com/article/1069195, appleinsider.com, discussions.apple.com/thread/254212499).
  - Calls/FaceTime, timers, Now Playing, Maps navigation, Voice Memos.
  - Face ID, Apple Pay/Wallet, AirDrop, AirPods connected (with battery), charging, mute switch, Focus, Personal Hotspot, AirPlay, Shortcuts, CarKey/Watch unlock.
  - Third-party rides, delivery, sports and flights. I could not confirm Shazam.
- **Worth borrowing:** slot-based presentations, a numeric relevance with a start-time tiebreak, an alert that expands then recedes, and stale/age limits.
- **Not worth copying:** the 8 h cap, and overflow that only offers "minimal".

## 2. Android platform hooks

| Activity | API / signal | Min API | Permission | Conf. |
|---|---|---|---|---|
| Live Updates | `FLAG_PROMOTED_ONGOING` (262144 = 0x40000; the system sets it, a listener can read it). Also `hasPromotableCharacteristics()`, `EXTRA_REQUEST_PROMOTED_ONGOING`, `setShortCriticalText`, and a `when`/chronometer status chip (a `when` ≥2 min ahead renders "5min"). Eligible styles: Standard, BigText, CallStyle, ProgressStyle, MetricStyle. Not eligible: custom RemoteViews, colorized, group summary, `IMPORTANCE_MIN`. OEMs may add their own criteria. | 36 | Notification listener | H (developer.android.com/develop/ui/views/notifications/live-update) |
| ProgressStyle | Segments (with colours), points (milestones), tracker/start/end icons | 36 | Notification listener | H for the API (developer.android.com/about/versions/16/features/progress-centric-notifications); M for raw extra keys such as `android.progressSegments` |
| Calls | `EXTRA_CALL_TYPE`: 0 unknown, 1 incoming, 2 ongoing, 3 screening. Also answer/decline/hang-up intents, `callPerson`, `setIsVideo`, verification text. Ongoing calls allow 2 custom actions (e.g. speaker/mute). | 31 (constants 34) | Notification listener | H (developer.android.com/reference/android/app/Notification.CallStyle) |
| Timers / stopwatch / recorders | `EXTRA_SHOW_CHRONOMETER` + `when` + `EXTRA_CHRONOMETER_COUNT_DOWN` | 24 | Notification listener | H |
| Media | `MediaSessionManager.getActiveSessions(listenerComponent)` | 21 | Notification listener | H |
| Navigation | `CATEGORY_NAVIGATION`. Before 16, Google Maps puts distance/instruction in title/text and the turn arrow in the large icon **[M]**. On 16 QPR, Maps uses a Live Update chip with time-to-turn/ETA (staged rollout) **[M]** (androidauthority.com/google-maps-live-updates-3532808) | — | Notification listener | M |
| Screen recording | `WindowManager.addScreenRecordingCallback`; reports whether this UID's windows are being captured (feature-flagged) | 35 | `DETECT_SCREEN_RECORDING` (normal) | M for overlay windows (AOSP commit c3aa8fee8c99; proandroiddev.com screen-recording-detection-android-15) |
| Mic / camera in use | `AudioManager.registerAudioRecordingCallback` (package names redacted); `CameraManager.AvailabilityCallback` | 24 / 21 | none | H |
| Bluetooth | `ACTION_ACL_CONNECTED` + `EXTRA_DEVICE`, `BluetoothClass` for device type. Battery via `android.bluetooth.device.action.BATTERY_LEVEL_CHANGED` / `...extra.BATTERY_LEVEL`, historically `@hide` and device-dependent (AOSP commit 1d312bfa) | 31 | `BLUETOOTH_CONNECT` (runtime) | H / M |
| Battery | `ACTION_BATTERY_CHANGED`, `ACTION_BATTERY_LOW/OKAY`, `BATTERY_STATUS_FULL`, `BatteryManager.computeChargeTimeRemaining()` | 28 | none | H |
| Ringer / DND | `RINGER_MODE_CHANGED_ACTION`, `ACTION_INTERRUPTION_FILTER_CHANGED`; Android 15 Modes (`AutomaticZenRule`) | 23 / 35 | none | H |
| VPN / hotspot | `NetworkCallback` with `TRANSPORT_VPN` (H); `android.net.wifi.WIFI_AP_STATE_CHANGED` (hidden constant) (L-M) | 21 | `ACCESS_NETWORK_STATE` | — |
| Alarms | `AlarmManager.getNextAlarmClock()` + `ACTION_NEXT_ALARM_CLOCK_CHANGED`; a ringing alarm is `CATEGORY_ALARM` + `fullScreenIntent` | 21 | none | H |
| Not feasible | Clipboard (background reads blocked since Android 10), NFC/Wallet taps, biometric-unlock success (only `ACTION_USER_PRESENT`) | — | — | H |

- **Samsung Now Bar:** One UI 8 surfaces Android 16 Live Updates there, but the rollout is gated **[M]** (androidauthority.com/one-ui-8-live-updates-support-3573794).
- **Competitors:** DynamicSpot offers timer countdowns, Maps direction + distance, Bluetooth headset battery on connect, low-battery and charging alerts, unlock popup, replies, and a Pro lock-screen view **[M]** (apkmirror changelogs v1.70–1.82; sourceforge.net/app/dynamicspot).

## 3. Droidbubble today

### Activity inventory

- **Media:** MediaSession via the listener, with a 6-step best-controller pick. Paused sessions drop after 10 min (`NotificationReceiverService.kt:413-468`, `:511`). Only one session is shown and there is no switcher.
- **Screen recording:**
  - Detected from ongoing notification text, package and channel (`NRS:707-757`). English text only.
  - The last `||` clauses accept *any* ongoing notification titled "screen recorder". That catches idle "ready" notifications from third-party recorders.
  - The app also has its own MediaProjection recorder (`ScreenRecordingService.kt`).
- **Progress/downloads:** progress extras, plus the ProgressStyle segment sum (`NRS:926-933`, `:1216`). There is one slot, `_activeProgress` (`IslandStateManager.kt:116`).
- **Navigation:**
  - Detected as `CATEGORY_NAVIGATION` or any package containing "maps", and ongoing (`NRS:950-953`). It is forced into the indeterminate PROGRESS card.
  - The card title defaults to "Downloading" (`IslandView.kt:3996`).
  - The turn-arrow large icon is lost because the app icon wins (`NRS:1162-1170`).
- **Notification-based kinds** (`liveActivityKind`, `LiveActivities.kt:226-253`):
  - **CALL:** `CATEGORY_CALL` or CallStyle. Answer/decline come from extras, else from English action titles (`NRS:1044-1052`). `contains("end")` also matches "Send…". Screening calls (type 3) lose Answer. Custom mute/speaker actions and the video flag are ignored (`LiveActivities.kt:625-695`).
  - **ALERT:** clock package plus `CATEGORY_ALARM` + `fullScreenIntent`, or the English text "time's up" (`NRS:941-944`).
  - **RECORDING / TIMER / STOPWATCH:**
    - Base time: chronometer `when`, else a timer read out of an inflated custom RemoteViews `Chronometer` (`NRS:1238-1266`), else a "00:42"-style title minus `postTime`, stabilized (`IslandEvent.kt:117`).
    - Kind: package-hint lists (`LiveActivities.kt:96-128`) plus English words.
  - **ALARM:** clock app + "alarm"/"snooz" + a printed clock time.
  - **TRACKING:** promoted, or (ongoing and a tracking package hint) (`:129-142`). The ETA comes from `shortCriticalText` or "N min".
  - **SPORTS:** score regex (`:156`) for sports packages *or any ongoing/promoted notification*.
- **System transients** (`IslandOverlayService.kt:280-359`):
  - Charging: 4 s, plug-in only.
  - Unlock: 2.2 s, from `USER_PRESENT`.
  - Bluetooth: 3 s, from ACL and A2DP/headset events. No name, type or battery is shown, and it fires for every device.
  - Wi-Fi: on every `onAvailable`.
  - Ringer: 2.5 s.
  - Flashlight: torch callback with a flicker filter (`IslandStateManager.kt:366-394`).
- **Persistent widgets:**
  - Weather: location guessed from the timezone, synthetic hourly entries, fake "22°, Partly Cloudy" on failure (`PersistentWidgetTelemetryManager.kt:262-336`).
  - Network speed: 1 Hz `TrafficStats`, screen-on only (`:148-200`). Cellular is hard-coded "5G" (`:175`, `IslandView.kt:2852`).
  - FPS: removed (`IslandView.kt:473`), but the enum and views remain.
- **Spoken alerts:** text-to-speech for calls and alerts (`SpokenAlerts.kt:77`).

### Priority and arbitration

- **Ordering:** built in the view (`IslandView.kt:499-539`).
  - Calls and user "priority apps" come first.
  - Then the user's drag order of `MEDIA`, `PROGRESS`, `RECORDING`, `NOTIFICATION`, `FLASHLIGHT` (default MEDIA first).
  - Weather and network speed fill in only when nothing else is active.
  - The first item is primary; the second becomes the split bubble, which acts as a "minimal".
  - A 3rd, 4th or 5th activity is hidden with no indication.
- **Second, contradicting order:** `IslandStateManager.currentPriority` (`:326-336`) hard-codes a different order and is used by `expand()`.
- **One notification slot:** every notification kind (timers, rides, scores, messages) shares `_activeNotification` plus a single `stashedLiveNotification` (`:528`, `:563-571`).
  - A one-off message pushes an ongoing call, timer or recorder aside.
  - Because auto-dismiss is off by default, the message stays until it is swiped away.
  - A second live activity overwrites the stash, so the earlier one is lost. Countdowns are rarely reposted, so that loss is permanent.
  - Only ringing calls and alerts are protected (`:558`).
- **Transient overlays:** charging, Bluetooth, unlock and ringer collapse the expanded island and suppress the split; ringing calls block them.

### Data model

- There is **no unified model**. Kinds come from three switch dimensions: `IslandPriority` (9 values), `LiveActivityKind` (9 values), and many separate `StateFlow`s.
- Each presentation is a hand-written `when`: compact leading and trailing (`IslandView.kt:2654-2970`), minimal/aux (`:2462`), expanded (`:2973`), heights (`IslandComponents.kt:211`), outline colours, gestures, `openAppForPriority`, and settings.
- `IslandPriority.FLASHLIGHT` alone is referenced 14× in `IslandView`.
- Adding a type touches about 10 places in 5 or more files.
- `liveActivityKind()` (regex) is recomputed on every recomposition, including each timer tick.
- The `LiveLayout` design rules (`LiveActivities.kt:316-358`) are good and reusable.

## 4. Gaps

**P0**
1. **Live activities are off by default.**
   - Calls, timers, alarms, recorders, scores, and rides without a progress bar all go through the "Incoming Notifications" toggle, whose default is `false` (`SettingsRepository.kt:92`, `NRS:956-960`).
   - The view also gates every notification kind on `showNotifications` (`IslandView.kt:463`). So a ride Live Update *with* a progress bar passes the service (under "Live Progress & Activities") and is then hidden by the view.
   - On a fresh install, none of the headline activities appear.
2. **Single-slot arbitration** (see section 3): a chat message hides an ongoing call or timer, and a third activity is lost for good. Two downloads at once: only the last one is shown.
3. **English-only heuristics:**
   - Pause/stop/snooze regexes (`LiveActivities.kt:148-154`), answer/decline titles (`NRS:1044-1051`), "time's up", screen-recording text, "paused".
   - On non-English phones, incoming calls without CallStyle lose Answer, recorders and timers lose their controls, and screen recordings are missed.
   - The package hints (talabat, careem, jahez) suggest many MENA users.

**P1**

4. **The score regex misfires.** It accepts `:`, so text like "Arrive at 10:45 PM" or "Boarding at 10:30 Gate B" is parsed as a score. The SPORTS check runs before TRACKING (`:250-251`).
5. **Package hints bypass the silent-channel and app-on-screen filters** (`NRS:947-949`, `:987`, `:1011`). Every notification from apps such as Uber, Delta or United, and from the Google app (`googlequicksearchbox` is in the sports hints), pops the island, including silent ones and marketing pushes.
6. **Navigation looks like a download:** no maneuver arrow, distance or instruction layout.
7. **The call card is thin:** no mute/speaker (CallStyle's custom actions), no video badge or verification text, screening calls are wrong, and there is no ongoing-call duration in the compact view when `when` is missing.
8. **Live Updates are only partly used:** the `when` countdown, ProgressStyle points, segment colours, tracker icon, and `hasPromotableCharacteristics()` are all ignored.
9. **Lock-screen privacy:** with the accessibility overlay, the island draws over the keyguard but ignores `Notification.visibility`, `publicVersion`, and the lock-screen visibility in the ranking.
10. **Bluetooth:** no device name, type or battery, and watches that reconnect often keep re-triggering it.
11. **Weather and network truthfulness:** fabricated data and a hard-coded "5G".
12. **Two sources of priority truth**, and ringing alarms are not top tier in the compact order.

**P2**

13. No overflow indicator when 3 or more activities are running.
14. Screen-recording false positives (see section 3).
15. No fullscreen/immersive policy, and landscape is either all or nothing.
16. Dead code:
    - The manifest receiver for `ACTION_POWER_CONNECTED`, which apps cannot receive in the manifest since API 26; it works only because the service registers it at runtime.
    - FPS remnants.
    - The *exported* debug receiver (`IslandOverlayService.kt:479-496`) lets any app inject fake cards. Hand this to the security agent.
17. A dismissed card returns when its text changes (`IslandStateManager.kt:552`), so dismissed ride cards come back on every update.
18. The recorder waveform opens Droidbubble's own `AudioRecord` (`AudioLevels.kt:273`). That attributes the mic indicator to Droidbubble, and the audio is probably silenced: the service is a `specialUse` FGS, not a microphone one.
19. Missing activities:
    - Low battery, full charge, time to full.
    - Headset battery.
    - VPN, DND/Modes, next alarm.
    - Mic/camera in use.

## 5. Recommendations

1. **Make live activities their own setting (quick win).**
   - What: a separate `showLiveActivities` setting (default on), apart from "Incoming Notifications". Pass a single gate to both the service and the view.
   - Files: `IslandSettings.kt`, `SettingsRepository.kt`, `NRS:956-960`, `IslandView.kt:463`, `MainScreen.kt:715-835`.
   - Effort S. Risk low.
2. **Unified `LiveActivitySpec` and an `ActivityStore`.**
   - What:
     ```kotlin
     data class LiveActivitySpec(id, kind, source, pkg, tier, relevance, startedAt,
       lastSignificantAt, staleAt, alerting, accent,
       compactLeading: Slot, compactTrailing: Slot, minimal: Slot,
       expanded: ExpandedSpec /* leading, trailing, center, bottom, actions */)
     sealed interface Slot { Icon; Avatar; Text; Timer(base, countDown); Progress(f | indeterminate); Ring; Waveform; Score }
     ```
     The store is a `Map<id, LiveActivitySpec>` that replaces `_activeNotification`, `_activeProgress` and the stash.
   - Why: it fixes P0 #2, makes a new type a single adapter plus slots, and computes the kind once when the notification arrives.
   - How: renderers draw a slot generically and keep the current bespoke rows as custom slot implementations. Migrate one kind at a time behind the existing `IslandPriority`.
   - Files: a new `island/activity/*`, then `IslandStateManager`, `IslandView` (`CompactContent`, `AuxiliaryBubbleContent`, `ExpandedCardContent`), `IslandComponents.cardHeightFor`.
   - Effort L. Risk medium: it touches the morph/split animation keys. Depends on nothing; #3 to #6 build on it.
3. **Tiered relevance arbiter (a pure function, easy to unit-test).**
   - Tiers:
     - **T0:** ringing call, ringing alarm or "Time's up". Always primary, auto-expand.
     - **T1:** ongoing call, navigation, voice/screen recording, running timer, Live Update with an ETA.
     - **T2:** media, downloads, scores, flashlight, VPN/hotspot.
     - **T3:** message banners. These pop in and expand for about 4.5 s, then *recede* to a badge or the secondary bubble; they never cover T1.
     - **T4:** system glances (charging, Bluetooth, ringer, unlock).
     - **T5:** persistent widgets.
   - Within a tier: the user's drag order, then the last significant change (a state change, not a clock tick), then the start time.
   - Display: primary, secondary, and a "+N" overflow dot; swipe cycles through.
   - Delete `currentPriority`. Files: `IslandStateManager`, `IslandView:489-539`. Effort M. Risk medium. Depends on #2.
4. **Adapters that read Android's structured signals first.** Heuristics stay as a fallback.
   - **CallStyle:** call type 3, `isVideo`, verification text, and the two ongoing custom actions mapped to speaker/mute buttons. Effort S.
   - **Live Updates / ProgressStyle:**
     - Promoted or `hasPromotableCharacteristics`.
     - Use `shortCriticalText`, else the `when` countdown, as compact trailing.
     - Segment colours, points and tracker icon as the expanded bottom.
     - Large icon as compact leading.
     - Effort M. Risk: raw extra keys **[M]**, so guard with `runCatching`.
   - **Navigation:** maneuver large icon (leading), distance (trailing), instruction (expanded center). Stop forcing it into PROGRESS. Effort S-M.
   - **Chronometer:** move the existing logic out of `NRS:1056-1074`. Effort S.
5. **Locale-independent control mapping.**
   - Order of preference: (a) structured extras, (b) `Action.getSemanticAction()`, (c) the action icon's resource entry name in the posting package (`ic_pause`, `ic_stop`) **[M]**, (d) regex tables for ar, es, pt, fr, de, tr, ru, hi, id, zh.
   - Files: `LiveActivities.kt:148-219`, `NRS:690-757`, `:1044-1051`. Effort M. Risk: OEM icon naming varies. Add test fixtures per locale.
6. **Harden the heuristics.**
   - Score regex: require a sports package, or a hyphen/en-dash separator; never `:` next to AM/PM.
   - Check TRACKING before SPORTS.
   - Remove `googlequicksearchbox` from the sports hints.
   - Package hints should only bypass the filters for *ongoing* notifications.
   - Match "end" with word boundaries.
   - Screen recording: drop the bare title-only `||` clauses.
   - Effort S. Risk low.
7. **Lock-screen privacy.**
   - While `KeyguardManager.isKeyguardLocked`, render `publicVersion` or "app name + New notification" for `VISIBILITY_PRIVATE`, hide `VISIBILITY_SECRET`, and honor the ranking's lock-screen visibility. Live activities keep showing (system UI shows Live Updates on the lock screen too).
   - Files: `NRS` (carry visibility and public text into the model), `IslandView`. Effort S.
8. **Screen-recording callback** on API 35+ (`DETECT_SCREEN_RECORDING`), with text matching as the fallback. Files: `IslandOverlayService`, manifest. Effort S. Risk: whether overlay windows count **[M]**.
9. **New system sources** (T2/T4):
   - Low battery 20%/10%, full charge, and charge ETA via `computeChargeTimeRemaining`.
   - Bluetooth: device name, type icon and battery, with a per-device cooldown (e.g. 10 min) so watches don't spam.
   - VPN on/off, DND/Modes, next alarm in the evening.
   - Mic/camera in use (see section 6).
   - Files: `IslandOverlayService` receivers, or a new `SystemActivitySource`. Effort M.
10. **Truthful widgets.**
    - Weather: label the location as approximate or use coarse location, drop the synthetic hourly entries, and show "unavailable" instead of fake data.
    - Network: derive the cellular label (or just "Cellular").
    - Effort S.
11. **Fullscreen/immersive policy.** When the foreground window hides the status bar (accessibility `getWindows()` / insets **[M]**), shrink to a minimal dot or hide anything below T0/T1. Effort M.
12. **Housekeeping.**
    - Reconsider the "unchanged text" check for dismissed cards: keep a dismissal until a *state* change.
    - Remove the dead receiver and FPS code.
    - For the waveform, use `AudioRecordingCallback` activity or the static wobble rather than opening the mic.
    - Effort S.

## 6. Droidbubble's own take (Android-only, deliberately not iOS)

- **A full renderer for Live Updates.** The system status chip is limited to 96 dp. Droidbubble can draw the whole ProgressStyle journey (coloured segments, milestones, tracker icon), on every app screen, on non-Samsung phones, and on older Android via heuristic adapters.
- **"Pin as activity":** long-press any ongoing notification (Pomodoro, workout, VPN timer, build runner) to promote it. A user-defined promotion iOS cannot offer; it reuses the generic Slot renderer.
- **Real actions:** Android hands over the PendingIntents, so keep inline reply, smart-reply chips (`RemoteInput.choices`) and the CallStyle custom actions as first-class features.
- **Multi-session media switcher:** swipe between active MediaSessions (e.g. podcast and YouTube). iOS shows only one.
- **Privacy pill:** "Mic in use" or "Camera in use", from `AudioRecordingCallback` / `CameraManager.AvailabilityCallback`. Tapping opens the privacy dashboard. Original and needs no permission.
- **Battery for every Bluetooth device** (earbuds, watch, controller), not just earbuds.
- **Accessibility first:** spoken alerts (already in place), per-tier haptic patterns, high-contrast mode.
- **Transparent arbitration:** show the user why an activity is primary ("Tier: live call"), with per-app tier overrides, rather than a black box like iOS.
- **Deliberately different from iOS:** message banners recede instead of hiding a live activity; there is no 8-hour cap; overflow shows "+N" instead of silently dropping activities.

## 7. Proposed activity catalogue

| Activity | Detection source | Compact leading | Compact trailing | Minimal | Expanded | Tier |
|---|---|---|---|---|---|---|
| Incoming call | CallStyle type 1 / `CATEGORY_CALL` + answer | Avatar | Pulsing phone icon | Avatar | Name, verification text, Decline/Answer | T0 |
| Ringing alarm / Time's up | `CATEGORY_ALARM` + full-screen intent / clock app | Alarm or timer icon | "Time's up" / time | Icon | Headline, overtime, Snooze or +1:00, Stop | T0 |
| Ongoing call | CallStyle type 2 + chronometer | Green phone icon | Call timer | Phone icon | Avatar, timer, mute/speaker (custom actions), hang up | T1 |
| Navigation | `CATEGORY_NAVIGATION` / promoted Maps | Turn arrow (large icon) | Distance or `shortCriticalText` | Arrow | Instruction, ETA, progress | T1 |
| Voice / screen recording | Chronometer + recorder package; API 35 callback | Waveform / red dot | Elapsed time | Red dot | Time, waveform, pause, stop | T1 |
| Timer / stopwatch | Chronometer (countdown flag) / custom view | Countdown ring / clock | Remaining or elapsed | Ring | Label, time, cancel, pause | T1 |
| Ride / delivery / flight | Promoted, or ProgressStyle + ongoing | App icon | ETA (`shortCriticalText` / `when`) | App icon | Status, segment bar + points, ETA | T1 |
| Media | MediaSession | Album art | Equalizer | Art | Art, title, scrubber, controls, session switcher | T2 |
| Download / progress | Progress extras | App icon | % or spinner | Ring | Title, detail, bar, actions | T2 |
| Live score | Sports package + strict regex / promoted | Sport icon | "2–1" | Score | Teams, score, status | T2 |
| Flashlight | TorchCallback (filtered) | Torch icon | — | Torch | Brightness and off | T2 |
| VPN / hotspot | `TRANSPORT_VPN` / AP state | Shield / hotspot icon | "VPN" | Icon | Network name, disconnect via the app | T2 |
| Mic / camera in use | Recording / camera availability callbacks | Mic or camera icon | Orange or green dot | Dot | "In use", open privacy settings | T2 |
| Message banner | Listener, importance ≥ default | Sender avatar | Title | Avatar | Text, reply, smart replies, actions | T3 (recedes) |
| Charging / low / full | Battery broadcasts | Bolt / battery icon | % + watts / ETA | — | — (glance) | T4 |
| Bluetooth connect | ACL + `EXTRA_DEVICE` + battery | Device-type icon | Name / battery % | — | — (glance) | T4 |
| Ringer / DND / Mode | Ringer + interruption filter | Bell / moon icon | Mode name | — | — (glance) | T4 |
| Unlock | `USER_PRESENT` | Lock icon | — | — | — | T4 |
| Weather / network speed | Polled (screen on) | Condition / Wi-Fi icon | Temperature / ↓ speed | Icon | Details (honest data only) | T5 |

Most important files for the follow-up work:
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\island\IslandStateManager.kt`
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\island\LiveActivities.kt`
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\island\IslandView.kt`
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\service\NotificationReceiverService.kt`
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\service\IslandOverlayService.kt`
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\data\SettingsRepository.kt`
- `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\service\PersistentWidgetTelemetryManager.kt`
