package com.ngam.testverificacion.Logic

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Validation(private val context: Context) {

    @OptIn(ExperimentalEncodingApi::class)
    private fun nonce():String{
    val random = UUID.randomUUID()
    return Base64.encode(random.toString().toByteArray())
    }

    fun bind(){

        val integrity = IntegrityManagerFactory.create(context)
        integrity.requestIntegrityToken(IntegrityTokenRequest.builder().setNonce(nonce()).build())
            .addOnSuccessListener {
            val token=it.token()
                println("integrity token -> $token")
        }.addOnFailureListener {
                println("integrity error")
                it.printStackTrace()
            }
    }
}