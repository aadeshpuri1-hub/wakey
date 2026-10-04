package app.upwake.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.ui.theme.Inter
import app.upwake.ui.theme.W

/** Stopwatch state, kept in prefs so it keeps running while you switch tabs or close the app. */
private object StopwatchStore {
    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("upwake_stopwatch", Context.MODE_PRIVATE)

    data class State(val running: Boolean, val startedAt: Long, val banked: Long, val laps: List<Long>)

    fun load(ctx: Context): State {
        val sp = p(ctx)
        var s = State(
            running = sp.getBoolean("running", false),
            startedAt = sp.getLong("startedAt", 0L),
            banked = sp.getLong("banked", 0L),
            laps = sp.getString("laps", "")!!.split(",").mapNotNull { it.toLongOrNull() },
        )
        // phone restarted while running: elapsedRealtime went back to zero, so start over
        if (s.running && s.startedAt > SystemClock.elapsedRealtime()) s = State(false, 0L, 0L, emptyList())
        return s
    }

    fun save(ctx: Context, s: State) {
        p(ctx).edit()
            .putBoolean("running", s.running)
            .putLong("startedAt", s.startedAt)
            .putLong("banked", s.banked)
            .putString("laps", s.laps.joinToString(","))
            .apply()
    }
}

/** 01:23.45 or 1:02:03.45 */
private fun fmt(ms: Long): String {
    val cs = (ms / 10) % 100
    val totalS = ms / 1000
    val s = totalS % 60
    val m = (totalS / 60) % 60
    val h = totalS / 3600
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}

private val DigitStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontFeatureSettings = "tnum",
    letterSpacing = (-2).sp,
)

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var st by remember { mutableStateOf(StopwatchStore.load(ctx)) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(st.running) {
        while (st.running) {
            withFrameMillis { }
            now = SystemClock.elapsedRealtime()
        }
    }

    fun set(s: State) {
        st = s
        StopwatchStore.save(ctx, s)
        now = SystemClock.elapsedRealtime()
    }

    val elapsed = st.banked + if (st.running) (now - st.startedAt).coerceAtLeast(0L) else 0L
    val lapTotal = st.laps.sum()
    val currentLap = elapsed - lapTotal
    val started = elapsed > 0

    Column(modifier.fillMaxSize().background(W.Bg)) {
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Text(
                fmt(elapsed), style = DigitStyle, color = W.Text,
                fontSize = if (elapsed >= 3_600_000) 60.sp else 78.sp,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // left: Lap while running, Reset while paused
            if (st.running || !started) {
                RoundButton(
                    "Lap", fg = if (started) W.Text else W.Text3, bg = W.Card2, enabled = st.running,
                ) {
                    set(st.copy(laps = st.laps + currentLap))
                }
            } else {
                RoundButton("Reset", fg = W.Text, bg = W.Card2) {
                    set(StopwatchStore.State(false, 0L, 0L, emptyList()))
                }
            }
            if (st.running) {
                RoundButton("Stop", fg = W.Red, bg = W.RedDim) {
                    val t = SystemClock.elapsedRealtime()
                    set(st.copy(running = false, banked = st.banked + (t - st.startedAt)))
                }
            } else {
                RoundButton("Start", fg = W.Green, bg = W.GreenDim) {
                    set(st.copy(running = true, startedAt = SystemClock.elapsedRealtime()))
                }
            }
        }

        // laps, newest first (current lap on top, like Apple's Clock)
        val rows = buildList {
            if (started) add(st.laps.size + 1 to currentLap)
            st.laps.forEachIndexed { i, v -> add(i + 1 to v) }
        }.sortedByDescending { it.first }
        val best = if (st.laps.size >= 2) st.laps.min() else null
        val worst = if (st.laps.size >= 2) st.laps.max() else null

        LazyColumn(Modifier.fillMaxWidth().padding(top = 24.dp, start = 20.dp, end = 20.dp)) {
            itemsIndexed(rows) { idx, (n, v) ->
                val isCurrent = started && idx == 0
                val c = when {
                    isCurrent -> W.Text
                    v == best -> W.Green
                    v == worst -> W.Red
                    else -> W.Text
                }
                HorizontalDivider(color = W.Line, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Lap $n", color = c, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(fmt(v), color = c, fontSize = 16.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                }
            }
            if (rows.isNotEmpty()) item { HorizontalDivider(color = W.Line, thickness = 0.5.dp) }
        }
    }
}

private typealias State = StopwatchStore.State

/** Apple-style round button with a thin inner ring. */
@Composable
fun RoundButton(label: String, fg: Color, bg: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(84.dp).clip(CircleShape).background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(76.dp).clip(CircleShape).border(2.dp, W.Bg, CircleShape))
        Text(label, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
