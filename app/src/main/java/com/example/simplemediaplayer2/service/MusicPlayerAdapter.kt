package com.example.simplemediaplayer2.service

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.simplemediaplayer2.data.MusicLibrary

/**
 * Create on 3/15/19 by Sang
 * Description:
 **/
class MusicPlayerAdapter(
    context: Context,
    private val playbackStateChange: (PlaybackStateCompat) -> Unit, // Notification action
    private val playbackComplete: (() -> Unit)? = null
) : PlayerAdapter(context) {

    private var fileName: String? = null
    private var playbackStateCode: Int = PlaybackStateCompat.STATE_NONE
    // MediaPlayer might be in Paused (not playing) and is called MediaPlayer.seekTo()
    private var seekWhileNotPlaying = -1L
    private var isMediaCompleted: Boolean = false

    private val applicationContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    var currentMetadata: MediaMetadataCompat? = null

    override fun onPlay() {
        mediaPlayer?.apply {
            if (isPlaying.not()) {
                start()
                setNewPlaybackStateCode(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun setNewPlaybackStateCode(@PlaybackStateCompat.State newPlaybackState: Int) {
        playbackStateCode = newPlaybackState
        if (playbackStateCode == PlaybackStateCompat.STATE_STOPPED) {
            isMediaCompleted = true
        }
        val currentPosition: Long
        if (seekWhileNotPlaying >= 0) {
            currentPosition = seekWhileNotPlaying
            if (playbackStateCode == PlaybackStateCompat.STATE_PLAYING) {
                seekWhileNotPlaying = -1
            }
        } else {
            currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0
        }
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(getAvailableAction())
            .setState(playbackStateCode, currentPosition, 1F, SystemClock.elapsedRealtime())
        playbackStateChange.invoke(stateBuilder.build())
    }

    private fun getAvailableAction(): Long {
        var action = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        when (playbackStateCode) {
            PlaybackStateCompat.STATE_PLAYING -> {
                action = action or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                action = action or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_STOP
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                action = action or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE
            }
            else -> {
                action = action or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE
            }
        }
        return action
    }

    override fun onPause() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
                setNewPlaybackStateCode(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    override fun onStop() {
        setNewPlaybackStateCode(PlaybackStateCompat.STATE_STOPPED)
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.also {
            it.release()
            mediaPlayer = null
        }
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    override fun setVolume(volume: Float) {
        mediaPlayer?.apply { setVolume(volume, volume) }
    }

    override fun seekTo(position: Long) {
        mediaPlayer?.apply {
            if (isPlaying.not()) {
                seekWhileNotPlaying = position
            }
            this.seekTo(position.toInt())
            // Set the new state (to the current state) because the position changed and should be reported to clients.
            setNewPlaybackStateCode(playbackStateCode)
        }
    }

    override fun playFromMedia(metadata: MediaMetadataCompat) {
        currentMetadata = metadata
        metadata.description.mediaId?.let { playFile(MusicLibrary.getMusicFileName(it) ?: return) }
    }

    private fun playFile(newFileName: String) {
        var isMediaChanged = newFileName != fileName
        if (isMediaCompleted) {
            // research to end of MediaPlayer
            // Last audio file was played to completion, the resourceId hasn't been changed.
            // But the player was released, so force a reload of the media file for playback.
            isMediaChanged = true
            isMediaCompleted = false
        }
        if (isMediaChanged.not()) {
            if (!isPlaying()) {
                play()
            }
            return
        }
        releaseMediaPlayer()
        // Play new media file
        fileName = newFileName
        initializeMediaPlayer()
        try {
            applicationContext.assets.openFd(newFileName).apply {
                mediaPlayer?.apply {
                    setDataSource(fileDescriptor, startOffset, length)
                    prepare()
                }
                play()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error: ${e.message}")
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    playbackComplete?.invoke()
                    // Set the state to "Paused" because it most closely matches the state of MediaPlayer
                    // with regards to available state transitions compared to "Stop".
                    // "Paused" state allows: seekTo(), start(), pause(), stop().
                    // "Stop" state allows: stop().
                    setNewPlaybackStateCode(PlaybackStateCompat.STATE_PAUSED)
                }
            }
        }
    }
}
