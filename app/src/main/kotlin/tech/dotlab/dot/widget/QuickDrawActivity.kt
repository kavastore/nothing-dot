package tech.dotlab.dot.widget

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixCanvas
import tech.dotlab.dot.designsystem.DotTheme
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.device.DeviceOverride
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.matrix.MatrixRenderer
import tech.dotlab.dot.matrix.MatrixRendererFactory

/**
 * Fast finger-draw surface launched from the home-screen widget.
 *
 * While drawing, every stroke mirrors live to the Glyph Matrix. On "ГОТОВО" the final frame is
 * saved (for the widget preview) and held on the matrix for [HOLD_MS] before auto-clearing — the
 * 10s display is a fire-and-forget delayed close on the app process (same pattern as
 * [tech.dotlab.dot.feature.key.action.ShowGlyphArtAction]), so it survives the activity finishing.
 */
class QuickDrawActivity : ComponentActivity() {

    private lateinit var profile: DeviceProfile
    private var renderer: MatrixRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Respect a dev override even when launched without MainActivity having run first.
        DeviceOverride.init(applicationContext)
        profile = resolveProfile()
        renderer = MatrixRendererFactory.create(applicationContext, profile)

        enableEdgeToEdge()
        setContent {
            DotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickDrawScreen(
                        profile = profile,
                        appMatrixSupported = profile.supportsAppMatrix,
                        onStroke = { renderer?.showFrame(it) },
                        onDone = ::commitAndFinish,
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    private fun commitAndFinish(frame: LogicalFrame) {
        WidgetDrawStore.save(applicationContext, frame)
        DrawWidgetProvider.refresh(applicationContext)
        renderer?.showFrame(frame)
        // Hand the session off to a delayed close so it outlives this activity.
        val held = renderer
        renderer = null
        Handler(Looper.getMainLooper()).postDelayed({ held?.close() }, HOLD_MS)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Closed only if the user left without committing; commit nulls this out.
        renderer?.close()
        renderer = null
    }

    private fun resolveProfile(): DeviceProfile = when (val s = DeviceRegistry.resolveCurrent()) {
        is DeviceSupport.Supported -> s.profile
        is DeviceSupport.SystemTooOld -> s.profile
        DeviceSupport.UnsupportedDevice ->
            DeviceRegistry.byId("DEVICE_25111p") ?: DeviceRegistry.all().first()
    }

    private companion object {
        const val HOLD_MS = 10_000L
    }
}

@Composable
private fun QuickDrawScreen(
    profile: DeviceProfile,
    appMatrixSupported: Boolean,
    onStroke: (LogicalFrame) -> Unit,
    onDone: (LogicalFrame) -> Unit,
    onClose: () -> Unit,
) {
    var frame by remember { mutableStateOf(LogicalFrame.empty(profile.matrixSize)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionLabel("РИСУЙ → МАТРИЦА")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "НАРИСУЙ\nПАЛЬЦЕМ",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DotMatrixCanvas(
                mask = profile.shapeMask,
                frame = frame,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f),
                onCell = { x, y ->
                    val next = frame.brightness.copyOf()
                    next[frame.index(x, y)] = 255
                    frame = LogicalFrame(frame.size, next)
                    onStroke(frame)
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (appMatrixSupported) {
                "Рисунок идёт на заднюю матрицу вживую. «ГОТОВО» оставит его на 10 секунд."
            } else {
                "Матрица недоступна на этом устройстве — рисунок сохранится для превью виджета."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onDone(frame) },
            colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ГОТОВО (10 СЕК)", fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    frame = LogicalFrame.empty(profile.matrixSize)
                    onStroke(frame)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("ОЧИСТИТЬ", fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text("НАЗАД", fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
