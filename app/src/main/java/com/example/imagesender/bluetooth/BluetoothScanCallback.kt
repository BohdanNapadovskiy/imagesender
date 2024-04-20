package com.example.imagesender.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import com.example.imagesender.permisssion.PermissionManager

class BluetoothScanCallback(private val mArrayAdapter: ArrayAdapter<String>, private val mDevices: MutableList<BluetoothDevice>,
                            private val context: Context) : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return
        }
        Log.i("TAG", "Device found: " + result.device.getName())
        val device = result.device
        if (!mDevices.contains(device)) {
            mDevices.add(device)
            mArrayAdapter.add("""
    ${device.getName()}
    ${device.getAddress()}
    """.trimIndent())
            mArrayAdapter.notifyDataSetChanged()
        }
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e("TAG", "BLE Scan Failed with code $errorCode")
    }
}
