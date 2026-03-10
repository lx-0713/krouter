plugins {
    kotlin("jvm")
    id("maven-publish")
    id("signing")
}

group = findProperty("GROUP")?.toString() ?: "com.kmp"
version = findProperty("KROUTER_COMPILER_VERSION")?.toString() ?: "1.0.0"

dependencies {
    // KSP 符号处理 API
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.16")
}

// 确保与主项目 JVM 版本一致（Java 17）
kotlin {
    jvmToolchain(17)
}

// Maven Central 要求提供 sources jar 和 javadoc jar
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // 空 javadoc jar，满足 Maven Central 的最低要求
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
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            artifactId = "krouter-compiler"
            pom {
                name.set("krouter-compiler")
                description.set("KSP compiler for KRouter: generates route registration from @KRoute annotations.")
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
