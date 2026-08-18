plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.fgfchallenge.feature.logs"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }
}
