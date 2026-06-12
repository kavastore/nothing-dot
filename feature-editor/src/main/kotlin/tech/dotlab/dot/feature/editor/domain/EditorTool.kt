package tech.dotlab.dot.feature.editor.domain

enum class EditorTool {
    PEN,
    ERASER,

    /** Flood fill the contiguous region under the touch (within the shape mask). */
    FILL,

    EYEDROPPER,
}
