package com.demoapp.masterslave.core.data.handlers.states

sealed class FileTransferState {
    data object WaitingForSignal : FileTransferState()
    data object ReadyToSend : FileTransferState()
    data class Complete(val videoList: List<String>, val playbackTime: Long) : FileTransferState()
    data class Error(val message: String) : FileTransferState()
}
