package com.demoapp.masterslave.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerViewModel: ViewModel() {
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _videoPositionState = MutableLiveData<Triple<String, Long, Long>>()
    val videoPositionState: LiveData<Triple<String, Long, Long>> get() = _videoPositionState

    fun updateVideoPosition(currentVideo: String, currentPosition: Long, timestamp: Long) {
        _videoPositionState.value = Triple(currentVideo, currentPosition, timestamp)
    }

    fun setPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }
}