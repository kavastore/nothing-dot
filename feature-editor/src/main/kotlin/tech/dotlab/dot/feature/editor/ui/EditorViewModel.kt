package tech.dotlab.dot.feature.editor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.ShapeMask
import tech.dotlab.dot.feature.editor.data.PixelArtRepository
import tech.dotlab.dot.feature.editor.domain.AnimationFrame
import tech.dotlab.dot.feature.editor.domain.EditorDocument
import tech.dotlab.dot.feature.editor.domain.EditorTool
import tech.dotlab.dot.feature.editor.domain.FrameOps
import tech.dotlab.dot.matrix.MatrixRenderer
import tech.dotlab.dot.matrix.MatrixRendererFactory

data class EditorUiState(
    val document: EditorDocument,
    val frameIndex: Int = 0,
    val tool: EditorTool = EditorTool.PEN,
    val brightness: Int = 255,
    val livePreview: Boolean = false,
    val matrixAvailable: Boolean = false,
) {
    val currentFrame: LogicalFrame get() = document.frames[frameIndex].frame
}

class EditorViewModel(
    private val repository: PixelArtRepository,
    private val renderer: MatrixRenderer,
    val profile: DeviceProfile,
) : ViewModel() {

    val mask: ShapeMask get() = profile.shapeMask

    private val _state = MutableStateFlow(
        EditorUiState(
            document = EditorDocument.blank(
                name = "Untitled",
                deviceId = profile.id,
                matrixSize = profile.matrixSize,
            ),
            // Auto-LIVE on matrix-capable devices: drawing mirrors to the back matrix immediately.
            livePreview = profile.supportsAppMatrix,
            matrixAvailable = profile.supportsAppMatrix || renderer.isAvailable,
        ),
    )
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        if (_state.value.livePreview) renderer.showFrame(_state.value.currentFrame)
    }

    fun loadOrNew(artId: Long?) {
        if (artId == null || artId <= 0) return
        viewModelScope.launch {
            repository.load(artId)?.let { doc ->
                _state.update { it.copy(document = doc, frameIndex = 0) }
            }
        }
    }

    fun selectTool(tool: EditorTool) = _state.update { it.copy(tool = tool) }

    fun setBrightness(value: Int) = _state.update { it.copy(brightness = value.coerceIn(0, 255)) }

    fun onCell(x: Int, y: Int) {
        val s = _state.value
        when (s.tool) {
            EditorTool.PEN -> mutateFrame { FrameOps.setPixel(it, mask, x, y, s.brightness) }
            EditorTool.ERASER -> mutateFrame { FrameOps.setPixel(it, mask, x, y, 0) }
            EditorTool.FILL -> mutateFrame { FrameOps.floodFill(it, mask, x, y, s.brightness) }
            EditorTool.EYEDROPPER ->
                _state.update { it.copy(brightness = it.currentFrame.brightnessAt(x, y)) }
        }
    }

    fun invert() = mutateFrame { FrameOps.invert(it, mask) }
    fun mirrorHorizontal() = mutateFrame { FrameOps.mirrorHorizontal(it, mask) }
    fun mirrorVertical() = mutateFrame { FrameOps.mirrorVertical(it, mask) }
    fun shift(dx: Int, dy: Int) = mutateFrame { FrameOps.shift(it, mask, dx, dy) }

    fun clearFrame() = mutateFrame { LogicalFrame.empty(mask.size) }

    fun setFrame(frame: LogicalFrame) = mutateFrame { frame }

    fun selectFrame(index: Int) = _state.update {
        if (index in it.document.frames.indices) it.copy(frameIndex = index) else it
    }

    fun addFrame() = _state.update { s ->
        val frames = s.document.frames + AnimationFrame(LogicalFrame.empty(mask.size))
        s.copy(document = s.document.copy(frames = frames), frameIndex = frames.lastIndex)
    }

    fun duplicateFrame() = _state.update { s ->
        val current = s.document.frames[s.frameIndex]
        val frames = s.document.frames.toMutableList().apply {
            add(s.frameIndex + 1, current.copy(frame = current.frame.copy()))
        }
        s.copy(document = s.document.copy(frames = frames), frameIndex = s.frameIndex + 1)
    }

    fun deleteFrame() = _state.update { s ->
        if (s.document.frames.size <= 1) return@update s
        val frames = s.document.frames.toMutableList().apply { removeAt(s.frameIndex) }
        s.copy(document = s.document.copy(frames = frames), frameIndex = s.frameIndex.coerceAtMost(frames.lastIndex))
    }

    fun rename(name: String) = _state.update { it.copy(document = it.document.copy(name = name)) }

    fun save(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(_state.value.document)
            _state.update { it.copy(document = it.document.copy(id = id)) }
            onSaved(id)
        }
    }

    fun makeActiveAod(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(_state.value.document)
            repository.setActiveAod(id)
            _state.update { it.copy(document = it.document.copy(id = id, isAodToy = true)) }
            onDone()
        }
    }

    fun toggleLivePreview() {
        val enabled = !_state.value.livePreview
        _state.update { it.copy(livePreview = enabled, matrixAvailable = renderer.isAvailable) }
        if (enabled) renderer.showFrame(_state.value.currentFrame) else renderer.close()
    }

    private fun mutateFrame(transform: (LogicalFrame) -> LogicalFrame) {
        _state.update { s ->
            val frames = s.document.frames.toMutableList()
            val updated = frames[s.frameIndex].copy(frame = transform(s.currentFrame))
            frames[s.frameIndex] = updated
            s.copy(document = s.document.copy(frames = frames))
        }
        if (_state.value.livePreview) renderer.showFrame(_state.value.currentFrame)
    }

    override fun onCleared() {
        renderer.close()
    }

    companion object {
        fun factory(context: Context): androidx.lifecycle.ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    val profile = EditorGraph.editingProfile()
                    EditorViewModel(
                        repository = EditorGraph.repository(appContext),
                        renderer = MatrixRendererFactory.create(appContext, profile),
                        profile = profile,
                    )
                }
            }
        }
    }
}
