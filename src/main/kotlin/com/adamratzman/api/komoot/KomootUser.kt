package com.adamratzman.komoot

import kotlinx.serialization.Serializable

@Serializable
data class KomootUser(
    val email: String,
    val password: String,
    val user: User,
    val username: String
)

@Serializable
data class User(
    val content: Content,
    val createdAt: String,
    val displayname: String,
    val fitness: Fitness,
    val imageUrl: String,
    val locale: String,
    val metric: Boolean,
    val newsletter: Boolean,
    val state: String,
    val username: String,
    val welcomeMails: Boolean
)

@Serializable
data class Fitness(val personalised: Boolean)

@Serializable
data class Content(val hasImage: Boolean)