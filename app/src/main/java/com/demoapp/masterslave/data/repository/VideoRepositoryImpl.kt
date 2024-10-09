package com.demoapp.masterslave.data.repository

import com.demoapp.masterslave.domain.model.VideoFile
import com.demoapp.masterslave.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.ServerSocket
import java.net.Socket

class VideoRepositoryImpl(
    private val fileDirectory: File
) : VideoRepository {

    override suspend fun getVideosFromDirectory(): List<VideoFile> = withContext(Dispatchers.IO) {
        val videoFiles = fileDirectory.listFiles { file ->
            file.extension.equals("mp4", ignoreCase = true) ||
                    file.extension.equals("mkv", ignoreCase = true)
        }
        videoFiles?.map { VideoFile(it.name, it.path) } ?: emptyList()
    }

    override suspend fun sendVideosToClients(selectedVideos: List<VideoFile>, serverSocket: ServerSocket) = withContext(Dispatchers.IO) {
        try {
            val clientSocket = serverSocket.accept()
            val outputStream = DataOutputStream(clientSocket.getOutputStream())
            val inputStream = DataInputStream(clientSocket.getInputStream())

            notifySlaveBeforeSending(outputStream)

            val clientResponse = inputStream.readUTF()
            if (clientResponse == "READY") {
                for (video in selectedVideos) {
                    sendFile(video, outputStream)
                }
            }

        } catch (e: Exception) {
            println("Error sending videos: ${e.message}")
        }
    }

    private fun notifySlaveBeforeSending(outputStream: DataOutputStream) {
        outputStream.writeUTF("READY_TO_SEND")
        outputStream.flush()
    }

    private fun sendFile(video: VideoFile, outputStream: DataOutputStream) {
        val file = File(video.path)
        val fileInputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int

        outputStream.writeUTF(file.name)
        outputStream.writeLong(file.length())
        outputStream.flush()

        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }

        outputStream.flush()
        fileInputStream.close()
    }
}