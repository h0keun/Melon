package com.melon

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.melon.databinding.FragmentPlayerBinding
import com.melon.service.MusicDto
import com.melon.service.MusicService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var model: PlayerModel = PlayerModel()
    private var binding: FragmentPlayerBinding? = null
    // private var isWatchingPlayListView = true : 위치이동(PlayerModel)
    private var player: SimpleExoPlayer? = null
    private lateinit var playListAdapter: PlayListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayView(fragmentPlayerBinding)
        initPlayListButton(fragmentPlayerBinding)
        initPlayControlButtons(fragmentPlayerBinding)
        initRecyclerView(fragmentPlayerBinding)

        getVideoListFromServer()
    }

    private fun initPlayControlButtons(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playControlImageView.setOnClickListener {
            val player = this.player ?: return@setOnClickListener

            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

        fragmentPlayerBinding.skipNextImageView.setOnClickListener {
            val nextMusic = model.nextMusic() ?: return@setOnClickListener
            playMusic(nextMusic)
        }

        fragmentPlayerBinding.skipPrevImageView.setOnClickListener {
            val prevMusic = model.prevMusic() ?: return@setOnClickListener
            playMusic(prevMusic)
        }
    }

    private fun initPlayView(fragmentPlayerBinding: FragmentPlayerBinding) {
        context?.let { // player 가 지금 null 이기 때문에 초기화하고 넣어줌
            player = SimpleExoPlayer.Builder(it).build()
        }
        fragmentPlayerBinding.playerView.player = player

        binding?.let { binding ->
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        binding.playControlImageView.setImageResource(R.drawable.ic_baseline_pause_48)
                    } else {
                        binding.playControlImageView.setImageResource(R.drawable.ic_baseline_play_arrow_48)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)

                    val newIndex = mediaItem?.mediaId ?: return
                    model.currentPosition = newIndex.toInt()
                    playListAdapter.submitList(model.getAdapterModels())
                    // DiffUtil 을 통한 UI 업데이트비용 : 낮음.
                    // data class - Copy 를 통해 isPlaying 값만 바꾸어 주었기 때문에
                    // 전체List 다시 그리는게 아니라 isPlaying 이 true > false 혹은 false > true 된 부분만 리프래쉬 해줌
                }
            })
        }
    }

    private fun initRecyclerView(fragmentPlayerBinding: FragmentPlayerBinding) {
        playListAdapter = PlayListAdapter {
            playMusic(it)
        }

        fragmentPlayerBinding.playListRecyclerView.apply {
            adapter = playListAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun initPlayListButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playlistImageView.setOnClickListener {
            if(model.currentPosition == -1) return@setOnClickListener
            fragmentPlayerBinding.playerViewGroup.isVisible = model.isWatchingPlayListView
            fragmentPlayerBinding.playerListViewGroup.isVisible = model.isWatchingPlayListView.not()

            model.isWatchingPlayListView = !model.isWatchingPlayListView

            binding?.let { binding ->
                if(model.isWatchingPlayListView){
                    apply {
                        binding.playControlImageView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        binding.playlistImageView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        binding.skipNextImageView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        binding.skipPrevImageView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    }
                }else{
                    apply {
                        binding.playControlImageView.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                        binding.playlistImageView.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                        binding.skipNextImageView.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                        binding.skipPrevImageView.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
                    }
                }
            }
        }
    }

    private fun getVideoListFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(MusicService::class.java)
            .also {
                it.listMusics()
                    .enqueue(object : Callback<MusicDto> {
                        override fun onResponse(
                            call: Call<MusicDto>,
                            response: Response<MusicDto>
                        ) {
                            response.body()?.let { musicDto ->
//                                val modelList = it.musics.mapIndexed { index, musicEntity ->
//                                    musicEntity.mapper(index.toLong()) // 확장해서 mapper 선언해줬기 때문에 가능
//                                }
//  마찬가지로 MusicModelMapper로 이전

                                // 데이터를 서버에서 불러오는 부분에 model 클래스를 초기화헤주며,
                                // 모델에 modelList 를 바로 넣을수 없기 때문에
                                // MusicModelMapper 에 선언해줌(매핑)
                                model = musicDto.mapper()

                                setMusicList(model.getAdapterModels())
                                playListAdapter.submitList(model.getAdapterModels())
                            }
                        }

                        override fun onFailure(call: Call<MusicDto>, t: Throwable) {

                        }

                    })
            }
    }

    private fun setMusicList(modelList: List<MusicModel>) {
        context?.let{
            player?.addMediaItems(modelList.map { musicModel ->
                MediaItem.Builder()
                    .setMediaId(musicModel.id.toString())
                    .setUri(musicModel.streamUrl)
                    .build()
            })

            player?.prepare()
        }
    }

    private fun playMusic(musicModel: MusicModel) {
        model.updateCurrentPosition(musicModel)
        player?.seekTo(model.currentPosition, 0)
        player?.play()
    }

    companion object { // newInstance 로 인자를 넘겨줄 때 apply 를 통해 arguments 를 쉽게 추가해줄 수 있다.
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }

}