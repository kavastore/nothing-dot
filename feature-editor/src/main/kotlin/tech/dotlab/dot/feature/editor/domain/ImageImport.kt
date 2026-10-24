package tech.dotlab.dot.feature.editor.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask
import kotlin.math.min
import kotlin.math.roundToInt

/** How a composed image is converted to matrix brightness values. */
enum class ImageImportMode {
    /** Preserve all 256 brightness levels. */
    GRAYSCALE,

    /** Strict 1-bit conversion: every cell is exactly off or on. */
    BINARY,

    /** Strict 1-bit conversion with Floyd-Steinberg error diffusion. */
    DITHER,
}

/** One movable image in a collage. Offsets are expressed in matrix cells. */
data class ImageLayer(
    val bitmap: Bitmap,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

/**
 * Composes one or more pictures and converts the result to a logical matrix frame.
 *
 * Each source is fit inside the square first. [ImageLayer.scale] then zooms around the centre and
 * the offsets may move it completely outside the canvas. This avoids destructive pre-cropping and
 * lets the user position awkward web icons precisely.
 */
object ImageImport {

    fun compose(
        layers: List<ImageLayer>,
        mask: ShapeMask,
        mode: ImageImportMode = ImageImportMode.DITHER,
        threshold: Int = 128,
        contrast: Float = 1f,
        invert: Boolean = false,
    ): LogicalFrame {
        val size = mask.size
        if (layers.isEmpty()) return LogicalFrame.empty(size)

        // Render above matrix resolution so transforms remain smooth before the final sample.
        val renderScale = 16
        val side = size * renderScale
        val composed = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composed)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        layers.forEach { layer ->
            val source = layer.bitmap
            if (source.width <= 0 || source.height <= 0) return@forEach
            val fit = min(side.toFloat() / source.width, side.toFloat() / source.height)
            val factor = fit * layer.scale.coerceIn(MIN_SCALE, MAX_SCALE)
            val width = source.width * factor
            val height = source.height * factor
            val left = (side - width) / 2f + layer.offsetX * renderScale
            val top = (side - height) / 2f + layer.offsetY * renderScale
            canvas.drawBitmap(source, null, RectF(left, top, left + width, top + height), paint)
        }

        val sampled = Bitmap.createScaledBitmap(composed, size, size, true)
        val gray = FloatArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = sampled.getPixel(x, y)
                val alpha = Color.alpha(pixel) / 255f
                val sourceLuma = 0.299f * Color.red(pixel) +
                    0.587f * Color.green(pixel) +
                    0.114f * Color.blue(pixel)
                // Transparent pixels always stay off. Inversion therefore also handles black
                // artwork on a transparent background without lighting the empty canvas.
                var luma = (if (invert) 255f - sourceLuma else sourceLuma) * alpha
                luma = if (alpha == 0f) 0f else
                    ((luma - 128f) * contrast + 128f).coerceIn(0f, 255f)
                gray[y * size + x] = luma
            }
        }

        val out = when (mode) {
            ImageImportMode.GRAYSCALE -> IntArray(gray.size) { gray[it].roundToInt() }
            ImageImportMode.BINARY -> IntArray(gray.size) {
                if (gray[it] >= threshold.coerceIn(0, 255)) 255 else 0
            }
            ImageImportMode.DITHER -> dither(gray, size, threshold.coerceIn(0, 255))
        }

        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) out[y * size + x] = 0
            }
        }
        return LogicalFrame(size, out)
    }

    /** Compatibility helper for callers that only need one centred image. */
    fun fromBitmap(
        bitmap: Bitmap,
        mask: ShapeMask,
        threshold: Int = 128,
        contrast: Float = 1f,
        dither: Boolean = true,
    ): LogicalFrame = compose(
        layers = listOf(ImageLayer(bitmap)),
        mask = mask,
        mode = if (dither) ImageImportMode.DITHER else ImageImportMode.GRAYSCALE,
        threshold = threshold,
        contrast = contrast,
    )

    private fun dither(source: FloatArray, size: Int, threshold: Int): IntArray {
        val work = source.copyOf()
        val out = IntArray(work.size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val index = y * size + x
                val old = work[index]
                val next = if (old >= threshold) 255f else 0f
                out[index] = next.toInt()
                val error = old - next
                diffuse(work, size, x + 1, y, error * 7f / 16f)
                diffuse(work, size, x - 1, y + 1, error * 3f / 16f)
                diffuse(work, size, x, y + 1, error * 5f / 16f)
                diffuse(work, size, x + 1, y + 1, error / 16f)
            }
        }
        return out
    }

    private fun diffuse(buffer: FloatArray, size: Int, x: Int, y: Int, error: Float) {
        if (x !in 0 until size || y !in 0 until size) return
        val index = y * size + x
        buffer[index] = (buffer[index] + error).coerceIn(0f, 255f)
    }

    private const val MIN_SCALE = 0.1f
    private const val MAX_SCALE = 8f
}
