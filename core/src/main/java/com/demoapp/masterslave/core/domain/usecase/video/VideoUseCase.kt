package com.demoapp.masterslave.core.domain.usecase.video

import android.net.Uri
import com.demoapp.masterslave.core.domain.model.VideoFile
import kotlinx.coroutines.flow.Flow

interface VideoUseCase {
    fun getVideosFromDirectory(): Flow<List<VideoFile>>
    suspend fun moveToMedia(uri: Uri)
}