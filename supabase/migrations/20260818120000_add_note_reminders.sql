-- Migration to add Google Calendar style notification/reminder fields to notes table.

alter table public.notes
  add column if not exists reminder_at timestamptz null,
  add column if not exists reminder_offsets integer[] not null default '{0}';

create index if not exists notes_user_reminder_at_idx
  on public.notes (user_id, reminder_at)
  where reminder_at is not null;
