package io.github.wladyslawpopov.kpage.demo.domain.error

enum class NetworkFailureKind { NO_CONNECTION, TIMEOUT }

/** The server could not be reached. */
class NetworkException(
    val kind: NetworkFailureKind,
    cause: Throwable? = null,
) : Exception("Network failure: $kind", cause)

/** The server answered with an error. [message] is the server's text for the user, if it gave one. */
class ServerException(
    val status: Int,
    val code: String? = null,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
