package tech.dotlab.dot.feature.editor.domain

import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.ShapeMask

/**
 * Pure operations on a [LogicalFrame], all respecting the [ShapeMask] so only physically present
 * cells are ever touched. Each returns a new frame, which keeps undo/redo trivial to add later.
 */
object FrameOps {

    fun setPixel(frame: LogicalFrame, mask: ShapeMask, x: Int, y: Int, brightness: Int): LogicalFrame {
        if (!mask.isOn(x, y)) return frame
        val next = frame.brightness.copyOf()
        next[frame.index(x, y)] = brightness.coerceIn(0, 255)
        return LogicalFrame(frame.size, next)
    }

    fun floodFill(frame: LogicalFrame, mask: ShapeMask, x: Int, y: Int, brightness: Int): LogicalFrame {
        if (!mask.isOn(x, y)) return frame
        val target = frame.brightnessAt(x, y)
        val value = brightness.coerceIn(0, 255)
        if (target == value) return frame

        val size = frame.size
        val next = frame.brightness.copyOf()
        val stack = ArrayDeque<Int>()
        stack.addLast(y * size + x)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            if (next[idx] != target) continue
            val cx = idx % size
            val cy = idx / size
            if (!mask.isOn(cx, cy)) continue
            next[idx] = value
            if (cx > 0) stack.addLast(idx - 1)
            if (cx < size - 1) stack.addLast(idx + 1)
            if (cy > 0) stack.addLast(idx - size)
            if (cy < size - 1) stack.addLast(idx + size)
        }
        return LogicalFrame(size, next)
    }

    fun invert(frame: LogicalFrame, mask: ShapeMask): LogicalFrame =
        mapCells(frame, mask) { _, _, v -> 255 - v }

    fun mirrorHorizontal(frame: LogicalFrame, mask: ShapeMask): LogicalFrame =
        remap(frame, mask) { x, y -> (frame.size - 1 - x) to y }

    fun mirrorVertical(frame: LogicalFrame, mask: ShapeMask): LogicalFrame =
        remap(frame, mask) { x, y -> x to (frame.size - 1 - y) }

    /** Shifts the image by ([dx], [dy]) cells; content moved off-mask is dropped. */
    fun shift(frame: LogicalFrame, mask: ShapeMask, dx: Int, dy: Int): LogicalFrame {
        val size = frame.size
        val next = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                val sx = x - dx
                val sy = y - dy
                if (sx in 0 until size && sy in 0 until size && mask.isOn(sx, sy)) {
                    next[y * size + x] = frame.brightnessAt(sx, sy)
                }
            }
        }
        return LogicalFrame(size, next)
    }

    private inline fun mapCells(
        frame: LogicalFrame,
        mask: ShapeMask,
        transform: (x: Int, y: Int, value: Int) -> Int,
    ): LogicalFrame {
        val size = frame.size
        val next = frame.brightness.copyOf()
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                next[y * size + x] = transform(x, y, frame.brightnessAt(x, y)).coerceIn(0, 255)
            }
        }
        return LogicalFrame(size, next)
    }

    private inline fun remap(
        frame: LogicalFrame,
        mask: ShapeMask,
        source: (x: Int, y: Int) -> Pair<Int, Int>,
    ): LogicalFrame {
        val size = frame.size
        val next = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                val (sx, sy) = source(x, y)
                if (mask.isOn(sx, sy)) next[y * size + x] = frame.brightnessAt(sx, sy)
            }
        }
        return LogicalFrame(size, next)
    }
}
