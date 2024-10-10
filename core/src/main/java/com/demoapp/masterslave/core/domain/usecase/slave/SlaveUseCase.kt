package com.demoapp.masterslave.core.domain.usecase.slave

import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.Flow
import java.net.Socket

interface SlaveUseCase {
    fun discoverMasterService(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onConnected: (Socket) -> Unit,
        onReceivingProgress: (Int) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onError: (String, Boolean) -> Unit
    )
    fun startListeningToMasterPosition(socket: Socket): Flow<Triple<String, Long, Long>>
    fun cancelScope(masterSocket: Socket?)
}