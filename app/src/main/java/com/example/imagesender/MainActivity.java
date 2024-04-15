package com.example.imagesender;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import com.example.imagesender.bluetooth.BluetoothDeviceSelectionActivity;
import com.example.imagesender.activity.ImageSelectorActivity;
import com.example.imagesender.utils.ImageUtils;
import com.example.imagesender.bluetooth.BluetoothNusSendFile;
import com.example.imagesender.utils.network.HttpsServerUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button buttonSendImage;
    private ImageUtils imageUtils;
    private HttpsServerUtil serverUtil;
    private ImageView mImageView;
    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<String> mGetContent;
    private ActivityResultLauncher<Intent> intentContent;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT = 101;
    private TextView textViewPath;
    private BluetoothNusSendFile nusFileSender;
    private String deviceName;
    private String deviceAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check if Bluetooth connect permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
        } else {
            // Permission already granted
            initializeBluetooth();
        }

        imageUtils = new ImageUtils();
        serverUtil = new HttpsServerUtil(this);

        Intent intent = getIntent();
        if (intent != null) {
            deviceName = intent.getStringExtra("device_name");
            deviceAddress = intent.getStringExtra("device_address");
            String status = intent.getStringExtra("status");
            TextView statusTextView = findViewById(R.id.status_text_view); // Assuming you have a TextView for this
            statusTextView.setText("Device: " + deviceName + "\nAddress: " + deviceAddress+"\nStatus: " + status);
        }


        findViewById(R.id.button_select_image).setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, ImageSelectorActivity.class));
        });
        mImageView = findViewById(R.id.image_view_selected);
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                mImageView.setImageURI(uri);
            }
        });


        findViewById(R.id.button_select_image).setOnClickListener(v -> mGetContent.launch("image/*"));
        mImageView = findViewById(R.id.image_view_selected);

        findViewById(R.id.button_connect_server).setOnClickListener(view -> serverUtil.connectToHTTPSServer());

        findViewById(R.id.button_scan_bluetooth).setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, BluetoothDeviceSelectionActivity.class)));

        findViewById(R.id.button_send_image)
                .setOnClickListener(view -> sendImageByBluetooth());
    }

    private void sendImageByBluetooth() {
        if (mImageView.getDrawable() != null) {
            if (serverUtil.isNetworkConnected(this)) {
                if (deviceAddress !=null) {
                    sendFile();
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth device not selected!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "No internet connection!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No image selected!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFile() {
        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
        File file = saveBitmapToCache(drawable.getBitmap());
        String imageBase64 = imageUtils.pmgFileToBase64(file);
        String dz38Data = serverUtil.sendBase64ToServer(imageBase64);
        if (dz38Data != null) {
            File tmpFile = imageUtils.base64ToPng(dz38Data);
            BluetoothNusSendFile nusFileSender = new BluetoothNusSendFile(this,bluetoothAdapter, deviceAddress);
            nusFileSender.sendPngFile(tmpFile);
        } else {
            Toast.makeText(MainActivity.this, "Empty result from https server", Toast.LENGTH_SHORT).show();
        }
    }



    private void initializeBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, continue with Bluetooth operations.
                initializeBluetooth();
            } else {
                // Permission was denied. Inform the user that the permission is necessary.
                Toast.makeText(this, "Bluetooth permission is necessary to connect to devices.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public File saveBitmapToCache(Bitmap bitmap) {
        File cachePath = new File(this.getCacheDir(), "images");
        if (!cachePath.exists()) {
            cachePath.mkdirs(); // Make sure the directory exists
        }
        File file = new File(cachePath, "temp_image.png");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }


}
