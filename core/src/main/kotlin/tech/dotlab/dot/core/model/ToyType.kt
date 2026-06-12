package tech.dotlab.dot.core.model

/**
 * Categories of Glyph Toy a device can host.
 *
 * Phone (4a) Pro (`DEVICE_25111p`) supports only [AOD]; Phone (3) supports [ALL].
 */
enum class ToyType {
    /** Always-on Display toy. Receives `EVENT_AOD` roughly once per minute. */
    AOD,

    /** Full interactive toy (Glyph Button driven). Not available on 4a Pro. */
    ALL,
}
