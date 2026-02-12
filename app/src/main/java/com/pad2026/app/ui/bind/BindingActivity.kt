package com.pad2026.app.ui.bind

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pad2026.app.R
import com.pad2026.app.core.AppContainer
import com.pad2026.app.sync.SyncScheduler
import com.pad2026.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class BindingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_binding)

        val accountInput = findViewById<EditText>(R.id.accountInput)
        val codeInput = findViewById<EditText>(R.id.bindCodeInput)
        val bindBtn = findViewById<Button>(R.id.bindButton)

        bindBtn.setOnClickListener {
            val accountId = accountInput.text.toString().trim()
            val bindCode = codeInput.text.toString().trim()
            if (accountId.isEmpty() || bindCode.isEmpty()) {
                Toast.makeText(this, R.string.bind_input_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    AppContainer.from(this@BindingActivity)
                        .deviceRepository
                        .bind(accountId, bindCode, BuildConfig.VERSION_NAME)
                    SyncScheduler.triggerManualSync(this@BindingActivity)
                    startActivity(Intent(this@BindingActivity, PlayerActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@BindingActivity,
                        getString(R.string.bind_failed, e.message ?: "unknown"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
