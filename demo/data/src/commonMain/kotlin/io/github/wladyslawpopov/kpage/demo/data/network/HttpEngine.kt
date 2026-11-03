package io.github.wladyslawpopov.kpage.demo.data.network

import io.ktor.client.engine.HttpClientEngine

internal expect fun createHttpEngine(): HttpClientEngine
