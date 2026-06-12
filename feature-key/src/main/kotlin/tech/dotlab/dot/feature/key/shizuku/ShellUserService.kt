package tech.dotlab.dot.feature.key.shizuku

import java.io.BufferedReader

/**
 * Runs in a separate process under shell uid (started by Shizuku). From here `pm` has the same
 * privileges as `adb shell`, so we can enable/disable the Essential Space packages.
 */
class ShellUserService : IShellUserService.Stub() {

    override fun destroy() {
        System.exit(0)
    }

    override fun setPackageEnabled(packageName: String, enabled: Boolean): Boolean {
        val command = if (enabled) {
            "pm enable $packageName"
        } else {
            "pm disable-user --user 0 $packageName"
        }
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.inputStream.bufferedReader().use(BufferedReader::readText)
            process.errorStream.bufferedReader().use(BufferedReader::readText)
            process.waitFor() == 0
        }.getOrDefault(false)
    }
}
