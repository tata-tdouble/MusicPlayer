package org.hyperskill.musicplayer.models

data class SongSelector(
    var isSelected: Boolean,
    val song: Song
)