package com.demoapp.masterslave.core.data.source

import com.demoapp.masterslave.core.data.helper.SocketHelper
import com.demoapp.masterslave.core.domain.model.VideoFile
import java.io.File
import java.io.IOException
import java.net.Socket

class MasterDataSource(private val socketHelper: SocketHelper) {

    suspend fun sendFilesToClient(socket: Socket, videos: List<VideoFile>, onProgress: (Int) -> Unit) {
        videos.forEachIndexed { index, video ->
            val isLastFile = index == videos.size - 1
            val file = File(video.path)
            socketHelper.sendFile(socket, file, onProgress, isLastFile)
            val ack = socketHelper.readUTFMessage(socket)
            if (ack != "FILE_RECEIVED") {
                throw IOException("File not acknowledged by client")
            }
        }
    }

    suspend fun sendPlayTimestamp(socket: Socket, videoFiles: List<VideoFile>, masterTime: Long, playbackStartTime: Long) {
        socketHelper.sendUTFMessage(socket, "TIMESTAMP_DATA")
        socketHelper.sendUTFMessage(socket, masterTime.toString())
        socketHelper.sendUTFMessage(socket, playbackStartTime.toString())
        videoFiles.forEach { video -> socketHelper.sendUTFMessage(socket, video.name) }
    }
}