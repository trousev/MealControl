package pro.trousev.mealcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.trousev.mealcontrol.ui.chat.ChatScreen
import pro.trousev.mealcontrol.ui.chat.ConversationsListScreen
import pro.trousev.mealcontrol.ui.meals.MealsScreen
import pro.trousev.mealcontrol.ui.scanmeal.CapturedPhotoScreen
import pro.trousev.mealcontrol.ui.scanmeal.ScanMealScreen
import pro.trousev.mealcontrol.ui.settings.SettingsScreen
import pro.trousev.mealcontrol.ui.theme.MealControlTheme
import pro.trousev.mealcontrol.viewmodel.ChatViewModel
import pro.trousev.mealcontrol.viewmodel.MealViewModel
import pro.trousev.mealcontrol.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MealControlTheme {
                MealControlApp()
            }
        }
    }
}

@Composable
fun MealControlApp() {
    var currentTab by rememberSaveable { mutableStateOf(AppTab.SCAN_MEAL) }
    var capturedPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedConversationId by rememberSaveable { mutableLongStateOf(-1L) }

    val mealViewModel: MealViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        selected = tab == currentTab,
                        onClick = {
                            currentTab = tab
                            capturedPhotoUri = null
                            selectedConversationId = -1L
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.SCAN_MEAL -> {
                    if (capturedPhotoUri != null) {
                        CapturedPhotoScreen(
                            photoUri = capturedPhotoUri!!,
                            onSubmit = { description, components ->
                                mealViewModel.saveMeal(
                                    photoUri = capturedPhotoUri!!,
                                    description = description,
                                    components = components
                                )
                                capturedPhotoUri = null
                                currentTab = AppTab.MEALS
                            },
                            onRetake = { capturedPhotoUri = null }
                        )
                    } else {
                        ScanMealScreen(
                            onPhotoCaptured = { uri ->
                                capturedPhotoUri = uri
                            }
                        )
                    }
                }

                AppTab.MEALS -> {
                    MealsScreen(
                        viewModel = mealViewModel
                    )
                }

                AppTab.CHAT -> {
                    if (selectedConversationId > 0) {
                        ChatScreen(
                            conversationId = selectedConversationId,
                            viewModel = chatViewModel,
                            onBackClick = { selectedConversationId = -1L }
                        )
                    } else {
                        ConversationsListScreen(
                            viewModel = chatViewModel,
                            onConversationClick = { conversationId ->
                                selectedConversationId = conversationId
                            }
                        )
                    }
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    SCAN_MEAL("Scan Meal", Icons.Default.Star),
    MEALS("Meals", Icons.Default.List),
    CHAT("Chat", Icons.Default.Email),
    SETTINGS("Settings", Icons.Default.Settings),
}
