package com.demoapp.masterslave.domain.repository

import com.demoapp.masterslave.domain.model.VideoFile
import java.net.ServerSocket

interface VideoRepository {
    suspend fun getVideosFromDirectory(): List<VideoFile>
    suspend fun sendVideosToClients(selectedVideos: List<VideoFile>, serverSocket: ServerSocket)
}