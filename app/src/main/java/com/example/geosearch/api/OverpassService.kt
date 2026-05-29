package com.example.geosearch.api

import com.example.geosearch.model.OverpassResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassService {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun getFeatures(
        @Field("data") query: String
    ): OverpassResponse
}
