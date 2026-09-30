package com.aditya.wakey.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons

@Composable
fun SettingsScreen(
    tick: Int,
    onChanged: () -> Unit,
    onQuickTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val checks = remember(tick) { Health.checks(ctx) }
    val bad = checks.count { it.required && it.ok == false }
    var pendingFallback by remember { mutableStateOf<Health.Fix.Permission?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val fb = pendingFallback
        // Denied permanently -> the system won't show the dialog again, open settings instead.
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 32.dp),
    ) {
        item {
            Column(Modifier.padding(start = 6.dp, bottom = 8.dp)) {
                Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = W.Text, letterSpacing = (-0.5).sp)
                Text(
                    if (bad == 0) "Everything's set. Your alarm will ring." else "$bad thing${if (bad > 1) "s" else ""} could stop your alarm",
                    color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item { SectionTitle("Alarm reliability") }
        item {
            SectionCard {
                checks.forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(if (c.ok == true) W.Accent else W.Card2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (c.ok == true) WIcons.CheckCircle else WIcons.Warning, null,
                                tint = if (c.ok == true) W.OnAccent else W.Text,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
                            Text(c.title, fontWeight = FontWeight.SemiBold, color = W.Text, fontSize = 15.sp)
                            Text(c.why, color = W.Text2, fontSize = 13.sp, lineHeight = 17.sp)
                        }
                        when (c.ok) {
                            true -> Text("On", color = W.Text3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            false -> Pill(if (c.required) "Fix" else "Allow", { fix(c) }, filled = c.required)
                            null -> Pill("Open", { fix(c) }, filled = false)
                        }
                    }
                }
            }
        }

        item { SectionTitle("Test") }
        item {
            SectionCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Tap, then lock your phone. In 10 seconds the alarm should take over the lock screen. Try it again while using another app.",
                        color = W.Text2, fontSize = 14.sp, lineHeight = 19.sp,
                    )
                    PrimaryButton(
                        "Ring a test alarm in 10 s", onClick = onQuickTest, icon = WIcons.Play,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    )
                }
            }
        }
    }
}
