package io.github.wladyslawpopov.kpager.core.paging

import kotlin.time.Clock

fun nowAsEpochSeconds(): Long {
    return Clock.System.now().epochSeconds
}
