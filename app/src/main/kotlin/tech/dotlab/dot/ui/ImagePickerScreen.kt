package tech.dotlab.dot.ui

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixPreview
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.editor.domain.AnimationFrame
import tech.dotlab.dot.feature.editor.domain.EditorDocument
import tech.dotlab.dot.feature.editor.domain.ImageImport
import tech.dotlab.dot.feature.editor.domain.ImageImportMode
import tech.dotlab.dot.feature.editor.domain.ImageLayer
import tech.dotlab.dot.feature.editor.domain.PngExport
import tech.dotlab.dot.feature.editor.ui.EditorGraph
import tech.dotlab.dot.feature.key.data.KeyPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Image/collage composer shared by notification icons, DRAW frames, AOD and Essential Key. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerScreen(
    onBack: () -> Unit,
    onOpenEditor: (Long) -> Unit,
    targetArtId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { EditorGraph.editingProfile() }
    val mask = profile.shapeMask

    var layers by remember { mutableStateOf<List<ImageLayer>>(emptyList()) }
    var activeLayer by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(ImageImportMode.DITHER) }
    var threshold by remember { mutableIntStateOf(128) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var invert by remember { mutableStateOf(false) }
    var durationSec by remember { mutableFloatStateOf(3f) }
    var status by remember { mutableStateOf<String?>(null) }
    var previewSidePx by remember { mutableFloatStateOf(1f) }

    val frame = remember(layers, mode, threshold, contrast, invert) {
        ImageImport.compose(
            layers = layers,
            mask = mask,
            mode = mode,
            threshold = threshold,
            contrast = contrast,
            invert = invert,
        )
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { decodeSoftwareBitmap(context, uri) }
                if (bitmap != null) {
                    layers = layers + ImageLayer(bitmap)
                    activeLayer = layers.lastIndex
                    status = null
                } else {
                    status = "Не удалось открыть картинку."
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SectionLabel(if (targetArtId == null) "IMG → MATRIX" else "IMG → НОВЫЙ КАДР") },
                navigationIcon = { ModeTextButton("‹", onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Добавляй слои, двигай картинку пальцем и масштабируй щипком. " +
                    "Изображение можно увести за край — обрезается только итоговый кадр ${profile.matrixSize}×${profile.matrixSize}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (layers.isEmpty()) "ВЫБРАТЬ" else "+ СЛОЙ", fontFamily = FontFamily.Monospace)
                }
                if (layers.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            layers = layers.toMutableList().also { it.removeAt(activeLayer) }
                            activeLayer = activeLayer.coerceAtMost((layers.lastIndex).coerceAtLeast(0))
                        },
                    ) { Text("УДАЛИТЬ", fontFamily = FontFamily.Monospace) }
                }
            }

            if (layers.isNotEmpty()) {
                LayerStrip(
                    count = layers.size,
                    active = activeLayer,
                    onSelect = { activeLayer = it },
                )

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DotMatrixPreview(
                        mask = mask,
                        frame = frame,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                            .border(1.dp, DotColors.DimGrey)
                            .onSizeChanged { previewSidePx = it.width.toFloat().coerceAtLeast(1f) }
                            .pointerInput(activeLayer, previewSidePx) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val current = layers.getOrNull(activeLayer) ?: return@detectTransformGestures
                                    updateLayer(layers, activeLayer) {
                                        current.copy(
                                            scale = (current.scale * zoom).coerceIn(0.1f, 8f),
                                            offsetX = (current.offsetX + pan.x / previewSidePx * mask.size)
                                                .coerceIn(-mask.size * 2f, mask.size * 2f),
                                            offsetY = (current.offsetY + pan.y / previewSidePx * mask.size)
                                                .coerceIn(-mask.size * 2f, mask.size * 2f),
                                        )
                                    }.also { layers = it }
                                }
                            },
                    )
                }

                val selected = layers[activeLayer]
                SliderRow(
                    label = "МАСШТАБ СЛОЯ",
                    value = String.format(Locale.US, "%.2f×", selected.scale),
                    sliderValue = selected.scale,
                    onChange = { value -> layers = updateLayer(layers, activeLayer) { it.copy(scale = value) } },
                    valueRange = 0.1f..4f,
                )
                SliderRow(
                    label = "X",
                    value = selected.offsetX.roundToInt().toString(),
                    sliderValue = selected.offsetX,
                    onChange = { value -> layers = updateLayer(layers, activeLayer) { it.copy(offsetX = value) } },
                    valueRange = -profile.matrixSize.toFloat()..profile.matrixSize.toFloat(),
                )
                SliderRow(
                    label = "Y",
                    value = selected.offsetY.roundToInt().toString(),
                    sliderValue = selected.offsetY,
                    onChange = { value -> layers = updateLayer(layers, activeLayer) { it.copy(offsetY = value) } },
                    valueRange = -profile.matrixSize.toFloat()..profile.matrixSize.toFloat(),
                )

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModeChip("СЕРЫЙ", mode == ImageImportMode.GRAYSCALE) { mode = ImageImportMode.GRAYSCALE }
                    ModeChip("МОНО 0/1", mode == ImageImportMode.BINARY) { mode = ImageImportMode.BINARY }
                    ModeChip("ДИЗЕР 0/1", mode == ImageImportMode.DITHER) { mode = ImageImportMode.DITHER }
                }

                if (mode != ImageImportMode.GRAYSCALE) {
                    SliderRow(
                        label = "ПОРОГ 0/1",
                        value = threshold.toString(),
                        sliderValue = threshold.toFloat(),
                        onChange = { threshold = it.roundToInt() },
                        valueRange = 0f..255f,
                    )
                }
                SliderRow(
                    label = "КОНТРАСТ",
                    value = String.format(Locale.US, "%.1f×", contrast),
                    sliderValue = contrast,
                    onChange = { contrast = it },
                    valueRange = 0.25f..3f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ИНВЕРСИЯ",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(checked = invert, onCheckedChange = { invert = it })
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val repo = EditorGraph.repository(context)
                            val id = if (targetArtId != null) {
                                val existing = repo.load(targetArtId)
                                if (existing == null) {
                                    status = "Рисунок не найден."
                                    return@launch
                                }
                                repo.save(existing.copy(frames = existing.frames + AnimationFrame(frame)))
                            } else {
                                repo.save(
                                    EditorDocument(
                                        name = if (layers.size > 1) "Коллаж" else "Импорт",
                                        deviceId = profile.id,
                                        matrixSize = profile.matrixSize,
                                        frames = listOf(AnimationFrame(frame)),
                                    ),
                                )
                            }
                            onOpenEditor(id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
                ) {
                    Text(
                        if (targetArtId == null) "СОХРАНИТЬ В DRAW" else "ДОБАВИТЬ КАДР В АНИМАЦИЮ",
                        fontFamily = FontFamily.Monospace,
                    )
                }

                if (targetArtId == null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val uri = saveNotificationIcon(context, frame, mask)
                                status = if (uri != null) {
                                    openEssentialNotifications(context)
                                    "PNG сохранён в Pictures/Dot. В Essential Notifications выбери Custom → + и этот файл."
                                } else {
                                    "Не удалось сохранить PNG."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("В БИБЛИОТЕКУ УВЕДОМЛЕНИЙ", fontFamily = FontFamily.Monospace)
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val repo = EditorGraph.repository(context)
                                val id = repo.save(
                                    EditorDocument(
                                        name = if (layers.size > 1) "Коллаж" else "Картинка",
                                        deviceId = profile.id,
                                        matrixSize = profile.matrixSize,
                                        frames = listOf(AnimationFrame(frame)),
                                    ),
                                )
                                repo.setActiveAod(id)
                                val opened = openToysManager(context)
                                status = if (opened) "Сохранено. Включи Dot. в менеджере Glyph Toys." else
                                    "Сохранено как Always-on. Открой Glyph Interface → Glyph Toys."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("ПОСТАВИТЬ ALWAYS-ON", fontFamily = FontFamily.Monospace) }

                    SliderRow(
                        label = "ПОКАЗ ПО КНОПКЕ",
                        value = "${durationSec.roundToInt()} сек",
                        sliderValue = durationSec,
                        onChange = { durationSec = it },
                        valueRange = 1f..15f,
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                KeyPrefs(context).setShowImage(frame, durationSec.roundToInt())
                                status = "Готово. Назначь действие «Показать рисунок» в (03) KEY."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("ПО ESSENTIAL KEY", fontFamily = FontFamily.Monospace) }
                }
            }

            status?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = DotColors.Red, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LayerStrip(count: Int, active: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            ModeChip("СЛОЙ ${index + 1}", index == active) { onSelect(index) }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) DotColors.Red else DotColors.DimGrey
    val color = if (selected) DotColors.Red else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier.border(1.dp, border).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun ModeTextButton(label: String, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).padding(16.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    sliderValue: Float,
    onChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Row {
            Text(label, color = MaterialTheme.colorScheme.onBackground, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = DotColors.Red, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Slider(value = sliderValue, onValueChange = onChange, valueRange = valueRange)
    }
}

private fun updateLayer(
    layers: List<ImageLayer>,
    index: Int,
    transform: (ImageLayer) -> ImageLayer,
): List<ImageLayer> = layers.toMutableList().apply {
    if (index in indices) this[index] = transform(this[index])
}

private fun decodeSoftwareBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
    }
}.getOrNull()

private suspend fun saveNotificationIcon(
    context: Context,
    frame: LogicalFrame,
    mask: tech.dotlab.dot.device.ShapeMask,
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "dot-icon-$stamp.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dot")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            PngExport.writePng(PngExport.renderNotificationIcon(frame, mask), out)
        } ?: error("MediaStore output stream failed")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    }.getOrNull()
}

/** Nothing exposes no public import API, so open its settings after placing the PNG in MediaStore. */
private fun openEssentialNotifications(context: Context) {
    val candidates = listOf(
        Intent("android.settings.GLYPH_INTERFACE_SETTINGS"),
        Intent("com.nothing.settings.GLYPH_INTERFACE_SETTINGS"),
        Intent(Settings.ACTION_SETTINGS),
    )
    val intent = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
        ?: Intent(Settings.ACTION_SETTINGS)
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun openToysManager(context: Context): Boolean = runCatching {
    val intent = Intent()
        .setComponent(
            ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
            ),
        )
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
}.getOrElse { false }
