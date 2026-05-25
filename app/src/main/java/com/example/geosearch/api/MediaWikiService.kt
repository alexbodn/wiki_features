package com.example.geosearch.api

import com.example.geosearch.model.GeosearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MediaWikiService {
    @GET("w/api.php")
    suspend fun getNearbyPagesWithImages(
        @Query("action") action: String = "query",
        @Query("generator") generator: String = "geosearch",
        @Query("prop") prop: String = "coordinates|pageimages",
        @Query("ggscoord") ggscoord: String,
        @Query("ggsradius") ggsradius: Int,
        @Query("format") format: String = "json",
        @Query("ggslimit") limit: Int = 50
    ): GeosearchResponse
}
