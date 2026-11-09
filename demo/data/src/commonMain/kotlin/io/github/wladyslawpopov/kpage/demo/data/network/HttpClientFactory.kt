package io.github.wladyslawpopov.kpage.demo.data.network

import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkException
import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkFailureKind
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

/** The one HTTP client of the app: every failure leaves it as an error of the domain. */
internal fun createHttpClient(engine: HttpClientEngine, json: Json): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(json) }
    install(Logging) { level = LogLevel.INFO }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 20_000
        socketTimeoutMillis = 20_000
    }
    // The network is often not up yet when the app is back from sleep, and is up when the first try has failed.
    // So a failure of the network (no connection, a timeout) is repeated once after a pause. The plugin sees the
    // raw failure, before the validator below turns it into a domain error. A server answer, even an error one,
    // is not repeated; only reads are sent, so a repeat is safe.
    install(HttpRequestRetry) {
        maxRetries = 1
        retryOnExceptionIf { _, cause -> cause.toDomainError() is NetworkException }
        retryIf { _, _ -> false }
        constantDelay(millis = 1_000, randomizationMs = 0)
    }
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            val mapped = cause.toDomainError()
            if (mapped !== cause) throw mapped
        }
    }
}

/** Timeouts are IOExceptions too, so they are checked first. */
internal fun Throwable.toDomainError(): Throwable = when (this) {
    is CancellationException -> this
    is ResponseException -> ServerException(status = response.status.value, cause = this)
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
        NetworkException(NetworkFailureKind.TIMEOUT, this)
    is IOException -> NetworkException(NetworkFailureKind.NO_CONNECTION, this)
    else -> this
}
