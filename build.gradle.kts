import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    application
}

group = "id.walt.webwallet"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.walt.id/repository/waltid/")
    maven("https://maven.walt.id/repository/waltid-ssi-kit/")

    maven("https://maven.walt.id/repository/danubetech")
    maven {
        url = uri("https://maven.pkg.github.com/microblock-tau/edclexcel2ebsi")
        credentials {
            username = System.getenv("USERNAME")
            password = System.getenv("TOKEN")
        }
    }

    mavenLocal()
    maven("https://repo.danubetech.com/repository/maven-public/")
}

dependencies {
    implementation("io.javalin:javalin-bundle:4.3.0")
    implementation("com.github.kmehrunes:javalin-jwt:0.3")
    implementation("com.beust:klaxon:5.5")
    implementation("com.nimbusds:oauth2-oidc-sdk:9.27")

    // SSIKIT
    implementation("id.walt:waltid-ssi-kit:1.7-SNAPSHOT")
    implementation("id.walt:waltid-ssikit-vclib:1.15.0-SNAPSHOT")

    // Service-Matrix
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.1.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.0-alpha6")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha6")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")

    implementation( "fi.tuni.microblock:edclexcel2ebsi:0.2.0-snapshot")



    // Testing
    //testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.12.2")

    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-assertions-core:5.1.0")
    testImplementation("io.kotest:kotest-assertions-json:5.1.0")

    // HTTP
    testImplementation("io.ktor:ktor-client-core:1.6.7")
    testImplementation("io.ktor:ktor-client-cio:1.6.7")
    testImplementation("io.ktor:ktor-client-serialization:1.6.7")
    testImplementation("io.ktor:ktor-client-logging:1.6.7")
    testImplementation("io.ktor:ktor-client-jackson:1.6.7")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("id.walt.MainKt")
}
