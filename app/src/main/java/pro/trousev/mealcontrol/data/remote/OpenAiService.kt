package pro.trousev.mealcontrol.data.remote

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI

class OpenAiService(
    private val apiKey: String
) {
    private val openAI: OpenAI = OpenAI(token = apiKey)

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
