package com.demoapp.masterslave.core.repository

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.demoapp.masterslave.core.common.SharedState
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.ClientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ClientRepositoryImpl(
    private val serverSocket: ServerSocket,
    private val nsdManager: NsdManager,
    private val sharedState: SharedState
) : ClientRepository {


    override suspend fun startTcpServer(
        onConnected: (Socket, String) -> Unit,
        onFailed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            while (true) {
                if (serverSocket.localPort in sharedState.connectedClients.map { it.localPort }) return@withContext

                val clientSocket = serverSocket.accept()
                sharedState.connectedClients.add(clientSocket)
                val hostName = clientSocket.inetAddress.hostName
                onConnected.invoke(clientSocket, hostName)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            onFailed.invoke()
        }
    }

    override suspend fun registerNsdService(onRegistered: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val port = serverSocket.localPort

            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "MasterService"
                serviceType = SERVICE_TYPE
                setPort(port)
            }

            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                        onRegistered.invoke(serviceInfo.serviceName)
                    }

                    override fun onRegistrationFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int
                    ) {
                    }

                    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                    override fun onUnregistrationFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int
                    ) {
                    }
                })
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override suspend fun sendFilesToClient(
        socket: Socket,
        videos: List<VideoFile>,
        onSuccess: (List<String>, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val outputStream = DataOutputStream(socket.getOutputStream())
            val inputStream = DataInputStream(socket.getInputStream())

            notifySlaveBeforeSending(outputStream)

            if (inputStream.readUTF() == "READY") {
                for ((index, video) in videos.withIndex()) {
                    val isLastFile = (index == videos.size - 1)
                    sendFile(video, outputStream, isLastFile)

                    val ack = inputStream.readUTF()
                    if (ack != "FILE_RECEIVED") {
                        println("No acknowledgment from client for ${video.name}")
                    }
                }
            }

            sendTimestamp(outputStream, videos, onSuccess)
        } catch (e: IOException) {
            println("Error during file transfer: ${e.message}")
            e.printStackTrace()
            socket.close()
        }
    }

    private fun notifySlaveBeforeSending(outputStream: DataOutputStream) {
        outputStream.writeUTF("READY_TO_SEND")
        outputStream.flush()
    }

    private fun sendFile(v: VideoFile, outputStream: DataOutputStream, isLastFile: Boolean) {
        val video = File(v.path)
        val fileInputStream = FileInputStream(video)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        val fileSize = video.length()

        outputStream.writeUTF(video.name)
        outputStream.writeLong(fileSize)
        outputStream.writeBoolean(isLastFile)
        outputStream.flush()

        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }

        outputStream.flush()
        fileInputStream.close()
    }

    private fun sendTimestamp(
        outputStream: DataOutputStream,
        videos: List<VideoFile>,
        onSuccess: (List<String>, Long) -> Unit
    ) {
        val masterTime = System.currentTimeMillis()
        val playbackStartTime = masterTime + 10000
        val videoList = videos.map { it.name }

        outputStream.writeLong(masterTime)
        outputStream.writeLong(playbackStartTime)
        outputStream.writeInt(videoList.size)

        for (video in videoList) {
            outputStream.writeUTF(video)
        }

        outputStream.flush()
        onSuccess.invoke(videoList, playbackStartTime)
    }

    override fun closeClient() {
        serverSocket.close()
    }

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
    }
}