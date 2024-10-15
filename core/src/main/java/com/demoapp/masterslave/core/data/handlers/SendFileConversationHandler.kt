package com.demoapp.masterslave.core.data.handlers

import com.demoapp.masterslave.core.data.handlers.states.SendFileState
import com.demoapp.masterslave.core.data.socket.MasterSocketService
import com.demoapp.masterslave.core.domain.model.VideoFile
import java.io.File
import java.net.Socket

class SendFileConversationHandler(
    private val masterSocketHelper: MasterSocketService,
    private val socket: Socket,
    private val videos: List<VideoFile>,
    private val onSendingProgress: (Int) -> Unit,
    private val onSuccess: () -> Unit
) {
    private var currentState: SendFileState = SendFileState.NotifySlave
    private var currentFileIndex = 0

    suspend fun startConversation() {
        processState(currentState)
    }

    private suspend fun processState(state: SendFileState) {
        when (state) {
            is SendFileState.NotifySlave -> {
                masterSocketHelper.sendMessage(socket, "READY_TO_SEND")
                currentState = SendFileState.SlaveReady
                processState(currentState)
            }

            is SendFileState.SlaveReady -> {
                val receivedMessage = masterSocketHelper.receiveMessage<String>(socket)
                if (receivedMessage == "READY") {
                    sendNextFile()
                } else {
                    handleUnexpectedMessage(receivedMessage ?: "null")
                }
            }

            is SendFileState.SendFileName -> {
                masterSocketHelper.sendMessage(socket, state.fileName)
                currentState = SendFileState.SendFileSize(videos[currentFileIndex].size)
                processState(currentState)
            }

            is SendFileState.SendFileSize -> {
                masterSocketHelper.sendMessage(socket, state.fileSize)
                currentState = SendFileState.SendLastFileStatus(currentFileIndex == videos.size - 1)
                processState(currentState)
            }

            is SendFileState.SendLastFileStatus -> {
                masterSocketHelper.sendMessage(socket, state.isLastFile)
                currentState = SendFileState.SendFile(videos[currentFileIndex])
                processState(currentState)
            }

            is SendFileState.SendFile -> state.videoFile.run {
                onSendingProgress.invoke(0)

                masterSocketHelper.sendFile(socket, File(path), onSendingProgress)

                val receivedMessage = masterSocketHelper.receiveMessage<String>(socket)
                if (receivedMessage == "FILE_RECEIVED") {
                    currentFileIndex++
                    if (currentFileIndex < videos.size) {
                        sendNextFile()
                    } else {
                        currentState = SendFileState.Finished
                        processState(currentState)
                    }
                } else {
                    handleUnexpectedMessage(receivedMessage ?: "null")
                }
            }

            is SendFileState.Finished -> {
                onSuccess()
            }
        }
    }

    private suspend fun sendNextFile() {
        val videoFile = videos[currentFileIndex]
        currentState = SendFileState.SendFileName(videoFile.name)
        processState(currentState)
    }

    private fun handleUnexpectedMessage(message: String) {
        println("Unexpected message: $message")
    }
}