package pro.trousev.mealcontrol.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoStorage {
    private const val PHOTO_DIR = "meal_photos"

    fun getPhotoDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTO_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createPhotoFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "MEAL_$timestamp.jpg"
        return File(getPhotoDirectory(context), fileName)
    }

    fun deletePhoto(photoUri: String): Boolean {
        val file = File(photoUri)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
