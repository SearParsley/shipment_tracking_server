import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting // Ensure this line is present if you use desktopTest

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // ADDED: Core coroutines library for common module
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // Also ensure you have libs.junit.jupiter.api here if commonTest uses JUnit 5 assertions
            // For example: implementation(libs.junit.jupiter.api)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        desktopTest.dependencies {
            // These are for the test runner and coroutine testing
            runtimeOnly(libs.junit.jupiter.engine)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}


compose.desktop {
    application {
        mainClass = "marcus.hansen.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "marcus.hansen"
            packageVersion = "1.0.0"
        }
    }
}

// ADD THIS BLOCK AT THE VERY END OF THE FILE, OUTSIDE OF OTHER BLOCKS
tasks.withType<Test> {
    useJUnitPlatform() // This configures the test task to use JUnit 5 as the underlying runner
}