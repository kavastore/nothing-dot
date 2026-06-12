package tech.dotlab.dot.feature.key.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.key.action.ActionRegistry

/** Pick an action to bind to a gesture (torch, camera, screenshot, etc.). */
@Composable
fun ActionPickerScreen(
    gesture: Gesture,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KeyViewModel = viewModel(factory = KeyViewModel.factory(LocalContext.current)),
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SectionLabel("ДЕЙСТВИЕ ДЛЯ ЖЕСТА")
        Spacer(Modifier.height(16.dp))

        ActionRegistry.all.forEach { action ->
            ActionRow(title = action.title) {
                ensurePermissions(context, action.needsPermissions)
                viewModel.bind(gesture, action.id)
                onDone()
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        ActionRow(title = "Сбросить") {
            viewModel.clearBinding(gesture)
            onDone()
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

private fun ensurePermissions(context: Context, permissions: List<String>) {
    if (Manifest.permission.ACCESS_NOTIFICATION_POLICY in permissions) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (manager?.isNotificationPolicyAccessGranted == false) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun ActionRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DotColors.MidGrey)
            .clickable(onClick = onClick)
            .padding(18.dp),
    )
}
