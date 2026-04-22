-- ============================================================
-- Automemoria — Supabase Schema
-- Run this in your Supabase project's SQL Editor
-- ============================================================

-- Enable UUID extension
create extension if not exists "pgcrypto";

-- ─── Habits ──────────────────────────────────────────────────────────────────

create table if not exists habits (
  id              uuid primary key default gen_random_uuid(),
  name            text not null,
  description     text,
  icon            text,
  color           text,
  frequency       text not null default 'daily',
  frequency_days  int[] default '{}',
  target_streak   int default 0,
  is_archived     boolean default false,
  created_at      timestamptz default now(),
  updated_at      timestamptz default now(),
  deleted_at      timestamptz
);

create table if not exists habit_logs (
  id           uuid primary key default gen_random_uuid(),
  habit_id     uuid references habits(id) on delete cascade,
  logged_date  date not null,
  completed    boolean default true,
  note         text,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now(),
  unique(habit_id, logged_date)
);

-- ─── Goals ───────────────────────────────────────────────────────────────────

create table if not exists goals (
  id               uuid primary key default gen_random_uuid(),
  title            text not null,
  description      text,
  icon             text,
  color            text,
  status           text default 'active',
  progress         int default 0,
  target_date      date,
  parent_id        uuid references goals(id),
  linked_habit_ids uuid[] default '{}',
  created_at       timestamptz default now(),
  updated_at       timestamptz default now(),
  deleted_at       timestamptz
);

create table if not exists goal_milestones (
  id           uuid primary key default gen_random_uuid(),
  goal_id      uuid references goals(id) on delete cascade,
  title        text not null,
  is_completed boolean default false,
  due_date     date,
  completed_at timestamptz,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now()
);

-- ─── Calendar ────────────────────────────────────────────────────────────────

create table if not exists calendar_events (
  id              uuid primary key default gen_random_uuid(),
  title           text not null,
  description     text,
  location        text,
  start_time      timestamptz not null,
  end_time        timestamptz,
  is_all_day      boolean default false,
  color           text,
  recurrence      jsonb,
  linked_goal_id  uuid references goals(id),
  linked_habit_id uuid references habits(id),
  created_at      timestamptz default now(),
  updated_at      timestamptz default now(),
  deleted_at      timestamptz
);

-- ─── Kanban ───────────────────────────────────────────────────────────────────

create table if not exists boards (
  id             uuid primary key default gen_random_uuid(),
  title          text not null,
  description    text,
  color          text,
  icon           text,
  sort_order     int default 0,
  linked_goal_id uuid references goals(id),
  created_at     timestamptz default now(),
  updated_at     timestamptz default now(),
  deleted_at     timestamptz
);

create table if not exists board_columns (
  id         uuid primary key default gen_random_uuid(),
  board_id   uuid references boards(id) on delete cascade,
  title      text not null,
  color      text,
  sort_order int default 0,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists cards (
  id             uuid primary key default gen_random_uuid(),
  column_id      uuid references board_columns(id) on delete cascade,
  title          text not null,
  description    text,
  priority       text default 'none',
  due_date       date,
  labels         text[] default '{}',
  sort_order     int default 0,
  is_completed   boolean default false,
  linked_note_id uuid,
  created_at     timestamptz default now(),
  updated_at     timestamptz default now(),
  deleted_at     timestamptz
);

-- ─── Notes ───────────────────────────────────────────────────────────────────

create table if not exists notes (
  id             uuid primary key default gen_random_uuid(),
  title          text not null,
  content        text,
  tags           text[] default '{}',
  is_pinned      boolean default false,
  linked_goal_id uuid references goals(id),
  linked_card_id uuid references cards(id),
  created_at     timestamptz default now(),
  updated_at     timestamptz default now(),
  deleted_at     timestamptz
);

create table if not exists note_links (
  id             uuid primary key default gen_random_uuid(),
  source_note_id uuid references notes(id) on delete cascade,
  target_note_id uuid references notes(id) on delete cascade,
  created_at     timestamptz default now(),
  unique(source_note_id, target_note_id)
);

-- ─── Auto-update updated_at trigger ──────────────────────────────────────────

create or replace function update_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

-- Apply trigger to all tables with updated_at
do $$
declare
  t text;
begin
  foreach t in array array[
    'habits','habit_logs','goals','goal_milestones',
    'calendar_events','boards','board_columns','cards','notes'
  ]
  loop
    execute format(
      'create or replace trigger trg_%s_updated_at
       before update on %s
       for each row execute function update_updated_at();', t, t
    );
  end loop;
end;
$$;

-- ─── Row Level Security ───────────────────────────────────────────────────────
-- Single-user app: enable RLS and allow authenticated user full access

alter table habits          enable row level security;
alter table habit_logs      enable row level security;
alter table goals           enable row level security;
alter table goal_milestones enable row level security;
alter table calendar_events enable row level security;
alter table boards          enable row level security;
alter table board_columns   enable row level security;
alter table cards           enable row level security;
alter table notes           enable row level security;
alter table note_links      enable row level security;

-- Policies: authenticated users can do everything
do $$
declare
  t text;
begin
  foreach t in array array[
    'habits','habit_logs','goals','goal_milestones',
    'calendar_events','boards','board_columns','cards','notes','note_links'
  ]
  loop
    execute format(
      'create policy "authenticated_all_%s" on %s
       for all to authenticated using (true) with check (true);', t, t
    );
  end loop;
end;
$$;
