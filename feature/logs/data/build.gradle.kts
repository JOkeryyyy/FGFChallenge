import com.android.build.api.variant.HostTestBuilder

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.fgfchallenge.feature.logs.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

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
}

// AGP 9 creates host tests only for the selected test build type. The benchmark fixture has
// benchmark-only unit tests, so its owning variant must register a host-test task.
androidComponents {
    beforeVariants(selector().withBuildType("benchmark")) { variantBuilder ->
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

// Run only benchmark-variant contract tests, not the shared suite a second time.
tasks.withType<Test>().configureEach {
    if (name == "testBenchmarkUnitTest") {
        filter {
            includeTestsMatching("*Benchmark*Test")
            isFailOnNoMatchingTests = true
        }
    }
}

dependencies {
    implementation(projects.core.network)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.runtime)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit.kotlinx.serialization)
}
