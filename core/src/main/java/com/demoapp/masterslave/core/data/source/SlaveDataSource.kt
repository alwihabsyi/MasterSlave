package com.demoapp.masterslave.core.data.source

import com.demoapp.masterslave.core.data.helper.SocketHelper
import java.io.File
import java.net.Socket

class SlaveDataSource(private val socketHelper: SocketHelper) {

    suspend fun receiveFilesFromMaster(socket: Socket, fileDirectory: File, onProgress: (Int) -> Unit): List<String> {
        val videoList = mutableListOf<String>()
        while (true) {
            val fileName = socketHelper.readUTFMessage(socket)
            val fileSize = socketHelper.readUTFMessage(socket).toLong()
            val isLastFile = socketHelper.readUTFMessage(socket).toBoolean()

            val file = File(fileDirectory, fileName)
            socketHelper.receiveFile(socket, file, fileSize, onProgress)

            socketHelper.sendUTFMessage(socket, "FILE_RECEIVED")
            videoList.add(fileName)

            if (isLastFile) break
        }
        return videoList
    }

    suspend fun receivePlayTimestamp(socket: Socket): Pair<Long, Long> {
        val masterTime = socketHelper.readUTFMessage(socket).toLong()
        val playbackStartTime = socketHelper.readUTFMessage(socket).toLong()
        return masterTime to playbackStartTime
    }

    suspend fun reconnectToMaster(socket: Socket, retries: Int, delay: Long): Boolean {
        return socketHelper.reconnect(socket, retries, delay)
    }

    fun closeConnection(socket: Socket?) {
        socketHelper.closeSocket(socket)
    }
}