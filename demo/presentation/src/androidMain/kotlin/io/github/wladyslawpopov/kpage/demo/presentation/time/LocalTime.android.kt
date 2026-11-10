package io.github.wladyslawpopov.kpage.demo.presentation.time

import java.util.TimeZone

internal actual fun utcOffsetMillis(epochMillis: Long): Long =
    TimeZone.getDefault().getOffset(epochMillis).toLong()
