package io.github.wladyslawpopov.kpage.demo.presentation.component

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * What a component lends to its engines: they launch work, the component owns the scopes and their
 * lifetime. A local `onError` replaces the component handling for that launch; cancellation always
 * propagates.
 */
interface ComponentTasks {
    val isActive: Boolean
    val workDispatcher: CoroutineDispatcher
    val ioDispatcher: CoroutineDispatcher

    fun launchMain(
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job

    fun launchWork(
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job

    fun launchIo(
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job

    /** Launches in a running task's scope: cancellation stays local, sibling failures are isolated. */
    fun launchChild(
        parent: CoroutineScope,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job
}
