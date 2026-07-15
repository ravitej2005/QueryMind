# QueryMind — Design

## 1. Product Vision

QueryMind turns "I need a report" into a conversation. A user connects a database, asks a question in plain English, and immediately sees the SQL that answers it, the data, a chart, and a plain-language explanation of what the numbers mean — then can save it, drop it on a dashboard, and share it with a team.

The product feels less like a form-heavy admin panel and more like a **focused analytical tool** — closer to a code editor or a terminal than to a typical dashboard-builder SaaS. Precision and information density over decoration.

## 2. UI Philosophy

- **Data first, chrome second.** Charts, tables, and SQL are the content; navigation and controls stay quiet and out of the way.
- **Transparency over magic.** Every AI answer shows its work: the generated SQL is always visible and editable, never hidden behind a "trust me" black box. This is both a UX principle and a trust/safety feature (users can visually verify the AI isn't doing something destructive before running it).
- **Keyboard-first for power users**, mouse-first for casual ones — the SQL editor and chat both support command-palette style shortcuts (`Cmd+Enter` to run, `Cmd+K` for command palette).
- **One primary action per screen.** The chat screen's primary action is "ask." The editor's is "run." The dashboard's is "add widget." Everything else is secondary, visually recessive.

## 3. UX Principles

1. Never let the user wonder "is it thinking?" — every AI/query action has an explicit loading state with the current stage (`Generating SQL… / Validating… / Running… / Explaining…`).
2. Every destructive/irreversible action (disconnect a database, delete a dashboard) requires a confirmation with the resource name typed or explicitly confirmed — no accidental data loss.
3. Errors are actionable, not just descriptive: a rejected AI-generated SQL statement explains *why* it was rejected ("this looks like a write operation and has been blocked") rather than a generic 400.
4. Empty states teach: a fresh workspace's chat screen suggests 3 example questions based on the connected schema instead of a blank box.
5. Latency budget: anything over ~400ms shows a skeleton/loading state; nothing pops in without a transition.

## 4. Navigation

Persistent left sidebar (collapsible to icon-only rail):

```
🏠 Overview
💬 Ask (AI Chat)
🗄  Connections
📝 SQL Editor
📊 Dashboards
📁 Reports
🧠 Saved Prompts
⚙️  Settings
```

Top bar: workspace switcher (left), global search / command palette trigger (center), user menu + notifications bell (right).

Routing (React Router):
```
/                     -> Overview (redirects to /ask if first login)
/ask                  -> AI Chat
/editor               -> SQL Editor
/connections          -> Connection list
/connections/:id      -> Connection detail (schema explorer, settings)
/dashboards           -> Dashboard list
/dashboards/:id       -> Dashboard view/edit
/reports              -> Saved/scheduled reports (Phase 5+)
/prompts              -> Saved prompt library
/settings/*           -> Profile, workspace, security
```

## 5. Layouts

**Ask (Chat) screen** — three-pane layout:
```
┌───────────────┬─────────────────────────────┐
│               │   Generated SQL (Monaco,     │
│  Chat thread  │   read-only unless "Edit")   │
│  (messages)   ├─────────────────────────────┤
│               │   Results: Table | Chart tab │
│               ├─────────────────────────────┤
│  [ input ]    │   AI Explanation panel        │
└───────────────┴─────────────────────────────┘
```
On mobile, this collapses to a single-column stack with a tab switcher (Chat / SQL / Results).

**SQL Editor screen** — Monaco editor top, results grid bottom, saved-queries list as a collapsible left rail.

**Dashboard screen** — CSS grid canvas, widgets are draggable/resizable cards (react-grid-layout), an "Add widget" button opens a picker (chart type + saved query/prompt source).

## 6. Chat Interface Design

- Messages are left-aligned (user) / right-aligned (assistant) bubbles, but the assistant's "bubble" is really a rich card: SQL snippet preview + a "View full" expand, a compact chart thumbnail, and the explanation text.
- Inline citations: when the explanation references a number, that number is visually tied back to the table/chart (hover-highlight) so users trust the AI isn't making numbers up.
- A persistent "Run" vs "Explain only" toggle above the input — some users just want the SQL, not execution.

## 7. SQL Editor Design

- Monaco Editor with a custom SQL language configuration: keyword highlighting, schema-aware autocomplete (table/column suggestions sourced from the cached schema for the active connection).
- Inline gutter markers for the validator: a red dot on a line the `SafeExecutionEngine` would reject, with a hover tooltip explaining why, shown **before** the user even hits Run.
- `Cmd+Enter` executes; `Cmd+S` saves as a named query; `Cmd+/` toggles comment.
- Results grid virtualizes rows (only render what's visible) since result sets can be large even after capping.

## 8. Color Palette

Dark-mode-first (matches the "editor/terminal" feel), with a light mode as a secondary supported theme, not an afterthought.

| Token | Dark | Light | Usage |
|---|---|---|---|
| `--bg-base` | `#0B0D12` | `#FFFFFF` | App background |
| `--bg-surface` | `#12151C` | `#F7F8FA` | Cards, panels |
| `--bg-surface-raised` | `#1A1E28` | `#FFFFFF` (with shadow) | Modals, popovers |
| `--border` | `#242833` | `#E5E7EB` | Dividers, card borders |
| `--text-primary` | `#F5F6F8` | `#0B0D12` | Primary text |
| `--text-secondary` | `#9AA1AF` | `#5B6472` | Secondary text |
| `--accent` | `#5B8DEF` | `#3D6FD6` | Primary actions, links, active nav |
| `--accent-soft` | `#1E2A44` | `#E8EEFC` | Selected states, subtle highlight |
| `--success` | `#3DD68C` | `#1FA968` | Success states |
| `--warning` | `#F5B942` | `#B9860A` | Warnings, cost estimates |
| `--danger` | `#F0587A` | `#D6335A` | Destructive actions, blocked SQL |
| `--chart-1..6` | curated categorical palette, colorblind-safe (e.g. Okabe-Ito derived) | | Chart series |

No pure black backgrounds and no gradients-for-decoration-only ("glassmorphism" as a goal is explicitly rejected here — it's a trend, not a design principle; subtle 1px borders + a restrained shadow scale communicate depth better and age better in a portfolio).

## 9. Component Library Recommendations

- **Base primitives:** Radix UI (unstyled, accessible primitives — dialog, popover, dropdown, tabs) styled with Tailwind. This gives full visual control (needed since "Zero Bootstrap" is a stated goal) while inheriting correct ARIA behavior for free.
- **Charts:** Recharts for standard chart types (bar/line/area/pie) — good React ergonomics, sufficient customization. ECharts only if a specific chart type (e.g. heatmap) genuinely needs it; don't ship two charting libraries without a reason.
- **Code editor:** Monaco Editor (`@monaco-editor/react`).
- **Grid layout:** `react-grid-layout` for dashboard drag/resize.
- **Icons:** Lucide (consistent stroke weight, tree-shakeable).
- **Forms:** React Hook Form + Zod for schema validation, shared validation schemas mirrored (not duplicated) from backend DTO constraints where practical.
- **Type safety substitute (JavaScript, not TypeScript):** since the frontend is plain JavaScript, `prop-types` is required on every shared component to catch prop-shape mistakes at runtime in development, and non-trivial functions/hooks carry JSDoc `@param`/`@returns` annotations so editors still surface type hints via `jsconfig.json`'s `checkJs`. This is a deliberate substitute for compile-time typing, not an omission.

## 10. Responsive Design

- Breakpoints: `sm 640 / md 768 / lg 1024 / xl 1280 / 2xl 1536` (Tailwind defaults — no reason to diverge).
- Sidebar collapses to a bottom tab bar below `md`.
- The three-pane Ask screen and two-pane Editor screen both collapse to a single scrollable column with a sticky tab switcher below `lg`.
- Dashboards are view-only (no drag/resize) below `md` — editing a dense grid layout on a phone is not a good use of engineering time for a resume project's actual audience (evaluators on a laptop).

## 11. Accessibility

- All interactive elements reachable and operable by keyboard; visible focus rings (`--accent` outline, never `outline: none` without a replacement).
- Color is never the only signal — the blocked-SQL indicator uses an icon + text + color, not color alone.
- Charts include a "view as table" toggle for screen-reader and colorblind accessibility.
- Minimum contrast ratio 4.5:1 for body text, checked against both theme palettes above.
- Radix primitives handle correct `aria-*` wiring for modals/menus/tabs by default — this is a major reason to choose them over hand-rolled components.

## 12. Design System Rules

1. No inline hex colors in components — always reference CSS variables/Tailwind theme tokens.
2. Spacing scale is Tailwind's default 4px-based scale; no arbitrary one-off pixel values in class names except for genuinely one-off layout needs (documented with a comment).
3. Every new component starts from a Radix primitive if one exists for that interaction pattern; only build fully custom for genuinely novel UI (e.g. the schema tree explorer).
4. Motion is functional, not decorative: transitions communicate state change (loading → loaded, collapsed → expanded) with a max duration of 200ms ease-out; no animation exists purely for flourish.
5. Every screen has a defined empty, loading, and error state before it's considered "done" — this is part of Definition of Done in `rules.md`.
