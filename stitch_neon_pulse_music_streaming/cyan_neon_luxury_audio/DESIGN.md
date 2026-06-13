---
name: Cyan-Neon Luxury Audio
colors:
  surface: '#0a1322'
  surface-dim: '#0a1322'
  surface-bright: '#31394a'
  surface-container-lowest: '#060e1d'
  surface-container-low: '#131c2b'
  surface-container: '#17202f'
  surface-container-high: '#222a3a'
  surface-container-highest: '#2c3545'
  on-surface: '#dae2f8'
  on-surface-variant: '#bac9cc'
  inverse-surface: '#dae2f8'
  inverse-on-surface: '#283141'
  outline: '#849396'
  outline-variant: '#3b494c'
  surface-tint: '#00daf3'
  primary: '#c3f5ff'
  on-primary: '#00363d'
  primary-container: '#00e5ff'
  on-primary-container: '#00626e'
  inverse-primary: '#006875'
  secondary: '#d0bcff'
  on-secondary: '#3c0091'
  secondary-container: '#571bc1'
  on-secondary-container: '#c4abff'
  tertiary: '#ffe7e5'
  on-tertiary: '#68000f'
  tertiary-container: '#ffc1be'
  on-tertiary-container: '#a6292f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#9cf0ff'
  primary-fixed-dim: '#00daf3'
  on-primary-fixed: '#001f24'
  on-primary-fixed-variant: '#004f58'
  secondary-fixed: '#e9ddff'
  secondary-fixed-dim: '#d0bcff'
  on-secondary-fixed: '#23005c'
  on-secondary-fixed-variant: '#5516be'
  tertiary-fixed: '#ffdad8'
  tertiary-fixed-dim: '#ffb3b0'
  on-tertiary-fixed: '#410006'
  on-tertiary-fixed-variant: '#8c1520'
  background: '#0a1322'
  on-background: '#dae2f8'
  surface-variant: '#2c3545'
typography:
  display-lg:
    fontFamily: Sora
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Sora
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Sora
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-md:
    fontFamily: Sora
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-sm:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.1em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  container-margin: 24px
  gutter-md: 16px
  stack-unit: 8px
  section-gap: 40px
---

## Brand & Style

This design system establishes a high-end, futuristic cyberpunk aesthetic tailored for a premium music streaming experience. The personality is immersive, sophisticated, and technologically advanced, moving away from "retro" cyberpunk into a refined "SaaS-meets-Luxury" space. 

The visual narrative is built on **Glassmorphism** and **Luminescence**. Interfaces should feel like holographic glass floating in deep space. We utilize heavy background blurs, translucent layers, and glowing accents to create a sense of depth and physical presence. The emotional response is one of exclusivity and awe—positioning the app as a high-fidelity portal to a sonic universe.

## Colors

The palette is anchored by a deep navy foundation (#081120), providing a high-contrast stage for neon accents. 

- **Primary (Electric Cyan):** Used for critical actions, playback progress, and primary branding. It represents the "energy" of the system.
- **Secondary (Neon Purple):** Used for atmospheric accents, gradients, and genre-specific highlights.
- **Highlight (Soft Coral):** Reserved strictly for active states, notifications, or "Live" indicators to provide a warm, human contrast to the cool tech palette.
- **Surface Strategy:** Backgrounds use a 60% opacity navy with a 32px backdrop blur. Borders are thin, semi-transparent white "light-leaks" that define edges without closing off the space.

## Typography

The typography strategy balances futuristic geometry with high-readability SaaS standards. 

- **Sora (Headlines):** Its geometric construction and wide aperture reflect a tech-forward, premium feel. Use for artist names and track titles.
- **Hanken Grotesk (Body):** A clean, sharp contemporary typeface that ensures legibility in descriptions and playlist details.
- **Geist (Labels/Meta):** The monospaced technical influence of Geist is used for timestamps, bit-rate indicators (e.g., "Hi-Res Lossless"), and technical metadata to reinforce the futuristic theme.

## Layout & Spacing

This design system uses a **fluid-to-safe-area** model. Since the target is a mobile music app, content is vertically stacked with generous breathing room to accommodate large touch targets.

- **Margins:** A consistent 24px side margin keeps content away from bezel edges, creating a luxurious "framed" look.
- **The "Pulse" Rhythm:** All spacing is based on an 8px grid. Vertical gaps between song items are 16px, while section headers are separated by 40px to create distinct visual chapters.
- **Album Art Focus:** Layouts are driven by imagery. Content should dynamically shift or color-bleed based on the album art currently in focus.

## Elevation & Depth

Depth is not achieved through traditional black shadows, but through **Tonal Luminance and Blurs**.

1.  **Level 0 (Base):** The #081120 background.
2.  **Level 1 (Cards):** 60% opacity surfaces with 32px backdrop-blur and a 1px border of `rgba(255, 255, 255, 0.1)`.
3.  **Level 2 (Floating Controls):** Elements like the "Now Playing" bar use a slightly higher opacity and a subtle inner glow (Electric Cyan, 10% opacity) to appear closer to the user.
4.  **Accents:** Glowing halos are used behind primary buttons or featured artist images to simulate a neon light source reflecting off the glass surface.

## Shapes

The shape language is defined by **large, organic radii**. This softens the "cold" tech aesthetic, making it feel more premium and ergonomic.

- **Cards & Containers:** Use a 24px (rounded-xl) radius. This applies to album art, playlist cards, and main glass containers.
- **Interactive Elements:** Buttons and pill-shaped chips use a maximum radius to create a "capsule" look.
- **Inner Elements:** When nesting elements (like a play button inside a card), the radius should be reduced proportionally to maintain visual harmony (e.g., 12px or 16px).

## Components

### Buttons
- **Primary:** Gradient from Electric Cyan to Neon Purple. No border. Text is dark navy for maximum contrast.
- **Secondary (Glass):** Transparent background with 1px `border_glass`. Subtle glow on hover/tap.

### Cards (Glass)
Used for playlists and albums. Feature a `surface_glass` background and `rounded-xl` corners. The bottom section of the card should have a 20% vertical gradient overlay to ensure white text remains legible over light album art.

### Now Playing Bar
A persistent glass component at the bottom of the UI. It uses a 40px backdrop blur and a thin Electric Cyan line on the very top edge to represent the current track progress.

### Chips & Tags
Used for genres or mood filters. Pill-shaped with a dark blue fill and a 1px Neon Purple border. When active, the fill switches to Neon Purple with white text.

### Visualizers
A unique component for this system. Dynamic waveforms using the Primary and Secondary colors, featuring a "motion blur" effect to simulate liquid light.

### Input Fields
Search bars should be fully rounded (pill-shaped) with a deep-navy inset fill and a subtle inner shadow to feel "etched" into the glass surface.