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
        val items = listOf(root, usbDebug, emulator, hooking, signature)
        if (items.filterNotNull().isEmpty()) {
            throw ValidateException("Not initialized")
        } else {
            return items.find { item -> item != null && item } ?: false
        }
    }
}