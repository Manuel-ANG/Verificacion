package com.ngam.check_device.tap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.Window
import androidx.core.app.ActivityCompat

class HideOverlay(private val context: Context) {
    fun apply(window: Window){
        if (ActivityCompat.checkSelfPermission(context,Manifest.permission.HIDE_OVERLAY_WINDOWS)==PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setHideOverlayWindows(true)
            }
        }
    }
}