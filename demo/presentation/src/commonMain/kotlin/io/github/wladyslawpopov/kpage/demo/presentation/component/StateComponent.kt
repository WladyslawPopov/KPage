package io.github.wladyslawpopov.kpage.demo.presentation.component

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base of every component: coroutines bound to its lifecycle and one way to handle failures.
 * The scopes are cancelled when [context] is destroyed; subclasses release only their own resources
 * in [onDispose].
 */
abstract class StateComponent(
    protected val context: ComponentContext,
    protected val dispatchers: ComponentDispatchers = ComponentDispatchers(),
    private val errorMapper: ErrorMapper = ErrorMapper.Default,
    private val errorReporter: ErrorReporter = ErrorReporter.Print,
) : ComponentTasks {
    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, error -> handleError(error) }

    protected val scope = CoroutineScope(job + dispatchers.main + exceptionHandler)
    protected val workScope = CoroutineScope(job + dispatchers.work + exceptionHandler)
    protected val ioScope = CoroutineScope(job + dispatchers.io + exceptionHandler)

    final override val isActive: Boolean get() = job.isActive
    final override val workDispatcher get() = dispatchers.work
    final override val ioDispatcher get() = dispatchers.io

    private var disposed = false

    init {
        // A context destroyed before construction delivers no ON_DESTROY to a new observer. Only the
        // scopes are cancelled: subclass code must not run while the subclass is not constructed yet.
        if (context.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            disposed = true
            job.cancel()
        } else {
            context.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) dispose()
            })
        }
    }

    final override fun launchMain(
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit
    ) =
        launchHandled(scope, onError, block)

    final override fun launchWork(
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit
    ) =
        launchHandled(workScope, onError, block)

    final override fun launchIo(
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit
    ) =
        launchHandled(ioScope, onError, block)

    final override fun launchChild(
        parent: CoroutineScope,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit,
    ) = launchHandled(parent, onError, block)

    private fun launchHandled(
        target: CoroutineScope,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = target.launch {
        try {
            coroutineScope(block)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            withContext(dispatchers.main) {
                if (onError == null) applyError(error) else onError(error)
            }
        }
    }

    /** The common handling of a failure, on Main. Safe to call from any thread. */
    protected fun handleError(error: Throwable) {
        if (error is CancellationException) throw error
        if (!isActive) return
        if (dispatchers.main.isDispatchNeeded(EmptyCoroutineContext)) {
            scope.launch { applyError(error) }
        } else {
            applyError(error)
        }
    }

    private fun applyError(error: Throwable) {
        if (!isActive) return
        val decision = errorMapper.map(error)
        if (decision.report) report(error)
        decision.error?.let(::onError)
    }

    /** Shows the error; pages put it into their state. */
    protected open fun onError(error: ScreenError) = Unit

    /** Releases resources of the subclass; called once, after the scopes are cancelled. */
    protected open fun onDispose() = Unit

    private fun report(error: Throwable) {
        try {
            errorReporter.report(error)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) { // A failing reporter must not crash the component.
        }
    }

    private fun dispose() {
        if (disposed) return
        disposed = true
        job.cancel()
        onDispose()
    }
}
