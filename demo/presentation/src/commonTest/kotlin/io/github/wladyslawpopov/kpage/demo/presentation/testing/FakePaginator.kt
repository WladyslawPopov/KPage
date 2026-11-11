package io.github.wladyslawpopov.kpage.demo.presentation.testing

import io.github.wladyslawpopov.kpage.core.paging.data.paginator.Paginator
import io.github.wladyslawpopov.kpage.core.paging.data.state.LoadState
import io.github.wladyslawpopov.kpage.core.paging.data.state.PagingState
import kotlinx.coroutines.flow.MutableStateFlow

/** A [Paginator] without data: the test sets its load state and counts what the code under test asks of it. */
internal class FakePaginator<T : Any>(val query: String = "") : Paginator<T> {
    override val state = MutableStateFlow(PagingState.initial<T>())
    var refreshCount = 0
    var closed = false
    val updatedIds = mutableListOf<String>()

    override fun refresh() {
        refreshCount++
    }

    override suspend fun getItemById(id: String): T? = null
    override suspend fun updateItem(id: String, transform: (T) -> T): T? {
        updatedIds += id
        return null
    }

    override fun close() {
        closed = true
    }

    override fun onVisibleRangeChanged(visible: IntRange) = Unit

    fun fail(error: Throwable) {
        state.value = PagingState.initial<T>().copy(loadState = LoadState.ERROR(error), isRestored = true)
    }

    /** The list is loaded to its end with no items. */
    fun recover() {
        state.value = PagingState.initial<T>().copy(loadState = LoadState.END, isRestored = true)
    }
}
