-- Financial targets / savings goals schema. All monetary amounts use exact decimal IDR.

create table public.targets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  name varchar(80) not null check (name = btrim(name) and name <> ''),
  target_amount numeric(18,2) not null check (target_amount > 0),
  current_amount numeric(18,2) not null default 0 check (current_amount >= 0),
  target_date date,
  category_id uuid,
  account_id uuid,
  color varchar(7),
  icon varchar(80),
  status text not null default 'active' check (status in ('active', 'achieved', 'paused', 'cancelled')),
  notes varchar(1000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint targets_category_owner_fkey
    foreign key (category_id, user_id)
    references public.categories (id, user_id)
    on delete set null,
  constraint targets_account_owner_fkey
    foreign key (account_id, user_id)
    references public.accounts (id, user_id)
    on delete set null,
  unique (id, user_id)
);

create index targets_user_status_date_idx
  on public.targets (user_id, status, target_date);
create index targets_user_created_at_idx
  on public.targets (user_id, created_at desc);
create index targets_user_category_id_idx
  on public.targets (user_id, category_id)
  where category_id is not null;
create index targets_user_account_id_idx
  on public.targets (user_id, account_id)
  where account_id is not null;

create trigger targets_set_updated_at
before update on public.targets
for each row execute function public.set_updated_at();

alter table public.targets enable row level security;
alter table public.targets force row level security;

create policy targets_select_own on public.targets for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy targets_insert_own on public.targets for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy targets_update_own on public.targets for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy targets_delete_own on public.targets for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
