plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "io.github.wladyslawpopov.kpage.demo.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true } // Compose Resources need Android resources in a KMP library
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            // Swift works with these modules directly (states, components, PagingListTracker).
            export(project(":presentation"))
            export(project(":ui-core"))
            export(project(":domain"))
            export(libs.kpage.core)
        }
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":presentation"))
            api(project(":ui-core"))
            api(project(":domain"))
            api(libs.kpage.core)
            implementation(project(":data"))
            api(libs.koin.core)
            api(libs.compose.runtime)
            api(libs.compose.components.resources)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "io.github.wladyslawpopov.kpage.demo.shared.resources"
}
