package com.demoapp.masterslave.core.data.handlers.states

sealed class SendTimeStampState {
    data class SendMasterTime(val masterTime: Long) : SendTimeStampState()
    data class SendPlaybackTime(val playbackTime: Long) : SendTimeStampState()
    data class SendVideoCount(val videoCount: Int) : SendTimeStampState()
    data class SendVideoName(val videoName: String) : SendTimeStampState()
    data object TimeStampReceived: SendTimeStampState()
}