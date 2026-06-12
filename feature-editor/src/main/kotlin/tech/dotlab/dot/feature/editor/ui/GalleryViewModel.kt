package tech.dotlab.dot.feature.editor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.dotlab.dot.feature.editor.data.PixelArtRepository
import tech.dotlab.dot.feature.editor.domain.EditorDocument

/** Exposes the saved-drawing list and gallery actions (delete, set AOD). */
class GalleryViewModel(private val repository: PixelArtRepository) : ViewModel() {

    val gallery: StateFlow<List<EditorDocument>> =
        repository.observeGallery().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun delete(document: EditorDocument) {
        viewModelScope.launch { repository.delete(document) }
    }

    fun makeActiveAod(document: EditorDocument) {
        viewModelScope.launch { repository.setActiveAod(document.id) }
    }

    companion object {
        fun factory(context: Context): androidx.lifecycle.ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer { GalleryViewModel(EditorGraph.repository(appContext)) }
            }
        }
    }
}
