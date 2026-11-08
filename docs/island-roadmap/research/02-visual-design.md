# Visual design audit: shape, geometry, color, materials, icons

I made no edits; everything was read-only. Line numbers refer to the working tree on `island-polish`. Following the user's update, each recommendation below states the principle behind it, then an Android-native way to apply it. Apple is used as a reference, not a spec to copy.

## 1. Apple reference

**Apple-documented** (HIG Live Activities, 393×852 / 430×932 pt screens):
- **Compact:** each of the leading and trailing regions is 52.33×36.67 pt on the 393 screen and 62.33×36.67 pt on the 430 screen.
- **Minimal:** 36.67 to 45 pt wide, 36.67 pt tall. It is "circular or oval depending on content". Up to three activities show at once: one attached to the island, the others detached beside it.
- **Expanded:** 371×84–160 pt on the 393 screen and 408×84–160 pt on the 430 screen. That leaves about 11 pt of margin each side.
- **Landscape (limited width):** content must fit a 28 pt circle.
- **Corner radius:** no number is given. Apple says to use `ContainerRelativeShape`, nest shapes concentrically with even margins, and use an inset container or a thick line to separate blocks rather than drawing to the edge.
- **Margins:** the Lock Screen standard margin is 14 pt. `contentMargins(_:_:for:)` overrides margins for each mode.
- **Color:** the background is always opaque black. A keyline appears on dark backgrounds, and you should tint it (`keylineTint`) to match the activity. Bold color carries app identity. Use a logo mark without its container, not the whole app icon.
- **Near the camera:** in compact, content sits snug against the sensor with no padding, and leading and trailing are balanced (shorten units if needed). In expanded, content wraps tightly around the sensor with no empty "forehead".
- **Avoid:** ads or promotions; drawing attention to the island; more than one interactive element in compact; content drawn to the edge; unnecessary height; activities running past about 8 h. Animations are capped at 2 s.
- **WWDC23 additions:** use extra-rounded, heavier shapes and heavy text. Check optical centring by blurring the content. Keep elements in the same relative place from compact to expanded.
- **iOS 26:** `ConcentricRectangle` / `.containerConcentric` compute inner corner radii automatically.

**Community-measured / estimated:**
- The hardware island is about 126×37.33 pt at y = 11 pt (DynamicIslandUtilities; one gist says 126×36.9).
- The iPhone 14 Pro display corner radius is 55 pt (private `_displayCornerRadius`, via ScreenCorners).
- Expanded corner radius is about 44 pt. This is my own estimate (55 pt display radius minus the 11 pt inset), not a published figure.
- The keyline looks like a hairline of about 1 pt (estimated).

**Android side:**
- Material Symbols axes: FILL 0–1, weight 100–700, grade −25 to 200, optical size 20–48. Google recommends grade −25 for light icons on dark backgrounds.
- Google has stopped publishing the Compose `material-icons-extended` library. It now says to use Material Symbols vector drawables instead.

## 2. Droidbubble today

**What is already good**
- `SquircleShape` (IslandShapes.kt:31) draws continuous corners in Figma's smoothing style at 0.6, and fades the smoothing out as the shape reaches a capsule. The morph radius is therefore continuous.
- `LiveLayout` (LiveActivities.kt:328) is properly concentric: `cornerRadius = control/2 + margin`. At the default scale of 1.1 that gives a 42.9 dp radius, 15.4 dp margin and an 85.8 dp one-row card, which matches Apple's 84 pt minimum.
- `CompactContactAvatar` (IslandView.kt:2629) keeps an even black ring around the photo.

