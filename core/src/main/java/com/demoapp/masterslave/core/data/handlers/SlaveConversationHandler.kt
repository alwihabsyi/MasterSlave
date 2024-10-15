package com.demoapp.masterslave.core.data.handlers

import com.demoapp.masterslave.core.data.handlers.states.FileTransferState
import com.demoapp.masterslave.core.data.socket.SlaveSocketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.Socket

class SlaveConversationHandler(
    private val slaveSocketHelper: SlaveSocketService,
    private val socket: Socket,
    private val fileDirectory: File,
    private val onReceivingProgress: (Int) -> Unit,
    private val onAllFilesReceived: (List<String>, Long) -> Unit,
    private val onError: (String, Boolean) -> Unit
) {
    private var currentState: FileTransferState = FileTransferState.WaitingForSignal
    private val videoList = mutableListOf<String>()

    suspend fun startConversation() {
        processState(currentState)
    }

    private suspend fun processState(state: FileTransferState) {
        when (state) {
            is FileTransferState.WaitingForSignal -> {
                val signal = slaveSocketHelper.receiveMessage<String>(socket)
                if (signal == "READY_TO_SEND") {
                    slaveSocketHelper.sendMessage(socket, "READY")
                    currentState = FileTransferState.ReadyToSend
                    processState(currentState)
                } else {
                    onError("No files to receive or incorrect signal: $signal", false)
                }
            }

            is FileTransferState.ReadyToSend -> {
                receiveFiles()

                val masterTime = slaveSocketHelper.receiveMessage<Long>(socket)
                val playbackStartTime = slaveSocketHelper.receiveMessage<Long>(socket)

                val slaveCurrentTime = System.currentTimeMillis()
                val timeOffset = slaveCurrentTime - (masterTime ?: 0)

                val adjustedPlaybackTime = (playbackStartTime ?: 0) + timeOffset

                val videoCount = slaveSocketHelper.receiveMessage<Int>(socket)
                for (i in 0 until (videoCount ?: 0)) {
                    val videoName = slaveSocketHelper.receiveMessage<String>(socket)
                    videoList.add(videoName ?: "")
                }

                slaveSocketHelper.sendMessage(socket, "TIMESTAMP_RECEIVED")
                currentState = FileTransferState.Complete(videoList, adjustedPlaybackTime)
                processState(currentState)
            }

            is FileTransferState.Complete -> {
                onAllFilesReceived(state.videoList, state.playbackTime)
            }

            is FileTransferState.Error -> {
                onError(state.message, false)
            }
        }
    }

    private suspend fun receiveFiles() = withContext(Dispatchers.IO) {
        while (isActive) {
            try {
                val fileName = slaveSocketHelper.receiveMessage<String>(socket) ?: break
                val fileSize = slaveSocketHelper.receiveMessage<Long>(socket) ?: break
                val isLastFile = slaveSocketHelper.receiveMessage<Boolean>(socket) ?: break

                val file = File(fileDirectory, fileName)

                slaveSocketHelper.receiveFile(socket, file, fileSize, onReceivingProgress)
                slaveSocketHelper.sendMessage(socket, "FILE_RECEIVED")

                if (isLastFile) break
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }
        }
    }
}

