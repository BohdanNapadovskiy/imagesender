package com.example.imagesender.activity;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.example.imagesender.R;
import com.example.imagesender.enums.ServiceEnum;


import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothDeviceSelectionActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        ListView listView = findViewById(R.id.listview_bluetooth_devices);
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> connectToDevice(mDevices.get(position)));

        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth scan permissions required");
            return;
        }
        mBluetoothAdapter.startDiscovery();
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Check for NUS UUID in the advertised data (simplified check)
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Bluetooth connect permissions required");
                    return;
                }
//                if (device.fetchUuidsWithSdp() && Arrays.asList(device.getUuids()).contains(ServiceDevice.NUS_SERVICE_UUID)) {
                    mDevices.add(device);
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        BluetoothSocket socket = null;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Bluetooth connect permissions required");
                return;
            }
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(ServiceEnum.NUS_SERVICE_UUID.value));
            socket.connect();
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("Bluetooth", "Connection failed", e);
            Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth scan permissions required");
            return;
        }
        mBluetoothAdapter.cancelDiscovery();
    }

//    private BluetoothAdapter bluetoothAdapter;
//    private ArrayList<String> deviceList = new ArrayList<>();
//    private ArrayAdapter<String> arrayAdapter;
//    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
//                boolean allGranted = permissions.containsValue(true);
//                if (allGranted) {
//                    startDiscovery();
//                } else {
//                    Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_bluetooth);
//
//        ListView listView = findViewById(R.id.listview_bluetooth_devices);
//        arrayAdapter = createArrayAdapterView();
//        listView.setAdapter(arrayAdapter);
//
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (bluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
//        createButton();
//    }
//
//    private void startDiscovery() {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothDevice.ACTION_FOUND);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        filter.addAction(BluetoothDevice.ACTION_UUID);
//        registerReceiver(receiver, filter);
//        bluetoothAdapter.startDiscovery();
//    }
//
//    private final BroadcastReceiver receiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                String deviceName = device.getName();
//                String deviceAddress = device.getAddress(); // MAC address
//
//                // You can add a condition here to filter devices by name, type, or bonding state
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                    Log.d("Bluetooth Device", "Device found: " + deviceName + " [" + deviceAddress + "]");
//                    deviceList.add(deviceName + "\n" + deviceAddress);
//                    arrayAdapter.notifyDataSetChanged();
//                }
//            }
//        }
//    };
//
//
//    private Button createButton() {
//        Button scanButton = findViewById(R.id.button_scan_bluetooth);
//        scanButton.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.S)
//            @Override
//            public void onClick(View view) {
//                if (ContextCompat.checkSelfPermission(BluetoothDeviceSelectionActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
//                        ContextCompat.checkSelfPermission(BluetoothDeviceSelectionActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT});
//                } else {
//                    startDiscovery();
//                }
//            }
//        });
//        return scanButton;
//    }
//
//
//    private ArrayAdapter<String> createArrayAdapterView() {
//        return arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item_bluetooth_device, deviceList) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                if (convertView == null) {
//                    convertView = getLayoutInflater().inflate(R.layout.list_item_bluetooth_device, parent, false);
//                }
//                TextView textView = convertView.findViewById(R.id.textview_device_info);
//                Button button = convertView.findViewById(R.id.button_select_device);
//
//                String item = getItem(position);
//                textView.setText(item);
//
//                button.setOnClickListener(view -> {
//                    String deviceAddress = item.split("\n")[1];
//                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
//                    selectDevice(device);
//                });
//
//                return convertView;
//            }
//        };
//    }
//
//
//    private void selectDevice(BluetoothDevice device) {
//        runOnUiThread(() -> {
//            Intent intent = new Intent(BluetoothDeviceSelectionActivity.this, MainActivity.class);
//            intent.putExtra("device_name", device.getName());
//            intent.putExtra("device_address", device.getAddress());
//            startActivity(intent);
//        });
//
//    }

}
