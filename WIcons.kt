package app.upwake.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import app.upwake.data.MissionType

/** Consistent 2px line icons (24x24 grid) used everywhere instead of emoji. */
object WIcons {
    private fun line(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        )
        for (p in paths) {
            b.addPath(
                pathData = addPathNodes(p),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val AlarmClock = line(
        "alarm",
        "M12 5a8 8 0 1 0 0 16a8 8 0 1 0 0-16z",
        "M12 9v4l2.5 2",
        "M5 3L2 6", "M22 6l-3-3",
        "M6.4 18.7L4.5 20.5", "M17.6 18.7l1.9 1.8",
    )
    val Stopwatch = line(
        "stopwatch",
        "M12 6a7.5 7.5 0 1 0 0 15a7.5 7.5 0 1 0 0-15z",
        "M12 10v4", "M10 2h4", "M12 2v4", "M18.5 6.5l1.5-1.5",
    )
    val Minus = line("minus", "M5 12h14")
    val Steps = line(
        "steps",
        "M7 3c1.7 0 2.5 1.8 2.5 4S8.7 12 7 12S4.5 9.2 4.5 7S5.3 3 7 3z", "M5 15h4v2.5a2 2 0 0 1-4 0z",
        "M17 8c1.7 0 2.5 1.8 2.5 4S18.7 17 17 17S14.5 14.2 14.5 12S15.3 8 17 8z", "M15 20h4",
    )
    val Squat = line(
        "squat",
        "M12 2.5a2 2 0 1 0 0 4a2 2 0 1 0 0-4z", "M12 8.5v5l-4 3v4.5", "M12 13.5l4 3v4.5", "M6 10.5h12",
    )
    val Pushup = line(
        "pushup",
        "M19.5 7.5a2 2 0 1 0 0 4a2 2 0 1 0 0-4z", "M17 11l-12 3.5", "M15 11.6V17", "M5 14.5V17", "M2 20h20",
    )
    val BellOff = line(
        "bell_off",
        "M8.7 3.3A6 6 0 0 1 18 8c0 2.2 .3 3.9 .7 5.2",
        "M17 17H3s3-2 3-9c0-.6 .1-1.2 .3-1.7",
        "M10.3 21a1.94 1.94 0 0 0 3.4 0",
        "M2 2l20 20",
    )
    val Camera = line(
        "camera",
        "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z",
        "M12 10a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
    )
    val Barcode = line(
        "barcode",
        "M3 7V5a2 2 0 0 1 2-2h2", "M17 3h2a2 2 0 0 1 2 2v2",
        "M21 17v2a2 2 0 0 1-2 2h-2", "M7 21H5a2 2 0 0 1-2-2v-2",
        "M8 7v10", "M12 7v10", "M16 7v10",
    )
    val Music = line(
        "music",
        "M9 18V5l12-2v13",
        "M6 15a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
        "M18 13a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
    )
    val Volume = line(
        "volume",
        "M11 5L6 9H2v6h4l5 4V5z",
        "M15.5 8.5a5 5 0 0 1 0 7",
        "M19 5a10 10 0 0 1 0 14",
    )
    val Vibrate = line(
        "vibrate",
        "M2 8l2 2-2 2 2 2-2 2", "M22 8l-2 2 2 2-2 2 2 2",
        "M8 5h8a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z",
    )
    val Snooze = line("snooze", "M3 11h7l-7 9h7", "M14 4h6l-6 7h6")
    val Warning = line(
        "warning",
        "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z",
        "M12 9v4", "M12 17h.01",
    )
    val Keyboard = line(
        "keyboard",
        "M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",
        "M6 10h.01", "M10 10h.01", "M14 10h.01", "M18 10h.01", "M8 14h8",
    )
    val Sun = line(
        "sun",
        "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z",
        "M12 2v2", "M12 20v2", "M4.9 4.9l1.4 1.4", "M17.7 17.7l1.4 1.4",
        "M2 12h2", "M20 12h2", "M6.3 17.7l-1.4 1.4", "M19.1 4.9l-1.4 1.4",
    )
    val CheckCircle = line(
        "check_circle",
        "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z",
        "M8 12l3 3 5-6",
    )
    val Play = line("play", "M7 4l13 8-13 8z")
    val Chevron = line("chevron", "M9 6l6 6-6 6")
    val Moon = line("moon", "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z")
    val Lock = line(
        "lock",
        "M7 11h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
        "M8 11V7a4 4 0 0 1 8 0v4",
    )
    val Globe = line(
        "globe",
        "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z",
        "M2 12h20",
        "M12 2a15 15 0 0 1 0 20a15 15 0 0 1 0-20z",
    )
    val Apps = line(
        "apps",
        "M4 4h6v6H4z", "M14 4h6v6h-6z", "M4 14h6v6H4z", "M14 14h6v6h-6z",
    )
    val Book = line(
        "book",
        "M4 19.5A2.5 2.5 0 0 1 6.5 17H20",
        "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z",
    )
    val Search = line("search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M21 21l-4.3-4.3")
    val Eye = line(
        "eye",
        "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z",
        "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
    )
    val Check = line("check", "M5 12l5 5L20 7")
    val Plus = line("plus", "M12 5v14", "M5 12h14")
    val Close = line("close", "M6 6l12 12", "M18 6L6 18")
    val Trash = line(
        "trash",
        "M3 6h18", "M8 6V4h8v2",
        "M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6",
    )
    val Shield = line("shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z", "M9 12l2 2 4-4")
    val Repeat = line("repeat", "M17 1l4 4-4 4", "M3 11V9a4 4 0 0 1 4-4h14", "M7 23l-4-4 4-4", "M21 13v2a4 4 0 0 1-4 4H3")
    val Gear = line(
        "gear",
        "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
    )
    val Tag = line(
        "tag",
        "M12 2H2v10l9.3 9.3a1 1 0 0 0 1.4 0l8.6-8.6a1 1 0 0 0 0-1.4L12 2z",
        "M7 7h.01",
    )
}

fun MissionType.icon(): ImageVector = when (this) {
    MissionType.NONE -> WIcons.BellOff
    MissionType.PHOTO -> WIcons.Camera
    MissionType.BARCODE -> WIcons.Barcode
    MissionType.STEPS -> WIcons.Steps
    MissionType.SQUATS -> WIcons.Squat
    MissionType.PUSHUPS -> WIcons.Pushup
}
