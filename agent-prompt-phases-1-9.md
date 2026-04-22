# Automemoria — AI Agent Build Prompt
## Phases 1–9: Feature Implementation

---

## Context

You are building **Automemoria**, a private single-user Android productivity app.
Phase 0 (project scaffold) is already complete. The following are fully implemented:

- Jetpack Compose + Material3 dark-first theme
- Room database with all 10 entities and DAOs
- Supabase client (Postgres + Auth + Realtime)
- `HabitRepository` — the reference pattern for all other repositories
- `SyncWorker` (WorkManager) + `NetworkObserver` + `SyncPreferences`
- Hilt DI wired for database, network, and WorkManager
- Bottom navigation with 5 tabs: Home, Habits, Goals, Calendar, Boards
- `HomeScreen`, `HabitListScreen` (with create sheet), `CalendarScreen` (month grid), placeholder screens for Goals / Boards / Notes
- `PersonalAssistantService` + `QuickCaptureActivity` overlay skeleton (Assist API)

### Tech stack
- **Language:** Kotlin 2.x
- **UI:** Jetpack Compose + Material3
- **DI:** Hilt
- **Local DB:** Room (SQLite)
- **Cloud:** Supabase (Postgres + Auth + Realtime) via official Kotlin SDK
- **Background sync:** WorkManager (`SyncWorker`)
- **Settings/state:** DataStore Preferences
- **Async:** Kotlin Coroutines + Flow

### Package
`com.automemoria`

### Architecture pattern
MVVM + Repository. Every feature follows this strict layering:
```
Compose Screen → ViewModel (StateFlow<UiState>) → Repository → Room DAO (primary) + Supabase (sync)
```
**Never read from Supabase in real time in the UI.** Room is always the source of truth. Supabase is the sync target only.

### Sync pattern (follow exactly)
```kotlin
// Every write:
1. Write to Room with syncStatus = PENDING_UPLOAD
2. Return/emit immediately — UI updates from Room Flow
3. SyncWorker picks up PENDING_UPLOAD records and pushes to Supabase
4. On success: update syncStatus = SYNCED

// Every delete:
1. Set deletedAt = now(), syncStatus = PENDING_DELETE in Room
2. Exclude deleted records from all queries using WHERE deletedAt IS NULL
3. SyncWorker soft-deletes on Supabase, then hard-deletes from Room

// Every pull (in SyncWorker):
1. SELECT * FROM supabase_table WHERE updated_at > last_sync_timestamp
2. For each remote record: if remote.updated_at > local.updated_at → upsert into Room
3. Conflict resolution: last-write-wins by updated_at
```

### Domain models
All domain models are in `com.automemoria.domain.model.Models.kt`. Do not modify them unless explicitly asked. Map between Room entities ↔ domain models ↔ Supabase DTOs using extension functions in the repository file.

---

## Design rules
- **Dark-first.** Design all screens for dark mode. Light mode should also work via Material3.
- **Dense layouts.** Inspired by Obsidian — content-dense, not excessive whitespace.
- **No loading spinners unless necessary.** Data comes from Room Flows — it should appear instantly.
- **Animations:** Use `AnimatedContent`, `AnimatedVisibility`, and spring animations. Habit check-in should trigger a brief visual celebration (ripple or scale animation).
- **Haptics:** Use `HapticFeedbackConstants` on drag pick-up/drop in Kanban, and on habit completion.
- **Empty states:** Every list screen must have a well-designed empty state with an icon, message, and CTA button.
- **Error states:** Show a `Snackbar` on sync failure. Never crash on network errors.

---

## Phase 1 — Habits (Full Implementation)

### Deliverables

#### 1.1 `GoalRepository`, `NoteRepository`, `BoardRepository`, `CalendarRepository`
Create all 4 remaining repositories following the **exact same pattern** as `HabitRepository`:
- `observeAll()` returning `Flow<List<DomainModel>>` from Room
- `create(...)`, `update(...)`, `delete(id)` (soft delete)
- `pushPendingToSupabase()` and `pullFromSupabase(since: String)`
- Mapper extension functions: `Entity.toDomain()`, `Entity.toDto()`, `Dto.toEntity()`

Register all repositories in `RepositoryModule` Hilt module (create this file in `di/`).

