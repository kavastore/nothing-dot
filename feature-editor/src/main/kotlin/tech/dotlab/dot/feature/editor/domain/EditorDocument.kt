package tech.dotlab.dot.feature.editor.domain

import tech.dotlab.dot.core.model.LogicalFrame

data class AnimationFrame(
    val frame: LogicalFrame,
    val durationMs: Int = 500,
)

/**
 * Working representation of a drawing used by the editor and gallery, decoupled from Room rows.
 * [id] is 0 for a never-saved document.
 */
data class EditorDocument(
    val id: Long = 0,
    val name: String,
    val deviceId: String,
    val matrixSize: Int,
    val frames: List<AnimationFrame>,
    val isAodToy: Boolean = false,
) {
    val firstFrame: LogicalFrame get() = frames.first().frame

    companion object {
        fun blank(name: String, deviceId: String, matrixSize: Int): EditorDocument =
            EditorDocument(
                name = name,
                deviceId = deviceId,
                matrixSize = matrixSize,
                frames = listOf(AnimationFrame(LogicalFrame.empty(matrixSize))),
            )
    }
}
