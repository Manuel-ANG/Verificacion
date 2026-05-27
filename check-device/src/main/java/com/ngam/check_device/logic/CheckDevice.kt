package com.ngam.check_device.logic

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Base64
import androidx.annotation.Keep
import com.scottyab.rootbeer.RootBeer
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest

@Keep
class CheckDevice(private val context: Context) {
    private var report = persistentMapOf<String, Boolean>()

    fun checkIsRoot() = apply {
        report = report.put("isRoot", RootBeer(context).isRooted || checkRootMethod1() || checkRootMethod2() || checkRootMethod3())
    }

    fun checkIsUsbEnabled() = apply {
        report = report.put("isUsbEnabled", usbDebug())
    }

    fun checkIsEmulador() = apply {
        report = report.put("isEmulator", isEmulator())
    }

    fun checkIsHooking() = apply {
        report = report.put(
            "isHooking",
            detected() || detectFrida() || detectFridaMethod2() || detectXposed() || detectSuspiciousLibraries() || blockPtrace()
        )
    }

    fun checkSignature(signature: String) = apply {
        report = report.put("isValidSignaure", validateSignature(signature))
    }

    fun checkOrigin() = apply {
        report = report.put("isUnknowOrigin", unknownOrigin())
    }

    suspend fun build(): Boolean {
        return withContext(Dispatchers.IO) {
            report.values.any { it }
        }
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val inReader = BufferedReader(InputStreamReader(process.inputStream))
            inReader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    private fun usbDebug(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1 || Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }

    private fun isEmulator(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger() || checkBuildConfig() || checkEmulatorFiles()
    }

    private fun unknownOrigin(): Boolean {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        return installer != "com.android.vending"
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
        try {
            throw Exception("Stack Trace Check")
        } catch (e: Exception) {
            for (stackElement in e.stackTrace) {
                val className = stackElement.className.lowercase()
                if (className.contains("com.saurik.substrate") ||
                    className.contains("de.robv.android.xposed") ||
                    className.contains("frida")
                ) {
                    return true
                }
            }
        }
        return false
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

    private fun detectXposed(): Boolean {
        try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                val reader = BufferedReader(InputStreamReader(file.inputStream()))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("XposedBridge.jar", ignoreCase = true) == true) {
                        return true
                    }
                }
            }
            Class.forName("de.robv.android.xposed.XposedBridge")
            return true
        } catch (e: ClassNotFoundException) {
        } catch (e: Exception) {
        }
        return false
    }

    private fun detectFrida(): Boolean {
        val processes = listOf("frida-server", "frida-agent")
        for (process in processes) {
            try {
                val processBuilder = ProcessBuilder("ps")
                val processList = processBuilder.start().inputStream.bufferedReader().readText()
                if (processList.contains(process))
                    return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun detectFridaMethod2(): Boolean {
        try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                val reader = BufferedReader(InputStreamReader(file.inputStream()))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("frida", ignoreCase = true) == true) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar errores
        }
        return false
    }

    private fun detectSuspiciousLibraries(): Boolean {
        val suspiciousLibs = listOf("frida", "xposed", "substrate")
        val maps = File("/proc/self/maps")
        if (maps.exists()) {
            val content = maps.readText()
            for (lib in suspiciousLibs) {
                if (content.contains(lib, ignoreCase = true))
                    return true
            }
        }
        return false
    }

    private fun checkTracerPid(): Int {
        return try {
            val statusFile = File("/proc/self/status")
            if (statusFile.exists()) {
                val content = statusFile.readText()
                val lines = content.split("\n")
                for (line in lines) {
                    if (line.startsWith("TracerPid:")) {
                        val tracerPid = line.substringAfter(":").trim()
                        return tracerPid.toIntOrNull() ?: 0
                    }
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }

    private fun checkDebugFlags(): Boolean {
        return try {
            val statusFile = File("/proc/self/status")
            if (statusFile.exists()) {
                val content = statusFile.readText()
                val lines = content.split("\n")
                for (line in lines) {
                    if (line.startsWith("State:")) {
                        val state = line.substringAfter(":").trim()
                        return state.contains("T") || state.contains("t")
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkPtraceStatus(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/self/status")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readText()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun checkDebugFiles(): Boolean {
        return try {
            val debugFiles = listOf(
                "/proc/self/fd/0",
                "/proc/self/fd/1",
                "/proc/self/fd/2"
            )

            for (file in debugFiles) {
                val debugFile = File(file)
                if (debugFile.exists()) {
                    val target = debugFile.canonicalPath
                    if (target.contains("gdb") || target.contains("lldb") || target.contains("debugger")) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkDebugEnvironment(): Boolean {
        return try {
            val debugVars = listOf(
                "ANDROID_DEBUGGABLE",
                "JAVA_DEBUG",
                "DEBUG"
            )

            for (varName in debugVars) {
                val value = System.getenv(varName)
                if (value != null && value.isNotEmpty()) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun blockPtrace(): Boolean {
        return try {
            val tracerPid = checkTracerPid()
            val debuggerAttached = Debug.isDebuggerConnected()
            val debugFlags = checkDebugFlags()
            val ptraceStatus = checkPtraceStatus()
            val debugFiles = checkDebugFiles()
            val debugEnv = checkDebugEnvironment()
            val isBeingDebugged =
                tracerPid > 0 || debuggerAttached || debugFlags || ptraceStatus || debugFiles || debugEnv
            if (isBeingDebugged) {
                false
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}