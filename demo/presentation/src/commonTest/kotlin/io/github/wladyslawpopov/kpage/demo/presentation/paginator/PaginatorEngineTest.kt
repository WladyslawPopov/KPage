package io.github.wladyslawpopov.kpage.demo.presentation.paginator

import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.demo.presentation.testing.FakePaginator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaginatorEngineTest {

    private class Harness(
        val scope: TestScope,
        val refreshOn: MutableSharedFlow<Unit> = MutableSharedFlow(),
    ) {
        var state = PaginatorUiState<String, String, Nothing>(query = "")
        val errors = mutableListOf<Throwable>()
        var queryChanges = 0
        val created = mutableListOf<FakePaginator<String>>()
        val queries = MutableStateFlow("a")

        val engine = PaginatorEngine<String, String, Nothing>(
            getState = { state },
            updateState = { state = it },
            onQueryChanged = { queryChanges++ },
            onError = { errors += it },
            refreshOn = refreshOn,
        )

        val current get() = created.last()

        fun start() {
            scope.backgroundScope.launch {
                engine.collectPaging(queries) { query, _: CoroutineScope ->
                    FakePaginator<String>(query).also { created += it }
                }
            }
            scope.runCurrent()
        }
    }

    @Test
    fun a_new_query_closes_the_previous_paginator_and_creates_another() = runTest {
        val harness = Harness(this).apply { start() }
        val first = harness.current

        harness.queries.value = "b"
        runCurrent()

        assertTrue(first.closed)
        assertEquals(2, harness.created.size)
        assertEquals("b", harness.current.query)
        assertFalse(harness.current.closed)
        assertSame(harness.current, harness.state.events)
    }

    @Test
    fun the_same_query_keeps_the_paginator() = runTest {
        val harness = Harness(this).apply { start() }

        harness.queries.value = "a"
        runCurrent()

        assertEquals(1, harness.created.size)
        assertFalse(harness.current.closed)
    }

    @Test
    fun the_state_follows_the_paginator() = runTest {
        val harness = Harness(this).apply { start() }

        harness.current.recover()
        runCurrent()

        assertEquals(LoadState.END, harness.state.loadState)
        assertTrue(harness.state.isEmptyResult)
    }

    @Test
    fun a_load_error_is_reported_once_until_it_is_cleared() = runTest {
        val harness = Harness(this).apply { start() }
        val error = IllegalStateException("offline")

        harness.current.fail(error)
        runCurrent()
        // Another snapshot with the same error object must not report it again.
        harness.current.fail(error)
        runCurrent()
        assertEquals(listOf<Throwable>(error), harness.errors)

        harness.current.recover()
        runCurrent()
        val next = IllegalStateException("offline again")
        harness.current.fail(next)
        runCurrent()
        assertEquals(listOf<Throwable>(error, next), harness.errors)
    }

    @Test
    fun a_refresh_event_renews_the_current_paginator() = runTest {
        val harness = Harness(this).apply { start() }

        harness.refreshOn.emit(Unit)
        runCurrent()
        assertEquals(1, harness.current.refreshCount)

        harness.queries.value = "b"
        runCurrent()
        harness.refreshOn.emit(Unit)
        runCurrent()
        assertEquals(1, harness.current.refreshCount)
        assertEquals(1, harness.created.first().refreshCount)
    }

    @Test
    fun the_refresh_intent_renews_the_paginator() = runTest {
        val harness = Harness(this).apply { start() }

        harness.engine.onIntent(PaginatorIntent.Refresh)

        assertEquals(1, harness.current.refreshCount)
    }

    @Test
    fun a_new_query_intent_is_announced_once_and_the_same_one_is_ignored() = runTest {
        val harness = Harness(this)
        harness.state = harness.state.copy(query = "a")

        harness.engine.onIntent(PaginatorIntent.SetQuery("a"))
        assertEquals(0, harness.queryChanges)

        harness.engine.onIntent(PaginatorIntent.SetQuery("b"))
        assertEquals(1, harness.queryChanges)
        assertEquals("b", harness.state.query)
    }

    @Test
    fun cancelling_the_collector_leaves_no_running_work() = runTest {
        val harness = Harness(this)
        val job = backgroundScope.launch {
            harness.engine.collectPaging(harness.queries) { query, _ ->
                FakePaginator<String>(query).also { harness.created += it }
            }
        }
        runCurrent()
        assertTrue(job.isActive)

        job.cancel()
        runCurrent()
        harness.queries.value = "b"
        runCurrent()

        assertEquals(1, harness.created.size)
    }
}
