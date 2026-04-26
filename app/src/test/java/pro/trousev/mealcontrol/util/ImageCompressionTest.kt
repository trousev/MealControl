package pro.trousev.mealcontrol.util

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class ImageCompressionTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun compressImage_smallImageWithinBounds_returnsCompressedData() {
        val file = createTempImageFile(width = 100, height = 100, color = Color.RED)

        val result = ImageCompression.compressImage(file.absolutePath)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun compressImage_largeImageExceedingBounds_returnsCompressedData() {
        val file = createTempImageFile(width = 2048, height = 2048, color = Color.BLUE)

        val result = ImageCompression.compressImage(file.absolutePath)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun compressImage_wideImage_returnsCompressedData() {
        val file = createTempImageFile(width = 3000, height = 500, color = Color.GREEN)

        val result = ImageCompression.compressImage(file.absolutePath)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun compressImage_tallImage_returnsCompressedData() {
        val file = createTempImageFile(width = 500, height = 3000, color = Color.YELLOW)

        val result = ImageCompression.compressImage(file.absolutePath)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test(expected = java.io.FileNotFoundException::class)
    fun compressImage_nonExistingFile_throwsException() {
        ImageCompression.compressImage("/non/existing/path/image.jpg")
    }

    private fun createTempImageFile(
        width: Int,
        height: Int,
        color: Int,
    ): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)

        val file = File(context.cacheDir, "test_image_${width}x$height.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        bitmap.recycle()
        return file
    }
}
