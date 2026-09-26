package com.aditya.wakey.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.ui.theme.W

@Composable
fun SettingsScreen(
    tick: Int,
    onChanged: () -> Unit,
    onQuickTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val checks = remember(tick) { Health.checks(ctx) }
    var pendingFallback by remember { mutableStateOf<Health.Fix.Permission?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val fb = pendingFallback
        // Denied permanently -> the system won't show the dialog again, open settings instead.
        if (!granted && fb != null) Health.open(ctx, fb.fallback)
        pendingFallback = null
        onChanged()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)) {
                Text("Alarm reliability", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Everything must be green for the alarm screen to pop up every time.",
                    color = W.Text2, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        items(checks, key = { it.key }) { c ->
            Card(
                colors = CardDefaults.cardColors(containerColor = W.Card),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (c.ok) {
                        true -> Icons.Filled.CheckCircle to W.Good
                        false -> Icons.Filled.Warning to (if (c.required) W.Accent else W.Warn)
                        null -> Icons.Filled.Info to W.Warn
                    }
                    Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(c.title, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(c.why, color = W.Text2, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                    if (c.ok != true) {
                        Button(
                            onClick = {
                                when (val f = c.fix) {
                                    is Health.Fix.Permission -> {
                                        pendingFallback = f
                                        permLauncher.launch(f.permission)
                                    }
                                    is Health.Fix.Open -> Health.open(ctx, f.intents)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (c.ok == false && c.required) W.Accent else W.Card2,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) { Text(if (c.ok == null) "Open" else "Fix") }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp)) {
                Text("Test it", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Tap, then lock your phone. After 10 seconds the alarm screen should light up over the lock screen. Try it again while using another app.",
                    color = W.Text2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            Button(
                onClick = onQuickTest,
                colors = ButtonDefaults.buttonColors(containerColor = W.Accent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Text("  Ring a test alarm in 10 s", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
