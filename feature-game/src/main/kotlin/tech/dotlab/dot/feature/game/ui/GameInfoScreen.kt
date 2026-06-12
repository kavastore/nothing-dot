package tech.dotlab.dot.feature.game.ui

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixPreview
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.device.ShapeMask
import tech.dotlab.dot.feature.game.ArkanoidGame

/**
 * Explains the Arkanoid Glyph Toy and helps the user activate it. The game itself runs on the rear
 * matrix as a toy, so this screen is just guidance + a deep link to the Glyph Toys manager.
 *
 * @param hasGlyphButton whether the current device has a Glyph Button (Phone 3). When false the toy
 *   can't be controlled, so we say so honestly.
 */
@Composable
fun GameInfoScreen(
    hasGlyphButton: Boolean,
    matrixSize: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewFrame = remember(matrixSize) {
        ArkanoidGame(matrixSize, ShapeMask.circle(matrixSize)).draw()
    }
    val mask = remember(matrixSize) { ShapeMask.circle(matrixSize) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SectionLabel("(01) PLAY — ARKANOID")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ИГРА\nНА ЗАДНЕЙ МАТРИЦЕ",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(20.dp))
        DotMatrixPreview(
            mask = mask,
            frame = previewFrame,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("УПРАВЛЕНИЕ")
        Spacer(Modifier.height(8.dp))
        ControlLine("Наклоняй телефон — двигается платформа")
        ControlLine("Glyph-кнопка — запуск шара и выстрел")

        Spacer(Modifier.height(20.dp))
        if (!hasGlyphButton) {
            Text(
                text = "На этом устройстве нет Glyph-кнопки — игра рассчитана на Nothing Phone (3). " +
                    "Превью выше показывает, как выглядит поле.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = { openToysManager(context) },
            colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ОТКРЫТЬ GLYPH TOYS", fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("НАЗАД", fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun ControlLine(text: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text("· ", color = DotColors.Red, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}

private fun openToysManager(context: android.content.Context) {
    val intent = Intent().apply {
        component = ComponentName(
            "com.nothing.thirdparty",
            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(
            context,
            "Не удалось открыть Glyph Toys. Откройте Настройки → Glyph Interface.",
            Toast.LENGTH_LONG,
        ).show()
    }
}
