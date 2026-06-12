package tech.dotlab.dot.feature.key.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.key.adb.AdbResult
import tech.dotlab.dot.feature.key.adb.InAppAdb

/**
 * In-app wireless ADB: pair via the Android "pair with code" dialog (mDNS), then run
 * `pm disable-user`/`pm enable` to free or restore the Essential Key — no PC, no extra app.
 */
@Composable
fun InAppAdbScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun run(block: suspend () -> AdbResult) {
        busy = true
        message = null
        scope.launch {
            val result = block()
            busy = false
            when (result) {
                is AdbResult.Ok -> { message = result.message; isError = false }
                is AdbResult.Error -> { message = result.message; isError = true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SectionLabel("ВСТРОЕННЫЙ ADB")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Освобождение без ПК и без сторонних приложений. Нужен Android 11+ и включённая " +
                "«Беспроводная отладка».",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(20.dp))
        Step(1, "Открой «Параметры разработчика» и включи «Беспроводная отладка».")
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ОТКРЫТЬ НАСТРОЙКИ ОТЛАДКИ", fontFamily = FontFamily.Monospace) }

        Spacer(Modifier.height(16.dp))
        Step(2, "Нажми «Подключение с помощью кода сопряжения». Появится 6-значный код — введи его сюда (окно не закрывай).")
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text("PIN-код сопряжения", fontFamily = FontFamily.Monospace) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { run { InAppAdb.pair(context, pin) } },
            enabled = !busy && pin.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("СОПРЯЧЬ", fontFamily = FontFamily.Monospace) }

        Spacer(Modifier.height(20.dp))
        Step(3, "После сопряжения освободи кнопку. Это отключит Essential Space для текущего пользователя.")
        Button(
            onClick = { run { InAppAdb.setFreed(context, freed = true) } },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
        ) { Text("ОСВОБОДИТЬ КНОПКУ", fontFamily = FontFamily.Monospace) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { run { InAppAdb.setFreed(context, freed = false) } },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ВЕРНУТЬ КАК БЫЛО", fontFamily = FontFamily.Monospace) }

        if (busy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = DotColors.Red)
        }
        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                color = if (isError) DotColors.MidGrey else DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "ШАГ $number",
            color = DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
