// The demo tour (UiAutomator) in its own process, over the release build of `:androidApp`: the app runs
// on the real clock and gets the touches from the system, as from a person's finger.
plugins {
    alias(libs.plugins.androidTest)
}

kotlin { jvmToolchain(21) }

android {
    namespace = "io.github.wladyslawpopov.kpage.demo.tour"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        create("tour") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    targetProjectPath = ":androidApp"
    // The test is not inside the app's process: it can clear and start the app.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants { it.enable = it.buildType == "tour" }
}

dependencies {
    implementation(project(":ui-core"))
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.uiautomator)
}
