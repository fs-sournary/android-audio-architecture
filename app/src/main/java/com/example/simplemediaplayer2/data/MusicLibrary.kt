package com.example.simplemediaplayer2.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.example.simplemediaplayer2.BuildConfig
import com.example.simplemediaplayer2.R
import com.example.simplemediaplayer2.extensions.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

object MusicLibrary {

    private val musics = TreeMap<String, MediaMetadataCompat>()
    private val albumRes = HashMap<String, Int>()
    private val musicFileNames = HashMap<String, String>()

    init {
        createMediaMetadata(
            "Jazz_In_Paris",
            "Jazz in Paris",
            "Music Right Productions",
            "Jazz & Blues",
            "Jazz",
            103,
            TimeUnit.SECONDS,
            "jazz_in_paris.mp3",
            R.drawable.album_jazz_blues,
            "album_jazz_blues"
        )
        createMediaMetadata(
            "The_Coldest_Shoulder",
            "The Coldest Shoulder",
            "The 126ers",
            "Youtube Audio Library Rock 2",
            "Rock",
            160,
            TimeUnit.SECONDS,
            "the_coldest_shoulder.mp3",
            R.drawable.album_youtube_audio_library_rock_2,
            "album_youtube_audio_library_rock_2"
        )
    }

    private fun createMediaMetadata(
        musicId: String, title: String, artist: String,
        album: String, genre: String, duration: Long, durationUnit: TimeUnit,
        musicFileName: String, albumArtistResId: Int, albumArtistResName: String
    ) {
        val mediaMetadata = MediaMetadataCompat.Builder().apply {
            this.id = musicId
            this.title = title
            this.artist = artist
            this.album = album
            this.genre = genre
            this.duration = TimeUnit.MILLISECONDS.convert(duration, durationUnit)
            this.albumArtUri = getAlbumArtUri(albumArtistResName)
            this.displayIconUri = getAlbumArtUri(albumArtistResName)
        }.build()
        musics[musicId] = mediaMetadata
        albumRes[musicId] = albumArtistResId
        musicFileNames[musicId] = musicFileName
    }

    fun getRoot(): String = "Root"

    private fun getAlbumArtUri(resName: String): String =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/drawable/$resName"

    fun getMusicFileName(musicId: String): String? {
        if (musicFileNames.containsKey(musicId)) {
            return musicFileNames[musicId]
        }
        return null
    }

    fun getAlbumBitmap(context: Context, musicId: String): Bitmap? {
        if (albumRes.containsKey(musicId)) {
            val resId = albumRes[musicId] ?: return null
            return BitmapFactory.decodeResource(context.resources, resId)
        }
        return null
    }

    fun getMediaItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val result = mutableListOf<MediaBrowserCompat.MediaItem>()
        musics.values.forEach {
            val mediaItem = MediaBrowserCompat.MediaItem(
                it.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            result.add(mediaItem)
        }
        return result
    }

    fun getMusicMetadata(context: Context, musicId: String): MediaMetadataCompat? {
        return musics[musicId]?.let {
            val musicMetadataBuilder = MediaMetadataCompat.Builder().apply {
                this.id = it.id
                this.album = it.album
                this.artist = it.artist
                this.genre = it.genre
                this.title = it.title
                this.duration = it.duration
                getAlbumBitmap(context, musicId)?.also { albumArt -> this.albumArt = albumArt }
            }
            musicMetadataBuilder.build()
        }
    }
}
