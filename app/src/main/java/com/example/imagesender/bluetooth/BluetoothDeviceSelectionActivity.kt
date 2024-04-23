package com.example.imagesender.bluetooth;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.imagesender.MainActivity;
import com.example.imagesender.R;
import com.example.imagesender.enums.ServiceEnum;
import com.example.imagesender.permisssion.PermissionManager;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BluetoothDeviceSelectionActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    private PermissionManager permissionManager;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private List<BluetoothDevice> mDevices = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        permissionManager = new PermissionManager(this, PERMISSIONS_REQUEST_CODE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        ListView listView = findViewById(R.id.listview_bluetooth_devices);
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> connectToDevice(mDevices.get(position)));

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            checkAndRequestPermissions();
        }
    }


    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
            } else {
                startBLEScan();
            }
        } else {
            startBLEScan(); // Assume permissions are granted below Android 12 for simplicity
        }
    }

    private void startBLEScan() {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        String requiredPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermission = Manifest.permission.BLUETOOTH_SCAN;
        } else {
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION; // Consider ACCESS_COARSE_LOCATION if less precision is acceptable
        }
        if (ActivityCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            return; // Exit the method if the necessary permissions are not granted
        }
        BluetoothScanCallback scanCallback = new BluetoothScanCallback(mArrayAdapter, mDevices, this);
        mBluetoothAdapter.getBluetoothLeScanner().startScan(null, settings, scanCallback);
    }

    private void connectToDevice(BluetoothDevice device) {
        String requiredPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermission = Manifest.permission.BLUETOOTH_SCAN;
        } else {
            requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION; // Consider ACCESS_COARSE_LOCATION if less precision is acceptable
        }
        if (ActivityCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            return; // Exit the method if the necessary permissions are not granted
        }
        ImageSenderGattCallback gattCallback = new ImageSenderGattCallback(this);
        device.connectGatt(this, false, gattCallback);
        Log.i("TAG", "Connecting to device: " + device.getName());
        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("device_name", device.getName());
        intent.putExtra("device_address", device.getAddress());
        intent.putExtra("status", "Connected");
        startActivity(intent);
        finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter.getBluetoothLeScanner() != null) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(new ScanCallback() {
            });
        }
    }

}
