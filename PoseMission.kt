package app.upwake.ring

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.data.MissionType
import app.upwake.mission.PoseCamera
import app.upwake.mission.PoseFrame
import app.upwake.mission.rememberPreviewView
import app.upwake.ui.theme.ClockStyle
import app.upwake.ui.theme.W
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

private class P(val x: Float, val y: Float)

/** Angle at b (degrees) formed by a-b-c. */
private fun angle(a: P, b: P, c: P): Float {
    val v1x = a.x - b.x; val v1y = a.y - b.y
    val v2x = c.x - b.x; val v2y = c.y - b.y
    val d = sqrt(v1x * v1x + v1y * v1y) * sqrt(v2x * v2x + v2y * v2y)
    if (d == 0f) return 180f
    return Math.toDegrees(acos(((v1x * v2x + v1y * v2y) / d).coerceIn(-1f, 1f)).toDouble()).toFloat()
}

private const val MIN_LIKELY = 0.55f

/**
 * Coaches squats or push-ups from the camera:
 * - Squat: counts when the knee angle drops below ~100° (proper depth) and you stand back up (>160°).
 * - Push-up: counts when the elbow angle drops below ~90° and you lock out (>150°) with a straight body.
 * Half reps and bad form don't count, and the screen tells you why.
 */
private class Coach(val type: MissionType) {
    var down = false
    var lowest = 180f
    var badForm = false
    var lastRep = 0L

    /** Returns (repCounted, message, messageIsGood). */
    fun update(f: PoseFrame): Triple<Boolean, String, Boolean> {
        fun side(left: Boolean, vararg types: Int): List<P>? {
            val pts = types.map { t ->
                f.pose.getPoseLandmark(t)?.takeIf { it.inFrameLikelihood >= MIN_LIKELY } ?: return null
            }
            return pts.map { P(it.position.x, it.position.y) }
        }
        val L = PoseLandmark.LEFT_SHOULDER; val R = PoseLandmark.RIGHT_SHOULDER
        val now = SystemClock.elapsedRealtime()

        if (type == MissionType.SQUATS) {
            val pts = side(true, L, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
                ?: side(false, R, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
                ?: return Triple(false, "Step back so your whole body is in view", false)
            val (sh, hip, knee, ankle) = pts
            val k = angle(hip, knee, ankle)
            // torso lean: angle between hip->shoulder and straight up
            val lean = angle(sh, hip, P(hip.x, hip.y - 100f))
            if (!down && k < 140f) { down = true; lowest = k; badForm = false }
            if (down) {
                lowest = minOf(lowest, k)
                if (lean > 55f) badForm = true
                if (k > 160f) {
                    down = false
                    return when {
                        lowest > 105f -> Triple(false, "Go lower: thighs to parallel", false)
                        badForm -> Triple(false, "Keep your chest up", false)
                        now - lastRep < 700 -> Triple(false, "Slow down", false)
                        else -> { lastRep = now; Triple(true, "Good rep!", true) }
                    }
                }
                return Triple(false, if (k > 105f) "Lower…" else "Now stand up", true)
            }
            return Triple(false, "Squat down", true)
        } else {
            val pts = side(true, L, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
                ?: side(false, R, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
                ?: return Triple(false, "Put the phone side-on so your whole body shows", false)
            val sh = pts[0]; val el = pts[1]; val wr = pts[2]; val hip = pts[3]; val knee = pts[4]
            // must actually be horizontal (stops "push-ups" while standing)
            if (abs(sh.y - knee.y) > abs(sh.x - knee.x)) return Triple(false, "Get into push-up position", false)
            val e = angle(sh, el, wr)
            val body = angle(sh, hip, knee)
            if (!down && e < 130f) { down = true; lowest = e; badForm = false }
            if (down) {
                lowest = minOf(lowest, e)
                if (body < 150f) badForm = true
                if (e > 150f) {
                    down = false
                    return when {
                        lowest > 95f -> Triple(false, "Go lower: chest to the floor", false)
                        badForm -> Triple(false, "Keep your body straight", false)
                        now - lastRep < 600 -> Triple(false, "Slow down", false)
                        else -> { lastRep = now; Triple(true, "Good rep!", true) }
                    }
                }
                return Triple(false, if (body < 150f) "Hips in line!" else if (e > 95f) "Lower…" else "Push up", body >= 150f)
            }
            return Triple(false, if (body < 150f) "Straighten your body" else "Go down", body >= 150f)
        }
    }
}

private val BONES = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
)

@Composable
fun PoseMission(
    type: MissionType,
    target: Int,
    onAttempt: () -> Unit,
    onSuccess: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val pv = rememberPreviewView()
    val coach = remember(type) { Coach(type) }
    var done by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("Getting ready…") }
    var good by remember { mutableStateOf(true) }
    var frame by remember { mutableStateOf<PoseFrame?>(null) }

    LaunchedEffect(done) { if (done >= target) onSuccess() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PoseCamera(
            pv, Modifier.fillMaxSize(),
            onFrame = { f ->
                frame = f
                if (done >= target) return@PoseCamera
                val (rep, m, ok) = coach.update(f)
                msg = m
                good = ok
                if (rep) {
                    done++
                    onAttempt()
                }
            },
            onError = { onUnavailable() },
        )

        // live skeleton over the preview
        Canvas(Modifier.fillMaxSize()) {
            val f = frame ?: return@Canvas
            val s = max(size.width / f.width, size.height / f.height)
            val ox = (size.width - f.width * s) / 2
            val oy = (size.height - f.height * s) / 2
            fun map(lm: PoseLandmark): Offset {
                val x = lm.position.x * s + ox
                return Offset(if (f.mirrored) size.width - x else x, lm.position.y * s + oy)
            }
            val color = if (good) W.Green else W.Red
            for ((a, b) in BONES) {
                val pa = f.pose.getPoseLandmark(a)?.takeIf { it.inFrameLikelihood >= MIN_LIKELY } ?: continue
                val pb = f.pose.getPoseLandmark(b)?.takeIf { it.inFrameLikelihood >= MIN_LIKELY } ?: continue
                drawLine(color, map(pa), map(pb), strokeWidth = 8f, cap = StrokeCap.Round)
            }
            for (lm in f.pose.allPoseLandmarks) {
                if (lm.inFrameLikelihood >= MIN_LIKELY && lm.landmarkType >= PoseLandmark.LEFT_SHOULDER) {
                    drawCircle(Color.White, 7f, map(lm))
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().systemBarsPadding().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${(target - done).coerceAtLeast(0)}", style = ClockStyle, color = Color.White, fontSize = 96.sp,
            )
            Text(
                if (type == MissionType.SQUATS) "squats to go" else "push-ups to go",
                color = Color.White, fontSize = 16.sp,
            )
        }
        Text(
            msg, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
                .background(if (good) Color(0xCC0B3D1C) else Color(0xCC4A0F0F), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