Update `SyncWorker.doWork()` to call `pushPendingToSupabase()` and `pullFromSupabase()` on all 5 repositories.

#### 1.2 `HabitDetailScreen`
Route: `habit/{habitId}`

Layout (top to bottom):
- `TopAppBar` with back arrow, habit name, edit icon (navigates to `HabitEditorScreen`)
- Large icon bubble + name + frequency badge + color accent
- **Stats row:** 3 chips — "🔥 Current Streak: N", "⚡ Longest: N", "✅ Total: N"
- **Heatmap component** (see 1.3)
- **Recent logs section:** last 7 days shown as a row of day chips (Mon–Sun), tapping toggles completion for that day
- Archive and Delete buttons at the bottom (with confirmation dialog)

#### 1.3 `HabitHeatmap` composable
A GitHub-style contribution heatmap showing 52 weeks × 7 days.

Requirements:
- Render as a `Canvas` or `LazyRow` of week columns
- Each cell = 10dp × 10dp with 2dp gap
- Color scale: `#1a1a1a` (empty) → `#22C55E` at 25%, 50%, 75%, 100% opacity variants
- Cells are colored based on whether the habit was completed that day (from `HabitLog` data)
- Tapping a cell shows a `Tooltip` or `Popup`: "April 20 — Completed"
- Month labels above the grid (Jan, Feb, …)
- Week day labels on the left (M, W, F)
- Query data: `habitLogDao.getForDateRange(habitId, from = now - 365 days, to = now)`

#### 1.4 `HabitEditorScreen`
Route: `habit_editor?id={id}` (id is null for create, provided for edit)

Fields:
- Name (required, `OutlinedTextField`)
- Description (optional, multiline)
- **Icon picker:** scrollable grid of 30 emojis
- **Color picker:** row of 10 preset hex colors rendered as colored circles, tapping selects
- **Frequency selector:** segmented button — Daily / Weekly / Custom
  - If Weekly: show a row of 7 day chips (Mon–Sun), multi-selectable
  - If Custom: show a `Slider` for "every N days" (1–30)
- Target streak (number input, optional)
- Save / Cancel buttons

On save: call `HabitViewModel.save(...)` which calls `createHabit` or `updateHabit` depending on whether an id was provided.

#### 1.5 Streak notification scheduling
In `HabitRepository.createHabit(...)` and `updateHabit(...)`, after saving to Room, schedule a daily `AlarmManager` notification for the habit's reminder time (if set).

Create `HabitReminderScheduler`:
```kotlin
class HabitReminderScheduler @Inject constructor(@ApplicationContext context: Context) {
    fun schedule(habit: Habit) { ... }   // uses AlarmManager.setRepeating
    fun cancel(habitId: String) { ... }
}
```

Notification content: `"[icon] [name] — 🔥 Keep your streak alive!"`
Tapping notification: deep link to `HabitDetailScreen` for that habit.

Also schedule a **streak-at-risk** check at 8 PM daily using a `PeriodicWorkRequest`:
- Query all daily habits not yet completed today
- For each: fire a notification: `"⚠️ [name] — Streak at risk! Log it before midnight"`

#### 1.6 HomeScreen — wire up today's habits
Replace the placeholder `completedToday = false` in `HomeViewModel` with a real query:
- For each active habit, check `habitLogDao.getForDate(habitId, today)?.completed == true`
- Use `combine()` on the habits Flow and a derived logs Flow
- `HabitChip` on the home screen should reflect real completion state and toggle it on tap

---

## Phase 2 — Goals & Progress Tracking

### Deliverables

#### 2.1 `GoalListScreen`
Route: `goals`

Layout:
- `TopAppBar`: "Goals" + filter tabs row below it: **All / Active / Completed / Paused**
- `LazyColumn` of `GoalCard` items filtered by selected tab
- FAB: create new goal

`GoalCard` shows:
- Colored icon bubble + title + status badge
- `LinearProgressIndicator` (0–100% from `goal.progress`)
- Target date chip (e.g. "Due Jun 30") — red if overdue
- Linked habit count chip

#### 2.2 `GoalDetailScreen`
Route: `goal/{goalId}`

Layout:
- Header: large icon, title, description, status badge, target date
- Large circular or linear progress indicator (editable — tap opens a slider dialog)
- **Milestones section:**
  - `LazyColumn` of `MilestoneItem` — each is a checkbox row
  - Checking a milestone recalculates `goal.progress` as `(completedMilestones / totalMilestones) * 100`
  - Swipe to delete a milestone
  - "Add milestone" inline text field at the bottom of the list
