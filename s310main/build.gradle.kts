buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP のバージョンを 8.2.2 に統一 (Android Studio のバージョンと互換性のある最新の安定版)
        classpath("com.android.tools.build:gradle:8.2.2") // ★ここを 8.2.2 に修正★
        classpath("com.google.gms:google-services:4.4.2")
        // Kotlin Gradle Plugin のバージョンを 1.9.23 に統一
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    }
}

plugins {
    // Android Gradle Plugin のバージョンを buildscript と同じに
    id("com.android.application") version "8.2.2" apply false // ★ここを 8.2.2 に修正★
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}