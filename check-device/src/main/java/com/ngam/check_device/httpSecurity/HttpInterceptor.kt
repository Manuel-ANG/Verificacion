package com.ngam.check_device.httpSecurity

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.Keep
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

@Keep
class HttpInterceptor{

    fun certificatePinner(certificates: List<CertificateBuilder>): CertificatePinner {
        val builder = CertificatePinner.Builder()
        certificates.forEach {
            builder.add(it.apiUrl, it.certificate)
        }
        return builder.build()
    }

    fun secureInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            if (hasProxyApps(context) || hasUserCertificates()) {
                throw SecurityException()
            }
            chain.proceed(request)
        }
    }

    fun certificateInterceptor(localCertificate: String): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.isSuccessful) {
                response.handshake?.peerCertificates?.firstOrNull()?.let { cert ->
                    if (cert is X509Certificate) {
                        val validationResult = validateCertificate(
                            certificate = cert,
                            localCertificate = localCertificate
                        )
                        if (!validationResult) {
                            throw SecurityException()
                        }
                    }
                }
            }
            response
        }
    }


    private fun validateCertificate(
        certificate: X509Certificate,
        localCertificate: String
    ): Boolean {
        val hash = calculateCertificateHash(certificate)
        return if (hash.isNullOrEmpty()) {
            false
        } else {
            localCertificate == hash
        }
    }

    private fun calculateCertificateHash(certificate: X509Certificate): String? {
        return try {
            val pubKey = certificate.publicKey.encoded
            val sha256 = MessageDigest.getInstance("SHA-256").digest(pubKey)
            val base64 = Base64.getEncoder().encodeToString(sha256)
            "sha256/$base64"
        } catch (_: Exception) {
            null
        }
    }

    private fun hasProxyApps(context: Context): Boolean {
        val proxyApps = listOf(
            "com.charles.proxy",
            "com.fiddler.android",
            "com.burp.android",
            "com.proxyman.ios",
            "com.proxyman.mac",
            "com.wireless.sslstrip",
            "com.mitmproxy.android"
        )
        val packageManager = context.packageManager
        for (packageName in proxyApps) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return false
    }

    private fun hasUserCertificates(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)
            val aliases = keyStore.aliases()

            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                val cert = keyStore.getCertificate(alias) as? X509Certificate
                cert?.let {
                    if (isUserCertificate(it, alias)) {
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun isUserCertificate(
        certificate: X509Certificate,
        alias: String? = null
    ): Boolean {
        val systemIssuers = listOf(
            "Android System CA",
            "Android Debug CA",
            "Android Root CA"
        )
        val issuer = certificate.issuerDN.toString()
        if (alias != null && alias.startsWith("user:")) {
            return true
        }
        if (alias != null && alias.startsWith("system:")) {
            return false
        }

        return !systemIssuers.any { issuer.contains(it) }
    }
}