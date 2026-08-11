create table public.subtasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  task_id uuid not null,
  title varchar(280) not null check (title = btrim(title) and title <> ''),
  completed boolean not null default false,
  position integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint subtasks_task_owner_fkey
    foreign key (task_id, user_id)
    references public.tasks (id, user_id)
    on delete cascade,
  unique (id, user_id)
);

create index subtasks_task_position_idx
  on public.subtasks (task_id, position, created_at);
create index subtasks_user_task_idx
  on public.subtasks (user_id, task_id);

create trigger subtasks_set_updated_at
before update on public.subtasks
for each row execute function public.set_updated_at();

alter table public.subtasks enable row level security;
alter table public.subtasks force row level security;

create policy subtasks_select_own on public.subtasks for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy subtasks_insert_own on public.subtasks for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy subtasks_update_own on public.subtasks for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy subtasks_delete_own on public.subtasks for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

