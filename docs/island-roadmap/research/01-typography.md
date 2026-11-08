# 01 · Typography: research and audit

Scope: island (compact, bubble, expanded, Live Activities) plus the companion app. The working tree was read on branch `island-polish`. Apple is used as a **reference for principles, not a spec to clone**.

Confidence labels: **[A]** Apple-documented · **[C]** community-measured or estimated · **[V]** my own inference, verify on a device.

---

## 1. Apple reference

| Topic | What Apple does | Label |
|---|---|---|
| Typeface | The system face is SF Pro, with **dynamic optical sizes**: Text and Display merge into one continuous design, so you never pick a cut. Rounded variants exist for a softer voice. | [A] HIG Typography |
| Weight | Live Activities should use "large, heavier text", medium weight or higher, and small text only sparingly. Avoid Ultralight, Thin and Light. | [A] HIG Live Activities, WWDC23 10194 |
| Size floor | iOS default is 17 pt and the minimum is **11 pt**. The Dynamic Type "Large" sizes are Headline 17/22 Semibold, Body 17/22, Subhead 15/20, Footnote 13/18, Caption1 12/16, Caption2 11/13. | [A] |
| Tracking | Tracking is tuned per size for SF: 11 pt +6/1000 em, 12 pt 0, 13 pt −6, 15 pt −16, 17 pt −26, 20 pt −23, 24 pt +3, 28 pt +14, 38 pt +10. Display sizes go *positive* because the optical size tightens the design itself. | [A] |
| Compact text | Hard numbers are not published. One teardown measured compact text at 15 pt on a 22 pt line in a 36.67 pt-high island. | [C] Infinum (2022) |
| Geometry | Compact leading and trailing are 52.33×36.67 pt (393-wide phone) and 62.33×36.67 pt (430-wide). Minimal is 36.67–45×36.67 pt. In width-limited contexts, fit content inside a **28 pt circle**. Expanded is 371/408 pt wide by 84–160 pt high. | [A] |
| Truncation | **Avoid truncation.** Shorten units or show less precise data so leading and trailing stay balanced. If both don't fit, show only the leading view. | [A] |
| Consistency | Use the same color and typography for leading and trailing so they read as one piece of information. | [A] |
| Numbers | The minimal view may show live values, such as the remaining time on a timer. Use numeric content transitions for changing values and fade a timer out at zero. In SwiftUI: `.monospacedDigit()`, `.contentTransition(.numericText(countsDown:))` (iOS 16), `numericText(value:)` (iOS 17), and the self-updating `Text(timerInterval:countsDown:)`. | [A] |
| Width jitter | A timer-style `Text` in compact trailing reserves too much width. The community workaround is a fixed frame plus `minimumScaleFactor`. | [C] Apple Dev Forums 723316 |
| Color | Island text is white on opaque black, which developers cannot change. Use bold brand color in text for identity. In Dark Mode a keyline can be tinted. | [A] |
| Secondary text | `secondaryLabel` is roughly 60% of the label color (#EBEBF5), which works out to about #8D8D93 on black. | [C] (values come from UIKit, not HIG prose) |
| License | The SF fonts are licensed **only for mock-ups of software for Apple operating systems**. No embedding, no redistribution, no non-Apple OS. **SF Pro cannot ship in an Android APK.** | [A] developer.apple.com/fonts |

---

## 2. Droidbubble today

**Counts (island + service):**
- 59 `Text(` calls: IslandView 42, LiveActivities 13, Components 4. No `BasicText`.
- **33 hard-coded `fontSize = N.sp` literals across 11 distinct values:** 8, 10, 10.5, 11, 11.5, 12, 12.5, 13, 14, 15, 16.
- Only 12 references to `IslandTokens.Type*`. LiveActivities uses none.
- **Four parallel scale systems** each redefine "title":
  - `NotificationCardMetrics` and `ProgressCardMetrics` (IslandComponents.kt:254–293)
  - Media inline (IslandView.kt:3411–3412)
  - `LiveLayout` (LiveActivities.kt:347–351)
- Weights: SemiBold 32, Medium 28, Normal 3, Bold 3, Black 1.
- No `fontFamily` anywhere, so the island renders in whatever font the OEM or FlipFont provides.
- No `LineHeightStyle` anywhere.
- Explicit `lineHeight` only on the notification and progress body text.

**What is already good (keep):**
- Tabular digits are set island-wide: `LocalTextStyle … fontFeatureSettings = "tnum", letterSpacing = 0.sp` (IslandOverlayService.kt:712).
- `RollingNumberText` (LiveActivities.kt:399): tnum, per-character directional roll, and the whole string laid out once so the colon never moves. This is Droidbubble's own equivalent of `numericText`, and it is better than a crossfade.
- Media time labels use tnum and a **fixed width measured from a "0"-template** (IslandView.kt:3383–3396). This is the textbook fix for width jitter.
- Hero numbers deliberately don't grow with font scale (LiveActivities.kt:346–348).
- Font scale is capped to 0.85–1.5 (IslandOverlayService.kt:713).

**Findings:**

1. **Every island Text inherits a 24 sp line height.**
   - Line 712 copies M3 `bodyLarge` (16/24 sp, `LineHeightStyle` Center/Trim.None) and only overrides tnum and tracking.
   - So 11 sp captions, 13 sp subtitles and the 38–42 sp heroes all get a 24 sp line box (× fontScale).
   - Effects:
     - The `SpaceXxs = 2.dp` "title → subtitle" token is a fiction: about 4–5 dp of invisible padding sits inside each line box (IslandComponents.kt:113–131).
     - Small labels float in tall boxes.
     - **[V] Possible hero clipping:** a 41.8 sp hero (38 × 1.1) gets a 24 dp box. `RollingNumberText` clips to that box *during a roll* (LiveActivities.kt:478–480), which could shave the digit tops and bottoms for 380 ms.
2. **tnum escapes.** These calls use `style = TextStyle(...)`, which *replaces* `LocalTextStyle`:
   - IslandView.kt:4579, 4585, 4610, 4616.
   - These are the network speed values, the fastest-ticking numbers in the app.
   - On stock Roboto the digits are tabular by default anyway **[V]**, so the jitter shows mainly on OEM fonts (One UI Sans, OPPO Sans, MiSans, FlipFont), assuming those fonts honor `tnum`.
3. **Variable-length numeric strings in compact:**
   - `"${pct}%"` (2911)
   - `"${fps} FPS"` (2942)
   - `"↓ ${downloadSpeedFormatted}"` (2959): the formatter switches between "999 KB/s" and "1.2 MB/s" (PersistentWidgetTelemetryManager.kt:233–235)
   - Battery % (2030)
   - Each text's inner edge moves next to the camera gap, and none of these slots has a fixed-width template.
4. **Compact text is small and unbalanced.** Apple asks for large, heavy text that matches across both sides. The island is 36 dp tall by default, the same as Apple's 36.67 pt, yet:
   - Leading labels are 10 / 10.5 sp at 85% white: weather 2832, network 2854, FPS 2807.
   - Trailing values are 10.5 / 11.5 / 12 sp in accent colors.
   - Leading text uses ellipsis truncation (2768, 2836).
5. **Detached bubble: an 8 sp percentage inside a 17 dp ring** (IslandView.kt:2535).
   - That is below Apple's 11 pt floor and below Material's 11 sp `labelSmall`.
   - At font scale 1.3, "100" is wider than the ring.
   - The FPS bubble is the only `FontWeight.Black` in the island (2589).
6. **`expandedScale` (default 1.1) is applied unevenly:**
   - Notification, progress, media header and LiveLayout scale.
   - FPS, weather, network, idle and the media time labels don't (4119, 4319–4366, 4419–4507, 4579–4640, 3386).
   - Result at default settings: card titles are **17 / 17.6 / 18.7 sp** depending on the card. The card *height* scales (`cardHeightFor`), so unscaled cards just gain empty space.
7. **Tracking:**
   - Global 0, plus −0.025 em for numbers of 20 sp and up (LiveActivities.kt:387). That is reasonable for Roboto, which has no display cut.
   - There is no per-role tracking otherwise.
8. **Text colors mix two palettes:**
   - Tailwind zinc: #A1A1AA, #F4F4F5, #71717A.
   - Apple system grays: #8E8E93, #E5E5EA, #2C2C2E.
   - Ad-hoc `White.copy(alpha = 0.85f)`.
   - Accents are Apple's pre-iOS 26 dark system colors: #30D158, #FF9F0A, #FF453A, #FFD60A, #BF5AF2 (IslandTokens.kt:52–57).
9. **Companion app (MainScreen.kt, 78 Text calls):**
   - Only 15 use `MaterialTheme.typography`.
   - 9 sp literals: 11, 11, 11, 12, 12, 13, 13, 13.5, 16.
   - **51 weight overrides** (30 SemiBold, 16 Bold, 2 ExtraBold), e.g. `titleMedium + Bold` (1656). Bold is everywhere, so the hierarchy is flat.
   - `Type.kt` is the untouched template.
   - Selected segmented buttons switch Medium→Bold (2381), so the label width shifts on tap.
   - Slider value chips (2471) have no tnum.

---

## 3. Gaps, by priority

| Pri | Gap | User-visible impact |
|---|---|---|
| **P0** | Inherited 24 sp line height on all island text (finding 1) | Loose headers, spacing tokens that don't do what they say, possible hero clipping during the roll [V]. It also blocks every other spacing fix. |
| **P0** | tnum escapes on the network values (finding 2) | Speed digits wobble every second on OEM fonts |
| **P0** | 8 sp ring numeral in the bubble (finding 5) | Unreadable, and overflows the ring at large text sizes |
| **P1** | No bundled font | The island looks different on every OEM. SemiBold and Medium can snap to the same face on static OEM fonts, so the hierarchy collapses. tnum is not guaranteed. FlipFont can restyle the island. |
| **P1** | Four scale systems, 11 literal sizes, partial `expandedScale` (finding 6) | Title size jumps 10% between cards |
| **P1** | Compact text undersized and unbalanced, with ellipsis (finding 4) | The pill reads as "tiny status bar text", not as a glanceable activity |
| **P1** | Variable-length compact numbers (finding 3) | Edges shimmy next to the camera |
| **P2** | Tracking not tied to size or font | Fine on Roboto, wrong once a new font ships |
| **P2** | Mixed text-color palettes (finding 8) | Subtle inconsistency in secondary text |
| **P2** | Companion hierarchy (finding 9) | The settings app looks generic and heavy |
| **P2** | `Density(density, fontScale)` at service:713 is linear [V] | Probably drops Android 14's non-linear font scaling, so big text grows more than the platform intends |
| **P2** | `Locale.US` number formatting (`formatTimerText`, etc.) | Digits are never localized for Arabic or Persian users |

---

## 4. Recommendations

Each one follows the pattern principle, then the Android-native adaptation.

**R1 · One `IslandType` object and a real base style (P0)**
- **Principle:** Apple names a style for each role and never lets text inherit a body style by accident.
- **Android-native:** a Material-3-shaped role set owned by the island.
- **How:**
  - `@Immutable class IslandType(scale: Float, family: FontFamily)` exposing named `TextStyle`s: `hero`, `heroSmall`, `statValue`, `title`, `body`, `secondary`, `eyebrow`, `caption`, `button`, `compactValue`, `compactLabel`, `compactUnit`, `bubbleValue`.
  - Each role carries an explicit `lineHeight` and `lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.Both)`. For heroes, also use `LineHeightStyle.Mode.Minimum` if your Compose version has it [V], so the box never cuts glyphs.
  - Provide it through `staticCompositionLocalOf`, built with `remember(settings.expandedScale)` in `IslandOverlayRoot`.
  - At service:712, replace the `LocalTextStyle` override with `LocalTextStyle provides IslandType.body`, which kills the 24 sp inheritance.
  - Compact roles are *not* multiplied by `expandedScale`.
  - Fold the four metric classes into this object. `measure*CardHeight` must use the same styles, which it already does through merge.
- **Files:** IslandTokens.kt (or a new IslandType.kt), IslandComponents.kt, IslandView.kt, LiveActivities.kt, IslandOverlayService.kt.
- **Effort:** M. **Risk:** medium. Line boxes shrink, so fixed-height cards gain slack. Re-tune together with the layout/spacing agent.
- **Depends on:** the spacing tokens work. Let the motion agent know that `RollingNumberText` travel is `0.72 × box height`.

**R2 · Steady numbers everywhere (P0/P1)**
- **Principle:** monospaced digits plus a fixed slot, and abbreviate before you truncate.
- **How:**
  - Remove the `style = TextStyle(...)` escapes at 4579–4616, or use `IslandType.statValue`.
  - Add a small `NumericSlot(text, style, template)` that reserves the width of the longest template with digits mapped to "0". This generalizes the media time-label code (3383) to:
    - compact % and battery ("100%")
    - FPS ("000")
    - speed
  - Change the speed formatter to 3 significant digits ("870 KB/s", "1.23 MB/s", "12.3 MB/s"). In compact, show the number in `compactValue` and the unit in `compactUnit`, and drop the "↓" glyph when the leading icon already shows the direction.
- **Effort:** S. **Risk:** low.

**R3 · Bundle one variable font (P1)**
- **Principle:** Apple owns its typeface, so the island looks identical everywhere.
- **Android-native:** ship an OFL variable font in `res/font` and drive it through `FontVariation`.
  - minSdk 33 supports this fully.
  - Compose does not apply `opsz` automatically [V], so define Text and Display instances.
- **Snippet:** `Font(R.font.island_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.Setting("ROND", 100f), FontVariation.opticalSizing(40.sp)))`.
- **Bundle it, don't download it.** Downloadable fonts need Google Play Services, load asynchronously (the overlay would first draw in the fallback font), and fail on de-Googled ROMs. That matters for a GitHub-distributed open-source app.
- **Subsetting:** use fontTools `varLib.instancer` plus `pyftsubset` to Latin and Latin-ext, keeping only the needed axes and features (`tnum`, `lnum`, `case`, `calt`, `kern`, chosen `cv`/`ss`).
- **Fallback:** non-Latin notification text falls back to the system fonts automatically.
- **Licensing:** ship `OFL.txt` and add a licenses entry in the companion app.
- **Bonus [V]:** a variable weight axis makes Android's "Bold text" setting (fontWeightAdjustment +300) render smoothly instead of snapping.
- **Effort:** M. **Risk:** APK size, plus re-measuring card heights.
- **Must verify first:** `tnum` exists in the chosen file's GSUB (`ttx -t GSUB`).

| Candidate (OFL) | Full file (google/fonts) | Notes |
|---|---|---|
| **Google Sans Flex** | 3.91 MB; axes ROND 0–100, opsz 1–144, wdth 25–151, wght 1–1000, slnt | Android-native lineage. **ROND and wdth are signature tools.** Third-party sources say it has tabular figures [C], but verify. Trademark: the "Google" name can't be used in product names (TRADEMARKS.md). |
| **Inter 4** | 877 KB; opsz 14–32, wght 100–900 | Most mature: `tnum`, `zero`, `ss01` open digits, `cv01`–`cv13` [A, rsms.me/inter]. **Looks the most like SF**, so it carries clone risk. |
| Roboto Flex | 1.79 MB; 13 parametric axes | Native, but looks like stock Android. Less identity. |
| Figtree / Geist | 63 KB / 169 KB; wght only | Tiny. No opsz. Geist Mono is an option for telemetry numerals. |

**APK impact [C, estimate]:**
- Google Sans Flex instanced (wght 400–700, ROND, opsz, wdth 75–100) and Latin-subset: roughly 0.5–1.0 MB.
- Inter subset: roughly 0.3–0.5 MB.
- The current release APK is 3.54 MB. Measure with APK Analyzer.

**R4 · Compact balance and a size floor (P1)**
- **Principle:** compact text should be large, heavy and matched on both sides, with no truncation.
- **How:**
  - Leading and trailing both use `compactValue`/`compactLabel` at 13–14 sp, weight 600, the same color family. The accent goes on the value only, and leading labels use the primary color, not 85% white.
  - Order of operations when text doesn't fit: (a) squeeze the width axis to about 85, (b) abbreviate ("Partly cloudy" → "Cloudy", "Download" → nothing), (c) drop the leading text and keep the icon. **Never use an ellipsis in compact.**
  - Bubble: show the ring alone, or 11–12 sp at weight 700 with condensed width in a ring of at least 22 dp. Nothing below 11 sp.
- **Effort:** S–M. **Depends on:** the layout agent (slot widths).

**R5 · Text emphasis tokens (P2)**
- **Principle:** three emphasis levels plus an accent.
- **How:**
  - `TextPrimary` 100%, `TextSecondary` about 64%, `TextTertiary` about 42% white, plus the existing high-contrast bump.
  - Retire the Tailwind and Apple hex mix and the stray `alpha = 0.85f`.
- **Coordinate with:** the color agent.

**R6 · Tracking by font, not by Apple's table (P2)**
- Once a font with opsz ships, set tracking to 0 everywhere and let opsz do the work.
- Without opsz, keep the current −0.02 em on numbers of 20 sp and up.
- Labels set in caps get +0.04 em.
- Delete `numberLetterSpacing` only after testing.

**R7 · Companion app (P2)**
- Write a full `Typography` in Type.kt:
  - The brand face for display, headline and title roles and for all numeric chips.
  - `FontFamily.Default` for body and label roles, to respect the device font.
- Remove the 51 weight overrides.
- Make selection change color or fill, not weight.
- Add tnum to `labelMedium` value chips.
- Render the IslandStylePicker previews with `IslandType` so the previews match the real island.
- **Effort:** M.

**R8 · Font scale (P2, verify first)**
- Keep the 1.5× cap, but derive it from the platform's non-linear converter instead of building `Density(density, fontScale)` linearly.
- If that proves hard, cap compact text at about 1.3× and leave expanded text at 1.5×.

---

## 5. Droidbubble's own take

**Where to deliberately differ from Apple:**

1. **Typeface: Google Sans Flex, not an SF look-alike.** It comes from Android's own design lineage (Material 3 Expressive era), it is openly licensed, and its **ROND axis is the signature**:
   - Hero numerals (timers, scores, ETAs, speeds) at ROND 100: "soft numerals".
   - Body text and labels at ROND 0, so text stays crisp.
   - SF Pro Rounded exists, but Apple's families are fixed cuts. Continuous rounding is something only a variable font can do.
   - Optional experiment: tie roundness to state, e.g. live = 100 and paused = 40.
2. **Squeeze, then abbreviate, never ellipsize.** Use the width axis to condense compact text before abbreviating. This is a variable-font trick that is native to this stack. Apple relies on abbreviation and `minimumScaleFactor`.
3. **Keep `RollingNumberText` as a house signature.** It rolls each character up or down in the counting direction, which is distinct from Apple's numeric blur-morph.
4. **Use Material role names and a 4 dp line-height grid**, not Apple's Dynamic Type names and sizes.
5. **Respect Android settings:** font scale with a cap, and "Bold text" through the variable weight axis.
6. **Optional:** a faint Material You hue in secondary text on the black island, derived from wallpaper or album art the way the media accent already is.

**Near-literal clone risks (flagged):**
- Shipping SF Pro, or an "SF Pro Display.ttf" from a font-dump site. **That breaches the license, so don't.**
- Copying Apple's SF tracking table onto another font. It is a near-clone, and it is technically wrong because the table is tuned to SF's optical sizes.
- The **current** `IslandTokens` type scale (17/15/13/12/11) is exactly Dynamic Type Large Headline/Subhead/Footnote/Caption1/Caption2. The accents and grays are Apple's dark system colors (#30D158 is identical to today's HIG value). **Add Inter at SemiBold 17 to that and the island is an iOS clone.** Change at least two of the three: font, scale, palette.

