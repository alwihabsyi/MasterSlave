package com.demoapp.masterslave.core.domain.usecase.video

import com.demoapp.masterslave.core.domain.model.VideoFile
import kotlinx.coroutines.flow.Flow

interface VideoUseCase {
    fun getVideosFromDirectory(): Flow<List<VideoFile>>
}