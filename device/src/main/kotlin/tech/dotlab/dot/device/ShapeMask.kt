package tech.dotlab.dot.device

/**
 * Describes which of the `size * size` logical cells physically exist on a matrix.
 *
 * The Glyph Matrix is round, so corner cells are absent. Editor/game/preview draw only where
 * [isOn] is true; everything else is rendered as disabled (greyed) in the UI.
 *
 * The mask is stored row-major (index = y * size + x), matching [tech.dotlab.dot.core.model.LogicalFrame].
 */
class ShapeMask(
    val size: Int,
    private val cells: BooleanArray,
) {
    init {
        require(size > 0) { "size must be positive, was $size" }
        require(cells.size == size * size) {
            "mask must hold size*size (${size * size}) cells, was ${cells.size}"
        }
    }

    val activeCount: Int = cells.count { it }

    fun isOn(x: Int, y: Int): Boolean {
        if (x !in 0 until size || y !in 0 until size) return false
        return cells[y * size + x]
    }

    fun toBooleanArray(): BooleanArray = cells.copyOf()

    companion object {
        /**
         * Builds a circular mask: a cell is on when its center lies within [radius] of the grid
         * center. This is a reasonable approximation of the round matrix.
         *
         * TODO(device): replace with the exact LED-allocation map for `DEVICE_25111p` from the
         * GlyphMatrix Developer Kit so corner cells match the hardware precisely.
         */
        fun circle(size: Int, radius: Double = (size - 1) / 2.0 + 0.5): ShapeMask {
            val center = (size - 1) / 2.0
            val cells = BooleanArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dx = x - center
                    val dy = y - center
                    cells[y * size + x] = dx * dx + dy * dy <= radius * radius
                }
            }
            return ShapeMask(size, cells)
        }

        /**
         * Builds a mask from a visual pattern, where each string is a row and `#`/`1`/`*`/`o`
         * mark present cells. Handy for hand-tuning the exact hardware shape later.
         */
        fun fromPattern(rows: List<String>): ShapeMask {
            val size = rows.size
            require(rows.all { it.length == size }) { "pattern must be a square of $size rows" }
            val on = setOf('#', '1', '*', 'o', 'O')
            val cells = BooleanArray(size * size)
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, c -> cells[y * size + x] = c in on }
            }
            return ShapeMask(size, cells)
        }
    }
}
