package com.demoapp.masterslave.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast

fun Activity.toast(msg: String?) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.directoryName() = "Android/media/${packageName}/files"