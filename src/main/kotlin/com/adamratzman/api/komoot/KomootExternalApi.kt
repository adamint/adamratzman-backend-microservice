package com.adamratzman.api.komoot

import com.adamratzman.activityDistancesByWeek
import com.adamratzman.api.PaginationRequest
import com.adamratzman.api.PaginationResposne
import com.adamratzman.api.isInvalidOffsetAndLimit
import com.adamratzman.api.receivePaginationApiCall
import com.adamratzman.komootTours
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import java.time.format.TextStyle
import java.util.*

fun Routing.komootExternalApi() {
    get("/latest-komoot-tours-by-month") {
        val toursByMonth = komootTours
            .associateWith { Instant.parse(it.date).toLocalDateTime(TimeZone.currentSystemDefault()) }
            .entries
            .groupBy {
                MonthYearPair(
                    it.value.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.US),
                    it.value.year
                )
            }

        val paginationParameters = call.parameters.receivePaginationApiCall(toursByMonth.size)
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        val filteredPublicToursByMonth = toursByMonth
            .toList()
            .subList(paginationParameters.offset, paginationParameters.offset + paginationParameters.limit)
            .map {
                ToursInMonthYear(
                    it.first,
                    it.second.map { pair -> pair.key.toPublicTour() },
                    it.second
                        .groupBy { pair -> pair.key.sport.toKomootSportType(pair.key.name) }
                        .map { pair -> pair.key to pair.value.sumOf { entry -> entry.key.distance } }
                        .toMap()
                )
            }

        val nextOffset = paginationParameters.offset + paginationParameters.limit
        val previousOffset = paginationParameters.offset - paginationParameters.limit

        val next = if (isInvalidOffsetAndLimit(nextOffset, paginationParameters.limit, toursByMonth.size)) null
        else PaginationRequest(nextOffset, paginationParameters.limit)

        val previous = if (isInvalidOffsetAndLimit(previousOffset, paginationParameters.limit, toursByMonth.size)) null
        else PaginationRequest(previousOffset, paginationParameters.limit)

        call.respond(
            PaginationResposne(
                data = filteredPublicToursByMonth,
                total = toursByMonth.size,
                next = next,
                previous = previous
            )
        )
    }

    get("/activity-stats-by-week") {
        val paginationParameters = call.parameters.receivePaginationApiCall(activityDistancesByWeek.size)
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        val filteredWeeksData: List<Pair<WeekMonthYearPair, Map<SportType, Double>>> = activityDistancesByWeek
            .toList()
            .subList(paginationParameters.offset, paginationParameters.offset + paginationParameters.limit)

        val nextOffset = paginationParameters.offset + paginationParameters.limit
        val previousOffset = paginationParameters.offset - paginationParameters.limit

        val next =
            if (isInvalidOffsetAndLimit(nextOffset, paginationParameters.limit, activityDistancesByWeek.size)) null
            else PaginationRequest(nextOffset, paginationParameters.limit)

        val previous =
            if (isInvalidOffsetAndLimit(previousOffset, paginationParameters.limit, activityDistancesByWeek.size)) null
            else PaginationRequest(previousOffset, paginationParameters.limit)

        call.respond(
            PaginationResposne(
                data = filteredWeeksData,
                total = activityDistancesByWeek.size,
                next = next,
                previous = previous
            )
        )
    }
}


@Serializable
data class ToursInMonthYear(
    val monthYearPair: MonthYearPair,
    val tours: List<PublicTourInfo>,
    val distanceBySportType: Map<SportType, Double>
)

@Serializable
data class MonthYearPair(val month: String, val year: Int)

@Serializable
data class WeekMonthYearPair(
    val weekStartDay: Int,
    val weekStartMonth: Int,
    val weekEndDay: Int,
    val weekEndMonth: Int,
    val year: Int,
    val startEpochSeconds: Long
) : Comparable<WeekMonthYearPair> {
    override fun compareTo(other: WeekMonthYearPair): Int {
        return (other.startEpochSeconds - startEpochSeconds).toInt()
    }
}

@Serializable
data class SerializableLocalDate(
    val dateMillis: Long,
    val minute: Int,
    val hourOfDay: Int,
    val dayOfWeek: SerializableDayOfWeek,
    val dayOfMonth: Int,
    val month: SerializableMonth,
    val year: Int
)

@Serializable
data class SerializableDayOfWeek(val number: Int, val name: String)

@Serializable
data class SerializableMonth(val number: Int, val name: String)

@Serializable
data class RouteElevation(val up: Double, val down: Double)

@Serializable
data class PublicTourInfo(
    val name: String,
    val duration: Int,
    val distance: Double,
    val sportType: SportType,
    val bicycleInfo: SerializableBikeInfo?,
    val date: SerializableLocalDate,
    val mapImage: MapImage,
    val elevation: RouteElevation
)

fun Instant.toSerializable() = with(toLocalDateTime(TimeZone.currentSystemDefault())) {
    SerializableLocalDate(
        this@toSerializable.toEpochMilliseconds(),
        minute,
        hour,
        SerializableDayOfWeek(dayOfWeek.value, dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.US)),
        dayOfMonth,
        SerializableMonth(monthNumber, month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.US)),
        year
    )
}

fun Tour.toPublicTour(): PublicTourInfo {
    val sportType = sport.toKomootSportType(name)

    fun getNameAndSubtype(): Pair<String, SerializableBikeInfo?> {
        if (sportType != SportType.Biking && sportType != SportType.EBiking) return name to null

        return when {
            name.endsWith("(P)") -> name.removeSuffix("(P)").trim() to BikeType.Propella_7S.serializableBikeInfo
            name.endsWith("(R)") -> name.removeSuffix("(R)").trim() to BikeType.Cervelo_SLC_SL.serializableBikeInfo
            name.endsWith("(C)") -> name.removeSuffix("(C)").trim() to BikeType.REI_CO_OP_GENERATION_E.serializableBikeInfo
            else -> name to BikeType.Specialized_Turbo_Vado.serializableBikeInfo
        }
    }

    val (parsedName, parsedSubtype) = getNameAndSubtype()

    return PublicTourInfo(
        parsedName,
        duration,
        distance,
        sportType,
        parsedSubtype,
        Instant.parse(date).toSerializable(),
        mapImage,
        RouteElevation(up = elevationUp, down = elevationDown)
    )
}

