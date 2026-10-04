package app.upwake.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.upwake.ui.theme.WIcons
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
import app.upwake.alarm.AlarmScheduler
import app.upwake.data.Alarm
import app.upwake.data.AlarmStore
import app.upwake.data.MissionType
import app.upwake.focus.Focus
import app.upwake.focus.FocusStore
import app.upwake.ui.theme.W
import app.upwake.ui.theme.UpwakeTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmStore.init(this)
        AlarmScheduler.rescheduleAll(this)
        Focus.sync(this)
        setContent { UpwakeTheme { AppRoot() } }
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
    var settings by remember { mutableStateOf(false) }
    var worldEditing by remember { mutableStateOf(false) }
    var worldAdding by remember { mutableStateOf(false) }
    var journal by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    var onboarded by remember { mutableStateOf(Prefs.onboarded(ctx)) }
    var legalOk by remember { mutableStateOf(LegalPrefs.accepted(ctx)) }
    var legalTab by remember { mutableStateOf<Int?>(null) }

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
    LaunchedEffect(onboarded) {
        if (onboarded && Build.VERSION.SDK_INT >= 33 &&
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
        !legalOk -> LegalScreen(onAccept = {
            LegalPrefs.accept(ctx)
            legalOk = true
        })

        !onboarded -> OnboardingScreen(onFinish = { create ->
            Prefs.setOnboarded(ctx)
            onboarded = true
            healthTick++
            if (create) {
                isNew = true
                editing = Alarm(id = AlarmStore.newId(ctx), hour = 7, minute = 0, days = Alarm.WEEKDAYS)
            }
        })

        legalTab != null -> LegalScreen(onBack = { legalTab = null }, startTab = legalTab ?: 0)

        settings && !about && !journal && legalTab == null -> {
            BackHandler { settings = false }
            Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).clickable { settings = false }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(WIcons.Close, "Close", tint = W.Dawn, modifier = Modifier.size(20.dp))
                        Text("Done", color = W.Dawn, fontSize = 17.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                SettingsScreen(
                    tick = healthTick,
                    onChanged = { healthTick++ },
                    onJournal = { journal = true },
                    onAbout = { about = true },
                    onLegal = { legalTab = 0 },
                    onQuickTest = {
                        AlarmScheduler.scheduleTest(ctx, AlarmStore.QUICK_TEST_ID, 10)
                        toast("Lock your phone now. Test alarm in 10 seconds.")
                    },
                )
            }
        }

        about -> AboutScreen(onBack = { about = false }, onLegal = { legalTab = it })

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

        worldAdding -> WorldClockScreen(
            editing = false,
            adding = true,
            onAddingChange = { worldAdding = it },
        )

        else -> Scaffold(
            containerColor = W.Bg,
            topBar = {
                TopBar(
                    problems = problems,
                    onSettings = { settings = true },
                    tab = tab,
                    worldEditing = worldEditing,
                    onWorldEdit = { worldEditing = !worldEditing },
                    onAdd = {
                        when (tab) {
                            0 -> {
                                val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 8) }
                                isNew = true
                                editing = Alarm(
                                    id = AlarmStore.newId(ctx),
                                    hour = now.get(Calendar.HOUR_OF_DAY),
                                    minute = 0,
                                )
                            }
                            3 -> { worldEditing = false; worldAdding = true }
                        }
                    },
                )
            },
            bottomBar = { BottomBar(tab = tab, onTab = { tab = it; worldEditing = false }) },
        ) { pad ->
            when (tab) {
                0 -> AlarmListScreen(
                    alarms = alarms,
                    problems = problems,
                    modifier = Modifier.padding(pad),
                    onOpen = { isNew = false; editing = it },
                    onToggle = { a, on ->
                        val saved = persist(a.copy(enabled = on))
                        if (on) toast(AlarmScheduler.ringInText(AlarmScheduler.nextTrigger(saved)))
                    },
                    onFix = { settings = true },
                )
                1 -> FocusScreen(
                    tick = healthTick,
                    onPickApps = { pickingApps = true },
                    modifier = Modifier.padding(pad),
                )
                2 -> StopwatchScreen(Modifier.padding(pad))
                else -> WorldClockScreen(
                    editing = worldEditing,
                    adding = false,
                    onAddingChange = { worldAdding = it },
                    modifier = Modifier.padding(pad),
                )
            }
        }
    }
}

/** Apple-style nav row: Settings top-left, contextual actions top-right. */
@Composable
private fun TopBar(
    problems: Int,
    onSettings: () -> Unit,
    tab: Int,
    worldEditing: Boolean,
    onWorldEdit: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(W.Bg).statusBarsPadding().height(48.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(WIcons.Gear, "Settings", tint = W.Dawn, modifier = Modifier.size(24.dp))
            if (problems > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 8.dp)
                        .size(9.dp).clip(CircleShape).background(W.Red),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (tab == 3) {
            Text(
                if (worldEditing) "Done" else "Edit", color = W.Dawn, fontSize = 17.sp,
                fontWeight = if (worldEditing) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onWorldEdit).padding(10.dp),
            )
        }
        if (tab == 0 || tab == 3) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.Plus, "Add", tint = W.Dawn, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(W.Bg).navigationBarsPadding()) {
        HorizontalDivider(color = W.Line, thickness = 0.5.dp)
        Row(Modifier.fillMaxWidth().height(58.dp)) {
            listOf(
                WIcons.AlarmClock to "Alarm",
                WIcons.Moon to "Focus",
                WIcons.Stopwatch to "Stopwatch",
                WIcons.Globe to "World Clock",
            ).forEachIndexed { i, (icon, label) ->
                val c = if (tab == i) W.Dawn else W.Text2
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, label, tint = c, modifier = Modifier.size(24.dp))
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = c, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}
