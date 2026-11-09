package io.github.wladyslawpopov.kpage.demo.presentation.screens.space

import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceCenter
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageDetails
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceImageFile
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceQuery
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTag
import io.github.wladyslawpopov.kpage.demo.domain.space.SpaceTagKind
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.display.DetailsSection
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.mapper.toDisplay
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.details.SpaceDetailsIntent
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.display.SpaceChipTarget
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper.spaceChips
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.mapper.spaceFilters
import io.github.wladyslawpopov.kpage.demo.presentation.screens.space.listing.SpaceIntent
import io.github.wladyslawpopov.kpage.demo.uicore.text.AppStringKey
import io.github.wladyslawpopov.kpage.demo.uicore.text.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the screens draw is decided here, so it is checked here and not on two platforms by hand. */
class SpaceDisplayTest {

    private fun details(
        center: String? = "JSC",
        picture: SpaceImageFile? = null,
        description: String? = "About",
    ) = SpaceImageDetails(
        id = "id1",
        title = "Title",
        description = description,
        date = "2019-07-19",
        center = center,
        photographer = "NASA/Aubrey Gemignani",
        creator = null,
        location = null,
        keywords = listOf("Apollo 11"),
        albums = emptyList(),
        picture = picture,
        fullPicture = null,
        original = SpaceImageFile("u", 2000, 1400, 249_000),
        originalFormat = "JPG",
        isFavorite = false,
    )

    @Test
    fun details_rows_carry_the_action_a_tap_sends() {
        val sections = details().toDisplay().sections
        val rows = (sections.first() as DetailsSection.Fields).rows

        assertEquals(SpaceDetailsIntent.OpenYear(2019), rows[0].action)
        assertEquals(SpaceDetailsIntent.OpenCenter(SpaceCenter.JSC), rows[1].action)
        assertEquals(
            SpaceDetailsIntent.OpenTag(SpaceTag(SpaceTagKind.PHOTOGRAPHER, "NASA/Aubrey Gemignani")),
            rows[2].action,
        )
        // Cards without content are not listed: no albums here.
        assertEquals(
            listOf(AppStringKey.Space.DETAILS, AppStringKey.Space.KEYWORDS, AppStringKey.Space.IMAGE),
            sections.map { it.title },
        )
        // The size of the original is left to the platform to format.
        val file = (sections.last() as DetailsSection.Fields).rows
        assertTrue(file.any { it.value == UiText.FileSize(249_000) })
    }

    @Test
    fun a_center_the_filter_does_not_know_is_shown_by_its_code_and_is_not_tappable() {
        val row = (details(center = "XYZ").toDisplay().sections.first() as DetailsSection.Fields).rows[1]

        assertEquals(UiText.Plain("XYZ"), row.value)
        assertNull(row.action)
    }

    @Test
    fun picture_ratio_is_kept_within_limits_and_defaults_without_sizes() {
        fun ratio(width: Int?, height: Int?) =
            details(picture = SpaceImageFile("p", width, height, null)).toDisplay().picture.aspectRatio

        assertEquals(2f, ratio(5000, 1000))
        assertEquals(4f / 5, ratio(1000, 5000))
        assertEquals(4f / 3, ratio(null, null))
        // A description that only repeats the title is dropped.
        assertNull(details(description = "Title").toDisplay().description)
    }

    @Test
    fun chips_list_what_is_set_with_short_texts() {
        val query = SpaceQuery(
            center = SpaceCenter.JSC,
            yearStart = 2019,
            yearEnd = 2019,
            tag = SpaceTag(SpaceTagKind.KEYWORD, "Apollo 11"),
        )

        val chips = spaceChips(query, searchText = " moon ")

        assertEquals(
            listOf(UiText.Plain("moon"), UiText.Plain("JSC"), UiText.Plain("2019"), UiText.Plain("Apollo 11")),
            chips.map { it.text },
        )
        assertEquals(SpaceChipTarget.SEARCH, chips.first().opens)
        // Removing the center keeps everything else that is set.
        assertEquals(SpaceIntent.SetFilters(null, 2019, 2019, query.tag), chips[1].remove)
        assertEquals(UiText.KeyWith(AppStringKey.Space.FROM, "1960"), spaceChips(SpaceQuery(yearStart = 1960), "").single().text)
    }

    @Test
    fun year_lists_keep_the_range_valid_and_the_count_waits_for_loading() {
        val filters = spaceFilters(SpaceQuery(yearStart = 1990, yearEnd = 2000), totalCount = 42, isLoading = false)

        assertEquals(2000, filters.fromYears.first())
        assertEquals(1900, filters.fromYears.last())
        assertEquals(1990, filters.toYears.last())
        assertEquals(42, filters.resultCount)
        assertNull(spaceFilters(SpaceQuery(), totalCount = 42, isLoading = true).resultCount)
    }
}
