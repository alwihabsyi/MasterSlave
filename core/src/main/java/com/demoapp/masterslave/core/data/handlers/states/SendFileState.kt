package com.demoapp.masterslave.core.data.handlers.states

import com.demoapp.masterslave.core.domain.model.VideoFile

sealed class SendFileState {
    data object NotifySlave : SendFileState()
    data object SlaveReady : SendFileState()
    data class SendFileName(val fileName: String) : SendFileState()
    data class SendFileSize(val fileSize: Long) : SendFileState()
    data class SendFile(val videoFile: VideoFile): SendFileState()
    data class SendLastFileStatus(val isLastFile: Boolean) : SendFileState()
    data object Finished : SendFileState()
}