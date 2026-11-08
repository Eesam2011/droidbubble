plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Build outputs live outside OneDrive: syncing thousands of intermediate files on every build
// slowed it down a lot. APKs end up in ~/.droidbubble-build/app/outputs/apk/
layout.buildDirectory.set(File(System.getProperty("user.home"), ".droidbubble-build/app"))

android {
    namespace = "com.droidbubble.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.droidbubble.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 10
        versionName = "2.0"
    }

    buildTypes {
        debug {
            // Releasable debug apk
        }
        release {
            // R8 optimizes Compose code paths, which noticeably reduces animation jank in release builds
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    base {
        archivesName.set("Droidbubble")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    // Skip the lint scan that runs on every release build (lintVitalRelease): it was one of the
    // slowest steps and doesn't change the APK. Run ./gradlew lint by hand when needed.
    lint {
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric: the island crash tests render the real overlay Compose UI on the JVM
        unitTests.isIncludeAndroidResources = true
        // Record mode for the golden tables (S1.3, S5.3):
        // ./gradlew :app:testDebugUnitTest --tests "*IslandCharacterizationTest" -Pisland.record=true
        unitTests.all { test ->
            test.systemProperty("island.record", project.findProperty("island.record")?.toString() ?: "false")
        }
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.datastore.preferences)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(composeBom)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation("org.robolectric:robolectric:4.16.1")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Installs the baseline profiles bundled with Compose at install time, so animations run
  // precompiled instead of JIT-warming (first expands no longer stutter on sideloaded APKs)
  implementation("androidx.profileinstaller:profileinstaller:1.4.1")

  // Labelled trace sections (util/IslandTrace.kt); already on the classpath through Compose
  implementation("androidx.tracing:tracing:1.2.0")
}
