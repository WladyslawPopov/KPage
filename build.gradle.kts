plugins {
    id("com.android.kotlin.multiplatform.library") version "9.0.0" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.3.0" apply false
    id("org.jetbrains.compose") version "1.10.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    id("app.cash.sqldelight") version "2.2.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
