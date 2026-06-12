package tech.dotlab.dot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixPreview
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.core.model.ToyType
import tech.dotlab.dot.device.DeviceOverride
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.DeviceSupport

/**
 * Shows the verdict from [DeviceRegistry.resolveCurrent] and, when supported, previews the
 * device shape mask. Includes a dev override picker for testing without hardware.
 */
@Composable
fun DeviceCheckScreen(
    support: DeviceSupport,
    modifier: Modifier = Modifier,
    onContinue: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "DOT.",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            letterSpacing = 8.sp,
        )
        SectionLabel(text = "(01) DEVICE CHECK")

        when (support) {
            is DeviceSupport.Supported -> SupportedBody(support.profile)
            is DeviceSupport.SystemTooOld -> Message(
                title = "ОБНОВИТЕ NOTHING OS",
                body = "Устройство ${support.profile.id} поддерживается, но билд " +
                    "${support.currentBuild} ниже минимального 20250801. Обновите систему, " +
                    "чтобы работал live-показ на матрице.",
            )

            DeviceSupport.UnsupportedDevice -> Message(
                title = "УСТРОЙСТВО НЕ ПОДДЕРЖИВАЕТСЯ",
                body = "Это устройство пока вне реестра. Dot. рассчитан на Nothing Phone (4a) Pro. " +
                    "Рисовать можно и так — матрица просто не загорится.",
            )
        }

        if (onContinue != null) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            ) {
                Text(
                    text = "ПРОДОЛЖИТЬ",
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        DevDevicePicker()
    }
}

/**
 * Developer-only device override: forces a profile so Phone (3)'s 25×25 matrix, its interactive toy
 * and the Arkanoid game can be exercised on an emulator or a 4a Pro when the real phone isn't on
 * hand. Changing it persists via [DeviceOverride] and recreates the activity to re-resolve.
 */
@Composable
private fun DevDevicePicker() {
    val context = LocalContext.current
    SectionLabel(text = "DEV · ЭМУЛЯЦИЯ УСТРОЙСТВА")
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OverrideButton("AUTO", null, context)
        OverrideButton("(3)", "DEVICE_23112", context)
        OverrideButton("(4A)", "DEVICE_25111p", context)
    }
}

@Composable
private fun OverrideButton(label: String, id: String?, context: android.content.Context) {
    OutlinedButton(onClick = {
        DeviceOverride.set(context, id)
        (context as? Activity)?.recreate()
    }) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun SupportedBody(profile: DeviceProfile) {
    DotMatrixPreview(
        mask = profile.shapeMask,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .padding(top = 24.dp),
    )
    Text(
        text = buildString {
            appendLine("MATRIX  ${profile.matrixSize}×${profile.matrixSize}")
            appendLine("ACTIVE  ${profile.shapeMask.activeCount} LED")
            appendLine("AOD     ${if (profile.supportsAod) "YES" else "NO"}")
            appendLine("TOUCH   ${if (profile.hasGlyphTouch) "YES" else "NO"}")
            appendLine("TOYS    ${if (ToyType.ALL in profile.toyTypes) "ALL" else "AOD"}")
            append("APPMTX  ${if (profile.supportsAppMatrix) "YES" else "NO"}")
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = DotColors.Red,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Message(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
