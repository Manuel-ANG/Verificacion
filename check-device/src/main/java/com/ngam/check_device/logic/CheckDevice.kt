package com.ngam.check_device.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Base64
import androidx.annotation.Keep
import com.scottyab.rootbeer.RootBeer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest

@Keep
class CheckDevice(private val context: Context) {
    private var isRoot: Boolean? = null
    private var isSignatureValid: Boolean? = null
    private var usbEnabled: Boolean? = null
    private var isEmulator: Boolean? = null
    private var isHooking: Boolean? = null
    private var isUnknownOrigin: Boolean? = null

    fun checkIsRoot() = apply {
        if (isRoot == null) {
            runBlocking {
                isRoot = RootBeer(context).isRooted
            }
        }
    }

    fun checkIsUsbEnabled() = apply {
        if (usbEnabled == null) {
            runBlocking {
                usbEnabled = usbDebug()
            }
        }
    }

    fun checkIsEmulador() = apply {
        if (isEmulator == null) {
            runBlocking {
                isEmulator = isEmulator()
            }
        }
    }

    fun checkIsHooking() = apply {
        if (isHooking == null) {
            runBlocking {
                isHooking = detected()
            }
        }
    }

    fun checkSignature(signature: String) = apply {
        if (isSignatureValid == null) {
            runBlocking {
                isSignatureValid = validateSignature(signature)
            }
        }
    }

    fun checkOrigin()=apply{
        if(isUnknownOrigin==null){
            runBlocking {
                isUnknownOrigin=unknownOrigin()
            }
        }
    }

    fun build(): Boolean {
        return Values(isRoot, usbEnabled, isEmulator, isHooking, isSignatureValid).validate()
    }

    private fun usbDebug(): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1 ||
                Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    }

    private fun isEmulator(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger() || checkBuildConfig() || checkEmulatorFiles()
    }

    private fun unknownOrigin():Boolean{
        return Settings.Global.getInt(context.contentResolver, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
    }

    fun algo(activity: Activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                activity.startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                    Uri.parse(String.format("package:%s", context.packageName))), 1);
            } else {
                //permiso para instalar apps concedido
            }
        } else {
            println("ALGO PASA ACA")
        }
    }

    private fun checkBuildConfig(): Boolean {
        return (Build.MANUFACTURER.contains("Genymotion")
                || Build.MODEL.contains("google_sdk", true)
                || Build.MODEL.lowercase().contains("droid4x")
                || Build.MODEL.contains("sdk_gphone", true)
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built", true)
                || Build.HARDWARE == "goldfish"
                || Build.HARDWARE == "vbox86"
                || Build.HARDWARE.lowercase().contains("nox")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.PRODUCT == "sdk"
                || Build.PRODUCT == "google_sdk"
                || Build.PRODUCT == "sdk_x86"
                || Build.PRODUCT == "vbox86p"
                || Build.PRODUCT.lowercase().contains("nox")
                || Build.BOARD.lowercase().contains("nox")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))
    }

    private val GENY_FILES = arrayOf(
        "/dev/socket/genyd",
        "/dev/socket/baseband_genyd"
    )
    private val PIPES = arrayOf(
        "/dev/socket/qemud",
        "/dev/qemu_pipe"
    )
    private val X86_FILES = arrayOf(
        "ueventd.android_x86.rc",
        "x86.prop",
        "ueventd.ttVM_x86.rc",
        "init.ttVM_x86.rc",
        "fstab.ttVM_x86",
        "fstab.vbox86",
        "init.vbox86.rc",
        "ueventd.vbox86.rc"
    )
    private val ANDY_FILES = arrayOf(
        "fstab.andy",
        "ueventd.andy.rc"
    )
    private val NOX_FILES = arrayOf(
        "fstab.nox",
        "init.nox.rc",
        "ueventd.nox.rc"
    )

    private fun checkFiles(targets: Array<String>): Boolean {
        for (pipe in targets) {
            val file = File(pipe)
            if (file.exists()) {
                return true
            }
        }
        return false
    }

    private fun checkEmulatorFiles(): Boolean {
        return (checkFiles(GENY_FILES)
                || checkFiles(ANDY_FILES)
                || checkFiles(NOX_FILES)
                || checkFiles(X86_FILES)
                || checkFiles(PIPES))
    }

    private fun detected(): Boolean {
        var dectected = false
        try {
            throw Exception()
        } catch (e: Exception) {
            var zygoteInitCallCount = 0
            for (stackTraceElement in e.stackTrace) {
                if (stackTraceElement.className == "com.android.internal.os.ZygoteInit") {
                    zygoteInitCallCount++
                    if (zygoteInitCallCount == 2) {
                        dectected = true
                    }
                }
                if (stackTraceElement.className == "com.saurik.substrate.MS$2" && stackTraceElement.methodName == "invoked") {
                    dectected = true
                }
                if (stackTraceElement.className == "de.robv.android.xposed.XposedBridge" && stackTraceElement.methodName == "main") {
                    dectected = true
                }
                if (stackTraceElement.className == "de.robv.android.xposed.XposedBridge" && stackTraceElement.methodName == "handleHookedMethod") {
                    dectected = true
                }
            }
        }
        return dectected
    }

    private fun validateSignature(signature: String) = getAppSignature().string() != signature

    @SuppressLint("PackageManagerGetSignatures")
    private fun getAppSignature(): Signature? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName, PackageManager.PackageInfoFlags.of(getFlags().toLong())
            ).signingInfo?.apkContentsSigners?.firstOrNull()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                getFlags()
            ).signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                getFlags()
            ).signatures?.firstOrNull()
        }
    }

    private fun getFlags(): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_PERMISSIONS
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                PackageManager.GET_SIGNING_CERTIFICATES
            }

            else -> {
                PackageManager.GET_SIGNATURES
            }
        }
    }

    private fun Signature?.string(): String? {
        return if (this != null) {
            try {
                val signatureBytes = this.toByteArray()
                val digest = MessageDigest.getInstance("SHA")
                val hash = digest.digest(signatureBytes)
                Base64.encodeToString(hash, Base64.NO_WRAP)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}
