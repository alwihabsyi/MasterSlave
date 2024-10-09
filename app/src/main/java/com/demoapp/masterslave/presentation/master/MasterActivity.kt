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
import com.demoapp.masterslave.presentation.player.PlayerFragment
import com.demoapp.masterslave.utils.toast
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MasterActivity : AppCompatActivity() {

    private var _binding: ActivityMasterBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModel<MasterViewModel>()
    private val sharedState: SharedState by inject()

    private val selectedVideos = mutableListOf<VideoFile>()
    private val videoAdapter by lazy { VideoAdapter() }

    private val connectedClients get() = sharedState.connectedClients

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initUI()
        setupServer()
        observeViewModel()
    }

    private fun initUI() = with(binding) {
        rvVideos.apply {
            adapter = videoAdapter
            layoutManager = LinearLayoutManager(this@MasterActivity)
        }
        btnSend.setOnClickListener { sendSelectedVideos() }
    }

    private fun setupServer() = with(viewModel) {
        registerNsdService { serviceName -> Log.i(TAG, "Service Registered: $serviceName") }

        startTcpServer(
            onConnected = { _, host -> toast("Connected Host: $host") },
            onFailed = { toast("Failed to connect") }
        )
    }

    private fun observeViewModel() {
        viewModel.directoryVideos.observe(this) { videos ->
            videoAdapter.differ.submitList(videos)
        }
    }

    private fun sendSelectedVideos() {
        selectedVideos.apply {
            clear()
            addAll(videoAdapter.getSelectedVideos())
        }

        if (selectedVideos.isEmpty()) {
            toast("No videos selected")
            return
        }
        if (connectedClients.isEmpty()) {
            toast("No connected clients")
            return
        }

        connectedClients.forEach { client ->
            viewModel.sendVideosToClients(selectedVideos, client) { videos, startTime ->
                switchToExoPlayerFragment(videos, startTime)
            }
        }
    }

    private fun switchToExoPlayerFragment(videoList: List<String>, timeStamp: Long) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_master, PlayerFragment.newInstance(ArrayList(videoList), timeStamp))
            .commitAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.closeSocket()
        _binding = null
    }

    companion object {
        val TAG: String = MasterActivity::class.java.simpleName
        const val SERVICE_TYPE = "_http._tcp."
    }
}