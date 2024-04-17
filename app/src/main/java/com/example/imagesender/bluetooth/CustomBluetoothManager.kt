package com.example.imagesender.bluetooth;

import android.bluetooth.BluetoothGatt;

public class CustomBluetoothManager {

    private static CustomBluetoothManager instance;
    private BluetoothGatt bluetoothGatt;

    private CustomBluetoothManager() {}

    public static synchronized CustomBluetoothManager getInstance() {
        if (instance == null) {
            instance = new CustomBluetoothManager();
        }
        return instance;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.bluetoothGatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return bluetoothGatt;
    }
}
