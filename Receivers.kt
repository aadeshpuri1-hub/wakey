package app.upwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.upwake.data.AlarmStore

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        val kind = intent.getIntExtra(AlarmScheduler.EXTRA_KIND, AlarmScheduler.KIND_NORMAL)
        val alarm = AlarmStore.getForRing(ctx, id) ?: return

        if (kind == AlarmScheduler.KIND_NORMAL && id != AlarmStore.QUICK_TEST_ID) {
            if (alarm.days.isEmpty()) {
                // one-time alarm: turn it off now that it has fired
                AlarmStore.upsert(ctx, alarm.copy(enabled = false))
            } else {
                AlarmScheduler.schedule(ctx, alarm) // queue next occurrence
            }
        }
        RingService.start(ctx, id, gratitudeOnly = kind == AlarmScheduler.KIND_RESUME_GRATITUDE)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // Exported for system broadcasts only; ignore anything else another app might send.
        if (intent.action !in SYSTEM_ACTIONS) return
        AlarmScheduler.rescheduleAll(ctx)
        app.upwake.focus.Focus.sync(ctx)
        // Phone was switched off mid-alarm? Pick up exactly where it stopped.
        RingService.pending(ctx)?.let { (id, gratitude) ->
            AlarmScheduler.scheduleResume(ctx, id, gratitude)
        }
    }
}

private val SYSTEM_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
)
