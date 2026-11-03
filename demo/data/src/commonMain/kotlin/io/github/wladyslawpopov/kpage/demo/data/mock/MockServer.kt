package io.github.wladyslawpopov.kpage.demo.data.mock

import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkException
import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkFailureKind
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How the fake server behaves; it can be changed while the app runs. */
data class MockServerConfig(
    val latency: Duration = 600.milliseconds,
    /** Share of calls that fail with a server error, 0..1. */
    val failureRate: Float = 0f,
    val isOffline: Boolean = false,
) {
    init {
        require(failureRate in 0f..1f) { "failureRate must be in 0..1: $failureRate" }
    }
}

/** A server inside the app: every call waits like the network and fails like it. */
internal class MockServer(
    initial: MockServerConfig = MockServerConfig(),
    private val random: Random = Random.Default,
) {
    private val mutableConfig = MutableStateFlow(initial)
    val config: StateFlow<MockServerConfig> = mutableConfig.asStateFlow()

    fun configure(transform: (MockServerConfig) -> MockServerConfig) = mutableConfig.update(transform)

    suspend fun <T> call(block: suspend () -> T): T {
        val config = config.value
        delay(config.latency)
        if (config.isOffline) throw NetworkException(NetworkFailureKind.NO_CONNECTION)
        if (config.failureRate > 0f && random.nextFloat() < config.failureRate) {
            throw ServerException(status = 503, message = "Mock server failure")
        }
        return block()
    }
}
