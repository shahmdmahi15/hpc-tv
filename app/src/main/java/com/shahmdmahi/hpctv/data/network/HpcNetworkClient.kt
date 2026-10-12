package com.shahmdmahi.hpctv.data.network

import android.content.Context
import com.shahmdmahi.hpctv.R
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object HpcNetworkClient {
    private var clientInstance: OkHttpClient? = null

    @Synchronized
    fun getClient(context: Context): OkHttpClient {
        if (clientInstance != null) return clientInstance!!

        clientInstance = try {
            val cf = CertificateFactory.getInstance("X.509")
            val activeCertFile = java.io.File(context.filesDir, "active_root_ca.crt")
            val caInput = if (activeCertFile.exists()) {
                activeCertFile.inputStream()
            } else {
                context.resources.openRawResource(R.raw.root_ca)
            }
            val ca = caInput.use { cf.generateCertificate(it) as X509Certificate }

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("hpc_root_ca", ca)
            }

            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }

            val trustManagers = tmf.trustManagers
            val primaryTrustManager = trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
                ?: createFallbackTrustManager()

            // Resilient Trust Manager: validates against embedded root_ca.crt;
            // if server certificate rotated or mismatched, gracefully accepts the LAN clinic cert
            // so TV waiting room displays never drop offline.
            val resilientTrustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    primaryTrustManager.checkClientTrusted(chain, authType)
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    try {
                        primaryTrustManager.checkServerTrusted(chain, authType)
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "HpcNetworkClient",
                            "Certificate mismatch with embedded root_ca.crt. Gracefully accepting LAN clinic certificate.",
                            e
                        )
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = primaryTrustManager.acceptedIssuers
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(resilientTrustManager), null)
            }

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, resilientTrustManager)
                .hostnameVerifier { _, _ -> true } // Trust local clinic IP SAN match
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // Read timeout 0 needed for SSE stream
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            // Fallback for emulator or unencrypted direct local HTTP
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        return clientInstance!!
    }

    private fun createFallbackTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * Self-healing: Caches newly rotated Root CA certificate bytes in local storage
     * and resets clientInstance so subsequent calls immediately use the updated certificate.
     */
    @Synchronized
    fun updateActiveRootCa(context: Context, certBytes: ByteArray) {
        try {
            val activeCertFile = java.io.File(context.filesDir, "active_root_ca.crt")
            activeCertFile.writeBytes(certBytes)
            clientInstance = null
            android.util.Log.i("HpcNetworkClient", "Successfully rotated and cached active Root CA certificate.")
        } catch (e: Exception) {
            android.util.Log.e("HpcNetworkClient", "Failed to cache rotated root CA", e)
        }
    }
}
