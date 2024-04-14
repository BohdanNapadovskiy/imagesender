package com.example.imagesender.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.imagesender.R


class ImageSelectorActivity : AppCompatActivity() {
    private var imageViewSelected: ImageView? = null
    private val imagePickerLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageViewSelected!!.setImageURI(uri)
            val intent = Intent()
            intent.putExtra("selectedImageURI", uri.toString())
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_selector)
        imageViewSelected = findViewById(R.id.image_view_selected)
        val buttonSelectImage = findViewById<Button>(R.id.button_select_image)
        buttonSelectImage.setOnClickListener { v: View? -> imagePickerLauncher.launch("image/*") }
    }
}
