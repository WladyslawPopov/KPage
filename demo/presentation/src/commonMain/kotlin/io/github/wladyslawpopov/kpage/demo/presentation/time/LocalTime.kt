package io.github.wladyslawpopov.kpage.demo.presentation.time

import kotlin.time.Clock

/** The offset of the user's time zone from UTC at [epochMillis]; only the platform knows the zone. */
internal expect fun utcOffsetMillis(epochMillis: Long): Long

/** The number of the day [epochMillis] falls on in the user's time zone; equal for moments of one day. */
internal fun localDay(epochMillis: Long): Long =
    (epochMillis + utcOffsetMillis(epochMillis)).floorDiv(MILLIS_IN_DAY)

internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

private const val MILLIS_IN_DAY = 86_400_000L
