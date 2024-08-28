package org.hyperskill.musicplayer.models

data class Playlist (
    val playlistName: String,
    val songs: MutableList<Song>,
    val currentSong: Song
)