package com.example.geosearch.api

import com.example.geosearch.model.OverpassResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassService {
    @GET("api/interpreter")
    suspend fun getFeatures(
        @Query("data") query: String
    ): OverpassResponse
}
