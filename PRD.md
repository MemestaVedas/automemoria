# Product Requirements Document (PRD)
## Project: Automemoria — Android App
**Version:** 1.0.0  
**Author:** Personal  
**Status:** Draft  
**Last Updated:** 2026-04-20  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Backend:** Supabase (Postgres + Auth + Realtime + Storage)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Goals & Non-Goals](#2-goals--non-goals)
3. [User Persona](#3-user-persona)
4. [Architecture Overview](#4-architecture-overview)
5. [Data Architecture](#5-data-architecture)
6. [Supabase Schema](#6-supabase-schema)
7. [Android App Architecture](#7-android-app-architecture)
8. [Feature Specifications](#8-feature-specifications)
   - 8.1 [Authentication](#81-authentication)
   - 8.2 [Home Dashboard](#82-home-dashboard)
   - 8.3 [Habit Tracker](#83-habit-tracker)
   - 8.4 [Goal & Progress Tracker](#84-goal--progress-tracker)
   - 8.5 [Calendar](#85-calendar)
   - 8.6 [Kanban Board](#86-kanban-board)
   - 8.7 [Notes (Obsidian-style)](#87-notes-obsidian-style)
   - 8.8 [Android Assistant Integration](#88-android-assistant-integration)
9. [Sync Engine](#9-sync-engine)
10. [Offline-First Strategy](#10-offline-first-strategy)
11. [Notifications & Reminders](#11-notifications--reminders)
12. [UI/UX Guidelines](#12-uiux-guidelines)
13. [Navigation Structure](#13-navigation-structure)
14. [Tech Stack & Libraries](#14-tech-stack--libraries)
15. [Project Structure](#15-project-structure)
16. [Phased Delivery Plan](#16-phased-delivery-plan)
17. [Open Questions](#17-open-questions)

---

## 1. Overview

**Automemoria** is a private, single-user Android productivity application that combines the structured database-style organization of Notion with the local-first, linked-knowledge philosophy of Obsidian. It is not a SaaS product — it is a deeply personal tool built exclusively for one user.

The app consolidates the entire productivity stack into one place: daily habits, long-term goals, a calendar, project management via Kanban boards, and a linked notes system with wiki-style backlinks. It is designed to live on an Android phone as a first-class citizen — summonable at any moment via a long-press of the power button through Android's Assist API.

Data lives primarily on-device using Room (SQLite) and syncs to Supabase when a network connection is available. This means the app is fully functional offline at all times, and the cloud is treated as a sync target, not a dependency.

---

## 2. Goals & Non-Goals

### Goals
- Build a single cohesive Android app that replaces Notion, Obsidian, a habit tracker, a goal tracker, a calendar app, and a Kanban tool
- Offline-first: all reads and writes go to Room first, Supabase second
- Fast to open and capture: accessible via Android Assist (long-press power button) with a quick-capture overlay
- Linked knowledge: notes can reference each other via `[[wikilinks]]` and display backlinks
- Habit streaks and progress visualizations that motivate continued use
- Sync with Supabase for backup, access from web later, and data durability

### Non-Goals (for v1.0)
- No multi-user or sharing features
- No iOS support
- No real-time collaboration
- No plugin system
- No public web access
- No AI/LLM integration (future consideration)
- No file attachments (beyond text and markdown in v1)

---

## 3. User Persona

**The User:** A single advanced developer who wants a unified, personal productivity system that works entirely offline, syncs silently to the cloud, and is optimized for speed of capture and depth of organization. Aesthetics matter. Performance matters. No bloat.

Key behaviors:
- Opens the app multiple times per day for quick check-ins and captures
- Maintains long-running habits (months, years of streak data)
- Uses Kanban-style boards for software projects and personal goals
- Takes linked notes that reference goals, projects, and habits
- Wants the app summonable at any moment without navigating to it

---

## 4. Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                          │
│                                                          │
│  ┌──────────────┐    ┌─────────────────────────────┐    │
│  │  Jetpack      │    │       ViewModel Layer        │    │
│  │  Compose UI   │◄──►│  (StateFlow + UiState)      │    │
│  └──────────────┘    └──────────────┬──────────────┘    │
│                                     │                    │
│                      ┌──────────────▼──────────────┐    │
│                      │       Repository Layer        │    │
│                      │  (Single source of truth)     │    │
│                      └──────────┬──────────┬────────┘    │
│                                 │          │              │
│                    ┌────────────▼──┐  ┌────▼──────────┐  │
│                    │   Room (DB)   │  │  Supabase SDK  │  │
│                    │  Local SQLite │  │  (Sync Target) │  │
│                    └───────────────┘  └───────────────┘  │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │         WorkManager (Background Sync)            │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │     VoiceInteractionService (Assist API)         │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │     Supabase       │
                    │  ┌─────────────┐  │
                    │  │  PostgreSQL  │  │
                    │  ├─────────────┤  │
                    │  │  Auth (JWT) │  │
                    │  ├─────────────┤  │
                    │  │  Realtime   │  │
                    │  ├─────────────┤  │
                    │  │  Storage    │  │
                    │  └─────────────┘  │
                    └───────────────────┘
```

---

## 5. Data Architecture

### Philosophy
- **Room is the primary database.** The app never waits on the network.
- **Supabase is the sync target.** It receives changes asynchronously.
- **Every entity has a UUID** (generated on device) so records can be created offline and synced later without ID conflicts.
- **Every entity has `created_at`, `updated_at`, and `deleted_at` (soft delete) timestamps** for conflict resolution and sync.
- **Conflict resolution strategy (v1):** Last-write-wins based on `updated_at`. The record with the most recent timestamp wins during sync.
- **Sync state:** A `sync_status` enum field on each Room entity tracks whether a record is `PENDING_UPLOAD`, `SYNCED`, or `PENDING_DELETE`.

### Sync Status Enum
```
SYNCED          → record exists in both Room and Supabase, in agreement
PENDING_UPLOAD  → created/modified locally, not yet pushed to Supabase
PENDING_DELETE  → soft-deleted locally, deletion not yet confirmed in Supabase
```

---

## 6. Supabase Schema

All tables include Row Level Security (RLS) policies that restrict access to the single authenticated user.

### `habits`
```sql
create table habits (
  id           uuid primary key,
  name         text not null,
  description  text,
  icon         text,                        -- emoji or icon name
  color        text,                        -- hex color string
  frequency    text not null,               -- 'daily' | 'weekly' | 'custom'
  frequency_days int[],                     -- [1,2,3,4,5] for weekdays
  target_streak int default 0,
  is_archived  boolean default false,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  deleted_at   timestamptz
);
```

### `habit_logs`
```sql
create table habit_logs (
  id           uuid primary key,
  habit_id     uuid references habits(id),
  logged_date  date not null,               -- the calendar date this log is for
  completed    boolean default true,
  note         text,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now()
);
```

### `goals`
```sql
create table goals (
  id           uuid primary key,
  title        text not null,
  description  text,
  icon         text,
  color        text,
  status       text default 'active',       -- 'active' | 'completed' | 'paused' | 'abandoned'
  progress     int default 0,               -- 0–100
  target_date  date,
  parent_id    uuid references goals(id),   -- for sub-goals
  linked_habit_ids uuid[],                  -- habits that contribute to this goal
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  deleted_at   timestamptz
);
```

### `goal_milestones`
```sql
create table goal_milestones (
  id           uuid primary key,
  goal_id      uuid references goals(id),
  title        text not null,
  is_completed boolean default false,
  due_date     date,
  completed_at timestamptz,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now()
);
```

### `calendar_events`
```sql
create table calendar_events (
  id           uuid primary key,
  title        text not null,
  description  text,
  location     text,
  start_time   timestamptz not null,
  end_time     timestamptz,
  is_all_day   boolean default false,
  color        text,
  recurrence   jsonb,                        -- rrule-style recurrence config
  linked_goal_id uuid references goals(id),
  linked_habit_id uuid references habits(id),
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  deleted_at   timestamptz
);
```

### `boards`
```sql
create table boards (
  id           uuid primary key,
  title        text not null,
  description  text,
  color        text,
  icon         text,
  sort_order   int default 0,
  linked_goal_id uuid references goals(id),
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  deleted_at   timestamptz
);
```

### `board_columns`
```sql
create table board_columns (
  id           uuid primary key,
  board_id     uuid references boards(id),
  title        text not null,
  color        text,
  sort_order   int default 0,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now()
);
```

### `cards`
```sql
create table cards (
  id           uuid primary key,
  column_id    uuid references board_columns(id),
  title        text not null,
  description  text,
  priority     text default 'none',          -- 'none' | 'low' | 'medium' | 'high' | 'urgent'
  due_date     date,
  labels       text[],
  sort_order   int default 0,
  is_completed boolean default false,
  linked_note_id uuid,                       -- FK to notes (nullable)
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  deleted_at   timestamptz
);
```

### `notes`
```sql
create table notes (
  id           uuid primary key,
  title        text not null,
  content      text,                         -- raw markdown content
  tags         text[],
  is_pinned    boolean default false,
  linked_goal_id uuid references goals(id),
  linked_card_id uuid references cards(id),
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  deleted_at   timestamptz
);
```

### `note_links`
```sql
create table note_links (
  id           uuid primary key,
  source_note_id uuid references notes(id),
  target_note_id uuid references notes(id),
  created_at   timestamptz default now()
);
```

---

## 7. Android App Architecture

### Pattern: MVVM + Repository + Clean Architecture (simplified)

```
app/
├── ui/                     (Compose screens + ViewModels)
│   ├── home/
│   ├── habits/
│   ├── goals/
│   ├── calendar/
│   ├── kanban/
│   ├── notes/
│   └── assistant/          (Quick capture overlay)
├── data/
│   ├── local/              (Room entities, DAOs, Database)
│   ├── remote/             (Supabase API calls, DTOs)
│   └── repository/         (Combines local + remote)
├── domain/
│   └── model/              (Pure Kotlin data models, no Android deps)
├── sync/
│   └── SyncWorker.kt       (WorkManager background sync)
├── assist/
│   └── AssistantService.kt (VoiceInteractionService)
└── di/
    └── AppModule.kt        (Hilt dependency injection)
```

### Room Database

One `AppDatabase` class with the following entities mirroring the Supabase schema. Each entity adds a `syncStatus: SyncStatus` column not present in Supabase.

```kotlin
@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        GoalEntity::class,
        GoalMilestoneEntity::class,
        CalendarEventEntity::class,
        BoardEntity::class,
        BoardColumnEntity::class,
        CardEntity::class,
        NoteEntity::class,
        NoteLinkEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase()
```

### ViewModels

Each feature module has its own ViewModel that:
- Exposes `StateFlow<UiState>` to the UI
- Calls Repository methods
- Never touches Room or Supabase directly

---

## 8. Feature Specifications

---

### 8.1 Authentication

**Purpose:** Secure the Supabase connection. Since this is a single-user app, auth is a one-time setup, not a recurring login flow.

**Behavior:**
- On first launch, the app shows a simple setup screen: enter Supabase project URL and anon key, then sign in with email/password (pre-created in Supabase dashboard)
- On subsequent launches, the JWT token is refreshed silently using Supabase's session management
- No logout UI is needed in v1 — this is a personal device

**Implementation:**
- Use the official Supabase Kotlin SDK (`io.github.jan-tennert.supabase:gotrue-kt`)
- Store the session token in Android's `EncryptedSharedPreferences`
- If the session is valid, skip the setup screen entirely and go straight to the home dashboard

**Screens:**
- `SetupScreen` — URL + key + email + password fields, "Connect" button
- No dedicated login screen; auth is transparent after first setup

---

### 8.2 Home Dashboard

**Purpose:** The first screen the user sees. A glanceable overview of the day.

**Layout (top to bottom):**
1. **Date header** — "Monday, April 20" in large type
2. **Daily habits strip** — horizontally scrollable row of today's habits as checkable chips
3. **Active goals** — 2–3 goal cards with progress bars, showing % complete and target date
4. **Today's events** — compact list of calendar events for today
5. **Quick capture FAB** — floating action button (bottom right), opens a bottom sheet to quickly create a note, task, habit log, or calendar event
6. **Active boards summary** — cards count per board column for the user's most active Kanban board

**Interactions:**
- Tapping a habit chip marks it as completed for today (or un-completes it)
- Tapping a goal navigates to the Goal Detail screen
- Tapping an event navigates to the Calendar screen at that date
- Long-pressing the FAB shows quick action options

---

### 8.3 Habit Tracker

**Purpose:** Track recurring behaviors with streaks, heatmaps, and completion history.

#### Habit List Screen
- List of all active habits grouped by frequency (Daily / Weekly / Custom)
- Each habit card shows: icon, name, current streak (🔥 N days), today's completion status
- Swipe right to mark complete for today; swipe left to archive
- FAB to create a new habit

#### Habit Detail Screen
- Full habit info: name, description, frequency, color, icon
- **Streak stats:** current streak, longest streak, total completions
- **Completion heatmap:** GitHub-style contribution graph showing the past 12 months, one cell per day, colored by completion status
- **Calendar view:** monthly mini-calendar with completed days highlighted
- **Edit and archive buttons**
- Log section: recent completion notes

#### Create/Edit Habit Screen
- Fields: name (required), description, icon picker (emoji), color picker, frequency selector
- Frequency options: Daily, Specific weekdays (multi-select Mon–Sun), Custom interval (every N days)
- Target streak field (optional motivation goal)
- Save / Cancel

#### Habit Log Logic
- A `HabitLog` record is created for each day a habit is marked complete
- Streak is calculated in the ViewModel by querying consecutive days of logs
- If a daily habit has no log for yesterday, the streak resets to 0 (unless it's a weekly/custom habit — logic adjusts accordingly)

---

### 8.4 Goal & Progress Tracker

**Purpose:** Define high-level goals, track progress, and break them into milestones. Connect goals to habits and calendar events.

#### Goal List Screen
- Filter tabs: All / Active / Completed / Paused
- Each goal card shows: icon, title, progress bar (0–100%), target date, linked habit count
- FAB to create a goal
- Completed goals are moved to the Completed tab automatically when progress hits 100%

#### Goal Detail Screen
- Header: icon, title, description, target date, status badge
- **Progress bar** — large, tapable to manually adjust or auto-calculated from milestones
- **Milestones section** — checklist of sub-tasks; checking off a milestone recalculates progress
- **Linked habits** — list of habits that contribute to this goal; shows their streaks
- **Linked calendar events** — upcoming events tagged to this goal
- **Notes** — list of notes linked to this goal
- **Sub-goals** — nested child goals (1 level deep in v1)
- Edit / Archive / Delete buttons

#### Create/Edit Goal Screen
- Fields: title, description, icon, color, target date, status
- Milestone creator: add/remove/reorder milestones inline
- Link habits: multi-select from existing habits
- Progress mode: Manual (slider) or Auto (derived from milestone completion %)

---

### 8.5 Calendar

**Purpose:** A full-featured event calendar that integrates with goals and habits.

#### Calendar Screen
- **Top section:** Month view with dots below days that have events
- **Bottom section:** Scrollable day agenda for the selected day
- Tap a day to see its events in the agenda
- Swipe left/right to navigate months

#### Event Detail / Create / Edit Sheet
- Fields: title, description, location, start datetime, end datetime, all-day toggle, color, recurrence
- **Recurrence options:** None, Daily, Weekly (select days), Monthly, Custom
- **Link to goal:** optional dropdown to tag the event with a goal
- **Link to habit:** optional, marks a habit completion when the event is completed

#### Views (v1)
- Month view (primary)
- Day/Agenda view (secondary)
- Week view (stretch goal for v1)

#### Behavior
- Events are stored in Room and rendered entirely from local data
- Creating an event on a specific day pre-fills the date
- All-day events appear at the top of the day's agenda
- Recurring events are expanded on read (not stored as multiple rows — one row with recurrence JSON, expanded in the ViewModel)

---

### 8.6 Kanban Board

**Purpose:** Project and task management using a drag-and-drop Kanban interface.

#### Boards List Screen
- Grid of board cards showing: title, icon, color, card count, last updated
- FAB to create a new board
- Long-press a board to archive or delete it

#### Board Screen
- Horizontal scrollable list of columns
- Each column shows its title and a vertical list of cards
- **Drag and drop:** cards can be dragged within a column (reorder) or to another column
- "Add card" button at the bottom of each column
- Column header has a "..." menu: rename, change color, delete
- FAB adds a new column

#### Card Detail Sheet (bottom sheet)
- Title (editable inline)
- Description (markdown, editable)
- Priority selector: None / Low / Medium / High / Urgent (color-coded)
- Due date picker
- Labels (free-text tags, chip input)
- Link to note: opens note picker
- Complete/archive toggle
- Delete button

#### Create/Edit Column
- Simple dialog: column name, color picker

#### Card Logic
- Cards store `sort_order` as integers; reordering updates `sort_order` for affected cards in batch
- Dragging a card to another column updates `column_id` and `sort_order`
- Completed cards can be filtered out or shown dimmed (toggle in board toolbar)

---

### 8.7 Notes (Obsidian-style)

**Purpose:** A linked markdown note-taking system where notes can reference each other via `[[wikilinks]]` and surface backlinks.

#### Notes List Screen
- Search bar (full-text search across title and content)
- Sort options: Last modified, Created, Title, Pinned first
- Each note shows: title, first line of content, tags, last modified date
- Pinned notes appear at the top
- FAB to create a new note
- Swipe to delete (soft delete)

#### Note Editor Screen
- **Toolbar:** Bold, Italic, Heading, Bullet list, Numbered list, Checkbox, `[[Link]]` inserter, Code block
- **Live markdown preview toggle** (edit mode ↔ preview mode)
- **`[[wikilink]]` autocomplete:** when the user types `[[`, show a floating list of existing note titles to select from
- Title field (large, top of screen, separate from body)
- Tags input (chip-style, at the bottom)
- Auto-save on every keystroke with debounce (500ms)

#### Note Detail / Preview Screen
- Rendered markdown
- **Backlinks section** at the bottom: "Notes linking here" — list of notes that `[[link]]` to this note
- **Linked entities:** linked goal, linked Kanban card (if any), shown as chips
- Edit button (opens editor)

#### Wikilink Logic
- When a note is saved, the app parses all `[[Title]]` patterns using a regex
- For each matched title, look up the note by title in Room
- If found, upsert a `NoteLink` record: `(source_id, target_id)`
- When displaying a note, query all `NoteLink` records where `target_note_id = this note's id` to build the backlinks list
- Wikilinks are rendered as tappable inline chips in preview mode

#### Graph View (Stretch Goal v1)
- A force-directed node graph of all notes and their connections
- Nodes = notes, edges = `[[wikilink]]` relationships
- Tap a node to navigate to that note
- Use a lightweight graph rendering approach (Canvas-based in Compose)

---

### 8.8 Android Assistant Integration

**Purpose:** Register the app as the default Android assistant so a long-press of the power button (or home button depending on device) summons a quick-capture overlay immediately.

#### How It Works
- The app implements `VoiceInteractionService` and `VoiceInteractionSession`
- The user sets the app as their Default Digital Assistant in Android Settings → Apps → Default Apps → Digital Assistant
- Long-pressing the power button (Pixel/stock Android) or the home button triggers the assistant
- The app displays a **Quick Capture Bottom Sheet** that slides up over whatever the user is doing

#### Quick Capture Bottom Sheet
This is the most important UX surface in the app. It must open in under 200ms.

**Layout:**
- Large text input at the top ("What's on your mind?")
- Four quick-type buttons below the input: **Note**, **Task**, **Habit Log**, **Event**
- Smart defaults: if no type is selected, the input is saved as a Note
- "Expand" button opens the full editor for the selected type
- "Save" button saves immediately and dismisses

**Quick Capture Behavior by type:**
- **Note:** Creates a new note with the input as the title. Body is empty. Opens note editor on expand.
- **Task:** Creates a new card in the user's default board (configurable in settings), in the first column. Title = input text.
- **Habit Log:** Opens a mini habit selector (list of today's uncompleted habits); tapping one marks it done and dismisses
- **Event:** Creates a calendar event. Input = title, date = today, time = now + 1hr (editable before saving)

#### Implementation Details
```kotlin
// AndroidManifest.xml
<service
    android:name=".assist.PersonalAssistantService"
    android:permission="android.permission.BIND_VOICE_INTERACTION"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.voice.VoiceInteractionService" />
    </intent-filter>
    <meta-data
        android:name="android.voice_interaction_service"
        android:resource="@xml/interaction_service" />
</service>
```

The `VoiceInteractionSession` launches a transparent `Activity` that hosts the Quick Capture bottom sheet in Compose. This keeps the overlay lightweight and dismissible.

---

## 9. Sync Engine

### SyncWorker

A `WorkManager` `CoroutineWorker` that runs:
- On network connectivity restored (triggered by `NetworkCallback`)
- On app foreground (triggered by `ProcessLifecycleOwner`)
- On a periodic schedule: every 15 minutes (minimum WorkManager interval)

### Sync Flow

```
SyncWorker.doWork()
  │
  ├── Pull changes from Supabase
  │     for each table:
  │       SELECT * FROM table WHERE updated_at > last_sync_timestamp
  │       for each remote record:
  │         if exists in Room and remote.updated_at > local.updated_at:
  │           update Room record
  │         elif not exists in Room:
  │           insert into Room
  │
  ├── Push local changes to Supabase
  │     for each table:
  │       SELECT * FROM room_table WHERE sync_status = PENDING_UPLOAD
  │       upsert each record to Supabase (insert or update on conflict by id)
  │       mark record as SYNCED
  │
  ├── Push local deletions to Supabase
  │     SELECT * FROM room_table WHERE sync_status = PENDING_DELETE
  │     SET deleted_at = now() on Supabase record
  │     DELETE from Room (hard delete after confirming remote soft delete)
  │
  └── Update last_sync_timestamp in SharedPreferences
```

### Conflict Resolution

**v1: Last-write-wins by `updated_at`**

- If both local and remote have been modified since the last sync, the record with the higher `updated_at` wins
- The losing record's changes are silently discarded
- This is acceptable for a single-user app where the same data is rarely modified on two devices simultaneously

**Future (v2): Three-way merge for notes** — compare against last known common ancestor to preserve both sides of concurrent edits.

---

## 10. Offline-First Strategy

### Reads
- All reads come from Room exclusively
- The UI never waits for network data
- `Flow<T>` from Room DAOs provides reactive updates — when sync writes new data to Room, the UI updates automatically

### Writes
- Every write goes to Room first with `sync_status = PENDING_UPLOAD`
- The write is considered complete and reflected in the UI immediately
- `SyncWorker` picks up the pending record and pushes it to Supabase asynchronously
- On Supabase success: `sync_status` is updated to `SYNCED`
- On Supabase failure: record stays `PENDING_UPLOAD`, will retry on next sync cycle

### Deletes
- Never hard-delete from Room immediately
- Set `deleted_at` locally and `sync_status = PENDING_DELETE`
- The record is excluded from all queries using `WHERE deleted_at IS NULL`
- After `SyncWorker` confirms the remote soft delete, hard-delete from Room

### Network State Awareness
- The app observes `ConnectivityManager.NetworkCallback`
- When network becomes available: trigger `SyncWorker` immediately (one-time work request)
- Show a subtle sync indicator in the toolbar when a sync is in progress

---

## 11. Notifications & Reminders

### Habit Reminders
- Each habit can have an optional daily reminder time
- Implemented using `AlarmManager` with `setExactAndAllowWhileIdle` for precise timing
- Notification: "[Habit Name] — Time to check in 🔥 N day streak"
- Tapping the notification marks the habit as done for today (PendingIntent → BroadcastReceiver → HabitRepository)

### Goal Deadlines
- 3 days before a goal's target date: "⚡ [Goal Name] is due in 3 days"
- On the target date: "🎯 [Goal Name] — Target date is today"

### Calendar Event Reminders
- Default: 15 minutes before event
- Configurable per event: 5 min / 15 min / 30 min / 1 hour / 1 day
- Implemented using `AlarmManager`

### Streak at Risk
- If a daily habit is not completed by 8 PM local time, send a reminder: "[Habit Name] streak at risk! 🔥 Don't break your N-day streak"
- This is scheduled daily using `WorkManager` periodic work

### Notification Channels (Android 8+)
- `CHANNEL_HABITS` — Habit Reminders (default importance)
- `CHANNEL_GOALS` — Goal Deadlines (default importance)
- `CHANNEL_EVENTS` — Calendar Events (high importance)
- `CHANNEL_STREAK` — Streak Alerts (default importance)

---

## 12. UI/UX Guidelines

### Design Language
- **Dark mode first.** The app is designed primarily for dark mode with full light mode support.
- **Material Design 3** (Material You) as the base design system via Jetpack Compose
- Dynamic color theming using the device wallpaper color (Android 12+), with a hardcoded fallback palette for older devices
- **Dense but readable:** inspired by Obsidian — not excessive whitespace, content-dense layouts

### Typography
- Headings: `Inter` or system default sans-serif, bold
- Body: System default
- Code / Markdown code blocks: `JetBrains Mono`

### Colors
- Background: `#0F0F0F` (dark) / `#FAFAFA` (light)
- Surface: `#1A1A1A` (dark) / `#FFFFFF` (light)
- Primary accent: User-configurable (default: deep purple `#7C3AED`)
- Habit completion: Green `#22C55E`
- Goal progress: Blue `#3B82F6`
- High priority: Red `#EF4444`
- Streak fire color: Orange `#F97316`

### Motion
- All transitions use `AnimatedContent` and `AnimatedVisibility` from Compose
- Bottom sheets use spring animation (not linear)
- Card drags in Kanban use haptic feedback on pick-up and drop
- Habit check-in triggers a brief confetti or ripple animation

### Accessibility
- All interactive elements have content descriptions
- Minimum touch target size: 48dp
- Support system font size scaling

---

## 13. Navigation Structure

```
Bottom Navigation Bar (5 items):
  🏠 Home
  ✅ Habits
  🎯 Goals
  📅 Calendar
  📋 Boards

Additional destinations (no bottom nav):
  📝 Notes          (accessible from Home FAB, search, and wikilinks)
  🔍 Search         (global search across all content)
  ⚙️ Settings       (top-right icon on Home)
  🧩 Quick Capture  (via Assist API or FAB long-press)
```

### Navigation Implementation
- Use Compose Navigation (`androidx.navigation:navigation-compose`)
- A single `NavHost` at the app root
- Bottom navigation persists across all 5 primary destinations
- Deep links from notifications navigate directly to relevant screens (e.g., habit detail, goal detail, event)

---

## 14. Tech Stack & Libraries

### Core
| Library | Version | Purpose |
|---|---|---|
| Kotlin | 2.x | Language |
| Jetpack Compose | Latest stable | UI framework |
| Material3 Compose | Latest stable | Design system |
| Hilt | Latest stable | Dependency injection |
| Kotlin Coroutines | Latest stable | Async programming |
| Kotlin Flow | Latest stable | Reactive data streams |

### Data
| Library | Version | Purpose |
|---|---|---|
| Room | Latest stable | Local SQLite ORM |
| DataStore (Preferences) | Latest stable | Settings, sync timestamps |
| EncryptedSharedPreferences | Latest stable | JWT token storage |
| Supabase Kotlin SDK (`gotrue-kt`, `postgrest-kt`, `realtime-kt`) | Latest stable | Supabase integration |
| WorkManager | Latest stable | Background sync |

### UI
| Library | Version | Purpose |
|---|---|---|
| Markwon (or Compose Markdown) | Latest stable | Markdown rendering in Compose |
| Reorderable (Calvin Liang) | Latest stable | Drag-and-drop for Kanban |
| Compose Charts | Latest stable | Habit heatmap, progress charts |
| Coil (Compose) | Latest stable | Image loading |
| Accompanist | As needed | Permissions, system UI |

### Utilities
| Library | Version | Purpose |
|---|---|---|
| Timber | Latest stable | Logging |
| KotlinX Serialization | Latest stable | JSON parsing |
| ThreeTen ABP / `java.time` | Built-in (API 26+) | Date/time handling |

---

## 15. Project Structure

```
com.automemoria/
├── MainActivity.kt
├── AutomemoriaApp.kt              (Application class, Hilt setup)
│
├── assist/
│   ├── PersonalAssistantService.kt
│   ├── PersonalAssistantSession.kt
│   └── QuickCaptureActivity.kt
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── entity/
│   │   │   ├── HabitEntity.kt
│   │   │   ├── HabitLogEntity.kt
│   │   │   ├── GoalEntity.kt
│   │   │   ├── GoalMilestoneEntity.kt
│   │   │   ├── CalendarEventEntity.kt
│   │   │   ├── BoardEntity.kt
│   │   │   ├── BoardColumnEntity.kt
│   │   │   ├── CardEntity.kt
│   │   │   ├── NoteEntity.kt
│   │   │   └── NoteLinkEntity.kt
│   │   └── dao/
│   │       ├── HabitDao.kt
│   │       ├── HabitLogDao.kt
│   │       ├── GoalDao.kt
│   │       ├── CalendarEventDao.kt
│   │       ├── BoardDao.kt
│   │       ├── CardDao.kt
│   │       └── NoteDao.kt
│   │
│   ├── remote/
│   │   ├── SupabaseClient.kt
│   │   └── dto/              (Data Transfer Objects matching Supabase schema)
│   │
│   └── repository/
│       ├── HabitRepository.kt
│       ├── GoalRepository.kt
│       ├── CalendarRepository.kt
│       ├── BoardRepository.kt
│       └── NoteRepository.kt
│
├── domain/
│   └── model/
│       ├── Habit.kt
│       ├── HabitLog.kt
│       ├── Goal.kt
│       ├── CalendarEvent.kt
│       ├── Board.kt
│       ├── Card.kt
│       └── Note.kt
│
├── sync/
│   ├── SyncWorker.kt
│   ├── SyncManager.kt
│   └── NetworkObserver.kt
│
├── notification/
│   ├── NotificationHelper.kt
│   ├── HabitReminderScheduler.kt
│   └── StreakAlertWorker.kt
│
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt
│   │   └── Screen.kt
│   │
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Typography.kt
│   │
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   │
│   ├── habits/
│   │   ├── HabitListScreen.kt
│   │   ├── HabitDetailScreen.kt
│   │   ├── HabitEditorScreen.kt
│   │   ├── HabitHeatmap.kt
│   │   └── HabitViewModel.kt
│   │
│   ├── goals/
│   │   ├── GoalListScreen.kt
│   │   ├── GoalDetailScreen.kt
│   │   ├── GoalEditorScreen.kt
│   │   └── GoalViewModel.kt
│   │
│   ├── calendar/
│   │   ├── CalendarScreen.kt
│   │   ├── EventDetailSheet.kt
│   │   ├── EventEditorScreen.kt
│   │   └── CalendarViewModel.kt
│   │
│   ├── kanban/
│   │   ├── BoardListScreen.kt
│   │   ├── BoardScreen.kt
│   │   ├── CardDetailSheet.kt
│   │   └── KanbanViewModel.kt
│   │
│   ├── notes/
│   │   ├── NoteListScreen.kt
│   │   ├── NoteEditorScreen.kt
│   │   ├── NoteDetailScreen.kt
│   │   ├── WikilinkAutocomplete.kt
│   │   └── NoteViewModel.kt
│   │
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
│
└── di/
    ├── AppModule.kt
    ├── DatabaseModule.kt
    └── RepositoryModule.kt
```

---

## 16. Phased Delivery Plan

### Phase 0 — Project Setup (3–4 days)
- [ ] Create Android project with Kotlin + Jetpack Compose
- [ ] Configure Hilt for DI
- [ ] Set up Room database with all entities and DAOs
- [ ] Set up Supabase project, run all SQL schema scripts, enable RLS
- [ ] Integrate Supabase Kotlin SDK, implement auth (setup screen)
- [ ] Implement `SyncWorker` skeleton (no actual sync yet)
- [ ] Set up Compose navigation with all screen placeholders
- [ ] Configure Material3 theme with dark/light modes

### Phase 1 — Habits (1 week)
- [ ] Habit list screen
- [ ] Habit detail screen with streak calculation
- [ ] Habit heatmap component
- [ ] Create/edit habit screen
- [ ] Habit log (mark complete) logic
- [ ] Habit sync: Room ↔ Supabase for `habits` and `habit_logs` tables
- [ ] Habit reminder notifications

### Phase 2 — Goals (1 week)
- [ ] Goal list screen
- [ ] Goal detail screen with milestones
- [ ] Create/edit goal screen
- [ ] Link habits to goals
- [ ] Goal progress calculation
- [ ] Goal sync: Room ↔ Supabase

### Phase 3 — Calendar (1 week)
- [ ] Month calendar view
- [ ] Day agenda view
- [ ] Event create/edit sheet
- [ ] Recurring event expansion logic
- [ ] Calendar event sync

### Phase 4 — Kanban (1 week)
- [ ] Board list screen
- [ ] Board screen with columns
- [ ] Card drag-and-drop (reorder + move between columns)
- [ ] Card detail sheet
- [ ] Kanban sync

### Phase 5 — Notes (1 week)
- [ ] Note list screen with full-text search
- [ ] Markdown editor with toolbar
- [ ] Wikilink `[[autocomplete]]` input
- [ ] Wikilink parsing and `NoteLink` generation on save
- [ ] Backlinks section in note detail
- [ ] Notes sync

### Phase 6 — Home Dashboard (3 days)
- [ ] Assemble home screen using data from all modules
- [ ] Quick capture FAB + bottom sheet
- [ ] Dashboard polish

### Phase 7 — Assist API (3–4 days)
- [ ] Implement `VoiceInteractionService`
- [ ] Quick capture overlay Activity
- [ ] Test on device as default assistant

### Phase 8 — Sync Polish + Reliability (1 week)
- [ ] Full sync engine (pull + push + delete)
- [ ] Conflict resolution logic
- [ ] Sync status indicator in UI
- [ ] Network observer → trigger sync on connectivity

### Phase 9 — Polish & Testing (ongoing)
- [ ] Animations and haptics
- [ ] Edge case handling (empty states, error states)
- [ ] Notification polish
- [ ] Performance profiling (especially Kanban drag and note rendering)
- [ ] Graph view for notes (stretch goal)

---

## 17. Open Questions

| # | Question | Notes |
|---|---|---|
| 1 | Should the Quick Capture overlay support voice input? | Would require `SpeechRecognizer` integration; deferred to v2 |
| 2 | What is the "default board" for quick-capture tasks? | Configurable in Settings; default = first board created |
| 3 | Should note content be stored as raw markdown strings or as a structured AST? | v1: raw markdown strings for simplicity; v2: consider AST for richer editing |
| 4 | How are recurring calendar events handled during sync? | v1: only the recurrence rule is synced, expansion is local; no per-occurrence records unless modified |
| 5 | Should habits support "partial completion" or "quantity logging" (e.g., drank 6/8 glasses)? | Deferred to v2; v1 is binary (done/not done) |
| 6 | How to handle large note graphs (100+ notes) in the graph view? | Use lazy loading and culling; deferred to v2 |
| 7 | Should the app support widgets on the Android home screen? | High value feature; planned for v1.1 |
