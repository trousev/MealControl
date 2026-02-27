package pro.trousev.mealcontrol.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PhotoStorageTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun getPhotoDirectory_createsDirectoryIfNotExists() {
        val dir = PhotoStorage.getPhotoDirectory(context)

        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertTrue(dir.absolutePath.contains("meal_photos"))
    }

    @Test
    fun getPhotoDirectory_returnsSameDirectory() {
        val dir1 = PhotoStorage.getPhotoDirectory(context)
        val dir2 = PhotoStorage.getPhotoDirectory(context)

        assertEquals(dir1.absolutePath, dir2.absolutePath)
    }

    @Test
    fun createPhotoFile_createsFileWithCorrectPattern() {
        val file = PhotoStorage.createPhotoFile(context)

        assertTrue(file.parentFile?.exists() ?: false)
        assertTrue(file.name.startsWith("MEAL_"))
        assertTrue(file.name.endsWith(".jpg"))
        assertTrue(file.name.length > 10)
    }

    @Test
    fun createPhotoFile_createsFileWithPattern() {
        val file = PhotoStorage.createPhotoFile(context)

        assertTrue(file.name.startsWith("MEAL_"))
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun deletePhoto_returnsTrueForExistingFile() {
        val file = PhotoStorage.createPhotoFile(context)
        assertTrue(file.createNewFile())

        val result = PhotoStorage.deletePhoto(file.absolutePath)

        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun deletePhoto_returnsFalseForNonExistingFile() {
        val nonExistingPath = "/non/existing/path/photo.jpg"

        val result = PhotoStorage.deletePhoto(nonExistingPath)

        assertFalse(result)
    }

    @Test
    fun deletePhoto_handlesEmptyPath() {
        val result = PhotoStorage.deletePhoto("")

        assertFalse(result)
    }
}
