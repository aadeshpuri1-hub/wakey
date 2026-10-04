package app.upwake.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/** Tiny JSON-in-SharedPreferences store. Plenty for a handful of alarms. */
object AlarmStore {
    private const val PREFS = "upwake_alarms"
    private const val KEY = "alarms"
    private const val STATE = "upwake_state"

    /** Special id used by the "test alarm" button in Settings (never stored). */
    const val QUICK_TEST_ID = 99_999

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms

    @Volatile
    private var loaded = false

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        val raw = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
        _alarms.value = try {
            val arr = JSONArray(raw ?: "[]")
            (0 until arr.length()).map { Alarm.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
        loaded = true
    }

    fun all(ctx: Context): List<Alarm> {
        init(ctx)
        return _alarms.value
    }

    fun get(ctx: Context, id: Int): Alarm? = all(ctx).find { it.id == id }

    /** Alarm to ring for an id, including the unsaved quick-test alarm. */
    fun getForRing(ctx: Context, id: Int): Alarm? =
        if (id == QUICK_TEST_ID) {
            Alarm(id = QUICK_TEST_ID, hour = 0, minute = 0, label = "Test alarm", snoozeMinutes = 0)
        } else {
            get(ctx, id)
        }

    fun newId(ctx: Context): Int = (all(ctx).maxOfOrNull { it.id } ?: 0) + 1

    @Synchronized
    fun upsert(ctx: Context, alarm: Alarm) {
        val list = all(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == alarm.id }
        if (i >= 0) list[i] = alarm else list.add(alarm)
        save(ctx, list)
    }

    @Synchronized
    fun delete(ctx: Context, id: Int) {
        save(ctx, all(ctx).filterNot { it.id == id })
    }

    private fun save(ctx: Context, list: List<Alarm>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, JSONArray(sorted.map { it.toJson() }).toString())
            .apply()
        _alarms.value = sorted
    }

    // ---- snooze bookkeeping ----
    private fun state(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(STATE, Context.MODE_PRIVATE)

    fun snoozesUsed(ctx: Context, id: Int): Int = state(ctx).getInt("snz_$id", 0)

    fun setSnoozesUsed(ctx: Context, id: Int, n: Int) {
        state(ctx).edit().putInt("snz_$id", n).apply()
    }
}
