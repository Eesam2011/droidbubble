// Baseline profile generator for the app (F6.3). It reuses the S4 journeys
// (macrobenchmark/src/journeys). Generate with a phone connected: see README.md.
plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.androidx.baselineprofile)
}

// Build outputs live outside OneDrive, like the app's (see app/build.gradle.kts)
// -PdroidbubbleBuildRoot=<dir> gives a second checkout (a git worktree) its own outputs
layout.buildDirectory.set(File(providers.gradleProperty("droidbubbleBuildRoot").orNull
    ?: File(System.getProperty("user.home"), ".droidbubble-build").path, "baselineprofile"))

android {
    namespace = "com.droidbubble.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The island journeys, shared with :macrobenchmark
    sourceSets.getByName("main").java.srcDir("../macrobenchmark/src/journeys/java")

    targetProjectPath = ":app"
}

// Runs on the phone connected over adb, not a Gradle managed device: the island needs the real
// accessibility service and overlay permission
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
}
