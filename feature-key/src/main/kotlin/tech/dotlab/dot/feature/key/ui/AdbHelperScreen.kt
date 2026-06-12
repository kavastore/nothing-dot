package tech.dotlab.dot.feature.key.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel

private const val FREE_CMD = "adb shell pm disable-user --user 0 com.nothing.ntessentialspace\n" +
    "adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder"

private const val REVERT_CMD = "adb shell pm enable com.nothing.ntessentialspace\n" +
    "adb shell pm enable com.nothing.ntessentialrecorder"

/** Desktop ADB helper: copy-paste commands to disable/restore Essential Space packages. */
@Composable
fun AdbHelperScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SectionLabel("ADB С КОМПЬЮТЕРА")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Запасной путь без Shizuku. Нужен компьютер с установленным ADB.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(16.dp))
        Step("1", "Включите «Отладку по USB» в Настройки → Для разработчиков и подключите телефон к ПК.")
        Step("2", "Разрешите отладку (диалог на телефоне). Проверьте: adb devices показывает устройство.")
        Step("3", "Выполните команды освобождения кнопки:")
        CommandBlock(FREE_CMD)
        Step("4", "Готово — кнопка свободна. Назад в Dot. → уровень станет FULL.")

        Spacer(Modifier.height(24.dp))
        SectionLabel("ВЕРНУТЬ КАК БЫЛО")
        Spacer(Modifier.height(8.dp))
        CommandBlock(REVERT_CMD)

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = "$number.",
            color = DotColors.Red,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CommandBlock(command: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, DotColors.MidGrey)
            .padding(12.dp),
    ) {
        Text(
            text = command,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = "КОПИРОВАТЬ",
                color = DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { clipboard.setText(AnnotatedString(command)) }
                    .padding(top = 8.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}
