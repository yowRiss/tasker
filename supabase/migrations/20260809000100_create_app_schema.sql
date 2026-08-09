-- Personal Tasks + Notes application schema.
-- auth.users is owned by Supabase Auth and is deliberately not duplicated here.

create extension if not exists pgcrypto;

create function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create table public.projects (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  name varchar(80) not null check (name = btrim(name) and name <> ''),
  color varchar(7),
  is_archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id)
);

create unique index projects_user_name_ci_key
  on public.projects (user_id, lower(name));

create table public.tags (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  name varchar(40) not null check (name = btrim(name) and name <> ''),
  color varchar(7),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id)
);

create unique index tags_user_name_ci_key
  on public.tags (user_id, lower(name));

create table public.tasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  title varchar(280) not null check (title = btrim(title) and title <> ''),
  description text,
  status text not null default 'open' check (status in ('open', 'completed')),
  completed_at timestamptz,
  due_date date,
  priority smallint not null default 0 check (priority between 0 and 3),
  project_id uuid,
  search_vector tsvector generated always as (
    to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, ''))
  ) stored,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint tasks_completed_at_matches_status check (
    (status = 'completed' and completed_at is not null)
    or (status = 'open' and completed_at is null)
  ),
  constraint tasks_project_owner_fkey
    foreign key (project_id, user_id)
    references public.projects (id, user_id)
    on delete set null (project_id),
  unique (id, user_id)
);

create index tasks_user_status_due_date_idx
  on public.tasks (user_id, status, due_date);
create index tasks_user_project_idx
  on public.tasks (user_id, project_id);
create index tasks_user_updated_at_idx
  on public.tasks (user_id, updated_at desc);
create index tasks_search_vector_idx
  on public.tasks using gin (search_vector);

create table public.task_tags (
  user_id uuid not null references public.admins(id) on delete cascade,
  task_id uuid not null,
  tag_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (task_id, tag_id),
  constraint task_tags_task_owner_fkey
    foreign key (task_id, user_id)
    references public.tasks (id, user_id)
    on delete cascade,
  constraint task_tags_tag_owner_fkey
    foreign key (tag_id, user_id)
    references public.tags (id, user_id)
    on delete cascade
);

create index task_tags_user_id_idx on public.task_tags (user_id);
create index task_tags_tag_id_idx on public.task_tags (tag_id);

create table public.notes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  title varchar(280) not null check (title = btrim(title) and title <> ''),
  content_md text not null default '',
  search_vector tsvector generated always as (
    to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content_md, ''))
  ) stored,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id)
);

create index notes_user_updated_at_idx
  on public.notes (user_id, updated_at desc);
create index notes_search_vector_idx
  on public.notes using gin (search_vector);

create table public.note_tags (
  user_id uuid not null references public.admins(id) on delete cascade,
  note_id uuid not null,
  tag_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (note_id, tag_id),
  constraint note_tags_note_owner_fkey
    foreign key (note_id, user_id)
    references public.notes (id, user_id)
    on delete cascade,
  constraint note_tags_tag_owner_fkey
    foreign key (tag_id, user_id)
    references public.tags (id, user_id)
    on delete cascade
);

create index note_tags_user_id_idx on public.note_tags (user_id);
create index note_tags_tag_id_idx on public.note_tags (tag_id);

create table public.note_task_links (
  user_id uuid not null references public.admins(id) on delete cascade,
  note_id uuid not null,
  task_id uuid not null,
  created_at timestamptz not null default now(),
  primary key (note_id, task_id),
  constraint note_task_links_note_owner_fkey
    foreign key (note_id, user_id)
    references public.notes (id, user_id)
    on delete cascade,
  constraint note_task_links_task_owner_fkey
    foreign key (task_id, user_id)
    references public.tasks (id, user_id)
    on delete cascade
);

create index note_task_links_user_id_idx on public.note_task_links (user_id);
create index note_task_links_task_id_idx on public.note_task_links (task_id);

create table public.note_images (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  note_id uuid not null,
  bucket_id text not null default 'note-images' check (bucket_id = 'note-images'),
  object_path text not null,
  original_filename varchar(255) not null,
  mime_type varchar(100) not null,
  byte_size integer not null check (byte_size > 0 and byte_size <= 10485760),
  alt_text varchar(280),
  width integer check (width is null or width > 0),
  height integer check (height is null or height > 0),
  created_at timestamptz not null default now(),
  constraint note_images_note_owner_fkey
    foreign key (note_id, user_id)
    references public.notes (id, user_id)
    on delete cascade,
  unique (bucket_id, object_path)
);

create index note_images_user_id_idx on public.note_images (user_id);
create index note_images_note_id_idx on public.note_images (note_id);

