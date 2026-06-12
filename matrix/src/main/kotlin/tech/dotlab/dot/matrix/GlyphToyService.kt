package tech.dotlab.dot.matrix

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import tech.dotlab.dot.core.model.GlyphButtonEvent
import tech.dotlab.dot.core.model.LogicalFrame
import tech.dotlab.dot.device.DeviceProfile
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport

/**
 * Reusable base for Glyph Toy services: hides the [GlyphMatrixManager] lifecycle, device
 * registration and the [Messenger]/[Handler] plumbing that routes Glyph Button events, leaving
 * subclasses to implement just the toy behaviour.
 *
 * Events are mapped to clean callbacks:
 * - `EVENT_AOD`    → [onAodTick] (AOD toys, ~once/minute)
 * - `EVENT_CHANGE` → [onButton] with [GlyphButtonEvent.LONG_PRESS]
 * - `action_down`/`action_up` → [onButton] with [GlyphButtonEvent.HOLD_DOWN]/[GlyphButtonEvent.HOLD_UP]
 *
 * Frames are pushed with `setMatrixFrame` (toy context), unlike the app-side renderer which uses
 * `setAppMatrixFrame`.
 */
abstract class GlyphToyService : Service() {

    private var manager: GlyphMatrixManager? = null

    @Volatile
    protected var connected = false
        private set

    /** Device profile resolved for this toy; valid from [onBind] onward. */
    protected lateinit var profile: DeviceProfile
        private set

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) {
                super.handleMessage(msg)
                return
            }
            when (msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                GlyphToy.EVENT_AOD -> onAodTick()
                GlyphToy.EVENT_CHANGE -> onButton(GlyphButtonEvent.LONG_PRESS)
                EVENT_ACTION_DOWN -> onButton(GlyphButtonEvent.HOLD_DOWN)
                EVENT_ACTION_UP -> onButton(GlyphButtonEvent.HOLD_UP)
                else -> Unit
            }
        }
    }
    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder {
        profile = resolveProfile()
        initManager()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        onToyStopped()
        try {
            manager?.unInit()
        } catch (t: Throwable) {
            Log.w(tag(), "unInit failed", t)
        }
        manager = null
        connected = false
        return false
    }

    protected fun render(frame: LogicalFrame) {
        val gm = manager ?: return
        try {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(FrameBitmap.from(frame))
                .setScale(100)
                .setBrightness(255)
                .setPosition(0, 0)
                .build()
            val matrixFrame: GlyphMatrixFrame = GlyphMatrixFrame.Builder()
                .addTop(obj)
                .build(applicationContext)
            val rendered = matrixFrame.render()
            Log.i(tag(), "render: setMatrixFrame len=${rendered.size} lit=${rendered.count { it != 0 }}")
            gm.setMatrixFrame(rendered)
        } catch (t: Throwable) {
            Log.w(tag(), "render failed", t)
        }
    }

    protected open fun onToyConnected(profile: DeviceProfile) = Unit

    /** AOD tick (~once per minute) for AOD-capable toys. */
    protected open fun onAodTick() = Unit

    protected open fun onButton(event: GlyphButtonEvent) = Unit

    protected open fun onToyStopped() = Unit

    private fun resolveProfile(): DeviceProfile = when (val s = DeviceRegistry.resolveCurrent()) {
        is DeviceSupport.Supported -> s.profile
        is DeviceSupport.SystemTooOld -> s.profile
        DeviceSupport.UnsupportedDevice ->
            DeviceRegistry.byId("DEVICE_25111p") ?: DeviceRegistry.all().first()
    }

    private fun initManager() {
        try {
            manager = GlyphMatrixManager.getInstance(applicationContext).also { gm ->
                gm.init(object : GlyphMatrixManager.Callback {
                    override fun onServiceConnected(name: ComponentName?) {
                        SdkDeviceProbe.publishMatrixLength()
                        val code = GlyphTarget.resolve(profile.sdkTarget)
                        if (code == null) {
                            Log.w(tag(), "device ${profile.sdkTarget} unsupported by bundled SDK")
                            return
                        }
                        try {
                            val authorized = gm.register(code)
                            Log.i(tag(), "onServiceConnected: register($code) -> authorized=$authorized")
                            connected = true
                            onToyConnected(profile)
                        } catch (t: Throwable) {
                            Log.w(tag(), "register failed", t)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        connected = false
                    }
                })
            }
        } catch (t: Throwable) {
            Log.w(tag(), "GlyphMatrixManager init failed", t)
            manager = null
        }
    }

    private fun tag(): String = this::class.java.simpleName

    private companion object {
        const val EVENT_ACTION_DOWN = "action_down"
        const val EVENT_ACTION_UP = "action_up"
    }
}