- **Linked habits** section: horizontal row of habit chips with their current streaks
- **Linked notes** section: list of note titles that reference this goal
- **Sub-goals** section: list of child goals (same `GoalCard` style, smaller)
- Edit / Archive / Delete buttons

#### 2.3 `GoalEditorScreen`
Route: `goal_editor?id={id}`

Fields:
- Title, description, icon picker, color picker, target date (`DatePickerDialog`)
- Status selector: segmented button
- **Milestone creator:** `TextField` + Add button; renders added milestones as a reorderable list inline
- **Link habits:** button opens a multi-select bottom sheet of all active habits
- **Progress mode toggle:** Manual (slider) vs Auto (from milestones)
- Parent goal selector: optional dropdown of existing goals (for sub-goals)

#### 2.4 `GoalViewModel`
Exposes:
- `observeGoalsByStatus(status)`: `Flow<List<Goal>>`
- `observeGoalWithMilestones(goalId)`: `Flow<Pair<Goal, List<GoalMilestone>>>`
- `createGoal(...)`, `updateGoal(...)`, `deleteGoal(id)`
- `toggleMilestone(milestoneId)` — flips `isCompleted`, recomputes `goal.progress`, saves both
- `updateProgress(goalId, progress: Int)` — manual progress update

#### 2.5 HomeScreen — wire up goals
Replace the goals empty state placeholder with:
- 2–3 `GoalCard` items for active goals sorted by closest target date
- "View all" link to `GoalListScreen`

---

## Phase 3 — Calendar

### Deliverables

#### 3.1 Complete `CalendarScreen`
The month grid is already built. Complete it by:
- Wiring real `CalendarEvent` data from `CalendarRepository`
- Showing **event dots** below each day cell that has events (up to 3 colored dots)
- Scrollable agenda list for the selected day — `EventAgendaItem` rows showing time, title, color bar on left edge
- Swipe left on an agenda item to delete (with undo `Snackbar`)
- Tapping an agenda item opens `EventDetailSheet`

#### 3.2 `EventDetailSheet` (bottom sheet)
Shows:
- Title, description, location
- Start/end datetime formatted nicely
- Color badge, recurrence badge
- Linked goal chip (if any)
- Edit and Delete buttons

#### 3.3 `EventEditorScreen`
Route: `event_editor?id={id}`

Fields:
- Title (required), description, location
- Start datetime picker (`DatePickerDialog` + `TimePickerDialog`)
- End datetime picker (same)
- All-day toggle (hides time pickers when enabled)
- Color picker (10 preset colors)
- **Recurrence selector:** None / Daily / Weekly / Monthly / Yearly
  - If Weekly: show day-of-week multi-select
- Link to goal: optional dropdown
- Save / Cancel

#### 3.4 Recurring event expansion
Events have a `recurrence` JSON field. When querying events for a date range in `CalendarRepository`:
- Fetch all non-deleted events
- For each event with a non-null `recurrence`: expand occurrences within the requested date range using an `RRuleExpander` utility class you create
- Merge expanded occurrences with single events before returning to the ViewModel

`RRuleExpander` must support: `DAILY`, `WEEKLY` (with `BYDAY`), `MONTHLY`, `YEARLY`.

#### 3.5 Event reminder notifications
When an event is created/updated, schedule an `AlarmManager` notification:
- Default: 15 minutes before
- Use the event's configured reminder offset if set
- Notification: `"📅 [title] starts in 15 min — [location]"`
- Cancel the alarm when the event is deleted

---

## Phase 4 — Kanban Board

### Deliverables

#### 4.1 `BoardListScreen` (complete)
Replace the placeholder with:
- `LazyVerticalGrid(columns = GridCells.Fixed(2))` of `BoardCard` items
- `BoardCard`: colored header with icon + title, card count badge, last updated date
- Long-press a board: show context menu (Rename, Change color, Archive, Delete)
- FAB → `CreateBoardDialog` (title + icon + color)

#### 4.2 `BoardScreen`
Route: `board/{boardId}`

