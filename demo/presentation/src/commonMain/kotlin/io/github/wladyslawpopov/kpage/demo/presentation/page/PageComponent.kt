package io.github.wladyslawpopov.kpage.demo.presentation.page

import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentDispatchers
import io.github.wladyslawpopov.kpage.demo.presentation.component.ErrorMapper
import io.github.wladyslawpopov.kpage.demo.presentation.component.ErrorReporter
import io.github.wladyslawpopov.kpage.demo.presentation.component.ScreenError
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateComponent
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * A screen with the common page behavior: errors, loading, toasts and the empty state live in
 * [PageState] of its own state. Screens react to [PageIntent.Refresh] and [PageIntent.Reset] in
 * [onRefresh] and [onReset].
 */
abstract class PageComponent<S : PageScreenState<S>, I : Any>(
    context: ComponentContext,
    initialState: S,
    dispatchers: ComponentDispatchers = ComponentDispatchers(),
    errorMapper: ErrorMapper = ErrorMapper.Default,
    errorReporter: ErrorReporter = ErrorReporter.Print,
) : StateComponent(context, dispatchers, errorMapper, errorReporter), StateOwner<S, I>, PageOwner {

    private val mutableState = MutableStateFlow(initialState)
    final override val state: StateFlow<S> = mutableState.asStateFlow()
    protected val currentState: S get() = mutableState.value

    private var toastJob: Job? = null
    private var nextToastId = 0L

    protected open fun updateState(transform: (S) -> S) = mutableState.update(transform)

    /** Runs on Main and is ignored after the component is destroyed. */
    final override fun onPageIntent(intent: PageIntent) {
        if (intent is PageIntent.OnError && intent.error is CancellationException) throw intent.error
        if (!isActive) return
        if (dispatchers.main.isDispatchNeeded(EmptyCoroutineContext)) {
            launchMain { applyPageIntent(intent) }
        } else {
            applyPageIntent(intent)
        }
    }

    protected open fun onRefresh() = Unit

    protected open fun onReset() = Unit

    final override fun onError(error: ScreenError) = updatePage { it.copy(error = error) }

    private fun applyPageIntent(intent: PageIntent) {
        when (intent) {
            is PageIntent.SetLoading -> updatePage { it.copy(isLoading = intent.isLoading) }
            is PageIntent.ShowToast -> showToast(PageToast(nextToastId++, intent.text, intent.type))
            PageIntent.DismissToast -> hideToast()
            is PageIntent.OnError -> handleError(intent.error)
            PageIntent.ClearError -> updatePage { it.copy(error = null) }
            PageIntent.Refresh -> {
                updatePage { it.copy(error = null) }
                onRefresh()
            }

            PageIntent.Reset -> {
                updatePage { it.copy(error = null) }
                onReset()
            }
        }
    }

    private fun showToast(toast: PageToast) {
        toastJob?.cancel()
        updatePage { it.copy(toast = toast) }
        toastJob = launchMain {
            delay(TOAST_DURATION)
            hideToast()
        }
    }

    private fun hideToast() {
        toastJob?.cancel()
        toastJob = null
        updatePage { it.copy(toast = null) }
    }

    private fun updatePage(transform: (PageState) -> PageState) =
        updateState { it.withPage(transform(it.page)) }

    private companion object {
        val TOAST_DURATION = 2.seconds
    }
}
