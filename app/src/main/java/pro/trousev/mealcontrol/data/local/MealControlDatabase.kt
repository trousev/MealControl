package pro.trousev.mealcontrol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pro.trousev.mealcontrol.data.local.dao.ConversationDao
import pro.trousev.mealcontrol.data.local.dao.MealDao
import pro.trousev.mealcontrol.data.local.dao.MessageDao
import pro.trousev.mealcontrol.data.local.dao.UserSettingsDao
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        MealEntity::class,
        MealComponentEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        UserSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MealControlDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MealControlDatabase? = null

        fun getDatabase(context: Context): MealControlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MealControlDatabase::class.java,
                    "meal_control_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
