package io.github.wladyslawpopov.kpage.demo.presentation.testing

import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentTasks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** [ComponentTasks] that run everything in one test scope, so the test controls the time. */
internal class TestTasks(
    private val scope: CoroutineScope,
    override val workDispatcher: CoroutineDispatcher,
    override val ioDispatcher: CoroutineDispatcher,
) : ComponentTasks {
    override val isActive: Boolean get() = scope.isActive

    override fun launchMain(onError: (suspend (Throwable) -> Unit)?, block: suspend CoroutineScope.() -> Unit) =
        launchIn(scope, onError, block)

    override fun launchWork(onError: (suspend (Throwable) -> Unit)?, block: suspend CoroutineScope.() -> Unit) =
        launchIn(scope, onError, block)

    override fun launchIo(onError: (suspend (Throwable) -> Unit)?, block: suspend CoroutineScope.() -> Unit) =
        launchIn(scope, onError, block)

    override fun launchChild(
        parent: CoroutineScope,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit,
    ) = launchIn(parent, onError, block)

    private fun launchIn(
        target: CoroutineScope,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = target.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (onError != null) onError(e) else throw e
        }
    }
}
