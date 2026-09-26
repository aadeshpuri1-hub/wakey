package com.aditya.wakey.alarm

import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aditya.wakey.R
import com.aditya.wakey.WakeyApp
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.AlarmStore
import com.aditya.wakey.ring.RingActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Owns a ringing alarm: sound, vibration, wake locks, the full-screen notification,
 * and a watchdog that keeps forcing the alarm screen back on top until it's dismissed.
 * The alarm screen can NOT stop the sound by being closed; only dismiss()/snooze() can.
 */
class RingService : Service() {

    companion object {
        private const val TAG = "WakeyRing"
        private const val EXTRA_ID = "alarm_id"
        private const val NOTIF_ID = 4242

        private val _current = MutableStateFlow<Alarm?>(null)
        /** The alarm that is ringing right now, or null. */
        val current: StateFlow<Alarm?> = _current

        private var instance: RingService? = null

        fun start(ctx: Context, alarmId: Int) {
            val i = Intent(ctx, RingService::class.java).putExtra(EXTRA_ID, alarmId)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun dismiss() {
            instance?.doDismiss()
        }

        /** @return false if snooze isn't allowed (off or no snoozes left). */
        fun snooze(): Boolean = instance?.doSnooze() ?: false

        /** Lowers the volume while the person is doing the mission. */
        fun setMissionActive(active: Boolean) {
            instance?.missionActive = active
            instance?.applyVolume()
        }

        fun snoozesLeft(ctx: Context, alarm: Alarm): Int =
            if (alarm.snoozeMinutes <= 0) 0
            else (alarm.maxSnoozes - AlarmStore.snoozesUsed(ctx, alarm.id)).coerceAtLeast(0)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var cpuLock: PowerManager.WakeLock? = null
    private var savedVolume = -1
    private var ramp = 0.35f
    private var missionActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ID, -1) ?: -1
        val incoming = if (id != -1) AlarmStore.getForRing(this, id) else null
        val ringing = _current.value

        // Must call startForeground within a few seconds of startForegroundService().
        goForeground(ringing ?: incoming)

        if (ringing != null) {
            // Already ringing; just make sure the screen is up.
            launchAlarmScreen()
            return START_REDELIVER_INTENT
        }
        if (incoming == null) {
            stopEverything()
            return START_NOT_STICKY
        }

        _current.value = incoming
        missionActive = false
        ramp = 0.35f
        acquireLocks()
        startSound(incoming)
        startVibration(incoming)
        launchAlarmScreen()
        handler.postDelayed(watchdog, 2000)
        handler.postDelayed(rampUp, 1000)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        releaseAll()
        _current.value = null
        instance = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- UI

    private fun ringPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this, 1,
        Intent(this, RingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun goForeground(alarm: Alarm?) {
        val pi = ringPendingIntent()
        val title = alarm?.let { "⏰ ${it.timeText()} ${it.amPm()}" } ?: "⏰ Alarm"
        val text = alarm?.label?.takeIf { it.isNotBlank() } ?: "Time to wake up! Tap to open."
        val n = NotificationCompat.Builder(this, WakeyApp.CH_RING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true) // pops the alarm screen over the lock screen
            .build()
        try {
            val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            ServiceCompat.startForeground(this, NOTIF_ID, n, type)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    /**
     * Directly start the alarm screen. The full-screen notification alone is NOT enough:
     * when the phone is unlocked and in use, Android only shows a small heads-up banner.
     * With "Display over other apps" granted we're allowed to start the screen ourselves.
     */
    private fun launchAlarmScreen() {
        try {
            startActivity(
                Intent(this, RingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not start alarm screen directly", e)
        }
    }

    private fun isLocked(): Boolean =
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    /** Every 3s: if the alarm screen isn't visible (user pressed Home, screen off...), bring it back. */
    private val watchdog = object : Runnable {
        override fun run() {
            if (_current.value == null) return
            if (!RingActivity.isVisible && (Settings.canDrawOverlays(this@RingService) || isLocked())) {
                launchAlarmScreen()
            }
            handler.postDelayed(this, 3000)
        }
    }

    // ---------------------------------------------------------------- sound

    private val rampUp = object : Runnable {
        override fun run() {
            if (_current.value == null) return
            if (ramp < 1f) {
                ramp = (ramp + 0.04f).coerceAtMost(1f)
                applyVolume()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun applyVolume() {
        val v = if (missionActive) 0.12f else ramp
        try {
            player?.setVolume(v, v)
        } catch (_: Exception) {
        }
    }

    private fun startSound(alarm: Alarm) {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, max(1, (maxVol * alarm.volume).roundToInt()), 0)
        } catch (e: Exception) {
            savedVolume = -1
        }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val candidates = listOfNotNull(
            alarm.soundUri?.let { Uri.parse(it) },
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            Settings.System.DEFAULT_RINGTONE_URI,
        )
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(attrs)
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.prepare()
                mp.setVolume(ramp, ramp)
                mp.start()
                player = mp
                return
            } catch (e: Exception) {
                Log.w(TAG, "Can't play $uri", e)
                mp.release()
            }
        }
        // Last resort: plain beeps. An alarm must never be silent.
        try {
            tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            handler.post(beep)
        } catch (e: Exception) {
            Log.e(TAG, "No sound possible", e)
        }
    }

    private val beep = object : Runnable {
        override fun run() {
            if (_current.value == null) return
            tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
            handler.postDelayed(this, 1200)
        }
    }

    private fun startVibration(alarm: Alarm) {
        if (!alarm.vibrate) return
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 900, 700), 0)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        } catch (e: Exception) {
            Log.w(TAG, "vibrate failed", e)
        }
    }

    // ---------------------------------------------------------------- locks

    private fun acquireLocks() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakey:ring").apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L)
        }
        try {
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "wakey:screen",
            ).acquire(15_000L)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- stop

    private fun doDismiss() {
        _current.value?.let { AlarmStore.setSnoozesUsed(this, it.id, 0) }
        stopEverything()
    }

    private fun doSnooze(): Boolean {
        val a = _current.value ?: return false
        if (snoozesLeft(this, a) <= 0) return false
        AlarmStore.setSnoozesUsed(this, a.id, AlarmStore.snoozesUsed(this, a.id) + 1)
        AlarmScheduler.scheduleSnooze(this, a.id, a.snoozeMinutes)
        stopEverything()
        return true
    }

    private fun releaseAll() {
        handler.removeCallbacksAndMessages(null)
        player?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
        tone?.release()
        tone = null
        vibrator?.cancel()
        vibrator = null
        if (savedVolume >= 0) {
            try {
                (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
            } catch (_: Exception) {
            }
            savedVolume = -1
        }
        cpuLock?.let { if (it.isHeld) it.release() }
        cpuLock = null
    }

    private fun stopEverything() {
        releaseAll()
        _current.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
