package app.upwake.ring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import app.upwake.ui.theme.W
import java.io.File

/** The ringing screen's background: a built-in gradient or the user's own photo. */
object RingBg {
    data class Preset(val key: String, val title: String, val brush: Brush)

    val presets = listOf(
        Preset("sunrise", "Sunrise", Brush.verticalGradient(0f to Color.Black, 0.5f to Color.Black, 1f to W.DawnGlow)),
        Preset("night", "Night", Brush.verticalGradient(listOf(Color(0xFF05070F), Color(0xFF111A33), Color(0xFF2B2150)))),
        Preset("ocean", "Ocean", Brush.verticalGradient(listOf(Color(0xFF00111F), Color(0xFF003A4F), Color(0xFF0A6E77)))),
        Preset("forest", "Forest", Brush.verticalGradient(listOf(Color(0xFF040D08), Color(0xFF0E2A1A), Color(0xFF1F5137)))),
        Preset("black", "Black", Brush.verticalGradient(listOf(Color.Black, Color.Black))),
    )
    const val PHOTO = "photo"

    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("upwake_prefs", Context.MODE_PRIVATE)
    fun key(ctx: Context): String = p(ctx).getString("ring_bg", "sunrise") ?: "sunrise"
    fun setKey(ctx: Context, k: String) = p(ctx).edit().putString("ring_bg", k).apply()
    fun photoFile(ctx: Context) = File(ctx.filesDir, "ring_bg.jpg")

    fun title(ctx: Context): String =
        if (key(ctx) == PHOTO) "Your photo" else presets.firstOrNull { it.key == key(ctx) }?.title ?: "Sunrise"

    /** Copies a picked image into private storage, scaled down to screen size. */
    fun savePhoto(ctx: Context, uri: Uri): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1440 || bounds.outHeight / sample > 2560) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (bmp == null) false else {
            photoFile(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            setKey(ctx, PHOTO)
            true
        }
    } catch (e: Exception) {
        false
    }

    fun loadPhoto(ctx: Context): ImageBitmap? = try {
        val f = photoFile(ctx)
        if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
    } catch (e: Exception) {
        null
    }
}

/** Applies the chosen background; photos get a dark scrim so the clock stays readable. */
fun Modifier.ringBackground(key: String, photo: ImageBitmap?): Modifier =
    if (key == RingBg.PHOTO && photo != null) {
        this.background(Color.Black)
            .paint(BitmapPainter(photo), contentScale = ContentScale.Crop)
            .background(Color(0x8C000000))
    } else {
        this.background(RingBg.presets.firstOrNull { it.key == key }?.brush ?: RingBg.presets.first().brush)
    }
