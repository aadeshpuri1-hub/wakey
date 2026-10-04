package app.upwake.focus

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.Process
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
import app.upwake.ui.MainActivity

/** Starts/stops the blocker and the shield to match the saved settings. */
object Focus {
    fun sync(ctx: Context) {
        val cfg = FocusStore.get(ctx)
        val appsOn = cfg.enabled && cfg.apps.isNotEmpty()
        if (appsOn) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, FocusService::class.java))
            } catch (e: Exception) {
                Log.w("UpwakeFocus", "can't start focus service", e)
            }
        } else {
            ctx.stopService(Intent(ctx, FocusService::class.java))
        }
        val needShield = cfg.adultBlock || appsOn
        if (needShield) {
            if (VpnService.prepare(ctx) == null && !ShieldVpnService.running) ShieldVpnService.start(ctx)
        } else {
            ShieldVpnService.stop()
        }
    }

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Private DNS set to a specific provider bypasses the shield. */
    fun privateDnsBypass(ctx: Context): Boolean = try {
        Settings.Global.getString(ctx.contentResolver, "private_dns_mode") == "hostname"
    } catch (e: Exception) {
        false
    }
}

/**
 * Watches which app is on screen during the sleep window. If it's a blocked app,
 * covers it with a black "go to sleep" screen and sends the user home.
 */
class FocusService : Service() {

    private companion object {
        /** UsageEvents.Event.MOVE_TO_FOREGROUND / ACTIVITY_RESUMED (same value). */
        const val FOREGROUND_EVENT = 1
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var currentPkg: String? = null
    private var lastQuery = 0L
    private var lastActive: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground(FocusStore.get(this))
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideBlock()
        super.onDestroy()
    }

    private fun goForeground(cfg: FocusConfig) {
        val active = cfg.isActive()
        val text = if (active) {
            "Blocking ${cfg.apps.size} app${if (cfg.apps.size == 1) "" else "s"} until ${formatMin(cfg.endMin)}"
        } else {
            "Next block at ${formatMin(cfg.startMin)}"
        }
        val pi = PendingIntent.getActivity(
            this, 8, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(this, UpwakeApp.CH_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(if (active) "Sleep focus is on" else "Sleep focus")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build()
        try {
            val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(this, 4343, n, type)
        } catch (e: Exception) {
            Log.e("UpwakeFocus", "startForeground failed", e)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            val cfg = FocusStore.get(this@FocusService)
            if (!cfg.enabled || cfg.apps.isEmpty()) {
                stopSelf()
                return
            }
            val active = cfg.isActive()
            if (active != lastActive) {
                lastActive = active
                goForeground(cfg)
            }
            val pm = getSystemService(PowerManager::class.java)
            if (active && pm.isInteractive) {
                val fg = foregroundPackage()
                if (fg != null && fg in cfg.apps) showBlock(fg, cfg)
            } else if (!active) {
                hideBlock()
            }
            val delay = if (active) 700L else {
                (cfg.nextChange() - System.currentTimeMillis()).coerceIn(1_000L, 60_000L)
            }
            handler.postDelayed(this, delay)
        }
    }

    /** Most recent app that came to the foreground, from usage events. */
    private fun foregroundPackage(): String? {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return currentPkg
        val now = System.currentTimeMillis()
        val from = if (lastQuery == 0L) now - 60_000 else lastQuery - 1_000
        try {
            val events = usm.queryEvents(from, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == FOREGROUND_EVENT) currentPkg = e.packageName
            }
        } catch (ex: Exception) {
            Log.w("UpwakeFocus", "usage query failed", ex)
        }
        lastQuery = now
        return currentPkg
    }

    // ------------------------------------------------------------------ block screen

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    private fun font(id: Int): Typeface? = try {
        ResourcesCompat.getFont(this, id)
    } catch (e: Exception) {
        null
    }

    private fun appName(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        SocialCatalog.list.firstOrNull { it.pkg == pkg }?.name ?: "This app"
    }

    private fun showBlock(pkg: String, cfg: FocusConfig) {
        if (overlay != null) return
        if (!Settings.canDrawOverlays(this)) {
            goHome()
            return
        }
        val night = minuteOfDay() >= 12 * 60
        val name = appName(pkg)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32f), 0, dp(32f), 0)
        }
        fun label(t: String, sizeSp: Float, color: Int, tf: Typeface?, top: Int) = TextView(this).apply {
            text = t
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            typeface = tf
            gravity = Gravity.CENTER
            setPadding(0, dp(top.toFloat()), 0, 0)
        }
        col.addView(label(if (night) "Time to sleep" else "Good morning", 34f, Color.WHITE, font(R.font.inter_bold), 0))
        col.addView(
            label(
                if (night) "$name is blocked until ${formatMin(cfg.endMin)}.\nPut the phone down. Tomorrow-you says thanks."
                else "$name unlocks at ${formatMin(cfg.endMin)}.\nStart your day first.",
                16f, Color.parseColor("#8C8C8C"), font(R.font.inter_regular), 14,
            ),
        )
        val btn = label("Got it", 16f, Color.BLACK, font(R.font.inter_bold), 0).apply {
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16f).toFloat()
            }
            setPadding(dp(40f), dp(16f), dp(40f), dp(16f))
            setOnClickListener { hideBlock() }
        }
        val btnLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(36f) }
        col.addView(btn, btnLp)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                col,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER,
                ),
            )
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        )
        try {
            getSystemService(WindowManager::class.java).addView(root, lp)
            overlay = root
        } catch (e: Exception) {
            Log.w("UpwakeFocus", "overlay failed", e)
        }
        goHome()
    }

    private fun hideBlock() {
        val v = overlay ?: return
        overlay = null
        try {
            getSystemService(WindowManager::class.java).removeView(v)
        } catch (_: Exception) {
        }
    }

    private fun goHome() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
        }
    }
}
