package com.adamratzman.komoot


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KomootToursResponse(
    @SerialName("_embedded") val embedded: Embedded,
    @SerialName("_links") val links: LinksXX,
    @SerialName("page") val page: Page
)

@Serializable
data class Avatar(
    @SerialName("src") val src: String,
    @SerialName("templated") val templated: Boolean,
    @SerialName("type") val type: String
)

@Serializable
data class Coordinates(
    @SerialName("href") val href: String
)

@Serializable
data class CoverImages(
    @SerialName("href") val href: String
)

@Serializable
data class Creator(
    @SerialName("avatar") val avatar: Avatar,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("_links") val links: Links,
    @SerialName("status") val status: String,
    @SerialName("username") val username: String
)

@Serializable
data class CreatorX(
    @SerialName("href") val href: String
)

@Serializable
data class Difficulty(
    @SerialName("explanation_fitness") val explanationFitness: String,
    @SerialName("explanation_technical") val explanationTechnical: String,
    @SerialName("grade") val grade: String
)

@Serializable
data class Directions(
    @SerialName("href") val href: String
)

@Serializable
data class Embedded(
    @SerialName("tours") val tours: List<Tour>
)

@Serializable
data class EmbeddedX(
    @SerialName("creator") val creator: Creator
)

@Serializable
data class Links(
    @SerialName("relation") val relation: Relation, @SerialName("self") val self: Self
)

@Serializable
data class LinksX(
    @SerialName("coordinates") val coordinates: Coordinates,
    @SerialName("cover_images") val coverImages: CoverImages,
    @SerialName("creator") val creator: CreatorX,
    @SerialName("directions") val directions: Directions? = null,
    @SerialName("participants") val participants: Participants,
    @SerialName("self") val self: Self,
    @SerialName("surfaces") val surfaces: Surfaces? = null,
    @SerialName("timeline") val timeline: Timeline,
    @SerialName("translations") val translations: Translations,
    @SerialName("way_types") val wayTypes: WayTypes? = null
)

@Serializable
data class LinksXX(
    @SerialName("next") val next: Next? = null,
    @SerialName("self") val self: Self
)

@Serializable
data class Location(
    @SerialName("alt") val alt: Double,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double
)

@Serializable
data class MapImage(
    @SerialName("attribution") val attribution: String,
    @SerialName("src") val src: String,
    @SerialName("templated") val templated: Boolean,
    @SerialName("type") val type: String
)

@Serializable
data class MapImagePreview(
    @SerialName("attribution") val attribution: String,
    @SerialName("src") val src: String,
    @SerialName("templated") val templated: Boolean,
    @SerialName("type") val type: String
)

@Serializable
data class Next(
    @SerialName("href") val href: String
)

@Serializable
data class Page(
    @SerialName("number") val number: Int,
    @SerialName("size") val size: Int,
    @SerialName("totalElements") val totalElements: Int,
    @SerialName("totalPages") val totalPages: Int
)

@Serializable
data class Participants(
    @SerialName("href") val href: String
)

@Serializable
data class Path(
    @SerialName("index") val index: Int,
    @SerialName("location") val location: Location,
    @SerialName("reference") val reference: String? = null
)

@Serializable
data class Relation(
    @SerialName("href") val href: String, @SerialName("templated") val templated: Boolean
)

@Serializable
data class Segment(
    @SerialName("from") val from: Int, @SerialName("to") val to: Int, @SerialName("type") val type: String
)

@Serializable
data class SegmentX(
    @SerialName("from") val from: Int, @SerialName("to") val to: Int
)

@Serializable
data class Self(
    @SerialName("href") val href: String
)

@Serializable
data class StartPoint(
    @SerialName("alt") val alt: Double, @SerialName("lat") val lat: Double, @SerialName("lng") val lng: Double
)

@Serializable
data class Summary(
    @SerialName("surfaces") val surfaces: List<Surface>, @SerialName("way_types") val wayTypes: List<WayType>
)

@Serializable
data class Surface(
    @SerialName("amount") val amount: Double, @SerialName("type") val type: String
)

@Serializable
data class Surfaces(
    @SerialName("href") val href: String
)

@Serializable
data class Timeline(
    @SerialName("href") val href: String
)

@Serializable
data class Tour(
    @SerialName("changed_at") val changedAt: String,
    @SerialName("constitution") val constitution: Int? = null,
    @SerialName("date") val date: String,
    @SerialName("difficulty") val difficulty: Difficulty? = null,
    @SerialName("distance") val distance: Double,
    @SerialName("duration") val duration: Int,
    @SerialName("elevation_down") val elevationDown: Double,
    @SerialName("elevation_up") val elevationUp: Double,
    @SerialName("_embedded") val embedded: EmbeddedX,
    @SerialName("id") val id: Int,
    @SerialName("kcal_active") val kcalActive: Int,
    @SerialName("kcal_resting") val kcalResting: Int,
    @SerialName("_links") val links: LinksX,
    @SerialName("map_image") val mapImage: MapImage,
    @SerialName("map_image_preview") val mapImagePreview: MapImagePreview,
    @SerialName("name") val name: String,
    @SerialName("path") val path: List<Path>? = null,
    @SerialName("potential_route_update") val potentialRouteUpdate: Boolean,
    @SerialName("query") val query: String? = null,
    @SerialName("segments") val segments: List<Segment>? = null,
    @SerialName("source") val source: String,
    @SerialName("sport") val sport: String,
    @SerialName("start_point") val startPoint: StartPoint,
    @SerialName("status") val status: String,
    @SerialName("summary") val summary: Summary? = null,
    @SerialName("time_in_motion") val timeInMotion: Int? = null,
    @SerialName("tour_information") val tourInformation: List<TourInformation>? = null,
    @SerialName("type") val type: String,
    @SerialName("vector_map_image") val vectorMapImage: VectorMapImage,
    @SerialName("vector_map_image_preview") val vectorMapImagePreview: VectorMapImagePreview
)

@Serializable
data class TourInformation(
    @SerialName("segments") val segments: List<SegmentX>, @SerialName("type") val type: String
)

@Serializable
data class Translations(
    @SerialName("href") val href: String
)

@Serializable
data class VectorMapImage(
    @SerialName("attribution") val attribution: String,
    @SerialName("src") val src: String,
    @SerialName("templated") val templated: Boolean,
    @SerialName("type") val type: String
)

@Serializable
data class VectorMapImagePreview(
    @SerialName("attribution") val attribution: String,
    @SerialName("src") val src: String,
    @SerialName("templated") val templated: Boolean,
    @SerialName("type") val type: String
)

@Serializable
data class WayType(
    @SerialName("amount") val amount: Double, @SerialName("type") val type: String
)

@Serializable
data class WayTypes(
    @SerialName("href") val href: String
)