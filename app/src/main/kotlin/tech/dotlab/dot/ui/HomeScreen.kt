package tech.dotlab.dot.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixCanvas
import tech.dotlab.dot.designsystem.DotTitle
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.device.DeviceOverride
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.feature.key.data.KeyLevel
import tech.dotlab.dot.feature.key.data.UnlockState
import tech.dotlab.dot.feature.key.service.KeyEventBus
import tech.dotlab.dot.matrix.MatrixRenderer
import tech.dotlab.dot.matrix.MatrixRendererFactory

@Composable
fun HomeScreen(
    support: DeviceSupport,
    onDraw: () -> Unit,
    onKey: () -> Unit,
    onImage: () -> Unit,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onDeviceInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = (support as? DeviceSupport.Supported)?.profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        DotTitle("DOT.")
        SectionLabel("GLYPH MATRIX TOOLKIT", Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(24.dp))

        if (DeviceOverride.activeId != null) {
            OverrideBanner(activeId = DeviceOverride.activeId!!)
            Spacer(Modifier.height(24.dp))
        }

        if (profile != null && profile.supportsAppMatrix) {
            MatrixDemo(profile = profile)
            Spacer(Modifier.height(24.dp))
        }

        Tile(number = "01", title = "PLAY", subtitle = "Arkanoid на матрице", enabled = true, onClick = onPlay)
        Spacer(Modifier.height(16.dp))
        Tile(number = "02", title = "DRAW", subtitle = "Pixel editor + AOD toy", enabled = true, onClick = onDraw)
        Spacer(Modifier.height(16.dp))
        Tile(number = "03", title = "KEY", subtitle = "Essential Key remapper", enabled = true, onClick = onKey)
        Spacer(Modifier.height(16.dp))
        Tile(number = "04", title = "IMG", subtitle = "Картинка на матрицу", enabled = true, onClick = onImage)
        Spacer(Modifier.height(16.dp))
        Tile(number = "05", title = "ИНФО", subtitle = "Настройки и отладка", enabled = true, onClick = onSettings)

        Spacer(Modifier.height(24.dp))
        StatusBar(support = support, onClick = onDeviceInfo)
    }
}

/**
 * Live demonstration: tap/draw on the 13x13 grid and it appears on the physical matrix in real time.
 * Only shown on a matrix-capable device.
 */
@Composable
private fun MatrixDemo(profile: DeviceProfile) {
    val context = LocalContext.current
    var frame by remember(profile.id) { mutableStateOf(LogicalFrame.empty(profile.matrixSize)) }
    // Renderer lives only while Home is resumed, so it never holds the matrix session
    // while the editor (its own LIVE session) is open.
    var renderer by remember(profile.id) { mutableStateOf<MatrixRenderer?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, profile.id) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val r = MatrixRendererFactory.create(context, profile)
                    renderer = r
                    r.showFrame(frame)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    renderer?.close()
                    renderer = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            renderer?.close()
            renderer = null
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("LIVE DEMO")
            Spacer(Modifier.weight(1f))
            Text(
                text = "ОЧИСТИТЬ",
                color = DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable {
                        frame = LogicalFrame.empty(profile.matrixSize)
                        renderer?.showFrame(frame)
                    }
                    .padding(4.dp),
            )
        }
        Text(
            text = "Рисуй пальцем — появится на задней матрице.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        GlyphInterfaceHint()
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DotMatrixCanvas(
                mask = profile.shapeMask,
                frame = frame,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(1f),
                onCell = { x, y ->
                    val next = frame.brightness.copyOf()
                    next[frame.index(x, y)] = 255
                    frame = LogicalFrame(profile.matrixSize, next)
                    renderer?.showFrame(frame)
                },
            )
        }
    }
}

/**
 * Hint shown under the live demo: real matrix output requires Nothing OS "Glyph Interface" to be
 * enabled. That toggle is private (unreadable by apps), so if it is off the matrix silently stays
 * dark even though the SDK accepts frames. One tap jumps straight to the system toggle.
 */
@Composable
private fun GlyphInterfaceHint() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DotColors.Red.copy(alpha = 0.5f))
            .clickable { openGlyphInterfaceSettings(context) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Не светится матрица?",
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
            Text(
                text = "Включи Glyph Interface в настройках телефона.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(0.dp))
        Text(
            text = "ВКЛЮЧИТЬ ›",
            color = DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
    }
}

/** Visible warning that a developer profile override is forcing a non-native device profile. */
@Composable
private fun OverrideBanner(activeId: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DotColors.Red)
            .padding(16.dp),
    ) {
        Text(
            text = "ТЕСТ-РЕЖИМ",
            color = DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = "Принудительно выбран профиль $activeId. Вывод на реальную матрицу может не работать.",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "СБРОСИТЬ →",
            color = DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    DeviceOverride.set(context, null)
                    (context as? Activity)?.recreate()
                },
        )
    }
}

@Composable
private fun Tile(
    number: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val border = if (enabled) DotColors.MidGrey else DotColors.DimGrey
    val titleColor = if (enabled) MaterialTheme.colorScheme.onBackground else DotColors.MidGrey
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "($number)",
            color = DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 3.sp,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
        Text(text = "›", color = titleColor, fontSize = 22.sp)
    }
}

@Composable
private fun StatusBar(support: DeviceSupport, onClick: () -> Unit) {
    val context = LocalContext.current
    val matrix = when (support) {
        is DeviceSupport.Supported -> "OK"
        is DeviceSupport.SystemTooOld -> "OLD OS"
        DeviceSupport.UnsupportedDevice -> "NONE"
    }
    val keyConnected by KeyEventBus.serviceConnected.collectAsState()
    val keyLevel = UnlockState.level(context, keyConnected)
    val keyText = when (keyLevel) {
        KeyLevel.FULL -> "FULL"
        KeyLevel.LITE -> "LITE"
        KeyLevel.OFF -> "OFF"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(DotColors.DimGrey.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusItem("MATRIX", matrix, if (matrix == "OK") DotColors.Red else DotColors.MidGrey)
        StatusItem("KEY", keyText, if (keyLevel == KeyLevel.OFF) DotColors.MidGrey else DotColors.Red)
        StatusItem("AOD", "—", DotColors.MidGrey)
    }
}

@Composable
private fun StatusItem(label: String, value: String, valueColor: Color) {
    Row {
        Text("$label ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
