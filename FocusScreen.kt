package com.aditya.wakey.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.focus.Focus
import com.aditya.wakey.focus.FocusConfig
import com.aditya.wakey.focus.FocusStore
import com.aditya.wakey.focus.ShieldVpnService
import com.aditya.wakey.focus.formatMin
import com.aditya.wakey.ui.theme.ClockStyle
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(tick: Int, onPickApps: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    FocusStore.init(ctx)
    val cfg by FocusStore.cfg.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var localTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }
    val active = cfg.isActive(now)
    var timeSheet by remember { mutableStateOf<Int?>(null) } // 0 = bedtime, 1 = wake

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) Focus.sync(ctx)
        localTick++
    }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun update(c: FocusConfig) {
        FocusStore.save(ctx, c)
        val needShield = c.adultBlock || (c.enabled && c.apps.isNotEmpty())
        val consent = VpnService.prepare(ctx)
        if (needShield && consent != null) {
            try {
                vpnLauncher.launch(consent)
            } catch (_: Exception) {
            }
        } else {
            Focus.sync(ctx)
        }
        localTick++
    }

    fun locked(): Boolean {
        if (active) toast("Strict mode: locked until ${formatMin(cfg.endMin)}")
        return active
    }

    // permission states, refreshed on resume (tick) and after our own changes
    val usageOk = remember(tick, localTick) { Focus.hasUsageAccess(ctx) }
    val overlayOk = remember(tick, localTick) { Settings.canDrawOverlays(ctx) }
    val vpnConsented = remember(tick, localTick) { VpnService.prepare(ctx) == null }
    val shieldOn = remember(tick, localTick, now) { ShieldVpnService.running }
    val privateDns = remember(tick, localTick) { Focus.privateDnsBypass(ctx) }
    val needsShield = cfg.adultBlock || (cfg.enabled && cfg.apps.isNotEmpty())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 40.dp),
    ) {
        item {
            Column(Modifier.padding(start = 6.dp, bottom = 18.dp)) {
                Text("Sleep focus", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = W.Text, letterSpacing = (-0.5).sp)
                Text(
                    "No scrolling before bed or right after waking up.",
                    color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // ---------------------------------------------------------------- hero
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(if (active) W.Accent else W.Card).padding(22.dp),
            ) {
                val fg = if (active) W.OnAccent else W.Text
                val sub = if (active) W.OnAccent.copy(alpha = 0.6f) else W.Text2
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (active) WIcons.Lock else WIcons.Moon, null, tint = fg, modifier = Modifier.size(18.dp))
                    Text(
                        when {
                            active -> "BLOCKING NOW"
                            cfg.enabled && cfg.apps.isNotEmpty() -> "SCHEDULED"
                            else -> "OFF"
                        },
                        color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
                    Text(formatMin(cfg.startMin), style = ClockStyle, fontSize = 30.sp, color = fg)
                    Text("  →  ", color = sub, fontSize = 20.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text(formatMin(cfg.endMin), style = ClockStyle, fontSize = 30.sp, color = fg)
                }
                Text(
                    when {
                        active -> "Strict mode. Settings are locked until ${formatMin(cfg.endMin)}."
                        cfg.enabled && cfg.apps.isEmpty() -> "Pick at least one app to block."
                        else -> "Blocked from 30 min before bed until 30 min after you wake."
                    },
                    color = sub, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // ---------------------------------------------------------------- schedule
        item { SectionTitle("Schedule") }
        item {
            SectionCard {
                SettingRow(
                    WIcons.Moon, "Bedtime", formatMin(cfg.bedH * 60 + cfg.bedM),
                    onClick = { if (!locked()) timeSheet = 0 },
                )
                RowDivider()
                SettingRow(
                    WIcons.Sun, "Wake up", formatMin(cfg.wakeH * 60 + cfg.wakeM),
                    onClick = { if (!locked()) timeSheet = 1 },
                )
            }
        }

        // ---------------------------------------------------------------- block
        item { SectionTitle("Block") }
        item {
            SectionCard {
                SettingRow(
                    WIcons.Shield, "Sleep focus",
                    trailing = {
                        WSwitch(cfg.enabled) { on ->
                            if (!on && locked()) return@WSwitch
                            update(cfg.copy(enabled = on))
                        }
                    },
                )
                RowDivider()
                SettingRow(
                    WIcons.Apps, "Blocked apps",
                    value = if (cfg.apps.isEmpty()) "None" else "${cfg.apps.size} selected",
                    onClick = onPickApps,
                )
                RowDivider()
                SettingRow(
                    WIcons.Globe, "Adult websites",
                    value = if (cfg.adultBlock) "Always" else null,
                    trailing = {
                        WSwitch(cfg.adultBlock) { on ->
                            if (!on && locked()) return@WSwitch
                            update(cfg.copy(adultBlock = on))
                        }
                    },
                )
            }
            Text(
                "Websites of the apps you block (Instagram, YouTube…) are also blocked in your browser during the window.",
                color = W.Text3, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp),
            )
        }

        // ---------------------------------------------------------------- setup
        item { SectionTitle("Setup") }
        item {
            SectionCard {
                SetupRow(
                    WIcons.Eye, "Usage access", "Lets Wakey see which app is open.", usageOk,
                ) { Health.open(ctx, listOf(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))) }
                RowDivider()
                SetupRow(
                    WIcons.Apps, "Display over other apps", "Shows the block screen.", overlayOk,
                ) {
                    Health.open(
                        ctx,
                        listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${ctx.packageName}"))),
                    )
                }
                RowDivider()
                SetupRow(
                    WIcons.Globe, "Wakey Shield",
                    if (needsShield) "Local website filter. Nothing leaves your phone." else "Turns on with website blocking.",
                    if (!needsShield) true else vpnConsented && shieldOn,
                ) {
                    val consent = VpnService.prepare(ctx)
                    if (consent != null) vpnLauncher.launch(consent) else Focus.sync(ctx)
                    localTick++
                }
                RowDivider()
                SetupRow(
                    WIcons.Lock, "Always-on VPN",
                    "Recommended: choose Wakey Shield and turn on 'Always-on VPN' so it survives restarts.",
                    null,
                ) { Health.open(ctx, listOf(Intent(Settings.ACTION_VPN_SETTINGS))) }
                if (privateDns) {
                    RowDivider()
                    SetupRow(
                        WIcons.Warning, "Private DNS is on",
                        "It bypasses website blocking. Set Private DNS to Automatic or Off.",
                        false,
                    ) { Health.open(ctx, listOf(Intent(Settings.ACTION_WIRELESS_SETTINGS))) }
                }
            }
            Text(
                "Only one VPN can run at a time, so Wakey Shield pauses if you switch on another VPN.",
                color = W.Text3, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp),
            )
        }
    }

    // ---------------------------------------------------------------- time sheet
    val which = timeSheet
    if (which != null) {
        val startMin = if (which == 0) cfg.bedH * 60 + cfg.bedM else cfg.wakeH * 60 + cfg.wakeM
        var h12 by remember(which) { mutableIntStateOf(((startMin / 60) % 12).let { if (it == 0) 12 else it }) }
        var minute by remember(which) { mutableIntStateOf(startMin % 60) }
        var pm by remember(which) { mutableStateOf(startMin / 60 >= 12) }
        WSheet(if (which == 0) "Bedtime" else "Wake-up time", onDismiss = { timeSheet = null }) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth().height(WHEEL_ITEM_H).clip(RoundedCornerShape(14.dp)).background(W.Card2),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WheelPicker((1..12).map { it.toString() }, h12 - 1, { h12 = it + 1 }, loop = true)
                    Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = W.Text)
                    WheelPicker((0..59).map { "%02d".format(it) }, minute, { minute = it }, loop = true)
                    Spacer(Modifier.width(6.dp))
                    WheelPicker(listOf("AM", "PM"), if (pm) 1 else 0, { pm = it == 1 }, loop = false, width = 72.dp)
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                "Save",
                onClick = {
                    val h = (h12 % 12) + if (pm) 12 else 0
                    update(if (which == 0) cfg.copy(bedH = h, bedM = minute) else cfg.copy(wakeH = h, wakeM = minute))
                    timeSheet = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SetupRow(icon: ImageVector, title: String, desc: String, ok: Boolean?, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = W.Text, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(title, color = W.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(desc, color = W.Text2, fontSize = 13.sp, lineHeight = 17.sp)
        }
        when (ok) {
            true -> Box(
                Modifier.size(22.dp).clip(CircleShape).background(W.Accent),
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.Check, null, tint = W.OnAccent, modifier = Modifier.size(14.dp)) }
            false -> Pill("Fix", onFix)
            null -> Pill("Open", onFix, filled = false)
        }
    }
}
