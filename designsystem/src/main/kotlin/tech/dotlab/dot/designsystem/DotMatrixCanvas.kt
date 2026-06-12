package tech.dotlab.dot.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask
import kotlin.math.min

/**
 * Interactive matrix grid for the editor. Reports the (x, y) cell under taps and drags so the
 * caller can apply the active tool. Off-mask cells are dimmed and never reported.
 */
@Composable
fun DotMatrixCanvas(
    mask: ShapeMask,
    frame: LogicalFrame,
    modifier: Modifier = Modifier,
    litColor: Color = DotColors.White,
    offColor: Color = DotColors.DimGrey,
    onCell: (x: Int, y: Int) -> Unit,
) {
    require(frame.size == mask.size) { "frame/mask size mismatch" }
    val size = mask.size

    Canvas(
        modifier = modifier.pointerInput(size) {
            fun report(offset: Offset) {
                val side = min(this.size.width, this.size.height).toFloat()
                val cell = side / size
                if (cell <= 0f) return
                val originX = (this.size.width - side) / 2f
                val originY = (this.size.height - side) / 2f
                val x = ((offset.x - originX) / cell).toInt()
                val y = ((offset.y - originY) / cell).toInt()
                if (x in 0 until size && y in 0 until size && mask.isOn(x, y)) onCell(x, y)
            }
            detectTapGestures(onTap = { report(it) })
        }.pointerInput(size) {
            fun report(offset: Offset) {
                val side = min(this.size.width, this.size.height).toFloat()
                val cell = side / size
                if (cell <= 0f) return
                val originX = (this.size.width - side) / 2f
                val originY = (this.size.height - side) / 2f
                val x = ((offset.x - originX) / cell).toInt()
                val y = ((offset.y - originY) / cell).toInt()
                if (x in 0 until size && y in 0 until size && mask.isOn(x, y)) onCell(x, y)
            }
            detectDragGestures { change, _ ->
                change.consume()
                report(change.position)
            }
        },
    ) {
        val side = min(this.size.width, this.size.height)
        val cell = side / size
        val radius = cell * 0.42f
        val originX = (this.size.width - side) / 2f
        val originY = (this.size.height - side) / 2f
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                val cx = originX + cell * x + cell / 2f
                val cy = originY + cell * y + cell / 2f
                val v = frame.brightnessAt(x, y)
                val color = if (v > 0) litColor.copy(alpha = v / 255f) else offColor
                drawCircle(color = color, radius = radius, center = Offset(cx, cy))
            }
        }
    }
}
