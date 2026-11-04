package io.github.wladyslawpopov.kpage.demo.presentation.screens.chat

import io.github.wladyslawpopov.kpage.demo.domain.chat.ChatRepository
import io.github.wladyslawpopov.kpage.demo.presentation.component.ComponentContext
import io.github.wladyslawpopov.kpage.demo.presentation.component.StateOwner
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.AppNavigator
import io.github.wladyslawpopov.kpage.demo.presentation.navigation.ScreenComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageComponent
import io.github.wladyslawpopov.kpage.demo.presentation.page.PageOwner

interface ChatComponent : ScreenComponent, StateOwner<ChatState, ChatIntent>, PageOwner

internal class DefaultChatComponent(
    context: ComponentContext,
    private val navigator: AppNavigator,
    repository: ChatRepository,
) : PageComponent<ChatState, ChatIntent>(context, ChatState()), ChatComponent {

    private val engine = ChatEngine(
        tasks = this,
        repository = repository,
        getState = { currentState },
        updateState = ::updateState,
        onPageIntent = ::onPageIntent,
    )

    init {
        engine.start()
    }

    override fun onIntent(intent: ChatIntent) = engine.onIntent(intent)
}
