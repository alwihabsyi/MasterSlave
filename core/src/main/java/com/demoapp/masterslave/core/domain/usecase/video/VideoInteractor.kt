package com.demoapp.masterslave.core.domain.usecase.video

import android.net.Uri
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

class VideoInteractor(private val videoRepository: VideoRepository): VideoUseCase {
    override fun getVideosFromDirectory(): Flow<List<VideoFile>> =
        videoRepository.getVideosFromDirectory()

    override suspend fun moveToMedia(uri: Uri) =
        videoRepository.moveToMedia(uri)
}