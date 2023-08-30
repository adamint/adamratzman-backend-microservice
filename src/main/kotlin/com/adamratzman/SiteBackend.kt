package com.adamratzman

import com.adamratzman.api.komoot.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.SortedMap
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes

val LenientJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

val HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(LenientJson)
    }
}

var komootTours: List<Tour> = emptyList()
var activityDistancesByDayPerWeek: SortedMap<WeekMonthYearPair, WeekActivityDistance> = sortedMapOf()

suspend fun main() {
    KomootApi.login()
    fixedRateTimer("Komoot Update", daemon = true, initialDelay = 0, period = 60.minutes.inWholeMilliseconds) {
        runBlocking {
            komootTours = KomootApi.getAllTours()
            activityDistancesByDayPerWeek = computeActivityDistancesByDayPerWeek()
        }
    }

    embeddedServer(Netty, port = 80, module = setUpServer()).start(wait = true)
}

private fun setUpServer(): Application.() -> Unit = {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(LenientJson)
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    routing {
        get("/") { call.respondText("hi :)") }
        komootExternalApi()
    }
}