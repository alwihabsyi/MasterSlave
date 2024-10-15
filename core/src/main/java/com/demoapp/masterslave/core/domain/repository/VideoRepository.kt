package com.demoapp.masterslave.core.domain.repository

import android.net.Uri
import com.demoapp.masterslave.core.domain.model.VideoFile
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getVideosFromDirectory(): Flow<List<VideoFile>>
    suspend fun moveToMedia(uri: Uri)
}