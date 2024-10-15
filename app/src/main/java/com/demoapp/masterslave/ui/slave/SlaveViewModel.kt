package com.demoapp.masterslave.ui.slave

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.demoapp.masterslave.core.domain.usecase.slave.SlaveUseCase
import java.net.Socket

class SlaveViewModel(
    private val slaveUseCase: SlaveUseCase
) : ViewModel() {

    fun discoverMasterService(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onConnected: (Socket) -> Unit,
        onReceivingProgress: (Int) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onError: (String, Boolean) -> Unit
    ) {
        slaveUseCase.discoverMasterService(
            onDiscoveryStarted,
            onServiceFound,
            onConnected,
            onReceivingProgress,
            onAllFilesReceived,
            onError
        )
    }

    fun startListeningToMasterPosition(socket: Socket) = slaveUseCase.startListeningToMasterPosition(socket).asLiveData()

    fun cancelScope(masterSocket: Socket?) = slaveUseCase.cancelScope(masterSocket)
}