package tech.dotlab.dot.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixCanvas
import tech.dotlab.dot.designsystem.DotMatrixPreview
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.editor.domain.EditorTool

/** Pixel editor canvas with tools, animation frames, LIVE preview, and AOD export. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    artId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(artId) { viewModel.loadOrNew(artId) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SectionLabel("(02) DRAW") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", fontSize = 22.sp) } },
                actions = {
                    if (state.matrixAvailable || state.livePreview) {
                        TextButton(onClick = viewModel::toggleLivePreview) {
                            Text(if (state.livePreview) "LIVE ■" else "LIVE")
                        }
                    }
                    TextButton(onClick = { viewModel.save() }) { Text("SAVE") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        var showMore by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            DotMatrixCanvas(
                mask = viewModel.mask,
                frame = state.currentFrame,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(vertical = 8.dp),
                onCell = viewModel::onCell,
            )

            BrightnessSlider(value = state.brightness, onChange = viewModel::setBrightness)

            ToolRow(tool = state.tool, onSelect = viewModel::selectTool)

            MoreTools(
                expanded = showMore,
                onToggle = { showMore = !showMore },
                tool = state.tool,
                onSelectTool = viewModel::selectTool,
                onInvert = viewModel::invert,
                onMirrorH = viewModel::mirrorHorizontal,
                onMirrorV = viewModel::mirrorVertical,
                onShift = viewModel::shift,
                onClear = viewModel::clearFrame,
            )

            FrameStrip(viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun BrightnessSlider(value: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = "BRIGHTNESS  $value",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = DotColors.Red,
                activeTrackColor = DotColors.Red,
            ),
        )
    }
}

private val primaryTools = listOf(
    EditorTool.PEN to "ПЕРО",
    EditorTool.ERASER to "ЛАСТИК",
    EditorTool.FILL to "ЗАЛИВКА",
)

@Composable
private fun ToolRow(tool: EditorTool, onSelect: (EditorTool) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        primaryTools.forEach { (t, label) ->
            BigToolButton(
                label = label,
                selected = t == tool,
                onClick = { onSelect(t) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MoreTools(
    expanded: Boolean,
    onToggle: () -> Unit,
    tool: EditorTool,
    onSelectTool: (EditorTool) -> Unit,
    onInvert: () -> Unit,
    onMirrorH: () -> Unit,
    onMirrorV: () -> Unit,
    onShift: (Int, Int) -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(if (expanded) "ЕЩЁ ▾" else "ЕЩЁ ▸")
        }
        if (expanded) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChipButton("ПИПЕТКА", tool == EditorTool.EYEDROPPER) { onSelectTool(EditorTool.EYEDROPPER) }
                SmallButton("ИНВ", onInvert)
                SmallButton("⇄", onMirrorH)
                SmallButton("⇅", onMirrorV)
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SmallButton("←", { onShift(-1, 0) })
                SmallButton("→", { onShift(1, 0) })
                SmallButton("↑", { onShift(0, -1) })
                SmallButton("↓", { onShift(0, 1) })
                SmallButton("ОЧИСТИТЬ", onClear)
            }
        }
    }
}

@Composable
private fun BigToolButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) DotColors.Red else Color.Transparent
    val fg = if (selected) DotColors.White else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .height(56.dp)
            .background(bg)
            .border(1.dp, if (selected) DotColors.Red else DotColors.MidGrey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun FrameStrip(viewModel: EditorViewModel, state: EditorUiState) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("FRAMES")
            SmallButton("+", viewModel::addFrame)
            SmallButton("DUP", viewModel::duplicateFrame)
            SmallButton("DEL", viewModel::deleteFrame)
        }
        LazyRow(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.document.frames) { index, frame ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = if (index == state.frameIndex) 2.dp else 1.dp,
                            color = if (index == state.frameIndex) DotColors.Red else DotColors.DimGrey,
                        )
                        .clickable { viewModel.selectFrame(index) },
                ) {
                    DotMatrixPreview(
                        mask = viewModel.mask,
                        frame = frame.frame,
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                    )
                }
            }
        }
        Text(
            text = "В режиме Always-on кадр меняется раз в минуту.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) DotColors.Red else Color.Transparent
    val fg = if (selected) DotColors.White else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .background(bg)
            .border(1.dp, if (selected) DotColors.Red else DotColors.DimGrey)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = fg, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, DotColors.DimGrey)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
