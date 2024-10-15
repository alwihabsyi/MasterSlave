package com.demoapp.masterslave.core.data.socket

import com.demoapp.masterslave.core.common.SharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class MasterSocketService(
    private var serverSocket: ServerSocket,
    private var sharedState: SharedState
): BaseSocketService() {
    private var tcpServerJob: Job? = null

    private val closeAllSockets: (Socket) -> Unit = { socket ->
        if (!socket.isClosed) socket.close()
    }

    fun startTcpServer(onStatusChange: () -> Unit) {
        if (tcpServerJob == null) tcpServerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    if (serverSocket.isClosed) openServerSocket()
                    val clientSocket = serverSocket.accept()

                    sharedState.connectedClients.add(clientSocket)
                    onStatusChange()
                    launch { monitorClientDisconnection(clientSocket, onStatusChange) }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                if (isActive) onStatusChange()
            }
        }
    }

    private fun openServerSocket() = try {
        serverSocket = ServerSocket(8989)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    private suspend fun monitorClientDisconnection(
        clientSocket: Socket,
        onStatusChange: () -> Unit
    ) =
        withContext(Dispatchers.IO) {
            try {
                while (!clientSocket.isClosed) {
                    delay(3000)
                    if (!isClientConnected(clientSocket)) {
                        sharedState.connectedClients.remove(clientSocket)
                        clientSocket.close()
                        onStatusChange()
                        break
                    }
                }
            } catch (e: Exception) {
                sharedState.connectedClients.remove(clientSocket)
                clientSocket.close()
            }
        }

    private fun isClientConnected(socket: Socket): Boolean {
        return try {
            socket.sendUrgentData(0xFF)
            true
        } catch (e: IOException) {
            false
        }
    }

    fun closeSocket() = try {
        tcpServerJob?.cancel()
        tcpServerJob = null
        sharedState.connectedClients.forEach(closeAllSockets)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}