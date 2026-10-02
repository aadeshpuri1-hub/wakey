package com.aditya.wakey.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.gratitude.GratitudeStore
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons

@Composable
fun SettingsScreen(
    tick: Int,
    onChanged: () -> Unit,
    onQuickTest: () -> Unit,
    onJournal: () -> Unit,
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

    fun fix(c: Health.Check) {
        when (val f = c.fix) {
            is Health.Fix.Permission -> {
                pendingFallback = f
                permLauncher.launch(f.permission)
            }
            is Health.Fix.Open -> Health.open(ctx, f.intents)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
    ) {
        item {
            Row(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    "Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = W.Text,
                    letterSpacing = (-0.5).sp, modifier = Modifier.weight(1f),
                )
                Text(
                    if (bad == 0) "All set" else "$bad to fix",
                    color = if (bad == 0) W.Text2 else W.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        item { SectionTitle("Reliability") }
        item {
            SectionCard {
                checks.forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    CompactCheck(c) { fix(c) }
                }
            }
        }

        item { SectionTitle("More") }
        item {
            SectionCard {
                SettingRow(
                    WIcons.Book, "Gratitude journal",
                    value = "${GratitudeStore.all(ctx).size}",
                    onClick = onJournal,
                )
                RowDivider()
                SettingRow(WIcons.Play, "Test alarm", value = "in 10 s", onClick = onQuickTest)
            }
            Text(
                "Test: tap, lock your phone, and the alarm should take over in 10 seconds.",
                color = W.Text3, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp),
            )
        }
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
