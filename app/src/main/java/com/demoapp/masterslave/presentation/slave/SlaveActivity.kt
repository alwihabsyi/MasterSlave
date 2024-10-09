package com.demoapp.masterslave.presentation.slave

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.demoapp.masterslave.R
import com.demoapp.masterslave.presentation.master.MasterActivity.Companion.SERVICE_TYPE
import com.demoapp.masterslave.databinding.ActivitySlaveBinding
import com.demoapp.masterslave.presentation.player.PlayerFragment
import com.demoapp.masterslave.utils.directoryName
import com.demoapp.masterslave.utils.toast
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

@Suppress("DEPRECATION")
class SlaveActivity : AppCompatActivity() {

    private var _binding: ActivitySlaveBinding? = null
    private val binding get() = _binding!!

    private lateinit var nsdManager: NsdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySlaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        discoverMasterService()
    }

    private fun discoverMasterService() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                runOnUiThread {
                    toast("Discovery started")
                }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == SERVICE_TYPE) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            serviceInfo.host.hostAddress?.let { connectToMaster(it, serviceInfo.port) }
                        }

                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // Handle resolve failure
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        })
    }

    private fun connectToMaster(hostAddress: String, port: Int) = thread {
        while (true) {
            try {
                val socket = Socket().apply {
                    connect(InetSocketAddress(hostAddress, port), 5000)
                    soTimeout = 20000
                }

                runOnUiThread { toast("Connected to Master") }
                receiveFilesFromMaster(socket)
                break
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread { toast("Connection failed, retrying...") }
                Thread.sleep(3000)
            }
        }
    }

    private fun receiveFilesFromMaster(socket: Socket) = thread {
        val inputStream = DataInputStream(socket.getInputStream())
        val outputStream = DataOutputStream(socket.getOutputStream())
        val videoList = mutableListOf<String>()

        try {
            while (true) {
                val signal = inputStream.readUTF()
                if (signal == "READY_TO_SEND") {
                    outputStream.writeUTF("READY")
                    outputStream.flush()

                    while (true) {
                        val fileName = inputStream.readUTF()
                        val fileSize = inputStream.readLong()
                        val file = File(fileDirectoryPath(), fileName)
                        val isLastFile = inputStream.readBoolean()

                        FileOutputStream(file).use { outputStreamFile ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesReceived = 0L

                            while (totalBytesReceived < fileSize) {
                                bytesRead = inputStream.read(buffer, 0, minOf(buffer.size, (fileSize - totalBytesReceived).toInt()))
                                if (bytesRead == -1) break

                                outputStreamFile.write(buffer, 0, bytesRead)
                                totalBytesReceived += bytesRead

                                runOnUiThread {
                                    binding.progressBar.progress = (totalBytesReceived * 100 / fileSize).toInt()
                                }
                            }

                            if (totalBytesReceived == fileSize) {
                                outputStream.writeUTF("FILE_RECEIVED")
                                outputStream.flush()

                                runOnUiThread { toast("File $fileName received successfully") }
                            } else {
                                runOnUiThread { toast("File $fileName not fully received: $totalBytesReceived of $fileSize bytes") }
                            }
                        }

                        if (isLastFile) {
                            val masterTime = inputStream.readLong()
                            val playbackStartTime = inputStream.readLong()

                            // Calculate the difference between the master's time and the slave's current time
                            val slaveCurrentTime = System.currentTimeMillis()
                            val timeOffset = slaveCurrentTime - masterTime

                            // Adjust the playback start time for the slave, accounting for the offset
                            val adjustedPlaybackTime = playbackStartTime + timeOffset

                            // Receive the video list from the master
                            val videoCount = inputStream.readInt()
                            for (i in 0 until videoCount) {
                                val videoName = inputStream.readUTF()
                                videoList.add(videoName)
                            }

                            // Calculate how much time remains until playback should begin
                            val delayUntilPlayback = adjustedPlaybackTime - System.currentTimeMillis()

                            // Switch to the PlayerFragment for playback, with synchronized start time
                            runOnUiThread {
                                switchToExoPlayerFragment(videoList, adjustedPlaybackTime)
                                toast("Playback starts in ${delayUntilPlayback / 1000} seconds")
                            }

                            break
                        }
                    }
                } else {
                    runOnUiThread { toast("No files to receive or incorrect signal") }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread { toast("Error receiving file or connection lost") }
            discoverMasterService()
        } finally {
            try {
                inputStream.close()
                outputStream.close()
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun switchToExoPlayerFragment(videoList: MutableList<String>, timeStamp: Long) {
        val fragment = PlayerFragment.newInstance(ArrayList(videoList), timeStamp)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_slave, fragment)
            .commitAllowingStateLoss()
    }

    private fun fileDirectoryPath(): File {
        val mediaDir = File(Environment.getExternalStorageDirectory(), this.directoryName())
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        return mediaDir
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
