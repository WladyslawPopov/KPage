package io.github.wladyslawpopov.kpage.demo.data.favorites

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.wladyslawpopov.kpage.cache.PagingDatabase
import io.github.wladyslawpopov.kpage.core.paging.createPagingDriver
import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import io.github.wladyslawpopov.kpage.demo.data.db.DemoDatabase
import io.github.wladyslawpopov.kpage.demo.data.network.createHttpClient
import io.github.wladyslawpopov.kpage.demo.data.space.NasaImagesRemote
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * `sync()` is private: the tests drive it the way the app does, through the paginator, which asks the
 * repository for the first page as soon as it is created.
 */
class FavoritesRepositoryImplTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cacheDirectory = Files.createTempDirectory("kpage-test").toFile()
    private val requested = java.util.Collections.synchronizedList(mutableListOf<String>())

    // Ids that NASA knows get an item, "gone" gets an empty answer, "broken" a server error.
    private val engine = MockEngine { request ->
        val id = request.url.parameters["nasa_id"].orEmpty()
        requested += id
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        when (id) {
            "broken" -> respond("""{"reason":"boom"}""", HttpStatusCode.InternalServerError, json)
            "gone" -> respond(collection(), HttpStatusCode.OK, json)
            else -> respond(collection("""{"data":[{"nasa_id":"$id","title":"Title $id"}],"links":[]}"""), HttpStatusCode.OK, json)
        }
    }

    private val demoDb = DemoDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), DemoDatabase.Schema))
    private val store = FavoritesStore(demoDb)
    private val repository = FavoritesRepositoryImpl(
        remote = NasaImagesRemote(createHttpClient(engine, Json { ignoreUnknownKeys = true; explicitNulls = false })),
        store = store,
        db = PagingDatabase(createPagingDriver(cacheDirectory)),
    )

    @AfterTest
    fun tearDown() {
        scope.cancel()
        cacheDirectory.deleteRecursively()
    }

    private fun collection(vararg items: String) =
        """{"collection":{"items":[${items.joinToString(",")}],"metadata":{"total_hits":${items.size}}}}"""

    private fun <T> blocking(block: suspend () -> T): T = runBlocking { withTimeout(10.seconds) { block() } }

    private suspend fun Paginator<SpaceImage>.settled(): PagingState<SpaceImage> =
        state.first { it.loadState is LoadState.END || it.loadState is LoadState.ERROR }

    private fun PagingState<SpaceImage>.ids() = items.mapNotNull { it?.id }.toSet()

    @Test
    fun missing_images_are_fetched_and_shown() = blocking {
        store.add("a")
        store.add("b")

        val state = repository.favorites(scope).settled()

        assertEquals(setOf("a", "b"), state.ids())
        assertEquals(setOf("a", "b"), requested.toSet())
    }

    @Test
    fun a_refresh_asks_nasa_only_for_the_new_ids() = blocking {
        store.add("a")
        val paginator = repository.favorites(scope)
        paginator.settled()

        repository.setFavorite("b", isFavorite = true)
        paginator.refresh()
        paginator.state.first { it.ids() == setOf("a", "b") && it.loadState is LoadState.END }

        assertEquals(listOf("a", "b"), requested.toList())
    }

    @Test
    fun an_id_that_nasa_does_not_know_is_removed() = blocking {
        store.add("a")
        store.add("gone")

        val state = repository.favorites(scope).settled()

        assertEquals(setOf("a"), state.ids())
        assertEquals(listOf("a"), store.allIds())
    }

    @Test
    fun a_failed_request_keeps_the_id_and_reports_the_error() = blocking {
        store.add("a")
        store.add("broken")

        val state = repository.favorites(scope).settled()

        assertEquals(true, state.loadState is LoadState.ERROR)
        assertEquals(setOf("a", "broken"), store.allIds().toSet())
    }

    @Test
    fun the_images_that_arrived_survive_a_failure_of_another_one() = blocking {
        store.add("a")
        store.add("broken")
        repository.favorites(scope).settled()

        // The failed id is gone from the favorites; the next list needs no request for "a".
        repository.removeFavorites(listOf("broken"))
        requested.clear()
        val state = repository.favorites(scope).settled()

        assertEquals(setOf("a"), state.ids())
        assertEquals(emptyList(), requested.toList())
    }
}
