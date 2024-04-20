package com.example.imagesender.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt

class CustomBluetoothManager private constructor() {
    var gatt: BluetoothGatt? = null
    companion object {
        @kotlin.jvm.JvmStatic
        @get:Synchronized
        var instance: CustomBluetoothManager? = null
            get() {
                if (field == null) {
                    field = CustomBluetoothManager()
                }
                return field
            }
            private set
    }

    fun isConnected(): Boolean {
        return gatt != null && gatt!!.device.getBondState() === BluetoothDevice.BOND_BONDED
    }

    fun closeGatt() {
        if (gatt != null) {
            gatt!!.close()
            gatt = null
        }
    }
}
