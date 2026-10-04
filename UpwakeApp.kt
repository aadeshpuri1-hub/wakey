package app.upwake

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import app.upwake.data.AlarmStore

class UpwakeApp : Application() {
    companion object {
        const val CH_RING = "ring_v1"
        const val CH_FOCUS = "focus_v1"
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CH_RING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shows the alarm screen when an alarm goes off"
            setSound(null, null) // the service plays the sound itself
            enableVibration(false) // the service vibrates itself
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
        nm.createNotificationChannel(
            NotificationChannel(CH_FOCUS, "Sleep focus", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows when app blocking is scheduled or active"
                setShowBadge(false)
            },
        )
        AlarmStore.init(this)
    }
}
