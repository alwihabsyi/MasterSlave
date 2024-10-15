package com.demoapp.masterslave.core.data.repository

import com.demoapp.masterslave.core.data.handlers.SendFileConversationHandler
import com.demoapp.masterslave.core.data.handlers.SendTimeStampHandler
import com.demoapp.masterslave.core.data.nsd.NsdService
import com.demoapp.masterslave.core.data.socket.MasterSocketService
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.ClientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

class ClientRepositoryImpl(
    private val nsdService: NsdService,
    private val masterSocketService: MasterSocketService
) : ClientRepository {

    override suspend fun startTcpServer(onStatusChange: () -> Unit) =
        masterSocketService.startTcpServer(onStatusChange)

    override suspend fun registerNsdService(onRegistered: (String) -> Unit) =
        nsdService.registerNsdService(onRegistered)

    override suspend fun sendFilesToClient(
        socket: Socket,
        videos: List<VideoFile>,
        onSuccess: () -> Unit,
        onSendingProgress: (Int) -> Unit
    ) {
        val sendFileConversationHandler =
            SendFileConversationHandler(masterSocketService, socket, videos, onSendingProgress, onSuccess)

        sendFileConversationHandler.startConversation()
    }

    override suspend fun sendPlayTimeStamp(
        socket: Socket,
        videoFiles: List<VideoFile>,
        playbackStartTime: Long,
        masterTime: Long,
        onSuccess: () -> Unit
    ) {
        val sendTimeStampHandler = SendTimeStampHandler(
            masterSocketService,
            socket,
            videoFiles,
            playbackStartTime,
            onSuccess,
            masterTime
        )

        sendTimeStampHandler.startConversation()
    }

    override suspend fun sendVideoTimeStamp(
        socket: Socket,
        video: String,
        masterPosition: Long,
        masterTimestamp: Long
    ) = withContext(Dispatchers.IO) {
        val messages = listOf(
            "POSITION_UPDATE",
            video,
            masterTimestamp,
            masterPosition
        )

        masterSocketService.sendMessages(socket, messages)
    }

    override fun closeClient() {
        masterSocketService.closeSocket()
        nsdService.closeService()
    }

    companion object {
        const val SERVICE_TYPE = "_custommaster._tcp."
        const val SOCKET_PORT = 8989
    }
}
