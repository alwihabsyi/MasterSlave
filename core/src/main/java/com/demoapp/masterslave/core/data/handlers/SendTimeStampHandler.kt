package com.demoapp.masterslave.core.data.handlers

import com.demoapp.masterslave.core.data.handlers.states.SendTimeStampState
import com.demoapp.masterslave.core.data.socket.BaseSocketService
import com.demoapp.masterslave.core.domain.model.VideoFile
import java.net.Socket

class SendTimeStampHandler(
    private val baseSocketService: BaseSocketService,
    private val socket: Socket,
    private val videoFiles: List<VideoFile>,
    private val playbackStartTime: Long,
    private val onSuccess: () -> Unit,
    masterTime: Long,
) {
    private var currentState: SendTimeStampState = SendTimeStampState.SendMasterTime(masterTime)
    private var currentVideoIndex = 0

    suspend fun startConversation() {
        processState(currentState)
    }

    private suspend fun processState(state: SendTimeStampState) {
        when (state) {
            is SendTimeStampState.SendMasterTime -> {
                baseSocketService.sendMessage(socket, state.masterTime)
                currentState = SendTimeStampState.SendPlaybackTime(playbackStartTime)
                processState(currentState)
            }

            is SendTimeStampState.SendPlaybackTime -> {
                baseSocketService.sendMessage(socket, state.playbackTime)
                currentState = SendTimeStampState.SendVideoCount(videoFiles.size)
                processState(currentState)
            }

            is SendTimeStampState.SendVideoCount -> {
                baseSocketService.sendMessage(socket, state.videoCount)
                currentState = if (videoFiles.isNotEmpty()) {
                    SendTimeStampState.SendVideoName(videoFiles[currentVideoIndex].name)
                } else {
                    SendTimeStampState.TimeStampReceived
                }
                processState(currentState)
            }

            is SendTimeStampState.SendVideoName -> {
                baseSocketService.sendMessage(socket, state.videoName)
                currentVideoIndex++
                currentState = if (currentVideoIndex < videoFiles.size) {
                    SendTimeStampState.SendVideoName(videoFiles[currentVideoIndex].name)
                } else {
                    SendTimeStampState.TimeStampReceived
                }
                processState(currentState)
            }

            is SendTimeStampState.TimeStampReceived -> {
                val ack = baseSocketService.receiveMessage<String>(socket)
                if (ack == "TIMESTAMP_RECEIVED") {
                    onSuccess()
                } else {
                    handleUnexpectedMessage(ack ?: "No Acknowledgment")
                }
            }
        }
    }

    private fun handleUnexpectedMessage(message: String) {
        println("Unexpected message: $message")
    }
}