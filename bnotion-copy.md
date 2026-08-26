# BNotion: Notion Core Feature Analysis

> Product teardown and implementation reference for a Notion-style application.
>
> Research date: 2026-08-26  
> Scope: Notion's durable, user-facing core and the product layers built on it. This is not a pixel-by-pixel UI specification, a pricing comparison, or a claim that every Enterprise administration option must be cloned.

## Contents

1. [Executive summary](#1-executive-summary)
2. [Product principles](#2-product-principles-inferred-from-the-feature-set)
3. [Core object model](#3-core-object-model)
4. [Application shell and navigation](#4-application-shell-and-navigation)
5. [Documents and knowledge authoring](#5-documents-and-knowledge-authoring)
6. [Databases](#6-databases-the-second-foundational-system)
7. [Tasks, projects, sprints, and goals](#7-tasks-projects-sprints-and-goals)
8. [Wikis and company knowledge](#8-wikis-and-company-knowledge)
9. [Collaboration](#9-collaboration)
10. [Forms, sites, buttons, and automations](#10-forms-sites-buttons-and-automations)
11. [Notion AI and Agent](#11-notion-ai-and-agent-layer)
12. [Integrations and API](#12-integrations-and-api)
13. [Notion Calendar and adjacent products](#13-notion-calendar-and-adjacent-products)
14. [Offline, platform, and sync](#14-offline-platform-and-sync-behavior)
15. [Permissions, security, and administration](#15-permissions-security-and-administration)
16. [Notion-defining UX details](#16-ux-details-that-make-the-product-feel-like-notion)
17. [Suggested implementation model](#17-suggested-bnotion-implementation-model)
18. [Clone scope and delivery priorities](#18-clone-scope-and-delivery-priorities)
19. [Acceptance checklist](#19-acceptance-checklist-by-feature-family)
20. [Risks and complexity traps](#20-risks-and-complexity-traps)
21. [Final product conclusion](#21-final-product-conclusion)
22. [Primary official sources](#22-primary-official-sources)

## 1. Executive summary

Notion is best understood as a **composable workspace**, not as a collection of unrelated note, task, wiki, and project-management tools. Four primitives create most of the product:

1. A **workspace** contains people, teamspaces, pages, settings, and integrations.
2. A **page** is both a document and a nestable container.
3. A **block** is the smallest editable content unit inside a page.
4. A **database** is a structured collection in which every record is also a page.

This produces Notion's main differentiator: the same content can behave like a document, a task, a project, a wiki article, a form response, a calendar event, or a public webpage without leaving one shared content model. Views, relations, templates, permissions, automations, and AI add behavior around those primitives.

For BNotion, the correct architectural sequence is therefore:

`workspace → page tree → block editor → database/page unification → views → collaboration → workflow layers`

Building separate hard-coded Notes, Tasks, Projects, and Wikis modules first would reproduce the surface categories but miss Notion's core. Those experiences should be configurations of the same page/database system wherever possible.

## 2. Product principles inferred from the feature set

### 2.1 Progressive disclosure

A blank page begins as a quiet writing surface. Advanced capability appears through `/` commands, block handles, property menus, database view settings, and page actions. A first-time user can type immediately, while an expert can construct a relational system.

### 2.2 One object, multiple representations

A database item is not a thin row that points to a separate document type; it is a page. The same items can be rendered as a table, board, timeline, calendar, list, gallery, chart, map, feed, dashboard, or form-backed response collection. Each view stores presentation and query configuration without duplicating the underlying records.

### 2.3 Everything can be moved, linked, or nested

Pages nest under pages. Blocks move within and between pages. Database views can appear in other pages. Relations connect records. Mentions and backlinks connect knowledge. This makes information spatially flexible without losing identity.

### 2.4 Structure is optional, then incremental

A user can start with text and checkboxes, promote content into a page, add properties, move pages into databases, relate databases, and automate the resulting workflow. Notion avoids requiring a complete schema before capture.

### 2.5 Collaboration is attached to content

Sharing, comments, mentions, suggestions, presence, notifications, history, and permissions operate on pages, blocks, or database entries. Collaboration is not a separate chat module.

### 2.6 Keyboard and direct manipulation coexist

Slash commands, Markdown-style shortcuts, search shortcuts, quick-find, tab/shift-tab indentation, and command menus support speed. Drag handles, drop guides, resizable columns, board-card dragging, and peek panels support visual manipulation.

## 3. Core object model

### 3.1 Workspace

A workspace is the top-level tenancy and collaboration boundary. It owns:

- Members, guests, groups, workspace owners, and roles.
- Teamspaces and private/shared content.
- Pages, databases, templates, integrations, connected apps, and AI configuration.
- Plan limits, billing, security rules, export controls, domain rules, and audit features.
- Global navigation surfaces such as Home, Search, Inbox, Templates, Settings, and Trash.

Key behavior:

- Users can belong to multiple workspaces and switch between them.
- Content identity and permissions are workspace-scoped.
- A guest is invited to specific content; a member participates at workspace/teamspace level.
- Workspace configuration can constrain public sharing, exporting, integrations, guests, AI, and other capabilities.

### 3.2 Teamspace

A teamspace is a major shared-content area inside a workspace. It provides a recognizable home for a team or function and can be open, closed, or private depending on plan and configuration.

Core behavior:

- Has a name, icon, description, members, owners, and default permissions.
- Contains a hierarchy of pages.
- Can appear expanded or collapsed in the sidebar.
- Supplies inherited access to content nested within it.
- Lets large workspaces avoid putting every shared page into one undifferentiated tree.

For a personal-first BNotion, teamspaces are not an MVP requirement. A single workspace with `Private` and `Shared` sections is enough until multi-user collaboration is real.

### 3.3 Page

A page is Notion's universal container. It can be:

- A standalone document.
- A nested subpage.
- A database itself.
- An item inside a database.
- A wiki root or wiki article.
- A project, task, meeting note, form response, or published site page.

Typical page metadata and controls:

- Stable ID and URL.
- Title.
- Optional emoji, icon, or uploaded icon.
- Optional cover image with repositioning.
- Parent location and breadcrumbs.
- Created/updated timestamps and actors.
- Favorite state.
- Lock state.
- Sharing and permission state.
- Discussion, comments, and page activity.
- Trash/deletion state and version history.
- Optional database properties when the page is a record.

Page layout behaviors:

- Default, serif, or monospace typography.
- Normal or small text.
- Normal or full-width content.
- Nested pages with arbitrary depth.
- Open database items in side peek, center peek, or full page.
- Duplicate, move, export, import into, copy link, lock, delete, and restore.

The important implementation rule is that a page keeps the same identity when moved or displayed in a different context. Links and relations must not be based on its path in the sidebar.

### 3.4 Block

Every piece of page content is a block. Official documentation describes text, images, database content, and other content as blocks, manipulated with the `+` menu, the `⋮⋮` block handle, and `/` commands ([writing and editing](https://www.notion.com/help/writing-and-editing-basics)).

A block needs:

- Stable ID.
- Page or parent-block ID.
- Type.
- Type-specific payload.
- Sort position among siblings.
- Optional nested children.
- Rich-text spans and annotations where applicable.
- Created/updated metadata.
- Comment anchors and linkability.
- Permission behavior inherited from its containing page, except special source-content cases such as synced blocks.

#### Basic text and structure blocks

- Paragraph/text.
- Heading levels 1–3, optionally collapsible.
- Bulleted list item.
- Numbered list item.
- To-do item with checkbox.
- Toggle block with nested content.
- Quote.
- Callout with icon and background.
- Divider.
- Page/subpage.
- Table of contents.
- Breadcrumb.
- Simple table with rows and columns.
- Column list and columns.

#### Technical and expressive blocks

- Code block with syntax language and optional caption.
- Inline code.
- Inline and block math/equations.
- Template/button-style action blocks.
- AI output/custom AI blocks where available.

#### Media and external-content blocks

- Image, video, audio, and file.
- PDF preview.
- Web bookmark.
- Embed for supported external services.
- Link preview.
- Uploaded or externally hosted media with caption and display options.

#### Database and workflow blocks

- Inline database.
- Linked database/data-source view.
- Synced block.
- Button.
- Form/database view embeds.
- Charts and other database visualization blocks/views.

#### Block operations

Every block type should participate in a common operation model:

- Create from `+`, slash command, paste, keyboard shortcut, or conversion.
- Select one or many blocks.
- Drag and drop with visible insertion guides.
- Indent/outdent to change nesting.
- Move to another page.
- Duplicate, delete, or restore through page history.
- Turn into a compatible type without recreating content.
- Copy a stable deep link to the block.
- Apply text color or background where supported.
- Comment on or suggest changes to content.
- Ask AI to transform or act on the selection where enabled.

This common protocol matters more than the number of block types. BNotion should not implement each block as an isolated editor widget with incompatible selection, movement, undo, or serialization behavior.

### 3.5 Rich text

Text inside many blocks supports span-level annotations:

- Bold, italic, underline, strikethrough, and inline code.
- Text and background colors.
- Links.
- User, page, database, and date mentions.
- Inline equations.
- Comments or suggestions anchored to ranges.

Expected editor behavior includes selection across compatible blocks, clipboard interoperability, undo/redo, Markdown-style input shortcuts, link pasting, and automatic list continuation.

### 3.6 Page hierarchy and graph

Notion combines a tree and a graph:

- The sidebar and breadcrumbs expose the **tree**: every ordinary page has a parent location.
- Links, mentions, backlinks, relations, and synced blocks create the **graph**.

Required behaviors:

- Sidebar drag-and-drop reorders or reparents pages.
- A page can contain subpages and inline databases.
- `@` mentions can refer to pages or people.
- A page shows backlinks or incoming references.
- Copying a page link preserves navigation independent of sidebar placement.
- Moving a parent carries its descendants but does not break stable links.

## 4. Application shell and navigation

### 4.1 Left sidebar

The sidebar is the workspace map and primary navigation surface. Its functional groups include:

- Workspace switcher/account entry.
- Home.
- Search/command search.
- Inbox/updates.
- Favorites.
- Teamspaces or shared sections.
- Private pages.
- Nested page/database tree with disclosure controls.
- Templates.
- Settings.
- Trash.
- New page action.
- App links such as Calendar and, historically, Mail.

Interaction expectations:

- Collapsible on desktop and drawer-based on mobile.
- Resizable on desktop.
- Remembers expanded/collapsed page branches.
- Supports hover actions for add, more-menu, and drag handles.
- Supports page reordering and nesting through drag-and-drop.
- Indicates the current page and unsynced/offline states without overwhelming the tree.

### 4.2 Top page bar

The page bar typically supplies:

- Breadcrumb/location.
- Current viewers/presence.
- Last-edited or save/sync feedback.
- Share control.
- Favorite toggle.
- Comments/activity entry.
- Page actions menu.

Database pages add view switching and database configuration close to the content rather than in a separate administration screen.

### 4.3 Home

Home is a personalized starting surface rather than another content hierarchy. It may surface:

- Recently visited pages.
- Favorites.
- Suggested or popular content.
- Upcoming events.
- My tasks across compatible task databases.
- Search and Notion AI entry points.

The value is cross-workspace retrieval: users should not have to remember where every active item lives.

### 4.4 Search and command search

Notion search is both retrieval and navigation. Official behavior includes `cmd/ctrl + P` (or `cmd/ctrl + K` when focus allows), exact phrases in quotes, recent pages, and ranked labels such as frequently viewed/popular content ([workspace search](https://www.notion.com/help/search)).

Core features:

- Search page titles and page content.
- Search database records because they are pages.
- Fast recent-page access before typing.
- Filters for title, creator, teamspace, date, and other available facets.
- Keyboard selection and open-in-new-tab behavior.
- Ranking informed by recency, popularity, and user history.
- Optional AI search across workspace, connected apps, and the web.
- Permission-aware indexing: inaccessible content must never leak through titles, snippets, counts, or AI answers.

### 4.5 Inbox and notifications

Inbox centralizes actions that require attention:

- Mentions.
- Replies and comment activity.
- Page invitations.
- Permission requests and decisions.
- Reminders.
- Assignments and relevant database changes.
- Verification expiry/ownership notifications.

Notifications should be grouped, markable as read, link to exact content, and respect per-user preferences. Email/push delivery is a channel on top of the same notification event, not a separate source of truth.

## 5. Documents and knowledge authoring

### 5.1 Document creation

Users can create a page from:

- A blank page.
- A built-in or workspace template.
- A duplicated page/database.
- An imported document or dataset.
- A database template.
- A form submission.
- AI-generated structure.
- A Calendar meeting-note flow.

Creation should feel instant: create the page shell, focus the title or first block, then sync asynchronously with clear failure handling.

### 5.2 Page templates

Templates package useful starting structure:

- Normal page templates can be duplicated from a gallery or workspace collection.
- Database templates prefill properties and page blocks for new records.
- A database can have multiple templates and a default template.
- Repeating templates can create records on schedules where available.

Templates should copy structure while generating new page/block IDs. Relations or references need explicit copy semantics so duplication does not accidentally share mutable content.

### 5.3 Links, mentions, and backlinks

Core knowledge-network behavior:

- Paste or create normal hyperlinks.
- Mention a page inline with its current title and icon.
- Mention a person to notify them.
- Mention dates with reminder behavior.
- Copy page and block links.
- Surface incoming backlinks on the referenced page.
- Show link previews for supported external content.

### 5.4 Synced blocks

A synced block keeps one canonical block subtree rendered in multiple locations. Editing any instance updates all instances; viewers need access to the original content ([synced blocks](https://www.notion.com/help/synced-blocks)).

Implementation implications:

- Copies should reference a canonical source rather than clone payloads.
- The UI must distinguish synced content and expose source/copy locations.
- Unsyncing creates independent local content with new ownership semantics.
- Source deletion and permission changes require deliberate behavior.

This is valuable but not required for the first BNotion editor milestone.

### 5.5 Import, export, trash, and history

Official imports include text/Markdown, Word, CSV, HTML, PDF, ZIP, and migrations from several other products ([import data](https://www.notion.com/help/import-data-into-notion)). Core lifecycle capabilities are:

- Import supported documents into pages and CSV-like data into databases.
- Export a page or workspace in supported formats such as Markdown/CSV, HTML, or PDF, subject to settings.
- Move deleted pages to Trash rather than immediately hard-delete.
- Restore or permanently delete trash content according to policy.
- Keep version history by plan/retention level.
- Restore a previous page version.
- Preserve authorship and edit metadata where possible.

For BNotion, Markdown import/export and recoverable trash should precede broad third-party importers.

## 6. Databases: the second foundational system

Notion describes databases as collections of pages: every item is a page, and properties add structure to those pages ([database introduction](https://www.notion.com/help/intro-to-databases)). This is the most important feature after the block editor.

### 6.1 Database and data-source concepts

A database needs:

- Stable database ID.
- One or more underlying data sources where the current product exposes them.
- Property schema.
- Record/page collection.
- Saved views.
- Database templates.
- Layout settings for record pages.
- Permission, lock, automation, and integration settings.

Presentation modes:

- **Full-page database:** occupies its own page and appears in navigation.
- **Inline database:** rendered inside another page.
- **Linked view:** renders the same source records elsewhere with an independent view configuration.

Linked views are essential. They let a project page show only its related tasks without duplicating the task database.

### 6.2 Property types

Core schema property types include:

- Title: the page's primary name; exactly one title property.
- Rich text.
- Number with display formats such as currency and percent.
- Select.
- Multi-select.
- Status with grouped workflow states.
- Date/date range with optional time, timezone, and reminder.
- Person or group.
- Files and media.
- Checkbox.
- URL.
- Email.
- Phone.
- Formula.
- Relation.
- Rollup.
- Created time.
- Created by.
- Last edited time.
- Last edited by.
- Button.
- Auto-generated ID.
- Place/location where supported.
- Verification and owner fields for knowledge workflows where supported.
- AI autofill-generated content where enabled.

The official property reference confirms relations, rollups, people, files, formula-adjacent metadata, buttons, IDs, place fields, and automatic audit properties ([database properties](https://www.notion.com/help/database-properties)).

Property rules:

- Schema changes apply to every record in the source.
- Views decide which properties are visible and how they are ordered.
- Records may have empty values without changing schema.
- Select/status options have stable identity, labels, colors, and ordering.
- Property edits participate in permissions, history, notifications, and automations.
- Formula/rollup values are derived and not edited directly.

### 6.3 Relations and rollups

Relations connect pages in one database to pages in the same or another database. They may be one-way or surfaced reciprocally. Rollups aggregate a property through a relation.

Examples:

- Project ↔ Tasks.
- Company ↔ Contacts.
- Goal ↔ Projects.
- Meeting ↔ Attendees or project.

Rollup examples:

- Count completed tasks.
- Sum transaction amounts.
- Earliest due date.
- Percent checked.
- Show unique related values.

Relations are stable page-ID references, not copied labels. Deleting, hiding, or revoking access to a related record needs permission-safe rendering.

### 6.4 Formulas

Formula properties compute values from record properties and supported functions. A clone-grade formula system needs:

- Typed literals and property references.
- Arithmetic, comparisons, Boolean logic, strings, and dates.
- List/relation operations where supported.
- Clear syntax errors and inline help.
- Deterministic recalculation when dependencies change.
- Cycle detection.
- Correct null/empty semantics.
- Server-authoritative evaluation or an identical shared evaluator.

Formulas are powerful but expensive to reproduce faithfully. They belong after basic relations and simple aggregates.

### 6.5 Views

Notion currently lists table, board, timeline, calendar, list, gallery, dashboards, charts, map, forms, and feed among database view types ([database views](https://www.notion.com/help/category/database-views/all)). The same source records can have many saved views.

Each view stores:

- Name and icon/type.
- Source database/data source.
- Layout configuration.
- Visible properties and ordering.
- Filter expression.
- Sort list.
- Group/subgroup configuration.
- Aggregation/calculation settings.
- Card size, preview, and other type-specific options.
- Page opening mode: side peek, center peek, or full page.

#### Table

- Records are rows; properties are columns.
- Resize, reorder, hide, freeze, and calculate columns where supported.
- Edit cells inline.
- Add records quickly.
- Bulk-select rows and apply actions.

#### Board

- Kanban columns are groups based on a select, status, person, relation, or compatible property.
- Dragging a card between columns changes its grouped property.
- Supports subgroups/swimlanes, hidden groups, card previews, and visible properties.

#### Timeline

- Places records on a horizontal time axis from a date or date range.
- Supports zoom levels and optional grouping.
- Dragging/resizing changes dates.
- Often paired with dependencies and project planning.

#### Calendar

- Places records on dates.
- Month/week-oriented layouts where available.
- Dragging changes date values.
- Opens the underlying record page.
- Distinct from the separate Notion Calendar application.

#### List

- Compact, low-decoration record list.
- Shows selected properties and supports grouping.
- Useful when record content matters more than a spreadsheet grid.

#### Gallery

- Card grid with page cover, page content, or file/image property previews.
- Configurable card size, fit, and property display.

#### Chart

- Aggregates source records into supported chart types.
- Configures axes, grouping, measure, filters, and labels.
- Clicking visual elements should lead back to the source data where possible.

#### Dashboard

- Composes multiple database visualizations and controls into a reporting surface.
- Should reuse saved sources and permission-aware aggregations.

#### Map

- Plots records using place/location data.
- Requires clustering, viewport loading, and privacy-aware geodata handling.

#### Feed

- Presents record/page updates or content in a chronological, readable stream.

#### Form

- Presents selected database properties as a submission form.
- Responses create records in the backing database.
- Can be shared with workspace users or publicly, subject to settings.
- Supports required questions, descriptions, confirmation behavior, and workflow automations.

### 6.6 Filters, sorting, grouping, and calculations

Views apply a query layer to the source:

- One or more filter conditions.
- Nested `AND`/`OR` advanced filters.
- Relative dates and current-user conditions.
- Multiple ordered sorts.
- Grouping and optional subgrouping.
- Hidden empty groups.
- Per-group counts or property calculations.
- Conditional colors without changing the underlying property value.

Filters are view configuration, not access control. A user who can open the underlying database may be able to change or remove a filter unless database permissions prevent structural edits.

### 6.7 Database templates and layouts

Database templates create consistent record content and property defaults. Database layouts determine how properties and content appear when records open. Official layouts can place key properties prominently and organize content into tabs/modules consistently for all records in a database ([database layouts](https://www.notion.com/help/layouts)).

Important separation:

- **Template:** initial content copied when a record is created.
- **Layout:** shared presentation applied when any record is viewed.
- **View:** presentation of the collection of records.

### 6.8 Database permissions and locking

Database permissions need more nuance than ordinary documents:

- Full access.
- Can edit structure and content.
- Can edit content without changing schema, views, filters, sorts, or automations.
- Can create records without necessarily seeing all existing records where supported.
- Can comment.
- Can view.
- Page/record-level rules based on people properties where supported.
- Lock database structure while still allowing permitted record edits.

Notion documents these distinctions and notes that the broadest granted access generally wins ([sharing and permissions](https://www.notion.com/help/sharing-and-permissions)).

## 7. Tasks, projects, sprints, and goals

Notion's project-management experience is primarily an opinionated configuration of databases, relations, templates, views, and Home/My Tasks rather than a separate storage engine.

### 7.1 Task model

A compatible task database normally includes:

- Title.
- Status.
- Assignee/person.
- Due date.
- Project relation.
- Priority.
- Tags/type.
- Sub-items/parent task.
- Dependencies/blocking relations.
- Sprint relation where used.
- Created/updated metadata.
- Free-form page body for requirements, notes, files, and discussion.

Core task operations:

- Create from a database, project page, My Tasks, or template.
- Assign one or more people.
- Change status inline or by dragging board cards.
- Set due date/reminder.
- Filter to current user's tasks.
- Open in peek without losing list context.
- Comment, mention, attach files, and link supporting pages.
- Relate to project, sprint, goal, meeting, or other database content.

### 7.2 My Tasks

My Tasks aggregates assigned work from compatible task databases into one personal view. It depends on recognizing common task properties, respecting permissions, and avoiding record duplication.

Useful behaviors:

- Assigned-to-me filtering.
- Grouping by due date, project, or status.
- Inline completion/status changes.
- Source database/project context.
- Quick task capture to a chosen default task source.

### 7.3 Project model

A project is a database page with structured metadata plus a flexible body:

- Status/health.
- Owner and team.
- Dates/timeline.
- Related tasks.
- Goals/initiative relations.
- Progress derived from tasks or rollups.
- Summary, requirements, decisions, files, and updates in blocks.
- Views of only the tasks related to that project.

This demonstrates the page/database unification: project metadata can be scanned in a portfolio table while the same project opens as a full working document.

### 7.4 Sprints and dependencies

Engineering/project workflows can add:

- Sprint database with start/end dates and status.
- Current/upcoming/completed sprint views.
- Tasks assigned to a sprint.
- Backlog view for tasks without a sprint.
- Dependency properties such as blocked by/blocking.
- Timeline visualization of scheduling conflicts.
- Sprint templates and recurring creation behaviors.

### 7.5 Goals

Goals/OKRs can be another related database:

- Objective title, owner, period, status, and target.
- Relations to projects or key results.
- Rollup-derived progress.
- Portfolio views grouped by team or cycle.

Goals are a workflow template on the relational database engine, not a foundational MVP feature.

## 8. Wikis and company knowledge

A page can be turned into a wiki. Official wiki behavior includes `Home`, `All pages`, and `Pages I own` views, plus page owners and expiring or indefinite verification ([wikis and verified pages](https://www.notion.com/help/wikis-and-verified-pages)).

Core wiki features:

- Curated landing page plus database-like index of descendant knowledge.
- Page ownership.
- Verified/trusted indicator.
- Verification expiry and owner notification.
- Search visibility.
- Multiple views of wiki pages.
- Templates for common knowledge types.
- Comments, permissions, history, and analytics depending on plan.

The product problem solved is knowledge freshness, not simply nesting documents. A BNotion wiki clone is incomplete if it provides folders but no ownership, verification, search, and review lifecycle.

## 9. Collaboration

### 9.1 Sharing model

Content can be shared with:

- Specific people.
- Groups.
- Teamspaces.
- The entire workspace.
- Guests.
- Anyone with a link, if allowed.
- The public as a Notion Site.

Access levels include full access, edit, database content edit, create-only in supported cases, comment, and view ([sharing and permissions](https://www.notion.com/help/sharing-and-permissions)).

Permission behavior:

- Child pages generally inherit parent access.
- Direct grants can broaden access.
- The broadest applicable grant wins.
- Moving a page changes inherited access context and must warn when exposure changes.
- Linked databases and synced blocks must not bypass source permissions.
- Search, mentions, backlinks, notifications, previews, exports, API responses, and AI must all enforce the same effective access.

### 9.2 Real-time co-editing and presence

Core collaboration feedback includes:

- Other viewers' avatars.
- Active versus inactive presence.
- Collaborator cursors or block-level location.
- Near-real-time propagation of text and structural edits.
- Conflict handling for concurrent operations.
- Offline reconciliation with documented limits.

The editor needs an operation/transaction model that handles simultaneous block insertion, deletion, movement, and text changes. Last-write-wins on an entire page body would cause unacceptable data loss.

### 9.3 Comments, discussions, suggestions, and reactions

Notion supports top-level page discussions and inline comments, including mentions, replies, resolution, editing/deletion, and reopening resolved threads ([comments and mentions](https://www.notion.com/help/comments-mentions-and-reminders)).

Core collaboration objects:

- Page discussion thread.
- Inline/block or text-range comment thread.
- Reply.
- Mention and notification.
- Resolved/open state.
- Emoji reaction.
- Suggested edit with accept/reject lifecycle where available.

Comment anchors must survive ordinary edits where possible, or degrade clearly to a block-level anchor.

### 9.4 Reminders

Reminders attach to dates or inline date mentions and generate notifications. They can appear in tasks, database date properties, or ordinary documents. The reminder engine should store a timestamp/timezone and delivery state separately from its visual mention.

## 10. Forms, sites, buttons, and automations

### 10.1 Forms

Forms are a view over a database, not a disconnected survey store.

Core behavior:

- Choose which compatible properties become questions.
- Configure prompt text, descriptions, required state, and ordering.
- Share internally or publicly, subject to workspace policy.
- Accept submissions without exposing other records.
- Create one database record per valid response.
- Show confirmation/close behavior.
- Filter, sort, chart, and automate responses using normal database capabilities.
- Trigger conditional workflow actions where supported.

Official documentation confirms that responses are stored in the backing database and can feed filters, charts, and automations ([Notion Forms](https://www.notion.com/help/forms)).

### 10.2 Notion Sites

Any eligible page can become a public site.

Core behavior:

- Publish/unpublish a page and allowed descendants.
- Public `notion.site` address.
- Search-engine indexing control.
- Page updates reflected on the published site.
- Public navigation and site search where enabled.
- Light/dark/system theme.
- Share preview, title, description, favicon, and navigation customization on supported plans.
- Google Analytics integration and custom-domain add-on on supported plans.

Official site customization covers theme, favicon, header/breadcrumbs, navigation, search, SEO/share metadata, and analytics ([site customization](https://www.notion.com/help/edit-and-customize-your-notion-sites)).

Public-site rendering must strip private controls and enforce source visibility. Publishing a parent should not accidentally expose restricted linked data or private descendants.

### 10.3 Buttons

Buttons give users explicit, one-click workflow actions. Depending on context they can:

- Insert or edit blocks.
- Create pages/records.
- Edit properties.
- Open pages or URLs.
- Send notifications or messages where integrated.
- Trigger webhook actions.
- Execute a sequence of configured actions.

Database button properties apply an action in the context of the current record. Buttons should run with the clicker's permissions, show progress/result feedback, and avoid accidental duplicate execution.

### 10.4 Database automations

Automations are event-triggered workflows over database records.

Common trigger families:

- Record/page added.
- Property changed to or from a condition.
- Scheduled/recurring time.
- Form response added.
- Manual button action.

Common action families:

- Add or edit a record/page.
- Change properties.
- Add content.
- Send a notification or integrated message.
- Send email where supported.
- Call a webhook.

Automations can target an entire database or a qualifying view. They need execution logs, idempotency protection, permission/service identity rules, and loop prevention. Notion notes that automations cannot simply trigger other automations recursively ([automations](https://www.notion.com/help/category/automations)).

### 10.5 Webhook actions

Configured buttons and automations can send HTTP POST requests to external endpoints, enabling no-code/low-code workflows. These are distinct from integration webhooks, which notify API integrations of workspace changes ([webhook actions](https://www.notion.com/help/webhook-actions)).

## 11. Notion AI and Agent layer

AI is integrated across the workspace but remains a layer over the permissioned content model. Official capabilities currently include Notion Agent, Enterprise Search/connectors, Research Mode, AI Meeting Notes, inline writing help, AI blocks, translation, database creation/autofill/formula help, and file analysis/generation ([Notion AI overview](https://www.notion.com/help/notion-ai-faqs)).

### 11.1 Inline writing assistance

- Generate text from a prompt.
- Continue writing.
- Improve, shorten, lengthen, simplify, or change tone.
- Fix spelling and grammar.
- Summarize or extract action items.
- Translate.
- Transform selected blocks while preserving an accept/discard boundary.

### 11.2 Notion Agent

The Agent can use workspace and connected-app context to perform multi-step tasks, including creating and editing pages and databases. A safe clone needs:

- Explicit tool/action boundaries.
- Permission-aware retrieval and mutation.
- Preview/confirmation for destructive or broad edits.
- An audit trail of actions and affected pages.
- Source links for claims and generated research.
- Protection against instructions embedded in untrusted page or web content.

### 11.3 Enterprise Search and connectors

AI search can answer from:

- Permissioned Notion pages and databases.
- Uploaded files.
- Connected applications such as Slack, Google Drive, Microsoft Teams, Jira, GitHub, and others depending on configuration.
- The web when enabled.

The system must filter sources by the requesting user's access before retrieval and generation. A model prompt saying “do not reveal” is not an access-control boundary.

### 11.4 Research Mode

Research Mode handles open-ended analysis and report generation across workspace, connected apps, and the web, with source selection and follow-up questions ([Research Mode](https://www.notion.com/help/research-mode)).

### 11.5 AI Meeting Notes

- Creates or attaches a meeting-note page.
- Captures/transcribes a meeting with consent and platform permissions.
- Produces summaries, decisions, and action items.
- Keeps notes searchable and shareable in the workspace.
- Integrates with calendar events and configurable storage/sharing behavior.

Audio consent, retention, deletion, participant visibility, and jurisdictional privacy rules make this a high-risk later feature, not an editor MVP.

### 11.6 AI for databases

- Generate a database from a request.
- Autofill properties from page content.
- Summarize or classify records.
- Draft formulas and automations.
- Translate/extract structured values.

Generated schema and mutations should be reviewable before being applied broadly.

## 12. Integrations and API

### 12.1 Connections

Users can install and manage connections, then grant them access to specific pages/databases. API connections follow content-sharing principles rather than receiving automatic access to the whole workspace ([manage connections](https://www.notion.com/help/add-and-manage-connections-with-the-api)).

Required concepts:

- Internal integration owned by one workspace.
- Public OAuth integration installable by other workspaces.
- Granular capabilities/scopes.
- Page/database sharing with a connection.
- Revocation and token rotation.
- Rate limiting and stable object IDs.

### 12.2 Public API

A clone API should expose permission-aware operations for:

- Search.
- Page read/create/update/archive.
- Block read/append/update/delete.
- Database/data-source query and schema access.
- Property values and pagination.
- Users accessible to the integration.
- File upload or file references where supported.

API representation and editor representation should share stable IDs and semantics; building an unrelated “integration database” produces sync inconsistencies.

### 12.3 Integration webhooks

Integration webhooks notify subscribed connections about page/database changes in near real time. Official documentation distinguishes them from user-configured webhook actions ([Notion API connections](https://www.notion.com/help/create-integrations-with-the-notion-api)).

Webhook essentials:

- Signed events.
- Subscription verification.
- Retry with backoff.
- At-least-once delivery and event IDs for deduplication.
- Minimal payloads plus API fetch for current state.
- Permission/revocation handling.

### 12.4 Embeds and link previews

External content can appear as bookmarks, previews, or interactive embeds. A secure implementation needs allowlists or sandboxed frames, strict content security policy, URL validation, and clear fallback when a provider is unavailable.

## 13. Notion Calendar and adjacent products

Notion Calendar is a separate but integrated application. It can attach Notion pages/AI Meeting Notes to events and display/manage database records that have date properties ([Notion Calendar integration](https://www.notion.com/help/use-notion-calendar-with-notion)).

Core Calendar capabilities relevant to a clone:

- Multiple connected calendar accounts.
- Day/week views and timezone-aware scheduling.
- Event creation/editing and conferencing links.
- Availability overlays and scheduling links.
- Upcoming meeting context.
- Attach or create Notion pages for events.
- Display compatible Notion database records alongside calendar events.
- Desktop/mobile notifications and menu-bar quick actions.

Notion Calendar should be considered an adjacent integration, not part of the first page/database engine.

### 13.1 Notion Mail status

Notion Mail offered Gmail-backed inbox views, grouping/filtering/properties, AI assistance, composing, notifications, and Calendar-assisted scheduling. However, Notion's official help center states that Notion Mail will shut down on **2026-09-22** ([Notion Mail](https://www.notion.com/help/get-started-with-notion-mail)).

Therefore:

- Do not treat Mail as a durable Notion core feature.
- Do not include a Mail clone in BNotion scope.
- If email workflows are needed, support email notifications, send-email automation, or an external integration instead.

## 14. Offline, platform, and sync behavior

Notion is available on web, desktop, and mobile, with responsive editing differences. Offline pages are available in desktop/mobile apps, not normal web browsers. Users can manually download pages; paid plans can automatically download recent and favorited pages. For a downloaded database, the first 50 rows of its first view are downloaded by default, and some network-dependent blocks remain unavailable ([offline pages](https://www.notion.com/help/use-pages-offline)).

Core offline behaviors:

- Explicit `Available offline` state per page.
- Device-local download management.
- View/edit/create while offline for supported content.
- Background synchronization after reconnect.
- Visible unavailable state for embeds, AI, forms, buttons, and other online-only blocks.
- Text-conflict reconciliation where possible.
- Clear risk/last-writer behavior for conflicting non-text property edits.
- No false “saved” claim before durable local persistence.

For BNotion, start online-first with local draft recovery. Full multi-device offline collaboration requires a deliberate sync protocol and should not be improvised late.

## 15. Permissions, security, and administration

### 15.1 Effective permission model

Authorization should be calculated from:

- Workspace role.
- Teamspace membership and role.
- Ancestor/inherited grants.
- Direct user/group/guest grants.
- Database-level access.
- Record-level rules where supported.
- Public-link/site access.
- Integration connection grants.
- Workspace security restrictions.

Every read path must use the same effective access rules: UI fetches, search index, backlinks, relation pickers, notifications, exports, API, webhooks, public pages, analytics, and AI retrieval.

### 15.2 Administrative capability

Team/Enterprise administration may include:

- Member, guest, group, and teamspace management.
- Domain claim and controlled workspace joining.
- SSO/SAML and user provisioning/SCIM.
- Audit log.
- Content search/discovery for authorized admins.
- Retention, export, and data-loss controls.
- Public-link, guest, integration, and export restrictions.
- Session/device controls.
- AI and connector controls.
- Page ownership transfer and workspace export.

These features are essential for enterprise Notion but not for a personal BNotion MVP. The architecture should still avoid assumptions that make future multi-tenant authorization impossible.

### 15.3 Safety-critical invariants

- Client-side hiding is never authorization.
- A linked view cannot reveal records inaccessible at the source.
- A relation/rollup cannot reveal inaccessible property values.
- Search snippets and result counts cannot leak private content.
- Public sites/forms run through an explicit public-access path.
- AI retrieval filters sources before sending content to a model.
- File URLs should be short-lived or authorization-checked.
- Page moves that broaden access require a clear warning.
- Exports, webhooks, and integrations are auditable data-egress paths.

## 16. UX details that make the product feel like Notion

Feature parity alone will not create a convincing experience. These interaction qualities are core:

### 16.1 Instant capture

- New page/task creation opens immediately.
- Focus lands in the correct title or content field.
- Enter creates the next logical block/row.
- Optimistic changes provide visible sync state and recover safely on failure.

### 16.2 Context preservation

- Side peek lets users edit a record without losing table/board context.
- Closing a peek restores scroll position and selection.
- Search results open quickly and preserve a sensible back path.
- Breadcrumbs explain location without dominating the editor.

### 16.3 Universal menus

- `/` inserts blocks and actions.
- `@` mentions people, pages, and dates.
- Block handles expose movement and common actions.
- Property menus use consistent select/search/create behavior.
- Command search supports full keyboard use.

### 16.4 Direct manipulation

- Drag blocks, pages, cards, rows, and timeline items.
- Show precise drop targets.
- Update the underlying property when dragging between database groups.
- Provide keyboard alternatives for accessibility.

### 16.5 Low-chrome content surface

- Controls appear on focus or hover.
- Empty space remains usable, not filled with permanent toolbars.
- Database complexity is contained in view/property menus.
- Formatting UI is contextual to selection.

### 16.6 Trustworthy saving

- Distinguish saved, saving, offline-local, retrying, and failed states.
- Keep unsaved edits locally if the network drops.
- Never discard a draft because navigation occurred.
- Undo should cover structural operations, not only text typing.

### 16.7 Responsive behavior

- Desktop sidebar becomes a mobile drawer.
- Peek panels become full-screen sheets/pages.
- Tables can scroll horizontally while key context remains visible.
- Touch targets replace hover-only affordances.
- Block reordering has a touch-accessible alternative.

## 17. Suggested BNotion implementation model

### 17.1 Canonical entities

| Entity | Purpose | Essential relationships |
|---|---|---|
| `workspace` | Tenant and settings boundary | Has users, pages, databases, teamspaces |
| `workspace_user` | Membership and role | User ↔ workspace |
| `teamspace` | Shared navigation/access area | Workspace, members, root pages |
| `page` | Universal document/container/record | Parent page, optional database, blocks |
| `block` | Ordered typed content node | Page or parent block, children |
| `database` | Structured page collection | Schema, records, views, templates |
| `property_definition` | Database field schema | Database/data source |
| `property_value` | Typed record field value | Page/record + property definition |
| `database_view` | Saved query and presentation | Database source, filters, sorts, layout |
| `relation_value` | Stable record-to-record edge | Source page ↔ target page |
| `permission_grant` | Explicit access rule | Principal ↔ content/teamspace |
| `comment_thread` | Page/block/range discussion | Anchor, replies, resolved state |
| `notification` | User attention event | User, actor, target, read state |
| `automation` | Trigger/action workflow | Database/view, executions |
| `file_object` | Private uploaded media | Workspace, owner page/block/property |
| `integration` | External API principal | Workspace install, scopes, grants |
| `page_version` | Recoverable history | Page snapshot/operations, author/time |

### 17.2 Ordering

Use an order representation that supports frequent insertion and movement without rewriting every sibling. Block moves should be atomic: detach from old parent/order and insert into new parent/order in one transaction or one conflict-safe operation.

### 17.3 Typed values

Do not serialize every property into an unvalidated JSON string. Preserve type semantics for dates, numbers, people, relations, files, and derived values so sorting, filtering, authorization, aggregation, and API behavior remain correct.

### 17.4 View configuration

Treat filters, sorts, groups, visible properties, and layout settings as persisted view configuration. Never duplicate database records to create a new view.

### 17.5 Search indexing

Index titles, rich text, block content, and searchable property values. Include workspace and access metadata in the indexing/query pipeline. Recheck authorization at result retrieval even if the index is partitioned.

### 17.6 Event stream

Content mutations should emit durable events used by:

- Real-time clients.
- Search indexing.
- Notifications.
- Automations.
- Webhooks.
- Audit history.
- Analytics.

Avoid making each subsystem infer changes by repeatedly scanning the database.

## 18. Clone scope and delivery priorities

### Tier 0: product spine

These features create a usable Notion-like product and should be built first:

- Authentication and one workspace.
- Stable page tree with private content.
- Page title, icon, breadcrumbs, favorites, move, duplicate, trash, and restore.
- Block editor with paragraph, headings, lists, to-do, toggle, quote, callout, divider, code, image/file, link, and subpage.
- Slash menu, block selection, keyboard navigation, drag/reorder, indent/outdent, undo/redo, and local draft recovery.
- Search across page titles and block text.
- Responsive desktop/mobile shell.

### Tier 1: database foundation

- Database records are pages.
- Title, text, number, select, multi-select, status, date, checkbox, URL, and file properties.
- Full-page, inline, and linked database views.
- Table, board, list, calendar, and gallery views.
- Filters, sorts, grouping, visible-property settings, and peek modes.
- Database templates.

### Tier 2: connected workflows

- People properties and sharing.
- Relations and basic rollups.
- Formula subset.
- Task/project templates, sub-items, dependencies, and My Tasks.
- Comments, mentions, reminders, notifications, and real-time presence.
- Import/export and version history.
- Basic forms and charts.

### Tier 3: platform capabilities

- Teamspaces, groups, guests, granular inherited permissions, and record-level rules.
- Wikis, page owners, and verification.
- Sites and public publishing.
- Buttons and automations.
- API connections, OAuth, webhooks, and integration gallery.
- Robust offline downloads and conflict resolution.
- More views such as timeline, dashboard, map, and feed.

### Tier 4: intelligence and enterprise

- Inline AI and database AI.
- Permission-aware workspace search/RAG.
- Agentic page/database edits.
- Research Mode and connectors.
- AI Meeting Notes.
- SSO/SCIM, audit, retention, DLP-style controls, domain management, and enterprise administration.

### Explicit non-targets

- Pixel-identical copying of Notion's branding or proprietary assets.
- Notion Mail, because it is scheduled to shut down on 2026-09-22.
- Full formula-language parity in the first release.
- Full collaborative offline editing before a conflict-safe sync model exists.
- Enterprise controls before the product supports genuine multi-user tenancy.

## 19. Acceptance checklist by feature family

### Pages and editor

- A user can create, nest, move, link, duplicate, delete, restore, and search pages.
- Blocks retain stable identity through edits and movement.
- Mixed block selections support copy, move, delete, and undo.
- Pasting common rich text produces sensible blocks and annotations.
- Refresh/navigation does not lose acknowledged edits.

### Databases

- Every record opens as an editable page.
- Multiple views show the same records without duplication.
- Editing a view changes only view configuration; editing a record is visible in every view.
- Filters, sorts, groups, and derived values are type-correct.
- Linked views enforce the source database's permissions.

### Collaboration

- Effective permissions are consistent across direct navigation, search, links, relations, API, and public surfaces.
- Concurrent editors do not overwrite the whole page.
- Comments and mentions navigate to the exact target and generate one understandable notification.
- Deleted content can be restored according to retention rules.

### Workflow layers

- Form responses become normal database records.
- Board/timeline/calendar dragging updates the source property atomically.
- Automations have visible execution status and do not loop or run twice silently.
- Public sites never expose non-published or inaccessible linked content.

### AI

- Retrieval is permission-filtered before model access.
- Generated mutations are attributable and reviewable.
- Answers link back to supporting workspace or web sources.
- Destructive/broad actions require explicit confirmation.

## 20. Risks and complexity traps

1. **Building modules instead of primitives.** Separate task/note/wiki schemas create duplication and make cross-view behavior impossible.
2. **Treating a database row as less than a page.** This breaks the central Notion mental model.
3. **Storing pages as one HTML/Markdown blob.** That blocks block links, comments, drag/move, granular collaboration, and structured API access.
4. **Using view filters as permissions.** Users can change views; security must apply to source records.
5. **Retrofitting real-time collaboration.** Stable block IDs, operations, versions, and conflict rules must exist early even if multiplayer ships later.
6. **Underestimating drag-and-drop.** Movement changes hierarchy, ordering, grouped properties, permissions, and undo history.
7. **Implementing formulas too early.** A partial formula engine can consume the schedule before the editor and views are trustworthy.
8. **Letting AI bypass the product model.** AI must call the same permissioned content operations as users and integrations.
9. **Claiming offline support from a cache.** Offline creation/editing requires durable local state, reconciliation, failure UI, and conflict semantics.
10. **Copying visual appearance without interaction fidelity.** Peek panels, keyboard speed, context preservation, instant capture, and trustworthy saving are more important than exact colors.

## 21. Final product conclusion

Notion's core is a small set of recursively composable objects with unusually broad presentation and workflow capabilities:

- Pages provide identity, hierarchy, permissions, and a working surface.
- Blocks provide flexible document structure.
- Databases make pages structured and queryable.
- Views make one dataset useful in many contexts.
- Relations connect work into a graph.
- Collaboration, publishing, automations, integrations, offline sync, and AI operate on the same underlying objects.

The closest BNotion copy will come from reproducing that **coherent content model and interaction grammar**, then using it to assemble notes, tasks, projects, wikis, forms, and sites. A larger checklist of disconnected features would look more complete on paper but behave less like Notion in practice.

## 22. Primary official sources

- [Writing and editing basics](https://www.notion.com/help/writing-and-editing-basics)
- [Pages and blocks](https://www.notion.com/help/category/write-edit-and-customize)
- [Intro to databases](https://www.notion.com/help/intro-to-databases)
- [Database properties](https://www.notion.com/help/database-properties)
- [Database views](https://www.notion.com/help/category/database-views/all)
- [Views, filters, sorts, and groups](https://www.notion.com/help/views-filters-and-sorts)
- [Database page layouts](https://www.notion.com/help/layouts)
- [Sharing and permissions](https://www.notion.com/help/sharing-and-permissions)
- [Comments, mentions, and reminders](https://www.notion.com/help/comments-mentions-and-reminders)
- [Workspace search](https://www.notion.com/help/search)
- [Wikis and verified pages](https://www.notion.com/help/wikis-and-verified-pages)
- [Forms](https://www.notion.com/help/forms)
- [Automations](https://www.notion.com/help/category/automations)
- [Notion Sites customization](https://www.notion.com/help/edit-and-customize-your-notion-sites)
- [Notion AI overview](https://www.notion.com/help/notion-ai-faqs)
- [Research Mode](https://www.notion.com/help/research-mode)
- [Notion Calendar integration](https://www.notion.com/help/use-notion-calendar-with-notion)
- [Offline pages](https://www.notion.com/help/use-pages-offline)
- [Connections and API](https://www.notion.com/help/add-and-manage-connections-with-the-api)
- [Notion Mail shutdown notice](https://www.notion.com/help/get-started-with-notion-mail)

Feature availability varies by plan, workspace role, platform, region, and release timing. The official help center should remain the final reference during implementation.
