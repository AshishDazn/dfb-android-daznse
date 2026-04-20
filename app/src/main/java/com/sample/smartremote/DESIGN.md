# Design System Document: The Smart Remote Interface

## 1. Overview & Creative North Star
**Creative North Star: "The Obsidian Conductor"**

In the world of home cinema, the interface should never compete with the content; it should facilitate it with the grace of a silent conductor. This design system moves away from the "flat grid of buttons" typical of remote apps, instead embracing a **Tactile Ethereal** aesthetic. We achieve this by blending deep, infinite blacks with "physical" layers of light and glass.

The design breaks the traditional template through **intentional asymmetry** and **tonal depth**. By utilizing oversized typography scales for status and ultra-refined line work for controls, we create a signature editorial look that feels like a premium physical remote translated into a digital medium.

---

## 2. Colors: The Tonal Spectrum
Our palette is rooted in the "Void"—a base of `#0e0e0e`—layered with sophisticated purples and cool grays to define hierarchy without visual clutter.

### The "No-Line" Rule
**Prohibit 1px solid borders for sectioning.** Boundaries are defined strictly through background shifts. A `surface-container-low` section sitting on a `surface` background is sufficient. If you feel a border is needed, your tonal contrast isn't high enough.

### Surface Hierarchy & Nesting
Treat the UI as stacked sheets of obsidian and frosted glass.
*   **Base:** `surface` (#0e0e0e) – The infinite background.
*   **Nesting Level 1:** `surface-container-low` (#131313) – Primary navigation zones.
*   **Nesting Level 2:** `surface-container` (#1a1a1a) – Interactive pods or card groupings.
*   **Nesting Level 3:** `surface-container-highest` (#262626) – Active toggles or prioritized controls.

### The "Glass & Gradient" Rule
To achieve a "Tactile" feel, main CTAs (like Power or Play) must use a **Signature Texture**:
*   **Primary Gradient:** A linear transition from `primary` (#b6a0ff) to `primary_dim` (#7e51ff) at a 135-degree angle.
*   **Glassmorphism:** For overlays or volume sliders, use `surface_variant` at 40% opacity with a `20px` backdrop-blur.

---

## 3. Typography: Editorial Precision
We utilize a dual-font strategy: **Manrope** for high-impact display moments and **Inter** for functional utility.

*   **Display (Manrope):** Used for "Now Playing" titles or channel numbers. `display-lg` (3.5rem) should feel heroic and authoritative.
*   **Headlines (Manrope):** Use `headline-sm` (1.5rem) for category headers (e.g., "Recently Watched").
*   **Body & Labels (Inter):** These are the workhorses. `label-md` (0.75rem) is used for button sub-text to ensure extreme legibility even at low brightness.
*   **Contrast as Hierarchy:** Use `on_surface` (Pure White) for active content and `on_surface_variant` (#adaaaa) for secondary metadata.

---

## 4. Elevation & Depth
In this system, elevation is a property of light, not shadows.

*   **Tonal Layering:** Instead of a shadow, place a `surface-container-highest` card inside a `surface-container-low` tray. The delta in luminance creates "Natural Lift."
*   **Ambient Shadows:** For floating elements (like a Volume Popover), use a shadow with a `48px` blur, 8% opacity, using the `primary` color value as the shadow tint. This mimics the glow of a screen in a dark room.
*   **The "Ghost Border" Fallback:** If accessibility requires a stroke, use `outline-variant` at **15% opacity**. It should be felt, not seen.
*   **Tactile Radius:** Use the `xl` (1.5rem) rounding for large interactive containers and `full` (9999px) for pill-shaped transport controls (Play/Pause).

---

## 5. Components

### Buttons
*   **Primary (Tactile):** Gradient fill (`primary` to `primary_dim`). `0.5rem` (DEFAULT) roundedness. Subtle inner-glow (1px, 20% white) on the top edge to simulate a physical edge.
*   **Secondary:** `surface-container-highest` fill. No border. White text.
*   **Tertiary (Ghost):** No fill. `on_surface_variant` text. High-contrast only on interaction.

### Cards & Lists
*   **The No-Divider Rule:** Forbid 1px dividers. Separate list items using `12px` of vertical white space or by alternating background tones between `surface-container-low` and `surface-container-lowest`.

### Input & Controls
*   **The D-Pad (Signature Component):** A large, `surface-container` circular housing with four directional segments. Active states use a `primary` outer-glow rather than a color change.
*   **Volume/Brightness Sliders:** Use Glassmorphism (`surface_variant` @ 40% + blur). The "track" is the background; the "thumb" is a high-chroma `primary` pill.

### Chips
*   **Selection Chips:** Use `secondary_container` for inactive states. Upon selection, animate to `primary` with a `white` label.

---

## 6. Do's and Don'ts

### Do
*   **DO** use whitespace as a functional tool. A remote needs room for "blind thumb navigation."
*   **DO** use haptic feedback cues. The UI should look like it would feel "clicky."
*   **DO** use `tertiary` (#ff97b8) sparingly for critical alerts or recording indicators.

### Don't
*   **DON'T** use pure #000000 for backgrounds unless it's a "turned off" state. Use `surface` (#0e0e0e) to maintain OLED depth without "crushing" the blacks.
*   **DON'T** use icons with fills for inactive states. Use thin-line (1.5pt) weights to maintain an airy, premium feel.
*   **DON'T** use traditional "Material" drop shadows. Stick to Tonal Layering and Ambient Glows.