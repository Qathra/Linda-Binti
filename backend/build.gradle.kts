plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.lindabinti"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.lindabinti.backend.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback.classic)
    implementation(libs.bcrypt)
    implementation(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.gson)
    implementation(libs.sqlite.jdbc)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnit()
}

tasks.register<JavaExec>("devResetAdmin") {
    group = "development"
    description = "Reset NGO admin development password (local dev only; prints once to console)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.lindabinti.backend.dev.DevAdminPasswordResetKt")
    workingDir = projectDir
    environment("LINDA_DEV_ADMIN_RESET", "1")
}