package com.demoapp.masterslave.core.domain.usecase.client

import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.ClientRepository
import java.net.Socket

class ClientInteractor(private val clientRepository: ClientRepository):ClientUseCase {
    override suspend fun startTcpServer(
        onConnected: (Socket, String) -> Unit,
        onFailed: () -> Unit
    ) = clientRepository.startTcpServer(onConnected, onFailed)

    override suspend fun sendFilesToClient(
        socket: Socket,
        videos: List<VideoFile>,
        onSuccess: () -> Unit,
        onSendingProgress: (Int) -> Unit
    ) = clientRepository.sendFilesToClient(socket, videos, onSuccess, onSendingProgress)

    override suspend fun sendVideoTimeStamp(
        socket: Socket,
        video: String,
        masterPosition: Long,
        masterTimestamp: Long
    ) =
        clientRepository.sendVideoTimeStamp(socket, video, masterPosition, masterTimestamp)

    override suspend fun registerNsdService(onRegistered: (String) -> Unit) =
        clientRepository.registerNsdService(onRegistered)

    override suspend fun sendPlayTimeStamp(
        socket: Socket,
        videoFiles: List<VideoFile>,
        playbackStartTime: Long,
        masterTime: Long,
        onSuccess: () -> Unit
    ) =
        clientRepository.sendPlayTimeStamp(socket, videoFiles, playbackStartTime, masterTime, onSuccess)

    override fun closeClient() =
        clientRepository.closeClient()
}