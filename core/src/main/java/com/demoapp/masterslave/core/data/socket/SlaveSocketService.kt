package com.demoapp.masterslave.core.data.socket

import com.demoapp.masterslave.core.data.handlers.SlaveConversationHandler
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl.Companion.CONNECTION_TIMEOUT
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl.Companion.MAX_RETRY_ATTEMPTS
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl.Companion.RETRY_DELAY
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl.Companion.SO_TIMEOUT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

@Suppress("KotlinConstantConditions")
class SlaveSocketService(
    private val fileDirectory: File
): BaseSocketService() {
    private var connectToMasterJob: Job? = null
    private var masterPositionListenerJob: Job? = null

    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    fun connectToMaster(
        hostAddress: String,
        port: Int,
        onConnected: (Socket) -> Unit,
        onError: (String, Boolean) -> Unit,
        onAllFilesReceived: (List<String>, Long) -> Unit,
        onReceivingProgress: (Int) -> Unit
    ) {
        if (connectToMasterJob == null) connectToMasterJob = CoroutineScope(Dispatchers.IO).launch {
            var attempt = 0
            while (isActive && attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    val socket = Socket().apply {
                        connect(InetSocketAddress(hostAddress, port), CONNECTION_TIMEOUT)
                        soTimeout = SO_TIMEOUT
                    }.also(onConnected)

                    val slaveConversationHandler = SlaveConversationHandler(
                        this@SlaveSocketService,
                        socket,
                        fileDirectory,
                        onReceivingProgress,
                        onAllFilesReceived,
                        if (isActive) onError else {_,_ ->}
                    )

                    slaveConversationHandler.startConversation()
                    break
                } catch (e: IOException) {
                    attempt++
                    println("Connection attempt $attempt failed: ${e.message}")
                    if (attempt >= MAX_RETRY_ATTEMPTS) {
                        onError.invoke(
                            "Failed to connect to master after $MAX_RETRY_ATTEMPTS attempts",
                            false
                        )
                        break
                    }
                    delay(RETRY_DELAY)
                }
            }
        }
    }

    fun startListeningToMasterPosition(socket: Socket): Flow<Triple<String, Long, Long>> =
        callbackFlow {
            if (masterPositionListenerJob == null) masterPositionListenerJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    while (true) {
                        val message = receiveMessage<String>(socket) ?: break
                        if (message == "POSITION_UPDATE") {
                            val video = receiveMessage<String>(socket) ?: break
                            val masterPosition = receiveMessage<Long>(socket) ?: break
                            val masterTimestamp = receiveMessage<Long>(socket) ?: break

                            trySend(Triple(video, masterPosition, masterTimestamp))
                        }

                        delay(1000L)
                    }
                } catch (e: IOException) {
                    println("Connection to master lost while listening to master position.")
                }
            }

            awaitClose {
                masterPositionListenerJob?.cancel()
                masterPositionListenerJob = null
            }
        }

    private fun Closeable.closeQuietly() {
        try {
            this.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun cancelScope(socket: Socket?) {
        masterPositionListenerJob?.cancel()
        masterPositionListenerJob = null

        connectToMasterJob?.cancel()
        connectToMasterJob = null

        inputStream?.closeQuietly()
        outputStream?.closeQuietly()
        socket?.closeQuietly()
    }
}