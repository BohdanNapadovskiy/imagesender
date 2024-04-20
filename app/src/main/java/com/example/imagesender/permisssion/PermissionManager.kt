package com.example.imagesender.permisssion

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity, private val REQUEST_CODE: Int) {
    fun hasPermission(permission: String?): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission!!) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(permissions: Array<String?>?) {
        ActivityCompat.requestPermissions(activity, permissions!!, REQUEST_CODE)
    }

    fun verifyPermissions(grantResults: IntArray): Boolean {
        if (grantResults.size < 1) {
            return false
        }
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
