package tech.dotlab.dot.matrix

import tech.dotlab.dot.core.model.LogicalFrame

object NoopMatrixRenderer : MatrixRenderer {
    override val isAvailable: Boolean = false
    override fun showFrame(frame: LogicalFrame) = Unit
    override fun close() = Unit
}
