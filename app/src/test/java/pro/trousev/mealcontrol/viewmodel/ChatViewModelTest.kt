package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.TestDatabaseFactory

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private lateinit var application: Application
    private lateinit var database: MealControlDatabase

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
        database = TestDatabaseFactory.createInMemory(application)
    }

    @Test
    fun chatViewModel_creation_doesNotCrash() {
        val viewModel = ChatViewModel(application)
        assertNotNull(viewModel.conversations)
        assertNotNull(viewModel.currentConversation)
    }

    @Test
    fun chatViewModel_conversationsStateFlow_isInitialized() {
        val viewModel = ChatViewModel(application)
        assertNotNull(viewModel.conversations)
        assertNotNull(viewModel.conversations.value)
    }

    @Test
    fun chatViewModel_currentConversationStateFlow_isInitialized() {
        val viewModel = ChatViewModel(application)
        assertNotNull(viewModel.currentConversation)
        assertNull(viewModel.currentConversation.value)
    }
}
