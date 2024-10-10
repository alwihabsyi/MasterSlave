package com.demoapp.masterslave.presentation.master

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.demoapp.masterslave.R
import com.demoapp.masterslave.core.common.SharedState
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.ui.VideoAdapter
import com.demoapp.masterslave.databinding.ActivityMasterBinding
import com.demoapp.masterslave.presentation.player.PlayerViewModel
import com.demoapp.masterslave.utils.getIndicator
import com.demoapp.masterslave.utils.setFullScreen
import com.demoapp.masterslave.utils.switchToExoPlayerFragment
import com.demoapp.masterslave.utils.toast
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.Socket

class MasterActivity : AppCompatActivity() {

    private var _binding: ActivityMasterBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModel<MasterViewModel>()
    private val playerViewModel by viewModel<PlayerViewModel>()

    private val videoAdapter by lazy { VideoAdapter() }
    private val selectedVideos = mutableListOf<VideoFile>()

    private val sharedState: SharedState by inject()
    private val connectedClients get() = sharedState.connectedClients
    private val clientsReceivedFile = mutableListOf<Socket>()
    private val clientsReceivedTimeStamp = mutableListOf<Socket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupServer()
        initUI()
        observeViewModel()
    }

    private fun setupServer() = with(viewModel) {
        registerNsdService { serviceName -> Log.i(TAG, "Service Registered: $serviceName") }

        startTcpServer(
            onConnected = { _, hostAddress ->
                Log.i(TAG, "New slave connected: $hostAddress")
                runOnUiThread { setupConnectionStatus(true) }
            },
            onFailed = {
                runOnUiThread { setupConnectionStatus(false) }
            }
        )
    }

    private fun setupConnectionStatus(isConnected: Boolean) = binding.run {
        val indicator = isConnected.getIndicator(this@MasterActivity)
        val message = if (isConnected) getString(R.string.slave_connected, connectedClients.size.toString()) else "No slave connected"
        ivIndicator.setImageDrawable(indicator)
        tvIndicator.text = message
    }

    private fun initUI() = with(binding) {
        rvVideos.adapter = videoAdapter
        rvVideos.layoutManager = LinearLayoutManager(this@MasterActivity)
        btnSend.setOnClickListener {
            selectedVideos.apply {
                clear()
                addAll(videoAdapter.getSelectedVideos())
            }

            if (selectedVideos.isEmpty()) {
                toast("No videos selected")
                return@setOnClickListener
            }
            if (connectedClients.isEmpty()) {
                toast("No connected clients")
                return@setOnClickListener
            }

            sendSelectedVideos(selectedVideos)
        }
    }

    private fun observeViewModel() {
        viewModel.directoryVideos.observe(this) { videos ->
            videoAdapter.differ.submitList(videos)
        }

        playerViewModel.videoPositionState.observe(this) { positionState ->
            positionState?.let { (video, masterPosition, masterTimestamp) ->
                connectedClients.forEach(sendVideoTimeStamp(video, masterPosition, masterTimestamp))
            }
        }
    }

    private fun sendVideoTimeStamp(
        video: String,
        masterPosition: Long,
        masterTimestamp: Long
    ): (Socket) -> Unit = {
        viewModel.sendVideoTimeStamp(it, video, masterPosition, masterTimestamp)
    }

    private fun sendSelectedVideos(selectedVideos: MutableList<VideoFile>) {
        connectedClients.forEach { socket ->
            viewModel.sendVideosToClients(
                selectedVideos = selectedVideos,
                socket = socket,
                onSendingProgress = { runOnUiThread { binding.progressBar.progress = it } },
                onSuccess = {
                    clientsReceivedFile.add(socket)
                    handleSendTimeStamp(selectedVideos)
                }
            )
        }
    }

    private fun handleSendTimeStamp(selectedVideos: MutableList<VideoFile>) {
        val masterTime = System.currentTimeMillis()
        val playbackStartTime = masterTime + 10000
        if (clientsReceivedFile.size == connectedClients.size) {
            connectedClients.forEach {
                viewModel.sendPlayTimeStamp(it, selectedVideos, playbackStartTime, masterTime) {
                    clientsReceivedTimeStamp.add(it)
                    handleAllTimeStampReceived(selectedVideos, playbackStartTime)
                }
            }
        }
    }

    private fun handleAllTimeStampReceived(
        selectedVideos: MutableList<VideoFile>,
        playbackStartTime: Long
    ) {
        Log.i("CLIENT RECEIVE TIMESTAMP", "${clientsReceivedTimeStamp.size} | client received file: ${clientsReceivedFile.size}")
        if (clientsReceivedTimeStamp.size == clientsReceivedFile.size) {
            switchToExoPlayerFragment(selectedVideos.map { it.name }, playbackStartTime, true)
            clientsReceivedFile.clear()
            clientsReceivedTimeStamp.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        setFullScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.closeSocket()
        _binding = null
    }

    companion object {
        val TAG: String = MasterActivity::class.java.simpleName
    }
}