package tech.dotlab.dot.matrix

import tech.dotlab.dot.core.model.LogicalFrame

/**
 * The single render path to the Glyph Matrix.
 *
 * The editor, game and AOD toy never talk to the SDK directly — they hand a [LogicalFrame] here
 * and this module turns it into SDK calls. Keep all SDK knowledge behind this interface.
 */
interface MatrixRenderer {

    /** True if this renderer can actually drive a physical matrix on the current device. */
    val isAvailable: Boolean

    /** Pushes a frame to the matrix (live preview via `setAppMatrixFrame`). */
    fun showFrame(frame: LogicalFrame)

    /** Stops driving the matrix from the app (`closeAppMatrix`) and releases the session. */
    fun close()
}
