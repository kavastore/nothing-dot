package tech.dotlab.dot.feature.key.ui

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.key.action.ActionRegistry
import tech.dotlab.dot.feature.key.data.KeyLevel
import tech.dotlab.dot.feature.key.shizuku.ShizukuStatus
import tech.dotlab.dot.feature.key.shizuku.ShizukuUnlocker

private fun Gesture.label(): String = when (this) {
    Gesture.SINGLE -> "ОДИНАРНЫЙ"
    Gesture.DOUBLE -> "ДВОЙНОЙ"
    Gesture.TRIPLE -> "ТРОЙНОЙ"
    Gesture.LONG_PRESS -> "УДЕРЖАНИЕ"
}

@Composable
fun KeyStatusScreen(
    onPickAction: (Gesture) -> Unit,
    onRecordTrigger: () -> Unit,
    onUnlockHub: () -> Unit,
    onAdbHelper: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KeyViewModel = viewModel(factory = KeyViewModel.factory(LocalContext.current)),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()

    // Re-read package state (FULL/LITE) each time the screen resumes, e.g. after the wizard or ADB.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // In FULL the single press is ours too; otherwise the system owns it.
    val gestures = if (state.level == KeyLevel.FULL) {
        listOf(Gesture.SINGLE, Gesture.DOUBLE, Gesture.TRIPLE, Gesture.LONG_PRESS)
    } else {
        listOf(Gesture.DOUBLE, Gesture.TRIPLE, Gesture.LONG_PRESS)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SectionLabel("(03) KEY")
        Spacer(Modifier.height(12.dp))

        val (levelText, levelColor) = when (state.level) {
            KeyLevel.FULL -> "FULL · ВСЕ НАЖАТИЯ" to DotColors.Red
            KeyLevel.LITE -> "LITE · МУЛЬТИ-ТАП" to DotColors.Red
            KeyLevel.OFF -> "OFF · ВЫКЛ" to DotColors.MidGrey
        }
        Text(
            text = levelText,
            color = levelColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = when (state.level) {
                KeyLevel.FULL -> "Essential Space отключён. Ловятся все нажатия, включая одиночное."
                KeyLevel.LITE -> "Поверх системы: одиночное нажатие остаётся Essential Space, мульти-тап — наш."
                KeyLevel.OFF -> "Включите Accessibility-сервис, чтобы перехватывать жесты."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(16.dp))
        val keyText = state.config.keyCode?.let { KeyEvent.keyCodeToString(it) } ?: "не записана"
        Text(
            text = "КНОПКА: $keyText",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("ЖЕСТЫ")
        Spacer(Modifier.height(8.dp))
        gestures.forEach { gesture ->
            val actionTitle = state.config.bindings[gesture]?.let { ActionRegistry.byId(it)?.title } ?: "—"
            GestureRow(gesture.label(), actionTitle) { onPickAction(gesture) }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("ДОСТУП")
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ВКЛЮЧИТЬ ACCESSIBILITY", fontFamily = FontFamily.Monospace) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onRecordTrigger, modifier = Modifier.fillMaxWidth()) {
            Text("ЗАПИСАТЬ КНОПКУ", fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("ОСВОБОЖДЕНИЕ КНОПКИ")
        Spacer(Modifier.height(8.dp))
        if (state.level == KeyLevel.FULL) {
            Text(
                text = "Кнопка свободна.",
                color = DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val reverted = if (ShizukuUnlocker.status() == ShizukuStatus.READY) {
                            ShizukuUnlocker.apply(context, freed = false)
                        } else {
                            false
                        }
                        viewModel.refresh()
                        if (!reverted) onAdbHelper()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ВЕРНУТЬ КАК БЫЛО", fontFamily = FontFamily.Monospace) }
        } else {
            Button(
                onClick = onUnlockHub,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            ) { Text("ОСВОБОДИТЬ КНОПКУ", fontFamily = FontFamily.Monospace) }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Выбор способа: встроенный ADB · Shizuku · ADB с ПК",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun GestureRow(gesture: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DotColors.MidGrey)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = gesture,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = action,
            color = if (action == "—") DotColors.MidGrey else DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}
