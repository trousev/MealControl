package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import pro.trousev.mealcontrol.data.remote.ChatHistoryItem
import pro.trousev.mealcontrol.data.remote.MealComponentDto
import pro.trousev.mealcontrol.data.remote.MealDetectionResponse
import pro.trousev.mealcontrol.data.remote.OpenAiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

private const val TAG = "MealDetection"

data class MealDetectionState(
    val photoUri: String = "",
    val messages: List<MealDetectionMessage> = emptyList(),
    val currentComponents: List<MealComponentDto>? = null,
    val currentQuestion: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val conversationId: Long = -1
)

data class MealDetectionMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long
)

class MealDetectionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MealControlDatabase.getDatabase(application)
    private val userSettingsDao = database.userSettingsDao()
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    private val _state = MutableStateFlow(MealDetectionState())
    val state: StateFlow<MealDetectionState> = _state.asStateFlow()

    private val promptId = "pmpt_69a212c896ec8193a288574454e778290065f891e62410ce"

    fun initializeWithPhoto(photoUri: String) {
        _state.value = MealDetectionState()
        
        viewModelScope.launch {
            _state.value = _state.value.copy(
                photoUri = photoUri,
                isLoading = true,
                error = null
            )

            val conversationId = conversationDao.insertConversation(
                ConversationEntity(
                    title = "Meal Detection",
                    createdAt = System.currentTimeMillis(),
                    isMealDetection = true
                )
            )
            _state.value = _state.value.copy(conversationId = conversationId)

            analyzePhoto()
        }
    }

    private suspend fun analyzePhoto() {
        val settings = userSettingsDao.getSettings()
        val apiKey = settings?.openAiApiKey ?: ""

        if (apiKey.isBlank()) {
            Log.e(TAG, "API key is blank")
            _state.value = _state.value.copy(
                isLoading = false,
                error = "OpenAI API key not configured. Please set it in Settings."
            )
            return
        }

        try {
            val chatHistory = _state.value.messages.map {
                ChatHistoryItem(it.content, it.isFromUser)
            }

            val isFirstMessage = _state.value.messages.isEmpty()
            Log.d(TAG, "isFirstMessage=$isFirstMessage, history size=${chatHistory.size}")

            val openAiService = OpenAiService(apiKey)
            val imageBase64 = withContext(Dispatchers.IO) {
                val file = File(_state.value.photoUri)
                Log.d(TAG, "Reading image file: ${_state.value.photoUri}, size: ${file.length()}")
                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            }
            
            val result = openAiService.detectMealFromImage(
                imageBase64 = imageBase64,
                promptId = promptId,
                chatHistory = chatHistory,
                includeImage = true
            )

            when {
                result.isSuccess -> {
                    val response = result.getOrNull()
                    Log.d(TAG, "Response success: components=${response?.mealComponents?.size}, output=${response?.output?.size}")
                    response?.output?.forEachIndexed { index, out ->
                        Log.d(TAG, "Output[$index]: type=${out.type}, status=${out.status}, content=${out.message?.content?.take(200)}")
                    }
                    processResponse(response)
                }
                result.isFailure -> {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "API call failed: $errorMsg")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "API Error: $errorMsg"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in analyzePhoto: ${e.message}", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    private fun processResponse(response: MealDetectionResponse?) {
        Log.d(TAG, "processResponse called with response=$response")
        
        // Debug: log all output items
        response?.output?.forEachIndexed { index, out ->
            Log.d(TAG, "Output[$index]: type=${out.type}, text=${out.text}, content=${out.content}, message=${out.message}")
            out.content?.forEachIndexed { cindex, content ->
                Log.d(TAG, "  Content[$cindex]: type=${content.type}, text=${content.text}")
            }
        }
        
        val components = response?.mealComponents
        // The actual text is in output[1].content[0].text
        val messageOutput = response?.output?.find { it.type == "message" }
        val responseText = messageOutput?.content?.firstOrNull()?.text 
            ?: messageOutput?.text
            ?: ""
        
        // Try to parse components from JSON response text
        val parsedComponents = parseComponentsFromJson(responseText)
        val allComponents = components ?: parsedComponents
        
        Log.d(TAG, "Components: ${allComponents?.size}, Response text: '${responseText.take(100)}'")
        
        val question = if (responseText.isNotEmpty()) {
            parseClarificationQuestion(responseText)
        } else if (allComponents == null || allComponents.isEmpty()) {
            "Please provide more details about your meal."
        } else {
            null
        }

        val debugInfo = if (response != null) {
            "components=${allComponents?.size ?: "null"}, response='$responseText'"
        } else {
            "response is null"
        }
        
        Log.d(TAG, "question=$question, hasComponents=${allComponents?.isNotEmpty()}")
        Log.d(TAG, "Full responseText for history: $responseText")

        val messageContent = if (question != null) {
            question
        } else if (allComponents != null && allComponents.isNotEmpty()) {
            buildString {
                append("Detected ${allComponents.size} meal components:\n")
                allComponents.forEachIndexed { index, comp ->
                    append("${index + 1}. ${comp.name} - ${comp.weightG}g (${comp.energyKcal} kcal)\n")
                }
            }
        } else {
            responseText.ifEmpty { "Could not detect meal components. Please try again or provide more details." }
        }

        val botMessage = MessageEntity(
            conversationId = _state.value.conversationId,
            content = messageContent,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            messageDao.insertMessage(botMessage)
        }

        if (allComponents != null && allComponents.isNotEmpty()) {
            _state.value = _state.value.copy(
                messages = _state.value.messages + MealDetectionMessage(
                    content = messageContent,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                ),
                currentComponents = allComponents,
                currentQuestion = null,
                isLoading = false,
                error = null
            )
        } else if (question != null) {
            _state.value = _state.value.copy(
                messages = _state.value.messages + MealDetectionMessage(
                    content = messageContent,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                ),
                currentQuestion = question,
                currentComponents = null,
                isLoading = false,
                error = null
            )
        } else {
            _state.value = _state.value.copy(
                messages = _state.value.messages + MealDetectionMessage(
                    content = messageContent,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                ),
                currentQuestion = null,
                currentComponents = null,
                isLoading = false,
                error = "Debug: $debugInfo"
            )
        }
    }

    private fun parseClarificationQuestion(responseText: String): String? {
        if (responseText.startsWith("[") && responseText.contains("\"question\"")) {
            val jsonStr = responseText.trim()
            if (jsonStr.startsWith("[") && jsonStr.contains("question")) {
                val nameMatch = Regex(""""name"\s*:\s*"([^"]+)"""").find(jsonStr)
                val questionMatch = Regex(""""question"\s*:\s*"([^"]+)"""").find(jsonStr)
                if (nameMatch != null && questionMatch != null) {
                    val name = nameMatch.groupValues[1]
                    val question = questionMatch.groupValues[1]
                    return "Question about $name: $question"
                }
            }
        }
        return null
    }

    fun sendFollowUp(text: String) {
        Log.d(TAG, "sendFollowUp: '$text'")
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val userMessage = MessageEntity(
                conversationId = _state.value.conversationId,
                content = text,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(userMessage)

            _state.value = _state.value.copy(
                messages = _state.value.messages + MealDetectionMessage(
                    content = text,
                    isFromUser = true,
                    timestamp = System.currentTimeMillis()
                ),
                currentQuestion = null,
                currentComponents = null
            )

            analyzePhoto()
        }
    }

    private fun parseComponentsFromJson(responseText: String): List<MealComponentDto>? {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(responseText)
            
            // Check if it's a JSON object with "meal_components" key
            if (jsonElement is JsonElement && jsonElement.jsonObject.contains("meal_components")) {
                val componentsArray = jsonElement.jsonObject["meal_components"]?.jsonArray
                componentsArray?.mapNotNull { element ->
                    if (element is JsonElement && element.jsonObject.contains("name")) {
                        val obj = element.jsonObject
                        val getString: (String) -> String = { key ->
                            (obj[key] as? JsonPrimitive)?.content ?: ""
                        }
                        val getInt: (String) -> Int = { key ->
                            (obj[key] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        }
                        MealComponentDto(
                            name = getString("name"),
                            weightG = getInt("weight_g"),
                            energyKcal = getInt("energy_kcal"),
                            fatG = getInt("fat_g"),
                            proteinG = getInt("protein_g"),
                            carbsG = getInt("carbs_g")
                        )
                    } else null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse components from JSON: ${e.message}")
            null
        }
    }

    fun retake() {
        viewModelScope.launch {
            if (_state.value.conversationId > 0) {
                conversationDao.deleteConversation(_state.value.conversationId)
            }
        }
        _state.value = MealDetectionState()
    }
}
