plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.fgfchallenge.feature.logs.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }
}
