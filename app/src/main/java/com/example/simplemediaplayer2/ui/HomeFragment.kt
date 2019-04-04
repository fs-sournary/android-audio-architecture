package com.example.simplemediaplayer2.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.simplemediaplayer2.R
import com.example.simplemediaplayer2.client.MusicBrowserHelper
import com.example.simplemediaplayer2.data.MusicLibrary
import com.example.simplemediaplayer2.service.MusicService
import com.example.simplemediaplayer2.extensions.toTimeString
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home.*

/**
 * Created on 3/12/19 by Sang
 * Description:
 **/
class HomeFragment : Fragment() {

    private var isPlaying: Boolean = false

    private lateinit var musicBrowserHelper: MusicBrowserHelper<MusicService>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.apply {
            if (this is AppCompatActivity) {
                setSupportActionBar(toolbar)
            }
        }
        setupClient()
        setupEvents()
    }

    private fun setupClient() {
        val musicControllerCallback = object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                super.onMetadataChanged(metadata)
                showMusicInfoWhenMetadataChanged(metadata)
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                super.onPlaybackStateChanged(state)
                showPlayStateChanged(state)
            }
        }
        musicBrowserHelper =
            object : MusicBrowserHelper<MusicService>(activity!!, MusicService::class.java) {
                override fun onConnected(mediaController: MediaControllerCompat) {
                    musicSeekBar.setMediaController(mediaController)
                }

                override fun onDisconnected() {}

                override fun onChildrenLoaded(
                    parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    mediaController?.apply {
                        children.forEach { addQueueItem(it.description) }
                        transportControls.prepare()
                    }
                }
            }.apply { addControllerCallback(musicControllerCallback) }
    }

    private fun showMusicInfoWhenMetadataChanged(metadata: MediaMetadataCompat?) {
        metadata?.apply {
            musicTitleTextView.text = getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            musicArtistTextView.text = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            musicDurationTextView.text =
                getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toTimeString("mm:ss")
            context?.apply {
                val thumbnailImage = MusicLibrary.getAlbumBitmap(
                    this, getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                )
                musicThumbnailImageView.setImageBitmap(thumbnailImage)
            }
        }
    }

    private fun showPlayStateChanged(playbackState: PlaybackStateCompat?) {
        playbackState?.apply {
            isPlaying = state == PlaybackStateCompat.STATE_PLAYING
            if (isPlaying) {
                playPauseImageView.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp)
            } else {
                playPauseImageView.setImageResource(R.drawable.ic_play_circle_outline_black_24dp)
            }
        }
    }

    private fun setupEvents() {
        nextMusicImageView.setOnClickListener {
            musicBrowserHelper.mediaController?.transportControls?.skipToNext()
        }
        previousMusicImageView.setOnClickListener {
            musicBrowserHelper.mediaController?.transportControls?.skipToPrevious()
        }
        playPauseImageView.setOnClickListener {
            if (isPlaying) {
                musicBrowserHelper.mediaController?.transportControls?.pause()
            } else {
                musicBrowserHelper.mediaController?.transportControls?.play()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        musicBrowserHelper.onStart()
    }

    override fun onStop() {
        super.onStop()
        musicBrowserHelper.onStop()
        musicSeekBar.disconnectMediaController()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home_option, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        view?.apply { Snackbar.make(this, "Menu clicked", Snackbar.LENGTH_SHORT).show() }
        return true
    }

    companion object {

        fun newInstance() = HomeFragment()
    }
}
