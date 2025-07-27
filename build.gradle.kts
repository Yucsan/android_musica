 // Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // ✅ AGREGAR HILT
    id("com.google.dagger.hilt.android") version "2.48" apply false

    // ✅ AGREGAR KOTLINX SERIALIZATION
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" apply false
}

// Los repositorios están configurados en settings.gradle.kts