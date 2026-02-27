package pro.trousev.mealcontrol.ui.scanmeal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

data class MealComponent(
    val name: String,
    val calories: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbGrams: Int
)

@Composable
fun CapturedPhotoScreen(
    photoUri: String,
    onSubmit: (String, List<Pair<String, List<Int>>>) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hardcodedDescription = "Grilled Chicken Salad"
    val hardcodedComponents = listOf(
        MealComponent("Grilled Chicken", 250, 45, 8, 0),
        MealComponent("Mixed Greens", 30, 2, 0, 6),
        MealComponent("Cherry Tomatoes", 20, 1, 0, 4),
        MealComponent("Olive Oil Dressing", 150, 0, 14, 0),
        MealComponent("Avocado", 80, 1, 7, 4)
    )
    val totalCalories = hardcodedComponents.sumOf { it.calories }
    val totalProtein = hardcodedComponents.sumOf { it.proteinGrams }
    val totalFat = hardcodedComponents.sumOf { it.fatGrams }
    val totalCarbs = hardcodedComponents.sumOf { it.carbGrams }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(File(photoUri)),
            contentDescription = "Captured meal",
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = hardcodedDescription,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Components:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                hardcodedComponents.forEach { component ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = component.name)
                        Text(text = "${component.calories} kcal (P:${component.proteinGrams}g F:${component.fatGrams}g C:${component.carbGrams}g)")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$totalCalories kcal (P:${totalProtein}g F:${totalFat}g C:${totalCarbs}g)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSubmit(hardcodedDescription, hardcodedComponents.map { it.name to listOf(it.calories, it.proteinGrams, it.fatGrams, it.carbGrams) }) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retake")
        }
    }
}
