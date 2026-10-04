package app.upwake.ring

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.data.MissionType
import app.upwake.ui.theme.ClockStyle
import app.upwake.ui.theme.W
import app.upwake.ui.theme.icon
import kotlin.math.sqrt

/** True if this phone has the sensor a motion mission needs. */
fun motionAvailable(ctx: Context, type: MissionType): Boolean {
    val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
    val s = if (type == MissionType.PUSHUPS) Sensor.TYPE_PROXIMITY else Sensor.TYPE_ACCELEROMETER
    return sm.getDefaultSensor(s) != null
}

/**
 * Counts reps from the phone's sensors. No permissions needed.
 * - Steps: accelerometer peaks while walking with the phone.
 * - Squats: phone held at the chest; a big dip then a push back up = 1 squat.
 * - Push-ups: phone on the floor under your face; proximity sensor near -> far = 1 push-up.
 */
private class RepCounter(val type: MissionType, val onRep: () -> Unit) : SensorEventListener {
    private var armed = false
    private var lastRep = 0L
    private var smooth = SensorManager.GRAVITY_EARTH

    override fun onSensorChanged(e: SensorEvent) {
        val now = SystemClock.elapsedRealtime()
        when (type) {
            MissionType.PUSHUPS -> {
                val near = e.values[0] < (e.sensor.maximumRange.coerceAtMost(5f)) * 0.6f
                if (near) {
                    armed = true
                } else if (armed && now - lastRep > 600) {
                    armed = false
                    lastRep = now
                    onRep()
                }
            }
            else -> {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                val mag = sqrt(x * x + y * y + z * z)
                smooth = smooth * 0.75f + mag * 0.25f
                if (type == MissionType.STEPS) {
                    if (smooth > 10.9f) armed = true
                    else if (armed && smooth < 10.0f && now - lastRep > 280) {
                        armed = false
                        lastRep = now
                        onRep()
                    }
                } else { // squats: down (light) then up (heavy)
                    if (smooth < 8.6f) armed = true
                    else if (armed && smooth > 11.0f && now - lastRep > 900) {
                        armed = false
                        lastRep = now
                        onRep()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
fun MotionMission(
    type: MissionType,
    target: Int,
    onAttempt: () -> Unit,
    onSuccess: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val ctx = LocalContext.current
    var done by remember { mutableIntStateOf(0) }
    val attempt by rememberUpdatedState(onAttempt)

    DisposableEffect(type) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(
            if (type == MissionType.PUSHUPS) Sensor.TYPE_PROXIMITY else Sensor.TYPE_ACCELEROMETER,
        )
        val counter = RepCounter(type) {
            if (done < target) {
                done++
                attempt()
            }
        }
        if (sm == null || sensor == null) onUnavailable()
        else sm.registerListener(counter, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(counter) }
    }
    LaunchedEffect(done) { if (done >= target) onSuccess() }

    val left = (target - done).coerceAtLeast(0)
    val (title, how) = when (type) {
        MissionType.STEPS -> "Walk $target steps" to "Hold your phone and walk around. Steps in bed don't count."
        MissionType.SQUATS -> "Do $target squats" to "Hold your phone against your chest and squat all the way down, then stand up."
        else -> "Do $target push-ups" to "Put your phone on the floor under your face. Go down until your face is close to the screen, then push up."
    }

    Column(
        Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(type.icon(), null, tint = W.Dawn, modifier = Modifier.size(40.dp))
        Text(
            title, color = W.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Box(Modifier.padding(vertical = 36.dp).size(220.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { done / target.toFloat() },
                color = W.Dawn, trackColor = W.Card2, strokeWidth = 10.dp,
                modifier = Modifier.size(220.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$left", style = ClockStyle, color = W.Text, fontSize = 84.sp)
                Text("to go", color = W.Text2, fontSize = 15.sp)
            }
        }
        Text(how, color = W.Text2, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
    }
}
