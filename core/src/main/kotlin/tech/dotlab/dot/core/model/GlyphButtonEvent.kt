package tech.dotlab.dot.core.model

/**
 * Interactions a Glyph Toy can receive from the rear Glyph Button (Phone 3).
 *
 * Short press is owned by the system (cycles the toy carousel) and never reaches a toy, so only
 * these are available inside a toy service:
 * - [LONG_PRESS]  → SDK `EVENT_CHANGE`
 * - [HOLD_DOWN]   → SDK `action_down` (button pressed and held)
 * - [HOLD_UP]     → SDK `action_up` (button released)
 */
enum class GlyphButtonEvent {
    LONG_PRESS,
    HOLD_DOWN,
    HOLD_UP,
}
