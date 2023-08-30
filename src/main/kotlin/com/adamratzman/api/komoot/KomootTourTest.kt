package com.adamratzman.komoot

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

suspend fun main() = runBlocking {
    KomootApi.login()
    val tours = KomootApi.getAllTours()

    val toursWithDateTime = tours.associateWith { ZonedDateTime.parse(it.date) }
    val groupedTours = toursWithDateTime.entries.groupBy {
        "${it.value.year}-${it.value.month}"
    }

    groupedTours.forEach { (dateTime, toursWithDatetime) ->
        val monthTours = toursWithDatetime.map { it.key }
        val groupedToursBySport =
            monthTours.groupBy { if (it.sport.contains("bike") || it.sport.contains("bicycle")) "bike" else it.sport }
        print("$dateTime| ")

        groupedToursBySport.map { (sport, toursInSport) ->
            val sportOverview = "$sport: ${(toursInSport.sumOf { it.distance } / 1.609 / 1000).toInt()} miles"
        }
            .joinToString(", ")
            .let { println(it) }

        /*groupedToursBySport["bike"]?.let { bikeTours ->
            if (bikeTours.isNotEmpty()) {
                val parseTime = ZonedDateTime.parse(bikeTours.first().date)
                if (parseTime.year < 2022) {
                    bikeTours.forEach {
                        if (!it.name.endsWith("(R)")) KomootApi.setNameOfTour(
                            it,
                            "${it.name}(R)"
                        )
                    }
                }
            }
        }*/
    }
}