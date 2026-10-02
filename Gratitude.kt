package com.aditya.wakey.gratitude

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.aditya.wakey.ui.theme.WakeyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GratitudeEntry(val time: Long, val first: String, val second: String)

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
                GratitudeEntry(o.getLong("t"), o.optString("a"), o.optString("b"))
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
    fun add(ctx: Context, a: String, b: String) {
        val list = listOf(GratitudeEntry(System.currentTimeMillis(), a.trim(), b.trim())) + all(ctx)
        val arr = JSONArray(list.map { JSONObject().put("t", it.time).put("a", it.first).put("b", it.second) })
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

/**
 * After the alarm is dismissed: "Good morning" + two gratitude lines. Mandatory:
 * Back does nothing and leaving brings it back until both lines are written.
 */
class GratitudeActivity : ComponentActivity() {

    companion object {
        /** Shows the screen if today's entry isn't written yet. */
        fun launchIfNeeded(ctx: Context) {
            if (GratitudeStore.writtenToday(ctx)) return
            try {
                ctx.startActivity(
                    Intent(ctx, GratitudeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) {
            }
        }
    }

    private var done = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        if (GratitudeStore.writtenToday(this)) {
            done = true
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            WakeyTheme {
                GratitudeScreen(onSave = { a, b ->
                    GratitudeStore.add(this, a, b)
                    done = true
                    finish()
                })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()
        // Left without writing? Come back.
        if (!done && !isChangingConfigurations) {
            handler.postDelayed({
                if (!done) {
                    try {
                        startActivity(
                            Intent(this, GratitudeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (_: Exception) {
                    }
                }
            }, 1500)
        }
    }

    override fun onDestroy() {
        if (done) handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

private fun ok(s: String) = s.trim().length >= 3

@Composable
private fun GratitudeScreen(onSave: (String, String) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    Column(
        Modifier.fillMaxSize().background(W.Bg).systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(W.Card),
            contentAlignment = Alignment.Center,
        ) { Icon(WIcons.Sun, null, tint = W.Text, modifier = Modifier.size(34.dp)) }
        Text(
            if (hour < 12) "Good morning." else "Good day.",
            color = W.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            color = W.Text2, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "Before you start, write two things you're grateful for.",
            color = W.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp,
            modifier = Modifier.padding(top = 36.dp, bottom = 18.dp),
        )
        GratitudeField("1", "I'm grateful for…", a) { a = it }
        Spacer(Modifier.height(12.dp))
        GratitudeField("2", "I'm thankful that…", b) { b = it }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            "Start my day",
            onClick = { if (ok(a) && ok(b)) onSave(a, b) },
            enabled = ok(a) && ok(b),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GratitudeField(num: String, hint: String, value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(W.Card)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(140)) },
            textStyle = TextStyle(color = W.Text, fontSize = 17.sp, fontFamily = Inter, lineHeight = 24.sp),
            cursorBrush = SolidColor(W.Accent),
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("$num.  $hint", color = W.Text3, fontSize = 17.sp, lineHeight = 24.sp)
                    inner()
                }
            },
        )
    }
}
