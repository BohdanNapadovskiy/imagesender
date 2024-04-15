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
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.imagesender.bluetooth.BluetoothConnectionManager;
import com.example.imagesender.MainActivity;
import com.example.imagesender.R;
import com.example.imagesender.enums.ServiceEnum;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BluetoothDeviceSelectionActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private BluetoothGatt mBluetoothGatt;

    private final UUID NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startBLEScan();
        }
    }

    private void startBLEScan() {
        ScanFilter nusFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(NUS_SERVICE_UUID))
                .build();
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(nusFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (!mDevices.contains(device)) {
                    mDevices.add(device);
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    mArrayAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "BLE Scan Failed with code " + errorCode);
            }
        });
    }

    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mBluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        // Attempts to discover services after successful connection.
                        gatt.discoverServices();
                        CustomBluetoothManager.getInstance().setGatt(gatt);
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                } else {
                    Log.w(TAG, "Error " + status + " encountered for " + device.getAddress() + "! Disconnecting...");
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService nusService = gatt.getService(NUS_SERVICE_UUID);
                    if (nusService != null) {
                        BluetoothGattCharacteristic rxCharacteristic = nusService.getCharacteristic(UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"));
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
                    runOnUiThread(() -> Toast.makeText(BluetoothDeviceSelectionActivity.this, "Received: " + strData, Toast.LENGTH_SHORT).show());
                }
            }
        });
        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter.getBluetoothLeScanner() != null) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(new ScanCallback() {});
        }
    }


//    private BluetoothAdapter mBluetoothAdapter;
//    private ArrayAdapter<String> mArrayAdapter;
//    private Set<BluetoothDevice> mDevices = new HashSet<>();
//    BluetoothConnectionManager connectionManager;
//
//    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            result -> {
//                if (result.getResultCode() == RESULT_OK) {
//                    checkAndRequestPermissions();
//                } else {
//                    Toast.makeText(this, "Bluetooth must be enabled to discover devices", Toast.LENGTH_SHORT).show();
//                }
//            }
//    );
//
//    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
//            new ActivityResultContracts.RequestMultiplePermissions(),
//            permissions -> {
//                boolean allPermissionsGranted = permissions.values().stream().allMatch(granted -> granted);
//                if (allPermissionsGranted) {
//                    startBluetoothDiscovery();
//                } else {
//                    Toast.makeText(this, "Bluetooth permissions are required for this feature", Toast.LENGTH_SHORT).show();
//                }
//            }
//    );
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_bluetooth);
//        ListView listView = findViewById(R.id.listview_bluetooth_devices);
//        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
//        listView.setAdapter(mArrayAdapter);
//        listView.setOnItemClickListener((parent, view, position, id) -> {
//            List<BluetoothDevice> deviceList = new ArrayList<>(mDevices);
//            connectToDevice(deviceList.get(position));
//        });
//
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            enableBluetoothLauncher.launch(enableBtIntent);
//        } else {
//            checkAndRequestPermissions();
//        }
//    }
//
//    private void checkAndRequestPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//                (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
//                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
//                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION});
//        } else {
//            startBluetoothDiscovery();
//        }
//    }
//
//    private void startBluetoothDiscovery() {
//        if (mBluetoothAdapter.isDiscovering()) {
//            mBluetoothAdapter.cancelDiscovery();
//        }
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mReceiver, filter);
//        mBluetoothAdapter.startDiscovery();
//    }
//
//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (device != null && mDevices.add(device)) {
//                    mArrayAdapter.clear();
//                    mArrayAdapter.addAll(mDevices.stream().map(d -> d.getName() + "\n" + d.getAddress()).collect(Collectors.toList()));
//                    mArrayAdapter.notifyDataSetChanged();
//                    Log.d(TAG, "Device found: " + device.getName() + ", " + device.getAddress());
//                }
//            }
//        }
//    };
//
//    private void connectToDevice(BluetoothDevice device) {
//        mBluetoothAdapter.cancelDiscovery();
//        connectionManager = new BluetoothConnectionManager(device, ServiceEnum.NUS_SERVICE_UUID.value);
//        connectionManager.connect();
//        Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra("device_name", device.getName());
//        intent.putExtra("device_address", device.getAddress());
//        intent.putExtra("status", connectionManager.getStatusMessage());
//        startActivity(intent);
//        finish();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (connectionManager != null) {
//            connectionManager.disconnect();
//        }
//        if (mReceiver != null) {
//            unregisterReceiver(mReceiver);
//        }
//        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
//            mBluetoothAdapter.cancelDiscovery();
//        }
//    }

}
