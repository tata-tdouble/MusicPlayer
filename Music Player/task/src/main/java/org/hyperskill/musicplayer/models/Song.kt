package org.hyperskill.musicplayer.models

import org.hyperskill.musicplayer.extensions.toClockFormat

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val duration: Long,
    val duration_st: String = duration.toClockFormat(),
    val duration_sec: Int = duration.toInt() / 1000
)