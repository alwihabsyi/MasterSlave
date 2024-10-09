package com.demoapp.masterslave.presentation.player

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.demoapp.masterslave.databinding.FragmentPlayerBinding
import com.demoapp.masterslave.utils.directoryName
import java.io.File

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var videoFiles: List<String>
    private var timestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            videoFiles = it.getStringArrayList(ARG_VIDEO_FILES)?.toList() ?: emptyList()
            timestamp = it.getLong(ARG_TIMESTAMP)
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
    }

    @OptIn(UnstableApi::class)
    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = exoPlayer
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    private fun prepareAndPlayVideos() {
        for (videoFile in videoFiles) {
            val videoUri = Uri.fromFile(File(Environment.getExternalStorageDirectory(), requireContext().directoryName() + "/" + videoFile))
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer.release()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    companion object {
        private const val ARG_VIDEO_FILES = "video_files"
        private const val ARG_TIMESTAMP = "timestamp"

        fun newInstance(videoFiles: ArrayList<String>, timestamp: Long): PlayerFragment {
            return PlayerFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_VIDEO_FILES, videoFiles)
                    putLong(ARG_TIMESTAMP, timestamp)
                }
            }
        }
    }
}