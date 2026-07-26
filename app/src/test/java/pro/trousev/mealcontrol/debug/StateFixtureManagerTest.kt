package pro.trousev.mealcontrol.debug

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity
import pro.trousev.mealcontrol.util.SecureStorage

@RunWith(RobolectricTestRunner::class)
class StateFixtureManagerTest {
    private lateinit var database: MealControlDatabase
    private var storedApiKey: String = ""
    private var retrievedApiKeyCount: Int = 0

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication().applicationContext,
                    MealControlDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        storedApiKey = ""
        retrievedApiKeyCount = 0
        val mockSecureStorage =
            object : SecureStorage {
                override fun storeApiKey(apiKey: String) {
                    storedApiKey = apiKey
                }

                override fun retrieveApiKey(): String {
                    retrievedApiKeyCount++
                    return storedApiKey
                }
            }
        ServiceLocator.initialize(
            RuntimeEnvironment.getApplication(),
            mockSecureStorage,
            database,
        )
    }

    @After
    fun tearDown() {
        database.close()
        ServiceLocator.resetForTesting()
    }

    @Test
    fun dumpState_emptyDatabase_producesValidJson() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val json = StateFixtureManager.dumpState(context)

        assertNotNull(json)
        assertTrue(json.contains("\"formatVersion\": 1"))
        assertTrue(json.contains("\"userSettings\": null"))
        assertTrue(json.contains("\"meals\": []"))
        assertTrue(json.contains("\"conversations\": []"))
    }

    @Test
    fun dumpState_withUserSettings_includesAllFields() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        storedApiKey = "sk-test-api-key-12345"

        runBlocking {
            database.userSettingsDao().saveSettings(
                UserSettingsEntity(
                    weightKg = 120f,
                    heightCm = 180f,
                    age = 35,
                    gender = "MALE",
                    targetWeightChangeKg = -0.5f,
                    activityLevel = 2,
                    calorieDistribution = "HIGH_PROTEIN",
                    openAiApiKey = storedApiKey,
                ),
            )
        }

        val json = StateFixtureManager.dumpState(context)

        assertTrue(json.contains("\"weightKg\": 120.0"))
        assertTrue(json.contains("\"heightCm\": 180.0"))
        assertTrue(json.contains("\"age\": 35"))
        assertTrue(json.contains("\"gender\": \"MALE\""))
        assertTrue(json.contains("\"targetWeightChangeKg\": -0.5"))
        assertTrue(json.contains("\"activityLevel\": 2"))
        assertTrue(json.contains("\"calorieDistribution\": \"HIGH_PROTEIN\""))
        assertTrue(json.contains("\"apiKey\": \"sk-test-api-key-12345\""))
    }

    @Test
    fun dumpState_withMeals_includesMealsWithComponents() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        runBlocking {
            val mealId =
                database.mealDao().insertMeal(
                    MealEntity(
                        photoUri = "content://test/photo1",
                        description = "Chicken salad",
                        timestamp = 1000000L,
                    ),
                )
            database.mealDao().insertComponents(
                listOf(
                    MealComponentEntity(
                        mealId = mealId,
                        name = "Chicken breast",
                        weightGrams = 200,
                        calories = 330,
                        proteinGrams = 62,
                        fatGrams = 7,
                        carbGrams = 0,
                    ),
                ),
            )
        }

        val json = StateFixtureManager.dumpState(context)

        assertTrue(json.contains("\"photoUri\": \"content://test/photo1\""))
        assertTrue(json.contains("\"description\": \"Chicken salad\""))
        assertTrue(json.contains("\"name\": \"Chicken breast\""))
        assertTrue(json.contains("\"calories\": 330"))
        assertTrue(json.contains("\"proteinGrams\": 62"))
        assertTrue(json.contains("\"components\""))
    }

    @Test
    fun dumpState_withConversations_includesConversationsWithMessages() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        runBlocking {
            val conversationId =
                database.conversationDao().insertConversation(
                    ConversationEntity(
                        title = "Diet advice",
                        createdAt = 2000000L,
                        isMealDetection = false,
                    ),
                )
            database.messageDao().insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    content = "What should I eat?",
                    isFromUser = true,
                    timestamp = 2001000L,
                ),
            )
            database.messageDao().insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    content = "Try a salad!",
                    isFromUser = false,
                    timestamp = 2002000L,
                ),
            )
        }

        val json = StateFixtureManager.dumpState(context)

        assertTrue(json.contains("\"title\": \"Diet advice\""))
        assertTrue(json.contains("\"content\": \"What should I eat?\""))
        assertTrue(json.contains("\"content\": \"Try a salad!\""))
        assertTrue(json.contains("\"isFromUser\": true"))
        assertTrue(json.contains("\"isFromUser\": false"))
    }

    @Test
    fun restoreState_emptyFixture_clearsExistingData() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        runBlocking {
            database.userSettingsDao().saveSettings(UserSettingsEntity(weightKg = 80f))
        }

        val emptyFixture =
            """
            {
              "formatVersion": 1,
              "data": {
                "meals": [],
                "conversations": []
              },
              "secureStorage": {
                "apiKey": ""
              }
            }
            """.trimIndent()

        StateFixtureManager.restoreState(context, emptyFixture)

        runBlocking {
            val remainingSettings = database.userSettingsDao().getSettings()
            assertEquals(null, remainingSettings)
        }
    }

    @Test
    fun restoreState_withUserSettings_populatesSettings() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        val fixture =
            """
            {
              "formatVersion": 1,
              "data": {
                "userSettings": {
                  "id": 1,
                  "weightKg": 120.0,
                  "heightCm": 180.0,
                  "age": 35,
                  "gender": "MALE",
                  "targetWeightChangeKg": -0.5,
                  "activityLevel": 2,
                  "calorieDistribution": "HIGH_PROTEIN",
                  "customProteinPercent": 40,
                  "customFatPercent": 30,
                  "customCarbPercent": 30,
                  "openAiApiKey": "sk-restored-key",
                  "customModeEnabled": false,
                  "hideCaloriesEnabled": false,
                  "hideBudgetExceededEnabled": false,
                  "customProteinGrams": 0,
                  "customFatGrams": 0,
                  "customCarbGrams": 0
                },
                "meals": [],
                "conversations": []
              },
              "secureStorage": {
                "apiKey": "sk-restored-key"
              }
            }
            """.trimIndent()

        StateFixtureManager.restoreState(context, fixture)

        runBlocking {
            val savedSettings = database.userSettingsDao().getSettings()
            assertNotNull(savedSettings)
            assertEquals(120f, savedSettings!!.weightKg)
            assertEquals(180f, savedSettings.heightCm)
            assertEquals(35, savedSettings.age)
            assertEquals("MALE", savedSettings.gender)
            assertEquals(-0.5f, savedSettings.targetWeightChangeKg)
            assertEquals(2, savedSettings.activityLevel)
            assertEquals("HIGH_PROTEIN", savedSettings.calorieDistribution)
            assertEquals("", savedSettings.openAiApiKey)
        }

        assertEquals("sk-restored-key", storedApiKey)
    }

    @Test
    fun restoreState_withMeals_populatesMealsAndComponents() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        val fixture =
            """
            {
              "formatVersion": 1,
              "data": {
                "meals": [
                  {
                    "meal": {
                      "id": 1,
                      "photoUri": "content://test/meal1",
                      "description": "Test meal",
                      "timestamp": 5000000
                    },
                    "components": [
                      {
                        "id": 1,
                        "mealId": 1,
                        "name": "Test component",
                        "weightGrams": 100,
                        "calories": 200,
                        "proteinGrams": 15,
                        "fatGrams": 10,
                        "carbGrams": 5
                      }
                    ]
                  }
                ],
                "conversations": []
              },
              "secureStorage": {
                "apiKey": ""
              }
            }
            """.trimIndent()

        StateFixtureManager.restoreState(context, fixture)

        runBlocking {
            val meals = database.mealDao().getAllMealsWithComponents()
            assertEquals(1, meals.size)
            assertEquals("content://test/meal1", meals[0].meal.photoUri)
            assertEquals("Test meal", meals[0].meal.description)
            assertEquals(5000000L, meals[0].meal.timestamp)

            assertEquals(1, meals[0].components.size)
            assertEquals("Test component", meals[0].components[0].name)
            assertEquals(100, meals[0].components[0].weightGrams)
            assertEquals(200, meals[0].components[0].calories)
            assertEquals(15, meals[0].components[0].proteinGrams)
            assertEquals(10, meals[0].components[0].fatGrams)
            assertEquals(5, meals[0].components[0].carbGrams)
        }
    }

    @Test
    fun restoreState_withConversations_populatesConversationsAndMessages() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        val fixture =
            """
            {
              "formatVersion": 1,
              "data": {
                "meals": [],
                "conversations": [
                  {
                    "conversation": {
                      "id": 1,
                      "title": "Test chat",
                      "createdAt": 7000000,
                      "isMealDetection": false
                    },
                    "messages": [
                      {
                        "id": 1,
                        "conversationId": 1,
                        "content": "Hello",
                        "isFromUser": true,
                        "timestamp": 7001000
                      },
                      {
                        "id": 2,
                        "conversationId": 1,
                        "content": "Hi there!",
                        "isFromUser": false,
                        "timestamp": 7002000
                      }
                    ]
                  }
                ]
              },
              "secureStorage": {
                "apiKey": ""
              }
            }
            """.trimIndent()

        StateFixtureManager.restoreState(context, fixture)

        runBlocking {
            val conversations = database.conversationDao().getAllConversations()
            assertEquals(1, conversations.size)
            assertEquals("Test chat", conversations[0].title)
            assertEquals(7000000L, conversations[0].createdAt)

            val messages = database.messageDao().getAllMessages()
            assertEquals(2, messages.size)
            assertEquals("Hello", messages[0].content)
            assertEquals(true, messages[0].isFromUser)
            assertEquals("Hi there!", messages[1].content)
            assertEquals(false, messages[1].isFromUser)
        }
    }

    @Test
    fun roundtrip_exportThenImport_preservesAllData() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        storedApiKey = "sk-roundtrip-key"

        runBlocking {
            database.userSettingsDao().saveSettings(
                UserSettingsEntity(
                    weightKg = 90f,
                    heightCm = 170f,
                    age = 28,
                    gender = "FEMALE",
                    targetWeightChangeKg = 0.3f,
                    activityLevel = 3,
                    calorieDistribution = "BALANCED",
                    openAiApiKey = storedApiKey,
                    customModeEnabled = true,
                    hideCaloriesEnabled = false,
                    hideBudgetExceededEnabled = true,
                ),
            )

            val mealId =
                database.mealDao().insertMeal(
                    MealEntity(
                        photoUri = "uri1",
                        description = "Meal 1",
                        timestamp = 1000L,
                    ),
                )
            database.mealDao().insertComponents(
                listOf(
                    MealComponentEntity(mealId = mealId, name = "Comp 1", calories = 100),
                    MealComponentEntity(mealId = mealId, name = "Comp 2", calories = 200),
                ),
            )

            val convId =
                database.conversationDao().insertConversation(
                    ConversationEntity(title = "Conv 1", createdAt = 2000L),
                )
            database.messageDao().insertMessage(
                MessageEntity(
                    conversationId = convId,
                    content = "Msg 1",
                    isFromUser = true,
                    timestamp = 3000L,
                ),
            )
        }

        // Dump
        val json = StateFixtureManager.dumpState(context)

        // Clear database
        runBlocking {
            database.messageDao().deleteAllMessages()
            database.conversationDao().deleteAllConversations()
            database.mealDao().deleteAllComponents()
            database.mealDao().deleteAllMeals()
            database.userSettingsDao().deleteAll()
        }
        storedApiKey = ""

        // Restore
        StateFixtureManager.restoreState(context, json)

        // Verify user settings
        runBlocking {
            val restoredSettings = database.userSettingsDao().getSettings()
            assertNotNull(restoredSettings)
            assertEquals(90f, restoredSettings!!.weightKg)
            assertEquals(170f, restoredSettings.heightCm)
            assertEquals(28, restoredSettings.age)
            assertEquals("FEMALE", restoredSettings.gender)
            assertEquals(true, restoredSettings.customModeEnabled)
            assertEquals(true, restoredSettings.hideBudgetExceededEnabled)
        }

        // Verify API key
        assertEquals("sk-roundtrip-key", storedApiKey)

        // Verify meals
        runBlocking {
            val restoredMeals = database.mealDao().getAllMealsWithComponents()
            assertEquals(1, restoredMeals.size)
            assertEquals("Meal 1", restoredMeals[0].meal.description)
            assertEquals(2, restoredMeals[0].components.size)

            // Verify conversations
            val restoredConversations = database.conversationDao().getAllConversations()
            assertEquals(1, restoredConversations.size)
            assertEquals("Conv 1", restoredConversations[0].title)

            // Verify messages
            val restoredMessages = database.messageDao().getAllMessages()
            assertEquals(1, restoredMessages.size)
            assertEquals("Msg 1", restoredMessages[0].content)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreState_unsupportedFormatVersion_throwsException() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        val fixture =
            """
            {
              "formatVersion": 999,
              "data": {
                "meals": [],
                "conversations": []
              },
              "secureStorage": {
                "apiKey": ""
              }
            }
            """.trimIndent()

        StateFixtureManager.restoreState(context, fixture)
    }

    @Test
    fun restoreState_clearsExistingDataBeforeImport() {
        val context = RuntimeEnvironment.getApplication().applicationContext

        runBlocking {
            database.userSettingsDao().saveSettings(UserSettingsEntity(weightKg = 50f))
            database.mealDao().insertMeal(
                MealEntity(
                    photoUri = "old",
                    description = "Old meal",
                    timestamp = 1L,
                ),
            )
        }

        val fixture =
            """
            {
              "formatVersion": 1,
              "data": {
                "userSettings": {
                  "id": 1,
                  "weightKg": 100.0,
                  "heightCm": 180.0,
                  "age": 30,
                  "gender": "MALE",
                  "targetWeightChangeKg": 0.0,
                  "activityLevel": 1,
                  "calorieDistribution": "HIGH_PROTEIN",
                  "customProteinPercent": 40,
                  "customFatPercent": 30,
                  "customCarbPercent": 30,
                  "openAiApiKey": "",
                  "customModeEnabled": false,
                  "hideCaloriesEnabled": false,
                  "hideBudgetExceededEnabled": false,
                  "customProteinGrams": 0,
                  "customFatGrams": 0,
                  "customCarbGrams": 0
                },
                "meals": [],
                "conversations": []
              },
              "secureStorage": {
                "apiKey": ""
              }
            }
            """.trimIndent()

        StateFixtureManager.restoreState(context, fixture)

        runBlocking {
            val settings = database.userSettingsDao().getSettings()
            assertEquals(100f, settings!!.weightKg)

            val meals = database.mealDao().getAllMealsWithComponents()
            assertEquals(0, meals.size)
        }
    }
}
