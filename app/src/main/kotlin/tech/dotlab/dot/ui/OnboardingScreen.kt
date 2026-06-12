package tech.dotlab.dot.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixPreview
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.device.ShapeMask
import kotlin.math.abs

private data class Slide(val tag: String, val title: String, val body: String)

/** Feature slides tailored to the resolved device (size, Glyph Button, interactive toy, game). */
private fun featureSlidesFor(profile: DeviceProfile?): List<Slide> {
    val size = profile?.matrixSize ?: 13
    val slides = mutableListOf(
        Slide(
            tag = "(02) DRAW",
            title = "РИСУЙ\nПО ПИКСЕЛЯМ",
            body = "Редактор ${size}×${size}: перо, ластик, заливка, кадры анимации. С включённым " +
                "LIVE рисунок сразу зеркалится на заднюю матрицу.",
        ),
        Slide(
            tag = "AOD",
            title = "ВСЕГДА\nНА ЭКРАНЕ",
            body = "Поставь рисунок или картинку как Always-on Glyph Toy — она будет жить на " +
                "матрице даже при выключенном экране.",
        ),
    )
    if (profile?.hasGlyphTouch == true) {
        slides += Slide(
            tag = "(01) PLAY",
            title = "ARKANOID\nНА МАТРИЦЕ",
            body = "Играй прямо на задней матрице: наклоняй телефон, чтобы двигать платформу, " +
                "Glyph-кнопка запускает шар и стреляет.",
        )
        slides += Slide(
            tag = "GLYPH BUTTON",
            title = "ЗАДНЯЯ\nКНОПКА",
            body = "Долгое нажатие листает твои рисунки на матрице, удержание включает анимацию. " +
                "Кнопка управляет интерактивными тоями.",
        )
    }
    slides += Slide(
        tag = "(03) KEY",
        title = "ОСВОБОДИ\nESSENTIAL KEY",
        body = "Назначай действия на нажатия Essential Key. В режиме FULL ловятся все " +
            "нажатия, включая одиночное — без всплытия Essential Space.",
    )
    return slides
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val support = remember { DeviceRegistry.resolveCurrent() }
    val profile = (support as? DeviceSupport.Supported)?.profile
    val slides = remember(profile?.id) { featureSlidesFor(profile) }
    val pageCount = 1 + slides.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pageCount - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                TextButton(onClick = onFinish) {
                    Text("ПРОПУСТИТЬ", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            if (page == 0) {
                DeviceSlide(support = support)
            } else {
                FeatureSlide(slide = slides[page - 1], mask = profile?.shapeMask ?: ShapeMask.circle(13))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(pageCount) { i ->
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) DotColors.Red else DotColors.DimGrey),
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DotColors.Red),
            ) {
                Text(
                    text = if (isLast) "НАЧАТЬ" else "ДАЛЬШЕ",
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DeviceSlide(support: DeviceSupport) {
    val profile = (support as? DeviceSupport.Supported)?.profile

    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        SectionLabel("(01) ВАШЕ УСТРОЙСТВО")
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (profile != null) "NOTHING PHONE\n${deviceName(profile.id)}" else "УСТРОЙСТВО\nВНЕ РЕЕСТРА",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(20.dp))
        PixelMotif(mask = profile?.shapeMask ?: ShapeMask.circle(13))

        Spacer(Modifier.height(20.dp))
        if (profile != null) {
            Text(
                text = "ПОДДЕРЖИВАЕТСЯ:",
                color = DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            buildList {
                add("Матрица ${profile.matrixSize}×${profile.matrixSize} · ${profile.shapeMask.activeCount} LED")
                add("Рисование и LIVE-превью" + if (profile.supportsAppMatrix) "" else " (нет)")
                add("Always-on Glyph Toy" + if (profile.supportsAod) "" else " (нет)")
                if (profile.hasGlyphTouch) {
                    add("Glyph-кнопка: интерактивный той")
                    add("Игра Arkanoid на задней матрице")
                }
                add("Картинка → матрица (импорт + яркость)")
                add("Ремаппер Essential Key (Lite/Full)")
            }.forEach { FeatureLine(it) }
            if (profile.supportsAppMatrix) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Важно: для вывода на матрицу включите Glyph Interface в настройках " +
                        "телефона — иначе экран матрицы останется тёмным.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        } else {
            Text(
                text = "Dot. рассчитан на Nothing Phone (4a) Pro. Рисовать можно и так — матрица " +
                    "просто не загорится.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
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

@Composable
private fun FeatureSlide(slide: Slide, mask: ShapeMask) {
    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        SectionLabel(slide.tag)
        Spacer(Modifier.height(16.dp))
        Text(
            text = slide.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 42.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(20.dp))
        PixelMotif(mask = mask)
        Spacer(Modifier.height(20.dp))
        Text(
            text = slide.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 24.sp,
        )
    }
}

/** A small matrix that animates a sweeping brightness wave — the "pixel" motion accent. */
@Composable
private fun PixelMotif(mask: ShapeMask) {
    val transition = rememberInfiniteTransition(label = "pixel")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (mask.size * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val frame = remember(mask, phase.toInt()) {
        val size = mask.size
        val out = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!mask.isOn(x, y)) continue
                val d = abs((x + y) - phase)
                out[y * size + x] = if (d < 1.6f) (255 * (1f - d / 1.6f)).toInt().coerceIn(40, 255) else 40
            }
        }
        LogicalFrame(size, out)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DotMatrixPreview(
            mask = mask,
            frame = frame,
            modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f),
        )
    }
}

private fun deviceName(id: String): String = when (id) {
    "DEVICE_25111p" -> "(4A) PRO"
    "DEVICE_23112" -> "(3)"
    else -> id
}
