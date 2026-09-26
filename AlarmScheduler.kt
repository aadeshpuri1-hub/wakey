package com.aditya.wakey.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.AlarmStore
import com.aditya.wakey.ui.MainActivity
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_FIRE = "com.aditya.wakey.FIRE"
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_KIND = "kind"
    const val KIND_NORMAL = 0
    const val KIND_SNOOZE = 1
    const val KIND_TEST = 2

    private const val SNOOZE_OFFSET = 100_000
    private const val TEST_OFFSET = 200_000

    /** Next time (epoch ms) this alarm should ring, strictly in the future. */
    fun nextTrigger(alarm: Alarm, nowMs: Long = System.currentTimeMillis()): Long {
        val base = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.days.isEmpty()) {
            if (base.timeInMillis <= nowMs) base.add(Calendar.DAY_OF_YEAR, 1)
            return base.timeInMillis
        }
        for (i in 0..7) {
            val c = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            if (c.get(Calendar.DAY_OF_WEEK) in alarm.days && c.timeInMillis > nowMs) return c.timeInMillis
        }
        return base.timeInMillis + 7L * 24 * 60 * 60 * 1000
    }

    fun schedule(ctx: Context, alarm: Alarm) {
        cancel(ctx, alarm.id)
        if (!alarm.enabled) return
        set(ctx, alarm.id, nextTrigger(alarm), alarm.id, KIND_NORMAL)
    }

    fun scheduleSnooze(ctx: Context, alarmId: Int, minutes: Int) {
        set(ctx, alarmId + SNOOZE_OFFSET, System.currentTimeMillis() + minutes * 60_000L, alarmId, KIND_SNOOZE)
    }

    fun scheduleTest(ctx: Context, alarmId: Int, seconds: Int) {
        set(ctx, alarmId + TEST_OFFSET, System.currentTimeMillis() + seconds * 1000L, alarmId, KIND_TEST)
    }

    fun cancel(ctx: Context, alarmId: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (rc in listOf(alarmId, alarmId + SNOOZE_OFFSET)) {
            val pi = PendingIntent.getBroadcast(
                ctx, rc, fireIntent(ctx),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun rescheduleAll(ctx: Context) {
        AlarmStore.all(ctx).forEach { schedule(ctx, it) }
    }

    fun nextEnabled(ctx: Context, nowMs: Long = System.currentTimeMillis()): Pair<Alarm, Long>? =
        AlarmStore.all(ctx).filter { it.enabled }
            .map { it to nextTrigger(it, nowMs) }
            .minByOrNull { it.second }

    private fun fireIntent(ctx: Context) =
        Intent(ctx, AlarmReceiver::class.java).setAction(ACTION_FIRE)

    private fun set(ctx: Context, requestCode: Int, triggerAt: Long, alarmId: Int, kind: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val op = PendingIntent.getBroadcast(
            ctx, requestCode,
            fireIntent(ctx).putExtra(EXTRA_ID, alarmId).putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val show = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            // setAlarmClock = highest priority alarm Android has. Survives Doze,
            // shows the alarm icon in the status bar, and lets us start the ring service.
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), op)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, op)
        }
    }

    fun ringInText(triggerAt: Long, nowMs: Long = System.currentTimeMillis()): String {
        val totalMin = ((triggerAt - nowMs + 59_999) / 60_000).coerceAtLeast(0)
        val d = totalMin / (60 * 24)
        val h = (totalMin / 60) % 24
        val m = totalMin % 60
        return when {
            totalMin < 1 -> "Ring in less than a minute"
            d > 0 -> "Ring in $d day${if (d > 1) "s" else ""} $h hr"
            h > 0 -> "Ring in $h hr $m min"
            else -> "Ring in $m min"
        }
    }
}
