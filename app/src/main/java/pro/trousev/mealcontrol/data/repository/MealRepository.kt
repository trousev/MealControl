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
        components: List<Triple<String, Double, List<Number>>>
    ): Long {
        val timestamp = System.currentTimeMillis()
        val meal = MealEntity(
            photoUri = photoUri,
            description = description,
            timestamp = timestamp
        )
        val mealId = mealDao.insertMeal(meal)

        val componentEntities = components.map { (name, weight, values) ->
            MealComponentEntity(
                mealId = mealId,
                name = name,
                weightGrams = weight.toInt(),
                calories = (values.getOrElse(0) { 0 } as? Number)?.toInt() ?: 0,
                proteinGrams = (values.getOrElse(1) { 0 } as? Number)?.toInt() ?: 0,
                fatGrams = (values.getOrElse(2) { 0 } as? Number)?.toInt() ?: 0,
                carbGrams = (values.getOrElse(3) { 0 } as? Number)?.toInt() ?: 0
            )
        }
        mealDao.insertComponents(componentEntities)

        return mealId
    }

    suspend fun updateMeal(
        mealId: Long,
        description: String,
        components: List<Triple<String, Double, List<Number>>>,
        timestamp: Long
    ) {
        mealDao.updateMeal(
            mealId = mealId,
            description = description,
            timestamp = timestamp
        )

        mealDao.deleteComponentsByMealId(mealId)

        val componentEntities = components.map { (name, weight, values) ->
            MealComponentEntity(
                mealId = mealId,
                name = name,
                weightGrams = weight.toInt(),
                calories = (values.getOrElse(0) { 0 } as? Number)?.toInt() ?: 0,
                proteinGrams = (values.getOrElse(1) { 0 } as? Number)?.toInt() ?: 0,
                fatGrams = (values.getOrElse(2) { 0 } as? Number)?.toInt() ?: 0,
                carbGrams = (values.getOrElse(3) { 0 } as? Number)?.toInt() ?: 0
            )
        }
        mealDao.insertComponents(componentEntities)
    }

    suspend fun deleteMeal(mealId: Long) {
        mealDao.deleteMeal(mealId)
    }
}
