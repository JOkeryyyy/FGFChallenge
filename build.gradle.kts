// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.api.attributes.Bundling
import org.gradle.api.file.FileCollection

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.paparazzi) apply false
}

val ktlint by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(
            Bundling.BUNDLING_ATTRIBUTE,
            objects.named(Bundling.SHADOWED),
        )
    }
}

dependencies {
    ktlint(libs.ktlint.cli)
}

fun Project.registerKtlintTask(
    name: String,
    taskDescription: String,
    format: Boolean,
    ktlintClasspath: FileCollection,
): TaskProvider<JavaExec> =
    tasks.register<JavaExec>(name) {
        group = "verification"
        description = taskDescription
        classpath = ktlintClasspath
        mainClass.set("com.pinterest.ktlint.Main")
        inputs.files(
            fileTree(rootDir) {
                include("**/*.kt", "**/*.kts")
                exclude("**/.gradle/**", "**/build/**", "**/.codegraph/**")
            },
        )
        if (format) {
            args("--format")
        }
        args(
            "**/*.kt",
            "**/*.kts",
            "!**/.gradle/**",
            "!**/build/**",
            "!**/.codegraph/**",
        )
    }

registerKtlintTask(
    name = "ktlintCheck",
    taskDescription = "Checks Kotlin sources and Gradle Kotlin scripts.",
    format = false,
    ktlintClasspath = ktlint,
)
registerKtlintTask(
    name = "ktlintFormat",
    taskDescription = "Formats Kotlin sources and Gradle Kotlin scripts.",
    format = true,
    ktlintClasspath = ktlint,
)
