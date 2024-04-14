package com.example.imagesender.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.constraintlayout.widget.Constraints
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class ImageUtils {

    fun pmgFileToBase64(file: File):String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }


    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun base64ToBitmap(base64String: String?): Bitmap {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    fun convertBase64ToImage(context: Context, base64String: String?, fileName: String): String? {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        val newFileName = "_$fileName"
        var filePath: String? = null
        try {
            context.openFileOutput(newFileName, Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(decodedBytes)
                filePath = context.getFileStreamPath(newFileName).absolutePath
            }
        } catch (e: IOException) {
            Log.i(Constraints.TAG, "Error while decoding and storing the image: " + e.message)
        }
        return filePath
    }
}
