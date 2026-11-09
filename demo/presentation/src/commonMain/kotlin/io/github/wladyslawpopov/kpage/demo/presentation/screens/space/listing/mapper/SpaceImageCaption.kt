package io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImage

/** The line under the title of a card: "date · center". */
fun SpaceImage.caption(): String = listOfNotNull(date, center).joinToString(" · ")
