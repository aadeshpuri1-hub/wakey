package com.aditya.wakey.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.ui.theme.WIcons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.aditya.wakey.alarm.AlarmScheduler
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.AlarmStore
import com.aditya.wakey.data.MissionType
import com.aditya.wakey.focus.Focus
import com.aditya.wakey.focus.FocusStore
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WakeyTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmStore.init(this)
        AlarmScheduler.rescheduleAll(this)
        Focus.sync(this)
        setContent { WakeyTheme { AppRoot() } }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val alarms by AlarmStore.alarms.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var setup by remember { mutableStateOf<MissionType?>(null) }
    var healthTick by remember { mutableIntStateOf(0) }
    var pickingApps by remember { mutableStateOf(false) }
    var journal by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        healthTick++
        AlarmScheduler.rescheduleAll(ctx) // pick up newly granted exact-alarm permission etc.
        Focus.sync(ctx)
    }
    val problems = remember(healthTick) { Health.problemCount(ctx) }

    // Ask for notifications right away on first launch (Android 13+).
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        healthTick++
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

    fun persist(a: Alarm): Alarm {
        val clean = if (a.mission != MissionType.NONE && a.missionData.isNullOrBlank()) {
            a.copy(mission = MissionType.NONE)
        } else a
        AlarmStore.upsert(ctx, clean)
        AlarmScheduler.schedule(ctx, clean)
        return clean
    }

    val cur = editing
    when {
        pickingApps -> {
            val fc = FocusStore.get(ctx)
            AppPickerScreen(
                selected = fc.apps,
                locked = if (fc.isActive()) fc.apps else emptySet(),
                onDone = { picked ->
                    val latest = FocusStore.get(ctx)
                    // strict mode: never drop apps while the window is active
                    val finalSet = if (latest.isActive()) picked + latest.apps else picked
                    FocusStore.save(ctx, latest.copy(apps = finalSet))
                    Focus.sync(ctx)
                    pickingApps = false
                },
            )
        }

        journal -> JournalScreen(onBack = { journal = false })

        cur != null && setup == MissionType.PHOTO -> PhotoSetupScreen(
            alarmId = cur.id,
            onDone = { path ->
                if (path != null) editing = cur.copy(mission = MissionType.PHOTO, missionData = path)
                setup = null
            },
        )

        cur != null && setup == MissionType.BARCODE -> BarcodeSetupScreen(
            onDone = { value ->
                if (value != null) editing = cur.copy(mission = MissionType.BARCODE, missionData = value)
                setup = null
            },
        )

        cur != null -> EditAlarmScreen(
            alarm = cur,
            isNew = isNew,
            onChange = { editing = it },
            onSetupMission = { editing = it.first; setup = it.second },
            onBack = { editing = null },
            onDelete = {
                AlarmScheduler.cancel(ctx, cur.id)
                AlarmStore.delete(ctx, cur.id)
                editing = null
            },
            onSave = { a ->
                val saved = persist(a.copy(enabled = true))
                toast(AlarmScheduler.ringInText(AlarmScheduler.nextTrigger(saved)))
                editing = null
            },
            onTest = { a ->
                val saved = persist(a.copy(enabled = true))
                editing = saved
                isNew = false
                AlarmScheduler.scheduleTest(ctx, saved.id, 10)
                toast("Lock your phone now. It will ring in 10 seconds.")
            },
        )

        else -> Scaffold(
            containerColor = W.Bg,
            bottomBar = {
                BottomBar(
                    tab = tab, problems = problems, onTab = { tab = it },
                )
            },
            floatingActionButton = {
                if (tab == 0) {
                    Box(
                        Modifier.size(62.dp).clip(CircleShape).background(W.Accent).clickable {
                            val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 8) }
                            isNew = true
                            editing = Alarm(
                                id = AlarmStore.newId(ctx),
                                hour = now.get(Calendar.HOUR_OF_DAY),
                                minute = 0,
                            )
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(WIcons.Plus, "New alarm", tint = W.OnAccent, modifier = Modifier.size(28.dp)) }
                }
            },
        ) { pad ->
            if (tab == 1) {
                FocusScreen(
                    tick = healthTick,
                    onPickApps = { pickingApps = true },
                    modifier = Modifier.padding(pad),
                )
            } else if (tab == 0) {
                AlarmListScreen(
                    alarms = alarms,
                    problems = problems,
                    modifier = Modifier.padding(pad),
                    onOpen = { isNew = false; editing = it },
                    onToggle = { a, on ->
                        val saved = persist(a.copy(enabled = on))
                        if (on) toast(AlarmScheduler.ringInText(AlarmScheduler.nextTrigger(saved)))
                    },
                    onFix = { tab = 2 },
                )
            } else {
                SettingsScreen(
                    tick = healthTick,
                    onChanged = { healthTick++ },
                    modifier = Modifier.padding(pad),
                    onJournal = { journal = true },
                    onQuickTest = {
                        AlarmScheduler.scheduleTest(ctx, AlarmStore.QUICK_TEST_ID, 10)
                        toast("Lock your phone now. Test alarm in 10 seconds.")
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, problems: Int, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(W.Bg).navigationBarsPadding()) {
        HorizontalDivider(color = W.Line, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            listOf(WIcons.AlarmClock to "Alarm", WIcons.Moon to "Focus", WIcons.Gear to "Settings").forEachIndexed { i, (icon, label) ->
                val on = tab == i
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Icon(icon, label, tint = if (on) W.Text else W.Text3, modifier = Modifier.size(24.dp))
                        if (i == 2 && problems > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp)
                                    .size(8.dp).clip(CircleShape).background(W.Accent),
                            )
                        }
                    }
                    Text(
                        label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = if (on) W.Text else W.Text3, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
