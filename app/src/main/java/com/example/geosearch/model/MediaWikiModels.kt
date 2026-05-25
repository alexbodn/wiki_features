package com.example.geosearch.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeosearchResponse(
    @Json(name = "query") val query: Query?
)

@JsonClass(generateAdapter = true)
data class Query(
    @Json(name = "pages") val pages: Map<String, Page>?
)

@JsonClass(generateAdapter = true)
data class Page(
    @Json(name = "pageid") val pageid: Long,
    @Json(name = "title") val title: String,
    @Json(name = "coordinates") val coordinates: List<Coordinate>?,
    @Json(name = "thumbnail") val thumbnail: Thumbnail?
)

@JsonClass(generateAdapter = true)
data class Coordinate(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double
)

@JsonClass(generateAdapter = true)
data class Thumbnail(
    @Json(name = "source") val source: String,
    @Json(name = "width") val width: Int,
    @Json(name = "height") val height: Int
)
