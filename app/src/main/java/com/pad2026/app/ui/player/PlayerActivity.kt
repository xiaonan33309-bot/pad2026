package com.pad2026.app.ui.player

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pad2026.app.R
import com.pad2026.app.core.AppContainer
import com.pad2026.app.data.local.AdEntity
import com.pad2026.app.data.local.PlayLogEntity
import com.pad2026.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

class PlayerActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private var playlist: List<AdEntity> = emptyList()
    private var currentIndex = 0
    private var currentStartAtMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        hideSystemUi()

        playerView = findViewById(R.id.playerView)
        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        logCurrentPlayback(completed = true, failReason = null)
                        playNext()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    logCurrentPlayback(completed = false, failReason = error.errorCodeName)
                    playNext()
                }
            })
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val repo = AppContainer.from(this@PlayerActivity).adRepository
            playlist = repo.getPlayable(Instant.now().epochSecond)
            launch(Dispatchers.Main) {
                if (playlist.isNotEmpty()) playCurrent()
            }
        }
    }

    private fun playCurrent() {
        if (playlist.isEmpty()) return
        if (currentIndex >= playlist.size) currentIndex = 0
        val ad = playlist[currentIndex]
        val path = ad.localVideoPath ?: run {
            playNext(); return
        }
        if (!File(path).exists()) {
            playNext(); return
        }

        currentStartAtMs = System.currentTimeMillis()
        player.setMediaItem(MediaItem.fromUri(path))
        player.prepare()
        player.playWhenReady = true
    }

    private fun playNext() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        playCurrent()
    }

    private fun logCurrentPlayback(completed: Boolean, failReason: String?) {
        if (playlist.isEmpty()) return
        val ad = playlist[currentIndex]
        val end = System.currentTimeMillis()
        val durationSec = ((end - currentStartAtMs).coerceAtLeast(0L)) / 1000
        lifecycleScope.launch(Dispatchers.IO) {
            AppContainer.from(this@PlayerActivity).playLogRepository.insert(
                PlayLogEntity(
                    adId = ad.adId,
                    playStartAt = currentStartAtMs,
                    playEndAt = end,
                    playedDurationSec = durationSec,
                    completed = completed,
                    failReason = failReason
                )
            )
            SyncScheduler.triggerManualSync(this@PlayerActivity)
        }
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
