# Prompt: Create DESIGN.md — Minimalist Blue Redesign

Use this to have the agent define a full design system before touching any component code. This produces a plan only — no UI changes in this pass.

---

You are acting as a design lead. Before changing any UI code, create `DESIGN.md` at the project root: a design system and redesign plan for this app (a personal productivity tool combining Tasks, Notes, and Money Management — Vue 3 + Vite frontend). Read `PRD.md` and `AGENT.md` first for context on the app's actual screens and features so the design plan is grounded in what really exists, not a generic template.

## Brief
Redesign the entire app around a **minimalist, blue-toned ("bluey") aesthetic**. It should feel calm, precise, and fast — this is a personal daily-use tool (tasks, notes, money), not a marketing site, so the design should support quick scanning and low visual fatigue over long daily use, not make a bold first-impression statement.

## What DESIGN.md must contain

**1. Design tokens**
- **Color**: define the palette as 4–6 named hex values — a primary blue, one or two supporting blues/neutrals, a background and surface color, a text color, and a small set of semantic colors (success, warning/over-budget, danger/delete, info) that still read as part of a blue-led palette rather than clashing with it. State both a light-mode and dark-mode set if the app should support dark mode — flag this as an assumption to confirm if not already decided.
- **Typography**: choose a type pairing (a body face optimized for readability at small sizes/dense lists, and a secondary face for headings/emphasis if used) with a defined type scale (sizes, weights, line-heights) for headings, body text, labels, and numeric/tabular data (important for the Money module — amounts should be easy to scan).
- **Spacing & layout grid**: define a spacing scale and base layout grid/breakpoints for the app shell (nav/sidebar + workspace).
- **Elevation & borders**: define how surfaces are distinguished — flat with hairline borders vs. soft shadows — pick one approach consistent with "minimalist" and apply it everywhere (cards, modals, inputs).
- **Iconography**: pick one icon style/library consistent with a minimalist aesthetic and lightweight bundle size.
- **Motion**: define minimal, purposeful transition rules (e.g. what animates on state change, hover, page transition) — err toward subtle, since this is a utility app used daily, not a showcase.

**2. Component-level direction**
For each shared UI primitive already used or needed across Tasks/Notes/Money, define its look in the new theme: buttons (primary/secondary/destructive), inputs/forms, cards/list items, modals/dialogs, nav/sidebar, tags/chips/category badges, empty states, loading states, and toasts/notifications. Be specific enough that a frontend agent could implement each without guessing (states: default/hover/active/disabled/focus).

**3. Module-specific notes**
Briefly note anything module-specific: how task priority/status should read visually, how note cards with images should look, how money amounts/positive vs negative values/budget progress bars should be color-coded within the blue palette (don't just default to red/green if it breaks the minimalist blue direction — decide deliberately and justify it).

**4. Signature element**
Per good design practice, name the one distinctive, memorable element this redesign is built around (not scattered decoration) — e.g. a specific way active states or focus rings work, a distinctive sidebar treatment, a particular way numbers/data are displayed. State it explicitly and explain how it threads through the app.

**5. Accessibility & quality floor**
State the non-negotiables: sufficient color contrast (especially important with an all-blue palette — verify text/background contrast ratios, don't just assume blue-on-blue is readable), visible keyboard focus states, responsive behavior down to mobile, and reduced-motion support.

**6. Migration plan**
A short, ordered plan for applying this design system to the existing app without a full rewrite: e.g. tokens/CSS variables first, then app shell, then shared components, then module-by-module (Tasks → Notes → Money). Note any components that will need structural (not just style) changes.

## Process
Before writing the final `DESIGN.md`, briefly self-critique the palette and layout choices against generic "AI blue theme" defaults (flat corporate blue-on-white SaaS look) and revise anything that reads as templated rather than a deliberate choice for a fast, personal daily-use tool. State what you changed and why.

## Output rules
- Produce `DESIGN.md` only in this pass — do not modify any `.vue` files, CSS, or Tailwind config yet.
- Be concrete: real hex values, real font names, real spacing numbers — not placeholders.
- After writing `DESIGN.md`, stop and wait for my review before implementing it.