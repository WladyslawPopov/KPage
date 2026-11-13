package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat.listing

import io.github.wladyslawpopov.kpage.demo.domain.cache.CacheRepository
import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.Route
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface ChatsComponent : ScreenComponent, StateOwner<ChatsState, ChatsIntent>, PageOwner

/** The list of chats: always the root of its tab. */
internal class DefaultChatsComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: ChatRepository,
    cache: CacheRepository,
) : PageComponent<ChatsState, ChatsIntent>(context, ChatsState()), ChatsComponent {

    private val engine = ChatsEngine(
        tasks = this,
        repository = repository,
        cache = cache,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
        // A new chat opens at once.
        onCreated = { navigator.open(Route.ChatRoom(it.id)) },
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: ChatsIntent) {
        // Navigation belongs to the component; a tap in the selection mode only chooses.
        if (intent is ChatsIntent.Click && !currentState.isSelecting) {
            navigator.open(Route.ChatRoom(intent.id))
        } else {
            engine.onIntent(intent)
        }
    }

    override suspend fun onRefresh() = engine.onRefresh()

    override fun onEmptyAction() = engine.onIntent(ChatsIntent.StartCreate)
}
