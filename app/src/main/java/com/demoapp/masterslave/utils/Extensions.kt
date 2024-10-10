package com.demoapp.masterslave.utils

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Environment
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import com.demoapp.masterslave.R
import com.demoapp.masterslave.presentation.player.PlayerFragment
import java.io.File
import com.demoapp.masterslave.core.utils.directoryName

fun Activity.toast(msg: String?) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(msg: String?) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

fun FragmentActivity.switchToExoPlayerFragment(
    videoList: List<String>,
    timeStamp: Long,
    isMaster: Boolean
) {
    val fragment = PlayerFragment.newInstance(ArrayList(videoList), timeStamp, isMaster)
    val fragmentId = if (isMaster) R.id.fragment_container_master else R.id.fragment_container_slave
    supportFragmentManager.beginTransaction()
        .replace(fragmentId, fragment)
        .commitAllowingStateLoss()
}

fun Boolean.getIndicator(context: Context): GradientDrawable {
    val color = if (this) android.R.color.holo_green_light else android.R.color.holo_red_dark
    val colorValue = ContextCompat.getColor(context, color)

    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colorValue)
    }
}

fun Activity.setFullScreen() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Timeline.getMediaItemIndex(predicate: (MediaItem) -> Boolean): Int {
    for (i in 0 until windowCount) {
        val mediaItem = getWindow(i, Timeline.Window()).mediaItem
        if (predicate(mediaItem)) {
            return i
        }
    }
    return C.INDEX_UNSET
}

fun String.getDirectory(context: Context): File =
    if (Build.VERSION.SDK_INT <= M) {
        File(context.externalCacheDir, context.directoryName() + "/" + this)
    } else {
        File(Environment.getExternalStorageDirectory(), context.directoryName() + "/" + this)
    }