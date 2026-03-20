package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.data.remote.MealComponentDto
import pro.trousev.mealcontrol.data.remote.MealDetectionResponse
import pro.trousev.mealcontrol.data.remote.MealDetectionResult
import pro.trousev.mealcontrol.data.remote.OutputContentItem
import pro.trousev.mealcontrol.data.remote.OutputItem
import pro.trousev.mealcontrol.data.remote.parseMealDetectionResult

@RunWith(RobolectricTestRunner::class)
class MealDetectionViewModelTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
    }

    @Test
    fun mealDetectionViewModel_creation_doesNotCrash() {
        val viewModel = MealDetectionViewModel(application)
        assertNotNull(viewModel.state)
    }

    @Test
    fun mealDetectionViewModel_initialState_hasDefaultValues() {
        val viewModel = MealDetectionViewModel(application)
        val state = viewModel.state.value

        assertEquals("", state.photoUri)
        assertTrue(state.messages.isEmpty())
        assertNull(state.currentComponents)
        assertNull(state.currentQuestion)
        assertNull(state.mealName)
        assertNull(state.lastResponseJson)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(-1L, state.conversationId)
    }

    @Test
    fun mealDetectionState_defaultValues_areCorrect() {
        val state = MealDetectionState()

        assertEquals("", state.photoUri)
        assertTrue(state.messages.isEmpty())
        assertNull(state.currentComponents)
        assertNull(state.currentQuestion)
        assertNull(state.mealName)
        assertNull(state.lastResponseJson)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(-1L, state.conversationId)
    }

    @Test
    fun mealDetectionState_copy_worksCorrectly() {
        val state = MealDetectionState()
        val updatedState = state.copy(
            photoUri = "test_uri",
            isLoading = true,
            error = "test error"
        )

        assertEquals("test_uri", updatedState.photoUri)
        assertTrue(updatedState.isLoading)
        assertEquals("test error", updatedState.error)
        assertTrue(updatedState.messages.isEmpty())
    }

    @Test
    fun parseMealDetectionResult_nullResponse_returnsNull() {
        val result = parseMealDetectionResult(null)
        assertNull(result)
    }

    @Test
    fun parseMealDetectionResult_validResponse_parsesCorrectly() {
        val response = MealDetectionResponse(
            id = "resp_123",
            output = listOf(
                OutputItem(
                    id = "output_1",
                    type = "message",
                    content = listOf(
                        OutputContentItem(
                            type = "output_text",
                            text = """{"name":"Test Meal","components":[{"name":"Chicken","weight_g":150,"energy_kcal":250,"fat_g":10,"protein_g":30,"carbs_g":5},{"name":"Rice","weight_g":100,"energy_kcal":130,"fat_g":1,"protein_g":3,"carbs_g":28}],"followup":""}"""
                        )
                    )
                )
            ),
            error = null
        )

        val result = parseMealDetectionResult(response)

        assertNotNull(result)
        val parsedResult = result!!
        assertEquals("Test Meal", parsedResult.name)
        assertEquals(2, parsedResult.components.size)
        assertEquals("Chicken", parsedResult.components[0].name)
        assertEquals(150.0, parsedResult.components[0].weightG, 0.01)
        assertEquals(250.0, parsedResult.components[0].energyKcal, 0.01)
        assertEquals("Rice", parsedResult.components[1].name)
        assertEquals("", parsedResult.followup)
    }

    @Test
    fun parseMealDetectionResult_responseWithFollowup_parsesCorrectly() {
        val response = MealDetectionResponse(
            id = "resp_456",
            output = listOf(
                OutputItem(
                    id = "output_1",
                    type = "message",
                    text = """{"name":"Pizza","components":[],"followup":"How large was the pizza slice?"}"""
                )
            )
        )

        val result = parseMealDetectionResult(response)

        assertNotNull(result)
        val parsedResult = result!!
        assertEquals("Pizza", parsedResult.name)
        assertTrue(parsedResult.components.isEmpty())
        assertEquals("How large was the pizza slice?", parsedResult.followup)
    }

    @Test
    fun parseMealDetectionResult_invalidJson_returnsNull() {
        val response = MealDetectionResponse(
            id = "resp_789",
            output = listOf(
                OutputItem(
                    id = "output_1",
                    type = "message",
                    content = listOf(
                        OutputContentItem(
                            type = "output_text",
                            text = "not valid json"
                        )
                    )
                )
            )
        )

        val result = parseMealDetectionResult(response)

        assertNull(result)
    }

    @Test
    fun parseMealDetectionResult_emptyOutput_returnsNull() {
        val response = MealDetectionResponse(
            id = "resp_empty",
            output = emptyList()
        )

        val result = parseMealDetectionResult(response)

        assertNull(result)
    }

    @Test
    fun parseMealDetectionResult_responseWithoutMessageType_returnsNull() {
        val response = MealDetectionResponse(
            id = "resp_999",
            output = listOf(
                OutputItem(
                    id = "output_1",
                    type = "other_type"
                )
            )
        )

        val result = parseMealDetectionResult(response)

        assertNull(result)
    }

    @Test
    fun mealDetectionMessage_creation_worksCorrectly() {
        val message = MealDetectionMessage(
            content = "Test message",
            isFromUser = true,
            timestamp = 1234567890L
        )

        assertEquals("Test message", message.content)
        assertTrue(message.isFromUser)
        assertEquals(1234567890L, message.timestamp)
    }

    @Test
    fun mealComponentDto_creation_worksCorrectly() {
        val component = MealComponentDto(
            name = "Apple",
            weightG = 200.0,
            energyKcal = 95.0,
            fatG = 0.3,
            proteinG = 0.5,
            carbsG = 25.0
        )

        assertEquals("Apple", component.name)
        assertEquals(200.0, component.weightG, 0.01)
        assertEquals(95.0, component.energyKcal, 0.01)
        assertEquals(0.3, component.fatG, 0.01)
        assertEquals(0.5, component.proteinG, 0.01)
        assertEquals(25.0, component.carbsG, 0.01)
    }

    @Test
    fun mealDetectionResult_creation_worksCorrectly() {
        val components = listOf(
            MealComponentDto("Bread", 50.0, 130.0, 1.0, 4.0, 25.0),
            MealComponentDto("Butter", 10.0, 70.0, 8.0, 0.0, 0.0)
        )
        val result = MealDetectionResult(
            name = "Breakfast",
            components = components,
            followup = ""
        )

        assertEquals("Breakfast", result.name)
        assertEquals(2, result.components.size)
        assertEquals("", result.followup)
    }

    @Test
    fun mealDetectionResult_withFollowup_worksCorrectly() {
        val result = MealDetectionResult(
            name = "Unknown Meal",
            components = emptyList(),
            followup = "What type of bread was it?"
        )

        assertEquals("Unknown Meal", result.name)
        assertTrue(result.components.isEmpty())
        assertEquals("What type of bread was it?", result.followup)
    }
}