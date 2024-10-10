@file:Suppress("KotlinConstantConditions")

package com.demoapp.masterslave.core.data.repository

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.demoapp.masterslave.core.domain.repository.SlaveRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket

@Suppress("DEPRECATION")
class SlaveRepositoryImpl(
    private val nsdManager: NsdManager,
    private val fileDirectory: File
) : SlaveRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    override fun discoverMasterService(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onConnected: (Socket) -> Unit,
        onReceivingProgress: (Int) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onError: (String, Boolean) -> Unit
    ) {
        nsdManager.discoverServices(
            SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    onDiscoveryStarted.invoke()
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceType == SERVICE_TYPE) {
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                onServiceFound.invoke(serviceInfo)
                                repositoryScope.launch(Dispatchers.IO) {
                                    serviceInfo.host.hostAddress?.let {
                                        connectToMaster(
                                            it,
                                            serviceInfo.port,
                                            onConnected,
                                            onError,
                                            onAllFilesReceived,
                                            onReceivingProgress
                                        )
                                    }
                                }
                            }

                            override fun onResolveFailed(
                                serviceInfo: NsdServiceInfo,
                                errorCode: Int
                            ) {
                                onError.invoke("Failed to resolve connection", false)
                            }
                        })
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    onError.invoke("Master service lost. Reconnecting...", true)
                    repositoryScope.launch {
                        serviceInfo.host?.hostAddress?.let {
                            reconnectToMaster(
                                it,
                                serviceInfo.port,
                                onConnected,
                                onError,
                                onReceivingProgress,
                                onAllFilesReceived
                            )
                        }
                    }
                }

                override fun onDiscoveryStopped(serviceType: String) {}

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            })
    }

    private suspend fun reconnectToMaster(
        hostAddress: String,
        port: Int,
        onConnected: (Socket) -> Unit,
        onError: (String, Boolean) -> Unit,
        onReceivingProgress: (Int) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                println("Attempting to reconnect to master on attempt $attempt...")
                val socket = Socket()
                socket.connect(InetSocketAddress(hostAddress, port), CONNECTION_TIMEOUT)
                socket.soTimeout = SO_TIMEOUT

                onConnected.invoke(socket)
                receiveFilesFromMaster(onAllFilesReceived, onError, onReceivingProgress)
                println("Reconnected to master.")
                break
            } catch (e: IOException) {
                attempt++
                println("Reconnection attempt $attempt failed: ${e.message}")
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    onError.invoke(
                        "Failed to reconnect to master after $MAX_RETRY_ATTEMPTS attempts.",
                        true
                    )
                    break
                }
                delay(RETRY_DELAY)
            }
        }
    }

    private suspend fun connectToMaster(
        hostAddress: String,
        port: Int,
        onConnected: (Socket) -> Unit,
        onError: (String, Boolean) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onReceivingProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                Socket().apply {
                    connect(InetSocketAddress(hostAddress, port), CONNECTION_TIMEOUT)
                    soTimeout = SO_TIMEOUT
                }.also(initSocket(onConnected))

                receiveFilesFromMaster(onAllFilesReceived, onError, onReceivingProgress)
                break
            } catch (e: IOException) {
                attempt++
                println("Connection attempt $attempt failed: ${e.message}")
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    onError.invoke(
                        "Failed to connect to master after $MAX_RETRY_ATTEMPTS attempts",
                        false
                    )
                    break
                }
                delay(RETRY_DELAY)
            }
        }
    }

    private fun initSocket(onConnected: (Socket) -> Unit): (Socket) -> Unit = { socket ->
        inputStream = DataInputStream(socket.getInputStream())
        outputStream = DataOutputStream(socket.getOutputStream())
        onConnected.invoke(socket)
        println("Connected to master")
    }

    private suspend fun receiveFilesFromMaster(
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onError: (String, Boolean) -> Unit,
        onReceivingProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<String>()

        try {
            while (true) {
                val signal = inputStream?.readUTF() ?: break
                if (signal == "READY_TO_SEND") {
                    outputStream?.writeUTF("READY")
                    outputStream?.flush()

                    receiveFiles(inputStream!!, outputStream!!, onReceivingProgress)

                    val masterTime = inputStream?.readLong() ?: break
                    val playbackStartTime = inputStream?.readLong() ?: break

                    val slaveCurrentTime = System.currentTimeMillis()
                    val timeOffset = slaveCurrentTime - masterTime

                    val adjustedPlaybackTime = playbackStartTime + timeOffset

                    val videoCount = inputStream?.readInt() ?: break
                    for (i in 0 until videoCount) {
                        videoList.add(inputStream!!.readUTF())
                    }

                    outputStream?.writeUTF("TIMESTAMP_RECEIVED")
                    outputStream?.flush()
                    onAllFilesReceived.invoke(videoList, adjustedPlaybackTime)
                    break
                } else {
                    onError.invoke("No files to receive or incorrect signal", false)
                    break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            onError.invoke("Error receiving file or connection lost", true)
        }
    }

    private fun receiveFiles(
        inputStream: DataInputStream,
        outputStream: DataOutputStream,
        onReceivingProgress: (Int) -> Unit
    ) {
        while (true) {
            try {
                val fileName = inputStream.readUTF()
                val fileSize = inputStream.readLong()
                val isLastFile = inputStream.readBoolean()

                val directory = fileDirectory
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File(fileDirectory, fileName)
                receiveFile(inputStream, file, fileSize, onReceivingProgress)

                outputStream.writeUTF("FILE_RECEIVED")
                outputStream.flush()

                if (isLastFile) break
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }
        }
    }

    private fun receiveFile(
        inputStream: DataInputStream,
        file: File,
        fileSize: Long,
        onReceivingProgress: (Int) -> Unit
    ) {
        FileOutputStream(file).use { outputStreamFile ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesReceived = 0L
            onReceivingProgress.invoke(0)

            while (totalBytesReceived < fileSize) {
                bytesRead = inputStream.read(
                    buffer,
                    0,
                    minOf(buffer.size, (fileSize - totalBytesReceived).toInt())
                )
                if (bytesRead == -1) break

                outputStreamFile.write(buffer, 0, bytesRead)
                totalBytesReceived += bytesRead
                onReceivingProgress.invoke((totalBytesReceived * 100 / fileSize).toInt())
            }
        }
    }

    override fun startListeningToMasterPosition(socket: Socket): Flow<Triple<String, Long, Long>> =
        callbackFlow {
            withContext(Dispatchers.IO) {
                try {
                    while (true) {
                        val message = inputStream?.readUTF() ?: break
                        if (message == "POSITION_UPDATE") {
                            val video = inputStream?.readUTF() ?: break
                            val masterPosition = inputStream?.readLong() ?: break
                            val masterTimestamp = inputStream?.readLong() ?: break

                            trySend(Triple(video, masterPosition, masterTimestamp))
                        }

                        delay(1000L)
                    }
                } catch (e: IOException) {
                    println("Connection to master lost while listening to master position.")
                    repositoryScope.launch {
                        socket.inetAddress.hostAddress?.let {
                            reconnectToMaster(
                                it,
                                socket.port,
                                onConnected = {},
                                onError = { _, _ -> },
                                onReceivingProgress = {},
                                onAllFilesReceived = { _, _ -> })
                        }
                    }
                }
            }

            awaitClose {
                inputStream?.closeQuietly()
                outputStream?.closeQuietly()
            }
        }

    private fun Closeable.closeQuietly() {
        try {
            this.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val SERVICE_TYPE = "_custommaster._tcp."
        const val CONNECTION_TIMEOUT = 5000
        const val SO_TIMEOUT = 60000
        const val RETRY_DELAY = 3000L
        const val MAX_RETRY_ATTEMPTS = 5
    }

    override fun cancelScope(socket: Socket?) {
        repositoryScope.cancel()
        inputStream?.closeQuietly()
        outputStream?.closeQuietly()
        socket?.closeQuietly()
    }
}
