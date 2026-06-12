package tech.dotlab.dot.feature.key.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import tech.dotlab.dot.feature.key.data.EssentialKeyPackages
import kotlin.coroutines.resume

enum class ShizukuStatus {
    /** Shizuku app not installed or service not started. */
    NOT_RUNNING,

    /** Running but our app hasn't been granted Shizuku permission yet. */
    NEED_PERMISSION,

    /** Ready to run privileged commands. */
    READY,
}

/**
 * Frees (or restores) the Essential Key by toggling the Essential Space packages through a
 * Shizuku-hosted shell user service. No PC required once Shizuku is running.
 */
object ShizukuUnlocker {

    const val PERMISSION_REQUEST_CODE = 7001

    fun status(): ShizukuStatus {
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!running || Shizuku.isPreV11()) return ShizukuStatus.NOT_RUNNING
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return if (granted) ShizukuStatus.READY else ShizukuStatus.NEED_PERMISSION
    }

    fun requestPermission() {
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
    }

    /**
     * @param freed true = disable Essential Space packages (free the key); false = re-enable them.
     * @return true if both packages were toggled successfully.
     */
    suspend fun apply(context: Context, freed: Boolean): Boolean {
        if (status() != ShizukuStatus.READY) return false
        val component = ComponentName(context.packageName, ShellUserService::class.java.name)
        val args = Shizuku.UserServiceArgs(component)
            .daemon(false)
            .processNameSuffix("dotshell")
            .debuggable(false)
            .version(1)

        return suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val service = IShellUserService.Stub.asInterface(binder)
                    val enabled = !freed
                    val ok = runCatching {
                        EssentialKeyPackages.all.all { pkg ->
                            service.setPackageEnabled(pkg, enabled)
                        }
                    }.getOrDefault(false)
                    runCatching { Shizuku.unbindUserService(args, this, true) }
                    if (cont.isActive) cont.resume(ok)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            val bound = runCatching { Shizuku.bindUserService(args, connection) }.isSuccess
            if (!bound && cont.isActive) cont.resume(false)
        }
    }
}
