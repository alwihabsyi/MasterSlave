package com.demoapp.masterslave.core.domain.repository

import com.demoapp.masterslave.core.domain.model.VideoFile
import java.net.Socket

interface ClientRepository {
    suspend fun startTcpServer(onConnected: (Socket, String) -> Unit, onFailed: () -> Unit)
    suspend fun sendFilesToClient(socket: Socket, videos: List<VideoFile>, onSuccess: (List<String>, Long) -> Unit)
    suspend fun registerNsdService(onRegistered: (String) -> Unit)
    fun closeClient()
}