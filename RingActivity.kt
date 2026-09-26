package com.aditya.wakey.ring

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aditya.wakey.alarm.RingService
import com.aditya.wakey.ui.theme.WakeyTheme
import kotlinx.coroutines.launch

/**
 * The full-screen "wake up!" screen. Shows over the lock screen, turns the screen on,
 * ignores Back. Closing it does NOT stop the alarm: RingService keeps ringing and
 * brings this screen back until the mission is done.
 */
class RingActivity : ComponentActivity() {

    companion object {
        @Volatile
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Nope. Finish the mission.
            }
        })

        if (RingService.current.value == null) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            WakeyTheme {
                val alarm by RingService.current.collectAsState()
                alarm?.let { RingFlow(it) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                RingService.current.collect { if (it == null) finish() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
    }
}
