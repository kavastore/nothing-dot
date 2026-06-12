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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel

/**
 * Hub that explains the three ways to free the Essential Key (no single forced path), so the user
 * can pick what fits: in-app ADB (no PC, no extra app), Shizuku (extra app), or ADB from a PC.
 */
@Composable
fun UnlockMethodScreen(
    onInAppAdb: () -> Unit,
    onShizuku: () -> Unit,
    onAdbPc: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        SectionLabel("ОСВОБОЖДЕНИЕ КНОПКИ")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Чтобы поймать одиночное нажатие Essential Key, нужно один раз отключить " +
                "системный Essential Space командой уровня shell. Это безопасно и обратимо. " +
                "Выбери удобный способ:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(20.dp))
        MethodCard(
            badge = "РЕКОМЕНДУЕМ",
            title = "ВСТРОЕННЫЙ ADB",
            pros = "Без ПК и без сторонних приложений — всё внутри Dot.",
            cons = "Нужно включить «Беспроводную отладку» и один раз ввести PIN; после " +
                "перезагрузки телефона — переподключиться.",
            onClick = onInAppAdb,
            highlight = true,
        )
        Spacer(Modifier.height(14.dp))
        MethodCard(
            badge = null,
            title = "SHIZUKU",
            pros = "Без ПК. Удобно, если уже пользуешься Shizuku.",
            cons = "Нужно установить и запускать отдельное приложение Shizuku.",
            onClick = onShizuku,
            highlight = false,
        )
        Spacer(Modifier.height(14.dp))
        MethodCard(
            badge = null,
            title = "ADB С ПК",
            pros = "Самый надёжный способ.",
            cons = "Нужен компьютер с установленным adb (один раз).",
            onClick = onAdbPc,
            highlight = false,
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun MethodCard(
    badge: String?,
    title: String,
    pros: String,
    cons: String,
    onClick: () -> Unit,
    highlight: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (highlight) DotColors.Red else DotColors.MidGrey)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        if (badge != null) {
            Text(
                text = badge,
                color = DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 2.sp,
            )
            Text("›", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("+ $pros", color = DotColors.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text("− $cons", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
