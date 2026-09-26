package com.aditya.wakey.mission

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Offline "is this the same place?" check.
 * Combines structure (normalized cross-correlation of a blurred 32x32 grayscale,
 * tolerant to brightness changes and small shifts) with a color histogram.
 */
object PhotoMatcher {
    private const val G = 32
    private const val S = 128
    private const val BINS = 19

    class Print(val gray: FloatArray, val hist: FloatArray)

    fun threshold(sensitivity: Int): Float = when (sensitivity) {
        0 -> 0.50f
        2 -> 0.70f
        else -> 0.60f
    }

    fun sensitivityName(s: Int) = when (s) {
        0 -> "Easy"
        2 -> "Hard"
        else -> "Normal"
    }

    fun print(src: Bitmap): Print {
        val side = min(src.width, src.height)
        val sq = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(sq, S, S, true)
        val px = IntArray(S * S)
        small.getPixels(px, 0, S, 0, 0, S, S)

        val gray = FloatArray(G * G)
        val hist = FloatArray(BINS)
        val hsv = FloatArray(3)
        val block = S / G
        val norm = (block * block).toFloat()
        for (y in 0 until S) for (x in 0 until S) {
            val c = px[y * S + x]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[(y / block) * G + (x / block)] += (0.299f * r + 0.587f * g + 0.114f * b) / norm
            Color.RGBToHSV(r, g, b, hsv)
            val chroma = hsv[1] * hsv[2]
            hist[(hsv[0] / 20f).toInt().coerceIn(0, 17)] += chroma
            hist[18] += 1f - chroma
        }
        val sum = hist.sum()
        if (sum > 0f) for (i in hist.indices) hist[i] /= sum
        return Print(gray, hist)
    }

    /** 0..1, higher = more similar. */
    fun score(a: Print, b: Print): Float {
        var best = -1f
        for (dy in -3..3) for (dx in -3..3) best = max(best, ncc(a.gray, b.gray, dx, dy))
        var inter = 0f
        for (i in 0 until BINS) inter += min(a.hist[i], b.hist[i])
        return (0.7f * max(0f, best) + 0.3f * inter).coerceIn(0f, 1f)
    }

    private fun ncc(a: FloatArray, b: FloatArray, dx: Int, dy: Int): Float {
        val y0 = max(0, -dy)
        val y1 = min(G, G - dy)
        val x0 = max(0, -dx)
        val x1 = min(G, G - dx)
        var n = 0
        var sa = 0.0
        var sb = 0.0
        for (y in y0 until y1) for (x in x0 until x1) {
            sa += a[y * G + x]; sb += b[(y + dy) * G + x + dx]; n++
        }
        if (n == 0) return 0f
        val ma = sa / n
        val mb = sb / n
        var cov = 0.0
        var va = 0.0
        var vb = 0.0
        for (y in y0 until y1) for (x in x0 until x1) {
            val p = a[y * G + x] - ma
            val q = b[(y + dy) * G + x + dx] - mb
            cov += p * q; va += p * p; vb += q * q
        }
        if (va < 1e-6 || vb < 1e-6) return 0f
        return (cov / sqrt(va * vb)).toFloat()
    }

    fun load(path: String): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (max(opts.outWidth, opts.outHeight) / (sample * 2) >= 480) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun saveReference(ctx: Context, alarmId: Int, bmp: Bitmap): String {
        val scale = 640f / max(bmp.width, bmp.height)
        val out = if (scale < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        } else bmp
        val f = File(ctx.filesDir, "mission_${alarmId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return f.absolutePath
    }
}
