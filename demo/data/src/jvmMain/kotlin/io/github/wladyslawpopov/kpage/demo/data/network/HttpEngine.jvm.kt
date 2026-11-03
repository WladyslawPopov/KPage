package io.github.wladyslawpopov.kpage.demo.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

internal actual fun createHttpEngine(): HttpClientEngine = Java.create()
