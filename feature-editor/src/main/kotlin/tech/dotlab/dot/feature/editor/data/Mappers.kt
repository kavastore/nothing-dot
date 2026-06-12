package tech.dotlab.dot.feature.editor.data

import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.feature.editor.domain.AnimationFrame
import tech.dotlab.dot.feature.editor.domain.EditorDocument

/** Room row ↔ [EditorDocument] / [LogicalFrame] conversions used by [PixelArtRepository]. */
internal fun ByteArray.toLogicalFrame(size: Int): LogicalFrame {
    require(this.size == size * size) { "pixel byte count ${this.size} != ${size * size}" }
    val brightness = IntArray(this.size) { this[it].toInt() and 0xFF }
    return LogicalFrame(size, brightness)
}

internal fun LogicalFrame.toPixelBytes(): ByteArray =
    ByteArray(brightness.size) { (brightness[it].coerceIn(0, 255)).toByte() }

internal fun ArtWithFrames.toDocument(): EditorDocument {
    val size = art.matrixSize
    val frames = orderedFrames.map { pf ->
        AnimationFrame(pf.pixels.toLogicalFrame(size), pf.durationMs)
    }.ifEmpty { listOf(AnimationFrame(LogicalFrame.empty(size))) }
    return EditorDocument(
        id = art.id,
        name = art.name,
        deviceId = art.deviceId,
        matrixSize = size,
        frames = frames,
        isAodToy = art.isAodToy,
    )
}

internal fun EditorDocument.toFrameEntities(artId: Long): List<PixelFrame> =
    frames.mapIndexed { index, animationFrame ->
        PixelFrame(
            artId = artId,
            orderIndex = index,
            durationMs = animationFrame.durationMs,
            pixels = animationFrame.frame.toPixelBytes(),
        )
    }
