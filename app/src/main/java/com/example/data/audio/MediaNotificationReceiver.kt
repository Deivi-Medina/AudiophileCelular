package com.example.data.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PLAY = "com.example.audiophiles.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.audiophiles.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.audiophiles.ACTION_NEXT"
        const val ACTION_PREV = "com.example.audiophiles.ACTION_PREV"

        var onPlayCallback: (() -> Unit)? = null
        var onPauseCallback: (() -> Unit)? = null
        var onNextCallback: (() -> Unit)? = null
        var onPrevCallback: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> onPlayCallback?.invoke()
            ACTION_PAUSE -> onPauseCallback?.invoke()
            ACTION_NEXT -> onNextCallback?.invoke()
            ACTION_PREV -> onPrevCallback?.invoke()
        }
    }
}
