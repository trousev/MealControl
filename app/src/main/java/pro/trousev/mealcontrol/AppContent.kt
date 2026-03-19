package pro.trousev.mealcontrol

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents
import pro.trousev.mealcontrol.ui.chat.ChatScreen
import pro.trousev.mealcontrol.ui.chat.ConversationsListScreen
import pro.trousev.mealcontrol.ui.meals.MealsScreen
import pro.trousev.mealcontrol.ui.scanmeal.MealEditScreen
import pro.trousev.mealcontrol.ui.scanmeal.ScanMealScreen
import pro.trousev.mealcontrol.ui.settings.SettingsScreen
import pro.trousev.mealcontrol.viewmodel.ChatViewModel
import pro.trousev.mealcontrol.viewmodel.MealViewModel
import pro.trousev.mealcontrol.viewmodel.SettingsViewModel

@Composable
fun AppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val context = LocalContext.current
    val mealViewModel: MealViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val selectedTab = when {
        currentRoute == Screen.Meals.route -> AppTab.MEALS
        currentRoute == Screen.ConversationsList.route || 
        currentRoute?.startsWith("chat/") == true -> AppTab.CHAT
        currentRoute == Screen.Settings.route -> AppTab.SETTINGS
        currentRoute == Screen.ScanMeal.route -> AppTab.MEALS
        currentRoute?.startsWith("meal_edit/") == true -> AppTab.MEALS
        else -> AppTab.MEALS
    }

    BackHandler(enabled = true) {
        when {
            currentRoute == Screen.Meals.route -> {
                (context as? android.app.Activity)?.finish()
            }
            else -> {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        selected = selectedTab == tab,
                        onClick = {
                            when (tab) {
                                AppTab.MEALS -> {
                                    navController.popBackStack(Screen.Meals.route, inclusive = false)
                                }
                                AppTab.CHAT -> {
                                    navController.navigate(Screen.ConversationsList.route) {
                                        popUpTo(Screen.Meals.route)
                                        launchSingleTop = true
                                    }
                                }
                                AppTab.SETTINGS -> {
                                    navController.navigate(Screen.Settings.route) {
                                        popUpTo(Screen.Meals.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
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
            AppNavHost(
                navController = navController,
                mealViewModel = mealViewModel,
                chatViewModel = chatViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    mealViewModel: MealViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Meals.route,
        modifier = modifier
    ) {
        composable(Screen.Meals.route) {
            MealsScreen(
                mealViewModel = mealViewModel,
                settingsViewModel = settingsViewModel,
                onAddMealClick = {
                    navController.navigate(Screen.ScanMeal.route)
                },
                onMealClick = { meal ->
                    navController.navigate(Screen.MealEdit.createRoute(meal.meal.id))
                }
            )
        }

        composable(Screen.ScanMeal.route) {
            ScanMealScreen(
                onPhotoCaptured = { uri ->
                    navController.navigate(Screen.MealEdit.createRoute(null)) {
                        popUpTo(Screen.Meals.route)
                    }
                    mealViewModel.setPendingPhotoUri(uri)
                }
            )
        }

        composable(
            route = Screen.MealEdit.route,
            arguments = listOf(
                navArgument(Screen.MealEdit.MEAL_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getLong(Screen.MealEdit.MEAL_ID_ARG) ?: -1L
            val pendingPhotoUri by mealViewModel.pendingPhotoUri.collectAsState()
            var existingMeal by remember { mutableStateOf<MealWithComponents?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(mealId) {
                if (mealId > 0) {
                    existingMeal = mealViewModel.getMealById(mealId)
                }
            }

            val currentPendingPhoto = pendingPhotoUri
            val currentExistingMeal = existingMeal

            if (currentExistingMeal != null || currentPendingPhoto != null) {
                MealEditScreen(
                    photoUri = currentExistingMeal?.meal?.photoUri ?: currentPendingPhoto!!,
                    existingMeal = currentExistingMeal,
                    onUpdate = { description, components ->
                        if (currentExistingMeal != null) {
                            mealViewModel.updateMeal(
                                mealId = currentExistingMeal.meal.id,
                                description = description,
                                components = components,
                                timestamp = currentExistingMeal.meal.timestamp
                            )
                        } else {
                            mealViewModel.saveMeal(
                                photoUri = currentPendingPhoto!!,
                                description = description,
                                components = components
                            )
                        }
                        mealViewModel.clearPendingPhoto()
                        navController.popBackStack(Screen.Meals.route, inclusive = false)
                    },
                    onDelete = {
                        if (currentExistingMeal != null) {
                            mealViewModel.deleteMeal(currentExistingMeal.meal.id)
                        }
                        mealViewModel.clearPendingPhoto()
                        navController.popBackStack(Screen.Meals.route, inclusive = false)
                    },
                    onRetake = {
                        mealViewModel.clearPendingPhoto()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.ConversationsList.route) {
            ConversationsListScreen(
                viewModel = chatViewModel,
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversation = { newId ->
                    navController.navigate(Screen.Chat.createRoute(newId))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument(Screen.Chat.CONVERSATION_ID_ARG) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong(Screen.Chat.CONVERSATION_ID_ARG) ?: return@composable
            ChatScreen(
                conversationId = conversationId,
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel
            )
        }
    }
}
