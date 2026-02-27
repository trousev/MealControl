package pro.trousev.mealcontrol.data.local

import android.content.Context
import androidx.room.Room
import pro.trousev.mealcontrol.data.local.dao.ConversationDao
import pro.trousev.mealcontrol.data.local.dao.MealDao
import pro.trousev.mealcontrol.data.local.dao.MessageDao
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import androidx.room.RoomDatabase

object TestDatabaseFactory {

    fun createInMemory(context: Context): MealControlDatabase {
        return Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            MealControlDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    fun createInMemoryWithFallback(context: Context): MealControlDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            MealControlDatabase::class.java,
            "test_db_${System.nanoTime()}"
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }
}
