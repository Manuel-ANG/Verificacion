package com.ngam.testverificacion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ngam.check_device.logic.CheckDevice
import com.ngam.check_device.tap.HideOverlay
import com.ngam.testverificacion.Logic.Validation
import com.ngam.testverificacion.ui.theme.TestVerificacionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestVerificacionTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    val context = LocalContext.current
    val text = rememberSaveable { mutableStateOf("") }
    try {
        val activity = context as Activity
        HideOverlay(context).apply(activity.window)
        text.value = if (CheckDevice(context).checkIsRoot().checkIsHooking().checkIsEmulador().checkIsUsbEnabled().checkOrigin().build()) {
            "tu dispositivo es vulnerable"

        } else {
            "tu dispostivo es seguro"
        }
        //Validation(context).bind()
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    Text(text = text.value)
}