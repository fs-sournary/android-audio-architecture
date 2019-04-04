package com.example.simplemediaplayer2.service

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.example.simplemediaplayer2.R
import com.example.simplemediaplayer2.data.MusicLibrary
import com.example.simplemediaplayer2.extensions.isPlayEnabled
import com.example.simplemediaplayer2.extensions.isPlaying
import com.example.simplemediaplayer2.extensions.isSkipToNextEnabled
import com.example.simplemediaplayer2.extensions.isSkipToPreviousEnabled

/**
 * Create on 3/12/19 by Sang
 * Description:
 **/
class MusicNotificationManager(private val service: MusicService) {

    val notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_arrow_white_24dp,
        service.getString(R.string.play_action_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY)
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_white_24dp,
        service.getString(R.string.pause_action_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PAUSE)
    )
    private val nextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_white_24dp,
        service.getString(R.string.next_action_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )
    private val previousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_white_24dp,
        service.getString(R.string.previous_action_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )

    fun getNotification(
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        description: MediaDescriptionCompat
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val mediaStyle = MediaStyle().setMediaSession(token)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true) // For android 7 and earlier
        val deletePendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            service, PlaybackStateCompat.ACTION_STOP
        )
        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_music_note_black_24dp)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setDeleteIntent(deletePendingIntent)
            .setLargeIcon(MusicLibrary.getAlbumBitmap(service, description.mediaId ?: ""))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
        if (state.isSkipToPreviousEnabled) {
            builder.addAction(previousAction)
        }
        if (state.isPlaying) {
            builder.addAction(pauseAction)
        } else if (state.isPlayEnabled) {
            builder.addAction(playAction)
        }
        if (state.isSkipToNextEnabled) {
            builder.addAction(nextAction)
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "MediaSession", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "MediaPlayer with MediaSession demo"
                enableLights(true)
                lightColor = Color.RED
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {

        private const val CHANNEL_ID = "com.sournary.notification.MUSIC_CHANNEL"
        const val NOTIFICATION_ID = 111
    }
}