Layout:
- `TopAppBar` with board title, back arrow, "Add column" action
- Horizontally scrollable (`LazyRow`) list of `ColumnView` components
- Each `ColumnView`:
  - Fixed width: 280dp
  - Header: column title + card count + "⋯" menu (rename, change color, delete)
  - `LazyColumn` of `CardItem` components
  - "Add card" button at the bottom

#### 4.3 Drag and drop
Use the **`sh.calvin.reorderable`** library (add to `libs.versions.toml`):
- Cards can be dragged within a column to reorder
- Cards can be dragged to another column (cross-column drag)
- On drag start: apply `HapticFeedbackConstants.DRAG_START` haptic
- On drop: apply `HapticFeedbackConstants.GESTURE_END` haptic
- After drop: batch update `sort_order` for all affected cards in Room with `syncStatus = PENDING_UPLOAD`
- Show a subtle drop zone highlight when dragging over a column

#### 4.4 `CardDetailSheet` (bottom sheet)
Opens on card tap. Fields (all editable inline):
- Title (`BasicTextField`, large)
- Description (markdown `TextField`, multiline)
- **Priority selector:** row of 5 chips — None / Low / Medium / High / Urgent
  - Color coded: None=grey, Low=blue, Medium=yellow, High=orange, Urgent=red
- Due date (tappable chip → `DatePickerDialog`; shows red if overdue)
- Labels: `FlowRow` of chips + inline add-chip input
- "Link note" button: opens note picker sheet
- Complete toggle (checkbox)
- Delete button (with confirmation)
- Auto-save on every field change (debounced 800ms)

#### 4.5 `KanbanViewModel`
Exposes:
- `observeBoard(boardId)`: `Flow<Board>`
- `observeColumns(boardId)`: `Flow<List<BoardColumn>>`
- `observeCards(columnId)`: `Flow<List<Card>>`
- `createColumn(boardId, title, color)`
- `createCard(columnId, title)`
- `moveCard(cardId, toColumnId, newSortOrder)`
- `reorderCard(cardId, newSortOrder)` — within same column
- `updateCard(card: Card)`
- `deleteCard(cardId)`
- `deleteColumn(columnId)`

#### 4.6 Default board for Quick Capture
In `SyncPreferences`, after the first board is created, auto-save its ID as `defaultBoardId`.
In `QuickCaptureActivity`, when type = TASK, use `SyncPreferences.getDefaultBoardId()` to find the first column of that board and create a card there.

---

## Phase 5 — Notes (Obsidian-style)

### Deliverables

#### 5.1 `NoteListScreen` (complete)
Replace the placeholder with:
- `SearchBar` (Material3) at the top — live filters via `NoteViewModel.search(query)`
- Sort dropdown: Last modified / Created / Title / Pinned first
- `LazyColumn` of `NoteCard` items
  - Pinned notes appear at top with a 📌 badge
  - Shows: title, first 2 lines of content, tags as chips, last modified time
- Swipe left to delete (soft delete + undo Snackbar)
- FAB → navigate to `NoteEditorScreen`

#### 5.2 `NoteEditorScreen`
Route: `note_editor?id={id}`

