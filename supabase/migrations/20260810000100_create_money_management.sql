-- Personal money-management schema. All monetary amounts use exact decimal IDR.

create table public.accounts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  name varchar(80) not null check (name = btrim(name) and name <> ''),
  account_type text not null check (account_type in ('cash', 'bank', 'e_wallet', 'credit_card')),
  currency char(3) not null default 'IDR' check (currency = 'IDR'),
  archived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id)
);

create unique index accounts_user_name_ci_key
  on public.accounts (user_id, lower(name));
create index accounts_user_archived_at_idx
  on public.accounts (user_id, archived_at);

create table public.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  name varchar(80) not null check (name = btrim(name) and name <> ''),
  category_type text not null check (category_type in ('income', 'expense')),
  icon varchar(80),
  color varchar(7),
  archived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, user_id)
);

create unique index categories_user_type_name_ci_key
  on public.categories (user_id, category_type, lower(name));
create index categories_user_archived_at_idx
  on public.categories (user_id, archived_at);

create table public.transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  transaction_type text not null check (transaction_type in ('income', 'expense', 'transfer')),
  amount numeric(18,2) not null check (amount > 0),
  transaction_date date not null,
  account_id uuid not null,
  transfer_account_id uuid,
  category_id uuid,
  description varchar(1000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint transactions_account_owner_fkey
    foreign key (account_id, user_id)
    references public.accounts (id, user_id)
    on delete restrict,
  constraint transactions_transfer_account_owner_fkey
    foreign key (transfer_account_id, user_id)
    references public.accounts (id, user_id)
    on delete restrict,
  constraint transactions_category_owner_fkey
    foreign key (category_id, user_id)
    references public.categories (id, user_id)
    on delete restrict,
  constraint transactions_shape_check check (
    (transaction_type in ('income', 'expense') and category_id is not null and transfer_account_id is null)
    or (transaction_type = 'transfer' and category_id is null and transfer_account_id is not null and transfer_account_id <> account_id)
  ),
  unique (id, user_id)
);

create index transactions_user_date_id_idx
  on public.transactions (user_id, transaction_date desc, id desc);
create index transactions_user_account_date_idx
  on public.transactions (user_id, account_id, transaction_date desc);
create index transactions_user_transfer_account_date_idx
  on public.transactions (user_id, transfer_account_id, transaction_date desc)
  where transfer_account_id is not null;
create index transactions_user_category_date_idx
  on public.transactions (user_id, category_id, transaction_date desc)
  where category_id is not null;
create index transactions_user_type_date_idx
  on public.transactions (user_id, transaction_type, transaction_date desc);

create table public.transaction_receipts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  transaction_id uuid not null,
  bucket_id text not null default 'note-images' check (bucket_id = 'note-images'),
  object_path text not null,
  original_filename varchar(255) not null,
  mime_type varchar(100) not null,
  byte_size integer not null check (byte_size > 0 and byte_size <= 10485760),
  width integer check (width is null or width > 0),
  height integer check (height is null or height > 0),
  created_at timestamptz not null default now(),
  constraint transaction_receipts_transaction_owner_fkey
    foreign key (transaction_id, user_id)
    references public.transactions (id, user_id)
    on delete cascade,
  unique (transaction_id),
  unique (bucket_id, object_path)
);

create index transaction_receipts_user_id_idx on public.transaction_receipts (user_id);
create index transaction_receipts_transaction_id_idx on public.transaction_receipts (transaction_id);

create table public.budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  category_id uuid not null,
  period_start date not null,
  period_end date not null,
  amount_limit numeric(18,2) not null check (amount_limit > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint budgets_period_check check (period_end >= period_start),
  constraint budgets_category_owner_fkey
    foreign key (category_id, user_id)
    references public.categories (id, user_id)
    on delete restrict,
  unique (user_id, category_id, period_start, period_end)
);

create index budgets_user_category_period_idx
  on public.budgets (user_id, category_id, period_start, period_end);

create table public.recurring_transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.admins(id) on delete cascade,
  transaction_type text not null check (transaction_type in ('income', 'expense')),
  amount numeric(18,2) not null check (amount > 0),
  account_id uuid not null,
  category_id uuid not null,
  description varchar(1000),
  cadence text not null check (cadence in ('weekly', 'monthly', 'yearly')),
  next_due_date date not null,
  ends_on date,
  is_active boolean not null default true,
  last_processed_on date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint recurring_transactions_end_date_check check (ends_on is null or ends_on >= next_due_date),
  constraint recurring_transactions_account_owner_fkey
    foreign key (account_id, user_id)
    references public.accounts (id, user_id)
    on delete restrict,
  constraint recurring_transactions_category_owner_fkey
    foreign key (category_id, user_id)
    references public.categories (id, user_id)
    on delete restrict
);

