package org.hyperskill.musicplayer.network

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "musicPlayerDatabase.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "playlist"
        private const val COLUMN_PLAYLIST_NAME = "playlistName"
        private const val COLUMN_SONG_ID = "songId"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_PLAYLIST_NAME TEXT,
                $COLUMN_SONG_ID INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(playlistName: String, songId: Int): Long {

        // Check if the pair already exists
        val selection = "$COLUMN_PLAYLIST_NAME = ? AND $COLUMN_SONG_ID = ?"
        val selectionArgs = arrayOf(playlistName, songId.toString())
        val cursor = readableDatabase.query(TABLE_NAME, null, selection, selectionArgs, null, null, null)

        // If the pair exists, return -1 to indicate a duplicate
        if (cursor.moveToFirst()) {
            cursor.close()
            return -1L
        }
        cursor.close()

        // Insert the new data
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_PLAYLIST_NAME, playlistName)
            put(COLUMN_SONG_ID, songId)
        }
        return db.insert(TABLE_NAME, null, contentValues)
    }


    // Read
    fun getAllData(): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_NAME,  // The table to query
            arrayOf(COLUMN_PLAYLIST_NAME, COLUMN_SONG_ID),  // The columns to return
            null,  // The columns for the WHERE clause
            null,  // The values for the WHERE clause
            null,  // Don't group the rows
            null,  // Don't filter by row groups
            null   // The sort order
        )
    }


    // Delete entire playlist
    fun deletePlaylist(playlistName: String): Int {
        val db = writableDatabase
        val selection = "$COLUMN_PLAYLIST_NAME = ?"
        val selectionArgs = arrayOf(playlistName)
        return db.delete(TABLE_NAME, selection, selectionArgs)
    }

    // Close database
    override fun close() {
        writableDatabase.close()
        super.close()
    }
}
