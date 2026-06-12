package tech.dotlab.dot.feature.key.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tech.dotlab.dot.feature.key.data.EssentialKeyPackages
import kotlin.coroutines.resume

/** Result of an in-app ADB step, carrying a human message for the UI. */
sealed interface AdbResult {
    data class Ok(val message: String) : AdbResult
    data class Error(val message: String) : AdbResult
}

/**
 * Drives the wireless-debugging flow entirely in-app: discover the pairing service over mDNS,
 * pair with the user's PIN, connect over TLS, and toggle the Essential Space packages with `pm`.
 * No PC and no third-party app required (Android 11+).
 */
object InAppAdb {

    private const val PAIRING_TYPE = "_adb-tls-pairing._tcp."

    suspend fun pair(context: Context, pin: String): AdbResult = withContext(Dispatchers.IO) {
        runCatching {
            val service = discoverPairing(context)
                ?: return@withContext AdbResult.Error(
                    "Не нашёл сервис сопряжения. Открой «Беспроводная отладка → Подключение по коду» " +
                        "и держи это окно открытым.",
                )
            val host = service.host?.hostAddress ?: "127.0.0.1"
            val ok = DotAdbManager.getInstance(context).pair(host, service.port, pin)
            if (ok) {
                AdbResult.Ok("Сопряжение выполнено. Теперь можно подключаться и освобождать кнопку.")
            } else {
                AdbResult.Error("Сопряжение отклонено. Проверь PIN и попробуй ещё раз.")
            }
        }.getOrElse { e ->
            AdbResult.Error("Сопряжение не удалось: ${e.message ?: e.javaClass.simpleName}. Проверь PIN и попробуй снова.")
        }
    }

    /** Disables (freed=true) or re-enables the Essential Space packages via an ADB shell. */
    suspend fun setFreed(context: Context, freed: Boolean): AdbResult = withContext(Dispatchers.IO) {
        runCatching {
            val manager = DotAdbManager.getInstance(context)
            val connected = manager.connectTls(context, 10_000)
            if (!connected) {
                return@withContext AdbResult.Error(
                    "Не удалось подключиться к ADB. Включи «Беспроводную отладку» и выполни сопряжение.",
                )
            }
            val verb = if (freed) "disable-user --user 0" else "enable"
            val log = StringBuilder()
            for (pkg in EssentialKeyPackages.all) {
                log.append(runShell(manager, "pm $verb $pkg")).append('\n')
            }
            manager.close()
            val msg = if (freed) {
                "Кнопка освобождена. Вернись назад — статус станет FULL."
            } else {
                "Готово, вернул как было."
            }
            AdbResult.Ok(msg)
        }.getOrElse { e ->
            AdbResult.Error("Команда не выполнилась: ${e.message ?: e.javaClass.simpleName}.")
        }
    }

    private fun runShell(manager: DotAdbManager, command: String): String {
        val stream = manager.openStream("shell:$command")
        try {
            return stream.openInputStream().readBytes().toString(Charsets.UTF_8).trim()
        } finally {
            runCatching { stream.close() }
        }
    }

    private suspend fun discoverPairing(context: Context): NsdServiceInfo? = withTimeoutOrNull(15_000) {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val resolved = suspendCancellableCoroutine<NsdServiceInfo?> { cont ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    if (cont.isActive) cont.resume(null)
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            if (cont.isActive) cont.resume(serviceInfo)
                        }
                    })
                }
            }
            nsd.discoverServices(PAIRING_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            cont.invokeOnCancellation { runCatching { nsd.stopServiceDiscovery(listener) } }
        }
        resolved
    }
}