Requirements:
- Large `BasicTextField` for title (no label, placeholder "Title")
- Full-screen `BasicTextField` for markdown body with monospace font (`JetBrains Mono`)
- **Markdown toolbar** (persists above keyboard):
  - Bold (`**`), Italic (`_`), H1/H2 (`#`, `##`), Bullet (`-`), Numbered (`1.`), Checkbox (`- [ ]`), Code block (` ``` `), Link inserter (`[[`)
  - Each button wraps selected text or inserts at cursor
- **`[[wikilink]]` autocomplete:**
  - Triggered when user types `[[`
  - Show a `DropdownMenu` or `ExposedDropdownMenu` listing all note titles matching the text after `[[`
  - On select: insert `[[Note Title]]` and close dropdown
- **Toggle button** in toolbar: switch between Edit mode and Preview mode
  - Preview mode: render markdown using `Markwon` (add to `libs.versions.toml`) inside a `AndroidView` composable wrapper
- **Auto-save:** debounce 500ms on any change, call `NoteViewModel.saveNote(...)`
- **Tags input** at the bottom: `FlowRow` of `InputChip` components + inline text input
- On save/navigate away: parse `[[wikilinks]]` from content, resolve to note IDs, upsert `NoteLink` records

#### 5.3 Wikilink parsing
Create `WikilinkParser` utility:
```kotlin
object WikilinkParser {
    private val WIKILINK_REGEX = Regex("\\[\\[([^\\]]+)\\]\\]")

    fun extractTitles(content: String): List<String> =
        WIKILINK_REGEX.findAll(content).map { it.groupValues[1].trim() }.toList()
}
```

In `NoteRepository.saveNote(note)`:
1. Save note to Room
2. Call `WikilinkParser.extractTitles(note.content)`
3. For each title: `noteDao.getByTitle(title)` — if found, create a `NoteLink`
4. Delete all existing `NoteLink` records where `sourceNoteId = note.id`
5. Insert new `NoteLink` records for resolved titles

#### 5.4 `NoteDetailScreen`
Route: `note/{noteId}`

Layout:
- `TopAppBar`: back arrow, title, Edit button, Pin toggle, "⋯" menu (Delete, Link to goal, Link to card)
- Markdown preview (Markwon rendered)
- **Tags section:** horizontal chip row
- **Linked entities section:** chips for linked goal, linked Kanban card
- **Backlinks section** at the bottom:
  - Header: "← Linked here (N)"
  - `LazyColumn` of note titles that link to this note (from `noteLinkDao.observeBacklinks(noteId)`)
  - Tapping a backlink navigates to that note's detail screen

#### 5.5 `NoteViewModel`
Exposes:
- `observeAll()`: `Flow<List<Note>>`
- `search(query: String)`: `Flow<List<Note>>`
- `observeNote(noteId)`: `Flow<Note?>`
- `observeBacklinks(noteId)`: `Flow<List<Note>>` — notes that link TO this note
- `saveNote(note: Note)` — create or update; triggers wikilink parsing
- `deleteNote(noteId)` — soft delete
- `togglePin(noteId)`

---

## Phase 6 — Home Dashboard (Full Assembly)

### Deliverables

#### 6.1 Complete `HomeScreen`

Replace all placeholders with real data. The home screen should load instantly from Room Flows (no spinners).

**Section 1 — Today's Habits strip**
Already partially wired. Complete it:
- Show all active habits ordered by: incomplete first, then completed (greyed + checkmark)
- Each chip shows the habit's streak number
- Completing a habit plays a brief scale + color animation on the chip

**Section 2 — Active Goals**
- Show top 3 goals by closest target date
- Each `GoalCard` shows progress bar + days remaining
- "View all →" TextButton navigates to `GoalListScreen`

**Section 3 — Today's Calendar Events**
- Compact list of events happening today sorted by start time
- Each row: time range + color dot + title
- "No events today" empty state if none
- Tapping an event opens `EventDetailSheet`

**Section 4 — Board Summary**
- Show the default board (from `SyncPreferences.getDefaultBoardId()`)
- Display column names as a horizontal `FlowRow` of chips with card counts
- e.g. "To Do (3) → In Progress (1) → Done (7)"
- Tapping navigates to `BoardScreen`

**Section 5 — Quick Actions row**
Already built. Keep as-is.

#### 6.2 Quick Capture FAB bottom sheet
Replace the FAB `onClick` stub with a fully functional `ModalBottomSheet`:

```
QuickCaptureSheet(
    onSaveNote(title) → NoteRepository.createNote(title = title, content = "")
    onSaveTask(title) → CardRepository.createCard(columnId = defaultColumnId, title = title)
    onSaveEvent(title) → CalendarRepository.createEvent(title = title, startTime = now)
    onLogHabit() → open a mini HabitSelector sheet listing today's uncompleted habits
)
```

The sheet must open in < 200ms (pre-loaded, not lazy). Auto-focus the text input.

#### 6.3 Sync status indicator
In the `TopAppBar` actions of `HomeScreen`:
- **Cloud + spinner icon** when `SyncWorker` is `RUNNING`
- **Cloud done icon** (grey) when last sync succeeded
- **Cloud off icon** (red) when offline
- **Cloud error icon** (amber) when last sync failed

Observe `WorkManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME_PERIODIC)` and convert to `SyncIndicator` enum in `HomeViewModel`.

---

## Phase 7 — Android Assist API (Full Implementation)

### Deliverables

#### 7.1 Complete `QuickCaptureActivity`

Wire up all 4 capture types with real repositories (inject via Hilt):

**Note:**
```kotlin
noteRepository.createNote(title = text, content = "")
// Then navigate to NoteEditorScreen via deep link intent
```

**Task:**
```kotlin
val defaultBoardId = syncPreferences.getDefaultBoardId()
val firstColumn = boardColumnDao.getFirstColumnForBoard(defaultBoardId)
cardRepository.createCard(columnId = firstColumn.id, title = text)
```

**Habit Log:**
- Instead of a text input, show a `LazyColumn` of today's **uncompleted** habits
- Each row: habit icon + name + current streak
- Tapping a row calls `habitRepository.toggleHabitCompletion(habitId)`
- Show a ✅ checkmark animation on the row, then auto-dismiss after 1 second

**Event:**
```kotlin
calendarRepository.createEvent(
    title = text,
    startTime = LocalDateTime.now().plusHours(1).withMinute(0),
    endTime = LocalDateTime.now().plusHours(2).withMinute(0)
)
```

#### 7.2 "Expand" button
Each capture type has an "Expand" button that opens the full editor for that type:
- Note → `NoteEditorScreen` with the text pre-filled as title
- Task → `CardDetailSheet` for the newly created card
- Event → `EventEditorScreen` with title pre-filled
- Habit → `HabitListScreen`

Use `Intent` with deep link flags to open the main app at the right screen.

#### 7.3 Setup instructions screen
Add a `SetupAssistantScreen` accessible from Settings. It shows step-by-step instructions:
1. Screenshot showing where to go in Android Settings
2. "Open Assistant Settings" button — fires `Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)`
3. Status check: shows ✅ if this app is already set as default assistant (check via `VoiceInteractionService`)

---

## Phase 8 — Sync Engine (Production Quality)

### Deliverables

#### 8.1 Full bidirectional sync for all entities
Update `SyncWorker.doWork()` to process all 5 repositories in dependency order (parents before children):
```
1. habits
2. habit_logs
3. goals → goal_milestones
4. calendar_events
5. boards → board_columns → cards
6. notes → note_links
```

#### 8.2 Sync Manager
Create `SyncManager` as a singleton:
```kotlin
@Singleton
class SyncManager @Inject constructor(
    private val workManager: WorkManager,
    private val syncPreferences: SyncPreferences,
    private val networkObserver: NetworkObserver
) {
    val syncState: StateFlow<SyncState> // IDLE | SYNCING | SUCCESS | ERROR
    fun triggerSync() // enqueues SyncWorker immediately
    fun schedulePeriodic() // registers the 15-min periodic work
}
```

Observe `SyncManager.syncState` in `HomeViewModel` to drive the sync indicator.

#### 8.3 Conflict resolution improvements
In each repository's `pullFromSupabase()`, handle the edge case where a record was deleted locally (`PENDING_DELETE`) but also updated remotely:
- If local `syncStatus == PENDING_DELETE`: skip the remote update (local delete wins)
- Log the conflict via `Timber.w()`

#### 8.4 First-launch full sync
On first launch (detect via `syncPreferences.getLastSyncTimestamp() == "1970-01-01T00:00:00"`):
- Show a one-time "Syncing your data..." overlay on the Home screen
- Pull all records from Supabase (the epoch timestamp pulls everything)
- Dismiss overlay when sync completes
- This covers the case of reinstalling the app or setting up on a new device

#### 8.5 Retry and error handling
- On `SyncWorker` failure: use exponential backoff (already configured in Phase 0's `setBackoffCriteria`)
- Store last sync error message in `SyncPreferences`
- Show the error in Settings screen with a "Retry now" button

---

## Phase 9 — Polish, Notifications, Widgets

### Deliverables

#### 9.1 Notification system (complete)

Create `NotificationHelper`:
```kotlin
@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext context: Context) {
    fun createChannels() // call in Application.onCreate()
    fun sendHabitReminder(habit: Habit)
    fun sendStreakAtRisk(habit: Habit, currentStreak: Int)
    fun sendGoalDeadlineAlert(goal: Goal, daysRemaining: Int)
    fun sendEventReminder(event: CalendarEvent)
}
```

**Channels to create:**
| Channel ID | Name | Importance |
|---|---|---|
| `habits` | Habit Reminders | DEFAULT |
| `streaks` | Streak Alerts | DEFAULT |
| `goals` | Goal Deadlines | DEFAULT |
| `events` | Calendar Events | HIGH |

**Notification taps must deep-link into the app:**
- Habit reminder → `HabitDetailScreen` for that habit
- Streak at risk → `HabitDetailScreen` with completion action as `PendingIntent`
- Goal deadline → `GoalDetailScreen`
- Event reminder → `CalendarScreen` scrolled to that date

**Mark-complete action on habit streak notification:**
Add a `PendingIntent` action button "✅ Done" to the streak-at-risk notification that calls a `BroadcastReceiver` which calls `habitRepository.toggleHabitCompletion(habitId)` directly — without opening the app.

#### 9.2 Home screen widget

Create a `HabitStreakWidget` using **Jetpack Glance** (add to `libs.versions.toml`):

Widget layout:
- App name header
- Today's date
- List of up to 5 active habits with: icon, name, streak count, check button
- Tapping the check button marks the habit complete (via `GlanceAppWidgetReceiver`)
- Tapping the widget body opens the app's Habits tab

Register in `AndroidManifest.xml` as an `AppWidgetProvider`.

#### 9.3 Animations and micro-interactions

Apply these animations throughout the app:

**Habit completion:**
```kotlin
// On HabitChip toggle to completed:
// 1. Scale from 1f → 1.15f → 1f (spring, 200ms)
// 2. Background color animates from surfaceVariant → accentColor.copy(alpha=0.2f)
// 3. Play haptic: HapticFeedbackConstants.VIRTUAL_KEY
```

**Kanban card drag:**
```kotlin
// On drag start: scale card to 1.05f, elevation +8dp, haptic DRAG_START
// While dragging: draw card with 80% opacity
// On drop: spring back to 1f, haptic GESTURE_END
```

**Note save indicator:**
```kotlin
// After auto-save debounce fires: briefly show "Saved" text next to title
// Animate: fadeIn → wait 1s → fadeOut
```

**Screen transitions:**
Ensure all `NavHost` transitions are consistent: `fadeIn + slideInHorizontally { it/4 }` on enter, reverse on exit.

#### 9.4 Note graph view (stretch goal)

If time permits, implement a force-directed graph of notes:

- Use `Canvas` in Compose
- Nodes: circles with note title text, colored by tag
- Edges: lines between `NoteLink` source and target
- **Layout algorithm:** simple spring/force-directed:
  - Repulsion between all node pairs: `F = k / distance²`
  - Attraction along edges: `F = k * distance`
  - Run for N iterations in a `LaunchedEffect`, updating node positions in a `MutableState`
- **Interaction:** pinch-to-zoom (`detectTransformGestures`), pan, tap a node to navigate to that note
- Limit to 100 nodes max; show a message if more exist

---

## General instructions for all phases

1. **Never break existing code.** Each phase builds on the previous. Run the app mentally before submitting.

2. **Follow the existing file structure exactly:**
   ```
   ui/{feature}/   → XxxScreen.kt, XxxViewModel.kt
   data/repository/  → XxxRepository.kt (with all mappers inside)
   ```

3. **Every new screen needs:**
   - A route defined in `Screen` sealed class in `AppNavHost.kt`
   - A `composable(Screen.Xxx.route) { XxxScreen(navController) }` entry in `NavHost`
   - `@AndroidEntryPoint` on ViewModel if needed

4. **Every new repository needs:**
   - `@Singleton` + `@Inject constructor`
   - Provided in a Hilt `@Module` `@InstallIn(SingletonComponent::class)`
   - Called in `SyncWorker`

5. **Supabase table names match exactly:**
   `habits`, `habit_logs`, `goals`, `goal_milestones`, `calendar_events`, `boards`, `board_columns`, `cards`, `notes`, `note_links`

6. **All IDs are UUIDs** generated on device with `UUID.randomUUID().toString()`.

7. **All timestamps are ISO-8601 strings** stored as `String` in Room and matched as `timestamptz` in Supabase. Use `LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)` for writes.

8. **Request `POST_NOTIFICATIONS` permission** at runtime before scheduling any notification (Android 13+). Use `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.

9. **Test the sync pipeline** after each phase by creating a record on device, killing the app, opening Supabase dashboard, and confirming the row appeared.

10. **Commit after each phase** with message format: `feat(phase-N): <description>`
