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

    fun getOfflineBotResponse(promptText: String): String {
        val p = promptText.trim().lowercase(java.util.Locale.US)
        return when {
            p.contains("hi") || p.contains("hello") || p.contains("hey") || p.contains("who are you") || p.contains("identity") || p.contains("muli bwanji") || p.contains("support") || p.contains("bot") -> {
                "Moni! I am Khobili Bot, your 24/7 digital group savings advisor. I am currently running in Offline Mode, meaning I can guide you through all local Matope Village Bank operations and regulations without intermediate network access! Ask me anything about savings, loan limits, compound/simple interest, or DZIDZIDZI emergency funds."
            }
            p.contains("interest") || p.contains("chiwongoladzanja") || p.contains("calculate") || p.contains("rate") || p.contains("percent") || p.contains("flat") || p.contains("reducing") || p.contains("compound") -> {
                "Khobili Offline Advisor: Our village bank supports three distinct calculation models: Simple Flat, Monthly Compounded, and Reducing Balance. The default monthly interest is set by our group preferences (normally 10%). You can preview custom amortized schedules using the Automated Interest Calculator in our Loans tab!"
            }
            p.contains("limit") || p.contains("max") || p.contains("borrow") || p.contains("loan") || p.contains("multiplier") || p.contains("kopempha") || p.contains("ngongole") -> {
                "Khobili Offline Advisor: Under standard CARE rules, your borrowing limit is strictly capped at up to 3x (or custom configured multiplier) of your cumulative personal savings inside the group bank. You can request loans in the Loans tab, which validates thresholds and active debt status automatically."
            }
            p.contains("contribution") || p.contains("save") || p.contains("share") || p.contains("deposit") || p.contains("zopereka") || p.contains("box") -> {
                "Khobili Offline Advisor: Group members can buy between 1 and 5 shares per monthly meeting. The default share value is MK 1,000 (maximum of MK 5,000 per meeting). Converted savings pool together to fund interest-accruing loans, building dividend reserves."
            }
            p.contains("emergency") || p.contains("thumba") || p.contains("dzidzidzi") || p.contains("medical") || p.contains("fee") || p.contains("help") -> {
                "Khobili Offline Advisor: The Emergency Fund (Thumba la Dzidzidzi) is a separate pool where members pay a flat fee (default MK 500) per meeting. These funds do not earn interest and are disbursed purely for welfare/medical assistance, or split equally at the end of the year."
            }
            p.contains("payout") || p.contains("dividend") || p.contains("year") || p.contains("cycle") || p.contains("care") || p.contains("gawana") -> {
                "Khobili Offline Advisor: At the end of the annual savings cycle, the entire portfolio is resolved under official CARE manual guidelines. Members receive their fully contributed principal plus collected interest dividends prorated in proportion to their share savings."
            }
            else -> {
                "Khobili Offline Advisor: I am here locally to assist! Your group data is secured with secure, end-to-end local persistence. You can record contributions, request loans with safety threshold warnings, compute interest structures, or trigger backups completely offline. Let me know if you need specific details!"
            }
        }
    }

    suspend fun generateSupportResponse(chatHistory: List<GeminiContent>): String {
        val lastUserMessage = chatHistory.lastOrNull { it.role == "user" || it.role == null }?.parts?.firstOrNull()?.text ?: ""
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getOfflineBotResponse(lastUserMessage)
        }

        val systemPrompt = """
            You are "Khobili bot", the 24/7 AI financial support assistant for the Village Savings and Loan Association (VSLA) group (named "Village Savings").
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
            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (textResult.isNullOrBlank() || textResult.contains("offline", ignoreCase = true) || textResult.contains("error", ignoreCase = true)) {
                getOfflineBotResponse(lastUserMessage)
            } else {
                textResult
            }
        } catch (e: Exception) {
            getOfflineBotResponse(lastUserMessage)
        }
    }
}
