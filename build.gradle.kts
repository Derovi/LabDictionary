import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.3"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
}

group = "by.derovi.botp2p"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.alessiop86.anti-antibot-cloudflare:anti-antibot-cloudflare-okhttpclient:1.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.seruco.encoding:base62:0.1.3")
    implementation("com.nimbusds:nimbus-jose-jwt:8.19")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:4.5.0")
    implementation("net.sourceforge.htmlunit:htmlunit:2.51.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.codeborne:phantomjsdriver:1.5.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.5.0")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
