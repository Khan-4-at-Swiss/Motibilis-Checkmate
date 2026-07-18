package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Hosting : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class WifiMessage(
    val type: String, // "MOVE", "CHAT", "RESET", "RESIGN", "INIT"
    val sender: String,
    val payload: String
)

class SocketManager {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedMessage = MutableStateFlow<WifiMessage?>(null)
    val receivedMessage = _receivedMessage.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var isRunning = false

    private val scope = CoroutineScope(Dispatchers.IO)

    // Retrieve local IPv4 address
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // Skip inactive, loopback or virtual interfaces
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SocketManager", "Error getting IP address", e)
        }
        return "127.0.0.1"
    }

    // Start hosting server
    fun hostGame(port: Int = 8888) {
        scope.launch {
            try {
                closeAll()
                _connectionState.value = ConnectionState.Hosting
                serverSocket = ServerSocket(port)
                isRunning = true

                Log.d("SocketManager", "Server started on port $port, waiting for connection...")
                val socket = serverSocket?.accept() // Blocking wait
                if (socket != null) {
                    clientSocket = socket
                    setupStreams(socket)
                    _connectionState.value = ConnectionState.Connected
                    Log.d("SocketManager", "Client connected!")
                    startListening()
                }
            } catch (e: Exception) {
                if (isRunning) {
                    _connectionState.value = ConnectionState.Error("Failed to host game: ${e.localizedMessage}")
                }
            }
        }
    }

    // Connect to host
    fun joinGame(hostIp: String, port: Int = 8888) {
        scope.launch {
            try {
                closeAll()
                _connectionState.value = ConnectionState.Connecting
                
                // 5-second socket connection timeout
                val socket = Socket()
                withContext(Dispatchers.IO) {
                    socket.connect(java.net.InetSocketAddress(hostIp, port), 5000)
                }
                
                clientSocket = socket
                setupStreams(socket)
                _connectionState.value = ConnectionState.Connected
                Log.d("SocketManager", "Connected to host $hostIp:$port")
                startListening()
            } catch (e: SocketTimeoutException) {
                _connectionState.value = ConnectionState.Error("Connection timed out. Check IP and Wi-Fi connection.")
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Could not connect: ${e.localizedMessage}")
            }
        }
    }

    private fun setupStreams(socket: Socket) {
        writer = PrintWriter(socket.getOutputStream(), true)
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    }

    private fun startListening() {
        scope.launch {
            try {
                while (isRunning) {
                    val line = reader?.readLine() ?: break
                    parseAndNotify(line)
                }
            } catch (e: Exception) {
                Log.e("SocketManager", "Error reading socket stream", e)
            } finally {
                _connectionState.value = ConnectionState.Error("Connection disconnected.")
                closeAll()
            }
        }
    }

    private fun parseAndNotify(line: String) {
        // Expected format: TYPE|SENDER|PAYLOAD
        try {
            val parts = line.split("|", limit = 3)
            if (parts.size >= 3) {
                _receivedMessage.value = WifiMessage(
                    type = parts[0],
                    sender = parts[1],
                    payload = parts[2]
                )
            }
        } catch (e: Exception) {
            Log.e("SocketManager", "Failed to parse line: $line", e)
        }
    }

    fun sendMessage(type: String, sender: String, payload: String) {
        scope.launch {
            try {
                val formattedMessage = "$type|$sender|$payload"
                writer?.println(formattedMessage)
            } catch (e: Exception) {
                Log.e("SocketManager", "Error sending message", e)
            }
        }
    }

    fun resetReceivedMessage() {
        _receivedMessage.value = null
    }

    fun disconnect() {
        isRunning = false
        closeAll()
        _connectionState.value = ConnectionState.Idle
    }

    private fun closeAll() {
        try {
            reader?.close()
            writer?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("SocketManager", "Error closing sockets", e)
        } finally {
            reader = null
            writer = null
            clientSocket = null
            serverSocket = null
        }
    }
}
