package org.hyperskill.musicplayer.adapters

import androidx.lifecycle.LiveData
import org.hyperskill.musicplayer.models.Playlist
import org.hyperskill.musicplayer.models.Song
import org.hyperskill.musicplayer.models.SongSelector

interface PlayMusicAdapterCallback {

    fun updateCurrentSong(song: Song)
    fun pauseCurrentSong()
    fun playCurrentSong()
    fun getCurrentSong(): Song?
    fun isPlaying(): Boolean?
    fun onLongClickFromPlayMusicAdapter(song: Song)

}

interface AddPlaylistFragmentCallback {
    fun onSelectedOkOrCancel()
}

//Add Playlist
interface ViewHolderAddPlaylistAdapterCallback {
    fun onSongSelected(position: Int)
    fun onSongUnSelected(position: Int)
}


//Play Music
interface ViewHolderPlayMusicAdapterCallback {
    fun onLongClickSongSelected(position: Int)
    fun getIsPlaying() : Boolean
    fun getCurrentSong(): Song
    fun getPlayList(): Playlist
    fun notifyAdapterItemChanged(position: Int)
    fun notifyAdapterDataChanged()
    fun onPlayPauseButtonClicked(position: Int)


}
