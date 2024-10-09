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
        onSuccess: (List<String>, Long) -> Unit
    ) = clientRepository.sendFilesToClient(socket, videos, onSuccess)

    override suspend fun registerNsdService(onRegistered: (String) -> Unit) =
        clientRepository.registerNsdService(onRegistered)

    override fun closeClient() =
        clientRepository.closeClient()
}