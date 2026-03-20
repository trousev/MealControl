package pro.trousev.mealcontrol

import android.content.Context
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.repository.ChatRepository
import pro.trousev.mealcontrol.data.repository.MealRepository
import pro.trousev.mealcontrol.data.repository.UserSettingsRepository
import pro.trousev.mealcontrol.util.ApiKeyManager
import pro.trousev.mealcontrol.util.SecureStorage

object ServiceLocator {
    private var database: MealControlDatabase? = null
    private var mealRepository: MealRepository? = null
    private var chatRepository: ChatRepository? = null
    private var userSettingsRepository: UserSettingsRepository? = null
    private var secureStorage: SecureStorage? = null
    private var context: Context? = null

    fun initialize(appContext: Context, testSecureStorage: SecureStorage? = null) {
        context = appContext.applicationContext
        database = MealControlDatabase.getDatabase(context!!)
        secureStorage = testSecureStorage ?: ApiKeyManager(context!!)
    }

    fun provideMealRepository(): MealRepository {
        return mealRepository ?: synchronized(this) {
            mealRepository ?: MealRepository(database!!.mealDao()).also { mealRepository = it }
        }
    }

    fun provideChatRepository(): ChatRepository {
        return chatRepository ?: synchronized(this) {
            chatRepository ?: ChatRepository(
                database!!.conversationDao(),
                database!!.messageDao(),
                provideUserSettingsRepository()
            ).also { chatRepository = it }
        }
    }

    fun provideUserSettingsRepository(): UserSettingsRepository {
        return userSettingsRepository ?: synchronized(this) {
            userSettingsRepository ?: UserSettingsRepository(
                database!!.userSettingsDao(),
                secureStorage!!
            ).also { userSettingsRepository = it }
        }
    }

    fun provideDatabase(): MealControlDatabase {
        return database ?: throw IllegalStateException("ServiceLocator not initialized. Call initialize() first.")
    }

    fun resetForTesting() {
        database = null
        mealRepository = null
        chatRepository = null
        userSettingsRepository = null
        secureStorage = null
        context = null
    }
}