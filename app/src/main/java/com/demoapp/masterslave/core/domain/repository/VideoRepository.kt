package com.demoapp.masterslave.core.domain.repository

import com.demoapp.masterslave.core.domain.model.VideoFile
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getVideosFromDirectory(): Flow<List<VideoFile>>
}