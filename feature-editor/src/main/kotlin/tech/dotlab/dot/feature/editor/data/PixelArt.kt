package tech.dotlab.dot.feature.editor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved drawing. [matrixSize] is duplicated from the device so the art survives a device change
 * and stays renderable at its original resolution.
 */
@Entity(tableName = "pixel_art")
data class PixelArt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val deviceId: String,
    val matrixSize: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isAodToy: Boolean = false,
)
