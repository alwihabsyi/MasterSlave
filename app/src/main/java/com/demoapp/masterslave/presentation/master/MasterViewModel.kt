package com.demoapp.masterslave.presentation.master

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demoapp.masterslave.domain.usecase.client.ClientUseCase
import kotlinx.coroutines.launch
import java.net.Socket

class MasterViewModel(
    private val clientUseCase: ClientUseCase
): ViewModel() {

    fun startTcpServer(onConnected: (Socket, String) -> Unit, onFailed: () -> Unit) = viewModelScope.launch {
        clientUseCase.startTcpServer(onConnected, onFailed)
    }
}