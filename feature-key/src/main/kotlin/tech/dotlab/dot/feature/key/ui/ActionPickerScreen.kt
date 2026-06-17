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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.feature.key.action.ActionRegistry
import tech.dotlab.dot.feature.key.action.OpenAppAction

/** Pick an action to bind to a gesture (torch, camera, screenshot, open an app, etc.). */
@Composable
fun ActionPickerScreen(
    gesture: Gesture,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KeyViewModel = viewModel(factory = KeyViewModel.factory(LocalContext.current)),
) {
    val context = LocalContext.current
    var pickingApp by remember { mutableStateOf(false) }

    if (pickingApp) {
        AppPickerList(
            modifier = modifier,
            onPick = { pkg ->
                viewModel.bind(gesture, OpenAppAction.ID_PREFIX + pkg)
                onDone()
            },
            onBack = { pickingApp = false },
        )
        return
    }

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

        ActionRow(title = "Открыть приложение") { pickingApp = true }

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

private data class LaunchableApp(val label: String, val packageName: String)

@Composable
private fun AppPickerList(
    modifier: Modifier,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val apps by produceState<List<LaunchableApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SectionLabel("ВЫБЕРИТЕ ПРИЛОЖЕНИЕ")
        Spacer(Modifier.height(16.dp))

        when (val list = apps) {
            null -> Text(
                text = "Загрузка…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )

            else -> LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(list, key = { it.packageName }) { app ->
                    ActionRow(title = app.label) { onPick(app.packageName) }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text("‹ НАЗАД", fontFamily = FontFamily.Monospace)
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .mapNotNull { resolve ->
            val pkg = resolve.activityInfo?.packageName ?: return@mapNotNull null
            LaunchableApp(resolve.loadLabel(pm).toString(), pkg)
        }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .sortedBy { it.label.lowercase() }
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
