package com.example.imagesender.bluetooth;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.example.imagesender.bluetooth.BluetoothConnectionManager;
import com.example.imagesender.MainActivity;
import com.example.imagesender.R;
import com.example.imagesender.enums.ServiceEnum;


import java.util.ArrayList;

public class BluetoothDeviceSelectionActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    BluetoothConnectionManager connectionManager;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();


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
            requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION});
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
                if (device != null) {
                    mDevices.add(device);
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    Log.d(TAG, "Device found: " + device.getName() + ", " + device.getAddress());
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        mBluetoothAdapter.cancelDiscovery();
        connectionManager = new BluetoothConnectionManager(device, ServiceEnum.NUS_SERVICE_UUID.value);
        int status = connectionManager.discoverServicesAndCheckUUID(device);
        String statusMessage = connectionManager.getStatusMessage(status);
        Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("device_name", device.getName());
        intent.putExtra("device_address", device.getAddress());
        intent.putExtra("status", statusMessage);
        startActivity(intent);
        finish(); // Optional: if you want to close the curren
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If using a connection manager instance at a broader scope, disconnect here
        if (connectionManager != null) {
            connectionManager.disconnect();
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }


}
