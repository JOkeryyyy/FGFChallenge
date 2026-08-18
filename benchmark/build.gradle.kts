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
            "androidx.benchmark.junit4.SideEffectRunListener"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    // Matches the app's release-like target build type by name, so no fallback is needed.
    buildTypes {
        create("benchmark")
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
