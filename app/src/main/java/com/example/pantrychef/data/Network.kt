package com.example.pantrychef.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------- TheMealDB (recipes) ----------
// Each meal is a flat JSON object, so we read it as a Map and map it ourselves.
data class MealResponse(val meals: List<Map<String, String?>>?)

interface MealApi {
    @GET("filter.php")
    suspend fun byIngredient(@Query("i") ingredient: String): MealResponse

    @GET("lookup.php")
    suspend fun details(@Query("i") id: String): MealResponse
}

// ---------- Gemini (AI) ----------
data class GeminiRequest(val contents: List<GContent>)
data class GContent(val parts: List<GPart>)
data class GPart(val text: String)
data class GeminiResponse(val candidates: List<GCandidate>?)
data class GCandidate(val content: GContent?)

interface GeminiApi {
    // If Google retires this model name, change it to a current Gemini Flash model.
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generate(
        @Header("x-goog-api-key") key: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}

object Network {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val meals: MealApi = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/api/json/v1/1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MealApi::class.java)

    val gemini: GeminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)
}
