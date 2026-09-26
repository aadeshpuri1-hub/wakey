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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.aditya.wakey.alarm.AlarmScheduler
import com.aditya.wakey.data.Alarm
import com.aditya.wakey.data.AlarmStore
import com.aditya.wakey.data.MissionType
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WakeyTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmStore.init(this)
        AlarmScheduler.rescheduleAll(this)
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        healthTick++
        AlarmScheduler.rescheduleAll(ctx) // pick up newly granted exact-alarm permission etc.
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
                NavigationBar(containerColor = Color(0xFF17181E)) {
                    val colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = W.AccentDim,
                        unselectedIconColor = W.Text2,
                        unselectedTextColor = W.Text2,
                    )
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.Notifications, null) },
                        label = { Text("Alarm") }, colors = colors,
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = {
                            BadgedBox(badge = { if (problems > 0) Badge { Text("$problems") } }) {
                                Icon(Icons.Filled.Settings, null)
                            }
                        },
                        label = { Text("Settings") }, colors = colors,
                    )
                }
            },
            floatingActionButton = {
                if (tab == 0) {
                    FloatingActionButton(
                        onClick = {
                            val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 8) }
                            isNew = true
                            editing = Alarm(
                                id = AlarmStore.newId(ctx),
                                hour = now.get(Calendar.HOUR_OF_DAY),
                                minute = 0,
                            )
                        },
                        containerColor = W.Accent,
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) { Icon(Icons.Filled.Add, "New alarm") }
                }
            },
        ) { pad ->
            if (tab == 0) {
                AlarmListScreen(
                    alarms = alarms,
                    problems = problems,
                    modifier = Modifier.padding(pad),
                    onOpen = { isNew = false; editing = it },
                    onToggle = { a, on ->
                        val saved = persist(a.copy(enabled = on))
                        if (on) toast(AlarmScheduler.ringInText(AlarmScheduler.nextTrigger(saved)))
                    },
                    onFix = { tab = 1 },
                )
            } else {
                SettingsScreen(
                    tick = healthTick,
                    onChanged = { healthTick++ },
                    modifier = Modifier.padding(pad),
                    onQuickTest = {
                        AlarmScheduler.scheduleTest(ctx, AlarmStore.QUICK_TEST_ID, 10)
                        toast("Lock your phone now. Test alarm in 10 seconds.")
                    },
                )
            }
        }
    }
}
