package com.shahmdmahi.hpctv.data.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.shahmdmahi.hpctv.data.model.DiscoveredServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

class ServerDiscoveryManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("hpc_tv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val UDP_PORT = 8888
        private const val DISCOVERY_PACKET = "HPC_DISCOVER_SERVER"
        private const val PREF_SAVED_IP = "saved_server_ip"
        private const val PREF_SAVED_PORT = "saved_server_port"
        private const val PREF_SAVED_SSL = "saved_server_ssl"
    }

    fun getSavedServer(): DiscoveredServer? {
        val ip = prefs.getString(PREF_SAVED_IP, null) ?: return null
        val port = prefs.getInt(PREF_SAVED_PORT, 3000)
        val ssl = prefs.getBoolean(PREF_SAVED_SSL, true)
        return DiscoveredServer(
            service = "hpc-clinic-server",
            ip = ip,
            port = port,
            ssl = ssl,
            name = "HPC Center"
        )
    }

    fun saveServer(server: DiscoveredServer) {
        prefs.edit()
            .putString(PREF_SAVED_IP, server.ip)
            .putInt(PREF_SAVED_PORT, server.port)
            .putBoolean(PREF_SAVED_SSL, server.ssl)
            .apply()
    }

    /**
     * Broadcasts UDP packet to 255.255.255.255 and subnet broadcast addresses on port 8888.
     */
    suspend fun discoverServer(timeoutMs: Long = 3500): DiscoveredServer? = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("hpc_udp_lock")?.apply {
            setReferenceCounted(true)
            acquire()
        }

        try {
            withTimeoutOrNull(timeoutMs) {
                var socket: DatagramSocket? = null
                try {
                    socket = DatagramSocket().apply {
                        broadcast = true
                        soTimeout = timeoutMs.toInt()
                    }

                    val sendData = DISCOVERY_PACKET.toByteArray(Charsets.UTF_8)
                    
                    // 1. Send to global broadcast 255.255.255.255
                    try {
                        val globalBroadcast = InetAddress.getByName("255.255.255.255")
                        socket.send(DatagramPacket(sendData, sendData.size, globalBroadcast, UDP_PORT))
                    } catch (_: Exception) {}

                    // 2. Send to all local interface broadcast addresses
                    try {
                        val interfaces = NetworkInterface.getNetworkInterfaces()
                        while (interfaces.hasMoreElements()) {
                            val networkInterface = interfaces.nextElement()
                            if (networkInterface.isLoopback || !networkInterface.isUp) continue
                            for (interfaceAddress in networkInterface.interfaceAddresses) {
                                val broadcast = interfaceAddress.broadcast ?: continue
                                try {
                                    socket.send(DatagramPacket(sendData, sendData.size, broadcast, UDP_PORT))
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}

                    // 3. Receive responder payload
                    val receiveBuf = ByteArray(2048)
                    val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
                    socket.receive(receivePacket)

                    val responseStr = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                    val server = json.decodeFromString<DiscoveredServer>(responseStr)
                    saveServer(server)
                    server
                } catch (e: Exception) {
                    null
                } finally {
                    socket?.close()
                }
            }
        } finally {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }
    }
}
