package org.hyperskill.musicplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.databinding.MainPlayerControllerFragmentBinding
import org.hyperskill.musicplayer.extensions.logger


class MainPlayerControllerFragment : Fragment() {

    private lateinit var binding: MainPlayerControllerFragmentBinding

    private lateinit var viewModel: MainViewModel

    // Use this function to set the viewModel
    fun setViewModel(viewModel: MainViewModel) {
        this.viewModel = viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_player_controller_fragment, container, false)
        binding.viewModel = viewModel // Bind ViewModel

        binding.controllerBtnPlayPause.setOnClickListener {
            if(viewModel.isPlaying == true) {
                viewModel.pauseCurrentSong()
            } else {
                viewModel.playCurrentSong()
            }
        }

        binding.controllerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            var state = false

            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {

                println("progress: $progress")
                if (fromUser) {
                    viewModel.updatePosition(progress  * 1000)
                } else {
                    if (progress + 1 == (viewModel.currentTrack?.duration_sec)) {
                        viewModel.pauseCurrentSong()
                        viewModel.player?.songEnd()
                    }

                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if(seekBar != null ) {
                    state = viewModel.isPlaying == true
                    viewModel.pauseCurrentSong()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    viewModel.updatePosition(it.progress * 1000)
                    if(state) viewModel.playCurrentSong()
                }
            }
        })

        binding.controllerBtnStop.setOnClickListener {
            viewModel.stopCurrentSong()

        }

        return binding.root
    }

}

