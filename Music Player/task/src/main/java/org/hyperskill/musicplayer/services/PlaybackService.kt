package org.hyperskill.musicplayer.services

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.databinding.ObservableField
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.extensions.toStringTime
import org.hyperskill.musicplayer.models.Song
import java.io.IOException
import android.content.ContentUris
import android.provider.MediaStore


class PlaybackService(applicationContext: Context, song: Song) : MediaPlayer.OnErrorListener {

    private var mediaPlayer: MediaPlayer? = null
    private val appContext: Context = applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var isDataSourceSet = false
    private var isStopped = false
    private var seekToPosition = -1
    private var currentTrack : Song ? = null

    val currentPosition = ObservableField(0)
    val currentPositionString = ObservableField("00:00")

    init {
        setupMP()
        setDataSource(song)
    }

    fun getSongUri(songId: Long): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

    fun setupMP() {
        mediaPlayer = MediaPlayer().apply {
            setOnErrorListener(this@PlaybackService)
        }
    }

    fun updatePosition(value: Int) {
        println("value $value")
        currentPosition.set(value / 1000)
        currentPositionString.set(value.toStringTime())
    }

    private fun setDataSource(song: Song) {
        mediaPlayer?.apply {
            setDataSource(appContext, getSongUri(songId = song.id.toLong()))
            prepare() // Prepare the MediaPlayer after setting the data source
            setOnPreparedListener {
                isDataSourceSet = true
            }
        }
    }


    private fun setDataSourceAndRun(song: Song) {
        mediaPlayer?.apply {
            setDataSource(appContext,  getSongUri(songId = song.id.toLong()))
            prepare() // Prepare the MediaPlayer after setting the data source
            setOnPreparedListener {
                isDataSourceSet = true
                runPlayer()
            }
        }
    }

    fun play(song: Song) {
        println("PLay 1")
        if (song.title == currentTrack?.title){
            println("PLay 2")
            if (!isDataSourceSet) {
                println("PLay 3")
                setDataSourceAndRun(song)
            } else {
                println("PLay 4")
                runPlayer()
            }
        } else {
            println("PLay 5")
            currentTrack = song
            if(isStopped){
                println("PLay 6")
                mediaPlayer?.reset()
                setDataSourceAndRun(song)
            } else {
                println("PLay 7")
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                setDataSourceAndRun(song)
            }
        }
        isStopped = false
    }

    fun runPlayer(){
        mediaPlayer?.apply {
            try {
                // Start playback
                if (seekToPosition >= 0) {
                    seekTo(seekToPosition)
                    seekToPosition = -1
                }
                start()
                // Create a runnable to update the current position
                updateRunnable = object : Runnable {
                    override fun run() {
                        if (isPlaying) {
                            updatePosition(currentPosition)
                            handler.postDelayed(this, 100)
                        }
                    }
                }
                // Start the runnable
                handler.post(updateRunnable!!)
            } catch (e: IOException) {
                Log.e("AudioPlayer", "Error playing audio: ${e.message}")
            }
        }

    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun stop() {
        songEnd()
        isStopped = true

    }



    fun songEnd() {
        mediaPlayer?.let {
            it.seekTo(0)
            it.stop()
        }
        isDataSourceSet = false
        updatePosition(0)
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable!!)
    }

    fun seekTo(positionSec: Int) {
        mediaPlayer?.seekTo(positionSec)
        seekToPosition = positionSec
        updatePosition(positionSec)
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        // Handle MediaPlayer error
        return true
    }
}
