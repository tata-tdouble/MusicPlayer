package org.hyperskill.musicplayer.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.extensions.logger
import org.hyperskill.musicplayer.extensions.mergeSongSelections
import org.hyperskill.musicplayer.extensions.toClockFormat
import org.hyperskill.musicplayer.extensions.toSong
import org.hyperskill.musicplayer.extensions.toStringTime
import org.hyperskill.musicplayer.models.Playlist
import org.hyperskill.musicplayer.models.Song
import org.hyperskill.musicplayer.models.SongSelector
import org.hyperskill.musicplayer.network.Repository
import org.hyperskill.musicplayer.services.PlaybackService
import java.util.Locale


enum class ViewState {  PLAY_MUSIC, ADD_PLAYLIST  }


interface MainViewModelCallback{
    fun onCurrentMaxDurationChanged(value: String)
    fun onMyPlaylistsChanged(value: List<Playlist>)
    fun onCurrentPlaylistChanged(value: Playlist)
    fun onCurrentLoadListChanged(value: Playlist)
    fun onCurrentTrackChanged(value: Song)
    fun onLoadingPlaylistChanged(value: Int)
    fun onIsPlayingChanged(value: Boolean)
    fun onViewStateChanged(value: ViewState)
}

class MainViewModel (
    private val applicationContext: Context,
    private val callback: MainViewModelCallback
) : ViewModel() {

    var currentMaxDuration = ObservableField("00:00")

    var myPlaylists = mutableListOf<Playlist>()

    var currentPlaylist : Playlist ? = null

    var currentLoadList  : Playlist ? = null

    var currentTrack : Song ? = null

    var loadingPlaylist = mutableListOf<SongSelector>()

    var isPlaying : Boolean ? = null

    var viewState = ViewState.PLAY_MUSIC

    var player : PlaybackService ? = null

    var repository = Repository(applicationContext)

    fun loadData() {
        val data = repository.data

        if (data.isEmpty()) {
            // Handle the case where the data is empty or null
            viewState = ViewState.PLAY_MUSIC // You can define a NO_DATA state if needed
            loadingPlaylist = mutableListOf()
            currentPlaylist = Playlist("", mutableListOf(), Song(0, "", "", 0))
            myPlaylists = mutableListOf()
            currentLoadList = currentPlaylist
            isPlaying = false
            currentTrack = null
            player = null // Or handle the PlaybackService differently when no track is available
        } else {
            // Normal flow
            val playlist = Playlist("All Songs", data, data[0])
            viewState = ViewState.PLAY_MUSIC
            loadingPlaylist = mutableListOf()
            currentPlaylist = playlist
            myPlaylists = repository.getPlaylists()
            currentLoadList = playlist
            isPlaying = false
            currentTrack = playlist.currentSong
            player = PlaybackService(applicationContext, currentTrack!!)
        }
    }

    fun loadCallback() {
        currentTrack?.let {
            callback.onCurrentTrackChanged(it)
        }
        callback.onViewStateChanged(viewState)
        currentLoadList?.let {
            callback.onCurrentLoadListChanged(it)
        }
        currentPlaylist?.let {
            callback.onCurrentPlaylistChanged(it)
        }
        myPlaylists.let {
            callback.onMyPlaylistsChanged(it)
        }
    }

    fun updateCurrentSong(song: Song) {
        currentTrack = song
        currentPlaylist = currentPlaylist!!.copy(currentSong = song)
        updateMaxDuration(song.duration_st)
        callback.onCurrentTrackChanged(song)
        callback.onCurrentMaxDurationChanged(song.duration_st)
        callback.onCurrentPlaylistChanged(currentPlaylist!!)
        callback.onViewStateChanged(ViewState.PLAY_MUSIC)
    }

    fun updateMaxDuration(value: String) {
        currentMaxDuration.set(value)
    }

    fun pauseCurrentSong() {
        isPlaying = false
        player?.pause()
        callback.onIsPlayingChanged(false)  
    }

    fun stopCurrentSong() {
        isPlaying = false
        player?.stop()
        callback.onIsPlayingChanged(false)
    }

    fun playCurrentSong() {
        if(currentTrack != null) {
            isPlaying = true
            updateMaxDuration(currentTrack!!.duration_st)
            callback.onIsPlayingChanged(true)
            player?.play(currentTrack!!)
        }
    }

    fun setCurrentPlaylist(name: String) {
        var newPlaylist = myPlaylists.filter { it.playlistName == name }.firstOrNull()
        val songsList = newPlaylist?.songs?.sortedBy { it.id }?.toMutableList()
        newPlaylist = if ((songsList?.contains(currentTrack)) == false){
            stopCurrentSong()
            currentTrack = newPlaylist?.currentSong
            callback.onCurrentTrackChanged(currentTrack!!)
            isPlaying = false
            callback.onIsPlayingChanged(false)
            songsList?.getOrNull(0)?.let { newPlaylist?.copy(currentSong = it) }
        }else {
            newPlaylist = newPlaylist?.copy(currentSong = currentTrack!!)
            newPlaylist
        }
        newPlaylist = newPlaylist?.copy(songs = newPlaylist.songs.sortedBy { it.id }.toMutableList())
        currentPlaylist = newPlaylist
        currentPlaylist?.let { callback.onCurrentPlaylistChanged(it) }
    }

    fun setCurrentLoadList(name: String, unSelector: (List<SongSelector>.() -> List<SongSelector>)? = null) {
        val newPlaylist = myPlaylists.filter { it.playlistName == name }
        var list = mergeSongSelections(newPlaylist[0].songs, loadingPlaylist)
        loadingPlaylist = loadingPlaylist.filter { list.contains(it) }.toMutableList()
        if (unSelector != null){
            list = list.unSelector()
            loadingPlaylist.clear()
        }
        val mList = list.sortedBy { it.song.id }.toSong().toMutableList()
        currentLoadList = Playlist("Loading", mList, mList[0])
        callback.onCurrentLoadListChanged(currentLoadList!!)
    }

    fun addSongToLoadList(newSong: SongSelector, position: Int) {
        loadingPlaylist = loadingPlaylist.let {
            it.add(newSong)
            it.toSet().toMutableList()
        }
        callback.onLoadingPlaylistChanged(position)
    }

    fun removeSongFromLoadList(newSong: SongSelector, position: Int) {
        loadingPlaylist = loadingPlaylist.let {
            it.remove(newSong)
            it.toSet().toMutableList()
        }
        callback.onLoadingPlaylistChanged(position)
    }

    fun savePlayList(newList: Playlist) {
        repository.insertPlaylist(newList)
        myPlaylists = repository.getPlaylists()
        callback.onMyPlaylistsChanged(myPlaylists)
        loadingPlaylist.clear()
    }

    fun updatePosition(newPosition: Int) {
        player?.seekTo(newPosition)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun deletePlayList(name : String) {
        repository.deletePlaylist(name)
        myPlaylists = repository.getPlaylists()
        callback.onMyPlaylistsChanged(myPlaylists)
        loadingPlaylist.clear()
    }

    fun changeViewState(viewState: ViewState){
        callback.onViewStateChanged(viewState)
    }

    fun closeDB(){
        repository.close()
    }

}

class MainViewModelFactory(
    private val applicationContext: Context,
    private val callback: MainViewModelCallback
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val viewModel = MainViewModel(applicationContext, callback)
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}