package app.upwake.ring

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
import app.upwake.alarm.RingService
import app.upwake.data.Alarm
import app.upwake.data.AlarmStore
import app.upwake.alarm.RingStage
import app.upwake.gratitude.GratitudeScreen
import app.upwake.gratitude.GratitudeStore
import app.upwake.ui.theme.Inter
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import app.upwake.data.MissionType
import app.upwake.mission.CameraPreview
import app.upwake.mission.PhotoMatcher
import app.upwake.mission.ScanFrame
import app.upwake.mission.ShutterButton
import app.upwake.mission.hasCamera
import app.upwake.mission.rememberPreviewView
import app.upwake.ui.theme.ClockStyle
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons
import app.upwake.ui.theme.icon
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MISSION_SECONDS = 60
private const val WRITING_IDLE_SECONDS = 90

@Composable
fun RingFlow(alarm: Alarm) {
    val ctx = LocalContext.current
    val stage by RingService.stage.collectAsState()
    var snoozesLeft by remember { mutableIntStateOf(RingService.snoozesLeft(ctx, alarm)) }

    when (stage) {
        RingStage.ALARM -> AlarmFace(
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
                    !alarm.hasMission -> finishAlarm(ctx, alarm)
                    alarm.mission.usesCamera && !hasCamera(ctx) -> RingService.setStage(RingStage.WRITING)
                    alarm.mission == MissionType.STEPS && !motionAvailable(ctx, alarm.mission) -> RingService.setStage(RingStage.WRITING)
                    else -> RingService.setStage(RingStage.MISSION)
                }
            },
            onWriteInstead = { RingService.setStage(RingStage.WRITING) },
        )

        RingStage.MISSION -> MissionHost(
            seconds = MISSION_SECONDS,
            onTimeout = { RingService.setStage(RingStage.ALARM) },
        ) { onAttempt ->
            when (alarm.mission) {
                MissionType.PHOTO -> PhotoMission(
                    path = alarm.missionData ?: "",
                    sensitivity = alarm.photoSensitivity,
                    onAttempt = onAttempt,
                    onSuccess = { RingService.setStage(RingStage.DONE) },
                    onUnavailable = { RingService.setStage(RingStage.WRITING) },
                )
                MissionType.BARCODE -> BarcodeMission(
                    target = alarm.missionData ?: "",
                    onSuccess = { RingService.setStage(RingStage.DONE) },
                    onUnavailable = { RingService.setStage(RingStage.WRITING) },
                )
                MissionType.SQUATS, MissionType.PUSHUPS -> PoseMission(
                    type = alarm.mission,
                    target = alarm.missionData?.toIntOrNull() ?: alarm.mission.counts.first(),
                    onAttempt = onAttempt,
                    onSuccess = { RingService.setStage(RingStage.DONE) },
                    onUnavailable = { RingService.setStage(RingStage.WRITING) },
                )
                MissionType.STEPS -> MotionMission(
                    type = alarm.mission,
                    target = alarm.missionData?.toIntOrNull() ?: alarm.mission.counts.first(),
                    onAttempt = onAttempt,
                    onSuccess = { RingService.setStage(RingStage.DONE) },
                    onUnavailable = { RingService.setStage(RingStage.WRITING) },
                )
                MissionType.NONE -> LaunchedEffect(Unit) { RingService.setStage(RingStage.DONE) }
            }
        }

        RingStage.WRITING -> MissionHost(
            seconds = WRITING_IDLE_SECONDS,
            onTimeout = { RingService.setStage(RingStage.ALARM) },
        ) { onAttempt ->
            WritingMission(onAttempt = onAttempt, onSuccess = { RingService.setStage(RingStage.DONE) })
        }

        RingStage.DONE -> DoneScreen(alarm)

        RingStage.GRATITUDE -> GratitudeScreen(onSave = { topic, text ->
            GratitudeStore.add(ctx, topic, text)
            RingService.dismiss()
        })
    }
}

// ------------------------------------------------------------------ alarm face

