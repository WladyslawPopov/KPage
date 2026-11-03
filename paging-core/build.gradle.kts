plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.wladyslawpopov"
version = "2.1.0"

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "paging-core", version.toString())

    pom {
        name.set("KPage paging-core")
        description.set("Kotlin Multiplatform paging library with stable pagination and persistent cache.")
        url.set("https://github.com/WladyslawPopov/KPage")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("WladyslawPopov")
                name.set("Wladyslaw Popov")
                url.set("https://github.com/WladyslawPopov")
            }
        }
        scm {
            url.set("https://github.com/WladyslawPopov/KPage")
            connection.set("scm:git:git://github.com/WladyslawPopov/KPage.git")
            developerConnection.set("scm:git:ssh://git@github.com/WladyslawPopov/KPage.git")
        }
    }
}

sqldelight {
    databases {
        create("PagingDatabase") {
            packageName.set("io.github.wladyslawpopov.kpage.cache")
        }
    }
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "io.github.wladyslawpopov.kpage.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    val xcfName = "paging-coreKit"
    iosArm64 { binaries.framework { baseName = xcfName } }
    iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                // Types of these libraries are part of the public API (StateFlow, KSerializer,
                // SqlDriver), so consumers get them too.
                api(libs.kotlinx.serialization.json)
                api(libs.sqldelight.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
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
        }
    }
}