create index recurring_transactions_user_next_due_idx
  on public.recurring_transactions (user_id, next_due_date)
  where is_active;
create index recurring_transactions_account_id_idx on public.recurring_transactions (account_id);
create index recurring_transactions_category_id_idx on public.recurring_transactions (category_id);

create trigger accounts_set_updated_at
before update on public.accounts
for each row execute function public.set_updated_at();

create trigger categories_set_updated_at
before update on public.categories
for each row execute function public.set_updated_at();

create trigger transactions_set_updated_at
before update on public.transactions
for each row execute function public.set_updated_at();

create trigger budgets_set_updated_at
before update on public.budgets
for each row execute function public.set_updated_at();

create trigger recurring_transactions_set_updated_at
before update on public.recurring_transactions
for each row execute function public.set_updated_at();

alter table public.accounts enable row level security;
alter table public.accounts force row level security;
alter table public.categories enable row level security;
alter table public.categories force row level security;
alter table public.transactions enable row level security;
alter table public.transactions force row level security;
alter table public.transaction_receipts enable row level security;
alter table public.transaction_receipts force row level security;
alter table public.budgets enable row level security;
alter table public.budgets force row level security;
alter table public.recurring_transactions enable row level security;
alter table public.recurring_transactions force row level security;

create policy accounts_select_own on public.accounts for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy accounts_insert_own on public.accounts for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy accounts_update_own on public.accounts for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy accounts_delete_own on public.accounts for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

create policy categories_select_own on public.categories for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy categories_insert_own on public.categories for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy categories_update_own on public.categories for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy categories_delete_own on public.categories for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

create policy transactions_select_own on public.transactions for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy transactions_insert_own on public.transactions for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy transactions_update_own on public.transactions for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy transactions_delete_own on public.transactions for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

create policy transaction_receipts_select_own on public.transaction_receipts for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy transaction_receipts_insert_own on public.transaction_receipts for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy transaction_receipts_update_own on public.transaction_receipts for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy transaction_receipts_delete_own on public.transaction_receipts for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

create policy budgets_select_own on public.budgets for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy budgets_insert_own on public.budgets for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy budgets_update_own on public.budgets for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy budgets_delete_own on public.budgets for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

create policy recurring_transactions_select_own on public.recurring_transactions for select
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy recurring_transactions_insert_own on public.recurring_transactions for insert
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy recurring_transactions_update_own on public.recurring_transactions for update
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid)
  with check (user_id = current_setting('request.jwt.claim.sub', true)::uuid);
create policy recurring_transactions_delete_own on public.recurring_transactions for delete
  using (user_id = current_setting('request.jwt.claim.sub', true)::uuid);

-- The backend-only Storage key bypasses these policies after Go authorizes the
-- local JWT. Keeping them aligned with the same local claim prevents direct
-- Storage access from becoming a second authorization path.
drop policy if exists note_images_storage_select_own on storage.objects;
drop policy if exists note_images_storage_insert_own on storage.objects;
drop policy if exists note_images_storage_update_own on storage.objects;
drop policy if exists note_images_storage_delete_own on storage.objects;

create policy private_media_storage_select_own on storage.objects for select
  using (
    bucket_id = 'note-images'
    and (storage.foldername(name))[1] in ('notes', 'receipts')
    and (storage.foldername(name))[2] = current_setting('request.jwt.claim.sub', true)
  );
create policy private_media_storage_insert_own on storage.objects for insert
  with check (
    bucket_id = 'note-images'
    and (storage.foldername(name))[1] in ('notes', 'receipts')
    and (storage.foldername(name))[2] = current_setting('request.jwt.claim.sub', true)
  );
create policy private_media_storage_update_own on storage.objects for update
  using (
    bucket_id = 'note-images'
    and (storage.foldername(name))[1] in ('notes', 'receipts')
    and (storage.foldername(name))[2] = current_setting('request.jwt.claim.sub', true)
  )
  with check (
    bucket_id = 'note-images'
    and (storage.foldername(name))[1] in ('notes', 'receipts')
    and (storage.foldername(name))[2] = current_setting('request.jwt.claim.sub', true)
  );
create policy private_media_storage_delete_own on storage.objects for delete
  using (
    bucket_id = 'note-images'
    and (storage.foldername(name))[1] in ('notes', 'receipts')
    and (storage.foldername(name))[2] = current_setting('request.jwt.claim.sub', true)
  );
