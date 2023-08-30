package com.adamratzman.api

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class PaginationRequest(val offset: Int, val limit: Int)

@Serializable
data class PaginationResposne<T>(val data: T, val total: Int, val next: PaginationRequest? = null, val previous: PaginationRequest? = null)

fun Parameters.receivePaginationApiCall(total: Int): PaginationRequest? {
    val offset = get("offset")?.toIntOrNull() ?: return null
    val limit = get("limit")?.toIntOrNull() ?: return null

    // invalid query
    if (isInvalidOffsetAndLimit(offset, limit, total)) return null

    return PaginationRequest(offset = offset, limit = limit)
}

fun isInvalidOffsetAndLimit(offset: Int, limit: Int, total: Int): Boolean {
    return (offset >= total || limit <= 0 || offset < 0)
}