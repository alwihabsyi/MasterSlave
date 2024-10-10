package com.demoapp.masterslave.core.data.repository

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.demoapp.masterslave.core.common.SharedState
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.ClientRepository
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl.Companion.MAX_RETRY_ATTEMPTS
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl.Companion.RETRY_DELAY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class ClientRepositoryImpl(
    private val serverSocket: ServerSocket,
    private val nsdManager: NsdManager,
    private val sharedState: SharedState
) : ClientRepository {
    private var registrationListener: NsdManager.RegistrationListener? = null

    override suspend fun startTcpServer(
        onConnected: (Socket, String) -> Unit,
        onFailed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            while (true) {
                val clientSocket = serverSocket.accept()
                Log.i("TCP SERVER","CLIENT IS CONNECTING: ${serverSocket.inetAddress.hostAddress}")
                sharedState.connectedClients.add(clientSocket)
                val hostName = clientSocket.inetAddress.hostName
                onConnected(clientSocket, hostName)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            onFailed()
        }
    }

    override suspend fun registerNsdService(onRegistered: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "MasterService"
                serviceType = SERVICE_TYPE
                port = SOCKET_PORT
            }

            registrationListener = nsdListener(onRegistered)
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun nsdListener(onRegistered: ((String) -> Unit)? = null) = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) { onRegistered?.invoke(serviceInfo.serviceName) }
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }

    override suspend fun sendFilesToClient(
        socket: Socket,
        videos: List<VideoFile>,
        onSuccess: () -> Unit,
        onSendingProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val outputStream = DataOutputStream(socket.getOutputStream())
            val inputStream = DataInputStream(socket.getInputStream())

            notifySlaveBeforeSending(outputStream)

            if (inputStream.readUTF() == "READY") {
                sendAllFiles(videos, inputStream, outputStream, onSendingProgress)
            }

            onSuccess()
        } catch (e: IOException) {
            println("Error during file transfer: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun notifySlaveBeforeSending(outputStream: DataOutputStream) {
        outputStream.writeUTF("READY_TO_SEND")
        outputStream.flush()
    }

    private fun sendAllFiles(
        videos: List<VideoFile>,
        inputStream: DataInputStream,
        outputStream: DataOutputStream,
        onSendingProgress: (Int) -> Unit
    ) {
        for ((index, video) in videos.withIndex()) {
            val isLastFile = (index == videos.size - 1)
            sendFile(video, outputStream, isLastFile, onSendingProgress)

            val ack = inputStream.readUTF()
            if (ack != "FILE_RECEIVED") {
                println("No acknowledgment from client for ${video.name}")
            }
        }
    }

    private fun sendFile(videoFile: VideoFile, outputStream: DataOutputStream, isLastFile: Boolean, onSendingProgress: (Int) -> Unit) {
        val file = File(videoFile.path)
        val fileSize = file.length()
        var totalBytesSent = 0L
        onSendingProgress.invoke(0)

        outputStream.writeUTF(file.name)
        outputStream.writeLong(fileSize)
        outputStream.writeBoolean(isLastFile)
        outputStream.flush()

        FileInputStream(file).use { fileInputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesSent += bytesRead
                onSendingProgress.invoke((totalBytesSent * 100 / fileSize).toInt())
            }

            outputStream.flush()
        }
    }

    override suspend fun sendPlayTimeStamp(
        socket: Socket,
        videoFiles: List<VideoFile>,
        playbackStartTime: Long,
        masterTime: Long,
        onSuccess: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val inputStream = DataInputStream(socket.getInputStream())
        val outputStream = DataOutputStream(socket.getOutputStream())
        val videoList = videoFiles.map { it.name }

        outputStream.writeLong(masterTime)
        outputStream.writeLong(playbackStartTime)
        outputStream.writeInt(videoList.size)

        for (video in videoList) {
            outputStream.writeUTF(video)
        }

        outputStream.flush()

        val ack = inputStream.readUTF()
        if (ack == "TIMESTAMP_RECEIVED") onSuccess()
    }

    override suspend fun sendVideoTimeStamp(socket: Socket, video: String, masterPosition: Long, masterTimestamp: Long) = withContext(Dispatchers.IO) {
        try {
            val outputStream = DataOutputStream(socket.getOutputStream())
            outputStream.writeUTF("POSITION_UPDATE")
            outputStream.writeUTF(video)
            outputStream.writeLong(masterTimestamp)
            outputStream.writeLong(masterPosition)
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            reconnectToClient(socket)
        }
    }

    @Suppress("KotlinConstantConditions")
    private suspend fun reconnectToClient(disconnectedSocket: Socket) = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                Socket(disconnectedSocket.inetAddress.hostAddress, SOCKET_PORT)
                println("Reconnected to the client.")
                break
            } catch (e: IOException) {
                attempt++
                println("Reconnection attempt $attempt failed.")
                e.printStackTrace()
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    println("Max reconnection attempts reached.")
                    break
                }
                delay(RETRY_DELAY)
            }
        }
    }

    override fun closeClient() {
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val SERVICE_TYPE = "_custommaster._tcp."
        const val SOCKET_PORT = 8989
    }
}