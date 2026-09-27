package com.aditya.wakey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.alarm.AlarmScheduler
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.ui.theme.ClockStyle
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import com.aditya.wakey.ui.theme.icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    problems: Int,
    modifier: Modifier = Modifier,
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onFix: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            now = System.currentTimeMillis()
        }
    }
    val next = alarms.filter { it.enabled }
        .map { it to AlarmScheduler.nextTrigger(it, now) }
        .minByOrNull { it.second }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    if (next != null) AlarmScheduler.ringInText(next.second, now) else "No alarms on",
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
                Text(
                    if (next != null) {
                        SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(next.second))
                    } else "Tap + to set an alarm",
                    color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (problems > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x26FFB020)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onFix),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(WIcons.Warning, null, tint = W.Warn, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                "$problems setting${if (problems > 1) "s" else ""} could stop your alarm",
                                fontWeight = FontWeight.Bold, color = Color.White,
                            )
                            Text("The alarm screen might not pop up. Tap to fix.", color = W.Text2, fontSize = 13.sp)
                        }
                        Text("Fix", color = W.Warn, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (alarms.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(96.dp).background(W.AccentDim, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(WIcons.AlarmClock, null, tint = W.Accent, modifier = Modifier.size(48.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text("No alarms yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Tap the red + button to create one", color = W.Text2)
                }
            }
        }

        items(alarms, key = { it.id }) { a ->
            AlarmCard(a, onClick = { onOpen(a) }, onToggle = { onToggle(a, it) })
        }
    }
}

@Composable
private fun AlarmCard(a: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val main = if (a.enabled) Color.White else W.Text2.copy(alpha = 0.5f)
    Card(
        colors = CardDefaults.cardColors(containerColor = W.Card),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.daysText(), color = if (a.enabled) W.Text2 else main, fontSize = 13.sp)
                    if (a.hasMission) {
                        val c = if (a.enabled) W.Accent else main
                        Row(
                            Modifier.padding(start = 8.dp)
                                .background(if (a.enabled) W.AccentDim else W.Card2, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(a.mission.icon(), null, tint = c, modifier = Modifier.size(13.dp))
                            Text(
                                a.mission.title, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(a.timeText(), style = ClockStyle, fontSize = 48.sp, color = main)
                    Text(
                        a.amPm(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = main,
                        modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                    )
                }
                if (a.label.isNotBlank()) Text(a.label, color = W.Text2, fontSize = 14.sp)
            }
            Switch(
                checked = a.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = W.Accent,
                    uncheckedThumbColor = W.Text2,
                    uncheckedTrackColor = W.Card2,
                    uncheckedBorderColor = W.Card2,
                ),
            )
        }
    }
}
