package tech.dotlab.dot.matrix

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.DeviceProfile

/**
 * SDK-backed renderer. Holds a [GlyphMatrixManager] session and pushes frames via
 * `setAppMatrixFrame`. Service binding is asynchronous, so frames pushed before the connection is
 * ready are buffered and flushed on connect.
 *
 * All SDK access is guarded: on an emulator (or any device without the Glyph service) the session
 * never connects and [isAvailable] stays false, so callers transparently get a no-op.
 */
class GlyphMatrixRenderer(
    context: Context,
    private val profile: DeviceProfile,
) : MatrixRenderer {

    private val appContext = context.applicationContext
    private var manager: GlyphMatrixManager? = null

    @Volatile
    private var connected = false
    private var pendingFrame: LogicalFrame? = null

    override val isAvailable: Boolean
        get() = connected

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            SdkDeviceProbe.publishMatrixLength()
            val code = GlyphTarget.resolve(profile.sdkTarget)
            if (code == null) {
                Log.w(TAG, "device ${profile.sdkTarget} unsupported by bundled SDK")
                connected = false
                return
            }
            try {
                val authorized = manager?.register(code)
                Log.i(TAG, "onServiceConnected: register($code) -> authorized=$authorized")
                connected = true
                pendingFrame?.let { push(it) }
                pendingFrame = null
            } catch (t: Throwable) {
                Log.w(TAG, "register failed", t)
                connected = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected")
            connected = false
        }
    }

    init {
        if (profile.supportsAppMatrix) {
            try {
                Log.i(TAG, "init: binding GlyphMatrixManager for ${profile.id} (${profile.matrixSize}px)")
                manager = GlyphMatrixManager.getInstance(appContext).also { it.init(callback) }
            } catch (t: Throwable) {
                Log.w(TAG, "GlyphMatrixManager init failed; renderer unavailable", t)
                manager = null
            }
        } else {
            Log.i(TAG, "init: profile ${profile.id} does not support app-matrix; renderer is no-op")
        }
    }

    override fun showFrame(frame: LogicalFrame) {
        require(frame.size == profile.matrixSize) {
            "frame size ${frame.size} != device matrix ${profile.matrixSize}"
        }
        if (!connected) {
            pendingFrame = frame
            return
        }
        push(frame)
    }

    private fun push(frame: LogicalFrame) {
        val gm = manager ?: return
        try {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(FrameBitmap.from(frame))
                .setScale(100)
                .setOrientation(0)
                .setPosition(0, 0)
                .setBrightness(255)
                .build()
            val matrixFrame: GlyphMatrixFrame = GlyphMatrixFrame.Builder()
                .addTop(obj)
                .build(appContext)
            val rendered = matrixFrame.render()
            Log.i(TAG, "push: setAppMatrixFrame len=${rendered.size} lit=${rendered.count { it != 0 }}")
            gm.setAppMatrixFrame(rendered)
        } catch (t: Throwable) {
            Log.w(TAG, "showFrame failed", t)
        }
    }

    override fun close() {
        val gm = manager ?: return
        Log.i(TAG, "close: closeAppMatrix + unInit")
        try {
            gm.closeAppMatrix()
        } catch (t: Throwable) {
            Log.w(TAG, "closeAppMatrix failed", t)
        }
        try {
            gm.unInit()
        } catch (t: Throwable) {
            Log.w(TAG, "unInit failed", t)
        }
        manager = null
        connected = false
        pendingFrame = null
    }

    private companion object {
        const val TAG = "GlyphMatrixRenderer"
    }
}
