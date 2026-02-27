package pro.trousev.mealcontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Long = 1,
    val weightKg: Float = 0f,
    val heightCm: Float = 0f,
    val age: Int = 0,
    val gender: String = "MALE",
    val targetWeightChangeKg: Float = 0f,
    val activityLevel: Int = 1,
    val calorieDistribution: String = "HIGH_PROTEIN",
    val customProteinPercent: Int = 40,
    val customFatPercent: Int = 30,
    val customCarbPercent: Int = 30
)
