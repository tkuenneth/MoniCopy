import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.*
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.testballoon)
}

group = "com.thomaskuenneth.monicopy"
val properties = Properties()
val file = rootProject.file("src/main/resources/version.properties")
if (file.isFile) {
    InputStreamReader(FileInputStream(file), Charsets.UTF_8).use { reader ->
        properties.load(reader)
    }
} else error("${file.absolutePath} not found")
version = properties.getProperty("VERSION")
    ?: error("VERSION not found in ${file.absolutePath}")
val buildVersion = properties.getProperty("BUILD_VERSION")
    ?: error("BUILD_VERSION not found in ${file.absolutePath}")
val isMacOs = System.getProperty("os.name").contains("Mac", ignoreCase = true)

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

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
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.aboutlibraries.compose.m3)
    testImplementation(libs.testballoon.framework.core)
    testImplementation(kotlin("test"))
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.thomaskuenneth.monicopy.generated.resources"
}

aboutLibraries {
    export {
        prettyPrint = true
        outputFile = file("src/main/composeResources/files/aboutlibraries.json")
    }
}

tasks.named("copyNonXmlValueResourcesForMain").configure {
    dependsOn("exportLibraryDefinitions")
}

compose.desktop {
    application {
        mainClass = "com.thomaskuenneth.monicopy.MainKt"
        if (isMacOs) {
            jvmArgs(
                "-Xdock:icon=${project.file("src/main/composeResources/drawable/app_icon.png").absolutePath}",
            )
        }
        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MoniCopy"
            packageVersion = version.toString()
            description = "An easy-to-use folder copy app"
            copyright = "Copyright 2017 - 2026 Thomas Kuenneth"
            vendor = "Thomas Kuenneth"
            licenseFile.set(project.file("LICENSE"))
            modules("java.instrument", "java.prefs", "jdk.unsupported")
            macOS {
                bundleID = "com.thomaskuenneth.monicopy"
                iconFile.set(project.file("artwork/MoniCopy.icns"))
                packageBuildVersion = buildVersion
                appCategory = "public.app-category.utilities"
                signing {
                    // Local runDistributable stays unsigned (multiple Developer ID
                    // certs in login). GitHub Actions sets CI=true and signs there.
                    sign.set(
                        providers.environmentVariable("CI")
                            .map { it == "true" }
                            .orElse(false)
                    )
                    identity.set("Thomas Kuenneth")
                }
            }
            windows {
                iconFile.set(project.file("artwork/MoniCopy.ico"))
                menuGroup = "Thomas Kuenneth"
                upgradeUuid = "7a7524f4-596b-44d2-82c2-8551c0e3fff9"
                shortcut = true
            }
            linux {
                iconFile.set(project.file("src/main/composeResources/drawable/app_icon.png"))
            }
        }
    }
}
