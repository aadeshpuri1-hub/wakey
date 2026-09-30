package com.aditya.wakey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.alarm.AlarmScheduler
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.ui.theme.ClockStyle
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import com.aditya.wakey.ui.theme.icon
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
    val next = remember(alarms, now) {
        alarms.filter { it.enabled }
            .map { it to AlarmScheduler.nextTrigger(it, now) }
            .minByOrNull { it.second }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 14.dp)) {
                Text(
                    if (next != null) AlarmScheduler.ringInText(next.second, now) else "No alarms on",
                    fontSize = 30.sp, fontWeight = FontWeight.Bold, color = W.Text, letterSpacing = (-0.5).sp,
                )
                Text(
                    if (next != null) {
                        SimpleDateFormat("EEE, d MMM  ·  h:mm a", Locale.getDefault()).format(Date(next.second))
                    } else "Tap + to set an alarm",
                    color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (problems > 0) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(W.Card).clickable(onClick = onFix).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(W.Card2),
                        contentAlignment = Alignment.Center,
                    ) { Icon(WIcons.Warning, null, tint = W.Text, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(
                            "$problems setting${if (problems > 1) "s" else ""} need attention",
                            fontWeight = FontWeight.SemiBold, color = W.Text, fontSize = 15.sp,
                        )
                        Text("Your alarm may not pop up", color = W.Text2, fontSize = 13.sp)
                    }
                    Pill("Fix", onFix)
                }
            }
        }

        if (alarms.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(W.Card),
                        contentAlignment = Alignment.Center,
                    ) { Icon(WIcons.AlarmClock, null, tint = W.Text, modifier = Modifier.size(40.dp)) }
                    Spacer(Modifier.height(18.dp))
                    Text("No alarms yet", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = W.Text)
                    Text("Tap + to create one", color = W.Text2, modifier = Modifier.padding(top = 4.dp))
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
    val main = if (a.enabled) W.Text else W.Text3
    val sub = if (a.enabled) W.Text2 else W.Text3
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(W.Card)
            .clickable(onClick = onClick).padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.daysText(), color = sub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (a.label.isNotBlank()) {
                    Text("  ·  ${a.label}", color = sub, fontSize = 13.sp, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(a.timeText(), style = ClockStyle, fontSize = 50.sp, color = main)
                Text(
                    a.amPm(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = main,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                )
            }
            if (a.hasMission) {
                Row(
                    Modifier.padding(top = 2.dp).clip(RoundedCornerShape(8.dp)).background(W.Card2)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(a.mission.icon(), null, tint = sub, modifier = Modifier.size(14.dp))
                    Text(
                        a.mission.title, color = sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        WSwitch(checked = a.enabled, onCheckedChange = onToggle)
    }
}