**Geometry**
- **The camera is never measured.** Nothing in the code reads the system's cutout information (no `DisplayCutout` or `boundingRects` hits). Position depends on hand-tuned settings: top offset 12, width 180, height 36. The middle of the pill is a fixed `Spacer(36.dp)` copied 5 times (IslandView.kt:2022, 2060, 2105, 2173, 2867).
- **Two margin systems.** Live-activity cards use `LiveLayout.margin` (15.4 dp). Every other card uses `islandCardContentPadding` (IslandComponents.kt:237–249): 22 dp at scale 1.1, or 26/24 dp for FPS, weather and network, which add another `padding(4.dp, 2.dp)` on top. All cards share the 42.9 dp corner, but the inner radii are not derived from it:

  | Element | Location | Actual radius | Concentric would be |
  |---|---|---|---|
  | Media art (59 dp) | IslandView.kt:3445 | 12 dp | ~21 dp |
  | 40 dp tiles | :4008, 4285, 4402, 4534 | 10 dp | ~17 dp |
  | Panels | :4437, 4562, 4593 | 10–12 dp | — |
  | Action buttons (44–50 dp tall) | IslandComponents.kt:467 | 14 dp | ~23 dp, i.e. a capsule |
  | Compact album art | IslandView.kt:2782 | 5 dp | 10 dp, i.e. a circle |

- **Mixed shape families.** Inner shapes use plain `RoundedCornerShape` (circular arcs) inside the squircle outer shape. `notchPillShape` is circular as well (IslandShapes.kt:140).
- **Heights.** Fixed-height cards at scale 1.1: media and weather 214 dp, flashlight 226 dp, FPS and network 204 dp (IslandComponents.kt:220–231). Apple's ceiling is 160 pt. Card width is `340.dp*scale` (IslandView.kt:917), which is 374 of 412 dp, about 19 dp margins.
- **Fixed compact width.** The pill is 180 dp whatever it shows. Flashlight's trailing side is empty (IslandView.kt:2886), so the pill looks lopsided. The secondary bubble is always a 36 dp circle (:1696), never an oval.
- **Dead or ignored settings:**
  - `cornerRadiusDp` has no UI and only affects the collapsed morph target (:960).
  - `expandedCornerRadiusFor()`, `RadiusCard`, `SurfaceSunken` and `HighContrastOutline` are never used.
  - `outlineColorArgb` is never read by IslandView.

**Color**
- **High contrast is partly broken (accessibility bug).**
  - IslandOverlayService.kt:690–695 sets the outline to 55% white, but the island draws `primaryOutlineColor` at 15% alpha instead (IslandView.kt:542–558).
  - 9 literal `Color(0xFFA1A1AA)` texts bypass the high-contrast-aware `TextSecondary` token (e.g. the FPS, weather, network and idle views).
- **Hardcoded values:**

  | File | dp literals | Off the 4 dp grid (>2 dp) | Hex colors | Token references |
  |---|---|---|---|---|
  | IslandView | 250 | 102 | 79 | 44 |
  | IslandComponents | 85 | 43 | 0 | 36 |
  | LiveActivities | 27 | 18 | 9 (all centralised in `LiveActivityColors`, good) | 12 |

  The single worst literal is `#E5E5EA`, used 21 times.
- **Palette drift:**
  - Three reds for "recording": FF3B30 (:542), FF453A (:1420), FF5A47 (LiveActivities.kt:286).
  - Two greens (30D158, 34C759) and two yellows (FFD60A, FFCC00).
  - Two grey families: Apple greys (8E8E93, 2C2C2E, 1C1C1E) alongside Tailwind zinc (A1A1AA, 71717A, 52525B at :3894–3925).
- **Album-art accent is a plain RGB average** of a 16×16 downscale (IslandView.kt:329–378). It comes out muddy or grey for colorful covers and near-black for dark ones. No tone or chroma floor is applied before it is used for outlines, gradients and slider tints.
- **Legibility risks:**
  - Glass cards fade to 30% black when the blur is available (:1076). White text over a bright wallpaper can fall below 3:1.
  - The art backdrop is art at 75% under a 38% black scrim, with no scrim at the top where the title sits (:4680–4687). Light covers give roughly a mid-grey behind white text.
- The keyline is opt-in, 1.5 dp, and the same on light and dark wallpapers.

**Icons**
- The island mixes `Icons.Default` (Filled, 34 uses) and `Icons.Rounded` (21 uses). MusicNote, for example, is Filled in compact (:2785) and Rounded in expanded (:3453). LiveActivities uses Rounded throughout.
- Compact slots use 6 different glyph sizes: 14, 15, 16, 17, 18 and 19 dp, plus 20 dp art.
- Header tiles are fixed at 40/22 dp in the FPS, weather and network views, but scale as `40.dp*scale` in the notification and progress views.
- The deprecated `material-icons-extended` library is still a dependency (build.gradle.kts:89).

