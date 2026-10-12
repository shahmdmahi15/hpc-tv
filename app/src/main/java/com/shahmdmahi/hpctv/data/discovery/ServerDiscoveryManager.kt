package com.shahmdmahi.hpctv.data.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.shahmdmahi.hpctv.data.model.DiscoveredServer
import com.shahmdmahi.hpctv.data.network.HpcNetworkClient
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.net.*

/**
 * Intelligent Multi-Tier LAN Auto-Discovery Engine for Android TV.
 * Designed specifically for dynamically rotating DHCP clinic IPs (e.g. 192.168.2.10 today, 192.168.10.124 tomorrow).
 *
 * Tier 1: UDP Broadcast / Multicast on port 8888 (0.1s - 0.8s)
 * Tier 2: Instant verification of previous day's saved IP (0.2s)
 * Tier 3: High-concurrency parallel local subnet sweep across active interface (1.0s - 1.8s)
 * Tier 4: Cross-subnet sweep for standard clinic ranges (192.168.2, 192.168.10, 192.168.1, 192.168.0)
 */
class ServerDiscoveryManager(private val context: Context) {
    private val tag = "ServerDiscovery"
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("hpc_tv_prefs", Context.MODE_PRIVATE)
    private val client by lazy { HpcNetworkClient.getClient(context) }

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
            name = "Health & Pain Care Center"
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
     * Executes the comprehensive multi-tier auto-discovery pipeline.
     */
    suspend fun autoDiscoverServer(
        onProgress: ((String) -> Unit)? = null
    ): DiscoveredServer? = withContext(Dispatchers.IO) {
        // -------------------------------------------------------------
        // TIER 1: UDP Broadcast Probe (Fastest, ~300ms)
        // -------------------------------------------------------------
        onProgress?.invoke("Searching for clinic server via UDP broadcast...")
        val udpServer = discoverServerViaUdp(timeoutMs = 1200)
        if (udpServer != null) {
            Log.i(tag, "Tier 1: Found server via UDP at ${udpServer.ip}:${udpServer.port}")
            saveServer(udpServer)
            return@withContext udpServer
        }

        // -------------------------------------------------------------
        // TIER 2: Check Yesterday's Cached IP (~300ms)
        // -------------------------------------------------------------
        val saved = getSavedServer()
        if (saved != null) {
            onProgress?.invoke("Checking previous server address (${saved.ip})...")
            val verifiedSaved = verifyServerAtIp(saved.ip, saved.port)
            if (verifiedSaved != null) {
                Log.i(tag, "Tier 2: Cached server still active at ${saved.ip}:${saved.port}")
                saveServer(verifiedSaved)
                return@withContext verifiedSaved
            }
        }

        // -------------------------------------------------------------
        // TIER 3: Parallel Subnet Sweep for Dynamic Rotating IP
        // -------------------------------------------------------------
        val candidateSubnets = getLocalSubnetPrefixes()
        Log.i(tag, "Candidate subnets for sweeping: $candidateSubnets")

        for (subnet in candidateSubnets) {
            onProgress?.invoke("Scanning local subnet ($subnet.1 - 254) for rotating server IP...")
            val found = sweepSubnet(subnet, defaultPort = 3000)
            if (found != null) {
                Log.i(tag, "Tier 3: Found server via subnet sweep at ${found.ip}:${found.port}")
                saveServer(found)
                return@withContext found
            }
        }

        Log.w(tag, "Auto-discovery exhausted without locating server.")
        null
    }

