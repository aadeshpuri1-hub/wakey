package com.aditya.wakey.focus

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Sleep focus settings. Blocking runs from (bedtime - 30 min) to (wake-up + 30 min), every day.
 * Adult websites are blocked all the time while [adultBlock] is on.
 */
data class FocusConfig(
    val enabled: Boolean = false,
    val bedH: Int = 23,
    val bedM: Int = 0,
    val wakeH: Int = 7,
    val wakeM: Int = 0,
    val apps: Set<String> = emptySet(),
    val adultBlock: Boolean = false,
) {
    val startMin: Int get() = Math.floorMod(bedH * 60 + bedM - BUFFER_MIN, DAY)
    val endMin: Int get() = Math.floorMod(wakeH * 60 + wakeM + BUFFER_MIN, DAY)

    fun inWindow(minuteOfDay: Int): Boolean = when {
        startMin == endMin -> false
        startMin < endMin -> minuteOfDay in startMin until endMin
        else -> minuteOfDay >= startMin || minuteOfDay < endMin
    }

    /** True while social apps must be blocked right now. */
    fun isActive(nowMs: Long = System.currentTimeMillis()): Boolean =
        enabled && apps.isNotEmpty() && inWindow(minuteOfDay(nowMs))

    /** Epoch ms of the next moment the block starts or ends. */
    fun nextChange(nowMs: Long = System.currentTimeMillis()): Long =
        minOf(nextOccurrence(startMin, nowMs), nextOccurrence(endMin, nowMs))

    fun toJson(): JSONObject = JSONObject().apply {
        put("on", enabled); put("bh", bedH); put("bm", bedM); put("wh", wakeH); put("wm", wakeM)
        put("apps", JSONArray(apps.toList())); put("adult", adultBlock)
    }

    companion object {
        const val BUFFER_MIN = 30
        const val DAY = 24 * 60

        fun fromJson(o: JSONObject): FocusConfig {
            val arr = o.optJSONArray("apps") ?: JSONArray()
            return FocusConfig(
                enabled = o.optBoolean("on", false),
                bedH = o.optInt("bh", 23), bedM = o.optInt("bm", 0),
                wakeH = o.optInt("wh", 7), wakeM = o.optInt("wm", 0),
                apps = (0 until arr.length()).map { arr.getString(it) }.toSet(),
                adultBlock = o.optBoolean("adult", false),
            )
        }
    }
}

fun minuteOfDay(nowMs: Long = System.currentTimeMillis()): Int {
    val c = Calendar.getInstance().apply { timeInMillis = nowMs }
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

fun nextOccurrence(minOfDay: Int, nowMs: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, minOfDay / 60)
        set(Calendar.MINUTE, minOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (c.timeInMillis <= nowMs) c.add(Calendar.DAY_OF_YEAR, 1)
    return c.timeInMillis
}

/** "10:30 PM" */
fun formatMin(minOfDay: Int): String {
    val h = minOfDay / 60
    val m = minOfDay % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
}

object FocusStore {
    private const val PREFS = "wakey_focus"
    private const val KEY = "cfg"

    private val _cfg = MutableStateFlow(FocusConfig())
    val cfg: StateFlow<FocusConfig> = _cfg

    @Volatile
    private var loaded = false

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        val raw = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        _cfg.value = try {
            if (raw == null) FocusConfig() else FocusConfig.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            FocusConfig()
        }
        loaded = true
    }

    fun get(ctx: Context): FocusConfig {
        init(ctx)
        return _cfg.value
    }

    @Synchronized
    fun save(ctx: Context, c: FocusConfig) {
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, c.toJson().toString()).apply()
        _cfg.value = c
    }
}

/** Popular social apps: suggested in the picker, and their websites get blocked in browsers too. */
object SocialCatalog {
    class Known(val pkg: String, val name: String, val domains: List<String>)

    private val fb = listOf("facebook.com", "fbcdn.net", "fb.com", "facebook.net", "fb.watch")
    private val tiktok = listOf("tiktok.com", "tiktokcdn.com", "tiktokv.com", "tiktokcdn-us.com", "byteoversea.com", "ibytedtos.com", "musical.ly")

    val list = listOf(
        Known("com.instagram.android", "Instagram", listOf("instagram.com", "cdninstagram.com", "ig.me")),
        Known("com.google.android.youtube", "YouTube", listOf("youtube.com", "youtu.be", "ytimg.com", "googlevideo.com", "youtube-nocookie.com")),
        Known("com.snapchat.android", "Snapchat", listOf("snapchat.com", "sc-cdn.net", "snap-dev.net", "snapkit.com")),
        Known("com.facebook.katana", "Facebook", fb),
        Known("com.facebook.lite", "Facebook Lite", fb),
        Known("com.zhiliaoapp.musically", "TikTok", tiktok),
        Known("com.ss.android.ugc.trill", "TikTok", tiktok),
        Known("com.twitter.android", "X", listOf("x.com", "twitter.com", "twimg.com", "t.co")),
        Known("com.instagram.barcelona", "Threads", listOf("threads.net", "threads.com")),
        Known("com.reddit.frontpage", "Reddit", listOf("reddit.com", "redd.it", "redditmedia.com", "redditstatic.com")),
        Known("com.pinterest", "Pinterest", listOf("pinterest.com", "pinimg.com")),
        Known("in.mohalla.sharechat", "ShareChat", listOf("sharechat.com")),
        Known("in.mohalla.video", "Moj", listOf("mojapp.in")),
        Known("com.eterno.shortvideos", "Josh", listOf("myjosh.in")),
        Known("com.discord", "Discord", listOf("discord.com", "discord.gg", "discordapp.com", "discordapp.net")),
        Known("com.linkedin.android", "LinkedIn", listOf("linkedin.com", "licdn.com")),
        Known("com.tumblr", "Tumblr", listOf("tumblr.com")),
        Known("tv.twitch.android.app", "Twitch", listOf("twitch.tv", "ttvnw.net")),
    )

    val suggested: Set<String> = list.map { it.pkg }.toSet()

    fun domainsFor(pkgs: Set<String>): Set<String> =
        list.filter { it.pkg in pkgs }.flatMap { it.domains }.toSet()
}
