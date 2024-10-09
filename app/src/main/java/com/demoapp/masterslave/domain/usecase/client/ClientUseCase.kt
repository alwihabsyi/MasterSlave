package com.demoapp.masterslave.domain.usecase.client

import java.io.File
import java.net.Socket

interface ClientUseCase {
    suspend fun startTcpServer(onConnected: (Socket, String) -> Unit, onFailed: () -> Unit)
    fun sendFilesToClient(socket: Socket, videos: List<File>)
    fun closeClient()
}