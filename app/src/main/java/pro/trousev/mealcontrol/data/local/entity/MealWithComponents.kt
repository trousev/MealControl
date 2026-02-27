package pro.trousev.mealcontrol.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MealWithComponents(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val components: List<MealComponentEntity>
)
