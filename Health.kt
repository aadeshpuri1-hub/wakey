package com.aditya.wakey.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.aditya.wakey.mission.hasCamera

/** Everything that has to be true for an alarm to reliably take over the screen. */
object Health {

    sealed class Fix {
        data class Permission(val permission: String, val fallback: List<Intent>) : Fix()
        data class Open(val intents: List<Intent>) : Fix()
    }

    data class Check(
        val key: String,
        val title: String,
        val why: String,
        /** null = can't be detected, user has to check */
        val ok: Boolean?,
        val required: Boolean,
        val fix: Fix,
    )

    private val CHINESE_OEMS = setOf(
        "xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "iqoo", "oneplus",
        "huawei", "honor", "infinix", "tecno", "itel", "meizu", "asus",
    )

    fun isAggressiveOem() = Build.MANUFACTURER.lowercase() in CHINESE_OEMS

    fun isXiaomi() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    fun checks(ctx: Context): List<Check> {
        val pkg = ctx.packageName
        val pkgUri = Uri.parse("package:$pkg")
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
        val notifSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
        val list = mutableListOf<Check>()

        list += Check(
            "notif", "Notifications",
            "The alarm screen is launched through a notification. Without this, nothing pops up.",
            NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            required = true,
            fix = if (Build.VERSION.SDK_INT >= 33) {
                Fix.Permission(Manifest.permission.POST_NOTIFICATIONS, listOf(notifSettings, appDetails))
            } else {
                Fix.Open(listOf(notifSettings, appDetails))
            },
        )

        list += Check(
            "overlay", "Display over other apps",
            "Lets the alarm screen force itself on top even while you're using your phone, and come back if you press Home.",
            Settings.canDrawOverlays(ctx),
            required = true,
            fix = Fix.Open(listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri), appDetails)),
        )

        if (Build.VERSION.SDK_INT >= 34) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            list += Check(
                "fsi", "Full-screen alarms",
                "Android 14+ needs this to show the alarm over the lock screen.",
                nm.canUseFullScreenIntent(),
                required = true,
                fix = Fix.Open(listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri), appDetails)),
            )
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val am = ctx.getSystemService(AlarmManager::class.java)
            list += Check(
                "exact", "Alarms & reminders",
                "Makes the alarm ring at the exact minute instead of 'roughly around then'.",
                am.canScheduleExactAlarms(),
                required = true,
                fix = Fix.Open(listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri), appDetails)),
            )
        }

        val pm = ctx.getSystemService(PowerManager::class.java)
        list += Check(
            "battery", "Unrestricted battery",
            "Stops Android from putting Wakey to sleep overnight.",
            pm.isIgnoringBatteryOptimizations(pkg),
            required = true,
            fix = Fix.Open(
                listOf(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri),
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                    appDetails,
                ),
            ),
        )

        list += Check(
            "guard", "Turn-off prevention",
            "Blocks the power menu, notification shade and other apps until you finish your mission. Accessibility → Wakey turn-off prevention → On.",
            com.aditya.wakey.alarm.GuardAccessibilityService.isEnabled(ctx),
            required = true,
            fix = Fix.Open(listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), appDetails)),
        )

        list += Check(
            "camera", "Camera",
            "Needed for photo and barcode missions.",
            hasCamera(ctx),
            required = false,
            fix = Fix.Permission(Manifest.permission.CAMERA, listOf(appDetails)),
        )

        if (isXiaomi()) {
            list += Check(
                "miui", "Xiaomi: lock screen & pop-ups",
                "In 'Other permissions' turn ON: 'Show on Lock screen' and 'Display pop-up windows while running in the background'. Xiaomi blocks alarm screens without these.",
                null, required = false,
                fix = Fix.Open(
                    listOf(
                        Intent("miui.intent.action.APP_PERM_EDITOR")
                            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                            .putExtra("extra_pkgname", pkg),
                        Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", pkg),
                        appDetails,
                    ),
                ),
            )
        }

        if (isAggressiveOem()) {
            list += Check(
                "autostart", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }}: Autostart",
                "Your phone brand kills background apps. Turn ON Autostart / 'Allow background activity' for Wakey.",
                null, required = false,
                fix = Fix.Open(
                    listOf(
                        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                        Intent().setComponent(ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity")),
                        appDetails,
                    ),
                ),
            )
        }
        return list
    }

    fun problemCount(ctx: Context) = checks(ctx).count { it.required && it.ok == false }

    /** Try each intent until one opens. */
    fun open(ctx: Context, intents: List<Intent>) {
        for (i in intents) {
            try {
                ctx.startActivity(i)
                return
            } catch (_: Exception) {
            }
        }
    }
}