---

## 6. Proposed type scale

Sizes are in sp at `expandedScale` 1.0. Expanded roles scale size and line height together. Compact and bubble roles don't scale. Every role uses `LineHeightStyle(Center, Trim.Both)`.

| Surface | Role | Size / LH | Weight | Tracking | Features / axes |
|---|---|---|---|---|---|
| Compact | compactValue | 14 / 16 | 600 | 0 | tnum, NumericSlot, ROND 100 |
| Compact | compactLabel | 13 / 16 | 600 | 0 | wdth 85–100 squeeze, no ellipsis |
| Compact | compactUnit (%, MB/s, °) | 11 / 16 | 500 | +0.01 em | secondary color |
| Bubble | bubbleValue | 12 / 14 (min 11) | 700 | 0 | tnum, wdth 80 for 3 digits |
| Expanded | hero | 40 / 44 | 600 | 0 (opsz 40) | tnum, lnum, ROND 100, Mode.Minimum [V] |
| Expanded | heroSmall | 28 / 32 | 600 | 0 (opsz 28) | tnum, ROND 100 |
| Expanded | statValue (network, FPS tiles) | 20 / 24 | 600 | 0 | tnum, ROND 100 |
| Expanded | title | 16 / 20 | 600 | 0 | marquee only for media and call titles |
| Expanded | body | 15 / 20 | 400 | 0 | up to 3 lines, ellipsis allowed |
| Expanded | secondary (subtitle) | 13 / 16 | 500 | 0 | TextSecondary |
| Expanded | eyebrow / label | 12 / 16 | 600 | +0.02 em | accent at reduced strength |
| Expanded | caption / meta | 11 / 14 | 500 | +0.01 em | tnum when numeric |
| Expanded | button | 14 / 18 | 600 | 0 | — |
| Companion | headlineSmall | 24 / 32 | 600 | 0 | brand face |
| Companion | titleLarge (top bar) | 22 / 28 | 600 | 0 | brand face |
| Companion | titleMedium (section headers) | 16 / 24 | 600 | 0 | brand face, replaces titleMedium + Bold |
| Companion | titleSmall (card titles) | 14 / 20 | 600 | 0 | brand face |
| Companion | bodyLarge / bodyMedium | 16 / 24, 14 / 20 | 400 | M3 default | system font |
| Companion | labelLarge (buttons, segments) | 14 / 20 | 600, fixed weight | 0.1 sp | — |
| Companion | labelMedium (value chips) | 12 / 16 | 600 | 0.5 sp | tnum |
| Companion | labelSmall (BETA badges) | 11 / 16 | 700 | +0.5 sp, caps | — |

