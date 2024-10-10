package com.demoapp.masterslave.core.utils

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.M

fun Context.directoryName() = if (Build.VERSION.SDK_INT <= M) "files" else "Android/media/${packageName}/files"