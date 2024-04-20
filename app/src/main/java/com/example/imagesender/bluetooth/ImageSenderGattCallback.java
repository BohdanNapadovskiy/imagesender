package com.example.imagesender.bluetooth;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.imagesender.enums.ServiceEnum;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class ImageSenderGattCallback extends BluetoothGattCallback {

    private Context context;

    public ImageSenderGattCallback(Context context) {
        this.context = context;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        super.onConnectionStateChange(gatt, status, newState);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("TAG", "Connected to GATT server.");
                Objects.requireNonNull(CustomBluetoothManager.getInstance()).setGatt(gatt);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("TAG", "Disconnected from GATT server.");
            }
        } else {
            Log.w("TAG", "Connection state change error: " + status);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        UUID uuid = UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattService nusService = gatt.getService(uuid);
            if (nusService != null) {
                BluetoothGattCharacteristic rxCharacteristic = nusService.getCharacteristic(uuid);
                if (rxCharacteristic != null) {
                    gatt.setCharacteristicNotification(rxCharacteristic, true);
                    BluetoothGattDescriptor descriptor = rxCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            } else {
                Log.w(TAG, "NUS Service not found on device.");
            }
        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
        }
    }



    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            readCharacteristic(characteristic);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        readCharacteristic(characteristic);
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (UUID.fromString(ServiceEnum.NUS_TX_CHARACTERISTIC_UUID.value).equals(characteristic.getUuid())) {
            byte[] data = characteristic.getValue();
            String strData = new String(data, StandardCharsets.UTF_8);
            Log.i(TAG, "Received data: " + strData);
            Toast.makeText(context, "Received: " + strData, Toast.LENGTH_SHORT).show();
        }
    }



}
