package com.demoapp.masterslave.data.repository

import com.demoapp.masterslave.domain.repository.ClientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ClientRepositoryImpl(private val serverSocket: ServerSocket) : ClientRepository {

    override suspend fun startTcpServer(
        onConnected: (Socket, String) -> Unit,
        onFailed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val serverSocket = ServerSocket(SOCKET_PORT)

            while (true) {
                val clientSocket = serverSocket.accept()
                val hostName = clientSocket.inetAddress.hostName
                onConnected.invoke(clientSocket, hostName)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            onFailed.invoke()
        }
    }

    override fun sendFilesToClient(socket: Socket, videos: List<File>) {
        val outputStream = DataOutputStream(socket.getOutputStream())
        val inputStream = DataInputStream(socket.getInputStream())

        for ((index, video) in videos.withIndex()) {
            val isLastFile = (index == videos.size - 1)
            sendFile(video, outputStream, isLastFile)

            val ack = inputStream.readUTF()
            if (ack != "FILE_RECEIVED") {
                println("No acknowledgment from client for ${video.name}")
            }
        }

        sendTimestamp(outputStream)
    }

    private fun sendFile(video: File, outputStream: DataOutputStream, isLastFile: Boolean) {
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

    private fun sendTimestamp(outputStream: DataOutputStream) {
        val masterTime = System.currentTimeMillis()
        val playbackStartTime = masterTime + 10000

        outputStream.writeLong(masterTime)
        outputStream.writeLong(playbackStartTime)
        outputStream.flush()
    }

    override fun closeClient() {
        serverSocket.close()
    }

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
        const val SOCKET_PORT = 8989
    }
}