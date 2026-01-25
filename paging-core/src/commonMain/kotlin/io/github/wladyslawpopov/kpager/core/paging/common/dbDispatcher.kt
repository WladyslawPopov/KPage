package io.github.wladyslawpopov.kpager.core.paging.common

import kotlinx.coroutines.CoroutineDispatcher

expect fun dbDispatcher() : CoroutineDispatcher
