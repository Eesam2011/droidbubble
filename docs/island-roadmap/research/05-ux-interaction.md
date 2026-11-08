# UX & Interaction audit: Droidbubble

Base path: `C:\Users\oppoe\OneDrive\Desktop\Droidbubble\app\src\main\java\com\droidbubble\app\`. I read the working tree only and changed nothing. Correction to the brief: `app/build.gradle.kts` sets **minSdk = 29**, not 33. So the haptic constants added in API 30 and API 34 need version guards.

## 1. Reference: Apple, and the Android conventions it should be adapted to

**Apple HIG for Live Activities** ([JSON source of the HIG page](https://developer.apple.com/tutorials/data/design/human-interface-guidelines/live-activities.json)). These are high-confidence unless marked otherwise.
- **Tap** opens the app at the relevant detail, not its home screen ("Take people directly to related details and actions").
- **Touch and hold** shows the expanded presentation.
- **Alerts** light the screen, play a sound and show the expanded view. Apple says to use them only for essential updates. The HIG does not document how long the alert stays up; my observation of "a few seconds" is low confidence.
- **Animations** are capped at 2 s. Nothing animates on the reduced-luminance Always-On display.
- **Multiple activities** fall back to minimal presentations. Apple prefers one dynamic activity over several.
- **Sensitive content** should be redacted, or shown as a harmless summary, because the island is visible to other people.
- **Gestures** (medium confidence, from [Six Colors](https://sixcolors.com/post/2022/10/using-gestures-inside-the-dynamic-island/) and [iMore](https://www.imore.com/iphone/you-can-swipe-your-way-around-the-dynamic-island)):
  - Swiping toward the centre minimises or hides an activity; swiping outward restores it.
  - Swiping inward from the secondary bubble hides it.
  - What a swipe does depends on where it starts.
  - Some system activities, such as screen recording and navigation, cannot be hidden.

**Android conventions**
- **Haptics** ([haptic feedback](https://developer.android.com/develop/ui/views/haptics/haptic-feedback), [principles](https://developer.android.com/develop/ui/views/haptics/haptics-principles), [constants](https://developer.android.com/reference/android/view/HapticFeedbackConstants)). High confidence.
  - Prefer `View.performHapticFeedback`, which respects the user's touch-feedback setting.
  - Avoid `createOneShot` and `createWaveform`. For touch feedback, no haptic is better than a buzzy one.
  - Make strength match how important and how frequent the event is.
  - API 30 adds `CONFIRM`, `REJECT`, `GESTURE_START` and `GESTURE_END`. API 34 adds `TOGGLE_ON/OFF`, `GESTURE_THRESHOLD_ACTIVATE/DEACTIVATE`, `SEGMENT_TICK`, `SEGMENT_FREQUENT_TICK` and `DRAG_START`.
- **Heads-up notifications** (high confidence): swipe up tucks the notification into the shade, swipe sideways dismisses it, and the back gesture closes a bubble.
- **Android 16 Live Updates** (medium confidence, [Android Authority](https://www.androidauthority.com/android-16-qpr1-live-updates-3573399)): tapping the status-bar chip reveals the full notification. It does not open the app.
- **Accessibility timing:** `AccessibilityManager.getRecommendedTimeoutMillis()` (API 29) returns the user's "Time to take action" preference. High confidence.
- **Competitor pain points** (low to medium confidence, [XDA thread on dynamicSpot](https://forum.xda-developers.com/t/app-dynamicspot-iphone-14-pro-dynamic-island-notifications-for-android.4496049/post-87817161), [Tom's Guide](https://www.tomsguide.com/news/this-android-app-copies-the-iphone-14-pros-dynamic-island-and-i-just-tried-it)): popups that flick small, then big, then small again; expanded cards that can't be dismissed; "just a notification bar with long-press".

## 2. Droidbubble today

**Gesture map (`island/IslandView.kt`)**

| State | Gesture | Result |
|---|---|---|
| Compact pill | Tap | Expands the primary activity (1878-1887). Ignored for unlock, Bluetooth and ringer alerts. When idle, a bounce plays (1888-1933). |
| Compact pill | Long-press | Opens the app (1936-1949). For notifications this also dismisses the card. |
| Compact pill | Swipe right more than 32dp | **Hides the island** (1866-1869). |
| Compact pill | Swipe left | Shrinks it into the camera ring (1870-1872). |
| Ring | Tap or swipe right | Restores (1862, 1882). |
| Compact pill | Vertical swipe | Nothing happens, and the status bar does not get the touch. |
| Bubble | Tap / long-press | Expands the secondary activity / opens its app (1713-1732). |
| Expanded | Swipe up from the pill zone | Collapses (1286-1304). It fires on a **single-event delta of less than -15px**, so slow drags never collapse. |
| Expanded | Sideways on a card | Dismisses media, notification or progress cards. The threshold is a **fixed 160px**, with no velocity check and no fling-out (1342-1383, 1516-1558). Flashlight and persistent cards follow the finger but do nothing. Ringing calls are **not guarded**: a sideways swipe takes the Answer button away (1353-1358). |
| Expanded | Tap outside | Collapses, and the tap is swallowed (1250-1262). |
| Expanded | Back | Not handled. The window is not focusable (`service/IslandOverlayService.kt:606`), so the app underneath goes back instead. |

Other gesture findings:
- **Open-app target:** `launchAppOrIntent` tries the launcher intent first and the notification's own intent second (2351-2366). Long-press and the avatar therefore land on the app's home screen, not the chat. The body tap does the opposite and uses the notification's intent first (3710-3740).
- **Smart replies:** when the app supplies no suggestions, the card invents "OK", "Sure" and "Thanks!" chips. They send on one tap and are about 28dp tall (3830-3845).

**Touch region**
- The compact window is a touchable rectangle of (width + 80dp) × (top offset + height + 4dp), which is about **260×52dp** at the defaults (`IslandOverlayService.kt:985-999`).
- `IslandTouchManager` was written to pass touches through everywhere except the pill, but **it is never instantiated** (the field exists only at `IslandOverlayService.kt:573`).
- Result: status-bar icons near the cutout, and pulling the shade down from the centre, stop working whenever an activity is showing.
- When expanded, the window becomes full-screen and touchable (928-937).

**Haptics**
- The in-app haptics toggle routes everything through three helpers:
  - `CONTEXT_CLICK` (1198-1202) is used for five different meanings: expand, dismiss, hide, minimise and restore.
  - `CLOCK_TICK` is used for collapse (1204-1208).
  - `LONG_PRESS` is used for long-press (1210-1214).
- Charging uses `LONG_PRESS`, which feels heavy for a passive system event (1183-1187).
- Switching to ring mode plays a raw `createWaveform`, which Android discourages and which bypasses the touch-feedback setting (801-828).
- The flashlight slider and the flashlight-off button **ignore the in-app toggle** (3081, 3096, 3265).
- The dismiss haptic fires on release, not when the swipe crosses its threshold.

**Timing**

| Event | Duration | Location |
|---|---|---|
| Auto-collapse after last touch (paused while touching, typing a reply or a call is ringing) | 3 s | `IslandView.kt:1221-1226` |
| Optional notification auto-dismiss | 3 s | `IslandStateManager.kt:604-611` |
| Pop-in | 4.5 s | `IslandStateManager.kt:664` |
| Unlock | 2.2 s | `IslandStateManager.kt:421` |
| Connection | 3 s | `IslandStateManager.kt:439` |
| Ringer | 2.5 s | `IslandStateManager.kt:451` |
| Charging | 4 s | `IslandStateManager.kt:463` |

- None of these scale with `getRecommendedTimeoutMillis`.
- Media and a 10-line notification get the same 3 s.
- Animation-speed default is 0.7× (`model/IslandSettings.kt:41`).

**Context behaviour**
- **Landscape:** hidden by default. With the setting on, the island still sits top-centre, not next to the camera (`IslandOverlayService.kt:659`).
- **Fullscreen and immersive:** there is no detection anywhere. The island draws over portrait video, games and camera apps, and stays touchable.
- **Keyboard:** handled only for the inline reply, which makes the window focusable (789-801).
- **Lock screen:** there is no redaction (no handling of notification visibility). Full text and reply controls appear over the keyguard. That the accessibility overlay draws over the keyguard is medium confidence.
- **Screenshots and recordings:** the island appears in them.
- **Split-screen:** `foregroundPackage` follows only the last window event (`service/IslandAccessibilityService.kt:27-33`).

**Accessibility**
- Pill, bubble and tap-outside backdrop have no labels, no click labels and no custom actions (1251-1261, 1713, 1878).
- Icon labels are generic ("Media Art", "Notification Icon").
- Hide, minimise and dismiss are reachable by swipe only.
- The 3 s collapse cuts off TalkBack users, because swipe navigation sends the overlay no pointer events.
- Font scale is clamped to 0.85–1.5 (`IslandOverlayService.kt:713`).
- The system high-contrast flag is read once inside `remember{}`, so changing it needs a restart (683-687).
- The reply placeholder (#71717A on #212121) is about 3.3:1, below the 4.5:1 requirement.
- Reply Send and Cancel are 32dp and 28dp and sit next to each other (3905, 3920).
- Spoken Alerts uses its own TTS and can talk over TalkBack (`service/SpokenAlerts.kt`).
- There is no reduce-motion mode. The bell shake (830-845), card stretch (1323-1327), idle bounce and bubble shoot (1159-1181) always run. Compose likely does honour "Remove animations" (medium confidence).

**Companion app (`ui/main/MainScreen.kt`)**

Order of the main screen:
1. "Required Setup" (373-440)
2. Enable switch
3. A **large primary-coloured Update banner** that is always shown (463-565)
4. Links to three sub-pages
5. Island Activities, with drag-to-reorder and toggles
6. Priority Apps
7. System Alerts
8. A Test Live Activities panel
9. A disabled "Coming soon" section (989-1040)

Problems:
- **Permissions:** rows show titles only. The descriptions in `PermissionHelper.kt` are never displayed.
  - All six are styled as required in error red, including the optional microphone and battery items.
  - The accessibility service is labelled "Status Bar Touch Override", but it also reads which app is in the foreground. Users need to be told that.
  - There is no help for Android 13+ "restricted settings" on sideloaded installs.
  - The Bluetooth permission prompt fires on cold launch (`MainActivity.kt`).
- **Defaults:** notifications are **off by default** (`IslandSettings.kt:27`).
- **Unreachable or dead UI:**
  - The per-app notification filter sheet can never be opened (it is defined at 1053, but nothing sets it to show).
  - `ShizukuHelper.setSystemStatusBarIconsHidden` has no UI. It writes the global `policy_control` setting, so it must restore the setting if it is ever exposed.
- **Dimensions:** six raw sliders, with no cutout auto-fit, no reset and no preview. The pill is invisible while you adjust it unless something is active (1838-1912).
- **Page grouping:**
  - Animation speed is on Dimensions, but raw damping and stiffness editors are on Appearance (2302).
  - The Accessibility page holds behaviour toggles and the crash report (2130-2256).
- **Reorder handle:** about 24dp, with no move-up or move-down actions for TalkBack (774-830).

## 3. Gaps

**P0**
1. **Shade and status-bar conflict.** A dead touch zone of about 260×52dp sits over the status bar, and a downward swipe on the pill does nothing.
2. **No fullscreen policy.** The island covers portrait video, games and camera apps and stays touchable.
3. **TalkBack.** Elements have no labels, swipe-only actions have no alternative, and the 3 s timeout collapses the card while the user is still reading.

**P1**
1. Swipe right is a hide trap. The window turns not-touchable, and the island only comes back on the next event (`IslandStateManager.kt:83-98`, `IslandOverlayService.kt:985-989`). Directions are not mirrored for right-to-left languages.
2. Gestures are unreliable: the swipe-up delta bug, a pixel threshold with no velocity check, and no fling-out.
3. Back does not collapse the island.
4. Opening from the island lands on the app's home screen instead of the conversation.
5. Accidental-action risks: invented reply chips, and a ringing call can be swiped away.
6. The haptics map is inconsistent, the raw waveform is used, and some haptics ignore the toggle.
7. One fixed timeout is used for everything, with no accessibility scaling.
8. There is no redaction on the lock screen.
9. Onboarding: no explanations, no difference between required and optional, notifications off by default, the app filter is unreachable, no camera auto-fit, no preview.

**P2**
1. Landscape anchoring.
2. Reduce motion.
3. High contrast is not reactive, and placeholder contrast is too low.
4. The font clamp also applies to the expanded cards.
5. Settings layout: the Update banner is too loud, raw spring parameters are exposed, the Coming-soon section is clutter.
6. Accessibility of the reorder list.
7. TTS can overlap TalkBack.
8. The island shows in screenshots.

## 4. Recommendations

These are principles adapted to Android, not a copy of iOS.

| # | What and how | Files | Effort / risk |
|---|---|---|---|
| R1 (P0) | **Pass touches through outside the pill, and make the shade reachable.** Only the pill and bubble rectangles should take touches: hook up `IslandTouchManager` (rects from `onGloballyPositioned`). If the hidden API it relies on is blocked on API 34–36, fall back to resizing the window to exactly the pill and enlarging it only while it morphs. A **downward swipe on the pill** expands it; pulling further than about 2× calls `performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)` on the accessibility service. | `IslandOverlayService.kt` (674-724, 982-999), `IslandTouchManager.kt`, `IslandView.kt` (1850-1951) | M / Medium (hidden API) |
| R2 (P0/P1) | **One gesture detector** (`Modifier.islandGestures`). Use `awaitEachGesture` with a `VelocityTracker` and lock to one axis after touch slop. Thresholds in dp: about 48dp of travel, or fling velocity from `ViewConfiguration`. Inward/outward is measured from the centre and works right-to-left. Haptic on crossing the threshold. Hand the release velocity to an `Animatable` so a dismissed card flies off and a cancelled one springs back. Child controls (sliders, buttons) keep first claim on the touch. | `IslandView.kt` (1286-1383, 1516-1558, 1850-1877) | M–L / Medium |
| R3 (P0) | **Fullscreen policy.** Watch status-bar visibility through the overlay's insets (`isVisible(statusBars())`). The accessibility-window approach is more robust but needs content-retrieval permission. Setting "In fullscreen apps": Hide / Calls & alarms only (default) / Always. Treat apps with `ApplicationInfo.CATEGORY_GAME` and the camera app as fullscreen. | `IslandOverlayService.kt` (`updateWindowState`), `IslandAccessibilityService.kt`, `IslandSettings.kt`, `SettingsRepository.kt` | M / Medium (OEM insets vary) |
| R4 (P0) | **TalkBack semantics.** The pill gets one summary label (`clearAndSetSemantics`) with click labels "Expand" and "Open ⟨app⟩", custom actions (Minimise, Hide until next update, Dismiss) and a polite live region. The expanded card gets a `paneTitle`. The backdrop gets "Close island". Pause auto-collapse while accessibility focus is inside, by wrapping the ComposeView in a FrameLayout that overrides `onRequestSendAccessibilityEvent`. Skip the TTS when TalkBack is on. | `IslandView.kt`, `IslandOverlayService.kt:674`, `SpokenAlerts.kt` | M / Low |
| R5 (P1) | **Haptics token map** in a new `island/IslandHaptics.kt`, with SDK fallbacks. Every call goes through it and respects the in-app toggle. No raw waveforms; system alerts get no haptic by default. | `IslandView.kt` (801-828, 1183-1214, 1891, 3081, 3096, 3265) | S / Low |
| R6 (P1) | **Timing tokens** (`IslandTimings`):<br>• User-expanded card: 6 s; media 8 s.<br>• Peek: 4–5 s, similar to a heads-up (medium confidence).<br>• Calls and alarms: no timeout.<br>• Everything scaled by `getRecommendedTimeoutMillis(…, FLAG_CONTENT_TEXT\|CONTROLS)`.<br>• Transient system alerts stay at 2–3 s and live in one place. | `IslandView.kt:1221`, `IslandStateManager.kt` (421-664) | S / Low |
| R7 (P1) | **Back collapses the island.** While expanded, drop `FLAG_NOT_FOCUSABLE` (same path the reply uses, 789-801). Register `OnBackInvokedCallback`, and on API 34+ `OnBackAnimationCallback` to drive a predictive-back shrink. Restore the flag on collapse. | `IslandOverlayService.kt`, `IslandView.kt` | M / Medium (takes focus, which may close the underlying app's keyboard) |
| R8 (P1) | **Correct targets and safe actions.**<br>• Use the notification's own intent first for notifications and the media session's activity first for media (2345-2386); route the avatar the same way.<br>• Show reply chips only when the app supplied them (3830).<br>• A ringing call or alarm tucks back to compact on a sideways swipe instead of being dismissed.<br>• On the lock screen, follow the notification's visibility and the system's private-content setting, and hide reply and actions. | `IslandView.kt`, `IslandStateManager.kt`, `NotificationReceiverService.kt` | S–M / Low |
| R9 (P1) | **Rethink hide.** Swipe right no longer hides. Swiping inward minimises to a ring that stays tappable; swiping outward restores. Hiding completely becomes a custom action, a Quick Settings tile, or a foreground-notification action ("Pause island 1 h"), and resumes on a timer. | `IslandStateManager.kt:75-98`, `IslandView.kt:1850-1877`, `IslandOverlayService.kt:954-989` | S / Low |
| R10 (P1) | **Guided onboarding and new settings layout.**<br>• Stepper with explanations, Required vs Optional, disclosure for the accessibility service, and a helper for restricted settings (opens App info).<br>• Bluetooth permission asked when the Bluetooth toggle is switched on.<br>• Ask in onboarding which apps to show notifications for.<br>• **Auto-fit to the camera** using `DisplayCutout.boundingRects`.<br>• **Live preview:** while Dimensions or Appearance is open, post a sample activity through `LiveActivityTester` so the pill is visible.<br>• Reset buttons; make the app filter reachable. | `MainScreen.kt`, `PermissionHelper.kt`, `MainActivity.kt`, `MainScreenViewModel.kt`, `LiveActivityTester.kt`, `IslandSettings.kt` | L / Low |
| R11 (P2) | **Reduce motion and contrast.** Reduced motion turns on when the system animator scale is below 1 or the user enables it. It turns off overshoot, card stretch, bell shake, bounce and bubble shoot, and uses a 150ms crossfade instead. Watch high contrast with a ContentObserver; raise placeholder contrast to at least 4.5:1. | `IslandView.kt`, `IslandOverlayService.kt:683`, `IslandTokens.kt` | S–M / Low |
| R12 (P2) | **Landscape:** anchor to the cutout for each rotation. **Reorder list:** add Move up / Move down accessibility actions. **Expanded cards:** allow font scale up to 2.0. | Service, `MainScreen.kt` | M / Low |

Dependencies: R2 underpins R4's actions, R7 and R9. R5 and R6 should land first because they are small and the others call them.

## 5. Droidbubble's own take: where to deliberately differ from iOS

- **Tap expands, long-press opens the app.** This is the reverse of Apple. It matches the Android 16 Live Update chip, where tapping reveals the notification, and the user is never thrown out of the current app by accident. Keep it, but make long-press open the exact conversation.
- **Follow heads-up notification grammar on expanded cards.** Swipe up tucks the card back, swipe sideways dismisses it, and back or a predictive-back gesture collapses it. iOS has no back gesture to match.
- **A downward swipe on the pill expands it, and pulling further opens the system shade.** This keeps Android's top-edge muscle memory instead of blocking it.
- **Use Android's haptic meanings**, including the threshold haptics, rather than copying iOS taptics. Stay quiet for passive system events.
- **Android-only controls:** a Quick Settings tile and a notification action for "Pause island", a "calls & alarms only" mode in fullscreen and games, Do Not Disturb awareness (only priority pop-ins during Do Not Disturb), a Material You accent option, and TalkBack custom actions plus live regions.
- **Lock-screen behaviour follows Android's own private-content setting**, not Apple's rules.

## 6. Proposed interaction spec

Haptic tokens from R5, motion tokens built on the existing springs:

| State | Gesture | Result | Haptic | Motion |
|---|---|---|---|---|
| Compact, single | Tap | Expand | VIRTUAL_KEY | expand (0.8 / 380) |
| Compact | Long-press | Open the exact conversation or detail | LONG_PRESS | collapse |
| Compact | Swipe down 24–64dp | Expand | GESTURE_THRESHOLD_ACTIVATE (fallback CLOCK_TICK) | expand, using the swipe's velocity |
| Compact | Swipe down past 64dp | Open the system shade | GESTURE_END | collapse |
| Compact | Swipe inward | Minimise to the ring | Threshold, then GESTURE_END | split (0.63 / 320) |
| Ring | Tap or swipe outward | Restore | VIRTUAL_KEY | appear (0.6 / 280) |
| Dual: bubble | Tap / drag into pill | Expand secondary / swap primary and secondary | VIRTUAL_KEY / SEGMENT_TICK | split |
| Expanded | Swipe up, or tap outside | Tuck back to compact | CLOCK_TICK | collapse, using the swipe's velocity |
| Expanded | Swipe sideways | Dismiss the card (calls and alarms tuck instead) | Threshold, then GESTURE_END | fling-out (decay) or snap-back (spring) |
| Expanded | Back | Collapse | none | predictive shrink |
| Expanded | Toggle or slider | Act | TOGGLE_ON/OFF; SEGMENT_TICK; FREQUENT_TICK while seeking | none |
| Any | Reply sent / launch failed | Confirm / Reject | CONFIRM / REJECT | none |
| Any, TalkBack | Double-tap or custom actions | Expand, open, minimise, hide, dismiss | (system) | reduced |

Reduced motion replaces every motion token above with a 150ms crossfade and removes overshoot.

**Proposed companion-app layout**
1. **First-run setup:**
   1. Welcome, with the live island
   2. Essentials: overlay and accessibility (disclosure, restricted-settings helper)
   3. Notification access and which apps to show
   4. Calibrate to the camera (auto-fit, then nudge)
   5. Optional extras: battery, microphone, Bluetooth
2. **Home:** health and status card, master switch, "Show sample" preview, quick toggles.
3. **Activities:** media, notifications (app filter, priority apps), progress, calls and timers, recording, flashlight, system alerts, priority order.
4. **Look & feel:** style, size and position (live preview, auto-fit, reset), glass, outline, album art, motion presets (Calm / Default / Lively) with advanced curves collapsed.
5. **Behaviour:** fullscreen and games, landscape, timeouts, auto-dismiss, dismiss on pause, haptics.
6. **Accessibility:** high contrast, spoken alerts, reduce motion, longer timeouts.
7. **About:** quiet update row with a badge when an update exists, feedback, crash report, Test lab (the current test panel).
