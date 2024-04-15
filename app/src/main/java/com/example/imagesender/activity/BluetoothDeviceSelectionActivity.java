package com.example.imagesender.activity;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.imagesender.R;
import com.example.imagesender.enums.ServiceEnum;


import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothDeviceSelectionActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    checkAndRequestPermissions();
                } else {
                    Toast.makeText(this, "Bluetooth must be enabled to discover devices", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allPermissionsGranted = permissions.values().stream().allMatch(granted -> granted);
                if (allPermissionsGranted) {
                    startBluetoothDiscovery();
                } else {
                    Toast.makeText(this, "Bluetooth permissions are required for this feature", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        ListView listView = findViewById(R.id.listview_bluetooth_devices);
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> connectToDevice(mDevices.get(position)));

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            checkAndRequestPermissions();
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.ACCESS_FINE_LOCATION });
        } else {
            startBluetoothDiscovery();
        }
    }

    private void startBluetoothDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null ) {
//                    if(device.getName() != null) {
                        mDevices.add(device);
                        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        Log.d(TAG, "Device found: " + device.getName() + ", " + device.getAddress());
//                    }
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        BluetoothSocket socket;
        mBluetoothAdapter.cancelDiscovery();
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value));
            // Implement retry mechanism here
            socket.connect();
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("Bluetooth", "Connection failed", e);
            Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Consider retrying the connection here
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }




}
