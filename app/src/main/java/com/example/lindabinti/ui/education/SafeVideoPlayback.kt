package com.example.lindabinti.ui.education

import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import com.example.lindabinti.R
import com.example.lindabinti.util.canShowWindow
import com.example.lindabinti.util.findActivity

/**
 * VideoView’s built-in error path shows an AlertDialog. That crashes with
 * BadTokenException if Hide / back already tore the window down (seen 17 Sep).
 */
internal object VideoPlaybackSafety {
    fun consumeOsErrorDialog(): Boolean = true

    fun canShowUserMessage(hasActivity: Boolean, finishing: Boolean, destroyed: Boolean): Boolean =
        hasActivity && !finishing && !destroyed
}

internal fun VideoView.installSafePlayback(
    uri: Uri,
    autoStart: Boolean,
    onFailed: () -> Unit = {}
) {
    setOnErrorListener { _, _, _ ->
        val activity = context.findActivity()
        if (VideoPlaybackSafety.canShowUserMessage(
                hasActivity = activity != null,
                finishing = activity?.isFinishing == true,
                destroyed = activity?.isDestroyed == true
            )
        ) {
            Toast.makeText(activity, R.string.education_video_open_fail, Toast.LENGTH_SHORT).show()
        }
        onFailed()
        VideoPlaybackSafety.consumeOsErrorDialog()
    }
    setOnPreparedListener { player ->
        player.isLooping = false
        if (autoStart) start()
    }
    val activity = context.findActivity()
    if (activity != null && activity.canShowWindow()) {
        val controller = MediaController(activity)
        controller.setAnchorView(this)
        setMediaController(controller)
    }
    setVideoURI(uri)
}

internal fun VideoView.releaseSafely() {
    setOnErrorListener(null)
    setOnPreparedListener(null)
    setOnCompletionListener(null)
    setMediaController(null)
    runCatching { stopPlayback() }
}
