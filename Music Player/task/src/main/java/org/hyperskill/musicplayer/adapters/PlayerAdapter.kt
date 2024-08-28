package org.hyperskill.musicplayer.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.extensions.mergeSongSelections
import org.hyperskill.musicplayer.extensions.toClockFormat
import org.hyperskill.musicplayer.models.Playlist
import org.hyperskill.musicplayer.models.Song
import org.hyperskill.musicplayer.models.SongSelector
import org.hyperskill.musicplayer.ui.MainViewModel
import org.hyperskill.musicplayer.ui.ViewState
import android.view.View
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.hyperskill.musicplayer.databinding.ListItemSongBinding
import org.hyperskill.musicplayer.databinding.ListItemSongSelectorBinding

class PlayerAdapter(private val viewModel: MainViewModel) :
    RecyclerView.Adapter<ViewHolder>(),
    ViewHolderPlayMusicAdapterCallback,
    ViewHolderAddPlaylistAdapterCallback {

    private val emptySong = Song(0, "", "", 0)

    private var currentPlaylist = viewModel.currentPlaylist ?: Playlist("", mutableListOf(), emptySong) // Default to an empty playlist
    private var currentLoadList = viewModel.currentLoadList ?: Playlist("", mutableListOf(), emptySong) // Default to an empty load list
    private var currentTrack = viewModel.currentTrack ?: Song(0, "", "", 0) // Default to an empty song
    private var loadingPlaylist = viewModel.loadingPlaylist ?: mutableListOf()
    private var viewState = viewModel.viewState

    fun updateAdapterData(value: ViewState) {
        currentPlaylist = viewModel.currentPlaylist ?: Playlist("", mutableListOf(), emptySong)
        currentLoadList = viewModel.currentLoadList ?: Playlist("", mutableListOf(), emptySong)
        currentTrack = viewModel.currentTrack ?: Song(0, "", "", 0)
        loadingPlaylist = viewModel.loadingPlaylist ?: mutableListOf()
        viewState = value
        viewModel.changeViewState(viewState)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewState.PLAY_MUSIC.ordinal -> {
                val binding = ListItemSongBinding.inflate(inflater, parent, false)
                PlayMusicViewHolder(binding, this)
            }
            ViewState.ADD_PLAYLIST.ordinal -> {
                val binding = ListItemSongSelectorBinding.inflate(inflater, parent, false)
                AddPlaylistViewHolder(binding, this)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is PlayMusicViewHolder -> {
                holder.bind(currentPlaylist.songs.getOrNull(position) ?: return)
                holder.onIsPlayingChanged(getIsPlaying() && getCurrentSong() == currentPlaylist.songs.getOrNull(position))
            }
            is AddPlaylistViewHolder -> {
                val list = mergeSongSelections(currentLoadList.songs, loadingPlaylist)
                holder.bind(list.getOrNull(position) ?: return)
            }
        }
    }

    override fun getItemCount(): Int {
        return when (viewState) {
            ViewState.PLAY_MUSIC -> currentPlaylist.songs.size
            ViewState.ADD_PLAYLIST -> currentLoadList.songs.size
            else -> 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return viewState.ordinal
    }

    override fun onLongClickSongSelected(position: Int) {
        viewModel.addSongToLoadList(SongSelector(true, currentPlaylist.songs.getOrNull(position) ?: return), position)
        updateAdapterData(ViewState.ADD_PLAYLIST)
    }

    override fun onSongSelected(position: Int) {
        loadingPlaylist = loadingPlaylist.let {
            it.add(SongSelector(true, currentLoadList.songs.getOrNull(position) ?: return))
            it.toSet().toMutableList()
        }
        viewModel.addSongToLoadList(SongSelector(true, currentLoadList.songs.getOrNull(position) ?: return), position)
    }

    override fun onSongUnSelected(position: Int) {
        loadingPlaylist = loadingPlaylist.let {
            it.remove(SongSelector(true, currentLoadList.songs.getOrNull(position) ?: return))
            it.toSet().toMutableList()
        }
        viewModel.removeSongFromLoadList(SongSelector(true, currentLoadList.songs.getOrNull(position) ?: return), position)
    }

    override fun getIsPlaying(): Boolean {
        return viewModel.isPlaying ?: false
    }

    override fun getCurrentSong(): Song {
        return currentTrack
    }

    override fun getPlayList(): Playlist {
        return currentPlaylist
    }

    fun updateCurrentSong(song: Song) {
        currentTrack = song
        viewModel.updateCurrentSong(song)
        notifyDataSetChanged()
    }

    fun pauseCurrentSong() {
        viewModel.pauseCurrentSong()
    }

    fun playCurrentSong() {
        viewModel.playCurrentSong()
    }

    override fun notifyAdapterItemChanged(position: Int) {
        notifyItemChanged(position)
    }

    override fun notifyAdapterDataChanged() {
        notifyDataSetChanged()
    }

    override fun onPlayPauseButtonClicked(position: Int) {
        val currentSong = getCurrentSong()
        val songAtPosition = getPlayList().songs.getOrNull(position) ?: return

        if (currentSong == songAtPosition) {
            if (getIsPlaying()) {
                pauseCurrentSong()
            } else {
                playCurrentSong()
            }
        } else {
            updateCurrentSong(songAtPosition)
            playCurrentSong()
        }

        notifyAdapterItemChanged(position)
        val currentSongIndex = getPlayList().songs.indexOf(currentSong)
        if (currentSongIndex != -1) {
            notifyAdapterItemChanged(currentSongIndex)
        }
    }


    class PlayMusicViewHolder(private val binding: ListItemSongBinding, private val callback: PlayerAdapter) : ViewHolder(binding.root) {

        private var song: Song? = null

        init {
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    callback.onLongClickSongSelected(position)
                    true
                } else {
                    false
                }
            }

            binding.songItemImgBtnPlayPause.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION ) {
                    callback.onPlayPauseButtonClicked(adapterPosition)
                }
            }
        }

        fun bind(song: Song) {
            this.song = song
            binding.setVariable(BR.song, song)
            binding.songItemTvArtist.text = song.artist
            binding.songItemTvTitle.text = song.title
            binding.songItemTvDuration.text = song.duration.toClockFormat()
            binding.executePendingBindings()
        }

        fun onIsPlayingChanged(value: Boolean) {
            val iconResId = if (value && callback.getCurrentSong() == song) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
            binding.songItemImgBtnPlayPause.setImageResource(iconResId)
        }
    }

    class AddPlaylistViewHolder(private val binding: ListItemSongSelectorBinding, private val callback: PlayerAdapter) : ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                binding.songSelectorItemCheckBox.isChecked = !binding.songSelectorItemCheckBox.isChecked
                if (binding.songSelectorItemCheckBox.isChecked) {
                    itemView.setBackgroundColor(Color.LTGRAY)
                    callback.onSongSelected(position)
                } else {
                    itemView.setBackgroundColor(Color.WHITE)
                    callback.onSongUnSelected(position)
                }
            }
        }

        fun bind(songSelector: SongSelector) {
            binding.setVariable(BR.songSelector, songSelector)
            binding.songSelectorItemTvArtist.text = songSelector.song.artist
            binding.songSelectorItemTvTitle.text = songSelector.song.title
            binding.songSelectorItemTvDuration.text = songSelector.song.duration.toClockFormat()
            binding.songSelectorItemCheckBox.isClickable = false
            if (songSelector.isSelected) {
                itemView.setBackgroundColor(Color.LTGRAY)
                binding.songSelectorItemCheckBox.isChecked = true
            } else {
                itemView.setBackgroundColor(Color.WHITE)
                binding.songSelectorItemCheckBox.isChecked = false
            }
            binding.executePendingBindings()
        }
    }
}
