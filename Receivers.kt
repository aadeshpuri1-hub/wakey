package com.aditya.wakey.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aditya.wakey.data.AlarmStore

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
        RingService.start(ctx, id)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(ctx)
    }
}
