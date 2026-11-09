package io.github.wladyslawpopov.kpage.demo.data.space

import io.github.wladyslawpopov.kpage.demo.data.network.toDomainError
import io.github.wladyslawpopov.kpage.demo.data.space.dto.NasaErrorDto
import io.github.wladyslawpopov.kpage.demo.data.space.dto.NasaItemDto
import io.github.wladyslawpopov.kpage.demo.data.space.dto.NasaSearchDto
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * NASA Image Library, images only. A failed call leaves as [ServerException]: with the status and the
 * server's `reason` as the message, or with [INVALID_RESPONSE] when the body is not what we expect.
 */
internal class NasaImagesRemote(private val client: HttpClient) {

    suspend fun search(request: NasaSearchRequest): NasaSearchDto = get {
        parameter("q", request.text?.trim()?.ifEmpty { null })
        parameter("center", request.center)
        parameter("year_start", request.yearStart)
        parameter("year_end", request.yearEnd)
        parameter("keywords", request.keywords.forFilter())
        parameter("photographer", request.photographer.forFilter())
        parameter("secondary_creator", request.secondaryCreator.forFilter())
        parameter("location", request.location.forFilter())
        parameter("album", request.album.forFilter())
        parameter("page", request.page)
        parameter("page_size", request.pageSize)
    }

    /** The item by its id, or null if there is none. */
    suspend fun item(nasaId: String): NasaItemDto? =
        get { parameter("nasa_id", nasaId) }.collection.items.firstOrNull()

    /**
     * NASA answers 400 to an exact filter with an apostrophe in it, while the same value without it finds
     * the same images (the apostrophe is not indexed). The text search `q` has no such problem.
     */
    private fun String?.forFilter(): String? = this?.replace("'", "")

    private suspend fun get(block: HttpRequestBuilder.() -> Unit): NasaSearchDto {
        val response = client.get(SEARCH_URL) {
            expectSuccess = false // The error body is NASA's format: read it here, not in the shared client.
            parameter("media_type", "image")
            block()
        }
        val status = response.status
        if (!status.isSuccess()) throw ServerException(status.value, message = response.errorReason())
        return try {
            response.body()
        } catch (cause: Exception) {
            throw when (cause) {
                is SerializationException, is ContentConvertException, is NoTransformationFoundException ->
                    ServerException(status.value, code = INVALID_RESPONSE, cause = cause)
                else -> cause.toDomainError()
            }
        }
    }

    /** The server's text for the user; null when the body is not JSON (for example an nginx page). */
    private suspend fun HttpResponse.errorReason(): String? = try {
        body<NasaErrorDto>().reason?.takeIf { it.isNotBlank() }
    } catch (cause: CancellationException) {
        throw cause
    } catch (_: Exception) {
        null
    }

    companion object {
        const val INVALID_RESPONSE = "INVALID_RESPONSE"
        private const val SEARCH_URL = "https://images-api.nasa.gov/search"
    }
}
