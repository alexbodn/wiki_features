package com.example.geosearch.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "elements") val elements: List<OverpassElement>?
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    @Json(name = "type") val type: String,
    @Json(name = "id") val id: Long,
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?,
    @Json(name = "center") val center: Coordinate?,
    @Json(name = "tags") val tags: Map<String, String>?
)