**Morph (overlaps with the motion agent)**
- The card grows from width 0 rather than from the pill's outline, while the pill shrinks to 0 at the same time (IslandView.kt:902, 1037). It is two bodies, not one.
- On collapse the black container itself fades (`cardContainerAlpha`, :1084), leaving a grey ghost shape.

## 3. Gaps

| # | Pri | Gap | Impact users see |
|---|---|---|---|
| 1 | P0 | No cutout calibration; fixed 36 dp centre gap | Pill off-centre around the camera; content can sit on the camera; poor first impression |
| 2 | P0 | High-contrast outline and text broken | The accessibility setting does almost nothing |
| 3 | P1 | Two margin systems; inner radii not concentric | Cards look assembled from different kits; corners "fight" |
| 4 | P1 | Muddy accent extraction; duplicate semantic colors | Washed-out media tint; inconsistent reds and greens |
| 5 | P1 | Glass and art scrims too light | Text unreadable on bright wallpapers or covers |
| 6 | P1 | Mixed icon styles and sizes; deprecated library | Visible weight and size wobble between states |
| 7 | P1 | Cards 30–40% taller than needed; fixed heights | Card covers too much screen; empty space |
| 8 | P1 | Fixed compact width; unbalanced flashlight; circle-only bubble | Lopsided pill; text cramped in the bubble |
| 9 | P2 | Mixed shape families; circular notch | Subtle corner mismatch |
| 10 | P2 | Dead tokens and settings; 79 hex literals | Drift keeps coming back |
| 11 | P2 | Two-body morph; fading container | Grey ghost and "drop" seam during the morph |

## 4. Recommendations

**R1. Measure the cutout (P0, M).** Principle: blend with the hardware.
- **How:** in IslandOverlayService, read `WindowInsets.displayCutout`, using `boundingRectTop` and on API 31+ `cutoutPath.computeBounds`. From the camera's centre and diameter, derive a new `IslandGeometry`:
  - `cameraZone = cutoutW + 2×6dp`
  - `H = max(36dp, cutoutH + 2×8dp)`
  - default top and horizontal offsets centred on the camera
- The existing sliders become fine-tune deltas. Replace the five `Spacer(36.dp)` with `geometry.cameraZone`. The minimised circle then becomes truly concentric with the punch hole.
- **Files:** IslandOverlayService.kt, IslandSettings.kt (add `autoCalibrate`), IslandView.kt.
- **Risk:** phones without a cutout, under-display cameras or rotation; fall back to today's defaults.

**R2. Fix high contrast (P0, S).**
- **How:** when `IslandTokens.highContrast` is on, use `Color(settings.outlineColorArgb)` or `HighContrastOutline` for all four borders. Replace the literal A1A1AA, 8E8E93 and 71717A colors with `TextSecondary` / `TextTertiary`.
- **Files:** IslandView.kt.
- **Risk:** low.

**R3. One concentric geometry system (P1, M).** Principle: concentric nesting with one margin.
- **How:** promote `LiveLayout` into a shared `IslandGeometry(margin, cardRadius)` with `fun inner(inset: Dp, min: Dp = 6.dp) = (cardRadius - inset).coerceAtLeast(min)`. Provide it through a `LocalIslandContainer` CompositionLocal, and add a `Modifier.concentricClip(inset)`, the Compose counterpart of `ContainerRelativeShape`.
- Retire `islandCardContentPadding` in favour of `margin`.
- Bottom action rows become capsules: at 44–50 dp tall a 23 dp radius already is one.
- **Files:** IslandTokens, IslandShapes, IslandComponents, IslandView, LiveActivities.
- **Risk:** `measureNotificationCardHeight` and `measureProgressCardHeight` must change in lockstep; add screenshot tests.

**R4. Droidbubble's own color system (P1, M).** Principle: a black base with sparing, bold accents.
- Define semantic roles: `Recording`, `Success`, `Warning`, `Info`, `MediaAccent`, each one color only.
- Build them from a Droidbubble tonal palette using HCT tones: about 75–80 for foreground on black, about 25–30 for tinted containers. Add an optional "Match wallpaper" mode using `dynamicDarkColorScheme(context)`.
- **Album art:** replace the average with `androidx.palette` (vibrant, then dominant), or vendor Material Color Utilities `QuantizerCelebi` + `Score`, which is what SystemUI's media player uses. Clamp L* (via `ColorUtils.colorToLAB`) to 70–80 and require a minimum chroma, otherwise fall back to neutral.
- **Files:** IslandTokens, LiveActivityColors, `extractMediaAccentColor`.
- **Dependency:** palette-ktx.