create trigger projects_set_updated_at
before update on public.projects
for each row execute function public.set_updated_at();

create trigger tags_set_updated_at
before update on public.tags
for each row execute function public.set_updated_at();

create trigger tasks_set_updated_at
before update on public.tasks
for each row execute function public.set_updated_at();

create trigger notes_set_updated_at
before update on public.notes
for each row execute function public.set_updated_at();

-- RLS is active in this initial schema migration. The Go API sets
-- request.jwt.claim.sub transaction-locally after verifying the JWT.
alter table public.projects enable row level security;
alter table public.projects force row level security;
alter table public.tags enable row level security;
alter table public.tags force row level security;
alter table public.tasks enable row level security;
alter table public.tasks force row level security;
alter table public.task_tags enable row level security;
alter table public.task_tags force row level security;
alter table public.notes enable row level security;
alter table public.notes force row level security;
alter table public.note_tags enable row level security;
alter table public.note_tags force row level security;
alter table public.note_task_links enable row level security;
alter table public.note_task_links force row level security;
alter table public.note_images enable row level security;
alter table public.note_images force row level security;

create policy projects_select_own on public.projects for select using (auth.uid() = user_id);
create policy projects_insert_own on public.projects for insert with check (auth.uid() = user_id);
create policy projects_update_own on public.projects for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy projects_delete_own on public.projects for delete using (auth.uid() = user_id);

create policy tags_select_own on public.tags for select using (auth.uid() = user_id);
create policy tags_insert_own on public.tags for insert with check (auth.uid() = user_id);
create policy tags_update_own on public.tags for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy tags_delete_own on public.tags for delete using (auth.uid() = user_id);

create policy tasks_select_own on public.tasks for select using (auth.uid() = user_id);
create policy tasks_insert_own on public.tasks for insert with check (auth.uid() = user_id);
create policy tasks_update_own on public.tasks for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy tasks_delete_own on public.tasks for delete using (auth.uid() = user_id);

create policy task_tags_select_own on public.task_tags for select using (auth.uid() = user_id);
create policy task_tags_insert_own on public.task_tags for insert with check (auth.uid() = user_id);
create policy task_tags_update_own on public.task_tags for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy task_tags_delete_own on public.task_tags for delete using (auth.uid() = user_id);

create policy notes_select_own on public.notes for select using (auth.uid() = user_id);
create policy notes_insert_own on public.notes for insert with check (auth.uid() = user_id);
create policy notes_update_own on public.notes for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy notes_delete_own on public.notes for delete using (auth.uid() = user_id);

create policy note_tags_select_own on public.note_tags for select using (auth.uid() = user_id);
create policy note_tags_insert_own on public.note_tags for insert with check (auth.uid() = user_id);
create policy note_tags_update_own on public.note_tags for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy note_tags_delete_own on public.note_tags for delete using (auth.uid() = user_id);

create policy note_task_links_select_own on public.note_task_links for select using (auth.uid() = user_id);
create policy note_task_links_insert_own on public.note_task_links for insert with check (auth.uid() = user_id);
create policy note_task_links_update_own on public.note_task_links for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy note_task_links_delete_own on public.note_task_links for delete using (auth.uid() = user_id);

create policy note_images_select_own on public.note_images for select using (auth.uid() = user_id);
create policy note_images_insert_own on public.note_images for insert with check (auth.uid() = user_id);
create policy note_images_update_own on public.note_images for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy note_images_delete_own on public.note_images for delete using (auth.uid() = user_id);

-- Private bucket: object reads require a signed URL or authorized Storage access.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('note-images', 'note-images', false, 10485760,
  array['image/jpeg', 'image/png', 'image/webp', 'image/gif']::text[])
on conflict (id) do update set
  name = excluded.name,
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create policy note_images_storage_select_own on storage.objects for select
  using (bucket_id = 'note-images' and (storage.foldername(name))[1] = 'notes' and (storage.foldername(name))[2] = auth.uid()::text);
create policy note_images_storage_insert_own on storage.objects for insert
  with check (bucket_id = 'note-images' and (storage.foldername(name))[1] = 'notes' and (storage.foldername(name))[2] = auth.uid()::text);
create policy note_images_storage_update_own on storage.objects for update
  using (bucket_id = 'note-images' and (storage.foldername(name))[1] = 'notes' and (storage.foldername(name))[2] = auth.uid()::text)
  with check (bucket_id = 'note-images' and (storage.foldername(name))[1] = 'notes' and (storage.foldername(name))[2] = auth.uid()::text);
create policy note_images_storage_delete_own on storage.objects for delete
  using (bucket_id = 'note-images' and (storage.foldername(name))[1] = 'notes' and (storage.foldername(name))[2] = auth.uid()::text);
