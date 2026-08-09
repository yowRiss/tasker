-- Admin accounts for local authentication (replaces Supabase Auth for login).
create table public.admins (
  id uuid primary key default gen_random_uuid(),
  username varchar(80) not null unique,
  password_hash text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create trigger admins_set_updated_at
before update on public.admins
for each row execute function public.set_updated_at();
