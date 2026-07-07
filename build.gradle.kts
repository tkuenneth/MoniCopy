import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.*
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.koin.compiler)
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
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.components.resources)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.core.viewmodel)
                implementation(libs.koin.annotations)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.compose.adaptive)
                implementation(libs.compose.adaptive.layout)
                implementation(libs.compose.adaptive.navigation)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
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
        jvmArgs(
            "-Xdock:icon=${project.file("src/commonMain/composeResources/drawable/app_icon.png").absolutePath}",
        )
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
