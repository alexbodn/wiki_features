package com.example.geosearch.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object OverpassRetrofitClient {
    private const val BASE_URL = "http://overpass-api.de/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("User-Agent", "GeoSearchApp/1.0 (https://github.com/geosearch)")
            .build()
        chain.proceed(request)
    }.build()

    val instance: OverpassService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(OverpassService::class.java)
    }
}
