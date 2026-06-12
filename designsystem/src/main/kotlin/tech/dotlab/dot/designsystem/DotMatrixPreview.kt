package tech.dotlab.dot.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask
import kotlin.math.min

/**
 * The single reusable matrix renderer for on-screen previews.
 *
 * Draws real dots on a [Canvas]: cells absent from [mask] are dimmed, present cells are lit by the
 * [frame] brightness (0..255). Reused across gallery, editor, game and toy status per the spec.
 */
@Composable
fun DotMatrixPreview(
    mask: ShapeMask,
    modifier: Modifier = Modifier,
    frame: LogicalFrame? = null,
    litColor: Color = DotColors.White,
    offColor: Color = DotColors.DimGrey,
    dotSpacingRatio: Float = 0.16f,
) {
    require(frame == null || frame.size == mask.size) {
        "frame size (${frame?.size}) must match mask size (${mask.size})"
    }
    Canvas(modifier = modifier) {
        val size = mask.size
        val side = min(this.size.width, this.size.height)
        val cell = side / size
        val radius = cell * (1f - dotSpacingRatio) / 2f
        val originX = (this.size.width - side) / 2f
        val originY = (this.size.height - side) / 2f

        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                val cx = originX + cell * x + cell / 2f
                val cy = originY + cell * y + cell / 2f
                val brightness = frame?.brightnessAt(x, y) ?: 0
                val color = if (brightness > 0) litColor.copy(alpha = brightness / 255f) else offColor
                drawCircle(color = color, radius = radius, center = Offset(cx, cy))
            }
        }
    }
}