    /**
     * Broadcasts UDP packet to 255.255.255.255, multicast groups, and interface broadcast addresses.
     */
    suspend fun discoverServerViaUdp(timeoutMs: Long = 1500): DiscoveredServer? = withContext(Dispatchers.IO) {
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

                    // 1. Send to universal broadcast
                    try {
                        val globalBroadcast = InetAddress.getByName("255.255.255.255")
                        socket.send(DatagramPacket(sendData, sendData.size, globalBroadcast, UDP_PORT))
                    } catch (_: Exception) {}

                    // 2. Send to multicast discovery group 239.255.255.250
                    try {
                        val multicastGroup = InetAddress.getByName("239.255.255.250")
                        socket.send(DatagramPacket(sendData, sendData.size, multicastGroup, UDP_PORT))
                    } catch (_: Exception) {}

                    // 3. Send to all active network interface broadcasts
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

                    // 4. Receive responder payload
                    val receiveBuf = ByteArray(2048)
                    val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
                    socket.receive(receivePacket)

                    val responseStr = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                    val server = json.decodeFromString<DiscoveredServer>(responseStr)
                    server
                } catch (_: Exception) {
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

    /**
     * Ultra-fast parallel subnet sweep across a /24 subnet (e.g. "192.168.2" or "192.168.10").
     * Uses lightweight TCP socket connect test (180ms timeout) before HTTP verification,
     * sweeping all 254 hosts concurrently in ~1 second.
     */
    suspend fun sweepSubnet(
        subnetPrefix: String,
        defaultPort: Int = 3000
    ): DiscoveredServer? = coroutineScope {
        val cleanPrefix = subnetPrefix.trimEnd('.')
        // Prioritized host list: test common DHCP assignments first, then remaining hosts
        val priorityHosts = listOf(2, 10, 100, 105, 124, 46, 50, 1, 20, 30, 40, 60, 70, 80, 90, 150, 200)
        val allHosts = (priorityHosts + (1..254).filterNot { priorityHosts.contains(it) })

        val semaphore = Semaphore(64) // 64 concurrent socket tests
        var foundServer: DiscoveredServer? = null

        val jobs = allHosts.map { host ->
            async(Dispatchers.IO) {
                if (foundServer != null) return@async

                val targetIp = "$cleanPrefix.$host"
                val isOpen = semaphore.withPermit {
                    isPortOpen(targetIp, defaultPort, timeoutMs = 180)
                }

                if (isOpen && foundServer == null) {
                    val verified = verifyServerAtIp(targetIp, defaultPort)
                    if (verified != null) {
                        foundServer = verified
                    }
                }
            }
        }

        jobs.awaitAll()
        foundServer
    }

    /**
     * Checks if a raw TCP port is open and listening with a short timeout.
     */
    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int = 180): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Verifies whether an IP address is running the HPC clinic server.
     * Tests HTTPS first (port 3000), falling back to HTTP.
     */
    suspend fun verifyServerAtIp(ip: String, port: Int = 3000): DiscoveredServer? = withContext(Dispatchers.IO) {
        // Try HTTPS first
        val httpsResult = probeHttpEndpoint("https://$ip:$port", ip, port, ssl = true)
        if (httpsResult != null) return@withContext httpsResult

        // Try HTTP fallback
        val httpResult = probeHttpEndpoint("http://$ip:$port", ip, port, ssl = false)
        httpResult
    }

    private fun probeHttpEndpoint(baseUrl: String, ip: String, port: Int, ssl: Boolean): DiscoveredServer? {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/_hpc_health")
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string() ?: ""
                if (body.contains("hpc-clinic-server") || body.contains("\"app\":\"hpc\"") || body.contains("Health And Pain Care Center")) {
                    DiscoveredServer(
                        service = "hpc-clinic-server",
                        ip = ip,
                        port = port,
                        ssl = ssl,
                        name = "Health & Pain Care Center"
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Dynamically retrieves all active IPv4 subnet prefixes on the device,
     * placing the current Wi-Fi/Ethernet subnet first.
     */
    fun getLocalSubnetPrefixes(): List<String> {
        val prefixes = linkedSetOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.interfaceAddresses) {
                    val ip = addr.address
                    if (ip is Inet4Address && !ip.isLoopbackAddress) {
                        val host = ip.hostAddress ?: continue
                        val parts = host.split(".")
                        if (parts.size == 4) {
                            prefixes.add("${parts[0]}.${parts[1]}.${parts[2]}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error detecting local network interfaces: ${e.message}")
        }

        // Add standard clinic LAN subnets if not already discovered
        val fallbackSubnets = listOf("192.168.2", "192.168.10", "192.168.1", "192.168.0")
        for (sub in fallbackSubnets) {
            prefixes.add(sub)
        }

        return prefixes.toList()
    }
}
