package pro.trousev.mealcontrol.data.repository

import pro.trousev.mealcontrol.data.local.dao.MealDao
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealEntity
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents

class MealRepository(private val mealDao: MealDao) {

    suspend fun getAllMeals(): List<MealWithComponents> {
        return mealDao.getAllMealsWithComponents()
    }

    suspend fun getMealById(mealId: Long): MealWithComponents? {
        return mealDao.getMealWithComponents(mealId)
    }

    suspend fun saveMeal(
        photoUri: String,
        description: String,
        components: List<Pair<String, Int>>
    ): Long {
        val timestamp = System.currentTimeMillis()
        val meal = MealEntity(
            photoUri = photoUri,
            description = description,
            timestamp = timestamp
        )
        val mealId = mealDao.insertMeal(meal)

        val componentEntities = components.map { (name, calories) ->
            MealComponentEntity(
                mealId = mealId,
                name = name,
                calories = calories
            )
        }
        mealDao.insertComponents(componentEntities)

        return mealId
    }

    suspend fun deleteMeal(mealId: Long) {
        mealDao.deleteMeal(mealId)
    }
}
