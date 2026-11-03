package io.github.wladyslawpopov.kpage.demo.presentation.paginator

sealed interface PaginatorIntent<out Q : Any> {
    data class SetQuery<Q : Any>(val query: Q) : PaginatorIntent<Q>
    data object Refresh : PaginatorIntent<Nothing>
}
