package com.example.imagesender.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class BluetoothConnectionManager implements Runnable {

    private static final String TAG = "BluetoothConnectionMgr";
    private BluetoothSocket socket;
    private BluetoothDevice device;
    private UUID serviceUUID;
    private final Object lock = new Object();

    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_CONNECTED = 2;
    public static final int STATUS_FAILED = 3;
    private int connectionStatus = STATUS_CONNECTING;

    public BluetoothConnectionManager(BluetoothDevice device, String serviceUUID) {
        this.device = device;
        this.serviceUUID = UUID.fromString(serviceUUID);
        this.socket = null;
    }

    @Override
    public void run() {
        // Lock to synchronize the connection process
        synchronized (lock) {
            connect();
            lock.notifyAll(); // Notify when connection status changes
        }
    }

    public void connect() {
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(this.serviceUUID);
            socket.connect();
            Log.i(TAG, "Connected to the device using NUS UUID");
            connectionStatus = STATUS_CONNECTED;
        } catch (IOException  e) {
            Log.e(TAG, "Initial connection failed, trying fallback method...", e);
//            connectionStatus = alternativeConnection();
        }
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
                Log.d(TAG, "Socket closed successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }

    // Return status message based on the connection status
    public String getStatusMessage() {
        switch (connectionStatus) {
            case STATUS_CONNECTING:
                return "Connecting";
            case STATUS_CONNECTED:
                return "Connected";
            case STATUS_FAILED:
                return "Failed to connect";
            default:
                return "Unknown status";
        }
    }

    // Get the Bluetooth socket
    public BluetoothSocket getSocket() {
        return socket;
    }

    // Fallback method if the initial connection method fails
    private int alternativeConnection() {
        try {
            Log.i(TAG, "Attempting fallback connection method...");
            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
            socket.connect();
            Log.i(TAG, "Fallback connection successful");
            return STATUS_CONNECTED;
        } catch (Exception e) {
            Log.e(TAG, "Fallback connection failed", e);
            return STATUS_FAILED;
        }
    }


}
