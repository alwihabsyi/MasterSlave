package com.demoapp.masterslave.core.data.repository

import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepositoryImpl(
    private val fileDirectory: File
) : VideoRepository {
    override fun getVideosFromDirectory(): Flow<List<VideoFile>> = callbackFlow {
        withContext(Dispatchers.IO) {
            val videoFiles = fileDirectory.listFiles { file ->
                file.extension.equals("mp4", ignoreCase = true) ||
                        file.extension.equals("mkv", ignoreCase = true)
            }
            trySend(videoFiles?.map { VideoFile(it.name, it.absolutePath) } ?: emptyList())
        }

        awaitClose()
    }
}