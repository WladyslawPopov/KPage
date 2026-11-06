package io.github.wladyslawpopov.kpage.demo.android

import android.app.Application
import android.content.Context
import io.github.wladyslawpopov.kpage.demo.shared.di.initKoin
import org.koin.dsl.module

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(module { single<Context> { this@DemoApplication } })
    }
}
