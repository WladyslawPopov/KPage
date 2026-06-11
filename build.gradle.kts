plugins {
    id("com.android.kotlin.multiplatform.library") version "9.2.1" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.0" apply false
    id("org.jetbrains.compose") version "1.11.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
    id("app.cash.sqldelight") version "2.3.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
