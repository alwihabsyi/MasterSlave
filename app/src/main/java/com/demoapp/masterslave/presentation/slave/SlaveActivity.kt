package com.demoapp.masterslave.presentation.slave

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.demoapp.masterslave.R
import com.demoapp.masterslave.databinding.ActivitySlaveBinding
import com.demoapp.masterslave.presentation.player.PlayerViewModel
import com.demoapp.masterslave.utils.getIndicator
import com.demoapp.masterslave.utils.setFullScreen
import com.demoapp.masterslave.utils.switchToExoPlayerFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.Socket

class SlaveActivity : AppCompatActivity() {

    private var _binding: ActivitySlaveBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModel<SlaveViewModel>()
    private val playerViewModel by viewModel<PlayerViewModel>()

    private var masterSocket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySlaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startDiscover()
        observer()
    }

    private fun observer() {
        playerViewModel.isPlaying.observe(this) { isPlaying ->
            if (isPlaying) listenToMasterPosition()
        }
    }

    private fun listenToMasterPosition() = masterSocket?.let {
        viewModel.startListeningToMasterPosition(it).observe(this) { (video, masterPosition, masterTimeStamp) ->
            playerViewModel.updateVideoPosition(video, masterPosition, masterTimeStamp)
        }
    }

    private fun startDiscover() = viewModel.discoverMasterService(
        onDiscoveryStarted = {
            runOnUiThread { setupIndicator(null) }
        },
        onServiceFound = { serviceInfo ->
            Log.i(TAG, "Service found: ${serviceInfo.serviceName}")
        },
        onConnected = { socket ->
            masterSocket = socket
            runOnUiThread { setupIndicator(socket.inetAddress.hostAddress) }
        },
        onReceivingProgress = { progress ->
            runOnUiThread { binding.progressBar.progress = progress }
        },
        onAllFilesReceived = { videoList, playbackTime ->
            switchToExoPlayerFragment(videoList, playbackTime, false)
        },
        onError = { _, connectionError ->
            if (connectionError) handleConnectionError()
        }
    )

    private fun setupIndicator(hostAddress: String?) = binding.run {
        val isConnected = hostAddress != null

        val indicator = isConnected.getIndicator(this@SlaveActivity)
        val message = if (isConnected) getString(R.string.connected_to_master, hostAddress) else "Not connected to master"
        ivIndicatorStatus.setImageDrawable(indicator)
        tvIndicator.text = message
    }

    private fun handleConnectionError() { startDiscover() }

    override fun onResume() {
        super.onResume()
        setFullScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cancelScope(masterSocket)
        _binding = null
    }

    companion object {
        val TAG: String = SlaveActivity::class.java.simpleName
    }
}
