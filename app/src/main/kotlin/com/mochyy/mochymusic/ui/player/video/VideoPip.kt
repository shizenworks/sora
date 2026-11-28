package com.mochyy.mochymusic.ui.player.video

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.models.MediaMetadata
import com.mochyy.mochymusic.playback.PlayerConnection
import com.mochyy.mochymusic.widget.MusicWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

/**
 * The video the video style is drawing a picture from right now. Set only while a picture is up,
 * so leaving the app does nothing when no video is on screen.
 */
object VideoPip {
    var session by mutableStateOf<YouTubeVideoSession?>(null)

    // the system takes a window only between 1:2.39 and 2.39:1
    @RequiresApi(Build.VERSION_CODES.O)
    fun params(context: Context, session: YouTubeVideoSession?, playing: Boolean, autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspect(session))
            .setActions(
                listOf(
                    action(
                        context,
                        requestCode = 201,
                        command = MusicWidgetReceiver.ACTION_PLAY_PAUSE,
                        icon = if (playing) R.drawable.pause else R.drawable.play,
                        label = context.getString(if (playing) R.string.pause else R.string.play),
                    ),
                    action(
                        context,
                        requestCode = 202,
                        command = MusicWidgetReceiver.ACTION_NEXT,
                        icon = R.drawable.skip_next,
                        label = context.getString(R.string.pip_next),
                    ),
                ),
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(autoEnter)
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun aspect(session: YouTubeVideoSession?): Rational {
        val ratio = session?.aspect?.takeIf { it > 0f } ?: return Rational(16, 9)
        val clamped = ratio.coerceIn(0.42f, 2.39f)
        return Rational((clamped * 1000).roundToInt(), 1000)
    }

    // the same play, pause and next the widget sends, so the service handles them as it always does
    @RequiresApi(Build.VERSION_CODES.O)
    private fun action(context: Context, requestCode: Int, command: String, icon: Int, label: String): RemoteAction {
        val intent = Intent(context, MusicWidgetReceiver::class.java).setAction(command)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return RemoteAction(Icon.createWithResource(context, icon), label, label, pending)
    }
}

/**
 * Keeps the activity's picture-in-picture params in step with the video and playback, and,
 * while the window is in picture-in-picture, covers the app with the video alone.
 */
@Composable
fun VideoPipLayer(activity: Activity, enabled: Boolean, playerConnection: PlayerConnection?, inPip: Boolean) {
    val session = VideoPip.session?.takeIf { enabled }
    val isPlaying = remember(playerConnection) { playerConnection?.isPlaying ?: MutableStateFlow(false) }
    val playing by isPlaying.collectAsState()
    val metadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsState()

    LaunchedEffect(activity, session, playing) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@LaunchedEffect
        // a rejected window shape only costs the picture-in-picture entry, never the app
        runCatching {
            activity.setPictureInPictureParams(VideoPip.params(activity, session, playing, autoEnter = session != null))
        }
    }

    // while in picture-in-picture this opaque layer covers the whole activity, so the app's own ui never shows in the window;
    // with no video (loading, or an audio-only track) the current song's artwork stands in for it
    if (inPip) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (session != null) {
                YouTubeVideoSurface(session = session, isPlaying = playing, modifier = Modifier.fillMaxSize())
            } else {
                metadata?.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(0.9f),
                    )
                }
            }
        }
    }
}
