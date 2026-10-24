package tech.dotlab.dot.feature.editor.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask
import java.io.OutputStream

/** Renders a frame to a dot-style grayscale PNG; brightness maps to white dot alpha on black. */
object PngExport {

    /**
     * Exports a square, nearest-neighbour icon for Nothing OS Essential Notifications.
     * A 25×25 frame becomes 400×400 by default: large enough for the system picker, while every
     * matrix cell remains a clean solid block with no grid lines or resampling artefacts.
     */
    fun renderNotificationIcon(
        frame: LogicalFrame,
        mask: ShapeMask,
        pixelScale: Int = 16,
    ): Bitmap {
        require(pixelScale > 0)
        val bitmap = Bitmap.createBitmap(
            frame.size * pixelScale,
            frame.size * pixelScale,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint().apply { isAntiAlias = false }
        for (y in 0 until frame.size) {
            for (x in 0 until frame.size) {
                if (!mask.isOn(x, y)) continue
                val value = frame.brightnessAt(x, y).coerceIn(0, 255)
                paint.color = Color.rgb(value, value, value)
                canvas.drawRect(
                    (x * pixelScale).toFloat(),
                    (y * pixelScale).toFloat(),
                    ((x + 1) * pixelScale).toFloat(),
                    ((y + 1) * pixelScale).toFloat(),
                    paint,
                )
            }
        }
        return bitmap
    }

    fun render(
        frame: LogicalFrame,
        mask: ShapeMask,
        cellPx: Int = 48,
        dotRatio: Float = 0.84f,
    ): Bitmap {
        val side = frame.size * cellPx
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = cellPx * dotRatio / 2f
        for (y in 0 until frame.size) {
            for (x in 0 until frame.size) {
                if (!mask.isOn(x, y)) continue
                val v = frame.brightnessAt(x, y)
                paint.color = Color.argb(255, v, v, v)
                val cx = x * cellPx + cellPx / 2f
                val cy = y * cellPx + cellPx / 2f
                canvas.drawCircle(cx, cy, radius, paint)
            }
        }
        return bitmap
    }

    fun writePng(bitmap: Bitmap, out: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}
