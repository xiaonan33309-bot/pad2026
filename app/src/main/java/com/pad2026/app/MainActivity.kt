package com.pad2026.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pad2026.app.core.AppContainer
import com.pad2026.app.service.PlaybackForegroundService
import com.pad2026.app.ui.bind.BindingActivity
import com.pad2026.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val device = AppContainer.from(this@MainActivity).deviceRepository.getDevice()
            PlaybackForegroundService.start(this@MainActivity)
            if (device == null) {
                startActivity(Intent(this@MainActivity, BindingActivity::class.java))
            } else {
                startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
            }
            finish()
        }
    }
}
