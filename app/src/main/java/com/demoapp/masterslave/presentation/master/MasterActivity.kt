package com.demoapp.masterslave.presentation.master

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.demoapp.masterslave.R
import com.demoapp.masterslave.databinding.ActivityMasterBinding
import com.demoapp.masterslave.domain.ui.VideoAdapter
import com.demoapp.masterslave.presentation.player.PlayerFragment
import com.demoapp.masterslave.utils.directoryName
import com.demoapp.masterslave.utils.toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MasterActivity : AppCompatActivity() {

    private var _binding: ActivityMasterBinding? = null
    private val binding get() = _binding!!

    private val selectedVideos = mutableListOf<File>()
    private val connectedClients = mutableListOf<Socket>()
    private val videoAdapter by lazy { VideoAdapter() }
    private lateinit var nsdManager: NsdManager
    private lateinit var serverSocket: ServerSocket
    private lateinit var executorService: ExecutorService
    private var acknowledgmentCount = 0

    private val viewModel by viewModel<MasterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        loadVideosFromDirectory()
        setupNsdService()
    }

    private fun loadVideosFromDirectory() {
        val mediaDir = File(Environment.getExternalStorageDirectory(), this.directoryName())
        if (mediaDir.exists() && mediaDir.isDirectory) {
            val videoFiles = mediaDir.listFiles { file ->
                file.extension.equals("mp4", ignoreCase = true) ||
                        file.extension.equals("mkv", ignoreCase = true)
            }
            videoAdapter.differ.submitList(videoFiles?.toList() ?: emptyList())
        }
    }

    private fun init() = binding.run {
        rvVideos.apply {
            adapter = videoAdapter
            layoutManager = LinearLayoutManager(this@MasterActivity)
        }

        btnSend.setOnClickListener { sendVideosToClients() }

        executorService = Executors.newCachedThreadPool()
        startTcpServer()
    }

    private fun startTcpServer() {
        viewModel.startTcpServer(
            onConnected = { socket, string ->
                connectedClients.add(socket)
                // TODO: Show Connected Indicator
            },
            onFailed = {
                //TODO: Show Disconnected Indicator
            }
        )
        executorService.execute {
            try {
                serverSocket = ServerSocket(SOCKET_PORT)

                while (true) {
                    val clientSocket = serverSocket.accept()
                    connectedClients.add(clientSocket)

                    val hostName = clientSocket.inetAddress.hostName
                    runOnUiThread { toast("Client connected: $hostName") }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread { toast("Error accepting client connection") }
            }
        }
    }

    private fun sendVideosToClients() {
        selectedVideos.clear()
        selectedVideos.addAll(videoAdapter.getSelectedVideos())

        if (selectedVideos.isNotEmpty() && connectedClients.isNotEmpty()) {
            acknowledgmentCount = 0
            for (client in connectedClients) {
                executorService.execute {
                    try {
                        handleClient(client)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        runOnUiThread {
                            toast("Error sending videos to ${client.inetAddress.hostAddress}")
                        }
                    }
                }
            }
        } else {
            if (selectedVideos.isEmpty()) toast("No videos selected")
            if (connectedClients.isEmpty()) toast("No connected clients")
        }
    }

    private fun handleClient(clientSocket: Socket) {
        val inputStream = DataInputStream(clientSocket.getInputStream())
        val outputStream = DataOutputStream(clientSocket.getOutputStream())

        notifySlaveBeforeSending(outputStream)

        val slaveResponse = inputStream.readUTF()
        if (slaveResponse == "READY") {
            for ((index, video) in selectedVideos.withIndex()) {
                val isLastFile = (index == selectedVideos.size - 1)
                sendFileToClient(video, outputStream, isLastFile)

                val ack = inputStream.readUTF()
                if (ack == "FILE_RECEIVED") {
                    synchronized(this) {
                        if (isLastFile) acknowledgmentCount++
                        runOnUiThread { toast("Slave ${clientSocket.inetAddress.hostName} confirmed file receipt") }
                    }
                } else {
                    runOnUiThread { toast("No acknowledgment from ${clientSocket.inetAddress.hostName} for ${video.name}") }
                }
            }

            if (acknowledgmentCount == connectedClients.size) {
                sendTimestamp(outputStream)
            }
        } else {
            runOnUiThread { toast("Slave is not ready, no files will be sent") }
        }
    }


    private fun notifySlaveBeforeSending(outputStream: DataOutputStream) {
        outputStream.writeUTF("READY_TO_SEND")
        outputStream.flush()
    }

    private fun sendFileToClient(file: File, outputStream: DataOutputStream, isLastFile: Boolean) {
        val fileInputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        val fileSize = file.length()
        var totalBytesSent = 0L

        outputStream.writeUTF(file.name)
        outputStream.writeLong(fileSize)
        outputStream.writeBoolean(isLastFile)
        outputStream.flush()

        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesSent += bytesRead

            val progress = (totalBytesSent * 100 / fileSize).toInt()
            runOnUiThread {
                binding.progressBar.progress = progress
            }
        }

        outputStream.flush()
        fileInputStream.close()

        runOnUiThread {
            binding.progressBar.progress = 0
            if (isLastFile) toast("Last file ${file.name} sent to client")
        }
    }

    private fun sendTimestamp(outputStream: DataOutputStream) {
        // Send the current master time
        val masterTime = System.currentTimeMillis()

        // Set the start time to be 10 seconds after sending the timestamp
        val playbackStartTime = masterTime + 10000

        // Send the times and video list to the slave
        val videoList = selectedVideos.map { it.name }

        outputStream.writeLong(masterTime)         // Master's current time
        outputStream.writeLong(playbackStartTime)  // Start time for synchronized playback
        outputStream.writeInt(videoList.size)

        for (video in videoList) {
            outputStream.writeUTF(video)
        }

        outputStream.flush()

        runOnUiThread {
            toast("Timestamp and video list sent to clients")
            switchToExoPlayerFragment(videoList, playbackStartTime)
        }
    }

    private fun switchToExoPlayerFragment(videoList: List<String>, timeStamp: Long) {
        val fragment = PlayerFragment.newInstance(ArrayList(videoList), timeStamp)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_master, fragment)
            .commitAllowingStateLoss()
    }

    private fun setupNsdService() {
        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        registerNsdService()
    }

    private fun registerNsdService() {
        executorService.execute {
            try {
                val port = serverSocket.localPort

                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "MasterService"
                    serviceType = SERVICE_TYPE
                    setPort(port)
                }

                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    object : NsdManager.RegistrationListener {
                        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                            runOnUiThread {
                                toast("Service Registered: ${serviceInfo.serviceName}")
                            }
                        }

                        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    })
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket.close()
        executorService.shutdownNow()
        _binding = null
    }

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
        const val SOCKET_PORT = 8989
    }
}