package com.example.imagesender.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Environment;

import com.example.imagesender.enums.ServiceEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class BluetoothNUSFileSender {

    private BluetoothGatt mBluetoothGatt;


    public void sendFile(File file) {
        mBluetoothGatt = CustomBluetoothManager.getInstance().getGatt();
        if(mBluetoothGatt != null) {
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
        while (offset < data.length) {
            int chunkSize = Math.min(data.length - offset, 20); // Maximum size of a BLE packet
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(data, offset, chunk, 0, chunkSize);
            writeChunkToNUS(chunk);
            offset += chunkSize;

            try {
                Thread.sleep(100); // Small delay to ensure the receiver can process the data
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void writeChunkToNUS(byte[] chunk) {
        BluetoothGattCharacteristic txCharacteristic = mBluetoothGatt.getService(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value))
                .getCharacteristic(UUID.fromString(ServiceEnum.NUS_TX_CHARACTERISTIC_UUID.value));
        if (txCharacteristic != null) {
            txCharacteristic.setValue(chunk);
            mBluetoothGatt.writeCharacteristic(txCharacteristic);
        }
    }

}
