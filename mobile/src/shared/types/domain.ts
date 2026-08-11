export interface Project {
  id: string;
  name: string;
  color?: string | null;
  is_archived: boolean;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface Tag {
  id: string;
  name: string;
  color?: string | null;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface Subtask {
  id: string;
  task_id: string;
  title: string;
  completed: boolean;
  position: number;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface Task {
  id: string;
  title: string;
  description?: string | null;
  status: 'open' | 'completed';
  completed_at?: string | null;
  due_date?: string | null;
  priority: number; // 0=None, 1=Low, 2=Medium, 3=High
  project_id?: string | null;
  project?: Project | null;
  tags?: Tag[];
  subtasks?: Subtask[];
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface NoteImage {
  id: string;
  note_id: string;
  bucket_id: string;
  object_path?: string | null;
  local_uri: string;
  original_filename: string;
  mime_type: string;
  byte_size: number;
  alt_text?: string | null;
  width?: number | null;
  height?: number | null;
  created_at: string;
  sync_status: 'pending' | 'uploading' | 'synced' | 'failed';
}

export interface Note {
  id: string;
  title: string;
  content_md: string;
  tags?: Tag[];
  linked_task_ids?: string[];
  images?: NoteImage[];
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface Account {
  id: string;
  name: string;
  account_type: 'cash' | 'bank' | 'e_wallet' | 'credit_card';
  currency: 'IDR';
  archived_at?: string | null;
  current_balance?: number; // Derived locally or from API
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface Category {
  id: string;
  name: string;
  category_type: 'income' | 'expense';
  icon?: string | null;
  color?: string | null;
  archived_at?: string | null;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface TransactionReceipt {
  id: string;
  transaction_id: string;
  bucket_id: string;
  object_path?: string | null;
  local_uri: string;
  original_filename: string;
  mime_type: string;
  byte_size: number;
  width?: number | null;
  height?: number | null;
  created_at: string;
  sync_status: 'pending' | 'uploading' | 'synced' | 'failed';
}

export interface Transaction {
  id: string;
  transaction_type: 'income' | 'expense' | 'transfer';
  amount: number;
  transaction_date: string;
  account_id: string;
  transfer_account_id?: string | null;
  category_id?: string | null;
  description?: string | null;
  account?: Account | null;
  transfer_account?: Account | null;
  category?: Category | null;
  receipt?: TransactionReceipt | null;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface Budget {
  id: string;
  category_id: string;
  category?: Category | null;
  period_start: string;
  period_end: string;
  amount_limit: number;
  spent_amount?: number;
  remaining_amount?: number;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface RecurringTransaction {
  id: string;
  transaction_type: 'income' | 'expense';
  amount: number;
  account_id: string;
  category_id: string;
  description?: string | null;
  cadence: 'weekly' | 'monthly' | 'yearly';
  next_due_date: string;
  ends_on?: string | null;
  is_active: boolean;
  last_processed_on?: string | null;
  created_at: string;
  updated_at: string;
  is_deleted?: number;
}

export interface SyncQueueItem {
  id: number;
  entity_type: string;
  entity_id: string;
  operation: 'CREATE' | 'UPDATE' | 'DELETE' | 'UPLOAD_IMAGE' | 'UPLOAD_RECEIPT';
  payload: string;
  created_at: string;
  retry_count: number;
  last_error?: string | null;
  status: 'pending' | 'processing' | 'failed';
}
