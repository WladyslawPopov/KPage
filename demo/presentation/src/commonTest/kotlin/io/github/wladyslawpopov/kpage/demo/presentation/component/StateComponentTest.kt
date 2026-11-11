package io.github.wladyslawpopov.kpage.demo.presentation.component

import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkException
import io.github.wladyslawpopov.kpage.demo.domain.error.NetworkFailureKind
import io.github.wladyslawpopov.kpage.demo.domain.error.ServerException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StateComponentTest {

    private class TestComponent(
        context: ComponentContext,
        dispatchers: ComponentDispatchers,
        reporter: ErrorReporter,
    ) : StateComponent(context, dispatchers, ErrorMapper.Default, reporter) {
        val shown = mutableListOf<ScreenError>()
        var disposed = 0

        override fun onError(error: ScreenError) {
            shown += error
        }

        override fun onDispose() {
            disposed++
        }
    }

    private class Harness(scope: TestScope, val context: ComponentContext = ComponentContext()) {
        val reported = mutableListOf<Throwable>()
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val component = TestComponent(
            context,
            ComponentDispatchers(main = dispatcher, work = dispatcher, io = dispatcher),
            { reported += it },
        )
    }

    @Test
    fun destroying_the_context_cancels_the_running_tasks_and_disposes_once() = runTest {
        val harness = Harness(this)
        var main = false
        var work = false
        harness.component.launchMain { try { awaitCancellation() } finally { main = true } }
        harness.component.launchWork { try { awaitCancellation() } finally { work = true } }
        runCurrent()
        assertTrue(harness.component.isActive)

        harness.context.destroy()
        harness.context.destroy()
        runCurrent()

        assertTrue(main && work)
        assertFalse(harness.component.isActive)
        assertEquals(1, harness.component.disposed)
    }

    @Test
    fun hiding_and_showing_do_not_stop_the_tasks() = runTest {
        val harness = Harness(this)
        var finished = false
        harness.component.launchMain { try { awaitCancellation() } finally { finished = true } }

        harness.context.show()
        harness.context.hide()
        runCurrent()

        assertTrue(harness.component.isActive)
        assertFalse(finished)
    }

    @Test
    fun a_component_made_after_the_destroy_is_dead_and_runs_nothing() = runTest {
        val context = ComponentContext().also { it.destroy() }
        val harness = Harness(this, context)
        var ran = false

        harness.component.launchMain { ran = true }
        runCurrent()

        assertFalse(harness.component.isActive)
        assertFalse(ran)
        // The subclass was not constructed when the scopes were cancelled, so it releases nothing.
        assertEquals(0, harness.component.disposed)
    }

    @Test
    fun an_unexpected_failure_is_shown_as_oops_and_reported() = runTest {
        val harness = Harness(this)
        val error = IllegalStateException("boom")

        harness.component.launchMain { throw error }
        runCurrent()

        assertEquals(listOf(ScreenError()), harness.component.shown)
        // Stack trace recovery of the coroutines may hand over a copy of the exception.
        assertEquals(listOf(error.message), harness.reported.map { it.message })
        assertTrue(harness.reported.single() is IllegalStateException)
    }

    @Test
    fun a_network_failure_is_shown_with_its_own_text_and_not_reported() = runTest {
        val harness = Harness(this)

        harness.component.launchIo { throw NetworkException(NetworkFailureKind.NO_CONNECTION) }
        runCurrent()

        assertEquals(listOf(ErrorKind.NO_INTERNET), harness.component.shown.map { it.kind })
        assertTrue(harness.reported.isEmpty())
    }

    @Test
    fun a_local_handler_replaces_the_handling_of_the_component() = runTest {
        val harness = Harness(this)
        val handled = mutableListOf<Throwable>()
        val error = IllegalStateException("boom")

        harness.component.launchMain(onError = { handled += it }) { throw error }
        runCurrent()

        assertEquals(listOf(error.message), handled.map { it.message })
        assertTrue(handled.single() is IllegalStateException)
        assertTrue(harness.component.shown.isEmpty())
        assertTrue(harness.reported.isEmpty())
    }

    @Test
    fun a_failed_task_does_not_stop_its_siblings() = runTest {
        val harness = Harness(this)
        var sibling = false

        harness.component.launchMain { throw IllegalStateException("boom") }
        harness.component.launchMain { sibling = true }
        runCurrent()

        assertTrue(sibling)
        assertTrue(harness.component.isActive)
    }

    @Test
    fun a_failed_child_does_not_stop_its_parent_task() = runTest {
        val harness = Harness(this)
        var parentFinished = false

        harness.component.launchMain {
            harness.component.launchChild(this) { throw IllegalStateException("boom") }
            parentFinished = true
        }
        runCurrent()

        assertTrue(parentFinished)
        assertEquals(1, harness.component.shown.size)
    }

    // ErrorMapper.Default

    private fun map(error: Throwable) = ErrorMapper.Default.map(error)

    @Test
    fun a_timeout_is_a_network_error_without_a_report() {
        val decision = map(NetworkException(NetworkFailureKind.TIMEOUT))

        assertEquals(ErrorKind.NETWORK, decision.error?.kind)
        assertFalse(decision.report)
    }

    @Test
    fun a_server_error_carries_the_message_and_the_status_as_the_code_and_is_reported() {
        val decision = map(ServerException(status = 500, message = "boom"))

        assertEquals(ErrorKind.SERVER, decision.error?.kind)
        assertEquals("boom", decision.error?.message)
        assertEquals("500", decision.error?.code)
        assertTrue(decision.report)
    }

    @Test
    fun the_code_of_the_server_wins_over_the_status() {
        assertEquals("INVALID_RESPONSE", map(ServerException(status = 200, code = "INVALID_RESPONSE")).error?.code)
    }

    @Test
    fun anything_else_is_unknown_and_reported() {
        val decision = map(IllegalArgumentException())

        assertEquals(ErrorKind.UNKNOWN, decision.error?.kind)
        assertNull(decision.error?.message)
        assertTrue(decision.report)
    }

    @Test
    fun a_cancellation_is_never_mapped() {
        assertFailsWith<CancellationException> { map(CancellationException("cancelled")) }
    }
}
