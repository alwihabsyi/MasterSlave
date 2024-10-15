package com.demoapp.masterslave.ui.player

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.demoapp.masterslave.databinding.FragmentPlayerBinding
import com.demoapp.masterslave.utils.getDirectory
import com.demoapp.masterslave.utils.getMediaItemIndex
import com.demoapp.masterslave.utils.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModel<PlayerViewModel>(ownerProducer = { requireActivity() })

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var videoFiles: List<String>
    private var timestamp: Long = 0
    private var isMaster = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            videoFiles = it.getStringArrayList(ARG_VIDEO_FILES)?.toList() ?: emptyList()
            timestamp = it.getLong(ARG_TIMESTAMP)
            isMaster = it.getBoolean(ARG_IS_MASTER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExoPlayer()
        prepareAndPlayVideos()
        startPeriodicPositionUpdate()
    }

    @OptIn(UnstableApi::class)
    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = exoPlayer
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    private fun prepareAndPlayVideos() {
        for (videoFile in videoFiles) {
            val videoUri = Uri.fromFile(videoFile.getDirectory(requireContext()))
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.addMediaItem(mediaItem)
        }
        exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        exoPlayer.prepare()

        val delay = timestamp - System.currentTimeMillis()

        if (delay > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                exoPlayer.play()
            }, delay)
        } else {
            exoPlayer.play()
        }

        viewModel.setPlaying(true)
//        observePosition()
    }

    private fun startPeriodicPositionUpdate() {
        if (!isMaster) return

        val handler = Handler(Looper.getMainLooper())
        val updateInterval = 10000L

        val updateRunnable = object : Runnable {
            override fun run() {
                val currentPosition = exoPlayer.currentPosition
                exoPlayer.currentMediaItemIndex
                val currentVideo = exoPlayer.currentMediaItem?.mediaId ?: "Unknown video"

                viewModel.updateVideoPosition(currentVideo, currentPosition, timestamp)
                handler.postDelayed(this, updateInterval)
            }
        }

        handler.post(updateRunnable)
    }

    private fun observePosition() {
        if (isMaster) return

        viewModel.videoPositionState.observe(viewLifecycleOwner) { positionState ->
            positionState?.let { (currentVideo, masterTimestamp, masterPosition) ->
                val mediaItemIndex = exoPlayer.currentTimeline.getMediaItemIndex { mediaItem ->
                    mediaItem.mediaId == currentVideo
                }


                if (mediaItemIndex != C.INDEX_UNSET && mediaItemIndex != exoPlayer.currentMediaItemIndex) {
                    exoPlayer.seekTo(mediaItemIndex, masterPosition)
                    exoPlayer.play()
                    toast("Switched to video: $currentVideo at position: $masterTimestamp")
                } else {
                    exoPlayer.seekTo(masterPosition)
                    exoPlayer.play()
                    toast("Position Adjusted (position: $masterPosition) (time: $masterTimestamp)")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setPlaying(false)
        exoPlayer.release()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setPlaying(false)
        exoPlayer.release()
    }

    companion object {
        private const val ARG_VIDEO_FILES = "video_files"
        private const val ARG_TIMESTAMP = "timestamp"
        private const val ARG_IS_MASTER = "is_master"

        fun newInstance(videoFiles: ArrayList<String>, timestamp: Long, isMaster: Boolean): PlayerFragment {
            return PlayerFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_VIDEO_FILES, videoFiles)
                    putLong(ARG_TIMESTAMP, timestamp)
                    putBoolean(ARG_IS_MASTER, isMaster)
                }
            }
        }
    }
}