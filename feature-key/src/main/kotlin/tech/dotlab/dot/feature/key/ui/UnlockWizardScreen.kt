package tech.dotlab.dot.feature.key.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.key.shizuku.ShizukuStatus
import tech.dotlab.dot.feature.key.shizuku.ShizukuUnlocker

/** Shizuku-based wizard to disable Essential Space packages and free the Essential Key. */
@Composable
fun UnlockWizardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KeyViewModel = viewModel(factory = KeyViewModel.factory(LocalContext.current)),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var version by remember { mutableIntStateOf(0) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> version++ }
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
        onDispose { runCatching { Shizuku.removeRequestPermissionResultListener(listener) } }
    }

    val status = remember(version) { ShizukuUnlocker.status() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SectionLabel("ОСВОБОЖДЕНИЕ ЧЕРЕЗ SHIZUKU")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Без ПК. Shizuku даёт приложению права уровня ADB, чтобы отключить пакеты " +
                "Essential Space. Полностью обратимо.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "СТАТУС: ${statusLabel(status)}",
            color = if (status == ShizukuStatus.READY) DotColors.Red else MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(20.dp))
        when (status) {
            ShizukuStatus.NOT_RUNNING -> {
                Text(
                    text = "1. Установите Shizuku (выберите любой источник ниже).\n" +
                        "2. Запустите его через беспроводную отладку (внутри Shizuku, без ПК) или через ADB.\n" +
                        "3. Вернитесь сюда и нажмите «Проверить снова».",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ОТКУДА СКАЧАТЬ:",
                    color = DotColors.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(8.dp))
                LinkButton(context, "GOOGLE PLAY", "market://details?id=moe.shizuku.privileged.api")
                Spacer(Modifier.height(8.dp))
                LinkButton(context, "ОФИЦИАЛЬНЫЙ САЙТ", "https://shizuku.rikka.app/")
                Spacer(Modifier.height(8.dp))
                LinkButton(context, "4PDA (ДЛЯ РОССИИ)", "https://4pda.to/forum/index.php?showtopic=965133")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Play может быть недоступен — тогда берите APK с офсайта или 4PDA.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { version++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("ПРОВЕРИТЬ СНОВА", fontFamily = FontFamily.Monospace)
                }
            }

            ShizukuStatus.NEED_PERMISSION -> {
                Text(
                    text = "Shizuku запущен. Выдайте Dot. разрешение Shizuku.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { ShizukuUnlocker.requestPermission() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
                ) { Text("ВЫДАТЬ РАЗРЕШЕНИЕ", fontFamily = FontFamily.Monospace) }
            }

            ShizukuStatus.READY -> {
                Text(
                    text = "Готово. Освободить кнопку (отключить Essential Space)?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = !working,
                    onClick = {
                        working = true
                        message = null
                        scope.launch {
                            val ok = ShizukuUnlocker.apply(context, freed = true)
                            working = false
                            viewModel.refresh()
                            message = if (ok) "Кнопка освобождена. Уровень FULL." else "Не удалось. Попробуйте ADB-путь."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
                ) { Text(if (working) "ВЫПОЛНЯЮ…" else "ОСВОБОДИТЬ КНОПКУ", fontFamily = FontFamily.Monospace) }
            }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = DotColors.Red, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun LinkButton(context: android.content.Context, label: String, url: String) {
    OutlinedButton(
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(label, fontFamily = FontFamily.Monospace) }
}

private fun statusLabel(status: ShizukuStatus): String = when (status) {
    ShizukuStatus.NOT_RUNNING -> "НЕ ЗАПУЩЕН"
    ShizukuStatus.NEED_PERMISSION -> "НУЖНО РАЗРЕШЕНИЕ"
    ShizukuStatus.READY -> "ГОТОВ"
}
