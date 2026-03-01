package pro.trousev.mealcontrol.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pro.trousev.mealcontrol.ui.theme.MealControlTheme

class DebugMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MealControlTheme {
                pro.trousev.mealcontrol.AppContent()
            }
        }
    }
}