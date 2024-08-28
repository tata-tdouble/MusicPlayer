package org.hyperskill.musicplayer.network

import org.hyperskill.musicplayer.models.Song
import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import org.hyperskill.musicplayer.models.Playlist

class Repository(private val context: Context) {

    val data = mutableListOf<Song>()

    private val dbHelper: MyDatabaseHelper = MyDatabaseHelper(context)

    init {
        data.addAll(getAudioFiles(context))
    }


    fun getAudioFiles(context: Context): MutableList<Song> {
        val songList = mutableListOf<Song>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getInt(idColumn)
                val artist = it.getString(artistColumn)
                val title = it.getString(titleColumn)
                val duration = it.getLong(durationColumn)

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    duration = duration
                )

                songList.add(song)
            }
        }

        return songList.toMutableList()
    }

    // Insert a song into a playlist
    fun insertPlaylist(playlist: Playlist) {
        deletePlaylist(playlist.playlistName)
        playlist.songs.forEach {
            dbHelper.insertData(playlist.playlistName, it.id)
        }
    }

    fun getPlaylists(): MutableList<Playlist> {

        val playlistSongPairs = getAllPlaylists()
        val allSongs = getAudioFiles(context)

        val playlistsMap = mutableMapOf<String, MutableList<Song>>()

        playlistSongPairs.forEach { (playlistName, songId) ->
            val song = allSongs.find { it.id == songId }
            song?.let {
                playlistsMap.getOrPut(playlistName) { mutableListOf() }.add(it)
            }
        }

        val myPlaylists = playlistsMap.map { (playlistName, songs) ->
            Playlist(
                playlistName = playlistName,
                songs = songs,
                currentSong = songs.first()
            )
        }.toMutableList()

        myPlaylists.add(0, Playlist("All Songs", allSongs, allSongs.first()))

        return myPlaylists
    }

    // Delete an entire playlist
    fun deletePlaylist(playlistName: String): Int {
        return dbHelper.deletePlaylist(playlistName)
    }

    // Get all playlists and their songs
    fun getAllPlaylists(): List<Pair<String, Int>> {
        val cursor = dbHelper.getAllData()
        val playlists = mutableListOf<Pair<String, Int>>()

        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("playlistName"))
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("songId"))
            playlists.add(name to id)
        }
        cursor.close()
        return playlists
    }

    // Close the database when done
    fun close() {
        dbHelper.close()
    }

}