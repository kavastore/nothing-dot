package tech.dotlab.dot.core.model

/**
 * Device-agnostic logical representation of a single Glyph Matrix frame.
 *
 * The editor, game and AOD toy all produce [LogicalFrame]s. The `:matrix` module is the only
 * place that knows how to turn this into actual SDK calls, keeping a single render path.
 *
 * Brightness is monochrome (0..255). On Phone (4a) Pro there is no color — "color" is brightness.
 */
class LogicalFrame(
    val size: Int,
    val brightness: IntArray,
) {
    init {
        require(size > 0) { "size must be positive, was $size" }
        require(brightness.size == size * size) {
            "brightness must hold size*size (${size * size}) values, was ${brightness.size}"
        }
    }

    fun brightnessAt(x: Int, y: Int): Int = brightness[index(x, y)]

    fun index(x: Int, y: Int): Int {
        require(x in 0 until size && y in 0 until size) { "($x,$y) out of bounds for size $size" }
        return y * size + x
    }

    fun copy(): LogicalFrame = LogicalFrame(size, brightness.copyOf())

    companion object {
        /** An all-off frame of the given matrix size. */
        fun empty(size: Int): LogicalFrame = LogicalFrame(size, IntArray(size * size))
    }
}
