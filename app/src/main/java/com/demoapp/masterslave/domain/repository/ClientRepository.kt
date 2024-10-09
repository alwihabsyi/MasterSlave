package com.demoapp.masterslave.domain.repository

import java.io.File
import java.net.Socket

interface ClientRepository {
    suspend fun startTcpServer(onConnected: (Socket, String) -> Unit, onFailed: () -> Unit)
    fun sendFilesToClient(socket: Socket, videos: List<File>)
    fun closeClient()
}