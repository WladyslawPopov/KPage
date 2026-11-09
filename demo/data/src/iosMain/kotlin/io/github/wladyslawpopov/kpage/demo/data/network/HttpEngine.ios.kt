package io.github.wladyslawpopov.kpage.demo.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun createHttpEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        // Right after the phone wakes up the network is not ready yet: without this a request fails or
        // hangs until its timeout, with it the system holds the request until a connection appears.
        waitsForConnectivity = true
    }
}
