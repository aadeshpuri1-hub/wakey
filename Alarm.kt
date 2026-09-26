package com.aditya.wakey.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class MissionType(val title: String, val emoji: String) {
    NONE("Off", "🔔"),
    PHOTO("Photo", "📸"),
    BARCODE("Barcode", "🏷️"),
}

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    /** Calendar.SUNDAY (1) .. Calendar.SATURDAY (7). Empty = rings once. */
    val days: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val label: String = "",
    val mission: MissionType = MissionType.NONE,
    /** PHOTO: absolute path of reference jpg. BARCODE: raw barcode value. */
    val missionData: String? = null,
    /** 0 easy, 1 normal, 2 hard */
    val photoSensitivity: Int = 1,
    val soundUri: String? = null,
    val volume: Float = 1f,
    val vibrate: Boolean = true,
    /** 0 = snooze off */
    val snoozeMinutes: Int = 5,
    val maxSnoozes: Int = 3,
) {
    val hasMission: Boolean get() = mission != MissionType.NONE && !missionData.isNullOrBlank()

    fun timeText(): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        return "%d:%02d".format(h, minute)
    }

    fun amPm(): String = if (hour < 12) "AM" else "PM"

    fun daysText(): String = when {
        days.isEmpty() -> "Once"
        days.size == 7 -> "Every day"
        days == WEEKDAYS -> "Weekdays"
        days == WEEKENDS -> "Weekends"
        else -> ORDER.filter { it in days }.joinToString(" ") { SHORT_NAMES[it - 1] }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("h", hour)
        put("m", minute)
        put("days", JSONArray(days.sorted()))
        put("on", enabled)
        put("label", label)
        put("mission", mission.name)
        put("mdata", missionData ?: JSONObject.NULL)
        put("sens", photoSensitivity)
        put("sound", soundUri ?: JSONObject.NULL)
        put("vol", volume.toDouble())
        put("vib", vibrate)
        put("snooze", snoozeMinutes)
        put("maxSnooze", maxSnoozes)
    }

    companion object {
        val ORDER = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY,
        )
        val SHORT_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val LETTERS = listOf("S", "M", "T", "W", "T", "F", "S")
        val WEEKDAYS = setOf(2, 3, 4, 5, 6)
        val WEEKENDS = setOf(1, 7)

        fun fromJson(o: JSONObject): Alarm {
            val arr = o.optJSONArray("days") ?: JSONArray()
            val days = (0 until arr.length()).map { arr.getInt(it) }.toSet()
            return Alarm(
                id = o.getInt("id"),
                hour = o.getInt("h"),
                minute = o.getInt("m"),
                days = days,
                enabled = o.optBoolean("on", true),
                label = o.optString("label", ""),
                mission = runCatching { MissionType.valueOf(o.optString("mission")) }
                    .getOrDefault(MissionType.NONE),
                missionData = if (o.isNull("mdata")) null else o.optString("mdata"),
                photoSensitivity = o.optInt("sens", 1),
                soundUri = if (o.isNull("sound")) null else o.optString("sound"),
                volume = o.optDouble("vol", 1.0).toFloat(),
                vibrate = o.optBoolean("vib", true),
                snoozeMinutes = o.optInt("snooze", 5),
                maxSnoozes = o.optInt("maxSnooze", 3),
            )
        }
    }
}
