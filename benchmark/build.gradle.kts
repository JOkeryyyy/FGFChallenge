// Configures the out-of-process Macrobenchmark APK that measures the app's benchmark variant.
// It is a `com.android.test` module: it builds no library, and its only output is the test APK
// that drives `:app`'s benchmark variant. See `documentation/performanceBenchmark.md`.
plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.example.fgfchallenge.benchmark"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Macrobenchmark itself requires API 29; the measured app still supports API 26.
        minSdk = 29
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Reduces unrelated background work around each measured iteration.
        testInstrumentationRunnerArguments["listener"] =
            "androidx.benchmark.macro.junit4.SideEffectRunListener"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    // Matches the app's release-like target build type by name. The fallback is for the library
    // modules that have no benchmark variant of their own — `:core:network` and
    // `:core:designsystem` resolve to release, exactly as they do for `:app:assembleBenchmark`.
    buildTypes {
        create("benchmark") {
            // Applies to the *test* APK only — the measured app stays non-debuggable and
            // profileable. Without it the instrumentation process is not allowed to write app-tag
            // atrace events, so the named interaction sections never reach the Perfetto trace and
            // TraceSectionMetric reports nothing.
            isDebuggable = true
            // Signed with the debug key so it installs beside the target; the measured app is the
            // one that must stay release-like, not this harness.
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}
