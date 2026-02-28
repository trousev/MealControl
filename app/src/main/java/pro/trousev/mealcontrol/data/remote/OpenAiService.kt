package pro.trousev.mealcontrol.data.remote

import android.util.Log
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "OpenAiService"

class OpenAiService(
    private val apiKey: String
) {
    private val openAI: OpenAI = OpenAI(token = apiKey)
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            socketTimeoutMillis = 120000
            connectTimeoutMillis = 30000
        }
    }

    suspend fun chat(messages: List<ChatMessage>): Result<String> {
        return try {
            val completion: ChatCompletion = openAI.chatCompletion(
                request = com.aallam.openai.api.chat.ChatCompletionRequest(
                    model = ModelId("gpt-5-mini-2025-08-07"),
                    messages = messages
                )
            )
            val response = completion.choices.firstOrNull()?.message?.content
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(Exception("Empty response from OpenAI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun detectMealFromImage(
        imageBase64: String,
        promptId: String,
        chatHistory: List<ChatHistoryItem>,
        includeImage: Boolean = true
    ): Result<MealDetectionResponse> {
        return try {
            val contentItems = mutableListOf<ContentItem>()

            if (includeImage) {
                val imageContent = ContentItem(
                    type = "input_image",
                    imageUrl = "data:image/jpeg;base64," + imageBase64
                )
                contentItems.add(imageContent)
            }

            val textParts = mutableListOf<String>()
            for (item in chatHistory) {
                if (item.isFromUser) {
                    textParts.add("User: ${item.content}")
                } else {
                    textParts.add("AI: ${item.content}")
                }
            }
            
            if (textParts.isNotEmpty()) {
                contentItems.add(ContentItem(type = "input_text", text = textParts.joinToString("\n")))
            } else {
                contentItems.add(ContentItem(type = "input_text", text = "Please analyze this meal image and identify all food components with their nutritional information."))
            }

            val inputList = listOf(
                InputMessage(
                    role = "user",
                    content = contentItems
                )
            )

            val requestBody = MealDetectionRequest(
                model = "gpt-5-mini-2025-08-07",
                input = inputList,
                prompt = PromptInfo(id = promptId, version = "3")
            )

            val response: HttpResponse = httpClient.post("https://api.openai.com/v1/responses") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(requestBody)
            }

            val responseText = response.body<String>()
            
            Log.d(TAG, "Raw response: $responseText")
            
            if (response.status.value >= 400) {
                return Result.failure(Exception("API Error (${response.status.value}): $responseText"))
            }

            val responseBody: MealDetectionResponse = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<MealDetectionResponse>(responseText)
            
            Result.success(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        const val SYSTEM_PROMPT = """You are a helpful nutrition and weight loss assistant. 
You specialize in helping users with:
- Meal planning and calorie counting
- Weight loss strategies
- Healthy eating habits
- Macronutrient balance
- Exercise recommendations

Provide practical, evidence-based advice. Always consider the user's personal context 
(weight goals, activity level, dietary preferences). Be concise but thorough. 
If you don't know something, admit it honestly."""

        fun estimateTokens(text: String): Int {
            return text.length / 4
        }

        fun truncateToTokenLimit(messages: List<ChatMessage>, maxTokens: Int = 100000): List<ChatMessage> {
            var totalTokens = 0
            val result = mutableListOf<ChatMessage>()

            val systemMessages = messages.filter { it.role == ChatRole.System }
            val otherMessages = messages.filter { it.role != ChatRole.System }

            for (msg in systemMessages) {
                val content = msg.content ?: continue
                val tokens = estimateTokens(content)
                if (totalTokens + tokens <= maxTokens) {
                    result.add(msg)
                    totalTokens += tokens
                }
            }

            for (msg in otherMessages.reversed()) {
                val content = msg.content ?: continue
                val tokens = estimateTokens(content)
                if (totalTokens + tokens <= maxTokens) {
                    result.add(0, msg)
                    totalTokens += tokens
                } else {
                    break
                }
            }

            return result
        }
    }
}

@Serializable
data class MealDetectionRequest(
    val model: String? = null,
    val input: List<InputMessage>? = null,
    val prompt: PromptInfo? = null
)

@Serializable
data class InputMessage(
    val role: String,
    val content: List<ContentItem>
)

@Serializable
data class ContentItem(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class PromptInfo(
    val id: String,
    val version: String
)

@Serializable
data class MealDetectionResponse(
    val id: String? = null,
    @SerialName("meal_components") val mealComponents: List<MealComponentDto>? = null,
    val output: List<OutputItem>? = null,
    val error: String? = null
)

@Serializable
data class OutputItem(
    val id: String? = null,
    val type: String? = null,
    val status: String? = null,
    val text: String? = null,
    val content: List<OutputContentItem>? = null,
    @SerialName("message") val message: MessageContent? = null
)

@Serializable
data class OutputContentItem(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class MessageContent(
    val content: String? = null
)

@Serializable
data class MealComponentDto(
    val name: String,
    @SerialName("weight_g") val weightG: Int,
    @SerialName("energy_kcal") val energyKcal: Int,
    @SerialName("fat_g") val fatG: Int,
    @SerialName("protein_g") val proteinG: Int,
    @SerialName("carbs_g") val carbsG: Int
)

data class ChatHistoryItem(
    val content: String,
    val isFromUser: Boolean
)
