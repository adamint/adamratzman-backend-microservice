package com.adamratzman.api

import kotlinx.serialization.Serializable

@Serializable
data class PaginatableResponse<T>(val data: T, val next: String? = null, val previous: String? = null)