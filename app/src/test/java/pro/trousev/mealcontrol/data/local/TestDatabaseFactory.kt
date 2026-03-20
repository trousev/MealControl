package pro.trousev.mealcontrol.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import pro.trousev.mealcontrol.data.local.dao.ConversationDao
import pro.trousev.mealcontrol.data.local.dao.MealDao
import pro.trousev.mealcontrol.data.local.dao.MessageDao
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MessageEntity

object TestDatabaseFactory {
    fun createInMemory(context: Context): MealControlDatabase =
        Room
            .inMemoryDatabaseBuilder(
                context.applicationContext,
                MealControlDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    fun createInMemoryWithFallback(context: Context): MealControlDatabase =
        Room
            .databaseBuilder(
                context.applicationContext,
                MealControlDatabase::class.java,
                "test_db_${System.nanoTime()}",
            ).fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
}
