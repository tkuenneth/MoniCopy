import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.*
import java.io.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val javafxVersion = "20"
val javafxPlatform = run {
    val os = System.getProperty("os.name").lowercase(Locale.US)
    val arch = System.getProperty("os.arch")
    when {
        os.contains("win") -> "win"
        os.contains("mac") && arch == "aarch64" -> "mac-aarch64"
        os.contains("mac") -> "mac"
        else -> "linux"
    }
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
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                listOf("javafx-base", "javafx-graphics", "javafx-controls").forEach { module ->
                    implementation("org.openjfx:$module:$javafxVersion:$javafxPlatform")
                }
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

tasks.withType<JavaExec>().configureEach {
    if (name != "run") return@configureEach
    doFirst {
        val javafxJars = classpath.filter { it.name.startsWith("javafx-") }
        if (javafxJars.isEmpty) return@doFirst
        classpath = classpath - javafxJars
        jvmArgs(
            "--module-path", javafxJars.asPath,
            "--add-modules", "javafx.controls",
        )
    }
}

compose.desktop {
    application {
        mainClass = "com.thomaskuenneth.monicopy.Launcher"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MoniCopy"
            packageVersion = version.toString()
            description = "An easy to use folder copy app"
            copyright = "2017 - 2023 Thomas Kuenneth. All rights reserved."
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
