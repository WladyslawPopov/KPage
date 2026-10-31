package io.github.wladyslawpopov.kpage.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

// Blocking database work belongs to the IO pool, not to the CPU one; one operation at a time.
private val nativeDbDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

actual fun dbDispatcher(): CoroutineDispatcher = nativeDbDispatcher
