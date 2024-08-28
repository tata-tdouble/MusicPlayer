package org.hyperskill.musicplayer.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.adapters.AddPlaylistFragmentCallback
import org.hyperskill.musicplayer.databinding.MainAddPlaylistFragmentBinding
import org.hyperskill.musicplayer.extensions.toSong
import org.hyperskill.musicplayer.models.Playlist


class MainAddPlaylistFragment(val viewModel: MainViewModel): Fragment() {

    private lateinit var binding: MainAddPlaylistFragmentBinding
    private lateinit var callback: AddPlaylistFragmentCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_add_playlist_fragment, container, false)
        return binding.root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as AddPlaylistFragmentCallback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addPlaylistBtnOk.setOnClickListener {
            if (viewModel.loadingPlaylist.isEmpty()) {
                Toast.makeText(
                    activity,
                    "Add at least one song to your playlist",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val enteredName = binding.addPlaylistEtPlaylistName.text.toString()
                if (enteredName.isEmpty()) {
                    Toast.makeText(
                        activity,
                        "Add a name to your playlist",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (enteredName == "All Songs") {
                        Toast.makeText(
                            activity,
                            "$enteredName is a reserved name choose another playlist name",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val playlist = viewModel.loadingPlaylist.toSong().toMutableList()
                        viewModel.savePlayList(Playlist(enteredName, playlist, playlist[0]))
                        callback.onSelectedOkOrCancel()
                    }
                }
            }
        }

        binding.addPlaylistBtnCancel.setOnClickListener {
            callback.onSelectedOkOrCancel()
        }
    }
}