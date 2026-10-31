package io.github.wladyslawpopov.kpage.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

// JDBC drivers do not serialize transactions of different threads (the in-memory one shares a single
// connection), and the file driver opens a connection per thread. One dedicated thread solves both.
private val jvmDbDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { task ->
    Thread(task, "kpage-db").apply { isDaemon = true }
}.asCoroutineDispatcher()

actual fun dbDispatcher(): CoroutineDispatcher = jvmDbDispatcher
