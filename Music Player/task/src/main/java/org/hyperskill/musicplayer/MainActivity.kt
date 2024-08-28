package org.hyperskill.musicplayer

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.hyperskill.musicplayer.adapters.AddPlaylistFragmentCallback
import org.hyperskill.musicplayer.adapters.PlayerAdapter
import org.hyperskill.musicplayer.databinding.ActivityMainBinding
import org.hyperskill.musicplayer.extensions.unSelector
import org.hyperskill.musicplayer.models.Playlist
import org.hyperskill.musicplayer.models.Song
import org.hyperskill.musicplayer.models.SongSelector
import org.hyperskill.musicplayer.network.Repository
import org.hyperskill.musicplayer.services.PlaybackService
import org.hyperskill.musicplayer.ui.MainAddPlaylistFragment
import org.hyperskill.musicplayer.ui.MainPlayerControllerFragment
import org.hyperskill.musicplayer.ui.MainViewModel
import org.hyperskill.musicplayer.ui.MainViewModelCallback
import org.hyperskill.musicplayer.ui.MainViewModelFactory
import org.hyperskill.musicplayer.ui.ViewState
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity(), AddPlaylistFragmentCallback, MainViewModelCallback {

    private var adapter: PlayerAdapter? = null
    private var myPlaylists: List<Playlist>? = null
    private var currentPlaylist: Playlist? = null
    private var currentLoadList: Playlist? = null

    private val REQUEST_CODE = 1

    private var mainAddPlaylistFragment: MainAddPlaylistFragment? = null
    private var mainPlayerControllerFragment: MainPlayerControllerFragment? = null

    private var viewModel: MainViewModel? = null

    private var currentScreen = ViewState.PLAY_MUSIC
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Create the ViewModel Factory
        val viewModelFactory = MainViewModelFactory(this, this)

        // Get the ViewModel instance
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        playMusicNavigation()

        binding?.mainSongList?.layoutManager = LinearLayoutManager(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {

            viewModel?.loadData()
        }

        adapter = PlayerAdapter(viewModel!!)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {

            viewModel?.loadCallback()
        }

        binding?.mainSongList?.adapter = adapter

        binding?.mainButtonSearch?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE)
            } else {
                mainMenuSearch()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    viewModel?.loadData()
                    viewModel?.loadCallback()
                    mainMenuSearch()
                } else {
                    Toast.makeText(this, "Songs cannot be loaded without permission", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mainMenuSearch() {
        when (currentScreen) {
            ViewState.PLAY_MUSIC -> {
                viewModel?.setCurrentPlaylist("All Songs")
                adapter?.updateAdapterData(ViewState.PLAY_MUSIC)
            }
            ViewState.ADD_PLAYLIST -> {
                viewModel?.setCurrentLoadList("All Songs")
                adapter?.updateAdapterData(ViewState.ADD_PLAYLIST)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mainMenuAddPlaylist -> {
                val boolean1 = myPlaylists?.filter { it.playlistName == "All Songs" }?.isNotEmpty() ?: false

                if (currentScreen == ViewState.PLAY_MUSIC) {
                    if (boolean1) {
                        Toast.makeText(this, "no songs loaded, click search to load songs", Toast.LENGTH_SHORT).show()
                        viewModel?.setCurrentLoadList("All Songs", unSelector)
                        adapter?.updateAdapterData(ViewState.ADD_PLAYLIST)
                    } else {
                        Toast.makeText(this, "no songs loaded, click search to load songs", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            R.id.mainMenuLoadPlaylist -> showPlaylistChooserDialog(this, "Choose playlist to load")
            R.id.mainMenuDeletePlaylist -> showPlaylistDeleteDialog(this, "Choose playlist to delete")
        }
        return super.onOptionsItemSelected(item)
    }

    fun showPlaylistChooserDialog(context: Context, title: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(myPlaylists?.map { it.playlistName }?.toTypedArray()) { _, which ->
                val playlist = myPlaylists?.get(which)
                if (currentScreen == ViewState.PLAY_MUSIC) {
                    if (currentPlaylist != playlist) {
                        viewModel?.setCurrentPlaylist(playlist?.playlistName ?: "All Songs")
                        adapter?.updateAdapterData(ViewState.PLAY_MUSIC)
                    }
                } else {
                    if (currentPlaylist != playlist) {
                        viewModel?.setCurrentLoadList(playlist?.playlistName ?: "All Songs")
                        adapter?.updateAdapterData(ViewState.ADD_PLAYLIST)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun showPlaylistDeleteDialog(context: Context, title: String) {
        val list = myPlaylists?.map { it.playlistName }
            ?.filter { it != "All Songs" }
            ?.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(list) { _, which ->
                val trashItemName = list?.get(which)
                val trashItem = myPlaylists?.find { it.playlistName == trashItemName }

                if (currentScreen == ViewState.PLAY_MUSIC) {
                    if (currentPlaylist == trashItem) {
                        viewModel?.setCurrentPlaylist("All Songs")
                        viewModel?.deletePlayList(trashItemName ?: "All Songs")
                        adapter?.updateAdapterData(ViewState.PLAY_MUSIC)
                    } else {
                        viewModel?.deletePlayList(trashItemName ?: "All Songs")
                    }
                } else {
                    if (currentLoadList == trashItem) {
                        viewModel?.setCurrentLoadList("All Songs")
                        if (currentPlaylist == trashItem) viewModel?.setCurrentPlaylist("All Songs")
                        viewModel?.deletePlayList(trashItemName ?: "All Songs")
                        adapter?.updateAdapterData(ViewState.ADD_PLAYLIST)
                    } else {
                        viewModel?.deletePlayList(trashItemName ?: "All Songs")
                        if (currentPlaylist == trashItem) viewModel?.setCurrentPlaylist("All Songs")
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun changeViewState(state: ViewState) {
        currentScreen = state
        when (state) {
            ViewState.PLAY_MUSIC -> {
                playMusicNavigation()
            }
            ViewState.ADD_PLAYLIST -> {
                addPlaylistNavigation()
            }
        }
    }

    fun playMusicNavigation() {
        if (mainPlayerControllerFragment == null) {
            mainPlayerControllerFragment = MainPlayerControllerFragment()
        }
        viewModel?.let { mainPlayerControllerFragment?.setViewModel(it) } // Set the viewModel after fragment creation
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.mainFragmentContainer, mainPlayerControllerFragment!!)
        fragmentTransaction.commit()
    }

    fun addPlaylistNavigation() {
        if (mainAddPlaylistFragment == null) {
            mainAddPlaylistFragment = viewModel?.let { MainAddPlaylistFragment(it) }
        }
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding?.mainFragmentContainer?.id ?: return, mainAddPlaylistFragment!!)
        fragmentTransaction.commit()
    }

    override fun onSelectedOkOrCancel() {
        currentScreen = ViewState.PLAY_MUSIC
        changeViewState(ViewState.PLAY_MUSIC)
        adapter?.updateAdapterData(currentScreen)
    }

    override fun onCurrentMaxDurationChanged(value: String) {
        // TODO: Not yet implemented
    }

    override fun onMyPlaylistsChanged(value: List<Playlist>) {
        myPlaylists = value
    }

    override fun onCurrentPlaylistChanged(value: Playlist) {
        currentPlaylist = value
        // adapter?.updateAdapterData(ViewState.PLAY_MUSIC)
    }

    override fun onCurrentLoadListChanged(value: Playlist) {
        currentLoadList = value
        // adapter?.updateAdapterData(ViewState.ADD_PLAYLIST)
    }

    override fun onCurrentTrackChanged(value: Song) {
        // println("trigger")
    }

    override fun onLoadingPlaylistChanged(value: Int) {
        // adapter?.notifyItemChanged(value)
    }

    override fun onIsPlayingChanged(value: Boolean) {
        adapter?.notifyDataSetChanged()
    }

    override fun onViewStateChanged(value: ViewState) {
        changeViewState(value)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel?.closeDB()
    }
}
