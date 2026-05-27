package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiResponseCandidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateSupportResponse(chatHistory: List<GeminiContent>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Support Agent offline: Please set valid GEMINI_API_KEY in Secrets. [Fallback: Ask our group leaders concerning contribution and loan regulations!]"
        }

        val systemPrompt = """
            You are "Chuma Bot", the 24/7 AI financial support assistant for the Village Savings and Loan Association (VSLA) group (named "Village Savings").
            Our organization conducts monthly contributions (typically around 200 to 500 currency modules), awards loans at a standard 10% interest rate over a 3-month term, and manages a shared financial pool.
            You should act as an empathetic, professional financial advisor and support coordinator. Help non-technical members understand:
            1. How compound/simple interest works for their group loans.
            2. The benefits of automated repayment alerts.
            3. The security of data encryption hashes.
            4. Practical planning ideas for farming, seed purchases, small trade inventories, or emergency cash flows.
            Keep responses clear, warm, concise, and structured. Encourage offline record checks for transparency.
        """.trimIndent()

        val request = GeminiRequest(
            contents = chatHistory,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I did not receive a proper response. Please check your network connection or try again shortly."
        } catch (e: Exception) {
            "An error occurred: ${e.localizedMessage ?: "Unable to fetch AI response."}. (Using offline FAQ fallback rules: Contributions can be logged in the Contributions tab. Active loans will accrue standard interest and should be repaid in full before maturity.)"
        }
    }
}
