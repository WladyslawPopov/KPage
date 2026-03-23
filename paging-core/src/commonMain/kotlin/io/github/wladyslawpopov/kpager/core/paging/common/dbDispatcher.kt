package io.github.wladyslawpopov.kpager.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher

internal expect fun dbDispatcher() : CoroutineDispatcher
