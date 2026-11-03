package io.github.wladyslawpopov.kpage.demo.presentation.component

import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkException
import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkFailureKind
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import kotlinx.coroutines.CancellationException

/** How a failure is handled: what the user sees (null = nothing) and whether it is reported. */
data class ErrorDecision(
    val error: ScreenError? = null,
    val report: Boolean = false,
)

/** Turns failures into decisions: errors of the domain get their own texts, the rest is "oops". */
fun interface ErrorMapper {
    fun map(error: Throwable): ErrorDecision

    companion object {
        val Default = ErrorMapper { error ->
            when (error) {
                is CancellationException -> throw error
                is NetworkException -> when (error.kind) {
                    NetworkFailureKind.NO_CONNECTION -> ErrorDecision(
                        error = ScreenError(ErrorKind.NO_INTERNET, AppStringKey.Core.NO_INTERNET_TITLE),
                    )
                    NetworkFailureKind.TIMEOUT -> ErrorDecision(
                        error = ScreenError(ErrorKind.NETWORK, AppStringKey.Core.NETWORK_ERROR),
                    )
                }
                is ServerException -> ErrorDecision(
                    error = ScreenError(
                        kind = ErrorKind.SERVER,
                        title = AppStringKey.Core.SERVER_ERROR,
                        message = error.message,
                        code = error.code ?: error.status.toString(),
                    ),
                    report = true,
                )
                else -> ErrorDecision(error = ScreenError(), report = true)
            }
        }
    }
}

/** Where unexpected failures go (logs, crash reporting). */
fun interface ErrorReporter {
    fun report(error: Throwable)

    companion object {
        val Print = ErrorReporter { println(it.stackTraceToString()) }
    }
}
