package com.aditya.wakey.ui

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.aditya.wakey.alarm.AlarmScheduler
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.MissionType
import com.aditya.wakey.mission.PhotoMatcher
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import com.aditya.wakey.ui.theme.icon

@Composable
fun EditAlarmScreen(
    alarm: Alarm,
    isNew: Boolean,
    onChange: (Alarm) -> Unit,
    onSetupMission: (Pair<Alarm, MissionType>) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Alarm) -> Unit,
    onTest: (Alarm) -> Unit,
) {
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)

    // Wheel state lives here so the three wheels never overwrite each other.
    var h12 by remember(alarm.id) { mutableIntStateOf(if (alarm.hour % 12 == 0) 12 else alarm.hour % 12) }
    var minute by remember(alarm.id) { mutableIntStateOf(alarm.minute) }
    var pm by remember(alarm.id) { mutableStateOf(alarm.hour >= 12) }

    fun current(): Alarm = alarm.copy(hour = (h12 % 12) + if (pm) 12 else 0, minute = minute)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            onChange(current().copy(soundUri = uri?.toString()))
        }
    }
    val soundName = remember(alarm.soundUri) {
        try {
            val uri = alarm.soundUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_ALARM)
            RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) ?: "Default alarm"
        } catch (e: Exception) {
            "Default alarm"
        }
    }

    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        // ---- top bar
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
            Text(
                if (isNew) "New alarm" else "Edit alarm",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (!isNew) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = W.Text2) }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            // ---- time wheels
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth().height(64.dp)
                        .background(W.Card, RoundedCornerShape(16.dp)),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WheelPicker(
                        values = (1..12).map { it.toString() },
                        selected = h12 - 1, loop = true,
                        onSelected = { h12 = it + 1 },
                    )
                    Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    WheelPicker(
                        values = (0..59).map { "%02d".format(it) },
                        selected = minute, loop = true,
                        onSelected = { minute = it },
                    )
                    Spacer(Modifier.width(8.dp))
                    WheelPicker(
                        values = listOf("AM", "PM"),
                        selected = if (pm) 1 else 0, loop = false,
                        onSelected = { pm = it == 1 }, width = 76.dp,
                    )
                }
            }
            Text(
                AlarmScheduler.ringInText(AlarmScheduler.nextTrigger(current())),
                color = W.Accent, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
            )

            // ---- repeat days
            Section("Repeat", current().daysText())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Alarm.ORDER.forEachIndexed { i, day ->
                    val on = day in alarm.days
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(if (on) W.Accent else W.Card)
                            .clickable {
                                val d = if (on) alarm.days - day else alarm.days + day
                                onChange(current().copy(days = d))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            Alarm.LETTERS[i], fontWeight = FontWeight.Bold,
                            color = if (on) Color.White else W.Text2,
                        )
                    }
                }
            }

            // ---- mission
            Section("Wake-up mission", if (alarm.hasMission) alarm.mission.title else "None")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MissionType.entries.forEach { m ->
                    val selected = if (m == MissionType.NONE) !alarm.hasMission else alarm.hasMission && alarm.mission == m
                    Column(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                            .background(if (selected) W.AccentDim else W.Card)
                            .border(
                                BorderStroke(2.dp, if (selected) W.Accent else Color.Transparent),
                                RoundedCornerShape(18.dp),
                            )
                            .clickable {
                                when {
                                    m == MissionType.NONE ->
                                        onChange(current().copy(mission = MissionType.NONE, missionData = null))
                                    selected -> Unit
                                    else -> onSetupMission(current() to m)
                                }
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            m.icon(), null,
                            tint = if (selected) W.Accent else W.Text2,
                            modifier = Modifier.size(30.dp),
                        )
                        Text(
                            m.title, color = if (selected) Color.White else W.Text2,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            if (alarm.hasMission && alarm.mission == MissionType.PHOTO) {
                val thumb = remember(alarm.missionData) {
                    alarm.missionData?.let { BitmapFactory.decodeFile(it) }?.asImageBitmap()
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp).background(W.Card, RoundedCornerShape(18.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (thumb != null) {
                        Image(
                            thumb, null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Match strictness", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (0..2).forEach { s ->
                                val on = alarm.photoSensitivity == s
                                Text(
                                    PhotoMatcher.sensitivityName(s),
                                    color = if (on) Color.White else W.Text2, fontSize = 13.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (on) W.Accent else W.Card2)
                                        .clickable { onChange(current().copy(photoSensitivity = s)) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    Text(
                        "Retake", color = W.Accent, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSetupMission(current() to MissionType.PHOTO) },
                    )
                }
            }
            if (alarm.hasMission && alarm.mission == MissionType.BARCODE) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp).background(W.Card, RoundedCornerShape(18.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Registered barcode", color = W.Text2, fontSize = 13.sp)
                        Text(
                            alarm.missionData ?: "", color = Color.White, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "Rescan", color = W.Accent, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSetupMission(current() to MissionType.BARCODE) },
                    )
                }
            }

            // ---- sound
            Section("Sound", null)
            Card2 {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val i = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                            .putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                alarm.soundUri?.let { Uri.parse(it) },
                            )
                        try {
                            picker.launch(i)
                        } catch (_: Exception) {
                        }
                    }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RowIcon(WIcons.Music)
                    Text(
                        soundName, color = Color.White, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text("Change", color = W.Accent, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(WIcons.Volume)
                    Slider(
                        value = alarm.volume,
                        onValueChange = { onChange(current().copy(volume = it.coerceAtLeast(0.1f))) },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White, activeTrackColor = W.Accent, inactiveTrackColor = W.Card2,
                        ),
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(WIcons.Vibrate)
                    Text("Vibration", color = Color.White, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Switch(
                        checked = alarm.vibrate,
                        onCheckedChange = { onChange(current().copy(vibrate = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = W.Accent, checkedThumbColor = Color.White),
                    )
                }
            }

            // ---- snooze
            Section("Snooze", if (alarm.snoozeMinutes == 0) "Off" else "${alarm.snoozeMinutes} min · up to ${alarm.maxSnoozes} times")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 5, 10, 15).forEach { m ->
                    val on = alarm.snoozeMinutes == m
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (on) W.Accent else W.Card)
                            .clickable { onChange(current().copy(snoozeMinutes = m)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (m == 0) "Off" else "$m", color = if (on) Color.White else W.Text2, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---- label
            Section("Label", null)
            OutlinedTextField(
                value = alarm.label,
                onValueChange = { onChange(current().copy(label = it.take(40))) },
                placeholder = { Text("e.g. Gym, College, Flight") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = W.Accent, unfocusedBorderColor = W.Card2,
                    focusedContainerColor = W.Card, unfocusedContainerColor = W.Card,
                    cursorColor = W.Accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { onTest(current()) },
                border = BorderStroke(1.dp, W.Card2),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp),
            ) {
                Icon(WIcons.Play, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Save & test: ring in 10 seconds", color = Color.White, modifier = Modifier.padding(start = 10.dp))
            }

            Spacer(Modifier.height(24.dp))
        }

        // ---- save
        Button(
            onClick = { onSave(current()) },
            colors = ButtonDefaults.buttonColors(containerColor = W.Accent),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(58.dp),
        ) { Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, null, tint = W.Text2, modifier = Modifier.size(22.dp))
}

@Composable
private fun Section(title: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = W.Text2, fontSize = 14.sp)
    }
}

@Composable
private fun Card2(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(W.Card, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}
