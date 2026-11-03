package io.github.wladyslawpopov.kpage.demo.presentation.component

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Dispatchers of a component; tests pass controlled ones. */
class ComponentDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main.immediate,
    val work: CoroutineDispatcher = Dispatchers.Default,
    val io: CoroutineDispatcher = Dispatchers.IO,
)
