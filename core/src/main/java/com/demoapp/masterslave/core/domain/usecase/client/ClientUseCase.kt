package com.demoapp.masterslave.core.domain.usecase.client

import com.demoapp.masterslave.core.domain.model.VideoFile
import java.net.Socket

interface ClientUseCase {
    suspend fun startTcpServer(onConnected: (Socket, String) -> Unit, onFailed: () -> Unit)
    suspend fun registerNsdService(onRegistered: (String) -> Unit)
    suspend fun sendFilesToClient(
        socket: Socket,
        videos: List<VideoFile>,
        onSuccess: () -> Unit,
        onSendingProgress: (Int) -> Unit
    )
    suspend fun sendPlayTimeStamp(
        socket: Socket,
        videoFiles: List<VideoFile>,
        playbackStartTime: Long,
        masterTime: Long,
        onSuccess: () -> Unit
    )
    suspend fun sendVideoTimeStamp(
        socket: Socket,
        video: String,
        masterPosition: Long,
        masterTimestamp: Long
    )
    fun closeClient()
}