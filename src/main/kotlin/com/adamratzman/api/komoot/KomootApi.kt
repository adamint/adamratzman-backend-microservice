package com.adamratzman.komoot

import com.adamratzman.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.io.IOException
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

        return deduplicatedTours
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