package com.demoapp.masterslave.ui.master

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.usecase.client.ClientUseCase
import com.demoapp.masterslave.core.domain.usecase.video.VideoUseCase
import kotlinx.coroutines.launch
import java.net.Socket

class MasterViewModel(
    private val clientUseCase: ClientUseCase,
    private val videoUseCase: VideoUseCase
): ViewModel() {
    val directoryVideos = videoUseCase.getVideosFromDirectory().asLiveData()

    fun startTcpServer(onStatusChange: () -> Unit) = viewModelScope.launch {
        clientUseCase.startTcpServer(onStatusChange)
    }

    fun registerNsdService(onSuccess: (String) -> Unit) = viewModelScope.launch {
        clientUseCase.registerNsdService(onSuccess)
    }

    fun sendVideosToClients(
        selectedVideos: List<VideoFile>,
        socket: Socket,
        onSuccess: () -> Unit,
        onSendingProgress: (Int) -> Unit
    ) = viewModelScope.launch {
        clientUseCase.sendFilesToClient(socket, selectedVideos, onSuccess, onSendingProgress)
    }

    fun sendPlayTimeStamp(
        socket: Socket,
        videoFiles: List<VideoFile>,
        playbackStartTime: Long,
        masterTime: Long,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        clientUseCase.sendPlayTimeStamp(socket, videoFiles, playbackStartTime, masterTime, onSuccess)
    }

    fun sendVideoTimeStamp(
        socket: Socket,
        video: String,
        masterPosition: Long,
        masterTimestamp: Long
    ) = viewModelScope.launch {
        clientUseCase.sendVideoTimeStamp(socket, video, masterPosition, masterTimestamp)
    }

    fun moveToMedia(uri: Uri) = viewModelScope.launch {
        videoUseCase.moveToMedia(uri)
    }

    fun closeSocket() = clientUseCase.closeClient()
}