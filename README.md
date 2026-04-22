# Automemoria — Android App

> Your personal Notion + Obsidian + habit tracker. Offline-first, Supabase-synced, summonable via long-press power button.

---

## Quick Start

### 1. Clone & open in Android Studio

```bash
git clone <your-repo-url>
cd automemoria
```

Open the project in **Android Studio Ladybug (2024.2.1)** or newer.

---

### 2. Configure Supabase credentials

Copy the template:
```bash
cp local.properties.template local.properties
```

Edit `local.properties` and fill in your values:
```
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
```

> ⚠️ `local.properties` is gitignored. Never commit credentials.

---

### 3. Set up Supabase

1. Go to [supabase.com](https://supabase.com) and open your project
2. Navigate to **SQL Editor**
3. Paste the entire contents of `supabase_schema.sql` and run it
4. Go to **Authentication → Users** and create your user account manually
5. Copy your **Project URL** and **anon public key** from **Settings → API**

---

### 4. Build and run

In Android Studio:
- Select your device or emulator (API 26+ required)
- Click **Run ▶️**

Or via terminal:
```bash
./gradlew installDebug
```

---

### 5. Set as Default Digital Assistant (power button summon)

On your Android device:
1. Go to **Settings → Apps → Default Apps → Digital Assistant**
2. Select **Automemoria**
3. Long-press the power button (or home button on some devices) — the Quick Capture overlay will appear

---

## Project Structure

```
app/src/main/kotlin/moe/memesta/automemoria/
├── assist/          # Assist API (power button summon + Quick Capture overlay)
├── data/
│   ├── local/       # Room database, entities, DAOs
│   ├── remote/      # Supabase client, DTOs
│   └── repository/  # Data layer — Room-first, Supabase-second
├── domain/model/    # Pure Kotlin domain models
├── sync/            # SyncWorker, SyncPreferences, NetworkObserver
├── ui/
│   ├── home/        # Dashboard screen
│   ├── habits/      # Habit list + create sheet
│   ├── goals/       # Goals (Phase 2)
│   ├── calendar/    # Calendar month view
│   ├── kanban/      # Board list (Phase 4)
│   ├── notes/       # Notes list (Phase 5)
│   ├── navigation/  # AppNavHost + Screen routes
│   └── theme/       # Material3 dark-first theme
├── di/              # Hilt modules
├── MainActivity.kt
└── AutomemoriaApp.kt
```

---

## Build Phases

| Phase | Feature | Status |
|-------|---------|--------|
| 0 | Project setup, Room, Supabase auth, SyncWorker skeleton | ✅ Scaffold done |
| 1 | Habits — streaks, heatmap, full sync | 🔜 Next |
| 2 | Goals — milestones, progress tracking | ⬜ |
| 3 | Calendar — month view, events, recurrence | ⬜ |
| 4 | Kanban — boards, drag-and-drop cards | ⬜ |
| 5 | Notes — markdown editor, wikilinks, backlinks | ⬜ |
| 6 | Home dashboard — full assembly | ⬜ |
| 7 | Assist API — full Quick Capture with repos | ⬜ |
| 8 | Sync engine — full pull/push/delete/conflict resolution | ⬜ |
| 9 | Polish — animations, widgets, graph view | ⬜ |

---

## Tech Stack

| | Library |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material3 |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Settings | DataStore Preferences |
| Sync | WorkManager |
| Cloud | Supabase (Postgres + Auth + Realtime) |
| HTTP | Ktor Android |
| Images | Coil |
| Logging | Timber |

---

## Next steps after Phase 0

1. Verify sync end-to-end: create a habit on device → check Supabase dashboard → confirm the row appears
2. Move to Phase 1: implement `HabitDetailScreen`, streak calculation, and the heatmap component
3. Add `GoalRepository` and `NoteRepository` following the same pattern as `HabitRepository`
