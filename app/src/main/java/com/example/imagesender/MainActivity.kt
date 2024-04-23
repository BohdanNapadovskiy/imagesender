package com.example.imagesender

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.imagesender.activity.ImageSelectorActivity
import com.example.imagesender.bluetooth.BluetoothDeviceSelectionActivity
import com.example.imagesender.bluetooth.BluetoothNUSFileSender
import com.example.imagesender.bluetooth.CustomBluetoothManager
import com.example.imagesender.bluetooth.CustomBluetoothManager.Companion.instance
import com.example.imagesender.permisssion.PermissionManager
import com.example.imagesender.utils.ImageUtils
import com.example.imagesender.utils.network.HttpsServerUtil

class MainActivity : AppCompatActivity() {
    private var customBluetoothManager: CustomBluetoothManager? = null
    private val buttonSendImage: Button? = null
    private var imageUtils: ImageUtils? = null
    private var serverUtil: HttpsServerUtil? = null
    private var mImageView: ImageView? = null
    private var permissionManager: PermissionManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mGetContent: ActivityResultLauncher<String>? = null
    private val intentContent: ActivityResultLauncher<Intent>? = null
    private val textViewPath: TextView? = null
    private val nusFileSender: BluetoothNUSFileSender? = null
    private var deviceName: String? = null
    private var deviceAddress: String? = null
    private val permissions = arrayOf<String?>(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        customBluetoothManager = instance
        super.onCreate(savedInstanceState)
        imageUtils = ImageUtils(this)
        serverUtil = HttpsServerUtil(this)
        permissionManager = PermissionManager(this, PERMISSIONS_REQUEST_CODE)
        setContentView(R.layout.activity_main)
        initializeBluetooth()
        val intent = intent
        if (intent != null) {
            deviceName = if (intent.getStringExtra("device_name") == null) "" else intent.getStringExtra("device_name")
            deviceAddress = if (intent.getStringExtra("device_address") == null) "" else intent.getStringExtra("device_address")
            val status = if (intent.getStringExtra("status") == null) "" else intent.getStringExtra("status")!!
            val statusTextView = findViewById<TextView>(R.id.status_text_view) // Assuming you have a TextView for this
            statusTextView.text = "Device: $deviceName\nAddress: $deviceAddress\nStatus: $status"
        }
        findViewById<View>(R.id.button_select_image).setOnClickListener { view: View? -> startActivity(Intent(this@MainActivity, ImageSelectorActivity::class.java)) }
        mImageView = findViewById(R.id.image_view_selected)
        mGetContent = registerForActivityResult<String, Uri>(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                mImageView!!.setImageURI(uri)
            }
        }
        findViewById<View>(R.id.button_select_image).setOnClickListener { v: View? -> mGetContent!!.launch("image/*") }
        mImageView = findViewById(R.id.image_view_selected)
        findViewById<View>(R.id.button_connect_server).setOnClickListener { view: View? -> serverUtil!!.connectToHTTPSServer() }
        findViewById<View>(R.id.button_scan_bluetooth).setOnClickListener { view: View? -> startActivity(Intent(this@MainActivity, BluetoothDeviceSelectionActivity::class.java)) }
        findViewById<View>(R.id.button_send_image)
                .setOnClickListener { view: View? -> sendImageByBluetooth() }
    }

    private fun sendImageByBluetooth() {
        if (mImageView!!.getDrawable() != null) {
            if (serverUtil!!.isNetworkConnected(this)) {
                if (deviceAddress != null) {
                    sendFile()
                } else {
                    Toast.makeText(this@MainActivity, "Bluetooth device not selected!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "No internet connection!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@MainActivity, "No image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendFile() {
        val drawable = mImageView!!.getDrawable() as BitmapDrawable
        val imageBase64 = imageUtils!!.bitmapToBase64(drawable.bitmap)
        val dz38Data = serverUtil!!.sendBase64ToServer(imageBase64)
        if (dz38Data != null) {
            val tmpFile = imageUtils!!.base64ToPng(dz38Data)
            val nusFileSender = BluetoothNUSFileSender(this)
            nusFileSender.sendFile(tmpFile)
        } else {
            Toast.makeText(this@MainActivity, "Empty result from https server", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeBluetooth() {
        if (!permissionManager!!.hasPermission(Manifest.permission.BLUETOOTH_SCAN) ||
                !permissionManager!!.hasPermission(Manifest.permission.BLUETOOTH_CONNECT) ||
                !permissionManager!!.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionManager!!.requestPermissions(permissions)
        } else {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, continue with Bluetooth operations.
                initializeBluetooth()
            } else {
                // Permission was denied. Inform the user that the permission is necessary.
                Toast.makeText(this, "Bluetooth permission is necessary to connect to devices.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val REQUEST_BLUETOOTH_CONNECT = 101
        private const val PERMISSIONS_REQUEST_CODE = 100
    }
}
