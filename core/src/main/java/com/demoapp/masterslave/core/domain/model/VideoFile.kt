package com.demoapp.masterslave.core.domain.model

import java.io.File

data class VideoFile(
    val name: String,
    val path: String,
    val size: Long = File(path).length()
)