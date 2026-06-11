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
version =  "1.0.1"

sqldelight {
    databases {
        create("PagingDataBase") {
            packageName.set("io.github.wladyslawpopov.kpager.cache")
        }
    }
}


kotlin {
    android {
        namespace = "io.github.wladyslawpopov.kpager.core"
        compileSdk = 37
    }

    val xcfName = "paging-coreKit"
    iosX64 { binaries.framework { baseName = xcfName } }
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    jvm()

    val composeVersion = "1.10.3"

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.ui:ui:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
                implementation("app.cash.sqldelight:coroutines-extensions:2.3.2")

                implementation("io.insert-koin:koin-core:4.2.1")
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

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-junit"))// JUnit 4
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            implementation("app.cash.turbine:turbine:1.2.1")
            implementation("org.jetbrains.compose.ui:ui-test:$composeVersion")
            implementation(compose.desktop.currentOs)
        }
    }
}

tasks.withType<Test> {
    useJUnit()
    // useJUnitPlatform() //JUnit 5
}

publishing {
    repositories {
        mavenLocal()
    }
}
