package io.github.wladyslawpopov.kpage.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun dbDispatcher(): CoroutineDispatcher = Dispatchers.IO
