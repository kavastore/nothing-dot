package tech.dotlab.dot.feature.editor.domain

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.scale
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask
import kotlin.math.roundToInt

/**
 * Imports a picture onto the matrix: downscale → grayscale → optional Floyd–Steinberg dithering.
 * Cells outside [mask] are forced off.
 */
object ImageImport {

    /**
     * @param threshold cut-off (0..255) used when [dither] is true.
     * @param contrast  multiplier around mid-grey; 1.0 = unchanged.
     * @param dither    true = 1-bit Floyd–Steinberg look; false = keep grayscale brightness.
     */
    fun fromBitmap(
        bitmap: Bitmap,
        mask: ShapeMask,
        threshold: Int = 128,
        contrast: Float = 1f,
        dither: Boolean = true,
    ): LogicalFrame {
        val size = mask.size
        val scaled = bitmap.scale(size, size)

        val gray = FloatArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val p = scaled.getPixel(x, y)
                val luma = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
                gray[y * size + x] = ((luma - 128f) * contrast + 128f).coerceIn(0f, 255f)
            }
        }

        val out = IntArray(size * size)
        if (dither) {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val i = y * size + x
                    val old = gray[i]
                    val new = if (old >= threshold) 255f else 0f
                    out[i] = new.toInt()
                    val err = old - new
                    diffuse(gray, size, x + 1, y, err * 7f / 16f)
                    diffuse(gray, size, x - 1, y + 1, err * 3f / 16f)
                    diffuse(gray, size, x, y + 1, err * 5f / 16f)
                    diffuse(gray, size, x + 1, y + 1, err * 1f / 16f)
                }
            }
        } else {
            for (i in gray.indices) out[i] = gray[i].roundToInt().coerceIn(0, 255)
        }

        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) out[y * size + x] = 0
            }
        }
        return LogicalFrame(size, out)
    }

    private fun diffuse(buffer: FloatArray, size: Int, x: Int, y: Int, error: Float) {
        if (x !in 0 until size || y !in 0 until size) return
        val i = y * size + x
        buffer[i] = (buffer[i] + error).coerceIn(0f, 255f)
    }
}
