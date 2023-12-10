@file:Suppress("DSL_SCOPE_VIOLATION")

import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("root.publication")
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.spotless).apply(false)
}

allprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/")
            ktlint(libs.versions.ktlint.get())
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/build/")
            ktlint(libs.versions.ktlint.get())
        }
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions { freeCompilerArgs += "-Xexpect-actual-classes" }
    }
}
