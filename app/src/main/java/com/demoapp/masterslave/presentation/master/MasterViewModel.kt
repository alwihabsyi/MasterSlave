package com.demoapp.masterslave.presentation.master

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
    videoUseCase: VideoUseCase
): ViewModel() {
    val directoryVideos = videoUseCase.getVideosFromDirectory().asLiveData()

    fun startTcpServer(onConnected: (Socket, String) -> Unit, onFailed: () -> Unit) = viewModelScope.launch {
        clientUseCase.startTcpServer(onConnected, onFailed)
    }

    fun registerNsdService(onSuccess: (String) -> Unit) = viewModelScope.launch {
        clientUseCase.registerNsdService(onSuccess)
    }

    fun sendVideosToClients(selectedVideos: List<VideoFile>, socket: Socket, onSuccess: (List<String>, Long) -> Unit) = viewModelScope.launch {
        clientUseCase.sendFilesToClient(socket, selectedVideos, onSuccess)
    }

    fun closeSocket() = clientUseCase.closeClient()
}