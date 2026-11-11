package io.github.wladyslawpopov.kpage.demo.data.space

import io.github.wladyslawpopov.kpage.demo.data.network.createHttpClient
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NasaImagesRemoteTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun remote(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): NasaImagesRemote {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return NasaImagesRemote(createHttpClient(engine, Json { ignoreUnknownKeys = true; explicitNulls = false }))
    }

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val html = headersOf(HttpHeaders.ContentType, "text/html")

    private fun request(text: String? = null, keywords: String? = null) =
        NasaSearchRequest(text = text, keywords = keywords, page = 1, pageSize = 20)

    @Test
    fun an_error_answer_carries_the_status_and_the_reason_of_nasa() = runTest {
        val remote = remote { respond("""{"reason":"Invalid filter"}""", HttpStatusCode.BadRequest, json) }

        val error = assertFailsWith<ServerException> { remote.search(request("moon")) }

        assertEquals(400, error.status)
        assertEquals("Invalid filter", error.message)
    }

    @Test
    fun an_error_page_that_is_not_json_has_no_message() = runTest {
        val remote = remote { respond("<html>Not Found</html>", HttpStatusCode.NotFound, html) }

        val error = assertFailsWith<ServerException> { remote.search(request("moon")) }

        assertEquals(404, error.status)
        assertNull(error.message)
    }

    @Test
    fun a_blank_reason_is_not_a_message() = runTest {
        val remote = remote { respond("""{"reason":"   "}""", HttpStatusCode.BadRequest, json) }

        assertNull(assertFailsWith<ServerException> { remote.search(request("moon")) }.message)
    }

    @Test
    fun an_answer_of_another_shape_is_an_invalid_response() = runTest {
        val remote = remote { respond("""{"unexpected":true}""", HttpStatusCode.OK, json) }

        val error = assertFailsWith<ServerException> { remote.search(request("moon")) }

        assertEquals(NasaImagesRemote.INVALID_RESPONSE, error.code)
        assertEquals(200, error.status)
    }

    @Test
    fun an_item_without_data_does_not_fail_the_page() = runTest {
        val remote = remote {
            respond(
                """{"collection":{"items":[{"links":[]},{"data":[{"nasa_id":"a"}]}],"metadata":{"total_hits":2}}}""",
                HttpStatusCode.OK,
                json,
            )
        }

        val page = remote.search(request("moon"))

        assertEquals(2, page.collection.metadata.totalHits)
        assertEquals(listOf(null, "a"), page.collection.items.map { it.nasaId })
    }

    @Test
    fun an_unknown_id_is_null() = runTest {
        val remote = remote { respond("""{"collection":{"items":[],"metadata":{"total_hits":0}}}""", HttpStatusCode.OK, json) }

        assertNull(remote.item("nope"))
        assertEquals("nope", requests.single().url.parameters["nasa_id"])
    }

    @Test
    fun a_known_id_is_returned() = runTest {
        val remote = remote {
            respond(
                """{"collection":{"items":[{"data":[{"nasa_id":"a","title":"A"}]}],"metadata":{"total_hits":1}}}""",
                HttpStatusCode.OK,
                json,
            )
        }

        assertNotNull(remote.item("a"))
    }

    @Test
    fun an_apostrophe_is_dropped_from_exact_filters_but_not_from_the_text() = runTest {
        val remote = remote { respond("""{"collection":{"items":[],"metadata":{"total_hits":0}}}""", HttpStatusCode.OK, json) }

        remote.search(request(text = "Hubble's view", keywords = "Hubble's"))

        val parameters = requests.single().url.parameters
        assertEquals("Hubble's view", parameters["q"])
        assertEquals("Hubbles", parameters["keywords"])
    }

    @Test
    fun a_blank_text_and_empty_filters_are_not_sent() = runTest {
        val remote = remote { respond("""{"collection":{"items":[],"metadata":{"total_hits":0}}}""", HttpStatusCode.OK, json) }

        remote.search(request(text = "   "))

        val parameters = requests.single().url.parameters
        assertNull(parameters["q"])
        assertNull(parameters["keywords"])
        assertEquals("image", parameters["media_type"])
        assertEquals("1", parameters["page"])
        assertEquals("20", parameters["page_size"])
    }
}
