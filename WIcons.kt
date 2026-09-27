package com.aditya.wakey.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.aditya.wakey.data.MissionType

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
}
