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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.aditya.wakey.alarm.AlarmScheduler
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.MissionType
import com.aditya.wakey.mission.PhotoMatcher
import com.aditya.wakey.ui.theme.Inter
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import com.aditya.wakey.ui.theme.icon

private enum class Sheet { NONE, MISSION, SNOOZE }

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
    var sheet by remember { mutableStateOf(Sheet.NONE) }

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
            RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) ?: "Default"
        } catch (e: Exception) {
            "Default"
        }
    }

    Column(Modifier.fillMaxSize().background(W.Bg).statusBarsPadding()) {
        // ---------------------------------------------------------------- top bar
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(WIcons.Close, onBack)
            Text(
                if (isNew) "New alarm" else "Edit alarm",
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = W.Text,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            if (!isNew) IconCircle(WIcons.Trash, onDelete) else Spacer(Modifier.size(44.dp))
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            // ------------------------------------------------------------ time
            SectionCard(Modifier.padding(top = 8.dp)) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(WHEEL_ITEM_H)
                            .clip(RoundedCornerShape(14.dp)).background(W.Card2),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WheelPicker(
                            values = (1..12).map { it.toString() },
                            selected = h12 - 1, loop = true,
                            onSelected = { h12 = it + 1 },
                        )
                        Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = W.Text)
                        WheelPicker(
                            values = (0..59).map { "%02d".format(it) },
                            selected = minute, loop = true,
                            onSelected = { minute = it },
                        )
                        Spacer(Modifier.width(6.dp))
                        WheelPicker(
                            values = listOf("AM", "PM"),
                            selected = if (pm) 1 else 0, loop = false,
                            onSelected = { pm = it == 1 }, width = 72.dp,
                        )
                    }
                }
            }
            Text(
                AlarmScheduler.ringInText(AlarmScheduler.nextTrigger(current())),
                color = W.Text2, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
            )

            // ------------------------------------------------------------ repeat
            SectionTitle("Repeat")
            SectionCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Alarm.ORDER.forEachIndexed { i, day ->
                        val on = day in alarm.days
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (on) W.Accent else W.Card2)
                                .clickable {
                                    val d = if (on) alarm.days - day else alarm.days + day
                                    onChange(current().copy(days = d))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                Alarm.LETTERS[i], fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = if (on) W.OnAccent else W.Text2,
                            )
                        }
                    }
                }
                RowDivider()
                SettingRow(
                    icon = WIcons.Repeat, title = "Every day",
                    trailing = {
                        WSwitch(alarm.days.size == 7) {
                            onChange(current().copy(days = if (it) Alarm.ORDER.toSet() else emptySet()))
                        }
                    },
                )
            }

            // ------------------------------------------------------------ mission
            SectionTitle("Wake-up mission")
            SectionCard {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // main slot
                    Column(
                        Modifier.size(88.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (alarm.hasMission) W.Accent else W.Card2)
                            .clickable { sheet = Sheet.MISSION },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (alarm.hasMission) {
                            Icon(alarm.mission.icon(), null, tint = W.OnAccent, modifier = Modifier.size(28.dp))
                            Text(
                                alarm.mission.title, color = W.OnAccent, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp),
                            )
                        } else {
                            Icon(WIcons.Plus, null, tint = W.Text, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            if (alarm.hasMission) "${alarm.mission.title} mission" else "No mission",
                            color = W.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        )
                        Text(
                            when {
                                alarm.hasMission && alarm.mission == MissionType.PHOTO ->
                                    "Retake the registered photo to turn the alarm off."
                                alarm.hasMission ->
                                    "Scan the registered barcode to turn the alarm off."
                                else -> "Add a mission you can only finish out of bed."
                            },
                            color = W.Text2, fontSize = 13.sp, lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                if (alarm.hasMission && alarm.mission == MissionType.PHOTO) {
                    val thumb = remember(alarm.missionData) {
                        alarm.missionData?.let {
                            BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = 4 })
                        }?.asImageBitmap()
                    }
                    RowDivider()
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (thumb != null) {
                            Image(
                                thumb, null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Match strictness", color = W.Text2, fontSize = 12.sp)
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (0..2).forEach { s ->
                                    val on = alarm.photoSensitivity == s
                                    Text(
                                        PhotoMatcher.sensitivityName(s),
                                        color = if (on) W.OnAccent else W.Text2, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(RoundedCornerShape(50))
                                            .background(if (on) W.Accent else W.Card2)
                                            .clickable { onChange(current().copy(photoSensitivity = s)) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------ sound
            SectionTitle("Sound")
            SectionCard {
                SettingRow(
                    icon = WIcons.Music, title = "Ringtone", value = soundName,
                    onClick = {
                        val i = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm.soundUri?.let { Uri.parse(it) })
                        try {
                            picker.launch(i)
                        } catch (_: Exception) {
                        }
                    },
                )
                RowDivider()
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(WIcons.Volume, null, tint = W.Text, modifier = Modifier.size(22.dp))
                    Slider(
                        value = alarm.volume,
                        onValueChange = { onChange(current().copy(volume = it.coerceAtLeast(0.1f))) },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = W.Accent, activeTrackColor = W.Accent, inactiveTrackColor = W.Card2,
                        ),
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                }
                RowDivider()
                SettingRow(
                    icon = WIcons.Vibrate, title = "Vibration",
                    trailing = { WSwitch(alarm.vibrate) { onChange(current().copy(vibrate = it)) } },
                )
            }

            // ------------------------------------------------------------ more
            SectionTitle("More")
            SectionCard {
                SettingRow(
                    icon = WIcons.Snooze, title = "Snooze",
                    value = if (alarm.snoozeMinutes == 0) "Off" else "${alarm.snoozeMinutes} min, ${alarm.maxSnoozes}×",
                    onClick = { sheet = Sheet.SNOOZE },
                )
                RowDivider()
                Row(
                    Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(WIcons.Tag, null, tint = W.Text, modifier = Modifier.size(22.dp))
                    Text(
                        "Label", color = W.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    BasicTextField(
                        value = alarm.label,
                        onValueChange = { onChange(current().copy(label = it.take(40))) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = W.Text2, fontSize = 15.sp, fontFamily = Inter, textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(W.Accent),
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (alarm.label.isEmpty()) Text("None", color = W.Text3, fontSize = 15.sp)
                                inner()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ---------------------------------------------------------------- bottom bar
        Row(
            Modifier.fillMaxWidth().background(W.Bg).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryButton("Preview", onClick = { onTest(current()) }, modifier = Modifier.weight(1f))
            PrimaryButton("Save", onClick = { onSave(current()) }, modifier = Modifier.weight(2f))
        }
    }

    // -------------------------------------------------------------------- sheets
    when (sheet) {
        Sheet.MISSION -> WSheet("Wake-up mission", onDismiss = { sheet = Sheet.NONE }) {
            MissionOption(
                icon = WIcons.Camera, title = "Photo",
                desc = "Take a photo of a spot away from your bed. Retake it to dismiss.",
                selected = alarm.hasMission && alarm.mission == MissionType.PHOTO,
            ) {
                sheet = Sheet.NONE
                onSetupMission(current() to MissionType.PHOTO)
            }
            Spacer(Modifier.height(10.dp))
            MissionOption(
                icon = WIcons.Barcode, title = "Barcode",
                desc = "Scan something like your toothpaste. Scan it again to dismiss.",
                selected = alarm.hasMission && alarm.mission == MissionType.BARCODE,
            ) {
                sheet = Sheet.NONE
                onSetupMission(current() to MissionType.BARCODE)
            }
            if (alarm.hasMission) {
                Spacer(Modifier.height(16.dp))
                SecondaryButton(
                    "Remove mission",
                    onClick = {
                        sheet = Sheet.NONE
                        onChange(current().copy(mission = MissionType.NONE, missionData = null))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Sheet.SNOOZE -> WSheet("Snooze", onDismiss = { sheet = Sheet.NONE }) {
            Text("Interval", color = W.Text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            ChipRow(
                options = listOf(0, 3, 5, 10, 15), selected = alarm.snoozeMinutes,
                label = { if (it == 0) "Off" else "$it m" },
                onSelect = { onChange(current().copy(snoozeMinutes = it)) },
            )
            if (alarm.snoozeMinutes > 0) {
                Text(
                    "Max snoozes", color = W.Text2, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                ChipRow(
                    options = listOf(1, 2, 3, 5), selected = alarm.maxSnoozes,
                    label = { "$it×" },
                    onSelect = { onChange(current().copy(maxSnoozes = it)) },
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Done", onClick = { sheet = Sheet.NONE }, modifier = Modifier.fillMaxWidth())
        }

        Sheet.NONE -> Unit
    }
}

@Composable
private fun IconCircle(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = W.Text, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun MissionOption(icon: ImageVector, title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(W.Card2)
            .border(BorderStroke(1.5.dp, if (selected) W.Accent else W.Card2), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(W.Bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = W.Text, modifier = Modifier.size(24.dp)) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = W.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(desc, color = W.Text2, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Icon(WIcons.Chevron, null, tint = W.Text3, modifier = Modifier.size(18.dp))
    }
}
