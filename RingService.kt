package app.upwake.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import app.upwake.R
import app.upwake.UpwakeApp
import app.upwake.data.Alarm
import app.upwake.data.AlarmStore
import app.upwake.ring.RingActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.roundToInt

/** Where the person is in the wake-up flow. Lives in the service so it survives screen-off / recreation. */
enum class RingStage { ALARM, MISSION, WRITING, DONE, GRATITUDE }

/**
 * Owns a ringing alarm until the person has completed everything (mission + gratitude).
 * Layers that keep the person inside Upwake:
 *  1. full-screen notification (lock screen), re-fired whenever the screen isn't showing
 *  2. direct relaunch of the alarm screen
 *  3. a black overlay that covers every other app ("Display over other apps")
 *  4. GuardAccessibilityService: closes the power menu / notification shade, kicks out other apps
 *  5. survives reboot: the ringing state is saved and resumes after the phone restarts
 */
class RingService : Service() {

    companion object {
        private const val TAG = "UpwakeRing"
        private const val EXTRA_ID = "alarm_id"
        private const val EXTRA_GRATITUDE = "gratitude_only"
        private const val NOTIF_ID = 4242
        private const val NUDGE_ID = 4245
        private const val PERSIST = "upwake_ring"

        private val _current = MutableStateFlow<Alarm?>(null)
        /** The alarm in progress (ringing, mission or gratitude), or null. */
        val current: StateFlow<Alarm?> = _current

        private val _stage = MutableStateFlow(RingStage.ALARM)
        val stage: StateFlow<RingStage> = _stage

        /** Paragraphs completed in the writing mission (kept if the person wanders off). */
        val writingDone = MutableStateFlow(0)
        val writingSet = MutableStateFlow<List<Int>>(emptyList())

        private var instance: RingService? = null

        fun start(ctx: Context, alarmId: Int, gratitudeOnly: Boolean = false) {
            val i = Intent(ctx, RingService::class.java)
                .putExtra(EXTRA_ID, alarmId).putExtra(EXTRA_GRATITUDE, gratitudeOnly)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun dismiss() {
            instance?.doDismiss()
        }

        /** @return false if snooze isn't allowed (off or no snoozes left). */
        fun snooze(): Boolean = instance?.doSnooze() ?: false

        fun setStage(s: RingStage) {
            _stage.value = s
            instance?.applyVolume()
        }

        /** Silence the alarm but keep the person in Upwake for the gratitude check-in. */
        fun toGratitude() {
            instance?.doGratitude()
        }

        /** Called by the alarm screen when it shows/hides. */
        fun onScreenVisibility() {
            instance?.let {
                it.applyVolume()
                if (RingActivity.isVisible) it.removeGuard()
            }
        }

        fun snoozesLeft(ctx: Context, alarm: Alarm): Int =
            if (alarm.snoozeMinutes <= 0) 0
            else (alarm.maxSnoozes - AlarmStore.snoozesUsed(ctx, alarm.id)).coerceAtLeast(0)

        /** Alarm that was in progress when the phone shut down: (id, gratitudeOnly). */
        fun pending(ctx: Context): Pair<Int, Boolean>? {
            val p = ctx.getSharedPreferences(PERSIST, Context.MODE_PRIVATE)
            val id = p.getInt("id", -1)
            return if (id == -1) null else id to p.getBoolean("grat", false)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var cpuLock: PowerManager.WakeLock? = null
    private var savedVolume = -1
    private var targetVolume = -1
    private var ramp = 0.35f
    private var guard: View? = null
    private var nudgeCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ID, -1) ?: -1
        val gratitudeOnly = intent?.getBooleanExtra(EXTRA_GRATITUDE, false) ?: false
        val incoming = if (id != -1) AlarmStore.getForRing(this, id) else null
        val running = _current.value

        // Must call startForeground within a few seconds of startForegroundService().
        goForeground(running ?: incoming)

        if (running != null) {
            launchAlarmScreen()
            return START_REDELIVER_INTENT
        }
        if (incoming == null) {
            stopEverything()
            return START_NOT_STICKY
        }

        _current.value = incoming
        writingDone.value = 0
        writingSet.value = emptyList()
        acquireLocks()
        if (gratitudeOnly) {
            _stage.value = RingStage.GRATITUDE
            persist(incoming.id, true)
        } else {
            _stage.value = RingStage.ALARM
            persist(incoming.id, false)
            ramp = 0.35f
            startSound(incoming)
            startVibration(incoming)
            handler.postDelayed(rampUp, 1000)
        }
        goForeground(incoming)
        launchAlarmScreen()
        handler.postDelayed(watchdog, 1500)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        releaseAll()
        removeGuard()
        _current.value = null
        instance = null
        super.onDestroy()
    }

    private fun persist(id: Int, gratitude: Boolean) {
        getSharedPreferences(PERSIST, Context.MODE_PRIVATE).edit()
            .putInt("id", id).putBoolean("grat", gratitude).apply()
    }

    private fun clearPersist() {
        getSharedPreferences(PERSIST, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // ---------------------------------------------------------------- notification + screen

    private fun ringPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this, 1,
        Intent(this, RingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun buildNotification(alarm: Alarm?, channelAlert: Boolean): android.app.Notification {
        val pi = ringPendingIntent()
        val grat = _stage.value == RingStage.GRATITUDE
        val title = when {
            grat -> "Finish your morning check-in"
            alarm != null -> "Alarm · ${alarm.timeText()} ${alarm.amPm()}"
            else -> "Alarm"
        }
        val text = if (grat) "Tap to write today's gratitude." else alarm?.label?.takeIf { it.isNotBlank() } ?: "Time to wake up. Tap to open."
        return NotificationCompat.Builder(this, UpwakeApp.CH_RING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(!channelAlert)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
    }

    private fun goForeground(alarm: Alarm?) {
        try {
            val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(alarm, false), type)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    /** Re-fires a full-screen notification: Android then puts the alarm screen over the lock screen again. */
    private fun nudgeFullScreen() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NUDGE_ID + (nudgeCount++ % 2), buildNotification(_current.value, true))
            handler.postDelayed({
                nm.cancel(NUDGE_ID)
                nm.cancel(NUDGE_ID + 1)
            }, 2500)
        } catch (_: Exception) {
        }
    }

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

    private fun isInteractive(): Boolean =
        getSystemService(PowerManager::class.java)?.isInteractive == true

    /** Every 1.5 s until the whole flow is done. */
    private val watchdog = object : Runnable {
        override fun run() {
            if (_current.value == null) return
            enforceVolume()
            if (RingActivity.isVisible) {
                removeGuard()
            } else {
                val ringing = _stage.value != RingStage.GRATITUDE
                when {
                    isInteractive() && !isLocked() -> {
                        showGuard() // covers whatever app is open
                        launchAlarmScreen()
                    }
                    isInteractive() -> {
                        nudgeFullScreen()
                        launchAlarmScreen()
                    }
                    ringing -> nudgeFullScreen() // screen off while ringing: turn it back on
                }
            }
            handler.postDelayed(this, 1500)
        }
    }

    // ---------------------------------------------------------------- guard overlay

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    /** Black full-screen window over every app. Only the way back into Upwake is clickable. */
    private fun showGuard() {
        if (guard != null || !Settings.canDrawOverlays(this)) return
        val bold = try { ResourcesCompat.getFont(this, R.font.inter_bold) } catch (e: Exception) { null }
        val regular = try { ResourcesCompat.getFont(this, R.font.inter_regular) } catch (e: Exception) { null }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32f), 0, dp(32f), 0)
        }
        val grat = _stage.value == RingStage.GRATITUDE
        col.addView(TextView(this).apply {
            text = if (grat) "Not so fast." else "Your alarm is still on."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            typeface = bold
            gravity = Gravity.CENTER
        })
        col.addView(TextView(this).apply {
            text = if (grat) "Finish your morning check-in first." else "Finish your mission to turn it off."
            setTextColor(Color.parseColor("#8C8C8C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = regular
            gravity = Gravity.CENTER
            setPadding(0, dp(10f), 0, 0)
        })
        val btn = TextView(this).apply {
            text = "Go back"
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = bold
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16f).toFloat()
            }
            setPadding(dp(40f), dp(16f), dp(40f), dp(16f))
            setOnClickListener { launchAlarmScreen() }
        }
        col.addView(btn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(32f) })
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true // swallow every touch
            addView(col, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER,
            ))
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE,
        )
        try {
            getSystemService(WindowManager::class.java).addView(root, lp)
            guard = root
        } catch (e: Exception) {
            Log.w(TAG, "guard overlay failed", e)
        }
    }

    fun removeGuard() {
        val v = guard ?: return
        guard = null
        try {
            getSystemService(WindowManager::class.java).removeView(v)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- sound

    private val rampUp = object : Runnable {
        override fun run() {
            if (_current.value == null || player == null) return
            if (ramp < 1f) {
                ramp = (ramp + 0.04f).coerceAtMost(1f)
                applyVolume()
                handler.postDelayed(this, 1000)
            }
        }
    }

    /** Quieter only while the person is actually on the mission screen. Leave it = full blast. */
    fun applyVolume() {
        val s = _stage.value
        val quiet = (s == RingStage.MISSION || s == RingStage.WRITING) && RingActivity.isVisible
        val v = if (quiet) 0.12f else ramp
        try {
            player?.setVolume(v, v)
        } catch (_: Exception) {
        }
    }

    /** Volume buttons can't turn the alarm down: the alarm stream is put back every tick. */
    private fun enforceVolume() {
        if (player == null && tone == null) return
        if (targetVolume < 0) return
        try {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) < targetVolume) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun startSound(alarm: Alarm) {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            targetVolume = max(1, (maxVol * alarm.volume).roundToInt())
            am.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        } catch (e: Exception) {
            savedVolume = -1
            targetVolume = -1
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
        try {
            tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            handler.post(beep)
        } catch (e: Exception) {
            Log.e(TAG, "No sound possible", e)
        }
    }

    private val beep = object : Runnable {
        override fun run() {
            val t = tone ?: return
            t.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
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

    private fun stopSound() {
        handler.removeCallbacks(rampUp)
        handler.removeCallbacks(beep)
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
        targetVolume = -1
    }

    // ---------------------------------------------------------------- locks

    private fun acquireLocks() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (cpuLock == null) {
            cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "upwake:ring").apply {
                setReferenceCounted(false)
                acquire(60 * 60 * 1000L)
            }
        }
        try {
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "upwake:screen",
            ).acquire(15_000L)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- finish

    private fun doGratitude() {
        val a = _current.value ?: return
        stopSound()
        AlarmStore.setSnoozesUsed(this, a.id, 0)
        _stage.value = RingStage.GRATITUDE
        persist(a.id, true)
        goForeground(a)
    }

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
        stopSound()
        cpuLock?.let { if (it.isHeld) it.release() }
        cpuLock = null
    }

    private fun stopEverything() {
        clearPersist()
        releaseAll()
        removeGuard()
        _current.value = null
        try {
            getSystemService(NotificationManager::class.java).apply {
                cancel(NUDGE_ID)
                cancel(NUDGE_ID + 1)
            }
        } catch (_: Exception) {
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
