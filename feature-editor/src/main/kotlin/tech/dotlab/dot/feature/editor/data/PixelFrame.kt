package tech.dotlab.dot.feature.editor.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One animation frame of a [PixelArt]. [pixels] holds `matrixSize * matrixSize` brightness bytes
 * (one byte per LED, 0..255), which is compact and color-free.
 */
@Entity(
    tableName = "pixel_frame",
    foreignKeys = [
        ForeignKey(
            entity = PixelArt::class,
            parentColumns = ["id"],
            childColumns = ["art_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("art_id")],
)
data class PixelFrame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "art_id") val artId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "duration_ms") val durationMs: Int = 500,
    val pixels: ByteArray,
) {
    // ByteArray needs structural equals/hashCode for Room/data-class correctness.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PixelFrame) return false
        return id == other.id &&
            artId == other.artId &&
            orderIndex == other.orderIndex &&
            durationMs == other.durationMs &&
            pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + artId.hashCode()
        result = 31 * result + orderIndex
        result = 31 * result + durationMs
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
