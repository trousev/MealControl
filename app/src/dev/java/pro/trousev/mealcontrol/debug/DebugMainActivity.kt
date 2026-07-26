package pro.trousev.mealcontrol.debug

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.ui.theme.MealControlTheme
import java.io.File

class DebugMainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "DebugMainActivity"
        const val ACTION_DUMP_STATE = "pro.trousev.mealcontrol.DUMP_STATE"
        const val ACTION_RESTORE_STATE = "pro.trousev.mealcontrol.RESTORE_STATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.initialize(this)

        when (intent?.action) {
            ACTION_DUMP_STATE -> {
                handleDumpState()
                finish()
                return
            }
            ACTION_RESTORE_STATE -> {
                handleRestoreState()
                finish()
                return
            }
        }

        enableEdgeToEdge()
        setContent {
            MealControlTheme {
                pro.trousev.mealcontrol.AppContent()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            ACTION_DUMP_STATE -> {
                handleDumpState()
                finish()
            }
            ACTION_RESTORE_STATE -> {
                handleRestoreState()
                finish()
            }
        }
    }

    private fun handleDumpState() {
        try {
            val json = StateFixtureManager.dumpState(this)
            val file = File(filesDir, StateFixtureManager.DUMP_FILE_NAME)
            file.writeText(json)
            Log.i(TAG, "State dumped successfully (${file.length()} bytes)")
            setResult(RESULT_OK)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dump state", e)
            setResult(RESULT_CANCELED)
        }
    }

    private fun handleRestoreState() {
        try {
            val file = File(filesDir, StateFixtureManager.RESTORE_FILE_NAME)
            if (!file.exists()) {
                Log.e(TAG, "Restore fixture file not found: ${file.absolutePath}")
                setResult(RESULT_CANCELED)
                return
            }
            val json = file.readText()
            StateFixtureManager.restoreState(this, json)
            file.delete()
            Log.i(TAG, "State restored successfully")
            setResult(RESULT_OK)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore state", e)
            setResult(RESULT_CANCELED)
        }
    }
}
