package tech.dotlab.dot.feature.key.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.key.service.KeyEventBus

/** Listens for the next Essential Key press and stores its keyCode in [KeyPrefs]. */
@Composable
fun RecordTriggerScreen(
    onRecorded: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KeyViewModel = viewModel(factory = KeyViewModel.factory(LocalContext.current)),
) {
    val connected by KeyEventBus.serviceConnected.collectAsState()
    val captured by KeyEventBus.captured.collectAsState()

    DisposableEffect(Unit) {
        KeyEventBus.startRecording()
        onDispose {
            KeyEventBus.stopRecording()
            KeyEventBus.consumeCaptured()
        }
    }

    LaunchedEffect(captured) {
        captured?.let { code ->
            viewModel.setKeyCode(code)
            KeyEventBus.consumeCaptured()
            onRecorded()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionLabel("ЗАПИСЬ КНОПКИ")
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (connected) {
                "Нажмите Essential Key один раз.\nЖдём событие клавиши…"
            } else {
                "Accessibility-сервис выключен.\nВключите его и вернитесь сюда."
            },
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        captured?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = KeyEvent.keyCodeToString(it),
                color = DotColors.Red,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack) {
            Text("‹ ОТМЕНА", fontFamily = FontFamily.Monospace)
        }
    }
}
