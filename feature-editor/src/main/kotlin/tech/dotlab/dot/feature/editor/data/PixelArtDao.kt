package tech.dotlab.dot.feature.editor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PixelArtDao {

    @Transaction
    @Query("SELECT * FROM pixel_art ORDER BY updatedAt DESC")
    fun observeGallery(): Flow<List<ArtWithFrames>>

    @Transaction
    @Query("SELECT * FROM pixel_art ORDER BY updatedAt DESC")
    suspend fun getAllArtWithFrames(): List<ArtWithFrames>

    @Transaction
    @Query("SELECT * FROM pixel_art WHERE id = :artId")
    suspend fun getArtWithFrames(artId: Long): ArtWithFrames?

    @Query("SELECT * FROM pixel_art WHERE isAodToy = 1 LIMIT 1")
    suspend fun getActiveAodArt(): PixelArt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArt(art: PixelArt): Long

    @Update
    suspend fun updateArt(art: PixelArt)

    @Delete
    suspend fun deleteArt(art: PixelArt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<PixelFrame>): List<Long>

    @Query("DELETE FROM pixel_frame WHERE art_id = :artId")
    suspend fun deleteFramesForArt(artId: Long)

    @Query("UPDATE pixel_art SET isAodToy = 0")
    suspend fun clearAllAodFlags()

    /** Replaces all frames for an art atomically (used on save). */
    @Transaction
    suspend fun replaceFrames(artId: Long, frames: List<PixelFrame>) {
        deleteFramesForArt(artId)
        insertFrames(frames.map { it.copy(artId = artId) })
    }

    /** Marks a single art as the active AOD toy, clearing the flag elsewhere. */
    @Transaction
    suspend fun setActiveAod(art: PixelArt) {
        clearAllAodFlags()
        updateArt(art.copy(isAodToy = true))
    }
}
