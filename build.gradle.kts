plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    kotlin("multiplatform").apply(false)
    kotlin("plugin.serialization").version("1.9.21").apply(false) // 请和 KMP Kotlin 版本对齐
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("org.jetbrains.compose").apply(false)
    id("com.google.devtools.ksp").version("1.9.21-1.0.16").apply(false)
}
