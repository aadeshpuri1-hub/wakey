package com.aditya.wakey.gratitude

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.ui.PrimaryButton
import com.aditya.wakey.ui.theme.Inter
import com.aditya.wakey.ui.theme.W
import com.aditya.wakey.ui.theme.WIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GratitudeEntry(val time: Long, val topic: String, val text: String)

/** Minimum words, about 2–3 lines on a phone. */
const val GRATITUDE_MIN_WORDS = 25

/** One topic per day so it never turns into "I'm grateful for my phone". */
val GRATITUDE_TOPICS = listOf(
    "A person who helped you recently, and what they did",
    "Something about your body or health you usually take for granted",
    "A small moment from yesterday that made you smile",
    "A skill you have today that you didn't have a year ago",
    "Someone in your family and one thing you love about them",
    "A friend who would pick up the phone at 3 AM",
    "A meal you really enjoyed and who you had it with",
    "Something difficult that taught you a lesson",
    "A teacher or mentor who changed how you think",
    "Something in your room or home that makes life easier",
    "A place you love going to and why",
    "A piece of music, book or film that moved you",
    "An opportunity you have that many people don't",
    "A habit you've built that you're proud of",
    "Something about today you're looking forward to",
    "A stranger's kindness you still remember",
    "A memory from childhood that still makes you happy",
    "Something in nature you noticed this week",
    "A mistake that turned out better than expected",
    "Someone who believes in you, and how you know it",
    "Your sleep, your bed, and the fact that you woke up today",
    "A conversation that stayed with you",
    "Something you own that you once wished for",
    "A problem you solved recently",
    "A part of your routine that keeps you grounded",
    "Someone who makes you laugh",
    "Freedom you have that someone in history didn't",
    "A goal you're working towards and why it matters",
    "A time someone forgave you",
    "Something simple: water, light, food, a roof. Pick one and go deep",
)

fun topicFor(timeMs: Long = System.currentTimeMillis()): String {
    val c = Calendar.getInstance().apply { timeInMillis = timeMs }
    val key = c.get(Calendar.YEAR) * 400 + c.get(Calendar.DAY_OF_YEAR)
    return GRATITUDE_TOPICS[key % GRATITUDE_TOPICS.size]
}

fun wordCount(s: String) = s.trim().split(Regex("\\s+")).count { w -> w.any { it.isLetterOrDigit() } }

object GratitudeStore {
    private const val PREFS = "wakey_gratitude"
    private const val KEY = "entries"

    private val _entries = MutableStateFlow<List<GratitudeEntry>>(emptyList())
    val entries: StateFlow<List<GratitudeEntry>> = _entries

    @Volatile
    private var loaded = false

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        val raw = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")
        _entries.value = try {
            val a = JSONArray(raw)
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                if (o.has("text")) {
                    GratitudeEntry(o.getLong("t"), o.optString("topic"), o.optString("text"))
                } else {
                    // older two-line entries
                    GratitudeEntry(o.getLong("t"), "", listOf(o.optString("a"), o.optString("b")).filter { s -> s.isNotBlank() }.joinToString("\n"))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
        loaded = true
    }

    fun all(ctx: Context): List<GratitudeEntry> {
        init(ctx)
        return _entries.value
    }

    @Synchronized
    fun add(ctx: Context, topic: String, text: String) {
        val list = listOf(GratitudeEntry(System.currentTimeMillis(), topic, text.trim())) + all(ctx)
        val arr = JSONArray(list.map { JSONObject().put("t", it.time).put("topic", it.topic).put("text", it.text) })
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString()).apply()
        _entries.value = list
    }

    fun writtenToday(ctx: Context): Boolean {
        val latest = all(ctx).firstOrNull() ?: return false
        val a = Calendar.getInstance().apply { timeInMillis = latest.time }
        val b = Calendar.getInstance()
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}

/** Morning check-in shown inside the alarm flow after the alarm is silenced. */
@Composable
fun GratitudeScreen(onSave: (topic: String, text: String) -> Unit) {
    val topic = remember { topicFor() }
    var text by rememberSaveable { mutableStateOf("") }
    val words = wordCount(text)
    val ready = words >= GRATITUDE_MIN_WORDS
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    Column(
        Modifier.fillMaxSize().background(W.Bg).systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(W.Card),
            contentAlignment = Alignment.Center,
        ) { Icon(WIcons.Sun, null, tint = W.Text, modifier = Modifier.size(30.dp)) }
        Text(
            if (hour < 12) "Good morning." else "Good day.",
            color = W.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            "TODAY'S GRATITUDE",
            color = W.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            topic, color = W.Text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        Box(
            Modifier.fillMaxWidth().heightIn(min = 160.dp).clip(RoundedCornerShape(20.dp))
                .background(W.Card).padding(18.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(1500) },
                textStyle = TextStyle(color = W.Text, fontSize = 17.sp, fontFamily = Inter, lineHeight = 25.sp),
                cursorBrush = SolidColor(W.Accent),
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                "Write at least 2–3 lines. Be specific: who, what, and why it matters to you.",
                                color = W.Text3, fontSize = 17.sp, lineHeight = 25.sp,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp)) {
            Text(
                if (ready) "Nice." else "${GRATITUDE_MIN_WORDS - words} more words",
                color = if (ready) W.Text else W.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text("$words / $GRATITUDE_MIN_WORDS", color = W.Text3, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            "Start my day",
            onClick = { if (ready) onSave(topic, text) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
    }
}
