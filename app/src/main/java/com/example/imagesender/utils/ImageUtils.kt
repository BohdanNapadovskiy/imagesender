package com.example.imagesender.utils

import android.content.Context
import android.graphics.Bitmap
import android.service.controls.ControlsProviderService
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.constraintlayout.widget.Constraints
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class ImageUtils(var context: Context) {


    fun pngFileToBase64(bitmap:Bitmap): String? {
        val executorService: ExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor();
        val futureString: Future<String> = executorService.submit(Callable {
            val file = saveBitmapToCache(bitmap)
            val bytes = file.readBytes()
            return@Callable Base64.encodeToString(bytes, Base64.DEFAULT)
        })
        return getResultFromFutureString(futureString)

    }

    private fun saveBitmapToCache(bitmap: Bitmap): File {
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs() // Make sure the directory exists
        val file = File(cachePath, "temp_image.png")
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush()
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return file
    }


    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun base64ToPng(base64String: String): File {
        val filename = "tmp_file.png";
        var imageFile: File? = null
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            imageFile = File(tempDir, filename)
            FileOutputStream(imageFile).use { output ->
                output.write(imageBytes)
            }
            return imageFile
        } catch ( e: IllegalArgumentException) {
            Toast.makeText(context, "Cannot convert base64 to image!", Toast.LENGTH_SHORT).show()
        }
        return imageFile!!;
    }


    private fun getResultFromFutureString(future: Future<String>): String? {
        var result: String? = null
        try {
            result = future.get()
        } catch (e: ExecutionException) {
            Log.e(ControlsProviderService.TAG, "Error getting result from server")
        } catch (e: InterruptedException) {
            Log.e(ControlsProviderService.TAG, "Error getting result from server")
        }
        return result
    }
}
