package pro.trousev.mealcontrol

import android.content.Context
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.repository.ChatRepository
import pro.trousev.mealcontrol.data.repository.MealRepository
import pro.trousev.mealcontrol.data.repository.UserSettingsRepository

object ServiceLocator {
    private var database: MealControlDatabase? = null
    private var mealRepository: MealRepository? = null
    private var chatRepository: ChatRepository? = null
    private var userSettingsRepository: UserSettingsRepository? = null

    fun initialize(context: Context) {
        database = MealControlDatabase.getDatabase(context.applicationContext)
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
                database!!.userSettingsDao()
            ).also { chatRepository = it }
        }
    }

    fun provideUserSettingsRepository(): UserSettingsRepository {
        return userSettingsRepository ?: synchronized(this) {
            userSettingsRepository ?: UserSettingsRepository(database!!.userSettingsDao())
                .also { userSettingsRepository = it }
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
    }
}