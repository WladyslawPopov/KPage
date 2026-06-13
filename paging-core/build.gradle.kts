plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.sqldelight)
    id("maven-publish")
}

group = "io.github.wladyslawpopov"
version = "1.0.1"

sqldelight {
    databases {
        create("PagingDataBase") {
            packageName.set("io.github.wladyslawpopov.kpager.cache")
        }
    }
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "io.github.wladyslawpopov.kpager.core"
        compileSdk = 37
    }

    val xcfName = "paging-coreKit"
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.ui)
                implementation(libs.runtime)
                implementation(libs.material3)
                implementation(libs.components.resources)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.sqldelight.coroutines)

                implementation(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.ui.test)
                implementation(libs.koin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqldelight.android)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.sqldelight.native)
            }
        }

        jvmMain.dependencies {
            implementation(libs.sqldelight.jvm)
            implementation(compose.desktop.currentOs)
        }
    }
}
