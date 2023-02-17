package com.ngam.check_device.logic

import androidx.annotation.Keep

@Keep
data class Values(
    private var root: Boolean?,
    private var usbDebug: Boolean?,
    private var emulator: Boolean?,
    private var hooking: Boolean?,
    private var signature: Boolean?
) {
    fun validate(): Boolean {
        if (root == null) {
            throw Exception("Not initialized")
        } else {
            val items = listOf(usbDebug, emulator, hooking, signature)
            val ret = items.find { item -> item != null && item }
            return if (ret != null) {
                root!! || ret
            } else {
                root!!
            }
        }
    }
}