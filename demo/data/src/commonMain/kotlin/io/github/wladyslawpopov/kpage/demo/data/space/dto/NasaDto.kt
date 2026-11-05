package io.github.wladyslawpopov.kpage.demo.data.space.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Only `nasa_id` is required: one odd item must not fail a whole page.

@Serializable
internal data class NasaSearchDto(val collection: NasaCollectionDto)

@Serializable
internal data class NasaCollectionDto(
    val items: List<NasaItemDto> = emptyList(),
    val metadata: NasaMetadataDto,
)

@Serializable
internal data class NasaMetadataDto(@SerialName("total_hits") val totalHits: Int)

@Serializable
internal data class NasaItemDto(
    val data: List<NasaItemDataDto> = emptyList(),
    val links: List<NasaLinkDto> = emptyList(),
)

@Serializable
internal data class NasaItemDataDto(
    @SerialName("nasa_id") val nasaId: String,
    val title: String? = null,
    val description: String? = null,
    @SerialName("date_created") val dateCreated: String? = null,
    val center: String? = null,
    val keywords: List<String> = emptyList(),
    val photographer: String? = null,
    val location: String? = null,
    @SerialName("secondary_creator") val secondaryCreator: String? = null,
)

@Serializable
internal data class NasaLinkDto(
    val href: String,
    val rel: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null,
)

/** The body of a NASA error answer. */
@Serializable
internal data class NasaErrorDto(val reason: String? = null)
