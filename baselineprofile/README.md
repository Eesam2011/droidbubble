# Baseline profile (F6.3)

`IslandBaselineProfileGenerator` records which app code the island journeys run (the S4 journeys
in `macrobenchmark/src/journeys`: app and service start, compact appear, expand, collapse, a few
other demos). The resulting profile ships in the APK, and `profileinstaller` has ART compile that
code at install time, so the first expands don't stutter while Compose JIT-warms.

The profile is saved to `app/src/main/generated/baselineProfiles/baseline-prof.txt` (and
`startup-prof.txt`) and committed. Normal builds never regenerate it; until it exists, the app
ships only the profiles bundled with its libraries (Compose and others).

## Generate

Before the run, on the phone:

- the app installed and signed with the same key as the build below, so the install keeps its data;
- the island turned on in the app and its accessibility service enabled;
- optional: something playing in a media app, so the media card and the dual split are covered too.

Then, with the phone connected over adb (Android 13+, or a rooted phone):

```
bash tools/gradle-locked.sh :app:generateBaselineProfile
```

(`./gradlew :app:generateBaselineProfile` in the main checkout.) Leave the phone alone during the
run. Commit the files it writes under `app/src/main/generated/baselineProfiles/`.

**The Gradle connected task uninstalls the app afterwards**, which drops its settings and
permission grants, as for the macrobenchmark (see `macrobenchmark/BASELINE.txt`).

## Check that it helps

`IslandFirstExpandBenchmark` in `:macrobenchmark` compares the first expand after a cold start
with the profile (`withBaselineProfile`) and with no compilation (`withoutCompilation`); P90
frame duration should be lower with the profile. Run it as `macrobenchmark/BASELINE.txt` shows,
with `-e class com.droidbubble.macrobenchmark.IslandFirstExpandBenchmark`.
