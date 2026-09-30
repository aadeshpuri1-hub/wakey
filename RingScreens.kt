package com.aditya.wakey.ring

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.alarm.RingService
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.MissionType
import com.aditya.wakey.mission.CameraPreview
import com.aditya.wakey.mission.PhotoMatcher
import com.aditya.wakey.mission.ScanFrame
import com.aditya.wakey.mission.ShutterButton
import com.aditya.wakey.mission.hasCamera
import com.aditya.wakey.mission.rememberPreviewView
import com.aditya.wakey.ui.theme.ClockStyle
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import com.aditya.wakey.ui.theme.icon
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Stage { ALARM, MISSION, TYPING, DONE }

private const val MISSION_SECONDS = 60
private const val ESCAPE_AFTER_MS = 3 * 60 * 1000L
private const val PHRASE = "I am awake and I am getting out of bed right now"

@Composable
fun RingFlow(alarm: Alarm) {
    val ctx = LocalContext.current
    var stage by remember { mutableStateOf(Stage.ALARM) }
    val ringStart = remember { System.currentTimeMillis() }
    var snoozesLeft by remember { mutableIntStateOf(RingService.snoozesLeft(ctx, alarm)) }

    LaunchedEffect(stage) {
        RingService.setMissionActive(stage == Stage.MISSION || stage == Stage.TYPING)
    }

    when (stage) {
        Stage.ALARM -> AlarmFace(
            alarm = alarm,
            snoozesLeft = snoozesLeft,
            onSnooze = {
                if (!RingService.snooze()) {
                    snoozesLeft = 0
                    Toast.makeText(ctx, "No snoozes left. Time to get up.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                when {
                    !alarm.hasMission -> RingService.dismiss()
                    !hasCamera(ctx) -> stage = Stage.TYPING
                    else -> stage = Stage.MISSION
                }
            },
        )

        Stage.MISSION -> MissionHost(
            onTimeout = { stage = Stage.ALARM },
            canEscape = { System.currentTimeMillis() - ringStart > ESCAPE_AFTER_MS },
            onEscape = { stage = Stage.TYPING },
        ) { onAttempt ->
            when (alarm.mission) {
                MissionType.PHOTO -> PhotoMission(
                    path = alarm.missionData ?: "",
                    sensitivity = alarm.photoSensitivity,
                    onAttempt = onAttempt,
                    onSuccess = { stage = Stage.DONE },
                    onUnavailable = { stage = Stage.TYPING },
                )
                MissionType.BARCODE -> BarcodeMission(
                    target = alarm.missionData ?: "",
                    onSuccess = { stage = Stage.DONE },
                    onUnavailable = { stage = Stage.TYPING },
                )
                MissionType.NONE -> LaunchedEffect(Unit) { stage = Stage.DONE }
            }
        }

        Stage.TYPING -> MissionHost(
            onTimeout = { stage = Stage.ALARM },
            canEscape = { false },
            onEscape = {},
        ) { onAttempt -> TypingMission(onAttempt = onAttempt, onSuccess = { stage = Stage.DONE }) }

        Stage.DONE -> DoneScreen()
    }
}

// ------------------------------------------------------------------ alarm face

@Composable
private fun AlarmFace(alarm: Alarm, snoozesLeft: Int, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    // Ripple rings drawn in the graphics layer only: no recomposition per frame.
    val ripple = rememberInfiniteTransition(label = "ripple").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "t",
    )

    Box(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Column(
            Modifier.fillMaxWidth().padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)),
                color = W.Text2, fontSize = 17.sp, fontWeight = FontWeight.Medium,
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now)),
                    style = ClockStyle, color = W.Text, fontSize = 104.sp,
                )
                Text(
                    SimpleDateFormat("a", Locale.US).format(Date(now)),
                    color = W.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 22.dp),
                )
            }
            if (alarm.label.isNotBlank()) {
                Text(
                    alarm.label, color = W.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(W.Card)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        // centre: alarm icon with expanding rings
        Box(Modifier.align(Alignment.Center).size(260.dp), contentAlignment = Alignment.Center) {
            for (k in 0..1) {
                Box(
                    Modifier.size(120.dp).graphicsLayer {
                        val t = (ripple.value + k * 0.5f) % 1f
                        scaleX = 1f + t * 1.1f
                        scaleY = 1f + t * 1.1f
                        alpha = (1f - t) * 0.35f
                    }.border(1.5.dp, W.Text, CircleShape),
                )
            }
            Box(
                Modifier.size(120.dp).clip(CircleShape).background(W.Card),
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.AlarmClock, null, tint = W.Text, modifier = Modifier.size(52.dp)) }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (alarm.snoozeMinutes > 0 && snoozesLeft > 0) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(W.Card)
                        .clickable(onClick = onSnooze).padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(WIcons.Snooze, null, tint = W.Text, modifier = Modifier.size(18.dp))
                    Text(
                        "Snooze ${alarm.snoozeMinutes} min  ·  $snoozesLeft left",
                        color = W.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(
                Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp))
                    .background(W.Accent).clickable(onClick = onDismiss),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (alarm.hasMission) {
                    Icon(alarm.mission.icon(), null, tint = W.OnAccent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (alarm.hasMission) "Start mission" else "Dismiss",
                    color = W.OnAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ mission host

/** Mission frame with a countdown. If time runs out, back to the loud alarm screen. */
@Composable
private fun MissionHost(
    onTimeout: () -> Unit,
    canEscape: () -> Boolean,
    onEscape: () -> Unit,
    content: @Composable (onAttempt: () -> Unit) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(MISSION_SECONDS) }
    var showEscape by remember { mutableStateOf(canEscape()) }

    LaunchedEffect(attempt) {
        remaining = MISSION_SECONDS
        while (remaining > 0) {
            delay(1000)
            remaining--
            if (!showEscape) showEscape = canEscape()
        }
        onTimeout()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        content { attempt++ }
        Column(Modifier.fillMaxWidth().systemBarsPadding()) {
            LinearProgressIndicator(
                progress = { remaining / MISSION_SECONDS.toFloat() },
                color = W.Accent, trackColor = Color(0x55FFFFFF),
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            if (showEscape) {
                Text(
                    "Can't do the mission?",
                    color = W.Text2, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.End).padding(12.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(50))
                        .clickable(onClick = onEscape).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp,
        modifier = modifier.padding(16.dp)
            .background(Color(0xAA000000), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

// ------------------------------------------------------------------ photo

@Composable
private fun PhotoMission(
    path: String,
    sensitivity: Int,
    onAttempt: () -> Unit,
    onSuccess: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val ref = remember(path) { PhotoMatcher.load(path)?.let { it to PhotoMatcher.print(it) } }
    if (ref == null) {
        LaunchedEffect(Unit) { onUnavailable() }
        return
    }
    val pv = rememberPreviewView()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val need = PhotoMatcher.threshold(sensitivity)

    Box(Modifier.fillMaxSize()) {
        CameraPreview(pv, Modifier.fillMaxSize(), onError = { onUnavailable() })

        Row(
            Modifier.systemBarsPadding().padding(top = 20.dp, start = 16.dp, end = 16.dp)
                .background(Color(0xAA000000), RoundedCornerShape(18.dp)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                ref.first.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp))
                    .border(2.dp, W.Accent, RoundedCornerShape(12.dp)),
            )
            Column(Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(WIcons.Camera, null, tint = W.Accent, modifier = Modifier.size(18.dp))
                    Text(
                        "Photo mission", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text("Go there and take the same photo", color = W.Text2, fontSize = 14.sp)
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            msg?.let { Hint(it) }
            ShutterButton(enabled = !busy) {
                val bmp = pv.bitmap ?: return@ShutterButton
                busy = true
                onAttempt()
                scope.launch {
                    val score = withContext(Dispatchers.Default) {
                        PhotoMatcher.score(ref.second, PhotoMatcher.print(bmp))
                    }
                    busy = false
                    if (score >= need) {
                        onSuccess()
                    } else {
                        msg = "Not a match yet: ${(score * 100).toInt()}% (need ${(need * 100).toInt()}%).\nMatch the angle and distance."
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ barcode

@Composable
private fun BarcodeMission(target: String, onSuccess: () -> Unit, onUnavailable: () -> Unit) {
    val pv = rememberPreviewView()
    var wrong by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(
            pv, Modifier.fillMaxSize(),
            scanBarcodes = true,
            onBarcodes = { values ->
                if (done) return@CameraPreview
                if (target in values) {
                    done = true
                    onSuccess()
                } else {
                    wrong = true
                }
            },
            onError = { onUnavailable() },
        )
        ScanFrame(Modifier.align(Alignment.Center))
        Column(
            Modifier.systemBarsPadding().padding(top = 20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Hint("Scan your registered barcode\n(ends in …${target.takeLast(4)})")
        }
        if (wrong) {
            Hint(
                "That's a different barcode",
                Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 32.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ typing fallback

private fun normalize(s: String) =
    s.lowercase().filter { it.isLetter() || it == ' ' }.split(' ').filter { it.isNotBlank() }.joinToString(" ")

@Composable
private fun TypingMission(onAttempt: () -> Unit, onSuccess: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(W.Bg).systemBarsPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(WIcons.Keyboard, null, tint = W.Accent, modifier = Modifier.size(28.dp))
            Text(
                "Type this exactly", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Text(
            PHRASE, color = W.Text2, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 20.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onAttempt()
                if (normalize(it) == normalize(PHRASE)) onSuccess()
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = W.Accent, unfocusedBorderColor = W.Card2,
                focusedContainerColor = W.Card, unfocusedContainerColor = W.Card,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ------------------------------------------------------------------ done

@Composable
private fun DoneScreen() {
    LaunchedEffect(Unit) {
        delay(1500)
        RingService.dismiss()
    }
    Column(
        Modifier.fillMaxSize().background(W.Bg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(140.dp).background(W.Card, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(WIcons.Sun, null, tint = W.Text, modifier = Modifier.size(64.dp)) }
        Spacer(Modifier.height(28.dp))
        Text("Good morning!", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("Mission complete. You're up.", color = W.Text2, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
