package io.github.wladyslawpopov.kpage.demo.domain.settings

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceLayout

/** The user's choices, kept on the device between launches. */
interface SettingsRepository {
    suspend fun spaceLayout(): SpaceLayout

    suspend fun setSpaceLayout(layout: SpaceLayout)
}
