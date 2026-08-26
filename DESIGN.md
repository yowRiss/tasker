# Tasker Mobile Design System

## 1. Product direction

Tasker is a private, daily-use productivity app. The native Android interface uses Material 3 with a quiet green accent, warm neutral surfaces, compact information hierarchy, and system light/dark mode. It should feel dependable and easy to scan rather than decorative.

Design dials for utility screens: variance 3/10, motion 2/10, density 6/10. Money screens may be denser than Tasks and Notes because amounts, dates, categories, and account context must remain visible together.

## 2. Tokens

The implementation source of truth is `android/app/src/main/java/com/tasker/android/ui/theme/`.

- Light surfaces: background `#FAFAF9`, surface `#FFFFFF`, alternate surface `#F5F4F2`, border `#E8E6E1`.
- Light text: primary `#1A1916`, secondary `#6B6760`, tertiary `#9C9891`.
- Light accent: green `#4A7C59`, hover `#3D6B4A`, subtle `#EBF2ED`.
- Dark surfaces: background `#141412`, surface `#1E1D1B`, alternate surface `#252422`, border `#2E2C28`.
- Dark text: primary `#F0EDE8`, secondary `#9C9891`, tertiary `#6B6760`.
- Dark accent: green `#5A9B6E`, hover `#4A8A5D`, subtle `#1A2E20`.
- Semantic colors: success for income, destructive for expenses and confirmed deletion, warning for approaching limits. Each has a subtle container token in both modes.
- Typography: Material 3 system sans. Screen title 24sp semibold, section title 16sp medium, body 14sp regular, metadata 12sp regular. Monetary totals use headline styles and monetary rows use title styles to keep digits aligned by weight and size.
- Spacing: 4dp base. Screen gutters 16dp; primary section gap 16dp; related-control gap 8dp; compact metadata gap 4dp.
- Shape: cards 12-16dp, fields and standard buttons 8-12dp, chips use Material pill geometry.
- Elevation: tonal surface separation by default. Use low elevation only for the primary account summary; lists remain flat cards against the background.
- Icons: Material Icons Outlined or Rounded only.
- Motion: navigation transitions only. Filtering and CRUD state changes use Compose state updates without decorative animation.

## 3. Layout and behavior

- Screens are single-column and vertically scrolling on phones. Horizontal chip rows own their horizontal scroll and never force the page wider.
- Primary creation uses one floating action button. Secondary destinations use compact top-bar actions.
- Long finance lists are grouped by date. Each row keeps description, account/category context, and amount visible; long text truncates before the amount.
- Filters appear directly above the content they affect. Active filters use the accent-subtle token and must offer a one-action reset.
- Empty states explain which filters are active and offer a direct way to clear them.
- Destructive actions always require confirmation. Tapping a transaction opens editing; deletion is a distinct trailing action.

## 4. Money semantics

- Income is positive and uses success color with a plus sign.
- Expense is negative and uses destructive color with a minus sign.
- Transfers are neutral because they do not change total net worth; show source and destination accounts.
- Total balance is all-time account net worth. Income, expenses, and net cash flow are scoped to the selected reporting period and account.
- Category charts show expense distribution only and cap the visible list to the five largest categories.
- Budget progress uses accent until the limit is crossed, then destructive. Copy must show both spent and limit values.
- Recurring entries show cadence and next due date, and inactive or deleted entries do not appear in the active list.

## 5. Reusable primitives and states

- `FinanceSummaryCard`: total balance, reporting-period label, income, expenses, and net cash flow. States: populated and zero-value.
- `MoneyFilterBar`: search field plus period, account, type, and category chips. States: default, active filters, and reset available.
- `TransactionRow`: semantic icon, description, context, signed amount, optional receipt marker, edit tap target, and delete action. States: income, expense, transfer, pressed, and pending-delete confirmation.
- `CategorySpendChart`: up to five accessible labeled bars with amount labels. States: populated and omitted when empty.
- `MoneyEmptyState`: short reason plus optional clear-filters action.
- `RecurringTransactionCard`: description/category, signed amount, cadence, due date, and delete action. States: income, expense, and pending-delete confirmation.
- `EntityDialog`: labeled fields, inline validation, cancel, and a disabled confirm action until required values are valid.
- `OfflineStatusBanner`: full-width exception strip above app content. States: offline with locally queued-change count, and sync-error with a direct path to Offline Access. It is hidden when the app is online and healthy so normal screens keep their low-chrome hierarchy.
- `OfflineSummaryCard`: connectivity icon, offline-readiness statement, and short explanation that Room-backed content remains editable. States: offline, online and synced, syncing, pending changes, and failed changes.
- `OfflineContentRow`: icon, locally available content label, safe aggregate count, and supporting description. Used for tasks, notes, and transactions; rows do not expose record titles or queued payload details.
- `TaskViewSelector`: horizontally scrollable List, Board, and Table choices above the task collection. The selected view uses accent-subtle treatment and persists locally across app sessions.
- `TaskKanbanColumn`: fixed-width horizontal board group with title, aggregate count, empty state, and vertically scrolling cards. Mobile moves use a labeled 48dp action instead of requiring precision drag gestures; moving a card updates the same Room-backed task record shown in every view.
- `TaskTableRow`: horizontally scrollable database row with stable Name, Status, Project, Due, and Priority columns. Header and rows share horizontal position; row taps open the same task detail used by List and Board.

All controls retain Material focus, pressed, disabled, and accessibility semantics. Touch targets should be at least 48dp unless a compact icon is nested inside a larger clickable row.

## 6. Accessibility and quality floor

- Use theme semantic tokens in both system modes; do not add raw colors in feature screens.
- Do not rely on color alone: amounts include signs and transaction types use icons/text.
- Every icon button has a content description, and all fields have visible labels.
- Numeric and date validation is inline and does not discard entered values.
- Text remains usable at Android font scaling; flexible columns take remaining width and amounts remain visible.
- Screen behavior must be checked on a phone-size Android surface in light and dark mode before release.
- Offline and sync states use text plus icons and counts rather than color alone. Offline messaging must distinguish “saved on this device” from “synced to the server.”
- Task database views must never duplicate records. A status change made from Board must appear immediately in List, Table, and Calendar through the shared local source of truth.

## 7. Current accepted debt

- Currency is fixed to Indonesian rupiah.
- The Android build and rendered visual checks run in CI/device infrastructure, not on the development host.
- The lowercase `design.md` file is an older redesign prompt and is not the implementation contract; this file is authoritative.
