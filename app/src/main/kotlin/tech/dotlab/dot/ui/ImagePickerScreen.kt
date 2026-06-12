package tech.dotlab.dot.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
import tech.dotlab.dot.feature.editor.ui.EditorGraph
import tech.dotlab.dot.feature.key.data.KeyPrefs
import kotlin.math.roundToInt

/** Import a photo into a matrix frame; save as AOD toy or bind to Essential Key "show image". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { EditorGraph.editingProfile() }
    val mask = profile.shapeMask

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dither by remember { mutableStateOf(true) }
    var brightness by remember { mutableStateOf(1f) }
    var durationSec by remember { mutableStateOf(3f) }
    var status by remember { mutableStateOf<String?>(null) }

    val frame: LogicalFrame? = remember(sourceBitmap, dither, brightness) {
        sourceBitmap?.let {
            val base = ImageImport.fromBitmap(it, mask, dither = dither)
            scaleBrightness(base, brightness)
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                sourceBitmap = withContext(Dispatchers.IO) { decodeSoftwareBitmap(context, uri) }
                status = null
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SectionLabel("КАРТИНКА → МАТРИЦА") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = "Выбери картинку — она превратится в ${profile.matrixSize}×${profile.matrixSize} точек. " +
                    "Можно поставить её на матрицу навсегда (Always-on) или показывать по Essential Key.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (sourceBitmap == null) "ВЫБРАТЬ КАРТИНКУ" else "ВЫБРАТЬ ДРУГУЮ",
                    fontFamily = FontFamily.Monospace,
                )
            }

            if (frame != null) {
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DotMatrixPreview(
                        mask = mask,
                        frame = frame,
                        modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ДИЗЕРИНГ",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(checked = dither, onCheckedChange = { dither = it })
                }

                SliderRow(
                    label = "ЯРКОСТЬ",
                    value = "${(brightness * 255).roundToInt()}",
                    sliderValue = brightness,
                    onChange = { brightness = it },
                    valueRange = 0.1f..1f,
                )

                SliderRow(
                    label = "ПОКАЗ ПО КНОПКЕ",
                    value = "${durationSec.roundToInt()} сек",
                    sliderValue = durationSec,
                    onChange = { durationSec = it },
                    valueRange = 1f..15f,
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val repo = EditorGraph.repository(context)
                            val doc = EditorDocument(
                                name = "Картинка",
                                deviceId = profile.id,
                                matrixSize = profile.matrixSize,
                                frames = listOf(AnimationFrame(frame)),
                            )
                            val id = repo.save(doc)
                            repo.setActiveAod(id)
                            val opened = openToysManager(context)
                            status = if (opened) {
                                "Картинка сохранена как Always-on. В открывшемся менеджере тоев " +
                                    "включи «Dot.»."
                            } else {
                                "Картинка сохранена. Открой Settings → Glyph Interface → Glyph Toys → " +
                                    "Always-on и выбери «Dot.»."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
                ) {
                    Text("ПОСТАВИТЬ ВСЕГДА (ALWAYS-ON)", fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            KeyPrefs(context).setShowImage(frame, durationSec.roundToInt())
                            status = "Готово. Назначь действие «Показать рисунок» на жест в (03) KEY — " +
                                "и картинка будет всплывать на ${durationSec.roundToInt()} сек."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("ПОКАЗЫВАТЬ ПО ESSENTIAL KEY", fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }
            }

            status?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    color = DotColors.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("НАЗАД", fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(24.dp))
        }
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
    Column(Modifier.padding(top = 12.dp)) {
        Row {
            Text(label, color = MaterialTheme.colorScheme.onBackground, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = DotColors.Red, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Slider(value = sliderValue, onValueChange = onChange, valueRange = valueRange)
    }
}

private fun scaleBrightness(frame: LogicalFrame, factor: Float): LogicalFrame {
    val out = IntArray(frame.brightness.size) {
        (frame.brightness[it] * factor).roundToInt().coerceIn(0, 255)
    }
    return LogicalFrame(frame.size, out)
}

private fun decodeSoftwareBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
    }
}.getOrNull()

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
