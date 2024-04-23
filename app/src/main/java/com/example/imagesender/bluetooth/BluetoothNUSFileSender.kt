package com.example.imagesender.bluetooth

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.imagesender.bluetooth.CustomBluetoothManager.Companion.instance
import com.example.imagesender.enums.ServiceEnum
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.min

class BluetoothNUSFileSender(private val context: Context) {
    private var mBluetoothGatt: BluetoothGatt? = null
    fun sendFile(file: File) {
        mBluetoothGatt = instance!!.gatt
        if (mBluetoothGatt != null) {
            try {
                FileInputStream(file).use { fileInputStream ->
                    val fileContent = ByteArray(file.length().toInt())
                    fileInputStream.read(fileContent)
                    sendByteArray(fileContent)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Log.e("BluetoothNUSFileSender", "Error during the sending file via bluetooth")
            Toast.makeText(context, "Error during the sending file via bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendByteArray(data: ByteArray) {
        var offset = 0
        val gattService = mBluetoothGatt!!.getService(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value))
        if (gattService != null) {
            while (offset < data.size) {
                val chunkSize = min((data.size - offset).toDouble(), 20.0).toInt() // Maximum size of a BLE packet
                val chunk = ByteArray(chunkSize)
                System.arraycopy(data, offset, chunk, 0, chunkSize)
                writeChunkToNUS(chunk, gattService)
                offset += chunkSize
                try {
                    Thread.sleep(100) // Small delay to ensure the receiver can process the data
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        } else {
            Log.e("BluetoothNUSFileSender", "Error during the sending file via bluetooth")
            Toast.makeText(context, "Required GATT service not found on the device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeChunkToNUS(chunk: ByteArray, gattService: BluetoothGattService) {
        val txCharacteristic = mBluetoothGatt!!.getService(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value))
                .getCharacteristic(UUID.fromString(ServiceEnum.NUS_RX_CHARACTERISTIC_UUID.value))
        if (txCharacteristic != null) {
            txCharacteristic.setValue(chunk)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            mBluetoothGatt!!.writeCharacteristic(txCharacteristic)
        }
    }
}
