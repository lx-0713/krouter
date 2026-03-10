plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    kotlin("multiplatform").apply(false)
    kotlin("plugin.serialization").version("1.9.21").apply(false) // 请和 KMP Kotlin 版本对齐
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("org.jetbrains.compose").apply(false)
    id("com.google.devtools.ksp").version("1.9.21-1.0.16").apply(false)
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = findProperty("GROUP")?.toString() ?: "io.github.lx-0713"

nexusPublishing {
    packageGroup = findProperty("GROUP")?.toString() ?: "io.github.lx-0713"
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(
                providers.gradleProperty("ossrhUsername")
                    .orElse(providers.environmentVariable("OSSRH_USERNAME")).getOrElse("")
            )
            password.set(
                providers.gradleProperty("ossrhPassword")
                    .orElse(providers.environmentVariable("OSSRH_PASSWORD")).getOrElse("")
            )
        }
    }
}
