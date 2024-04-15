package com.example.taskforce.utils.network

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.imagesender.enums.ServiceEnum
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Arrays
import java.util.UUID
import kotlin.math.min

class NUSFileSender(context: Context?, bluetoothAdapter: BluetoothAdapter) {
    private var mDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var mOutputStream: OutputStream? = null
    private val context: Context? = null

    init {
        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(ContentValues.TAG, "Requires permissions for bluetooth connect")
        }
    }

    fun sendFileOverBluetooth(context: Context?, file: File) {
        val fileContents = readFile(file)
        if (fileContents != null) {
            sendData(fileContents)
        }
    }


    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun splitByteArray(data: ByteArray, chunkSize: Int): List<ByteArray> {
        val chunks: MutableList<ByteArray> = ArrayList()
        val length = data.size
        var i = 0
        while (i < length) {
            val end = min(length.toDouble(), (i + chunkSize).toDouble()).toInt()
            chunks.add(Arrays.copyOfRange(data, i, end))
            i += chunkSize
        }
        return chunks
    }

    private fun readFile(file: File): ByteArray {
        val bytesArray = ByteArray(file.length().toInt())
        try {
            FileInputStream(file).use { fis -> fis.read(bytesArray) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bytesArray
    }

    private fun sendData(data: ByteArray) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(ContentValues.TAG, "Requires permissions for bluetooth connect")
                return
            }
            bluetoothSocket =
                mDevice!!.createRfcommSocketToServiceRecord(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value))
            bluetoothSocket?.run {
                connect()
                mOutputStream = getOutputStream()
            }

            // Send data in chunks
            val chunkSize = 20 // Example chunk size; adjust based on actual MTU
            var start = 0
            while (start < data.size) {
                val end = min((start + chunkSize).toDouble(), data.size.toDouble())
                    .toInt()
                val chunk = Arrays.copyOfRange(data, start, end)
                mOutputStream?.write(chunk)
                mOutputStream?.flush()
                start += chunkSize
            }
            mOutputStream?.close()
            bluetoothSocket?.run { close() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
