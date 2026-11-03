plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.wladyslawpopov"
version = "2.1.0"

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "paging-compose", version.toString())

    pom {
        name.set("KPage paging-compose")
        description.set("Compose Multiplatform bindings for KPage: connects lazy lists to the paginator.")
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

kotlin {
    jvmToolchain(21)

    android {
        namespace = "io.github.wladyslawpopov.kpage.compose"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                // Types of these libraries are part of the public API (paginator types, LazyListState).
                api(project(":paging-core"))
                api(libs.runtime)
                api(libs.foundation)

                implementation(libs.ui)
                implementation(libs.lifecycle.runtime.compose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ui.test)
            }
        }

        // Native Skiko binaries of the build machine: for UI tests only, never in the published library.
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
