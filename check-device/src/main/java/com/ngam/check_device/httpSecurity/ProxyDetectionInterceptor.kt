package com.ngam.check_device.httpSecurity

import androidx.annotation.Keep
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
@Keep
class ProxyDetectionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")
        if (!proxyHost.isNullOrEmpty() && proxyPort != null) {
            throw SecurityException()
        }
        val request: Request = chain.request()
        return chain.proceed(request)
    }
}