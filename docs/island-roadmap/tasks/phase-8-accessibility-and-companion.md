# Phase 8: Accessibility and small companion fixes

**Goal.** Make the island usable with TalkBack and large text. Make setup honest and self-explanatory. Give the companion app a few targeted fixes. All of this happens **inside the current settings structure** (PLAN.md v2 §0.1 keeps the settings app's structure). Nothing here restyles the island or the companion app.

**Epics:** C1 permission and onboarding fixes · C2 live preview and resets · C3 island TalkBack · C4 QS tile, "Pause island 1 h", DND awareness · C5 small settings fixes.

**Sources:** `research/05-ux-interaction.md` §2 (companion and a11y findings), R4 (TalkBack), R9 (rethink hide), R10 (onboarding), R11, R12; `research/01-typography.md` finding 9, R7 (chip `tnum` only) and R8 (font scale); `research/06-performance-architecture.md` R11 (debounced settings writes, which B8 owns); `research/02-visual-design.md` R1 (cutout calibration, which V1 owns).

**Baseline:** `island-polish` working tree, read 2026-11-08. Every `path:line` below was checked against that tree. Paths are relative to `app/src/main/java/com/droidbubble/app/` unless they start with `app/`.

### Order and parallelism, in short
- **Start any time, even before Phase 2:**
  - C1 (companion only)
  - C5 (companion only)
  - C3.1 (test harness and device spike)
  - C3.2, C3.3 (new pure files)
  - C3.9 (SpokenAlerts)
  - C4.1–C4.3 (service plus a new TileService)
  - C2.1 (LiveActivityTester only)

  None of these touches the island shell.
- **Waits for foundations:**
  - C2.2 needs B8 (debounced slider writes).
  - C2.4 needs V1.
  - C3.8 needs F3 (`IslandTimings`) and F5 (window controller).
  - C4.4 should land after B10 (NRS parsing moves off-main), or inside A2 if the arbiter is already in.
- **Waits for the shell (Phase 3):** C3.4–C3.7 edit the pill, bubble, card and backdrop modifiers in `IslandView.kt`, which M1, M2 and M4 rewrite. Run them **after M4**. C3.10 needs F4, V4 and M3.
- **File contention:**
  - **`ui/main/MainScreen.kt`** (2476 lines) is edited by C1.2–C1.6, C2.2–C2.4, C4.2, C4.3, C5.2–C5.6, plus D5 (curve editor) and V1 (Dimensions page). Either run those tasks one at a time, or do **C5.1** (a mechanical file split) first.
  - **`service/IslandOverlayService.kt`** is edited by C3.2, C3.8, C3.10, C4.1 and C4.2. Coordinate with B1, B8 and F5.
  - **`island/IslandView.kt`:** see PLAN.md §5. Only one writing session at a time.

### Where each task lands in the current companion app

| Area (current code) | Lines | Tasks |
|---|---|---|
| `MainScreen()` state, launchers, `ON_RESUME` re-check | `ui/main/MainScreen.kt:198-273` | C1.2 (runtime launchers), C1.4 (attempt tracking) |
| Main `Scaffold` (no `snackbarHost` today) | `MainScreen.kt:339-363` | C1.6, C2.3 |
| §1 "Required Setup" permission rows (title only, all error red) | `MainScreen.kt:372-440` | C1.2, C1.3, C1.4, C1.6 |
| §2 Enable Island master switch | `MainScreen.kt:442-461` | C4.2 (paused state), C4.3 (add-tile row) |
| Update banner (always primary-coloured) | `MainScreen.kt:463-565` | C5.3 |
| Links to Dimensions / Appearance / Accessibility | `MainScreen.kt:567-630` | (unchanged) |
| Island Activities, drag-to-reorder plus toggles | `MainScreen.kt:632-850` | C5.6, C5.2 (filter row) |
| Priority Apps row | `MainScreen.kt:852-886` | C5.2 (sibling row) |
| System Alerts (Bluetooth row `901-917`) | `MainScreen.kt:888-973` | C1.5 |
| Test Live Activities panel | `MainScreen.kt:975-984` | (C2 reuses its `islandRunning` rule, `:983`) |
| "Persistent Island Views · Coming soon" | `MainScreen.kt:986-1041` | C5.5 |
| Per-app filter sheet (never opened) | `MainScreen.kt:1052-1131` | C5.2 |
| Priority apps sheet | `MainScreen.kt:1133-1229` | — |
| Update sheet / Feedback sheet | `MainScreen.kt:1231-1374` / `:1376-1627` | C5.3 (sheet unchanged) |
| `SectionHeader`, `GroupPosition`, `SettingsCardItem`, `SliderSettingItem` (value chip `:1765-1777`) | `MainScreen.kt:1632-1788` | C2.3, C5.4 |
| `DimensionsPage` (six sliders `1850-1910`) | `MainScreen.kt:1809-1915` | C2.2, C2.3, C2.4 |
| `AppearancePage` (style, glass, outline, toggles, curve editor) | `MainScreen.kt:1919-2125` | C2.2, C2.3 |
| `AccessibilityPage` (auto-dismiss, dismiss on pause, high contrast, spoken alerts, crash report) | `MainScreen.kt:2128-2287` | C3.9 (copy), C4.4 (copy) |
| `AnimationCurveEditor` + `SliderSettingItemPct` (chip `:2471`) | `MainScreen.kt:2290-2476` | C5.4 (chip only; D5 owns the editor) |
| Permission catalogue | `ui/main/PermissionHelper.kt:14-177` | C1.1 |
| Cold-launch Bluetooth request | `MainActivity.kt:49-55` | C1.5 |

---

## C1: Permission and onboarding fixes (inside the current layout)

> **Identity check (all of C1):** use M3 `ListItem`, `AlertDialog` and `Snackbar`, and Android's own setting names ("Display over other apps", "Notification access", "Nearby devices", "Allow restricted settings"). Don't use iOS-style "Allow / Don't Allow" modal sheets. Remove the Apple term "Dynamic Island" from any copy these tasks touch (`ui/main/PermissionHelper.kt:79`).

### C1.1: Permission catalogue: levels and honest copy
- **Goal:** one data source that says, for each permission, how necessary it is, what it does, why it's needed and how to grant it.
- **Why:** rows show titles only, all six are styled as required, and the descriptions in `PermissionHelper` are never displayed (05 §2 "Permissions"; R10). The current microphone copy is also inaccurate: `ui/main/PermissionHelper.kt:150` says the visualizer doesn't use the mic, but the recorder waveform opens `AudioRecord(MediaRecorder.AudioSource.MIC)` (`island/AudioLevels.kt:271-274`).
- **Files:**
  - `ui/main/PermissionHelper.kt:14-20` (`PermissionItem`), `:71-177` (`getPendingPermissions`)
  - call site `ui/main/MainScreen.kt:403`
- **Spec:**
  - `enum class PermissionLevel { REQUIRED, RECOMMENDED, OPTIONAL }`, plus `enum class GrantKind { SETTINGS, RUNTIME, DISCLOSURE_THEN_SETTINGS }`.
  - `PermissionItem(id, title, whatItDoes, whyNeeded, level, grantKind, runtimePermission: String?, restrictedOnSideload: Boolean, isGranted, createIntent)`.
  - Split it into a pure `allPermissions(sdkInt)` list (ordering and copy) plus `pending(context)` (grant checks), so ordering and copy can be unit-tested.
  - Order: REQUIRED, then RECOMMENDED, then OPTIONAL.

  | id | Title | Level | What it does | Why it's needed | How to grant |
  |---|---|---|---|---|---|
  | `overlay` | Display over other apps | Required | Draws the island on top of other apps. | Without it nothing can appear around the camera. | `ACTION_MANAGE_OVERLAY_PERMISSION` (as today, `:82-85`) |
  | `notification_listener` | Notification access | Required | Reads notifications and media playback on this phone. | This is how the island knows about music, calls, timers, rides and messages. Nothing leaves your phone. | API 30+: `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` with `EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME`, which goes straight to our toggle. API 29: the list (as today, `:98-100`). `restrictedOnSideload = true` |
  | `accessibility` | Status bar layer | Recommended | Lets the island sit above the status bar and take touches there, and tells Droidbubble which app is open. | Without it the island draws under the status-bar icons, and an app's own activity (for example the music pill inside your music app) isn't hidden. | C1.3 disclosure, then `ACTION_ACCESSIBILITY_SETTINGS`. Don't use `ACTION_ACCESSIBILITY_DETAILS_SETTINGS`: it needs a privileged permission. `restrictedOnSideload = true` |
  | `battery_optimization` | Unrestricted battery | Recommended | Lets Droidbubble keep running in the background. | Some phones stop background apps to save power; the island then disappears until you open the app again. | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (as today) |
  | `post_notifications` (API 33+) | Droidbubble notifications | Optional | Shows Droidbubble's own notification. | Needed for update alerts and the "Pause for 1 hour" button (C4.2). The island itself works without it. | Runtime `POST_NOTIFICATIONS`. If permanently denied, fall back to `ACTION_APP_NOTIFICATION_SETTINGS` |
  | `record_audio` | Microphone (live visualizer) | Optional | Makes the music equalizer and the recorder waveform follow real sound. | The music equalizer reads the phone's audio output. While a voice-recorder activity is on screen, the waveform also listens to the microphone to draw levels; nothing is recorded or saved. Once A9 removes that mic path, drop the second sentence. | Runtime `RECORD_AUDIO` (existing launcher `MainScreen.kt:239-256`) |
  | `bluetooth_connect` (API 31+, only while the Bluetooth alert is on) | Nearby devices | Optional | Lets the island show when headphones or a car connect. | Android only tells apps about Bluetooth connections with this permission. | Runtime `BLUETOOTH_CONNECT` (added in C1.5) |
- **Steps:**
  1. Add the enums and fields, then move the copy into `allPermissions`.
  2. Keep the `id` strings unchanged. `RECORD_AUDIO_ID` and `"overlay"` are used at `MainScreen.kt:424` and `:983`.
  3. Keep the existing intents, and add the listener detail intent with an SDK guard.
- **Acceptance criteria:**
  - The order is Required, Recommended, Optional.
  - No copy says "Dynamic Island".
  - The mic copy matches the current behaviour of `AudioLevels.kt`.
  - Ids are unchanged.
- **Tests:** new `app/src/test/.../ui/main/PermissionCatalogTest.kt` (pure) checks:
  - level ordering
  - `post_notifications` present only for `sdkInt >= 33`, and `bluetooth_connect` only for `>= 31`
  - every item has a non-blank `whatItDoes` and `whyNeeded`
- **Depends on:** none.
- **Effort:** S   **Risk:** low. It is data only, and the rows still render titles until C1.2.
- **Ships as:** 1 PR, "Permission catalogue with levels and honest copy". Copy change only.

### C1.2: Permission rows: descriptions, levels, "Not now"
- **Goal:** the setup section explains each item, shows red only for what's truly required, and lets users skip optional items without the section nagging forever.
- **Why:**
  - 05 §2 found descriptions never shown, and optional items styled as errors.
  - `POST_NOTIFICATIONS` sends users to a settings screen instead of the one-tap runtime dialog (`ui/main/PermissionHelper.kt:112-123`).
- **Files:**
  - `ui/main/MainScreen.kt:372-440` (section), `:237-256` (mic launcher, to generalise)
  - `ui/main/MainScreenViewModel.kt:117-119`
  - `model/IslandSettings.kt:11-61`
  - `data/SettingsRepository.kt`: keys at `:21-67`, flow mapper at `:69-131`, update mapper at `:143-195`, writer at `:199-243`
- **Spec:**
  - **Header:**
    - "Required setup" with an error badge "N required" while any REQUIRED item is pending.
    - "Finish setup" with a neutral `secondaryContainer` badge "N optional" when only RECOMMENDED or OPTIONAL items are pending.
  - **Row** (keep `Surface` plus `getGroupedShape`):
    - `overlineContent` holds the level label.
    - `supportingContent` holds `whatItDoes` + `whyNeeded`, up to 3 lines.
    - Leading icon: Warning on `errorContainer` for REQUIRED; Info on `secondaryContainer` for the others.
    - Actions sit in a row under the supporting text:
      - `Button("Allow")` for REQUIRED, `FilledTonalButton("Allow")` for the others.
      - `TextButton("Not now")` for non-required items only.
    - The Allow button gets `semantics { contentDescription = "Allow <title>" }` so TalkBack's control navigation isn't ambiguous.
  - **Skip:** "Not now" adds the id to a new persisted `skippedPermissions: Set<String>` (key `skipped_permissions`).
    - Skipped items collapse into one trailing row: "N optional items skipped · Review".
    - "Review" expands them in place for the session.
    - REQUIRED items can never be skipped.
    - Revoking a permission later doesn't un-skip it.
  - **Runtime permissions:** one `rememberLauncherForActivityResult(RequestPermission())` per `runtimePermission`, generalised from `:239-256`. The "don't ask again" fallback opens app settings.
- **Steps:**
  1. Add `skippedPermissions` to `IslandSettings`, and the key to **both** repository mappers and the writer. If B8 already unified the mapper, add it once.
  2. Add `skipPermission(id)` and `reviewSkipped()` to the ViewModel.
  3. Extract `PermissionSection(items, skipped, onAllow, onSkip)` as its own composable, so it can be tested.
  4. Wire the runtime launchers.
- **Acceptance criteria:**
  - On a fresh install, Required rows come first and every row shows its explanation.
  - Microphone and battery are no longer red.
  - A skip survives an app restart.
  - The whole section disappears when nothing is pending; it collapses to one line when only skipped items remain.
  - On API 33+, notifications open the system runtime dialog.
- **Tests:**
  - Robolectric `SettingsRepositoryRoundTripTest`: write and read back `skippedPermissions`, and check every other field is preserved (this guards the writer-line omission).
  - Robolectric Compose test of `PermissionSection`: a REQUIRED row has no "Not now"; "Not now" fires `onSkip(id)`; the Allow button has content description "Allow Display over other apps".
- **Depends on:** C1.1.
- **Effort:** M   **Risk:** med. A new persisted key is the only persistence change; mitigate with the round-trip test, plus defaults so old stores read as an empty set.
- **Ships as:** 1 PR, "Setup section: explanations, required vs optional, skip". User-visible.

### C1.3: Accessibility-service disclosure
- **Goal:** before sending users to Accessibility settings, tell them plainly what the service does and doesn't do.
- **Why:**
  - 05 §2 says the service is labelled "Status Bar Touch Override", but it also reads the foreground app (`service/IslandAccessibilityService.kt:26-38`, `typeWindowStateChanged` in `app/src/main/res/xml/accessibility_service_config.xml`).
  - The system-settings description mentions touches only (`app/src/main/res/values/strings.xml:3`).
  - R10 asks for a disclosure; it is also Google Play's prominent-disclosure rule for non-accessibility-tool services.
- **Files:**
  - `ui/main/MainScreen.kt:422-429` (Grant click)
  - `app/src/main/res/values/strings.xml:3-4`
  - `ui/main/PermissionHelper.kt:162-174`
- **Spec:**
  - `AlertDialog` titled "Turn on Droidbubble's status bar layer?". The body has three short paragraphs:
    1. "**What it does:** lets the island sit above the status bar and respond to touches there, and tells Droidbubble the name of the app on screen so that app's own activity can be hidden while you use it." When M4 ships the pull-to-shade gesture, add: "It also opens the notification shade when you pull down on the island."
    2. "**What it can't do:** read what's on your screen, what you type, or passwords. Droidbubble doesn't send this anywhere."
    3. "You can turn it off at any time in Settings › Accessibility."
  - Buttons: "Not now" and "Continue". Continue opens `ACTION_ACCESSIBILITY_SETTINGS`.
  - `accessibility_service_title`: "Droidbubble status bar layer".
  - `accessibility_service_description`: "Lets the Droidbubble island appear above the status bar and respond to touches there. Droidbubble also learns which app is open (its name only) to hide that app's own activity. It can't read screen content or what you type."
- **Steps:**
  1. Add the dialog state to the section.
  2. Update both strings.
  3. Check every claim against the config: `canRetrieveWindowContent="false"`, and the only event type is window-state changes.
- **Acceptance criteria:**
  - Tapping Allow on the accessibility row always shows the dialog first.
  - The strings in system Settings match the dialog.
  - No claim exceeds what the config allows.
- **Tests:** a Robolectric Compose test: clicking Allow on the accessibility row shows a node with the dialog title, and "Not now" closes it without starting an intent (use `Intents` or a lambda spy).
- **Depends on:** C1.2.
- **Effort:** S   **Risk:** low. Copy has to stay truthful as M4 and A6 add uses; add a code comment next to `accessibility_service_config.xml` pointing at this dialog.
- **Ships as:** 1 PR, "Accessibility disclosure". User-visible.

### C1.4: Android 13+ restricted-settings helper
- **Goal:** sideloaded users who hit "Restricted setting" get the exact steps to unblock it.
- **Why:** GitHub APK installs on Android 13+ get "Restricted setting" for accessibility and notification access, and the app gives no help (05 §2, R10).
- **Files:**
  - `ui/main/MainScreen.kt:258-273` (resume re-check), `:372-440`
  - `ui/main/MainScreenViewModel.kt:117-119`
  - `ui/main/PermissionHelper.kt`
- **Spec:**
  - **Heuristic** (don't probe the hidden app-op): `Build.VERSION.SDK_INT >= 33`, and `packageManager.getInstallSourceInfo(packageName).packageSource` is not `PACKAGE_SOURCE_STORE`.
  - **Trigger:** the ViewModel counts `attempts[id]` when we launch the intent for an item with `restrictedOnSideload`. If on the next `ON_RESUME` that item is still pending and `attempts >= 1`, show inline help under the row:
    - "Android blocked this setting because Droidbubble was installed from a file."
    - "1. Tap **Open App info**. 2. Tap ⋮ (top right) › **Allow restricted settings**. 3. Come back and tap Allow again."
    - "Android only shows that option after it has blocked the setting once."
  - **Button:** `FilledTonalButton("Open App info")`, which opens `ACTION_APPLICATION_DETAILS_SETTINGS`, `package:` URI.
- **Steps:**
  1. Add `isSideloadRestricted(context)` to `PermissionHelper`.
  2. Add the attempt map to the ViewModel; it lives in memory only and resets per process.
  3. Render the help block.
- **Acceptance criteria:**
  - On an API 33+ sideloaded install, a failed attempt shows the help; a store install never does.
  - The help disappears once the item is granted.
- **Tests:**
  - Unit test of the heuristic with a fake `InstallSourceInfo` mapping (pure function of `sdkInt` and `packageSource`).
  - Manual: sideload a debug APK on a Pixel running 14, try notification access, and confirm the help appears and its steps work.
- **Depends on:** C1.2.
- **Effort:** S   **Risk:** low. OEM wording may differ; the copy names the Android setting, and the steps degrade gracefully.
- **Ships as:** 1 PR, "Restricted-settings helper". User-visible on sideloaded installs only.

### C1.5: Ask for Bluetooth only when it's needed
- **Goal:** no permission prompt on cold launch. "Nearby devices" is asked for only when the Bluetooth alert is wanted.
- **Why:** `MainActivity.kt:49-55` requests `BLUETOOTH_CONNECT` on every cold launch while it's missing (05 §2, R10). But `showBluetooth` defaults to `true` (`model/IslandSettings.kt:30`), so after removing the prompt, the alert silently never fires unless the UI says why.
- **Files:**
  - `MainActivity.kt:25-27`, `:49-55`
  - `ui/main/MainScreen.kt:901-917` (Bluetooth row)
  - `ui/main/PermissionHelper.kt` (`bluetooth_connect` entry from C1.1)
- **Spec:**
  - Delete the cold-launch request and `REQUEST_BLUETOOTH_CONNECT`.
  - Bluetooth row, API 31+:
    - **Switching on** without the grant launches the runtime request. If it's denied, the switch stays on and the row shows the supporting text "Needs Nearby devices permission" with a "Allow" `TextButton`.
    - **While on and not granted:** the same supporting text appears, and the OPTIONAL `bluetooth_connect` row appears in the setup section. It is skippable, as in C1.2.
    - **Switching off** removes the setup row.
  - API ≤ 30: unchanged (the `BLUETOOTH` permission is install-time, `AndroidManifest.xml:17`).
- **Steps:**
  1. Remove the code from `MainActivity`.
  2. Add the launcher to the row.
  3. Make `pending()` include `bluetooth_connect` only when `settings.showBluetooth`. Pass the settings in, or filter in the ViewModel.
- **Acceptance criteria:**
  - A fresh launch shows no system dialog.
  - Turning the Bluetooth alert on asks once.
  - Denying it leaves a clear, recoverable state.
- **Tests:** a Robolectric Compose test of the Bluetooth row: with the permission missing and `showBluetooth = true`, it shows "Needs Nearby devices permission". Manual: on a Pixel running 14, turn the alert on, deny, then allow from the row.
- **Depends on:** C1.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Bluetooth permission on demand". User-visible.

### C1.6: Light guided first run (reuses the existing screen)
- **Goal:** first-time users are walked through the two required grants on the main screen itself, with no new stepper screens.
- **Why:** R10 wants guidance, but PLAN v2 C1 allows a guided flow only if it reuses existing screens.
- **Files:**
  - `ui/main/MainScreen.kt:339-363` (add `snackbarHost`), `:372-440`
  - `ui/main/MainScreenViewModel.kt`
- **Spec:**
  - While any REQUIRED item is pending, the REQUIRED rows show "Step 1 of 2" and "Step 2 of 2" in `overlineContent`, in place of the level label.
  - The ViewModel compares the pending list before and after each `checkPermissions` and emits one-shot `SetupEvent`s:
    - **Granted with required still pending:** Snackbar "<title> is on. Next: <next title>" with the action "Allow". The action launches the next item's grant path, including the C1.3 dialog.
    - **Last required just granted:** Snackbar "The island is ready" with the action "Show sample", which calls `LiveActivityTester.showPreview()` (C2.1) for 6 s.
  - The events are session-only, so nothing is persisted.
- **Steps:**
  1. Add a `SharedFlow<SetupEvent>`.
  2. Add a `SnackbarHostState` to the main `Scaffold`.
  3. Collect the flow in a `LaunchedEffect`.
- **Acceptance criteria:**
  - Granting overlay, then notification access, gives two snackbars in order.
  - The second offers a sample that appears on the island.
  - Users who already granted everything see nothing new.
- **Tests:** a unit test of the ViewModel diff (fake pending lists to the expected events). Manual on a fresh install.
- **Depends on:** C1.2. "Show sample" needs C2.1; hide the action until it exists.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Guided first run on the main screen". User-visible on first run.

---

## C2: Live preview while tuning, and reset buttons

> **Identity check:** the preview is the real island showing a neutral "Preview" progress activity in Droidbubble's own design. No mock-up is drawn inside the app.

### C2.1: Preview sample activity
- **Goal:** a static sample that's guaranteed to be visible, that the settings pages can show and clear.
- **Why:**
  - R10 says the pill is invisible while adjusting Dimensions unless something is active (05 §2).
  - The existing demos are notifications, so they're gated by `showNotifications`, which defaults to off (`island/IslandView.kt:463`, `model/IslandSettings.kt:26`).
  - The demos use real packages (Clock, Uber), which hide when that app was the last foreground app (`island/IslandView.kt:460-465`).
- **Files:**
  - `island/LiveActivityTester.kt:18-51`, `:133-139`, `:156-160`
- **Spec:**
  - `fun showPreview()` posts a progress sample through `updateProgress` (the `post()` path at `:133-139`):
    - key `droidbubble.test.preview`
    - `packageName = "com.droidbubble.app"`; the accessibility service never reports our own package as foreground (`service/IslandAccessibilityService.kt:31`), so it's never hidden
    - `appName = "Droidbubble"`, `title = "Preview"`, `text = "Adjusting the island"`
    - `progress = 60`, `maxProgress = 100`, `isOngoing = true`
  - This path is gated only by `showLiveProgress`, which defaults to on.
  - There is no ticker.
  - `fun clearPreview()` removes it, and `clear()` also clears it.
  - `isTest()` already covers the key.
- **Steps:**
  1. Add the two functions.
  2. Make sure `liveActivityKind` returns `NONE` for our package (no hint matches "droidbubble").
- **Acceptance criteria:**
  - With default settings and the island on, `showPreview()` shows a compact progress pill.
  - `clearPreview()` removes it.
  - Real activities are untouched.
- **Tests:** `IslandStateBehaviorTest`-style unit test: after `showPreview()`, `IslandStateManager.activeProgress.value?.key == "droidbubble.test.preview"`; after `clearPreview()` it's null; `liveActivityKind(previewInfo) == NONE`.
- **Depends on:** none.
- **Effort:** S   **Risk:** low. If the user turned "Live Progress" off, C2.2 shows a hint instead.
- **Ships as:** 1 PR, folded into C2.2 if both are done in one session. Not user-visible alone.

### C2.2: Live preview on the Dimensions and Appearance pages
- **Goal:** the real island shows the sample while you adjust it, and you can see an expanded card on demand.
- **Why:** R10 "Live preview". 06 P2 "Settings writes per slider tick": with a visible preview, every tick also relayouts the overlay, so this needs B8's debounced writes.
- **Files:**
  - `ui/main/MainScreen.kt:1809-1915` (`DimensionsPage`), `:1919-2125` (`AppearancePage`)
  - the reused `islandRunning` rule (`:983`)
  - `island/IslandStateManager.kt:322-323` (`isOccupied`), `:715` (`expand`)
- **Spec:**
  - **Lifecycle:** `LifecycleEventObserver` inside each page.
    - On `ON_START`, if the island is running (`settings.isEnabled` and overlay granted), call `showPreview()`, but only when `!IslandStateManager.isOccupied`; if something real is showing, the real pill is the preview.
    - On `ON_STOP` and on dispose, call `clearPreview()`.
  - **Not running:** a one-line hint card at the top of the page: "Turn on the island to see your changes live." If `showLiveProgress` is off: "Turn on Live Progress & Activities to preview."
  - **"Preview expanded" button:** a `FilledTonalButton`, placed under the Expanded Card Scale and Animation Speed sliders on Dimensions, and above the curve editor on Appearance.
    - It calls `IslandStateManager.expand(IslandPriority.PROGRESS)`.
    - The existing auto-collapse (`island/IslandView.kt:1221-1226`) folds it back.
    - The expanded window is full-screen and touchable, and a tap outside collapses it (`island/IslandView.kt:1250-1262`; `service/IslandOverlayService.kt:923-937`). So the expanded preview is a short **show-me**, not something that stays open while you drag.
    - TalkBack label: "Preview expanded island".
- **Steps:**
  1. Add a `rememberIslandPreview()` helper composable shared by both pages.
  2. Add the hint card and the buttons.
- **Acceptance criteria:**
  - Opening Dimensions shows the sample pill.
  - Dragging Width resizes it live without jank, after B8.
  - Leaving the page, or pressing Home, removes it.
  - "Preview expanded" shows the card and it folds back by itself.
- **Tests:** a Robolectric test that the page's `ON_START`/`ON_STOP` call show and clear (inject the tester as a lambda). Manual on a device: check the slider drag is smooth (no per-tick window relayout in Perfetto after B8).
- **Depends on:** C2.1, B8 (debounced settings writes).
- **Effort:** S   **Risk:** low. The preview only appears while the page is in the foreground.
- **Ships as:** 1 PR, "Live island preview while tuning". User-visible.

### C2.3: Reset buttons
- **Goal:** every tuned value can go back to its default, per row and per page, with Undo.
- **Why:** 05 §2 "Dimensions: six raw sliders… no reset"; R10 "Reset buttons". Today only the curve editor has a Reset (`ui/main/MainScreen.kt:2332-2336`).
- **Files:**
  - `ui/main/MainScreen.kt:1740-1788` (`SliderSettingItem`), `:1822-1835` and `:1932-1945` (TopAppBars), `:339` (snackbar host)
  - `model/IslandSettings.kt:11-61`
  - defaults duplicated in `data/SettingsRepository.kt:79-129` and `:144-195`
- **Spec:**
  - **Model helpers:**
    - `fun IslandSettings.withDefaultDimensions()` resets `widthDp`, `heightDp`, `topOffsetDp`, `horizontalOffsetDp`, `expandedScale` and `animationSpeed`.
    - `fun IslandSettings.withDefaultAppearance()` resets `islandStyle`, `liquidGlassCards`, `showOutline`, `outlineThicknessDp`, `bubbleEffectEnabled`, `alwaysShowPill`, `hapticsEnabled`, `showInLandscape`, `blurredAlbumArtBackground` and the six curve fields.
    - Both read from `IslandSettings()`, the single source; don't add a third set of literals.
    - Once V1 lands, `withDefaultDimensions()` means "calibrated, deltas 0" and V1 updates it in one place.
  - **Per row:** `SliderSettingItem(defaultValue: Float?, onReset)`. A small `IconButton` (Restore icon, 48 dp target, `contentDescription = "Reset <label>"`) shows only when `value != defaultValue`.
  - **Per page:** TopAppBar action "Reset", which writes once, then shows Snackbar "Dimensions reset" with action "Undo" (Undo writes back the previous snapshot). Use Undo, not a confirm dialog (the Android pattern).
- **Steps:**
  1. Add the helpers.
  2. Extend `SliderSettingItem` with default parameters so existing call sites still compile.
  3. Add the page actions and the snackbar.
- **Acceptance criteria:**
  - Each slider shows Reset only when changed.
  - Page reset changes only that page's fields: `featurePriorities`, `blockedPackages`, `priorityApps`, `isEnabled` and every `show*` toggle are untouched.
  - Undo restores the exact values.
- **Tests:** a unit test of both helpers (a field-by-field diff against a fully customised instance). A Robolectric Compose test: changing a value shows a "Reset Width" node; clicking it calls `onReset`.
- **Depends on:** none. Do C5.1 first if MainScreen work runs in parallel.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Reset buttons with Undo". User-visible.

### C2.4: "Fit to camera" on the Dimensions page
- **Goal:** one tap places the pill around the camera, using V1's calibration, with the live preview from C2.2 showing the result.
- **Why:** 02 R1 and 05 R10 "Auto-fit to the camera". PLAN v2 C2 "pairs with V1".
- **Files:** `ui/main/MainScreen.kt:1845-1912`; V1's calibration API (it doesn't exist yet).
- **Spec:**
  - A `FilledTonalButton("Fit to camera")` above the sliders. It applies V1's `CutoutCalibration` result and zeroes the fine-tune deltas.
  - If V1 reports no usable cutout: Snackbar "No camera cutout found. Using the default position."
  - If V1 already shipped this button, reduce this task to: make the C2.3 reset mean "deltas = 0", and keep the preview visible while calibrating.
- **Steps:** wire V1's API, then add the button and the fallback message.
- **Acceptance criteria:** on a Pixel and a Samsung, the pill hugs the camera after one tap, and the sliders then nudge from that point.
- **Tests:** a unit test with a fake calibration result, checking that settings are written with zero deltas. Device check on 2 OEMs.
- **Depends on:** V1, C2.2, C2.3.
- **Effort:** S   **Risk:** med. Cutout variety across OEMs; V1 owns the fallback.
- **Ships as:** 1 PR, "Fit to camera button". User-visible.

---

## C3: Island TalkBack

> **Identity check (all of C3):** use Android's own accessibility vocabulary: click and long-click labels, custom actions, pane titles, live regions, state descriptions, and `getRecommendedTimeoutMillis`. Durations are spoken through ICU `MeasureFormat`, so they're localised.

### Semantics spec for each island element
This table is the contract for C3.3–C3.7. In it, "headline" is the node's `contentDescription` (it changes only on real events) and "value" is its `stateDescription` (it may tick).

| Element (anchor) | Headline | Value | Click label | Long-click label | Custom actions | Live region |
|---|---|---|---|---|---|---|
| Compact pill, activity (`island/IslandView.kt:1823-1951`, tag `PILL`) | from the table below | from the table below | "Expand" | "Open in <app>" (after B6: "Open conversation" for messages) | Minimise · Hide until next update · Dismiss* · Pause island for 1 hour (C4.1) | see C3.7; none while expanded |
| Minimised ring (same node, `userMinimized`) | "Island minimised" | primary headline | "Restore" | — | Hide until next update | none |
| System glances: charging, unlock, Bluetooth/Wi-Fi, ringer (`:1885` excludes them from tap) | "Charging" / "Unlocked" / "<device> connected" / "Silent mode" | "54 percent" / — / — / — | none | none | none | Polite, except unlock (none) |
| Idle always-shown pill | not focusable (`clearAndSetSemantics {}`) | | | | | |
| Bubble (`:1692-1733`, tag `BUBBLE`) | secondary headline | secondary value | "Expand" | "Open in <app>" | Dismiss* | none |
| Expanded card (`:1309-1311`, tag `CARD`) | `paneTitle = "Island: <kind>"` | — | — | — | Collapse · Dismiss* | the pane title is announced when the card appears |
| Tap-outside backdrop (`:1250-1262`) | "Close island" | — | "Close" | — | — | `traversalIndex = 1f` (read last) |
| Hero numbers (`island/LiveActivities.kt:447`) | `spokenText` ("4 minutes, 59 seconds") instead of "4:59" | | | | | none |

\*Dismiss is **not** offered for a ringing call, alarm or "Time's up" (they tuck instead, per B6), for screen recording (it has its own Stop), or for the flashlight (it gets "Turn off flashlight" instead).

| Activity | Headline | Value |
|---|---|---|
| Media | "Now playing: <title>, <artist>" | "Playing" / "Paused" |
| Message (POP_NOTIFICATION) | "<app>: <title>" | first 80 characters of the text, or the redacted text from A5 |
| Incoming call | "Incoming call: <name>" | — |
| Ongoing call | "Call: <name>" | elapsed time |
| Time's up / Alarm | "Time's up: <label>" / "Alarm: <label>" | — / alarm time |
| Timer | "Timer" (plus "<label>" if set) | "<remaining> left" / "Paused, <remaining> left" |
| Stopwatch / Recorder / Screen recording | "Stopwatch" / "Recording" / "Screen recording" | elapsed, or "Paused" |
| Tracking (ride, delivery, flight) | "<title>" | "<shortCriticalText>", "<n> percent" |
| Score | "<title>" | "<text>" |
| Download (PROGRESS) | "<title>" | "<n> percent" |
| Flashlight | "Flashlight on" | "Brightness <n> percent" |
| Network speed (persistent) | "Network speed" | "Down <x>, up <y>" |

### C3.1: Accessibility test harness and device spike
- **Goal:** a working place for a11y assertions, and proof that TalkBack can reach the overlay at all.
- **Why:**
  - `app/src/androidTest/java/com/droidbubble/app/ui/main/MainScreenTest.kt:17` calls `MainScreen(FAKE_DATA)`, a stale template that doesn't compile against `MainScreen(viewModel, modifier)` (`ui/main/MainScreen.kt:198`). So instrumented tests are broken today.
  - Separately, the overlay is `TYPE_ACCESSIBILITY_OVERLAY` (or `TYPE_APPLICATION_OVERLAY`) with `FLAG_NOT_FOCUSABLE` (`service/IslandOverlayService.kt:606-609`, `:643-650`). Whether TalkBack can focus its nodes is unverified.
- **Files:**
  - `app/src/androidTest/java/com/droidbubble/app/ui/main/MainScreenTest.kt`
  - new `app/src/test/java/com/droidbubble/app/island/IslandSemanticsTest.kt`
  - harness pattern from `app/src/test/java/com/droidbubble/app/island/IslandOverlayCrashTest.kt:38-141`
- **Spec:**
  - Replace the template with a minimal real test: build a `MainScreenViewModel(SettingsRepository(context))` and assert the "Enable Island" switch exists and is toggleable (`assertIsToggleable`).
  - Add `IslandSemanticsTest` with the same `@Config(sdk = [35])` and `GraphicsMode.NATIVE` setup as the crash test. Its first test pins the current state: `PILL` exists, and records whether it has a click action. Later tasks flip these assertions.
  - Add matcher helpers:
    - `hasClickLabel(label)`, reading `SemanticsActions.OnClick` and its `label`
    - `hasCustomAction(label)`
    - `hasLiveRegion(mode)`
    - `hasPaneTitle(title)`
- **Device spike** (30 min, Pixel running 14 or newer):
  1. Show the Timer demo, then use TalkBack swipe navigation to reach the pill, both with Droidbubble's accessibility service on and with it off.
  2. Record the result in this task's PR description.
  3. If the pill isn't reachable in either window type, stop and raise it. C3.4–C3.8 depend on it.
- **Steps:**
  1. Fix the androidTest.
  2. Add the Robolectric file and helpers.
  3. Run the spike.
- **Acceptance criteria:**
  - `./gradlew :app:testDebugUnitTest` and `:app:compileDebugAndroidTestKotlin` both succeed.
  - The spike result is written down.
- **Tests:** the new tests themselves.
- **Depends on:** none (S1 extends the same harness; coordinate if it's in flight).
- **Effort:** S   **Risk:** med. The spike may show TalkBack can't reach the overlay; then C3 needs a window-flag change and C3.8 grows. Find this out first.
- **Ships as:** 1 PR, "a11y test harness; fix stale androidTest". Not user-visible.

### C3.2: Accessibility environment (`IslandA11y`)
- **Goal:** one reactive answer to two questions: "is a screen reader on?" and "how long should things stay up?".
- **Why:**
  - R4 asks to skip TTS while TalkBack runs and to pause auto-collapse.
  - **Pitfall:** `AccessibilityManager.isEnabled` is `true` whenever Droidbubble's *own* accessibility service is on (`service/IslandAccessibilityService.kt`), so it can't be used to mean "TalkBack is on".
- **Files:**
  - new `island/IslandA11y.kt`
  - `service/IslandOverlayService.kt` (`onCreate`/`onDestroy`, `:1098-1121`)
- **Spec:**
  - `object IslandA11y { val screenReaderOn: StateFlow<Boolean> }`.
  - `screenReaderOn` = `isTouchExplorationEnabled` OR any service in `getEnabledAccessibilityServiceList(FEEDBACK_SPOKEN)` whose `resolveInfo.serviceInfo.packageName != ourPackage`.
  - Updated by `TouchExplorationStateChangeListener` and `AccessibilityStateChangeListener`, registered in the service's `onCreate` and removed in `onDestroy`.
  - Expose `fun recommendedTimeoutMs(base: Long): Long`. It delegates to F3's `IslandTimings` if that exists; otherwise to `getRecommendedTimeoutMillis(base, FLAG_CONTENT_TEXT or FLAG_CONTENT_CONTROLS)`.
- **Steps:** create the object, register it, and add a `@VisibleForTesting` setter.
- **Acceptance criteria:**
  - Turning TalkBack on or off flips the flow within a second.
  - Turning on only Droidbubble's accessibility service leaves it `false`.
- **Tests:** a Robolectric test using `ShadowAccessibilityManager` to set touch exploration and the enabled services list, including our own feedbackGeneric service, and assert the flow value.
- **Depends on:** none.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "IslandA11y screen-reader state". Not user-visible.

### C3.3: `IslandSpeech` label builder (pure)
- **Goal:** all spoken strings for the island in one tested place, so the wiring tasks are mechanical.
- **Why:** R4 "one summary label". Today the icon labels are generic ("Media Art" `island/IslandView.kt:2547`, "Notification Icon" `:2496`, `displayRingerMode.name` `:2154`), and timers read as "4 colon 59" (`island/LiveActivities.kt:447`).
- **Files:**
  - new `island/IslandSpeech.kt`
  - `app/src/main/res/values/strings.xml` (spoken strings go in resources, unlike the rest of the island's copy, so TalkBack users get them in their language later)
  - inputs from `model/IslandEvent.kt:49-149` (`NotificationInfo`), `:151-161` (`IslandPriority`), `island/LiveActivities.kt:92` (`LiveActivityKind`)
- **Spec:** pure functions taking a `Resources` and `Locale` plus the display model:
  - `headline(...)` and `value(...)`, per the tables above
  - `clickLabel(...)`, `longClickLabel(appName, target)`
  - `paneTitle(kind)`
  - `announcement(event)` (used by C3.7)
  - `spokenDuration(ms)` via `android.icu.text.MeasureFormat` (WIDE)
  - `spokenPercent(n)` via `NumberFormat.getPercentInstance`

  When A5 lands, these take the **redacted** model, never raw notification text.
- **Steps:** write the functions and strings, then the tests. No call sites yet.
- **Acceptance criteria:**
  - Every row of both tables has a test.
  - No string is concatenated outside resources with placeholders.
- **Tests:** `IslandSpeechTest` (Robolectric, for ICU): timer 299 000 ms gives "4 minutes, 59 seconds left"; an incoming call gives "Incoming call: Sam Carter"; a ringing call yields no Dismiss in `availableActions(...)`; `es` locale formatting works.
- **Depends on:** C3.1. It's coordinated with A5 but doesn't block on it.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "IslandSpeech: spoken labels". Not user-visible.

### C3.4: Pill, bubble and ring semantics
- **Goal:** TalkBack reads one meaningful summary for the pill and bubble, and double-tap and long-press are labelled.
- **Why:** R4. Today the pill, bubble and backdrop have no labels or click labels (`island/IslandView.kt:1251-1261`, `:1713`, `:1878`; 05 §2 "Accessibility").
- **Files:** `island/IslandView.kt:1692-1733` (bubble), `:1823-1951` (pill), `:1954` (`compactAlpha`); M4's gesture modifier once it exists.
- **Spec:**
  - On the pill node:
    - `Modifier.semantics { contentDescription = headline; stateDescription = value }`
    - `combinedClickable(onClickLabel = clickLabel, onLongClickLabel = longClickLabel, ...)`. The parameters exist on `combinedClickable`; use them instead of overriding `onClick` semantics.
  - On the pill's **content child**, `clearAndSetSemantics {}`, so the inner icons and texts aren't read twice. Don't put `clearAndSetSemantics` on the pill node itself: it would risk clearing the clickable's actions.
  - Same on the bubble.
  - Idle always-shown pill: `clearAndSetSemantics {}`, so it isn't focusable.
  - Glances (charging, unlock, Bluetooth, ringer): headline and value only, with no click action, matching `:1885`.
- **Steps:**
  1. Compute `IslandSpeech` values once per primary/secondary change in a `remember`, so they aren't rebuilt per frame (keep M2's ~0 recompositions per frame).
  2. Apply the modifiers.
  3. Flip the C3.1 assertions.
- **Acceptance criteria:**
  - TalkBack on the Timer demo reads "Timer, 4 minutes, 59 seconds left. Double-tap to expand. Double-tap and hold to open in Clock".
  - The idle pill is skipped.
  - The S4 recomposition count during a morph is unchanged.
- **Tests:** `IslandSemanticsTest`: the `PILL` node has the content description "Timer", `OnClick` label "Expand" and `OnLongClick` label "Open in Clock"; the idle `alwaysShowPill` pill has no content description; the bubble in dual mode has its own label.
- **Depends on:** C3.3, M4, and the C3.1 spike passing.
- **Effort:** M   **Risk:** med. It touches the hottest file; run it after M4, never in parallel with another `IslandView.kt` session.
- **Ships as:** 1 PR, "TalkBack labels for pill and bubble". User-visible to TalkBack users.

### C3.5: Custom actions (minimise, hide, dismiss, collapse)
- **Goal:** every swipe-only action has an alternative in the TalkBack Actions menu.
- **Why:** R4. Hide, minimise and dismiss are reachable by swipe only. D6 also moves full hide off the gesture, so the action becomes its main path (R9).
- **Files:**
  - `island/IslandView.kt:1823-1951`, `:1692-1733`, `:1309-1311`
  - state calls `island/IslandStateManager.kt:83-98` (`hideIsland`, `minimizeIsland`, `restoreIsland`), `:510` (`dismissMedia`), `:669` (`dismissCurrentNotification`), `:691` (`dismissProgress`), `:725` (`collapse`)
- **Spec:**
  - Actions, using `CustomAccessibilityAction(label) { ...; true }`:

    | Node | Actions |
    |---|---|
    | Pill | "Minimise", "Hide until next update", "Dismiss"\* (mapped per primary priority, as in the swipe handler at `:1346-1365`), "Turn off flashlight" (flashlight), "Pause island for 1 hour" (once C4.1 exists) |
    | Ring | "Hide until next update" |
    | Bubble | "Dismiss"\* |
    | Card | "Collapse", "Dismiss"\* |

  - Get the list from `IslandSpeech.availableActions(...)` (C3.3), so the exclusions (\*) live in one place.
  - If M4 adds a bubble "swap" gesture, add "Make primary".
- **Steps:** wire the actions, then add tests.
- **Acceptance criteria:**
  - TalkBack's Actions menu on the pill lists the actions.
  - A ringing call has no Dismiss.
  - Each action has the same effect as its gesture.
- **Tests:** `IslandSemanticsTest`: run each custom action through `config[SemanticsActions.CustomActions]` and assert the state change, for example that `userMinimized` becomes true; the ringing-call state lacks "Dismiss".
- **Depends on:** C3.4, B6 (the tuck rule for calls and alarms).
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "TalkBack custom actions". User-visible to TalkBack users.

### C3.6: Expanded card, backdrop and control labels
- **Goal:** the expanded card announces itself, the backdrop is a labelled "Close", and every control inside has a specific, state-aware label and a 48 dp touch target.
- **Why:** R4 calls for a `paneTitle` and "Close island". 05 §2 lists generic icon labels, and reply Send/Cancel buttons at 32 and 28 dp sitting next to each other.
- **Files:**
  - `island/IslandView.kt:1250-1262` (backdrop), `:1309-1311` (card)
  - labels: `:2154` (ringer `name`), `:2496` ("Notification Icon"), `:2547`, `:2779` ("Media Art"), `:3581`, `:3650` (shuffle and repeat, no state), `:3620` ("Play/Pause")
  - reply buttons `:3902-3926`
  - `island/IslandComponents.kt:520` ("Cover Art" default), `:684` ("Sender"), `:707` ("App Icon")
  - `island/LiveActivities.kt:398-447` (`RollingNumberText`)
- **Spec:**
  - **Card:** `semantics { paneTitle = IslandSpeech.paneTitle(kind); isTraversalGroup = true }`.
  - **Backdrop:** `semantics { contentDescription = "Close island"; traversalIndex = 1f }` plus `onClickLabel = "Close"`.
  - **Decorative images** (art, app icons next to a spoken app name) get `contentDescription = null`.
  - **State labels:**
    - Play/Pause reads "Pause" while playing and "Play" while paused.
    - Shuffle and Repeat get `stateDescription` "On"/"Off".
    - Ringer reads "Silent mode", "Vibrate mode" or "Ring mode".
  - **`RollingNumberText(spokenText: String? = null)`:** the semantics use `spokenText ?: text`. Callers pass `IslandSpeech.spokenDuration`.
  - **Reply:**
    - The field label is "Reply to <title>".
    - Send and Cancel keep their 32/28 dp visuals inside 48 dp touch boxes. The layout grows a little; review the S2 diff.
    - The labels become "Send reply" and "Cancel reply".
- **Steps:** card and backdrop first, then the label sweep, then reply.
- **Acceptance criteria:**
  - Expanding announces "Island: Timer".
  - The backdrop is reached last.
  - No control reads a generic label.
  - Reply targets are at least 48 dp.
  - The S2 screenshot diff shows only the reply-row change.
- **Tests:** `IslandSemanticsTest`:
  - `CARD` has the pane title
  - the backdrop node has "Close island"
  - Play/Pause reads "Pause" when `isPlaying`
  - the reply Send node's `touchBoundsInRoot` is at least 48 dp

  Also add an ATF check run in androidTest (see the test plan).
- **Depends on:** C3.3, C3.4, M3 (stable card content layers), B5 (the placeholder contrast fix sits next to these lines).
- **Effort:** M   **Risk:** med. Touch-target growth can shift the reply row; it's reviewed through S2.
- **Ships as:** 1 PR, "Expanded card a11y and control labels". User-visible to TalkBack users; small layout change in the reply row.

### C3.7: Live-region announcements
- **Goal:** TalkBack users hear important island events once, at the right urgency, and ticking values are never announced.
- **Why:**
  - R4 "polite live region".
  - `View.announceForAccessibility` is deprecated as of API 36, so the job goes to live regions and pane titles.
- **Files:**
  - `island/IslandView.kt:1823-1951` (pill)
  - triggers in `island/IslandStateManager.kt:581-589` (call or alert auto-expands) and `:599-601` (new notification pop-in)
- **Spec:**
  - The pill node gets `liveRegion` only while it's not expanded. Its **headline** (`contentDescription`) changes only on real events. The ticking value goes in `stateDescription`.
  - Politeness map:

    | Event | Mode | Notes |
    |---|---|---|
    | Incoming call, alarm, Time's up | Assertive, carried by the card's **pane title** (these auto-expand at `:581-589`) | The pill's region is suppressed, so it isn't announced twice |
    | New message pop-in | Polite | headline "<app>: <title>" |
    | A live activity starts (timer, ride, recorder, download) | Polite | headline |
    | Bluetooth/Wi-Fi connected, ringer change, charging connected | Polite | |
    | Ticking values, media track change, unlock, activity end | none | |
    | While paused (C4.1) or DND-suppressed (C4.4) | none | |

- **Steps:**
  1. Add a derived `announcementKey`: kind, activity key and coarse state.
  2. The headline recomputes only when that key changes.
  3. Apply `liveRegion`.
- **Acceptance criteria:**
  - Starting the Timer demo announces "Timer" once and not each second.
  - The Call demo announces "Incoming call: Sam Carter" once.
- **Tests:** `IslandSemanticsTest`: the pill has `LiveRegionMode.Polite` for a message and no live region while expanded; the headline text is unchanged across a 3 s timer tick (advance `mainClock`). Plus the manual script in the test plan (device behaviour of `stateDescription` changes).
- **Depends on:** C3.4, C3.3.
- **Effort:** S   **Risk:** med. TalkBack may also announce `stateDescription` changes on a live-region node. If the device check shows that, throttle `stateDescription` to minute granularity while the node isn't focused.
- **Ships as:** 1 PR, "Live-region announcements". User-visible to TalkBack users.

### C3.8: Auto-collapse pauses while accessibility focus is on the island
- **Goal:** TalkBack users aren't cut off mid-read. After focus leaves, the card collapses on the user's "Time to take action" timeout.
- **Why:**
  - R4. The 3 s collapse (`island/IslandView.kt:1216-1226`) restarts only on pointer events (`:1238-1245`).
  - TalkBack swipe navigation sends the overlay no pointer events, so the card collapses while it's being read.
- **Files:**
  - `service/IslandOverlayService.kt:674-722` (the `ComposeView`), `:734` (`addView`), `:800`, `:854`, `:896` (`updateViewLayout` call sites, or F5's controller)
  - `island/IslandView.kt:1221-1226`
  - `island/IslandStateManager.kt` (new flow)
- **Spec:**
  - Wrap the ComposeView in `IslandA11yFrame : FrameLayout` and override `onRequestSendAccessibilityEvent(child, event)`:
    - `TYPE_VIEW_ACCESSIBILITY_FOCUSED` → `IslandStateManager.setA11yFocusInside(true)`.
    - `TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED` → schedule `false` after 300 ms, cancelled by a new FOCUSED. Moving between nodes sends cleared then focused.
  - The frame is what's added to the WindowManager.
  - The auto-collapse `LaunchedEffect` adds `a11yFocusInside` to its keys and doesn't run while it's true.
  - The delay becomes `IslandA11y.recommendedTimeoutMs(3000)` (C3.2, or F3's `IslandTimings` value) while `screenReaderOn`.
- **Steps:**
  1. Add the frame class.
  2. Switch every `addView`/`updateViewLayout`/`removeView` call to the frame, or to the F5 controller's root.
  3. Add the flow and extend the effect keys.
- **Acceptance criteria:**
  - With TalkBack on, an expanded card stays open while focus is inside, for at least 30 s.
  - Once focus leaves, it collapses after the recommended timeout.
  - Without TalkBack, behaviour is unchanged.
- **Tests:**
  - A unit test of the frame: feed synthetic `AccessibilityEvent`s and assert the flow, including that cleared→focused within 300 ms stays true.
  - `IslandSemanticsTest`: with `a11yFocusInside = true`, after advancing the clock 10 s, `isExpanded` is still true.
  - Manual script, step 3.
- **Depends on:** C3.2, F3, F5 (it touches the same window code), and the C3.1 spike.
- **Effort:** M   **Risk:** med. It touches window plumbing; device-verify on Pixel and Samsung.
- **Ships as:** 1 PR, "Keep the card open under TalkBack focus". User-visible to TalkBack users.

### C3.9: Spoken Alerts defers to TalkBack
- **Goal:** the island's own TTS never talks over a screen reader.
- **Why:** R4 "Skip the TTS when TalkBack is on". `service/SpokenAlerts.kt` speaks independently (`:47-60`). R11's Spoken Alerts row also needs honest copy.
- **Files:**
  - `service/SpokenAlerts.kt:47-60`
  - `ui/main/MainScreen.kt:2230-2245` (Accessibility page row)
- **Spec:**
  - In `onNotification`, after computing `key`, set `lastSpokenKey`. If `IslandA11y.screenReaderOn.value`, return without speaking. Setting the key first means that if TalkBack turns off mid-alert, the same alert isn't spoken late.
  - Supporting text: "Reads "Time's up", alarms and who's calling aloud. Stays quiet in silent and vibrate mode, and while TalkBack is on (TalkBack announces these instead)."
- **Steps:** add the guard and the copy, then extend the test.
- **Acceptance criteria:** with Spoken Alerts and TalkBack both on, the Time's-up demo gives only TalkBack's announcement.
- **Tests:** `app/src/test/java/com/droidbubble/app/service/SpokenAlertsTest.kt`: when the screen reader is on, `phraseFor` is still computed but `say` isn't called (inject a speaker lambda, or test via a `@VisibleForTesting` hook).
- **Depends on:** C3.2.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Spoken Alerts stays quiet under TalkBack". User-visible.

### C3.10: Font scale: non-linear scaling, compact cap, expanded up to 2.0
- **Goal:** expanded cards follow the phone's font size up to 2.0×, using Android 14's non-linear curve. Compact text stays capped so it fits the pill.
- **Why:**
  - 05 §2: font scale is clamped to 0.85–1.5 for the whole island.
  - 01 R8 (and P2 [V]): building `Density(density, fontScale)` by hand probably drops non-linear scaling.
  - PLAN v2 C3: "expanded font scale up to 2.0".
- **Files:**
  - `service/IslandOverlayService.kt:710-714`
  - the card-height measurers in `island/IslandComponents.kt` (`measure*CardHeight`, used for layout)
  - F4's `IslandType`
  - the compact and expanded content layers from M3
- **Spec:**
  - **Step 0 (verify):** a Robolectric test at `sdk = 35` that compares `with(Density(d, 2f)) { 14.sp.toDp() }` with the platform density's result at font scale 2.0.
    - If they're equal, the converter is already applied: keep `Density` and only split the caps.
    - If not, add a `NonLinearDensity(base, maxScale): Density` that clamps `fontScale` and overrides `TextUnit.toDp()` / `Dp.toSp()` using `android.content.res.FontScaleConverterFactory.forScale()` on API 34+, and linear below 34.
  - **Caps:** compact pill and bubble at 1.3; expanded cards at 2.0. Two `LocalDensity` providers inside the root: one around compact content, one around card content.
  - Card heights must be measured with the card density.
  - Hero numerals keep not growing with font scale (`island/LiveActivities.kt:346-348`).
- **Steps:**
  1. Write the verification test.
  2. Write the density wrapper if it's needed.
  3. Split the providers at the M3 layer boundary.
  4. Check the card measurers.
- **Acceptance criteria:**
  - At system font size maximum (2.0) on API 34+, card body text grows non-linearly and nothing clips: no truncated title, buttons fully visible.
  - Compact text never exceeds the 1.3 cap.
  - S2 diffs at 1.0 are unchanged.
- **Tests:**
  - the verification test above
  - Roborazzi or S1 bounds tests at `fontScale` 1.0, 1.3 and 2.0, for Timer, Message and Media cards: settled card bounds contain all text nodes (`assertIsDisplayed` on the title and buttons inside `CARD` bounds)
- **Depends on:** F4, V4 (measured heights), M3.
- **Effort:** M   **Risk:** high. Large text can push cards past the screen. Mitigate with V4's measured heights, a max-height plus scroll inside the card at 2.0, and the bounds tests.
- **Ships as:** 1 PR, "Large text in expanded cards". User-visible at large font sizes.

---

## C4: Quick Settings tile, "Pause island 1 h", Do Not Disturb awareness

> **Identity check (all of C4):** these are Android-only affordances (05 §5): a QS tile with subtitle and state description, a notification action, and DND behaviour that mirrors the system's own "suppressed visual effects" settings rather than adding a parallel Droidbubble setting.

### C4.1: Pause model ("pause for 1 hour")
- **Goal:** a timed, persisted pause that new notifications can't undo and that always resumes by itself.
- **Why:**
  - R9: full hide moves to a QS tile or notification action, and resumes on a timer.
  - Today's "hide" (`island/IslandStateManager.kt:83-87`) is in-memory and is reset by every new notification (`restoreIsland()` at `:582`, `:587`, `:600`), so it can't implement a timed pause.
- **Files:**
  - `model/IslandSettings.kt:11-61`
  - `data/SettingsRepository.kt` (keys `:21-67`, both mappers, writer `:199-243`)
  - `service/IslandOverlayService.kt:806-822` (settings job), `:311` (`SCREEN_ON`), `:520-537` (`onStartCommand`)
  - new `service/IslandPause.kt`
- **Spec:**
  - **Setting:** `pausedUntilEpochMs: Long = 0L` (key `paused_until_ms`), with `fun isPaused(now) = pausedUntilEpochMs > now`.
  - **`IslandPause.pauseFor(context, durationMs = 3_600_000)` / `resume(context)`:** write the setting, then refresh the tile (`TileService.requestListeningState`, a no-op until C4.3) and the FGS notification (C4.2).
  - **Service, while paused:** `removeOverlay()`, the same path as `!isEnabled` (`:813-814`), but keep the foreground service running.
  - **Resume:** `delay(until - now)`, then `resume()`. Because `delay` is uptime-based and stalls in deep sleep, also re-check on `ACTION_SCREEN_ON` and in `onStartCommand`.
  - **Calls during a pause:** they don't show. The system heads-up still rings.
  - The pause is separate from `userHidden` and `userMinimized`.
- **Steps:**
  1. Add the setting and key to every mapper and the writer.
  2. Add `IslandPause`.
  3. Gate the settings job.
  4. Add the resume checks.
- **Acceptance criteria:**
  - Pausing removes the island within one frame.
  - New notifications don't bring it back.
  - It reappears on its own after 1 h, including after a screen-off period longer than 1 h.
  - Survives a process restart.
- **Tests:** a unit test of `isPaused`; a Robolectric repository round-trip; a unit test that `IslandStateManager.updateNotification` doesn't touch the pause; a manual check of a sleep longer than the pause.
- **Depends on:** none. Coordinate with B8 if it unifies the mapper.
- **Effort:** M   **Risk:** med. Missing a mapper line silently drops the pause; the round-trip test guards it.
- **Ships as:** 1 PR, "Timed island pause". Not user-visible until C4.2 or C4.3.

### C4.2: Pause surfaces: notification action and in-app state
- **Goal:** "Pause for 1 hour" and "Resume" are one tap away in the shade, and the app shows when the island is paused.
- **Why:** R9 "foreground-notification action ("Pause island 1 h")". The FGS notification has no actions today (`service/IslandOverlayService.kt:544-571`).
- **Files:**
  - `service/IslandOverlayService.kt:84-85` (actions), `:520-537`, `:544-571`, `:1186-1206` (channel copy)
  - `ui/main/MainScreen.kt:442-461` (master switch)
- **Spec:**
  - New intent actions `ACTION_PAUSE` and `ACTION_RESUME`, handled in `onStartCommand`. They use **explicit** `PendingIntent.getService(..., FLAG_IMMUTABLE)` with distinct request codes; the service stays `exported="false"`, consistent with B1.
  - Notification while running: "Island is on" with action "Pause for 1 hour".
  - Notification while paused: "Island paused until <time>" (`DateFormat.getTimeFormat`) with action "Resume". Re-post on every pause change.
  - Replace "Dynamic Island" in the copy: `:553`, channel description `:1198`, manifest `AndroidManifest.xml:65`.
  - On API 33+ the action is visible only with `POST_NOTIFICATIONS`. That's why C1.1 lists it as Optional, and why the QS tile (C4.3) is the main surface.
  - Master switch row: supporting text "Paused until 15:40" plus a `TextButton("Resume")` while paused.
- **Steps:** add the intent actions, update the notification builder, then the row.
- **Acceptance criteria:**
  - The shade action pauses and resumes.
  - The text updates.
  - The app row reflects the state and can resume.
- **Tests:** a Robolectric service test: deliver `ACTION_PAUSE` and assert `pausedUntilEpochMs` is about now + 1 h, and the posted notification has a "Resume" action (`ShadowNotificationManager`).
- **Depends on:** C4.1.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Pause and Resume from the notification". User-visible.

### C4.3: Quick Settings tile
- **Goal:** an "Island" tile that pauses, resumes or turns on the island, with honest states for TalkBack.
- **Why:** PLAN v2 C4 and 05 §5 (Android-only controls).
- **Files:**
  - new `service/IslandTileService.kt`
  - `AndroidManifest.xml` (services `:58-97`; MainActivity `:48-56`)
  - `app/src/main/res/values/strings.xml`
  - `ui/main/MainScreen.kt:442-461` (add-tile row)
- **Spec:**

  | Condition | `Tile.state` | Subtitle (API 29+) | `stateDescription` (API 30+) | Tap does |
  |---|---|---|---|---|
  | Overlay not granted | `STATE_UNAVAILABLE` | "Setup needed" | "Setup needed" | open the app (`startActivityAndCollapse(PendingIntent)` on 34+; the `Intent` overload below 34) |
  | `isEnabled = false` | `STATE_INACTIVE` | "Off" | "Off" | set `isEnabled = true`, start the service |
  | Paused | `STATE_INACTIVE` | "Paused until 15:40" | "Paused until 3:40 PM" | `IslandPause.resume()` |
  | Running | `STATE_ACTIVE` | "On" | "On" | `IslandPause.pauseFor(1 h)` |

  - **Manifest:** `service .service.IslandTileService` with:
    - `exported="true"`, `permission="android.permission.BIND_QUICK_SETTINGS_TILE"` (system-only binding)
    - icon `@drawable/ic_notification_island`, label "Island"
    - intent-filter `android.service.quicksettings.action.QS_TILE`
    - meta-data `android.service.quicksettings.ACTIVE_TILE=true` and `android.service.quicksettings.TOGGLEABLE_TILE=true`
  - **MainActivity** gets the intent-filter `android.service.quicksettings.action.QS_TILE_PREFERENCES`, so long-pressing the tile opens the app.
  - **Lifecycle:**
    - `onStartListening` reads settings through `settingsFlow.first()` in a scope that's cancelled in `onStopListening`.
    - Changes elsewhere call `TileService.requestListeningState(context, ComponentName(...))`, wired in `IslandPause` (C4.1) and in `toggleEnabled` (`ui/main/MainScreenViewModel.kt:127-136`).
  - **Taps on the lock screen** are allowed for pause and resume; opening the app goes through the system unlock.
  - **Add-tile prompt** (API 33+): under the master switch, a `TextButton("Add Quick Settings tile")` calls `StatusBarManager.requestAddTileService(...)`. Hide it after the result `TILE_ADD_REQUEST_RESULT_TILE_ADDED` or `..._ALREADY_ADDED`.
  - Use the same start rule as `MainActivity.kt:46-48`, but respect `isEnabled`.
- **Steps:** write the service, then the manifest, then wire refreshes, then the add-tile row.
- **Acceptance criteria:**
  - Every row of the state table is reproducible.
  - TalkBack reads "Island, On" and "Island, Paused until 3:40 PM".
  - The tile updates within 1 s of a change made in the app or the shade.
- **Tests:** a unit test of the pure `tileModel(settings, overlayGranted, now): TileModel` covering all four rows. Manual: Pixel and Samsung, with TalkBack.
- **Depends on:** C4.1. C4.2 is recommended first, for one consistent pause UX.
- **Effort:** M   **Risk:** med. `startActivityAndCollapse(Intent)` throws on API 34+ when targeting 34+; use the `PendingIntent` overload behind an SDK check.
- **Ships as:** 1 PR, "Quick Settings tile". User-visible.

### C4.4: Do Not Disturb awareness
- **Goal:** during DND the island behaves the way the user's DND settings say heads-up and the status bar should behave. Ongoing status (timers, media, calls in progress) stays visible.
- **Why:** 05 §5 "Do Not Disturb awareness (only priority pop-ins during Do Not Disturb)". NRS already reads the ranking (`service/NotificationReceiverService.kt:985-992`) but ignores DND.
- **Files:**
  - `service/NotificationReceiverService.kt:288` (`onListenerConnected`), `:859-866`, `:985-992`
  - `model/IslandEvent.kt:49-149` (`NotificationInfo`)
  - `island/IslandStateManager.kt:581-589` (auto-expand), `:599-601` (pop-in)
  - `service/SpokenAlerts.kt:47-60`
  - new pure `island/DndPolicy.kt`
- **Spec:**
  - **State source:** NRS `onInterruptionFilterChanged(filter)`, initialised from `getCurrentInterruptionFilter()` in `onListenerConnected`, sets `IslandStateManager.dndActive = filter != INTERRUPTION_FILTER_ALL`. A7 also plans a DND *indicator*; whichever lands first owns this flow and the other reuses it.
  - **Per notification** (at the existing ranking fetch): `matches = ranking.matchesInterruptionFilter()`, `effects = ranking.suppressedVisualEffects`. Store them on `NotificationInfo` as `dndAllowsPeek` and `dndAllowsStatusBar`.
  - **`DndPolicy.decide(dndActive, matches, effects, isOngoingLive)`** returns `{ showCompact, allowPopIn, allowAutoExpand, allowSoundAndSpeech }`:
    - When DND is off, everything is true.
    - When DND is on, the notification doesn't match the filter, and the system suppresses `SUPPRESSED_EFFECT_PEEK`: no pop-in, no auto-expand, no haptic, no speech.
    - With `SUPPRESSED_EFFECT_STATUS_BAR`: not shown in compact either.
    - Ongoing live activities (timer, nav, recorder, media) always keep `showCompact`.
  - **Calls and alarms** follow `matches`. An allowed caller still auto-expands; a blocked one shows compact only.
  - **No new setting.** The user's DND visual-effects settings are the control. Add one line to the Accessibility page under Auto-dismiss: "During Do Not Disturb, the island follows your Do Not Disturb settings."
  - If A2's arbiter is merged, put `decide()` in the arbiter instead of `IslandStateManager`.
- **Steps:**
  1. Write `DndPolicy` and its tests.
  2. Capture the NRS fields.
  3. Gate the pop-in, auto-expand and speech.
  4. Add the copy line.
- **Acceptance criteria:**
  - With DND on and default visual effects, a message from a non-priority contact doesn't pop the island; a starred contact's does.
  - A running timer stays in compact.
  - An alarm still expands.
- **Tests:** `DndPolicyTest`, a full truth table. An `IslandStateBehaviorTest` case: a suppressed message updates the state but doesn't call `triggerPopIn`. Manual: Pixel DND with "Don't pop notifications on screen".
- **Depends on:** B10 (NRS refactor), or do it before B10 and let B10 carry it. Coordinate with A1, A2 and A7.
- **Effort:** M   **Risk:** med. OEM DND variants (Samsung modes); `suppressedVisualEffects` is the documented signal, and the fallback is `matches` alone.
- **Ships as:** 1 PR, "Follow Do Not Disturb". User-visible.

---

## C5: Small settings fixes

> **Identity check:** keep the current grouped M3 list. Fixes are in place: no new screens and no restyle.

### C5.1: (Enabler, optional) Split `MainScreen.kt` into files, with no behaviour change
- **Goal:** let the C1, C2, C4, C5, D5 and V1 sessions edit different files instead of queueing on one 2476-line file.
- **Why:** PLAN.md §5 says never to run two writing sessions on the same file. This phase alone has 14 tasks that touch `MainScreen.kt`.
- **Files:** `ui/main/MainScreen.kt`. Moves:
  - `:1632-1788` (shared items) → `SettingsComponents.kt`
  - `:1809-1915` → `DimensionsPage.kt`
  - `:1919-2125` → `AppearancePage.kt`
  - `:2128-2287` → `AccessibilityPage.kt`
  - `:2290-2476` → `AnimationCurveEditor.kt`
  - sheets `:1052-1627` → `MainScreenSheets.kt`, as composables taking state and callbacks
- **Spec:** a pure move. Keep public names and signatures, and keep `MainScreenPage` and the navigation in `MainScreen.kt`. No string, layout or behaviour changes.
- **Steps:** move one block per commit, compiling after each, then delete the unused imports.
- **Acceptance criteria:**
  - The app looks and behaves identically.
  - `MainScreen.kt` is under 1100 lines.
  - Every other C task's anchors are re-pointed in its PR.
- **Tests:** compile, plus the C3.1 androidTest smoke test and a manual walk through all four pages.
- **Depends on:** none. Do it before parallel MainScreen work, and coordinate with D5, which owns `AnimationCurveEditor`.
- **Effort:** S   **Risk:** low. Mechanical, but it invalidates in-flight branches: land it when no MainScreen PR is open.
- **Ships as:** 1 PR, "Split MainScreen.kt (no UI change)".

### C5.2: Make the per-app filter reachable
- **Goal:** users can choose which apps may show on the island.
- **Why:** 05 §2 says the sheet "can never be opened". It is also broken:
  - It never calls `loadInstalledApps`; only the Priority row does (`ui/main/MainScreen.kt:864`).
  - It has no loading state.
  - Its switch reads a stale `AppItem.isBlocked` snapshot (`ui/main/MainScreenViewModel.kt:148-159`, `:187-191`).
- **Files:**
  - `ui/main/MainScreen.kt:213` (`showAppFilterSheet`), `:852-886` (sibling row position), `:1052-1131` (sheet)
  - `ui/main/MainScreenViewModel.kt:138-193`
  - the filter applies at `service/NotificationReceiverService.kt:866`
- **Spec:**
  - New row under Priority Apps: "Apps on the island". Supporting text: "All apps", or "<n> apps hidden". On click: `loadInstalledApps(context)`, then show the sheet.
  - Sheet changes:
    - title "Apps on the island"
    - one-line explanation: "Hidden apps don't show their notifications or live activities on the island. Music controls are set by the media player toggle." This matches `:866`, which filters posted notifications only.
    - a loading spinner like the one at `:1182-1191`
    - `checked = app.packageName !in settings.blockedPackages`, derived from settings like the priority sheet at `:1218`
    - per-row `contentDescription`: "Show <app> on the island"
  - Sort hidden apps first.
- **Steps:** add the row, fix the sheet's data path, then add the copy.
- **Acceptance criteria:**
  - The sheet opens with a populated list.
  - Toggling hides that app's next notification.
  - The state stays correct after closing and reopening, including changes made while it was closed.
- **Tests:** a Robolectric Compose test of the extracted `AppFilterSheet(apps, blocked, onToggle)`: a row's checked state follows `blocked`. A unit test that the ViewModel's `toggleAppBlocked` writes the set.
- **Depends on:** none.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Reachable per-app filter". User-visible.

### C5.3: A quieter update row and update notification
- **Goal:** updates are discoverable without a permanent primary-coloured banner, or a sticky high-importance notification.
- **Why:**
  - 05 §2 and P2: "A large primary-coloured Update banner that is always shown" (`ui/main/MainScreen.kt:463-565`).
  - The background check also posts an **ongoing, IMPORTANCE_HIGH** notification (`service/IslandOverlayService.kt:1146-1176`).
- **Files:**
  - `ui/main/MainScreen.kt:463-565`
  - `ui/main/MainScreenViewModel.kt:47-115`
  - `service/IslandOverlayService.kt:1143-1184`
- **Spec:**
  - **In-app:**
    - Replace the banner with a standard `SettingsCardItem` + `ListItem` row, "App updates", moved to the end of the main list (where "Coming soon" was).
    - Supporting text: "Version <x> · Up to date", "Checking…" or "Tap to check".
    - When an update is `Available`, the row uses `secondaryContainer`, shows "Update available · <version>" plus the BETA label, and opens the existing sheet (`:1231-1374`).
    - `Downloading` shows a percentage, with `tnum` (see C5.4).
  - **Notification:**
    - Create channel `droidbubble_app_updates_v2` at `IMPORTANCE_DEFAULT`, and delete the old channel id. A channel's importance can't be changed after creation; the same pattern is at `:1190-1191`.
    - Build with `setOngoing(false)`, `setAutoCancel(true)`, and text "Droidbubble <version> is available".
- **Steps:** replace the row, then the channel and builder.
- **Acceptance criteria:**
  - With no update, the main screen has no coloured banner.
  - With an update, the row is visibly tonal and opens the sheet.
  - The shade notification can be swiped away.
- **Tests:** a Robolectric Compose test of `UpdateRow(state)` for each `UpdateState`. A Robolectric check that the posted notification isn't ongoing (`ShadowNotification`).
- **Depends on:** none.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Quieter update row and notification". User-visible.

### C5.4: Steady value chips (`tnum`)
- **Goal:** slider value chips don't change width while dragging.
- **Why:** 01 finding 9 "Slider value chips (2471) have no tnum"; R7 "Add tnum to labelMedium value chips".
- **Files:** `ui/main/MainScreen.kt:1765-1777` (`SliderSettingItem` chip), `:2465-2472` (`SliderSettingItemPct` chip).
- **Spec:**
  - Chip text style: `MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")`. Keep the current weight; the full typography pass is out of scope.
  - The chip box gets `widthIn(min = …)`, measured once from the widest template ("−50 dp", "0.0 x", "100%") using `rememberTextMeasurer`, the same idea as the island's media time labels.
  - Value formatting stays as it is.
- **Steps:** add a `ValueChip(text, template)` helper and use it in both places.
- **Acceptance criteria:** dragging any slider leaves the chip's width and position steady.
- **Tests:** a Robolectric Compose test: chip width for "5 dp" equals the width for "50 dp" (`getBoundsInRoot`).
- **Depends on:** none. The curve editor's segmented labels (`:2378-2384`, Bold↔Medium width shift) belong to D5; flag them there.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, together with C5.5 if done in one session. User-visible (subtle).

### C5.5: Remove the "Coming soon" section
- **Goal:** no disabled placeholder clutter on the main screen.
- **Why:** 05 §2 and P2 "the Coming-soon section is clutter" (`ui/main/MainScreen.kt:986-1041`).
- **Files:** `ui/main/MainScreen.kt:986-1041` and now-unused imports (for example `alpha` at `:43`, `Widgets` at `:93`; check each with a grep before deleting).
- **Spec:** delete the section. Keep the `showWeather` and `showNetworkSpeed` settings and keys; B7 owns their truthfulness and any future UI.
- **Steps:** delete the section, then remove the imports.
- **Acceptance criteria:** the section is gone; nothing else moved except the update row, if C5.3 already landed.
- **Tests:** compile; the S2 companion screenshots, if any; manual.
- **Depends on:** none.
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, or with C5.4. User-visible.

### C5.6: Accessibility for the activity reorder list
- **Goal:** TalkBack and switch users can reorder island activities, and the drag handle is a proper target.
- **Why:** 05 §2 "Reorder handle: about 24dp, with no move-up or move-down actions for TalkBack"; R12.
- **Files:** `ui/main/MainScreen.kt:640-850`: handle `:772-820`, persistence `:793`, haptics `:680`, `:781`, `:795`.
- **Spec:**
  - Extract `ActivityPriorityList(keys, checked, onMove(from, to), onToggle)` from the inline block. The drag behaviour and its persistence through `updateSettings { it.copy(featurePriorities = list) }` stay as they are.
  - **Each row:**
    - `semantics { stateDescription = "Position 2 of 5" }`
    - custom actions "Move up" and "Move to top" (not on the first row) and "Move down" (not on the last). Each calls `onMove` and keeps accessibility focus on the moved row (`key(featureKey)` already keeps identity), so TalkBack reads the new position.
  - **Handle:** a 48 dp touch target (`minimumInteractiveComponentSize()` around the 20 dp icon). Use `contentDescription = null` on the icon, since the actions replace "Drag to reorder" for TalkBack.
  - `HapticFeedbackConstants.CONFIRM` (`:795`) is API 30. Guard it with a `CLOCK_TICK` fallback if D1 keeps minSdk at 29.
- **Steps:** extract the composable, add the semantics, then the target size.
- **Acceptance criteria:**
  - With TalkBack, "Move up" on "Media Player" moves it and announces "Position 1 of 5".
  - The order persists after an app restart.
  - Dragging still works.
- **Tests:** a Robolectric Compose test of `ActivityPriorityList`: run "Move down" on row 0 and assert `onMove(0, 1)`; row 0 has no "Move up"; `stateDescription` is correct; the handle's touch bounds are at least 48 dp.
- **Depends on:** none (C5.1 recommended).
- **Effort:** S   **Risk:** low.
- **Ships as:** 1 PR, "Reorder list: TalkBack actions and 48 dp handle". User-visible to TalkBack users.

---

## Accessibility test plan

### Automated: Robolectric (`app/src/test`, runs in CI)

| Suite | What it asserts | Task |
|---|---|---|
| `island/IslandSemanticsTest.kt` (new, `IslandOverlayCrashTest` harness) | **Pill:** `ContentDescription` and `StateDescription` from IslandSpeech; `OnClick` label "Expand"; `OnLongClick` label "Open in <app>"; idle pill has no description. **Custom actions:** labels per state, executed through `config[SemanticsActions.CustomActions]`, with state changes asserted; ringing call has no Dismiss. **Card and backdrop:** `PaneTitle` on `CARD`; backdrop "Close island" with `TraversalIndex` 1. **Live region:** `LiveRegion` Polite or none per the C3.7 map, and the headline is stable across ticks. **Auto-collapse:** paused under `a11yFocusInside`. | C3.1, C3.4–C3.8 |
| `island/IslandSpeechTest.kt` | Every headline and value row; ICU durations; locale formatting | C3.3 |
| `island/IslandA11yTest.kt` | Screen-reader detection ignores Droidbubble's own service | C3.2 |
| `service/SpokenAlertsTest.kt` (extend) | No speech while the screen reader is on | C3.9 |
| `ui/main/PermissionCatalogTest.kt`, `PermissionSection` Compose test | Level order; no "Not now" on Required; "Allow <title>" labels | C1.1, C1.2 |
| `data/SettingsRepositoryRoundTripTest.kt` | `skippedPermissions` and `pausedUntilEpochMs` survive a write and read; other fields are preserved | C1.2, C4.1 |
| `ActivityPriorityList` / `ValueChip` / `AppFilterSheet` / `UpdateRow` Compose tests | Move actions and position state; steady chip width; filter checked state; update states | C5.2–C5.6 |
| `DndPolicyTest`, `tileModel` test | Truth tables | C4.3, C4.4 |
| Font-scale bounds tests (fontScale 1.0, 1.3, 2.0) | Card text inside `CARD` bounds; compact text within the cap | C3.10 |

### Automated: instrumented (`app/src/androidTest`, run on a device or emulator before a release)
- Replace the stale `MainScreenTest.kt` (C3.1).
- Add Accessibility Test Framework checks to the companion tests (`enableAccessibilityChecks()` in recent Compose `ui-test`, plus the ATF dependency). This catches touch targets under 48 dp, missing labels and low contrast on: the main screen, the setup section in every pending state, the Dimensions page, and the filter sheet. Verify the API name against the pinned Compose BOM before adding it.
- The overlay can't be driven from instrumented tests reliably. The island is covered by Robolectric plus the manual script.

### Manual TalkBack script
Run it on a Pixel (Android 14+). Repeat steps 1–4 on a Samsung (One UI). Turn TalkBack on, with Droidbubble's accessibility service on, unless noted.

1. **Reach the pill.** Main screen › Test Live Activities › Timer. Swipe right until the island is focused.
   - Expect: "Timer, 4 minutes, 59 seconds left. Double-tap to expand. Double-tap and hold to open in Clock. Actions available."
   - Repeat with Droidbubble's accessibility service off (C3.1 spike).
2. **Expand.** Double-tap.
   - Expect: "Island: Timer" announced once.
   - Swipe through: the hero reads as a duration, Pause/+1:00/Cancel have clear labels, and "Close island" is last.
3. **No cut-off.** With focus inside the card, wait 30 s.
   - Expect: it doesn't collapse.
   - Then move focus off the island: it collapses after your "Time to take action" setting (try 1 min).
4. **Actions menu.** Collapse the card, focus the pill, and open TalkBack's Actions menu.
   - Expect: Minimise, Hide until next update, Dismiss, Pause island for 1 hour.
   - Run Minimise: you hear "Island minimised", and double-tap restores it.
5. **Urgent events.** Trigger the Incoming call demo.
   - Expect: one assertive "Incoming call: Sam Carter". Answer and Decline are labelled; Dismiss isn't offered.
   - Trigger the Alarm demo: "Alarm: Wake up".
6. **No chatter.** Leave the Timer running in compact for 30 s without focusing it.
   - Expect: no per-second announcements.
   - Trigger a new message (if B3 is in, from a real app): one polite "<app>: <title>".
7. **TTS overlap.** Turn on Spoken Alerts, then trigger Time's up.
   - Expect: only TalkBack speaks.
8. **Large text.** Set font size to max and display size to large, then expand Message, Timer and Media.
   - Expect: card text is larger and nothing is clipped; the compact pill text is capped.
9. **Companion.**
   - Setup section: each row reads the title, level and explanation, then "Allow <title>".
   - Reorder list: "Move up" on Media gives "Position 1 of 5".
   - Filter sheet: the switches read "Show <app> on the island, on".
   - Dimensions: the reset buttons read "Reset Width".
10. **QS tile.** Open Quick Settings and focus the tile.
    - Expect: "Island, On". Double-tap: "Island, Paused until <time>". Double-tap again: "On".
11. **DND.** Turn on DND with default settings, and send a message from a non-priority contact.
    - Expect: no pop-in and no announcement. A running Timer stays visible.

Record the device, OS, TalkBack version and pass/fail for each step in the PR that closes the epic.

---

## Optional later (not planned; only if the owner asks)
- **Settings restructure** into Home / Activities / Look & feel / Behaviour / Accessibility / About, and a full onboarding stepper (05 §6). PLAN v2 keeps the current structure.
- **Companion typography overhaul:** a full `Typography` in `theme/Type.kt` (currently the template, with only `bodyLarge` overridden), and removing the 51 weight overrides (01 R7).
- **UI for `ShizukuHelper.setSystemStatusBarIconsHidden`** (`util/ShizukuHelper.kt:82`). If it's exposed, it must restore the global `policy_control` setting.
- **Moving all island and companion copy into `strings.xml`** for localisation. C3.3 moves only the spoken strings.

---

## Execution order

Legend: **[early]** = can start before Phase 3, and most before Phase 2 · **[‖]** = can run in parallel with the item above it, because it touches different files · **[MS]** = edits `MainScreen.kt` (one session at a time unless C5.1 has landed).

1. **C3.1** a11y test harness and TalkBack device spike [early]. Do it first: the spike decides whether C3 needs a window change.
2. **C5.1** split `MainScreen.kt` [early] [MS]. Optional, but do it now if more than one MainScreen session will run.
3. **C1.1** permission catalogue [early] [‖ with 4–6].
4. **C3.2** `IslandA11y` [early] [‖].
5. **C3.3** `IslandSpeech` [early] [‖].
6. **C2.1** preview sample [early] [‖].
7. **C1.2** permission rows [early] [MS].
8. **C1.3** accessibility disclosure [early] [MS].
9. **C1.4** restricted-settings helper [early] [MS].
10. **C1.5** Bluetooth on demand [early] [MS].
11. **C1.6** guided first run [early] [MS]. Needs C2.1 for "Show sample".
12. **C3.9** Spoken Alerts defers to TalkBack [early] [‖ with 7–11].
13. **C5.2** per-app filter [early] [MS].
14. **C5.3** quieter update row and notification [early] [MS].
15. **C5.4 + C5.5** chips and "Coming soon" removal [early] [MS].
16. **C5.6** reorder list a11y [early] [MS].
17. **C4.1** pause model [early] [‖ with 13–16]. Land it after B1 and B8 to avoid service conflicts.
18. **C4.2** notification Pause/Resume [early] (service plus a small [MS] row).
19. **C4.3** QS tile [early] [‖ with MainScreen tasks, except the add-tile row].
20. **C2.3** reset buttons [early] [MS].
21. **C2.2** live preview on pages [MS]. After B8.
22. **C4.4** DND awareness. After B10, or folded into A2 if the arbiter is in flight.
23. **C2.4** Fit to camera [MS]. After V1.
24. **C3.4** pill and bubble semantics. After M4; runs on `IslandView.kt`, one session at a time.
25. **C3.5** custom actions. After C3.4 and B6.
26. **C3.7** live-region announcements. After C3.4.
27. **C3.6** expanded card, backdrop and control labels. After M3 and B5.
28. **C3.8** auto-collapse pause under a11y focus. After F3 and F5 [‖ with 24–27, since it's in the service rather than `IslandView.kt`, apart from the one effect-key line].
29. **C3.10** font scale up to 2.0. After F4, V4 and M3; last.

Close the epic by running the manual TalkBack script (all 11 steps) and recording the results.
