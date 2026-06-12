package tech.dotlab.dot.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.device.DeviceOverride
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.device.SdkSignal
import tech.dotlab.dot.feature.key.data.UnlockState
import tech.dotlab.dot.feature.key.service.KeyEventBus

/** App settings: device info, key remapper status, onboarding replay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyConnected by KeyEventBus.serviceConnected.collectAsState()
    val sections = remember(keyConnected) { collectInfo(context, keyConnected) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SectionLabel("(05) ИНФО / НАСТРОЙКИ") },
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
            sections.forEach { section ->
                Spacer(Modifier.height(16.dp))
                SectionLabel(section.title)
                Spacer(Modifier.height(8.dp))
                section.rows.forEach { (label, value) -> InfoRow(label, value) }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("ВЫВОД НА МАТРИЦУ")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Если матрица не светится, хотя приложение «рисует» — проверьте, что в системе " +
                    "включён Glyph Interface. Этот тумблер недоступен приложению, его нужно включить " +
                    "вручную в настройках телефона.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { openGlyphInterfaceSettings(context) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            ) { Text("ОТКРЫТЬ НАСТРОЙКИ GLYPH INTERFACE", fontFamily = FontFamily.Monospace) }

            Spacer(Modifier.height(24.dp))
            SectionLabel("МОДЕЛЬ (ДЛЯ ТЕСТА)")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Принудительно выбрать профиль устройства. Меняет матрицу/функции для проверки. " +
                    "Внимание: выбор не-родной модели отключает реальный вывод на матрицу — для " +
                    "обычной работы держите «Авто». После выбора экран перезапустится.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(10.dp))
            OverrideButton(label = "АВТО (СБРОС)", active = DeviceOverride.activeId == null) {
                DeviceOverride.set(context, null)
                (context as? Activity)?.recreate()
            }
            DeviceRegistry.all().forEach { profile ->
                Spacer(Modifier.height(8.dp))
                OverrideButton(
                    label = "${profile.id} · ${profile.matrixSize}×${profile.matrixSize}",
                    active = DeviceOverride.activeId == profile.id,
                ) {
                    DeviceOverride.set(context, profile.id)
                    (context as? Activity)?.recreate()
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("ДЕЙСТВИЯ")
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val text = sections.joinToString("\n\n") { section ->
                        section.title + "\n" + section.rows.joinToString("\n") { "${it.first}: ${it.second}" }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Dot debug", text))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            ) { Text("КОПИРОВАТЬ ОТЛАДОЧНУЮ ИНФО", fontFamily = FontFamily.Monospace) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onReplayOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("ПОКАЗАТЬ ОНБОРДИНГ ЗАНОВО", fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("НАЗАД", fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OverrideButton(label: String, active: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (active) Modifier.border(1.dp, DotColors.Red) else Modifier),
    ) {
        Text(
            text = (if (active) "● " else "") + label,
            fontFamily = FontFamily.Monospace,
            color = if (active) DotColors.Red else MaterialTheme.colorScheme.onBackground,
        )
    }
}

private data class InfoSection(val title: String, val rows: List<Pair<String, String>>)

private fun collectInfo(context: Context, keyConnected: Boolean): List<InfoSection> {
    val pkg = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val versionName = pkg?.versionName ?: "?"
    val versionCode = pkg?.longVersionCode?.toString() ?: "?"

    val support = DeviceRegistry.resolveCurrent()
    val verdict = when (support) {
        is DeviceSupport.Supported -> "Supported"
        is DeviceSupport.SystemTooOld -> "SystemTooOld"
        DeviceSupport.UnsupportedDevice -> "Unsupported"
    }
    val profile = (support as? DeviceSupport.Supported)?.profile

    val keyLevel = UnlockState.level(context, keyConnected).name

    return listOf(
        InfoSection(
            "ПРИЛОЖЕНИЕ",
            listOf(
                "Пакет" to context.packageName,
                "Версия" to "$versionName ($versionCode)",
            ),
        ),
        InfoSection(
            "ТЕЛЕФОН",
            listOf(
                "Производитель" to Build.MANUFACTURER,
                "Модель" to Build.MODEL,
                "Device" to Build.DEVICE,
                "Product" to Build.PRODUCT,
                "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                "Сборка" to Build.DISPLAY,
            ),
        ),
        InfoSection(
            "ОПРЕДЕЛЕНИЕ УСТРОЙСТВА",
            listOf(
                "Вердикт" to verdict,
                "Профиль" to (profile?.id ?: "—"),
                "Матрица" to (profile?.let { "${it.matrixSize}×${it.matrixSize} · ${it.shapeMask.activeCount} LED" } ?: "—"),
                "Glyph Touch" to (profile?.hasGlyphTouch?.toYesNo() ?: "—"),
                "App-matrix" to (profile?.supportsAppMatrix?.toYesNo() ?: "—"),
                "AOD" to (profile?.supportsAod?.toYesNo() ?: "—"),
                "SDK matrixLength" to (SdkSignal.matrixLength?.toString() ?: "null"),
                "Override" to (DeviceOverride.activeId ?: "нет"),
            ),
        ),
        InfoSection(
            "ESSENTIAL KEY",
            listOf(
                "Accessibility" to keyConnected.toYesNo(),
                "Уровень" to keyLevel,
            ),
        ),
    )
}

private fun Boolean.toYesNo(): String = if (this) "да" else "нет"
