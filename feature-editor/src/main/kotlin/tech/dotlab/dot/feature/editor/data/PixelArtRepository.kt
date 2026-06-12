package tech.dotlab.dot.feature.editor.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.feature.editor.domain.EditorDocument

/** Single access point to saved drawings; maps Room rows <-> [EditorDocument]. */
class PixelArtRepository(private val dao: PixelArtDao) {

    fun observeGallery(): Flow<List<EditorDocument>> =
        dao.observeGallery().map { list -> list.map { it.toDocument() } }

    suspend fun load(artId: Long): EditorDocument? =
        dao.getArtWithFrames(artId)?.toDocument()

    /** Inserts or updates a document and replaces its frames; returns the persisted id. */
    suspend fun save(document: EditorDocument): Long {
        val now = System.currentTimeMillis()
        val art = PixelArt(
            id = document.id,
            name = document.name,
            deviceId = document.deviceId,
            matrixSize = document.matrixSize,
            createdAt = if (document.id == 0L) now else now,
            updatedAt = now,
            isAodToy = document.isAodToy,
        )
        val id = if (document.id == 0L) dao.insertArt(art) else {
            dao.updateArt(art); art.id
        }
        dao.replaceFrames(id, document.toFrameEntities(id))
        return id
    }

    suspend fun delete(document: EditorDocument) {
        if (document.id == 0L) return
        dao.deleteArt(
            PixelArt(
                id = document.id,
                name = document.name,
                deviceId = document.deviceId,
                matrixSize = document.matrixSize,
                createdAt = 0,
                updatedAt = 0,
                isAodToy = document.isAodToy,
            ),
        )
    }

    suspend fun setActiveAod(artId: Long) {
        val art = dao.getArtWithFrames(artId)?.art ?: return
        dao.setActiveAod(art)
    }

    suspend fun activeAodFirstFrame(): LogicalFrame? {
        val art = dao.getActiveAodArt() ?: return null
        return dao.getArtWithFrames(art.id)?.toDocument()?.firstFrame
    }

    suspend fun activeAodDocument(): EditorDocument? {
        val art = dao.getActiveAodArt() ?: return null
        return dao.getArtWithFrames(art.id)?.toDocument()
    }

    /** All saved drawings (newest first) — used by the interactive toy to browse via Glyph Button. */
    suspend fun allDocuments(): List<EditorDocument> =
        dao.getAllArtWithFrames().map { it.toDocument() }
}
