package com.demoapp.masterslave.core.data.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class SocketHelper {

    suspend fun sendUTFMessage(socket: Socket, message: String) = withContext(Dispatchers.IO) {
        DataOutputStream(socket.getOutputStream()).apply {
            writeUTF(message)
            flush()
        }
    }

    suspend fun readUTFMessage(socket: Socket): String = withContext(Dispatchers.IO) {
        DataInputStream(socket.getInputStream()).readUTF()
    }

    suspend fun sendFile(socket: Socket, file: File, onProgress: (Int) -> Unit, isLastFile: Boolean) = withContext(Dispatchers.IO) {
        DataOutputStream(socket.getOutputStream()).apply {
            writeUTF(file.name)
            writeLong(file.length())
            writeBoolean(isLastFile)
            flush()

            FileInputStream(file).use { fileInputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesSent = 0L

                onProgress(0)
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    write(buffer, 0, bytesRead)
                    totalBytesSent += bytesRead
                    onProgress((totalBytesSent * 100 / file.length()).toInt())
                }
            }
            flush()
        }
    }

    suspend fun receiveFile(socket: Socket, file: File, fileSize: Long, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        DataInputStream(socket.getInputStream()).apply {
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(8192)
                var totalBytesReceived = 0L
                var bytesRead: Int

                onProgress(0)
                while (totalBytesReceived < fileSize) {
                    bytesRead = read(buffer)
                    if (bytesRead == -1) break
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesReceived += bytesRead
                    onProgress((totalBytesReceived * 100 / fileSize).toInt())
                }
            }
        }
    }

    suspend fun reconnect(socket: Socket, retries: Int, delay: Long): Boolean = withContext(Dispatchers.IO) {
        for (attempt in 1..retries) {
            try {
                Socket(socket.inetAddress.hostAddress, socket.port)
                return@withContext true
            } catch (e: IOException) {
                Thread.sleep(delay)
            }
        }
        return@withContext false
    }

    fun closeSocket(socket: Socket?) {
        socket?.close()
    }
}
