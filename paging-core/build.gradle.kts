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

group = "io.github.wladyslawpopov.kpager.core"
version =  "1.0.0"

sqldelight {
    databases {
        create("PagingDataBase") {
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
        namespace = "io.github.wladyslawpopov.kpager.core"
        compileSdk = 36
    }

    val xcfName = "paging-coreKit"
    iosX64 { binaries.framework { baseName = xcfName } }
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.ui:ui:1.10.3")
                implementation("org.jetbrains.compose.foundation:foundation:1.10.3")
                implementation("org.jetbrains.compose.runtime:runtime:1.10.3")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.3.2")

                implementation("io.insert-koin:koin-core:4.2.0")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        androidMain {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.3.2")
            }
        }

        nativeMain {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.3.2")
            }
        }

        jvmMain.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.3.2")
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
