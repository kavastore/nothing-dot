package tech.dotlab.dot.feature.key.action

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import tech.dotlab.dot.core.action.KeyAction

class SilentModeAction : KeyAction {
    override val id: String = "silent_mode"
    override val title: String = "Беззвучный режим"
    override val icon: Int = android.R.drawable.ic_lock_silent_mode
    override val needsPermissions: List<String> = listOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)

    override fun run(context: Context) {
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notifications?.isNotificationPolicyAccessGranted == false) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            AudioManager.RINGER_MODE_SILENT
        } else {
            AudioManager.RINGER_MODE_NORMAL
        }
    }
}
