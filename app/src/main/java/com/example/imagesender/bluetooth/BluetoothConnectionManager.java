package com.example.imagesender.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionManager {

    private static final String TAG = "BluetoothConnectionMgr";
    private BluetoothSocket socket;
    private BluetoothDevice device;
    private UUID serviceUUID;

    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_CONNECTED = 2;
    public static final int STATUS_FAILED = 3;

    public BluetoothConnectionManager(BluetoothDevice device, String serviceUUID) {
        this.device = device;
        this.serviceUUID = UUID.fromString(serviceUUID);
        this.socket = null;
    }

    public int connect() {
        try {
            socket = device.createRfcommSocketToServiceRecord(serviceUUID);
            socket.connect();
            Log.i(TAG, "Connected to the device using NUS UUID");
            return STATUS_CONNECTED;
        } catch (Exception e) {
            Log.e(TAG, "Initial connection failed, trying fallback method...", e);
            return alternativeConnection();
        }
    }
    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
                Log.d(TAG, "Socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }

    public String getStatusMessage(int status) {
        switch (status) {
            case STATUS_CONNECTING:
                return "Connecting";
            case STATUS_CONNECTED:
                return "Connected";
            case STATUS_FAILED:
                return "Failed";
            default:
                return "Unknown";
        }
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    public void closeConnection() {
        try {
            if(socket!=null) {
                this.socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int alternativeConnection() {
        try {
            Log.e("", "trying fallback...");
            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
            socket.connect();
            Log.e("", "Connected");
            return STATUS_CONNECTED;
        } catch (Exception e2) {
            Log.e("", "Couldn't establish Bluetooth connection!");
            return STATUS_FAILED;
        }
    }


}
