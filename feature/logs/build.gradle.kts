import com.android.build.api.variant.HostTestBuilder

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.example.fgfchallenge.feature.logs"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Mirrors the app's Macrobenchmark target so this module's `benchmark` source set — the
    // deterministic 100k fixture and its refresher — is selected instead of falling back to release.
    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// AGP 9 creates host tests only for the `testBuildType`, so the benchmark fixture's contract tests
// would have no task to run in. They are unit tests of benchmark-only source, so the variant that
// owns that source is the one that must be able to test it.
androidComponents {
    beforeVariants(selector().withBuildType("benchmark")) { variantBuilder ->
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

// That variant's test task would otherwise also run the shared `src/test` suite a second time,
// including the Paparazzi goldens — which are recorded against, and verified for, the debug variant
// only. This task exists for `src/testBenchmark`, so it runs exactly that: every benchmark-variant
// test class is named `*BenchmarkTest`/`Benchmark*Test`, and `failOnNoMatchingTests` makes a rename
// that escapes the pattern fail loudly instead of silently running nothing.
// `configureEach` rather than `named`: AGP registers the variant's test task after this script is
// evaluated, so looking it up by name here would not find it.
tasks.withType<Test>().configureEach {
    if (name == "testBenchmarkUnitTest") {
        filter {
            includeTestsMatching("*Benchmark*Test")
            isFailOnNoMatchingTests = true
        }
    }
}

dependencies {
    // Temporary bridge while presentation remains in this legacy module; Task 3 moves it.
    implementation(projects.feature.logs.data)
    implementation(projects.core.designsystem)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.compose)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Collects the ViewModel's `Flow<PagingData<…>>` without a Compose host.
    testImplementation(libs.androidx.paging.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.assertk)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
