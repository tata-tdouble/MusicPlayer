package org.hyperskill.musicplayer.extensions

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.hyperskill.musicplayer.models.Song
import org.hyperskill.musicplayer.models.SongSelector
import java.util.Locale

val audioPlayerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun Long.toClockFormat(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
}

fun Int.toStringTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
}

fun List<Song>.toSongSelector(): List<SongSelector> {
    return this.map{SongSelector(false, it )}
}

fun List<SongSelector>.toSong(): List<Song> {
    return this.map{ it.song }
}

val unSelector : List<SongSelector>.() -> List<SongSelector> ={
    this.map{ SongSelector( false, it.song) }
}

fun mergeSongSelections(songs: List<Song>, selections: List<SongSelector>): List<SongSelector> {
    val selectionMap = selections.associateBy { it.song } // Create a map for efficient lookup
    return songs.map { song ->
        val isSelected = selectionMap[song]?.isSelected ?: false // Check if song is selected
        SongSelector(isSelected, song)
    }.sortedBy { it.song.id }
}






fun logger(){
    Log.i("XXXXX", "logger: 1")
}



fun logger(text: String){
    Log.i("XXXXX", "logger: $text")
}


fun teller(){
    Log.i("XXXXX", "teller: 1")
}
