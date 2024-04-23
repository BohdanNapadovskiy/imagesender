package com.example.imagesender.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.imagesender.MainActivity
import com.example.imagesender.R
import com.example.imagesender.permisssion.PermissionManager

class BluetoothDeviceSelectionActivity : AppCompatActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mArrayAdapter: ArrayAdapter<String>? = null
    private var permissionManager: PermissionManager? = null
    private val mDevices: MutableList<BluetoothDevice> = ArrayList()
    private val enableBluetoothLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            checkAndRequestPermissions()
        } else {
            Toast.makeText(this, "Bluetooth must be enabled to discover devices", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        permissionManager = PermissionManager(this, PERMISSIONS_REQUEST_CODE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        val listView = findViewById<ListView>(R.id.listview_bluetooth_devices)
        mArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.setAdapter(mArrayAdapter)
        listView.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long -> connectToDevice(mDevices[position]) }
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ), 1)
            } else {
                startBLEScan()
            }
        } else {
            startBLEScan() // Assume permissions are granted below Android 12 for simplicity
        }
    }

    private fun startBLEScan() {
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()
        val requiredPermission: String
        requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION // Consider ACCESS_COARSE_LOCATION if less precision is acceptable
        }
        if (ActivityCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            return  // Exit the method if the necessary permissions are not granted
        }
        val scanCallback = BluetoothScanCallback(mArrayAdapter!!, mDevices, this)
        mBluetoothAdapter!!.getBluetoothLeScanner().startScan(null, settings, scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        val requiredPermission: String
        requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (ActivityCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            return  // Exit the method if the necessary permissions are not granted
        }
        val gattCallback = ImageSenderGattCallback(this, Handler(Looper.getMainLooper())) { device: BluetoothDevice, status: String -> handleConnectionResult(device, status) }
        device.connectGatt(this, false, gattCallback)
        Log.i("TAG", "Attempting to connect to device: " + device.getName())
        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show()
    }

    private fun handleConnectionResult(device: BluetoothDevice, status: String) {
        Log.i("TAG", "Connecting to device: " + device.getName())
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("device_name", device.getName())
        intent.putExtra("device_address", device.getAddress())
        intent.putExtra("status", status)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter!!.getBluetoothLeScanner() != null) {
            mBluetoothAdapter!!.getBluetoothLeScanner().stopScan(object : ScanCallback() {})
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }
}
