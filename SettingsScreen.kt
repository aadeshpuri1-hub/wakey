package app.upwake.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.gratitude.GratitudeStore
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons

@Composable
fun SettingsScreen(
    tick: Int,
    onChanged: () -> Unit,
    onQuickTest: () -> Unit,
    onJournal: () -> Unit,
    onAbout: () -> Unit,
    onLegal: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val checks = remember(tick) { Health.checks(ctx) }
    val bad = checks.count { it.required && it.ok == false }
    var pendingFallback by remember { mutableStateOf<Health.Fix.Permission?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val fb = pendingFallback
        if (!granted && fb != null) Health.open(ctx, fb.fallback)
        pendingFallback = null
        onChanged()
    }

    var disclosure by remember { mutableStateOf<Health.Check?>(null) }

    fun fix(c: Health.Check) {
        if (c.key == "guard") {
            disclosure = c
            return
        }
        when (val f = c.fix) {
            is Health.Fix.Permission -> {
                pendingFallback = f
                permLauncher.launch(f.permission)
            }
            is Health.Fix.Open -> Health.open(ctx, f.intents)
        }
    }

    var showAll by remember { mutableStateOf(false) }
    var bgSheet by remember { mutableStateOf(false) }
    var bgTitle by remember { mutableStateOf(app.upwake.ring.RingBg.title(ctx)) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && app.upwake.ring.RingBg.savePhoto(ctx, uri)) bgTitle = "Your photo"
        bgSheet = false
    }
    val needsFix = checks.filter { it.ok != true }
    val shown = if (showAll) checks else needsFix

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
    ) {
        item {
            Text(
                "Settings", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = W.Text,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }

        item { SectionTitle("Permissions") }
        item {
            SectionCard {
                shown.forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    CompactCheck(c) { fix(c) }
                }
                if (shown.isNotEmpty()) RowDivider()
                SettingRow(
                    if (bad == 0) WIcons.Check else WIcons.Warning,
                    if (showAll) "Hide details" else if (bad == 0) "All set" else "$bad to fix",
                    value = if (showAll) null else "${checks.size - needsFix.size}/${checks.size}",
                    onClick = { showAll = !showAll },
                )
            }
        }

        item { SectionTitle("Alarm") }
        item {
            SectionCard {
                SettingRow(WIcons.Sun, "Alarm background", value = bgTitle, onClick = { bgSheet = true })
                RowDivider()
                SettingRow(WIcons.Play, "Test alarm", value = "in 10 s", onClick = onQuickTest)
                RowDivider()
                SettingRow(
                    WIcons.Book, "Gratitude journal",
                    value = "${GratitudeStore.all(ctx).size}",
                    onClick = onJournal,
                )
            }
        }

        item { SectionTitle("About") }
        item {
            SectionCard {
                SettingRow(WIcons.Shield, "Terms & Privacy", onClick = onLegal)
                RowDivider()
                SettingRow(WIcons.Gear, "About Upwake", value = "v${app.upwake.BuildConfig.VERSION_NAME}", onClick = onAbout)
            }
        }
    }

    if (bgSheet) {
        WSheet("Alarm background", onDismiss = { bgSheet = false }) {
            val current = app.upwake.ring.RingBg.key(ctx)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                app.upwake.ring.RingBg.presets.forEach { pr ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(pr.brush)
                                .border(2.dp, if (current == pr.key) W.Dawn else W.Line, RoundedCornerShape(14.dp))
                                .clickable {
                                    app.upwake.ring.RingBg.setKey(ctx, pr.key)
                                    bgTitle = pr.title
                                    bgSheet = false
                                },
                        )
                        Text(pr.title, color = W.Text2, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SecondaryButton(
                "Choose a photo",
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    disclosure?.let { c ->
        StrictModeDisclosure(
            onAgree = {
                disclosure = null
                val f = c.fix
                if (f is Health.Fix.Open) Health.open(ctx, f.intents)
            },
            onDismiss = { disclosure = null },
        )
    }
}

/** One-line row; the explanation only shows for things that still need fixing. */
@Composable
private fun CompactCheck(c: Health.Check, onFix: () -> Unit) {
    val needsFix = c.ok != true
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .then(if (needsFix) Modifier.clickable(onClick = onFix) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(if (c.ok == true) W.Accent else W.Card2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (c.ok == true) WIcons.Check else WIcons.Warning, null,
                tint = if (c.ok == true) W.OnAccent else W.Text,
                modifier = Modifier.size(12.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(c.title, fontWeight = FontWeight.Medium, color = if (needsFix) W.Text else W.Text2, fontSize = 15.sp)
            if (needsFix) {
                Text(
                    c.why, color = W.Text2, fontSize = 12.sp, lineHeight = 16.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when (c.ok) {
            true -> Unit
            false -> Pill(if (c.required) "Fix" else "Allow", onFix, filled = c.required)
            null -> Pill("Open", onFix, filled = false)
        }
    }
}
