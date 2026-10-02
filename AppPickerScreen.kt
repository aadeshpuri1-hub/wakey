package com.aditya.wakey.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.aditya.wakey.focus.SocialCatalog
import com.aditya.wakey.ui.theme.Inter
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AppInfo(val pkg: String, val label: String, val icon: ImageBitmap?)

private fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    @Suppress("DEPRECATION")
    val ris = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    return ris.distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { ri ->
            val icon = try {
                ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
            } catch (e: Exception) {
                null
            }
            AppInfo(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), icon)
        }
        .sortedBy { it.label.lowercase() }
}

/**
 * Pick apps to block. During the active window (strict mode) apps can be added but not removed:
 * [locked] holds the ones that can't be unticked.
 */
@Composable
fun AppPickerScreen(selected: Set<String>, locked: Set<String>, onDone: (Set<String>) -> Unit) {
    val ctx = LocalContext.current
    var picked by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    BackHandler { onDone(picked) }

    val apps by produceState<List<AppInfo>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadApps(ctx) }
    }

    fun toggle(pkg: String) {
        if (pkg in picked) {
            if (pkg in locked) {
                Toast.makeText(ctx, "Strict mode: can't unblock until the window ends", Toast.LENGTH_SHORT).show()
                return
            }
            picked = picked - pkg
        } else {
            picked = picked + pkg
        }
    }

    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { onDone(picked) },
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.Close, "Close", tint = W.Text, modifier = Modifier.size(22.dp)) }
            Text(
                "Blocked apps", color = W.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Pill("Done (${picked.size})", { onDone(picked) })
            Box(Modifier.size(8.dp))
        }

        // search
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(48.dp)
                .clip(RoundedCornerShape(14.dp)).background(W.Card).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(WIcons.Search, null, tint = W.Text2, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = W.Text, fontSize = 16.sp, fontFamily = Inter),
                cursorBrush = SolidColor(W.Accent),
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Search apps", color = W.Text3, fontSize = 16.sp)
                        inner()
                    }
                },
            )
        }

        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading apps…", color = W.Text2)
            }
        } else {
            AppList(list, query, picked, locked, ::toggle)
        }
    }
}

@Composable
private fun AppList(
    list: List<AppInfo>,
    query: String,
    picked: Set<String>,
    locked: Set<String>,
    toggle: (String) -> Unit,
) {
    run {
        val q = query.trim().lowercase()
        val shown = if (q.isEmpty()) list else list.filter { it.label.lowercase().contains(q) }
        val suggested = shown.filter { it.pkg in SocialCatalog.suggested }
        val rest = shown.filter { it.pkg !in SocialCatalog.suggested }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            if (suggested.isNotEmpty()) {
                item { SectionTitle("Social media on this phone") }
                item {
                    SectionCard {
                        suggested.forEachIndexed { i, a ->
                            if (i > 0) RowDivider()
                            AppRow(a, a.pkg in picked, a.pkg in locked) { toggle(a.pkg) }
                        }
                    }
                }
            }
            item { SectionTitle("All apps") }
            items(rest, key = { it.pkg }) { a ->
                AppRow(a, a.pkg in picked, a.pkg in locked) { toggle(a.pkg) }
            }
        }
    }
}

@Composable
private fun AppRow(a: AppInfo, on: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = a.icon
        if (icon != null) {
            Image(icon, null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(W.Card2))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(a.label, color = W.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (on) W.Accent else W.Card2),
            contentAlignment = Alignment.Center,
        ) {
            if (on) {
                Icon(
                    if (isLocked) WIcons.Lock else WIcons.Check, null,
                    tint = W.OnAccent, modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