**R5. Legibility scrims (P1, S).**
- **How:** keep glass at a minimum of 0.55 alpha behind text. For the art backdrop, compute the blurred art's luminance (already in hand) and choose a scrim alpha so the composite has relative luminance ≤ 0.18, which keeps white text at 4.5:1 or better. Add a top gradient under the title.
- **Files:** IslandView.kt:1074–1082 and 4664–4710.

**R6. Material Symbols Rounded (P1, M).** Principle: one icon family and weight, sized to a grid.
- **How:** export vector drawables from Google Fonts with Rounded style, weight 500, grade −25, optical size 20 for compact and 24 for expanded, and FILL 1 for active states.
- Size tokens: `IconCompact` 18 dp, `IconBubble` 16 dp, `IconHeader` 24 dp, controls at 0.46 × the control size (already the rule).
- Drop `material-icons-extended`, which also cuts APK size.
- **Files:** res/drawable, build.gradle.kts, the three island files.
- **Risk:** some glyphs and names differ, so review each one.

**R7. Content-hugging compact, oval minimal (P1, M).** Principle: no wasted width, balanced sides.
- **How:** compact width = `cameraZone + 2·max(leadW, trailW) + 2·inset`, measured with `SubcomposeLayout`; `widthDp` becomes the maximum. Give flashlight a trailing level ("75%" or dots). Let the bubble width run from H to 1.25 H for short text.
- **Files:** IslandView.kt:893–911, 984, and `CompactContent` / `AuxiliaryBubbleContent`.
- **Risk:** bubble resting position and parenthesis maths assume a fixed width.

**R8. Height discipline (P1, M).** Principle: don't take unnecessary height.
- **How:** measure media, weather, FPS and network cards from their content, as notifications already are. Target 84–176 dp at scale 1. Use two-row layouts with one hero value per card (already LiveLayout rule 3).
- **Files:** `cardHeightFor` in IslandComponents.kt and the views.

**R9. One shape family (P2, S).**
- **How:** use `islandShape()` for every inner rounded rect 24 dp or larger. Give `SquircleShape` per-corner radii so the notch can use it. Alternative: `androidx.graphics:graphics-shapes` `RoundedPolygon` with `CornerRounding(r, smoothing)`, which also enables `Morph`.

**R10. Adaptive keyline (P2, S).** Principle: a tinted edge separates the black island from dark backgrounds.
- **How:** `Modifier.islandKeyline(accent)` drawn with `drawWithCache` as a 1 dp stroke inset 0.5 dp, at about 20% accent. Turn it on automatically when the wallpaper is dark (`WallpaperManager.getWallpaperColors()`, `HINT_SUPPORTS_DARK_TEXT` unset) or night mode is on.

**R11. Token hygiene (P2, M).**
- **How:** delete or wire up the dead tokens and `cornerRadiusDp`. Move the 79 hex literals into tokens. Snap spacing to the 4 dp grid, with 2 dp only for hairlines. Add a CI grep or detekt rule that bans `Color(0x` outside the token files.

**R12. One-body morph (P2, S; coordinate with the motion agent).**
- **How:** start CardMorph from the pill's rect (its current width and radius), keep the container at alpha 1, and fade only the content.

## 5. Droidbubble's own take

**Where to differ on purpose**
- **Detached expanded card**, 52 dp below the camera. On Android, status icons flank a small punch hole, so wrapping content around the camera is less natural. Keep it, but make the morph read as one body (R12).
- **"Orbit ring" signature.** In the minimised or bubble state, draw a thin accent progress ring hugging the circular punch hole for timers, downloads and charging. It only works with round Android cutouts, which makes it a natural Android identity.
- **Brackets "( )" as the brand mark** (IslandView.kt:2270). Tint them with the activity accent on dark wallpapers in place of Apple's full keyline: a "bracket keyline".
- **Material 3 Expressive shape morphing** for controls, such as play↔pause morphing between `MaterialShapes` and a squish on press. Material 3 1.5.0-alpha04 is already a dependency.
- **Keep the existing distinct features:** the charging "tank fill" pill, the notch style, and the liquid-neck split.
- **Own corner tuning.** Use a smoothing of about 0.45–0.5 as a "Droid corner" rather than the documented "≈ iOS 0.6", and consider exposing it as a style.

