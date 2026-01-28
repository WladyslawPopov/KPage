import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.cash.sqldelight")
    id("maven-publish")
}

object Versions {
    const val libraryVersion = "1.0.0"
    const val packageName = "io.github.wladyslawpopov.kpager.core"

    const val compileSdk = 36
    const val minSdk = 24

    const val sqlDelight = "2.0.2"
    const val serialization = "1.7.3"
    const val datetime = "0.6.1"
    const val koin = "4.0.0"
}

group = Versions.packageName
version = Versions.libraryVersion

sqldelight {
    databases {
        create("PagingDatabase") {
            packageName.set("io.github.wladyslawpopov.kpager.cache")
        }
    }
}


kotlin {
    androidLibrary {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    android {
        namespace = Versions.packageName
        compileSdk = Versions.compileSdk
    }

    val xcfName = "paging-coreKit"
    iosX64 { binaries.framework { baseName = xcfName } }
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.ui:ui:1.10.0")
                implementation("org.jetbrains.compose.foundation:foundation:1.10.0")
                implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.datetime}")
                implementation("app.cash.sqldelight:coroutines-extensions:${Versions.sqlDelight}")

                api("io.insert-koin:koin-core:${Versions.koin}")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        androidMain {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:${Versions.sqlDelight}")
            }
        }

        nativeMain {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:${Versions.sqlDelight}")
            }
        }

        jvmMain.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:${Versions.sqlDelight}")
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
