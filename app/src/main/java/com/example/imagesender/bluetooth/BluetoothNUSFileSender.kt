package com.example.imagesender.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.imagesender.enums.ServiceEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class BluetoothNUSFileSender {

    private BluetoothGatt mBluetoothGatt;
    private Context context;

    public BluetoothNUSFileSender(Context context) {
        this.context = context;
    }


    public void sendFile(File file) {
        mBluetoothGatt = CustomBluetoothManager.getInstance().getGatt();
        if (mBluetoothGatt != null) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] fileContent = new byte[(int) file.length()];
                fileInputStream.read(fileContent);

                sendByteArray(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendByteArray(byte[] data) {
        int offset = 0;
        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value));
        if (gattService != null) {
            while (offset < data.length) {
                int chunkSize = Math.min(data.length - offset, 20); // Maximum size of a BLE packet
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(data, offset, chunk, 0, chunkSize);
                writeChunkToNUS(chunk, gattService);
                offset += chunkSize;

                try {
                    Thread.sleep(100); // Small delay to ensure the receiver can process the data
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            Log.e("BluetoothNUSFileSender", "Error during the sending file via bluetooth");
            Toast.makeText(context, "Required GATT service not found on the device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeChunkToNUS(byte[] chunk, BluetoothGattService gattService) {
        BluetoothGattCharacteristic txCharacteristic = mBluetoothGatt.getService(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value))
                .getCharacteristic(UUID.fromString(ServiceEnum.NUS_RX_CHARACTERISTIC_UUID.value));
        if (txCharacteristic != null) {
            txCharacteristic.setValue(chunk);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mBluetoothGatt.writeCharacteristic(txCharacteristic);
        }

    }

}
