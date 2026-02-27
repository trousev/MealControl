package pro.trousev.mealcontrol.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.trousev.mealcontrol.viewmodel.ActivityLevel
import pro.trousev.mealcontrol.viewmodel.CalorieCalculation
import pro.trousev.mealcontrol.viewmodel.CalorieDistribution
import pro.trousev.mealcontrol.viewmodel.Gender
import pro.trousev.mealcontrol.viewmodel.SettingsFormState
import pro.trousev.mealcontrol.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val settingsLoaded by viewModel.settingsLoaded.collectAsState()

    if (!settingsLoaded) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Body Measurements",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = formState.weightKg,
            onValueChange = { viewModel.updateWeight(it) },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("30-300 kg") },
            isError = formState.weightKg.isNotEmpty() && (formState.weightKg.toFloatOrNull()?.let { it < 30 || it > 300 } ?: true)
        )

        OutlinedTextField(
            value = formState.heightCm,
            onValueChange = { viewModel.updateHeight(it) },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("100-250 cm") },
            isError = formState.heightCm.isNotEmpty() && (formState.heightCm.toFloatOrNull()?.let { it < 100 || it > 250 } ?: true)
        )

        OutlinedTextField(
            value = formState.age,
            onValueChange = { viewModel.updateAge(it) },
            label = { Text("Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("10-120 years") },
            isError = formState.age.isNotEmpty() && (formState.age.toIntOrNull()?.let { it < 10 || it > 120 } ?: true)
        )

        Text(
            text = "Gender",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Gender.entries.forEach { gender ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = formState.gender == gender,
                            onClick = { viewModel.updateGender(gender) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = formState.gender == gender,
                        onClick = null
                    )
                    Text(
                        text = gender.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Weight Goal",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = formState.targetWeightChangeKg,
            onValueChange = { viewModel.updateTargetWeightChange(it) },
            label = { Text("Weight change per week (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Negative to lose, positive to gain. Range: -5 to +5") },
            isError = formState.targetWeightChangeKg.isNotEmpty() && 
                     (formState.targetWeightChangeKg.toFloatOrNull()?.let { it < -5 || it > 5 } ?: true)
        )

        Text(
            text = "How active are you?",
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ActivityLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = formState.activityLevel == level,
                            onClick = { viewModel.updateActivityLevel(level) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = formState.activityLevel == level,
                        onClick = null
                    )
                    Text(
                        text = level.description,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Calorie Distribution",
            style = MaterialTheme.typography.titleLarge
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val distributions = listOf(
                CalorieDistribution.HIGH_PROTEIN to "High Protein (40/30/30)",
                CalorieDistribution.BALANCED to "Balanced (30/35/35)",
                CalorieDistribution.LOW_FAT to "Low Fat (30/20/50)",
                CalorieDistribution.CUSTOM to "Custom"
            )
            distributions.forEach { (dist, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = formState.calorieDistribution == dist,
                            onClick = { viewModel.updateCalorieDistribution(dist) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = formState.calorieDistribution == dist,
                        onClick = null
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        if (formState.calorieDistribution == CalorieDistribution.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.customProteinPercent.toString(),
                    onValueChange = { viewModel.updateCustomProtein(it.toIntOrNull() ?: 0) },
                    label = { Text("Protein %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = formState.customDistributionError != null
                )
                OutlinedTextField(
                    value = formState.customFatPercent.toString(),
                    onValueChange = { viewModel.updateCustomFat(it.toIntOrNull() ?: 0) },
                    label = { Text("Fat %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = formState.customDistributionError != null
                )
                OutlinedTextField(
                    value = formState.customCarbPercent.toString(),
                    onValueChange = { viewModel.updateCustomCarb(it.toIntOrNull() ?: 0) },
                    label = { Text("Carbs %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = formState.customDistributionError != null
                )
            }
            if (formState.customDistributionError != null) {
                Text(
                    text = formState.customDistributionError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (formState.calculation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            ResultsCard(calculation = formState.calculation!!)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ResultsCard(
    calculation: CalorieCalculation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Daily Calorie Budget",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            ResultRow(label = "BMR (Basal Metabolic Rate)", value = "${calculation.bmr} kcal")
            ResultRow(label = "TDEE (Total Daily Energy Expenditure)", value = "${calculation.tdee} kcal")

            if (calculation.dailyDeficit != 0) {
                val deficitLabel = if (calculation.dailyDeficit < 0) "Daily Deficit" else "Daily Surplus"
                ResultRow(label = deficitLabel, value = "${kotlin.math.abs(calculation.dailyDeficit)} kcal")
            }

            HorizontalDivider()

            Text(
                text = "${calculation.dailyCalories} kcal/day",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            Text(
                text = "Macronutrients",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem(label = "Protein", grams = calculation.proteinGrams)
                MacroItem(label = "Fat", grams = calculation.fatGrams)
                MacroItem(label = "Carbs", grams = calculation.carbGrams)
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroItem(
    label: String,
    grams: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${grams}g",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
