plugins {
    kotlin("jvm")
}

dependencies {
    // KSP 符号处理 API
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.16")
}

// 确保与主项目 JVM 版本一致（Java 17）
kotlin {
    jvmToolchain(17)
}
