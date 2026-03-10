plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("signing")
}

group = findProperty("GROUP")?.toString() ?: "com.kmp"
version = findProperty("KROUTER_VERSION")?.toString() ?: "1.0.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "krouter"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose Runtime（KRouterComponent.Content() 需要 @Composable）
                api(compose.runtime)

                // Decompose 核心（路由栈能力）
                val decomposeVersion = "3.1.0"
                api("com.arkivanov.decompose:decompose:$decomposeVersion")

                // 序列化（KRouteConfig 需要）
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // 协程（KRouter 事件总线 + KRouterComponent scope）
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        }
        val androidMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.kmp.krouter"

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

// Maven Local for testing; Sonatype for Maven Central (can publish alone or with root publishToSonatype)
publishing {
    repositories {
        mavenLocal()
        maven {
            name = "sonatype"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME") ?: ""
                password = findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name)
            description.set("Kotlin Multiplatform router with Decompose and Compose.")
            url.set("https://github.com/lx-0713/krouter")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://opensource.org/licenses/Apache-2.0")
                }
            }
            developers {
                developer {
                    id.set("lx-0713")
                    name.set("lixiong")
                }
            }
            scm {
                url.set("https://github.com/lx-0713/krouter")
                connection.set("scm:git:git://github.com/lx-0713/krouter.git")
                developerConnection.set("scm:git:ssh://git@github.com/lx-0713/krouter.git")
            }
        }
    }
}

signing {
    val keyId = findProperty("signing.keyId")?.toString() ?: System.getenv("SIGNING_KEY_ID")
    val secretKey = findProperty("signing.secretKey")?.toString() ?: System.getenv("SIGNING_SECRET_KEY")
    val password = findProperty("signing.password")?.toString() ?: System.getenv("SIGNING_PASSWORD")
    val keyFile = findProperty("signing.secretKeyRingFile")?.toString()?.let { path -> file(path) }
    if (keyId != null && password != null && (secretKey != null || keyFile?.exists() == true)) {
        if (secretKey != null) {
            useInMemoryPgpKeys(keyId, secretKey, password)
        } else {
            useInMemoryPgpKeys(keyId, keyFile!!.readText(), password)
        }
        sign(publishing.publications)
    }
}
