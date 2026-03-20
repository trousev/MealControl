package pro.trousev.mealcontrol.viewmodel

import androidx.room.Room
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.util.SecureStorage

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {
    private lateinit var database: MealControlDatabase

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication().applicationContext,
                    MealControlDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        val mockSecureStorage =
            object : SecureStorage {
                override fun storeApiKey(apiKey: String) {}

                override fun retrieveApiKey(): String = ""
            }
        ServiceLocator.initialize(RuntimeEnvironment.getApplication(), mockSecureStorage)
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
