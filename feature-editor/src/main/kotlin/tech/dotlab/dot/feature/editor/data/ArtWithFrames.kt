package tech.dotlab.dot.feature.editor.data

import androidx.room.Embedded
import androidx.room.Relation

data class ArtWithFrames(
    @Embedded val art: PixelArt,
    @Relation(parentColumn = "id", entityColumn = "art_id")
    val frames: List<PixelFrame>,
) {
    val orderedFrames: List<PixelFrame> get() = frames.sortedBy { it.orderIndex }
}
