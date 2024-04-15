package com.example.imagesender.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.imagesender.enums.ServiceEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BluetoothNusSendFile {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private BluetoothConnectionManager bluetoothManager;

    private static final String TAG = "BluetoothNusSendFile";

    public BluetoothNusSendFile(Context con, BluetoothAdapter adapter, String address) {
        context = con;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            // You might want to prompt the user to enable Bluetooth here
            return;
        }

        // Assume 'bluetoothDevice' is your previously connected or discovered device
        try {
            BluetoothDevice bluetoothDevice = adapter.getRemoteDevice(address);
            bluetoothManager = new BluetoothConnectionManager(bluetoothDevice, ServiceEnum.NUS_SERVICE_UUID.value);
            bluetoothManager.connect();
            outputStream = bluetoothManager.getSocket().getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Could not connect or get output stream: " + e.getMessage());
        }
    }

    public void sendPngFile(File file) {
        if (outputStream == null) {
            Toast.makeText(context, "Error during ne sending file by bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] fileData = readFile(file);
        if (fileData != null) {
            List<byte[]> chunks = splitByteArray(fileData, 20);  // Assuming MTU is 20 bytes
            try {
                for (byte[] chunk : chunks) {
                    outputStream.write(chunk);
                    outputStream.flush();
                }
                Toast.makeText(context, "File sent successfully via Bluetooth", Toast.LENGTH_SHORT).show();
                bluetoothManager.closeConnection();
            } catch (IOException e) {
                Toast.makeText(context, "Error during ne sending file via bluetooth", Toast.LENGTH_SHORT).show();
//                Log.e(TAG, "Error sending PNG data: " + e.getMessage());
            } finally {
                closeConnection();
                bluetoothManager.closeConnection();
            }
        }
    }

    private byte[] readFile(File file) {
        byte[] bytesArray = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytesArray);
        } catch (IOException e) {
            Toast.makeText(context, "Error during ne sending file by bluetooth", Toast.LENGTH_SHORT).show();
            return null;
        }
        return bytesArray;
    }

    private List<byte[]> splitByteArray(byte[] data, int chunkSize) {
        List<byte[]> chunks = new ArrayList<>();
        int length = data.length;
        for (int i = 0; i < length; i += chunkSize) {
            int end = Math.min(length, i + chunkSize);
            chunks.add(Arrays.copyOfRange(data, i, end));
        }
        return chunks;
    }

    public void closeConnection() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close output stream: " + e.getMessage());
            }
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket: " + e.getMessage());
            }
        }
    }

}
