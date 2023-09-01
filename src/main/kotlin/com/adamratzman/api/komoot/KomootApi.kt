package com.adamratzman.api.komoot

import com.adamratzman.HttpClient
import com.adamratzman.komootTours
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val komootApiBase = "https://api.komoot.de/v007"

object KomootApi {
    private lateinit var user: KomootUser

    suspend fun login() {
        val email = System.getenv("KomootEmail")
        val password = System.getenv("KomootPassword")

        user = HttpClient.get("https://api.komoot.de/v006/account/email/$email/") {
            basicAuth(email, password)
        }.body<KomootUser>()
    }

    suspend fun setNameOfTour(tour: Tour, newName: String) {
        HttpClient.patch("$komootApiBase/tours/${tour.id}?hl=en") {
            basicAuth(user.user.username, user.password)
            accept(ContentType.Any)
            contentType(ContentType.Application.HalJson)
            setBody(KomootTourUpdateNameBody(name = newName))
        }.throwIf401()
    }

    suspend fun getAllTours(): List<Tour> {
        suspend fun makeRequest(url: String): KomootToursResponse {
            return loginIf401 {
                HttpClient.get(url) {
                    basicAuth(user.user.username, user.password)
                    accept(ContentType.Any)
                }
                    .throwIf401()
                    .body()
            }
        }

        val allResponses =
            generateSequence(makeRequest("$komootApiBase/users/${user.user.username}/tours/?type=tour_recorded&format=coordinate_array")) { komootToursResponse ->
                runBlocking {
                    if (komootToursResponse.links.next == null) null
                    else makeRequest(komootToursResponse.links.next.href)
                }
            }.toList()

        val allTours = allResponses.flatMap {
            it.embedded.tours
        }

        val deduplicatedTours = mutableListOf<Tour>()

        allTours.forEach { tour ->
            val tourStart = Instant.parse(tour.date)
            val tourActivityPeriod = ActivityPeriod(tourStart, tourStart.plus(tour.duration.seconds))
            val hasAnyOtherOverlap = deduplicatedTours.any { otherTour ->
                if (otherTour == tour) false
                else {
                    val otherTourStart = Instant.parse(otherTour.date)
                    val otherTourActivityPeriod =
                        ActivityPeriod(otherTourStart, otherTourStart.plus(otherTour.duration.seconds))

                    tourActivityPeriod.hasOverlapWith(otherTourActivityPeriod)
                }
            }

            if (!hasAnyOtherOverlap) deduplicatedTours += tour
        }

        return deduplicatedTours.sortedByDescending { Instant.parse(it.date).epochSeconds }
    }

    private suspend fun <T> loginIf401(retry: Boolean = true, body: suspend () -> T): T {
        return try {
            body()
        } catch (ignored: Exception) {
            if (retry) {
                login()
                loginIf401(body = body, retry = false)
            } else throw ignored
        }
    }
}

fun HttpResponse.throwIf401(): HttpResponse {
    if (status == HttpStatusCode.Unauthorized) throw IOException()
    return this
}

data class ActivityPeriod(val start: Instant, val end: Instant) {
    fun hasOverlapWith(other: ActivityPeriod): Boolean {
        return start <= other.end && other.start <= end
    }
}

@Serializable
data class KomootTourUpdateNameBody(val name: String)

enum class SportType {
    Biking,
    EBiking,
    Running,
    Hiking,
    Other
}

enum class BikeType(val isElectric: Boolean) {
    Propella_7S(true),
    Specialized_Turbo_Vado(true),
    Cervelo(false);

    val serializableBikeInfo = SerializableBikeInfo(name, isElectric)
}

@Serializable
data class SerializableBikeInfo(val name: String, val isElectric: Boolean)

fun String.toKomootSportType(name: String): SportType = when {
    contains("bike") || contains("bicycle") -> {
        if (name.endsWith("(R)")) SportType.Biking else SportType.EBiking
    }
    this == "jogging" || this == "running" -> SportType.Running
    this == "hiking" -> SportType.Hiking
    else -> SportType.Other
}
@Serializable
data class DayMonth(val day: Int, val month: Int)

fun computeActivityDistancesByWeek(): SortedMap<WeekMonthYearPair, Map<SportType, Double>> {
    val now = Clock.System.now()
    val startOfThisWeek = getStartOfWeekForInstant(now)
    val lastActivityEpochSeconds = komootTours.last().let { Instant.parse(it.date) }.epochSeconds

    val toursWithLocalDateTime = komootTours.associateBy { Instant.parse(it.date).toLocalDateTime(TimeZone.currentSystemDefault()) }

    val builder = sequence {
        var currentWeek = startOfThisWeek

        while (currentWeek.epochSeconds >= lastActivityEpochSeconds) {
            val currentWeekStartLocalDateTime = currentWeek.toLocalDateTime(TimeZone.currentSystemDefault())
            val currentWeekEndLocalDateTime = currentWeek.plus(6.days).toLocalDateTime(TimeZone.currentSystemDefault())

            val dateRange = getWeekDateRangeForStartOfWeek(currentWeekStartLocalDateTime)
            val toursInRangeBySport = toursWithLocalDateTime
                .filter { it.key.year * 365 + it.key.dayOfYear in dateRange }
                .map { it.value }
                .groupBy { it.sport.toKomootSportType(it.name) }

            val distanceBySportType: Map<SportType, Double> = toursInRangeBySport.map { (sportType, tours) ->
                sportType to tours.sumOf { it.distance }
            }.toMap()

            yield(
                WeekMonthYearPair(
                    currentWeekStartLocalDateTime.dayOfMonth,
                    currentWeekStartLocalDateTime.month.value,
                    currentWeekEndLocalDateTime.dayOfMonth,
                    currentWeekEndLocalDateTime.month.value,
                    currentWeekStartLocalDateTime.year,
                    currentWeek.epochSeconds
                ) to distanceBySportType
            )

            currentWeek = currentWeek.minus(7.days)
        }
    }

    return builder
        .toMap()
        .toSortedMap()
}

fun getStartOfWeekForInstant(instant: Instant): Instant {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    return instant
        .minus((localDateTime.dayOfWeek.value - 1).days)
        .minus(localDateTime.hour.hours)
        .minus(localDateTime.hour.minutes)
}

fun getWeekDateRangeForStartOfWeek(startOfWeek: LocalDateTime): IntRange {
    val start = startOfWeek.year * 365 + startOfWeek.dayOfYear
    return start..<(start + 7)
}