package io.github.wladyslawpopov.kpager.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun dbDispatcher(): CoroutineDispatcher = Dispatchers.IO
