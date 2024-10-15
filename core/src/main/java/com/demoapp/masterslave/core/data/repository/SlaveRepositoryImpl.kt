package com.demoapp.masterslave.core.data.repository

import android.net.nsd.NsdServiceInfo
import com.demoapp.masterslave.core.data.nsd.NsdService
import com.demoapp.masterslave.core.data.socket.SlaveSocketService
import com.demoapp.masterslave.core.domain.repository.SlaveRepository
import kotlinx.coroutines.flow.Flow
import java.net.Socket

@Suppress("DEPRECATION")
class SlaveRepositoryImpl(
    private val nsdService: NsdService,
    private val slaveSocketService: SlaveSocketService
) : SlaveRepository {
    override fun discoverMasterService(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onConnected: (Socket) -> Unit,
        onReceivingProgress: (Int) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onError: (String, Boolean) -> Unit
    ) {
        nsdService.discoverMasterService(
            onDiscoveryStarted,
            onError = onError,
            onServiceFound = {
                it.host.hostAddress?.let { it1 ->
                    slaveSocketService.connectToMaster(
                        it1,
                        it.port,
                        onConnected,
                        onError,
                        onAllFilesReceived,
                        onReceivingProgress
                    )
                }
            }
        )
    }

    override fun startListeningToMasterPosition(socket: Socket): Flow<Triple<String, Long, Long>> =
        slaveSocketService.startListeningToMasterPosition(socket)

    companion object {
        const val SERVICE_TYPE = "_custommaster._tcp."
        const val CONNECTION_TIMEOUT = 5000
        const val SO_TIMEOUT = 60000
        const val RETRY_DELAY = 3000L
        const val MAX_RETRY_ATTEMPTS = 5
    }

    override fun cancelScope(socket: Socket?) {
        nsdService.closeService()
        slaveSocketService.cancelScope(socket)
    }
}
