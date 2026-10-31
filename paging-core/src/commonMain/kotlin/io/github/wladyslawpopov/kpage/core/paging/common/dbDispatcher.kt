package io.github.wladyslawpopov.kpage.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher

internal expect fun dbDispatcher() : CoroutineDispatcher
