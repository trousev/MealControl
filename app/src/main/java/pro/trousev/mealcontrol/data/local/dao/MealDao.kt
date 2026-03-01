package pro.trousev.mealcontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    suspend fun getAllMealsWithComponents(): List<MealWithComponents>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithComponents(mealId: Long): MealWithComponents?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<MealComponentEntity>)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMeal(mealId: Long)

    @Query("UPDATE meals SET description = :description, timestamp = :timestamp WHERE id = :mealId")
    suspend fun updateMeal(mealId: Long, description: String, timestamp: Long)

    @Query("DELETE FROM meal_components WHERE mealId = :mealId")
    suspend fun deleteComponentsByMealId(mealId: Long)
}
