package io.github.wladyslawpopov.kpage.demo.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigationSnapshotCodecTest {
    @Test
    fun snapshotSurvivesEncodeDecode() {
        val snapshot = NavigationSnapshot(
            selectedTab = AppTab.LAB,
            paths = mapOf(AppTab.SPACE to listOf(Route.Settings, Route.Chat), AppTab.LAB to emptyList()),
        )

        assertEquals(snapshot, NavigationSnapshotCodec.decode(NavigationSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun invalidTextDecodesToNull() {
        assertNull(NavigationSnapshotCodec.decode(""))
        assertNull(NavigationSnapshotCodec.decode("not json"))
    }
}
