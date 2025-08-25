package com.ngam.check_device.httpSecurity

import androidx.annotation.Keep

@Keep
data class CertificateBuilder(
    val apiUrl:String,
    val certificate:String
)
