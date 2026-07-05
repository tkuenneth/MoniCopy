import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.*
import java.util.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "com.thomaskuenneth.monicopy"
val properties = Properties()
val file = rootProject.file("src/jvmMain/resources/version.properties")
if (file.isFile) {
    InputStreamReader(FileInputStream(file), Charsets.UTF_8).use { reader ->
        properties.load(reader)
    }
} else error("${file.absolutePath} not found")
version = properties.getProperty("VERSION")

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.components.resources)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("io.insert-koin:koin-core:4.1.1")
                implementation("io.insert-koin:koin-compose:4.1.1")
                implementation("io.insert-koin:koin-compose-viewmodel:4.1.1")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.2.0")
                implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.2.0")
                implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.2.0")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
            }
        }
        val jvmTest by getting
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.thomaskuenneth.monicopy.generated.resources"
}

compose.desktop {
    application {
        mainClass = "com.thomaskuenneth.monicopy.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MoniCopy"
            packageVersion = version.toString()
            description = "An easy to use folder copy app"
            copyright = "2017 - 2026 Thomas Kuenneth. All rights reserved."
            vendor = "Thomas Kuenneth"
            macOS {
                bundleID = "com.thomaskuenneth.monicopy"
                iconFile.set(project.file("artwork/MoniCopy.icns"))
            }
            windows {
                iconFile.set(project.file("artwork/MoniCopy.ico"))
                menuGroup = "Thomas Kuenneth"
            }
        }
    }
}