**Near-literal Apple clones (flag)**
- **The palette** is iOS dark-mode system colors almost verbatim (30D158, FF9F0A, 64D2FF, BF5AF2, FF453A, FFD60A, 1C1C1E, 2C2C2E, 3A3A3C, 8E8E93, E5E5EA, F2F2F7).
- **"Liquid glass"** is the name of Apple's iOS 26 material. Rename it, for example to "Frosted cards".
- **The expanded media layout** (art top-left, equaliser top-right, scrubber with times, three transport buttons) mirrors the iOS Now Playing activity.
- **The ringer HUD** (red slashed bell with "Silent") and the unlock "lock + check" pill copy iOS system HUDs.
- **The squircle smoothing** is tuned to iOS.

## 6. Proposed tokens

| Presentation | Width | Height | Radius | Margins / inset |
|---|---|---|---|---|
| Camera ring (minimised) | H | H = max(36, cutout + 16) dp | H/2 | ring ≥ 6 dp, concentric with camera |
| Compact pill | cameraZone + 2·max(lead, trail) + 2·inset; 120 dp min to `widthDp` max | H | H/2 | inset (H − 20)/2 ≈ 8 dp; glyph 18 dp, art 20 dp circle |
| Minimal bubble | H to 1.25 H | H | H/2 | gap 6 dp |
| System alert pill (charging, unlock…) | compact + 20 dp | H | H/2 | 8 dp |
| Expanded row card | min(screen − 24 dp, 400 dp) | control + 2m ≈ 84–90 dp | R = control/2 + m ≈ 43 dp | m = 14 dp × scale (12–18) |
| Expanded tall card | same | measured, cap ~176 dp × scale | R (single value) | m; inner radius = max(R − inset, 6 dp) |
| Stacked cards | — | — | — | gap 10 dp |

| Color role | Value |
|---|---|
| `island.surface` | #000000, opaque at all times, including during the morph |
| `island.container` | white 8% (tiles) / 14% (secondary buttons) / 28% when high contrast |
| `onIsland` | primary #FFF; secondary white 64% (high contrast 85%); tertiary 48%; disabled 30% |
| `accent.*` | Droidbubble HCT palette, tone 75–80 on black (≥ 4.5:1); tinted containers at tone 25–30 or accent 22% |
| `accent.media` | Palette/Score result, L* clamped 70–80, minimum chroma |
| `keyline` | accent 20% at 1 dp; high contrast white 55% at 1.5 dp |
| `scrim` | glass ≥ 0.55 behind text; art adaptive to ≤ 0.18 relative luminance |

## Sources
- [Apple HIG: Live Activities](https://developer.apple.com/design/human-interface-guidelines/live-activities)
- [WWDC23: Design dynamic Live Activities](https://developer.apple.com/videos/play/wwdc2023/10194/)
- [keylineTint(_:)](https://developer.apple.com/documentation/widgetkit/dynamicisland/keylinetint(_:))
- [contentMargins(_:_:for:)](https://developer.apple.com/documentation/widgetkit/dynamicisland/contentmargins(_:_:for:))
- [DynamicIslandUtilities](https://github.com/theblixguy/DynamicIslandUtilities)
- [ericlewis gist](https://gist.github.com/ericlewis/3d7a89599d50ae51b44b6050e591a48f)
- [ScreenCorners](https://github.com/kylebshr/ScreenCorners)
- [Nil Coalescing: ConcentricRectangle](https://nilcoalescing.com/blog/ConcentricRectangleInSwiftUI)
- [CreateWithSwift: concentricity](https://www.createwithswift.com/exploring-concentricity-in-swiftui/)
- [Material Symbols guide](https://developers.google.com/fonts/docs/material_symbols)
- [Compose icons package (deprecation note)](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary)
