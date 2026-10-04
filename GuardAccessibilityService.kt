package app.upwake.alarm

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import app.upwake.ring.RingActivity

/**
 * "Turn-off prevention". Only does anything while an alarm / morning check-in is in progress:
 *  - closes the power menu (no power off / restart from the menu)
 *  - closes the notification shade and quick settings
 *  - any other app that opens (Home, Recents, Settings, Instagram…) gets covered by the alarm screen
 * It reads nothing else and never runs outside the alarm.
 */
class GuardAccessibilityService : AccessibilityService() {

    companion object {
        private val POWER_WORDS = listOf(
            "power off", "restart", "reboot", "shut down", "shutdown", "switch off", "turn off",
            "power menu", "बंद करें", "रीस्टार्ट", "पावर बंद",
        )

        fun isEnabled(ctx: Context): Boolean {
            val me = ComponentName(ctx, GuardAccessibilityService::class.java).flattenToString()
            val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            return list.split(':').any { it.equals(me, ignoreCase = true) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var allowed: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            flags = AccessibilityServiceInfo.DEFAULT
        }
        refreshAllowed()
    }

    /** Keyboards and the system permission dialog must keep working during the mission. */
    private fun refreshAllowed() {
        val ime = try {
            getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
        allowed = (ime + listOf(
            packageName,
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.phone", "com.google.android.dialer", "com.android.server.telecom", // emergency calls
        )).toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (RingService.current.value == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in allowed) return

        val cls = event.className?.toString()?.lowercase() ?: ""
        val text = event.text.joinToString(" ").lowercase()

        if (cls.contains("recents") || cls.contains("overview")) {
            bringBack()
            return
        }
        if (pkg == "com.android.systemui" || pkg == "android" || cls.contains("globalactions")) {
            val powerMenu = cls.contains("globalactions") || cls.contains("powermenu") ||
                POWER_WORDS.any { text.contains(it) }
            if (powerMenu) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 150)
            }
            closeShade()
            return
        }

        // Any other app came to the front: put the alarm back on top.
        bringBack()
    }

    private fun closeShade() {
        if (Build.VERSION.SDK_INT >= 31) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        }
    }

    private fun bringBack() {
        try {
            startActivity(
                Intent(this, RingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            )
        } catch (_: Exception) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {}
}