@Composable
private fun AlarmFace(
    alarm: Alarm,
    snoozesLeft: Int,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onWriteInstead: () -> Unit,
) {
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

    val bgCtx = LocalContext.current
    val bgKey = remember { RingBg.key(bgCtx) }
    val bgPhoto = remember { if (bgKey == RingBg.PHOTO) RingBg.loadPhoto(bgCtx) else null }
    Box(
        Modifier.fillMaxSize().ringBackground(bgKey, bgPhoto).systemBarsPadding(),
    ) {
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
                    }.border(1.5.dp, W.Dawn, CircleShape),
                )
            }
            Box(
                Modifier.size(120.dp).clip(CircleShape).background(W.DawnDim),
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.AlarmClock, null, tint = W.Dawn, modifier = Modifier.size(52.dp)) }
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
                    .background(W.Dawn).clickable(onClick = onDismiss),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (alarm.hasMission) {
                    Icon(alarm.mission.icon(), null, tint = W.OnDawn, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (alarm.hasMission) "Start mission" else "Dismiss",
                    color = W.OnDawn, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (alarm.hasMission) {
                Text(
                    "Not at home? Write 4 paragraphs instead",
                    color = W.Text2, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 14.dp).clip(RoundedCornerShape(50))
                        .clickable(onClick = onWriteInstead).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ mission host

/** Mission frame with a countdown. Idle too long = back to the loud alarm screen. */
@Composable
private fun MissionHost(
    seconds: Int,
    onTimeout: () -> Unit,
    content: @Composable (onAttempt: () -> Unit) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(seconds) }

    LaunchedEffect(attempt) {
        remaining = seconds
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        onTimeout()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        content { attempt++ }
        LinearProgressIndicator(
            progress = { remaining / seconds.toFloat() },
            color = W.Dawn, trackColor = Color(0x33FFFFFF),
            modifier = Modifier.fillMaxWidth().systemBarsPadding().height(4.dp),
        )
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

// ------------------------------------------------------------------ writing mission (backup)

/** Copy 4 paragraphs exactly (a couple of typos allowed). The backup when you're not near your photo/barcode. */
@Composable
private fun WritingMission(onAttempt: () -> Unit, onSuccess: () -> Unit) {
    LaunchedEffect(Unit) {
        if (RingService.writingSet.value.isEmpty()) {
            RingService.writingSet.value = PARAGRAPHS.indices.shuffled().take(WRITING_COUNT)
        }
    }
    val set by RingService.writingSet.collectAsState()
    val done by RingService.writingDone.collectAsState()
    if (set.size < WRITING_COUNT) return
    val index = done.coerceAtMost(WRITING_COUNT - 1)
    val target = PARAGRAPHS[set[index]]
    var text by remember(index) { mutableStateOf("") }
    val progress = typedProgress(text, target)

    Column(
        Modifier.fillMaxSize().background(W.Bg).systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(WIcons.Keyboard, null, tint = W.Text, modifier = Modifier.size(22.dp))
            Text(
                "Paragraph ${index + 1} of $WRITING_COUNT", color = W.Text, fontSize = 18.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
            Text("${(progress * 100).toInt()}%", color = W.Text2, fontSize = 14.sp)
        }
        Row(Modifier.padding(top = 12.dp).fillMaxWidth()) {
            for (i in 0 until WRITING_COUNT) {
                Box(
                    Modifier.weight(1f).padding(end = if (i < WRITING_COUNT - 1) 6.dp else 0.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(if (i < done) W.Dawn else W.Card2),
                )
            }
        }
        Text(
            "Type this exactly:", color = W.Text2, fontSize = 13.sp,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        Text(
            target, color = W.Text, fontSize = 17.sp, lineHeight = 25.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(W.Card).padding(16.dp),
        )
        Box(
            Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 140.dp)
                .clip(RoundedCornerShape(18.dp)).background(W.Card2).padding(16.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { v ->
                    text = v
                    onAttempt()
                    if (typedMatches(v, target)) {
                        val next = done + 1
                        RingService.writingDone.value = next
                        if (next >= WRITING_COUNT) onSuccess()
                    }
                },
                textStyle = TextStyle(color = W.Text, fontSize = 17.sp, fontFamily = Inter, lineHeight = 25.sp),
                cursorBrush = SolidColor(W.Accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("Start typing…", color = W.Text3, fontSize = 17.sp)
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ done

/** Silences the alarm; real alarms then require today's gratitude before Upwake lets go. */
private fun finishAlarm(ctx: android.content.Context, alarm: Alarm) {
    if (alarm.id != AlarmStore.QUICK_TEST_ID && !GratitudeStore.writtenToday(ctx)) {
        RingService.toGratitude()
    } else {
        RingService.dismiss()
    }
}

@Composable
private fun DoneScreen(alarm: Alarm) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        delay(1000)
        finishAlarm(ctx, alarm)
    }
    Column(
        Modifier.fillMaxSize().background(W.Bg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(140.dp).background(W.DawnDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(WIcons.Check, null, tint = W.Dawn, modifier = Modifier.size(64.dp)) }
        Spacer(Modifier.height(28.dp))
        Text("Mission complete", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("You're up.", color = W.Text2, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
