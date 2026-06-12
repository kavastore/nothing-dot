package tech.dotlab.dot.matrix

import android.graphics.Bitmap
import android.graphics.Color
import tech.dotlab.dot.core.model.LogicalFrame

/**
 * Converts a monochrome [LogicalFrame] into a 1:1 grayscale [Bitmap] the SDK can consume.
 *
 * The matrix maps image luminance to per-LED brightness, so each cell's 0..255 value becomes a
 * grayscale pixel. The SDK requires a square bitmap matching the matrix length.
 */
object FrameBitmap {

    fun from(frame: LogicalFrame): Bitmap {
        val size = frame.size
        val pixels = IntArray(size * size)
        for (i in pixels.indices) {
            val v = frame.brightness[i].coerceIn(0, 255)
            pixels[i] = Color.argb(255, v, v, v)
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
