package com.demoapp.masterslave.domain.usecase.client

import com.demoapp.masterslave.domain.repository.ClientRepository
import java.io.File
import java.net.Socket

class ClientInteractor(private val clientRepository: ClientRepository): ClientUseCase {
    override suspend fun startTcpServer(
        onConnected: (Socket, String) -> Unit,
        onFailed: () -> Unit
    ) = clientRepository.startTcpServer(onConnected, onFailed)

    override fun sendFilesToClient(socket: Socket, videos: List<File>) =
        clientRepository.sendFilesToClient(socket, videos)
}