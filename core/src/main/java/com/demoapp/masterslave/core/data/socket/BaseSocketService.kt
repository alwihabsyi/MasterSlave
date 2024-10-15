package com.demoapp.masterslave.core.data.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

@Suppress("UNCHECKED_CAST")
open class BaseSocketService {
    suspend fun <T> sendMessage(socket: Socket, message: T) = withContext(Dispatchers.IO) {
        try {
            val outputStream = ObjectOutputStream(socket.getOutputStream())
            outputStream.writeObject(message)
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun <T> sendMessages(socket: Socket, messages: List<T>) = withContext(Dispatchers.IO) {
        try {
            val outputStream = ObjectOutputStream(socket.getOutputStream())
            for (message in messages) {
                outputStream.writeObject(message)
            }
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun <T> receiveMessage(socket: Socket): T? = withContext(Dispatchers.IO) {
        try {
            val inputStream = ObjectInputStream(socket.getInputStream())
            inputStream.readObject() as T
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun receiveFile(socket: Socket, file: File, fileSize: Long, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val inputStream = DataInputStream(socket.getInputStream())
            FileOutputStream(file).use { fileOutput ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesReceived: Long = 0

                while (totalBytesReceived < fileSize && isActive) {
                    bytesRead = inputStream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break

                    fileOutput.write(buffer, 0, bytesRead)
                    totalBytesReceived += bytesRead
                    onProgress((totalBytesReceived * 100 / fileSize).toInt())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun sendFile(socket: Socket, file: File, onSendingProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val outputStream = DataOutputStream(socket.getOutputStream())
        val fileSize = file.length()
        var totalBytesSent = 0L

        onSendingProgress.invoke(0)

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
}
