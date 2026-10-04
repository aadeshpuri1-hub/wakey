package app.upwake.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.ui.theme.Inter
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object WorldClockStore {
    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("upwake_world", Context.MODE_PRIVATE)
    fun zones(ctx: Context): List<String> =
        p(ctx).getString("zones", "")!!.split(",").filter { it.isNotBlank() }
    fun save(ctx: Context, z: List<String>) = p(ctx).edit().putString("zones", z.joinToString(",")).apply()
}

/** "America/New_York" -> "New York" */
fun cityName(zone: String): String = zone.substringAfterLast('/').replace('_', ' ')

/** Every real city time zone (Region/City), sorted by city name. */
private val ALL_CITIES: List<String> by lazy {
    val regions = setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")
    ZoneId.getAvailableZoneIds()
        .filter { it.substringBefore('/') in regions && it.count { c -> c == '/' } >= 1 }
        .sortedBy { cityName(it) }
}

@Composable
fun WorldClockScreen(
    editing: Boolean,
    adding: Boolean,
    onAddingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var zones by remember { mutableStateOf(WorldClockStore.zones(ctx)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }

    if (adding) {
        CityPicker(
            onPick = { z ->
                if (z !in zones) {
                    zones = zones + z
                    WorldClockStore.save(ctx, zones)
                }
                onAddingChange(false)
            },
            onCancel = { onAddingChange(false) },
        )
        return
    }

    val is24 = DateFormat.is24HourFormat(ctx)
    val local = ZoneId.systemDefault()
    val instant = Instant.ofEpochMilli(now)
    val today = LocalDate.now(local)

    Column(modifier.fillMaxSize().background(W.Bg)) {
        Text(
            "World Clock", color = W.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp),
        )
        if (zones.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No World Clocks", color = W.Text2, fontSize = 22.sp)
            }
            return@Column
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            items(zones, key = { it }) { z ->
                val zt = ZonedDateTime.ofInstant(instant, ZoneId.of(z))
                val diffMin = (zt.offset.totalSeconds - local.rules.getOffset(instant).totalSeconds) / 60
                val day = when (zt.toLocalDate().compareTo(today).coerceIn(-1, 1)) {
                    -1 -> "Yesterday"
                    1 -> "Tomorrow"
                    else -> "Today"
                }
                val diff = when {
                    diffMin == 0 -> "+0HRS"
                    diffMin % 60 == 0 -> "%+dHRS".format(diffMin / 60)
                    else -> "%+d:%02dHRS".format(diffMin / 60, kotlin.math.abs(diffMin % 60))
                }
                HorizontalDivider(color = W.Line, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().height(92.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (editing) {
                        Box(
                            Modifier.padding(end = 14.dp).size(24.dp).clip(CircleShape).background(W.Red)
                                .clickable {
                                    zones = zones - z
                                    WorldClockStore.save(ctx, zones)
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(WIcons.Minus, "Remove", tint = W.Text, modifier = Modifier.size(16.dp)) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("$day, $diff", color = W.Text2, fontSize = 14.sp)
                        Text(cityName(z), color = W.Text, fontSize = 24.sp, maxLines = 1)
                    }
                    if (!editing) {
                        val t = if (is24) zt.format(DateTimeFormatter.ofPattern("HH:mm"))
                        else zt.format(DateTimeFormatter.ofPattern("h:mm"))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                t, color = W.Text, fontSize = 54.sp,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum", letterSpacing = (-1.5).sp),
                            )
                            if (!is24) {
                                Text(
                                    if (zt.hour < 12) "AM" else "PM", color = W.Text, fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
            item { HorizontalDivider(color = W.Line, thickness = 0.5.dp) }
        }
    }
}

@Composable
private fun CityPicker(onPick: (String) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    var q by remember { mutableStateOf("") }
    val shown = remember(q) {
        val s = q.trim().lowercase()
        if (s.isEmpty()) ALL_CITIES else ALL_CITIES.filter { cityName(it).lowercase().contains(s) || it.lowercase().contains(s) }
    }
    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Text(
            "Choose a City", color = W.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).background(W.Card2)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(WIcons.Search, null, tint = W.Text2, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (q.isEmpty()) Text("Search", color = W.Text2, fontSize = 17.sp)
                    BasicTextField(
                        q, { q = it }, singleLine = true,
                        textStyle = TextStyle(color = W.Text, fontSize = 17.sp, fontFamily = Inter),
                        cursorBrush = SolidColor(W.Dawn),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text(
                "Cancel", color = W.Dawn, fontSize = 17.sp,
                modifier = Modifier.padding(start = 12.dp).clickable(onClick = onCancel),
            )
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            items(shown, key = { it }) { z ->
                Column(Modifier.fillMaxWidth().clickable { onPick(z) }) {
                    Text(
                        "${cityName(z)}, ${z.substringBefore('/')}",
                        color = W.Text, fontSize = 17.sp, modifier = Modifier.padding(vertical = 12.dp),
                    )
                    HorizontalDivider(color = W.Line, thickness = 0.5.dp)
                }
            }
        }
    }
}
