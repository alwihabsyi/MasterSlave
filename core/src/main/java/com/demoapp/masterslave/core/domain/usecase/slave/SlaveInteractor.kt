package com.demoapp.masterslave.core.domain.usecase.slave

import android.net.nsd.NsdServiceInfo
import com.demoapp.masterslave.core.domain.repository.SlaveRepository
import java.net.Socket

class SlaveInteractor(private val slaveRepository: SlaveRepository) :
    SlaveUseCase {
    override fun discoverMasterService(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onConnected: (Socket) -> Unit,
        onReceivingProgress: (Int) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onError: (String, Boolean) -> Unit
    ) =
        slaveRepository.discoverMasterService(
            onDiscoveryStarted,
            onServiceFound,
            onConnected,
            onReceivingProgress,
            onAllFilesReceived,
            onError
        )

    override fun startListeningToMasterPosition(socket: Socket) =
        slaveRepository.startListeningToMasterPosition(socket)

    override fun cancelScope(masterSocket: Socket?) =
        slaveRepository.cancelScope(masterSocket)
}