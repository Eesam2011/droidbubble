// Macrobenchmarks for the island (S4.2): frame timing of expand and collapse on a real device.
// Run: ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest   (results: macrobenchmark/BASELINE.txt)
plugins {
  alias(libs.plugins.android.test)
}

// Build outputs live outside OneDrive, like the app's (see app/build.gradle.kts)
// -PdroidbubbleBuildRoot=<dir> gives a second checkout (a git worktree) its own outputs
layout.buildDirectory.set(File(providers.gradleProperty("droidbubbleBuildRoot").orNull
    ?: File(System.getProperty("user.home"), ".droidbubble-build").path, "macrobenchmark"))

android {
    namespace = "com.droidbubble.macrobenchmark"
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

    buildTypes {
        // Matches the app's benchmark build type: release code, debug signing, profileable
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
}

androidComponents {
    beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" }
}