At the default `expandedScale` of 1.1, the expanded title is 17.6 sp and the hero is 44 sp. The hero still grows with the row, not with font scale, as `LiveLayout` already does.

---

## Sources

- Apple HIG, Live Activities: https://developer.apple.com/design/human-interface-guidelines/live-activities
- Apple HIG, Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple HIG, Color: https://developer.apple.com/design/human-interface-guidelines/color
- WWDC23, "Design dynamic Live Activities": https://developer.apple.com/videos/play/wwdc2023/10194/
- Creating custom views for Live Activities: https://developer.apple.com/documentation/activitykit/creating-custom-views-for-live-activities
- `numericText(countsDown:)`: https://developer.apple.com/documentation/swiftui/contenttransition/numerictext(countsdown:)
- Apple Developer Forums, timer width in compact: https://developer.apple.com/forums/thread/723316
- Apple fonts and license: https://developer.apple.com/fonts/
- Infinum, Dynamic Island design guide: https://infinum.com/?p=28092
- Inter: https://rsms.me/inter/
- Google Sans Flex: https://design.google/library/google-sans-flex-font
- Google Sans Flex metadata and trademarks, and font file sizes: github.com/google/fonts (`ofl/googlesansflex`, `ofl/inter`, `ofl/robotoflex`, `ofl/figtree`, `ofl/geist`)
- Google Sans Flex vs Inter: https://fontalternatives.com/blog/google-sans-flex-vs-inter/
