package pro.trousev.mealcontrol

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Meals : Screen("meals")
    data object ScanMeal : Screen("scan_meal")
    data object MealEdit : Screen("meal_edit/{mealId}") {
        fun createRoute(mealId: Long?) = "meal_edit/${mealId ?: -1}"
        const val MEAL_ID_ARG = "mealId"
    }
    data object ConversationsList : Screen("conversations")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: Long) = "chat/$conversationId"
        const val CONVERSATION_ID_ARG = "conversationId"
    }
    data object Settings : Screen("settings")
}

enum class AppTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    MEALS(Screen.Meals.route, "Meals", Icons.AutoMirrored.Filled.List),
    CHAT(Screen.ConversationsList.route, "Chat", Icons.Filled.Email),
    SETTINGS(Screen.Settings.route, "Settings", Icons.Filled.Settings),
}

fun AppTab.toScreen(): Screen = when (this) {
    AppTab.MEALS -> Screen.Meals
    AppTab.CHAT -> Screen.ConversationsList
    AppTab.SETTINGS -> Screen.Settings
}
