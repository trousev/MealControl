package pro.trousev.mealcontrol.viewmodel

import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private lateinit var database: MealControlDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication().applicationContext,
            MealControlDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        ServiceLocator.initialize(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        database.close()
        ServiceLocator.resetForTesting()
    }

    @Test
    fun chatViewModel_creation_doesNotCrash() {
        val viewModel = ChatViewModel()
        assertNotNull(viewModel.conversations)
        assertNotNull(viewModel.currentConversation)
    }

    @Test
    fun chatViewModel_conversationsStateFlow_isInitialized() {
        val viewModel = ChatViewModel()
        assertNotNull(viewModel.conversations)
        assertNotNull(viewModel.conversations.value)
    }

    @Test
    fun chatViewModel_currentConversationStateFlow_isInitialized() {
        val viewModel = ChatViewModel()
        assertNotNull(viewModel.currentConversation)
        assertNull(viewModel.currentConversation.value)
    }
}