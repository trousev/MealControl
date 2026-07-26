package pro.trousev.mealcontrol.debug

import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity
import java.time.Instant

/**
 * Handles serialization and deserialization of the complete application state
 * to/from a human-readable JSON fixture file.
 *
 * Only available in the debug variant. Uses [ServiceLocator] which must be
 * initialized before calling any methods.
 */
object StateFixtureManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    private const val FORMAT_VERSION = 1
    const val DUMP_FILE_NAME = "state_fixture.json"
    const val RESTORE_FILE_NAME = "restore_fixture.json"

    // ── Serializable data classes ─────────────────────────────────────────

    @Serializable
    data class StateFixture(
        val formatVersion: Int = FORMAT_VERSION,
        val createdAt: String = Instant.now().toString(),
        val description: String = "",
        val roomSchemaVersion: Int = 8,
        val data: FixtureData,
        val secureStorage: SecureStorageData = SecureStorageData(),
    )

    @Serializable
    data class FixtureData(
        val userSettings: UserSettingsData? = null,
        val meals: List<MealWithComponentsData> = emptyList(),
        val conversations: List<ConversationWithMessagesData> = emptyList(),
    )

    @Serializable
    data class UserSettingsData(
        val id: Long = 1,
        val weightKg: Float = 0f,
        val heightCm: Float = 0f,
        val age: Int = 0,
        val gender: String = "MALE",
        val targetWeightChangeKg: Float = 0f,
        val activityLevel: Int = 1,
        val calorieDistribution: String = "HIGH_PROTEIN",
        val customProteinPercent: Int = 40,
        val customFatPercent: Int = 30,
        val customCarbPercent: Int = 30,
        val openAiApiKey: String = "",
        val customModeEnabled: Boolean = false,
        val hideCaloriesEnabled: Boolean = false,
        val hideBudgetExceededEnabled: Boolean = false,
        val customProteinGrams: Int = 0,
        val customFatGrams: Int = 0,
        val customCarbGrams: Int = 0,
    )

    @Serializable
    data class MealData(
        val id: Long = 0,
        val photoUri: String,
        val description: String,
        val timestamp: Long,
    )

    @Serializable
    data class MealComponentData(
        val id: Long = 0,
        val mealId: Long,
        val name: String,
        val weightGrams: Int = 0,
        val calories: Int,
        val proteinGrams: Int = 0,
        val fatGrams: Int = 0,
        val carbGrams: Int = 0,
    )

    @Serializable
    data class MealWithComponentsData(
        val meal: MealData,
        val components: List<MealComponentData> = emptyList(),
    )

    @Serializable
    data class ConversationData(
        val id: Long = 0,
        val title: String,
        val createdAt: Long,
        val isMealDetection: Boolean = false,
    )

    @Serializable
    data class MessageData(
        val id: Long = 0,
        val conversationId: Long,
        val content: String,
        val isFromUser: Boolean,
        val timestamp: Long,
    )

    @Serializable
    data class ConversationWithMessagesData(
        val conversation: ConversationData,
        val messages: List<MessageData> = emptyList(),
    )

    @Serializable
    data class SecureStorageData(
        val apiKey: String = "",
    )

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Reads all application state (Room DB + SecureStorage) and serializes it
     * to a pretty-printed JSON string.
     */
    fun dumpState(context: Context): String {
        val db = ServiceLocator.provideDatabase()
        val settingsRepo = ServiceLocator.provideUserSettingsRepository()

        // Read all data within a single blocking coroutine scope
        val (userSettings, mealsWithComponents, conversationsWithMessages) = runBlocking {
            val settings = settingsRepo.getSettings()
            val meals = db.mealDao().getAllMealsWithComponents()
            val conversations = db.conversationDao().getAllConversationsWithMessages()
            Triple(settings, meals, conversations)
        }

        // Build fixture data
        val fixtureData = FixtureData(
            userSettings = userSettings?.let { mapToUserSettingsData(it) },
            meals = mealsWithComponents.map { mapToMealWithComponentsData(it) },
            conversations = conversationsWithMessages.map { mapToConversationWithMessagesData(it) },
        )

        // API key comes from SecureStorage (already filled in by getSettings())
        val secureStorage = SecureStorageData(
            apiKey = userSettings?.openAiApiKey ?: "",
        )

        val fixture = StateFixture(
            data = fixtureData,
            secureStorage = secureStorage,
        )

        return json.encodeToString(fixture)
    }

    /**
     * Deserializes a JSON fixture string and writes all data to Room DB
     * and SecureStorage. Existing data is cleared first.
     *
     * @throws IllegalArgumentException if the fixture format version is unsupported
     */
    fun restoreState(context: Context, jsonString: String) {
        val fixture = json.decodeFromString<StateFixture>(jsonString)

        if (fixture.formatVersion != FORMAT_VERSION) {
            throw IllegalArgumentException(
                "Unsupported fixture format version: ${fixture.formatVersion}. " +
                    "Expected: $FORMAT_VERSION. Re-record the fixture with the current app version.",
            )
        }

        val db = ServiceLocator.provideDatabase()

        runBlocking {
            // Clear existing data in FK-safe order
            db.messageDao().deleteAllMessages()
            db.conversationDao().deleteAllConversations()
            db.mealDao().deleteAllComponents()
            db.mealDao().deleteAllMeals()
            db.userSettingsDao().deleteAll()

            // Restore user settings (API key is handled by UserSettingsRepository)
            fixture.data.userSettings?.let { settingsData ->
                val entity = mapToUserSettingsEntity(settingsData)
                ServiceLocator.provideUserSettingsRepository().saveSettings(entity)
            }

            // Restore meals with components
            fixture.data.meals.forEach { mealWithComponents ->
                val mealEntity = mapToMealEntity(mealWithComponents.meal)
                val mealId = db.mealDao().insertMeal(mealEntity)

                if (mealWithComponents.components.isNotEmpty()) {
                    val componentEntities = mealWithComponents.components.map { component ->
                        mapToMealComponentEntity(component, mealId)
                    }
                    db.mealDao().insertComponents(componentEntities)
                }
            }

            // Restore conversations with messages
            fixture.data.conversations.forEach { conversationWithMessages ->
                val conversationEntity = mapToConversationEntity(conversationWithMessages.conversation)
                val conversationId = db.conversationDao().insertConversation(conversationEntity)

                conversationWithMessages.messages.forEach { message ->
                    val messageEntity = mapToMessageEntity(message, conversationId)
                    db.messageDao().insertMessage(messageEntity)
                }
            }
        }
    }

    // ── Mapping helpers ──────────────────────────────────────────────────

    private fun mapToUserSettingsData(entity: UserSettingsEntity) = UserSettingsData(
        id = entity.id,
        weightKg = entity.weightKg,
        heightCm = entity.heightCm,
        age = entity.age,
        gender = entity.gender,
        targetWeightChangeKg = entity.targetWeightChangeKg,
        activityLevel = entity.activityLevel,
        calorieDistribution = entity.calorieDistribution,
        customProteinPercent = entity.customProteinPercent,
        customFatPercent = entity.customFatPercent,
        customCarbPercent = entity.customCarbPercent,
        openAiApiKey = entity.openAiApiKey,
        customModeEnabled = entity.customModeEnabled,
        hideCaloriesEnabled = entity.hideCaloriesEnabled,
        hideBudgetExceededEnabled = entity.hideBudgetExceededEnabled,
        customProteinGrams = entity.customProteinGrams,
        customFatGrams = entity.customFatGrams,
        customCarbGrams = entity.customCarbGrams,
    )

    private fun mapToUserSettingsEntity(data: UserSettingsData) = UserSettingsEntity(
        id = 1,
        weightKg = data.weightKg,
        heightCm = data.heightCm,
        age = data.age,
        gender = data.gender,
        targetWeightChangeKg = data.targetWeightChangeKg,
        activityLevel = data.activityLevel,
        calorieDistribution = data.calorieDistribution,
        customProteinPercent = data.customProteinPercent,
        customFatPercent = data.customFatPercent,
        customCarbPercent = data.customCarbPercent,
        openAiApiKey = data.openAiApiKey,
        customModeEnabled = data.customModeEnabled,
        hideCaloriesEnabled = data.hideCaloriesEnabled,
        hideBudgetExceededEnabled = data.hideBudgetExceededEnabled,
        customProteinGrams = data.customProteinGrams,
        customFatGrams = data.customFatGrams,
        customCarbGrams = data.customCarbGrams,
    )

    private fun mapToMealWithComponentsData(
        mealWithComponents: pro.trousev.mealcontrol.data.local.entity.MealWithComponents,
    ) = MealWithComponentsData(
        meal = MealData(
            id = mealWithComponents.meal.id,
            photoUri = mealWithComponents.meal.photoUri,
            description = mealWithComponents.meal.description,
            timestamp = mealWithComponents.meal.timestamp,
        ),
        components = mealWithComponents.components.map { component ->
            MealComponentData(
                id = component.id,
                mealId = component.mealId,
                name = component.name,
                weightGrams = component.weightGrams,
                calories = component.calories,
                proteinGrams = component.proteinGrams,
                fatGrams = component.fatGrams,
                carbGrams = component.carbGrams,
            )
        },
    )

    private fun mapToConversationWithMessagesData(
        conversationWithMessages: pro.trousev.mealcontrol.data.local.entity.ConversationWithMessages,
    ) = ConversationWithMessagesData(
        conversation = ConversationData(
            id = conversationWithMessages.conversation.id,
            title = conversationWithMessages.conversation.title,
            createdAt = conversationWithMessages.conversation.createdAt,
            isMealDetection = conversationWithMessages.conversation.isMealDetection,
        ),
        messages = conversationWithMessages.messages.map { message ->
            MessageData(
                id = message.id,
                conversationId = message.conversationId,
                content = message.content,
                isFromUser = message.isFromUser,
                timestamp = message.timestamp,
            )
        },
    )

    private fun mapToMealEntity(data: MealData) = MealEntity(
        id = data.id,
        photoUri = data.photoUri,
        description = data.description,
        timestamp = data.timestamp,
    )

    private fun mapToMealComponentEntity(
        data: MealComponentData,
        mealId: Long,
    ) = MealComponentEntity(
        id = data.id,
        mealId = mealId,
        name = data.name,
        weightGrams = data.weightGrams,
        calories = data.calories,
        proteinGrams = data.proteinGrams,
        fatGrams = data.fatGrams,
        carbGrams = data.carbGrams,
    )

    private fun mapToConversationEntity(data: ConversationData) = ConversationEntity(
        id = data.id,
        title = data.title,
        createdAt = data.createdAt,
        isMealDetection = data.isMealDetection,
    )

    private fun mapToMessageEntity(
        data: MessageData,
        conversationId: Long,
    ) = MessageEntity(
        id = data.id,
        conversationId = conversationId,
        content = data.content,
        isFromUser = data.isFromUser,
        timestamp = data.timestamp,
    )
}
