package pro.trousev.mealcontrol.ui.scanmeal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents
import pro.trousev.mealcontrol.data.remote.MealComponentDto
import pro.trousev.mealcontrol.viewmodel.MealDetectionMessage
import pro.trousev.mealcontrol.viewmodel.MealDetectionViewModel
import java.io.File

@Composable
fun MealEditScreen(
    photoUri: String,
    existingMeal: MealWithComponents? = null,
    onUpdate: (String, List<Triple<String, Double, List<Number>>>) -> Unit,
    onDelete: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MealDetectionViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    val isEditMode = existingMeal != null

    LaunchedEffect(existingMeal, photoUri) {
        if (isEditMode && existingMeal != null) {
            val components = existingMeal.components.map { component ->
                MealComponentDto(
                    name = component.name,
                    weightG = component.weightGrams.toDouble(),
                    energyKcal = component.calories.toDouble(),
                    proteinG = component.proteinGrams.toDouble(),
                    fatG = component.fatGrams.toDouble(),
                    carbsG = component.carbGrams.toDouble()
                )
            }
            viewModel.initializeForEdit(photoUri, existingMeal.meal.description, components)
        } else if (!isEditMode && photoUri.isNotEmpty()) {
            viewModel.retake()
            viewModel.initializeWithPhoto(photoUri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        if (state.isLoading && state.messages.isEmpty() && !isEditMode) {
            InitialLoadingState(
                photoUri = photoUri,
                modifier = Modifier.weight(1f)
            )
        } else {
            ChatContent(
                photoUri = photoUri,
                messages = state.messages,
                currentComponents = state.currentComponents,
                currentQuestion = state.currentQuestion,
                mealName = state.mealName,
                modifier = Modifier.weight(1f)
            )
        }

        if (state.error != null) {
            ErrorDisplay(
                error = state.error!!,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        ChatInput(
            onSend = { text -> viewModel.sendFollowUp(text) },
            enabled = !state.isLoading,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (isEditMode) {
            EditModeButtons(
                currentComponents = state.currentComponents,
                isLoading = state.isLoading,
                onUpdate = {
                    val components = state.currentComponents
                    val name = state.mealName ?: existingMeal?.meal?.description ?: "Edited Meal"
                    if (components != null) {
                        val mealComponents = components.map {
                            Triple(
                                it.name,
                                it.weightG,
                                listOf(it.energyKcal, it.proteinG, it.fatG, it.carbsG)
                            )
                        }
                        onUpdate(name, mealComponents)
                    }
                },
                onDelete = onDelete,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            NewMealButtons(
                mealName = state.mealName,
                currentComponents = state.currentComponents,
                isLoading = state.isLoading,
                onSubmit = {
                    val components = state.currentComponents
                    val name = state.mealName ?: "Detected Meal"
                    if (components != null) {
                        val mealComponents = components.map {
                            Triple(
                                it.name,
                                it.weightG,
                                listOf(it.energyKcal, it.proteinG, it.fatG, it.carbsG)
                            )
                        }
                        onUpdate(name, mealComponents)
                    }
                },
                onRetake = {
                    viewModel.retake()
                    onRetake()
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun InitialLoadingState(
    photoUri: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(File(photoUri)),
            contentDescription = "Captured meal",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Analyzing meal...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ChatContent(
    photoUri: String,
    messages: List<MealDetectionMessage>,
    currentComponents: List<MealComponentDto>?,
    currentQuestion: String?,
    mealName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = rememberAsyncImagePainter(File(photoUri)),
            contentDescription = "Captured meal",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        messages.forEach { message ->
            ChatMessageItem(
                message = message,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (currentComponents != null && currentComponents.isNotEmpty()) {
            if (mealName != null) {
                Text(
                    text = mealName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            MealComponentsList(
                components = currentComponents,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (currentQuestion != null) {
            QuestionDisplay(
                question = currentQuestion,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: MealDetectionMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (message.isFromUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MealComponentsList(
    components: List<MealComponentDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detected Components:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            components.forEach { component ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${component.weightG.toInt()}g of ${component.name}")
                        Text(
                            text = "${component.proteinG.toInt()}g prot, ${component.fatG.toInt()}g fat, ${component.carbsG.toInt()}g carb",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${component.energyKcal.toInt()}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "KCAL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val totalCalories = components.sumOf { it.energyKcal }
            val totalProtein = components.sumOf { it.proteinG }
            val totalFat = components.sumOf { it.fatG }
            val totalCarbs = components.sumOf { it.carbsG }

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
}

@Composable
private fun QuestionDisplay(
    question: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Clarification needed:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun ErrorDisplay(
    error: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ChatInput(
    onSend: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type your answer...") },
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier
                .background(
                    color = if (enabled && text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun EditModeButtons(
    currentComponents: List<MealComponentDto>?,
    isLoading: Boolean,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSubmit = currentComponents != null && currentComponents.isNotEmpty() && !isLoading

    Column(modifier = modifier) {
        Button(
            onClick = onUpdate,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text("Update")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Delete")
        }
    }
}

@Composable
private fun NewMealButtons(
    mealName: String?,
    currentComponents: List<MealComponentDto>?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSubmit = currentComponents != null && currentComponents.isNotEmpty() && !isLoading

    Column(modifier = modifier) {
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Retake")
        }
    }
}
