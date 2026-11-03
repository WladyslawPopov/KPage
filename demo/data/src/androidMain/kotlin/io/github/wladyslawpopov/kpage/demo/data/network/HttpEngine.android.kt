package io.github.wladyslawpopov.kpage.demo.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

internal actual fun createHttpEngine(): HttpClientEngine = Android.create()
