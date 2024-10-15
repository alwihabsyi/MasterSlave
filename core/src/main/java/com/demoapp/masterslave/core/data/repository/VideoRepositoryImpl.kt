package com.demoapp.masterslave.core.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.provider.OpenableColumns
import com.demoapp.masterslave.core.domain.model.VideoFile
import com.demoapp.masterslave.core.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class VideoRepositoryImpl(
    private val fileDirectory: File,
    private val context: Context
) : VideoRepository {
    fun observerEvent(path: String?): List<VideoFile> {
        if (path != null) {
            val videoFiles = fileDirectory.listFiles { file ->
                file.extension.equals("mp4", ignoreCase = true) ||
                        file.extension.equals("mkv", ignoreCase = true)
            }

            return videoFiles?.map { VideoFile(it.name, it.absolutePath) } ?: emptyList()
        }

        return emptyList()
    }

    override fun getVideosFromDirectory(): Flow<List<VideoFile>> = callbackFlow {
        val fileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(fileDirectory, CREATE or DELETE or MOVED_TO or MOVED_FROM) {
                override fun onEvent(event: Int, path: String?) {
                    trySend(observerEvent(path))
                }
            }
        } else {
            object : FileObserver(fileDirectory.absolutePath, CREATE or DELETE or MOVED_TO or MOVED_FROM) {
                override fun onEvent(event: Int, path: String?) {
                    trySend(observerEvent(path))
                }
            }
        }

        fileObserver.startWatching()

        withContext(Dispatchers.IO) {
            val videoFiles = fileDirectory.listFiles { file ->
                file.extension.equals("mp4", ignoreCase = true) ||
                        file.extension.equals("mkv", ignoreCase = true)
            }
            trySend(videoFiles?.map { VideoFile(it.name, it.absolutePath) } ?: emptyList())
        }

        awaitClose { fileObserver.stopWatching() }
    }

    override suspend fun moveToMedia(uri: Uri): Unit = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val videoInputStream = resolver.openInputStream(uri)

            val fileName = uri.getFileName(context)
            val destinationFile = File(fileDirectory, fileName)

            videoInputStream?.let {
                val outputStream = FileOutputStream(destinationFile)
                videoInputStream.copyTo(outputStream)
                videoInputStream.close()
                outputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Uri.getFileName(context: Context): String {
        var fileName = "unknown_file"
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}