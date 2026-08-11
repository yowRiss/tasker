import * as Crypto from 'expo-crypto';
import { getDatabase } from '../database';
import { DatabaseNotifier } from '../notifier';
import {
  Account,
  Category,
  Transaction,
  TransactionReceipt,
  Budget,
  RecurringTransaction,
} from '../../shared/types/domain';
import { LocalPickedImage } from '../../services/imageService';

export interface CreateAccountInput {
  name: string;
  account_type: 'cash' | 'bank' | 'e_wallet' | 'credit_card';
}

export interface CreateCategoryInput {
  name: string;
  category_type: 'income' | 'expense';
  color?: string;
  icon?: string;
}

export interface CreateTransactionInput {
  transaction_type: 'income' | 'expense' | 'transfer';
  amount: number;
  transaction_date: string;
  account_id: string;
  transfer_account_id?: string;
  category_id?: string;
  description?: string;
}

export interface CreateBudgetInput {
  category_id: string;
  period_start: string;
  period_end: string;
  amount_limit: number;
}

export class MoneyLocalRepository {
  // Accounts
  static getAccounts(): Account[] {
    const db = getDatabase();
    const rows = db.getAllSync<any>(
      `SELECT * FROM accounts WHERE is_deleted = 0 ORDER BY LOWER(name) ASC`
    );

    return rows.map((r) => {
      const balance = this.calculateAccountBalance(r.id);
      return {
        id: r.id,
        name: r.name,
        account_type: r.account_type,
        currency: r.currency,
        archived_at: r.archived_at,
        current_balance: balance,
        created_at: r.created_at,
        updated_at: r.updated_at,
      };
    });
  }

  static calculateAccountBalance(accountId: string): number {
    const db = getDatabase();
    const rows = db.getAllSync<{ transaction_type: string; amount: number; account_id: string; transfer_account_id: string }>(
      `SELECT transaction_type, amount, account_id, transfer_account_id
       FROM transactions
       WHERE is_deleted = 0 AND (account_id = ? OR transfer_account_id = ?)`,
      [accountId, accountId]
    );

    let total = 0;
    for (const tx of rows) {
      if (tx.transaction_type === 'income' && tx.account_id === accountId) {
        total += tx.amount;
      } else if (tx.transaction_type === 'expense' && tx.account_id === accountId) {
        total -= tx.amount;
      } else if (tx.transaction_type === 'transfer') {
        if (tx.account_id === accountId) {
          total -= tx.amount;
        } else if (tx.transfer_account_id === accountId) {
          total += tx.amount;
        }
      }
    }
    return total;
  }

  static createAccount(input: CreateAccountInput): Account {
    const db = getDatabase();
    const id = Crypto.randomUUID();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO accounts (id, name, account_type, currency, created_at, updated_at, is_deleted)
         VALUES (?, ?, ?, 'IDR', ?, ?, 0)`,
        [id, input.name.trim(), input.account_type, now, now]
      );

      const payload = JSON.stringify({ name: input.name.trim(), account_type: input.account_type });
      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('account', ?, 'CREATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['accounts', 'sync_queue']);
    return { id, name: input.name.trim(), account_type: input.account_type, currency: 'IDR', current_balance: 0, created_at: now, updated_at: now };
  }

  // Categories
  static getCategories(): Category[] {
    const db = getDatabase();
    return db.getAllSync<Category>(
      `SELECT * FROM categories WHERE is_deleted = 0 ORDER BY category_type ASC, LOWER(name) ASC`
    );
  }

  static createCategory(input: CreateCategoryInput): Category {
    const db = getDatabase();
    const id = Crypto.randomUUID();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO categories (id, name, category_type, icon, color, created_at, updated_at, is_deleted)
         VALUES (?, ?, ?, ?, ?, ?, ?, 0)`,
        [id, input.name.trim(), input.category_type, input.icon || null, input.color || null, now, now]
      );

      const payload = JSON.stringify({
        name: input.name.trim(),
        category_type: input.category_type,
        icon: input.icon || null,
        color: input.color || null,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('category', ?, 'CREATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['categories', 'sync_queue']);
    return { id, name: input.name.trim(), category_type: input.category_type, icon: input.icon, color: input.color, created_at: now, updated_at: now };
  }

  // Transactions
  static getTransactions(filters: { account_id?: string; category_id?: string; type?: string; search?: string } = {}): Transaction[] {
    const db = getDatabase();
    const conditions: string[] = ['t.is_deleted = 0'];
    const params: any[] = [];

    if (filters.account_id) {
      conditions.push('(t.account_id = ? OR t.transfer_account_id = ?)');
      params.push(filters.account_id, filters.account_id);
    }
    if (filters.category_id) {
      conditions.push('t.category_id = ?');
      params.push(filters.category_id);
    }
    if (filters.type && filters.type !== 'all') {
      conditions.push('t.transaction_type = ?');
      params.push(filters.type);
    }
    if (filters.search && filters.search.trim()) {
      conditions.push('t.description LIKE ?');
      params.push(`%${filters.search.trim()}%`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;
    const sql = `
      SELECT t.*,
             a.name as account_name, a.account_type as account_type,
             ta.name as transfer_account_name,
             c.name as category_name, c.color as category_color
      FROM transactions t
      LEFT JOIN accounts a ON t.account_id = a.id
      LEFT JOIN accounts ta ON t.transfer_account_id = ta.id
      LEFT JOIN categories c ON t.category_id = c.id
      ${whereClause}
      ORDER BY t.transaction_date DESC, t.created_at DESC
    `;

    const rows = db.getAllSync<any>(sql, params);

    return rows.map((r) => {
      const receipt = db.getFirstSync<TransactionReceipt>(
        `SELECT * FROM transaction_receipts WHERE transaction_id = ?`,
        [r.id]
      );

      return {
        id: r.id,
        transaction_type: r.transaction_type,
        amount: r.amount,
        transaction_date: r.transaction_date,
        account_id: r.account_id,
        transfer_account_id: r.transfer_account_id,
        category_id: r.category_id,
        description: r.description,
        account: r.account_id ? { id: r.account_id, name: r.account_name, account_type: r.account_type, currency: 'IDR', created_at: '', updated_at: '' } : null,
        transfer_account: r.transfer_account_id ? { id: r.transfer_account_id, name: r.transfer_account_name, account_type: 'bank', currency: 'IDR', created_at: '', updated_at: '' } : null,
        category: r.category_id ? { id: r.category_id, name: r.category_name, category_type: r.transaction_type, color: r.category_color, created_at: '', updated_at: '' } : null,
        receipt: receipt || null,
        created_at: r.created_at,
        updated_at: r.updated_at,
      };
    });
  }

  static createTransaction(input: CreateTransactionInput): Transaction {
    const db = getDatabase();
    const id = Crypto.randomUUID();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO transactions (id, transaction_type, amount, transaction_date, account_id, transfer_account_id, category_id, description, created_at, updated_at, is_deleted)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`,
        [
          id,
          input.transaction_type,
          input.amount,
          input.transaction_date,
          input.account_id,
          input.transfer_account_id || null,
          input.category_id || null,
          input.description?.trim() || null,
          now,
          now,
        ]
      );

      const payload = JSON.stringify({
        id,
        transaction_type: input.transaction_type,
        amount: input.amount,
        transaction_date: input.transaction_date,
        account_id: input.account_id,
        transfer_account_id: input.transfer_account_id || null,
        category_id: input.category_id || null,
        description: input.description?.trim() || null,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('transaction', ?, 'CREATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['transactions', 'accounts', 'budgets', 'sync_queue']);
    return this.getTransactions().find((t) => t.id === id)!;
  }

  static addReceipt(transactionId: string, image: LocalPickedImage): TransactionReceipt {
    const db = getDatabase();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO transaction_receipts (id, transaction_id, bucket_id, local_uri, original_filename, mime_type, byte_size, width, height, created_at, sync_status)
         VALUES (?, ?, 'note-images', ?, ?, ?, ?, ?, ?, ?, 'pending')`,
        [
          image.id,
          transactionId,
          image.localUri,
          image.originalFilename,
          image.mimeType,
          image.byteSize,
          image.width || null,
          image.height || null,
          now,
        ]
      );

      const payload = JSON.stringify({
        transaction_id: transactionId,
        receipt_id: image.id,
        local_uri: image.localUri,
        original_filename: image.originalFilename,
        mime_type: image.mimeType,
        byte_size: image.byteSize,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('transaction_receipt', ?, 'UPLOAD_RECEIPT', ?, ?, 'pending')`,
        [image.id, payload, now]
      );
    });

    DatabaseNotifier.notify(['transactions', 'sync_queue']);

    return {
      id: image.id,
      transaction_id: transactionId,
      bucket_id: 'note-images',
      local_uri: image.localUri,
      original_filename: image.originalFilename,
      mime_type: image.mimeType,
      byte_size: image.byteSize,
      created_at: now,
      sync_status: 'pending',
    };
  }

  // Budgets
  static getBudgets(): Budget[] {
    const db = getDatabase();
    const rows = db.getAllSync<any>(
      `SELECT b.*, c.name as category_name, c.color as category_color
       FROM budgets b
       LEFT JOIN categories c ON b.category_id = c.id
       WHERE b.is_deleted = 0
       ORDER BY b.period_start DESC`
    );

    return rows.map((r) => {
      // Spent sum
      const spentRow = db.getFirstSync<{ spent: number }>(
        `SELECT COALESCE(SUM(amount), 0) as spent
         FROM transactions
         WHERE is_deleted = 0
           AND transaction_type = 'expense'
           AND category_id = ?
           AND transaction_date >= ?
           AND transaction_date <= ?`,
        [r.category_id, r.period_start, r.period_end]
      );

      const spent = spentRow ? spentRow.spent : 0;
      const remaining = r.amount_limit - spent;

      return {
        id: r.id,
        category_id: r.category_id,
        category: { id: r.category_id, name: r.category_name, category_type: 'expense', color: r.category_color, created_at: '', updated_at: '' },
        period_start: r.period_start,
        period_end: r.period_end,
        amount_limit: r.amount_limit,
        spent_amount: spent,
        remaining_amount: remaining,
        created_at: r.created_at,
        updated_at: r.updated_at,
      };
    });
  }

  static createBudget(input: CreateBudgetInput): Budget {
    const db = getDatabase();
    const id = Crypto.randomUUID();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO budgets (id, category_id, period_start, period_end, amount_limit, created_at, updated_at, is_deleted)
         VALUES (?, ?, ?, ?, ?, ?, ?, 0)`,
        [id, input.category_id, input.period_start, input.period_end, input.amount_limit, now, now]
      );

      const payload = JSON.stringify({
        category_id: input.category_id,
        period_start: input.period_start,
        period_end: input.period_end,
        amount_limit: input.amount_limit,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('budget', ?, 'CREATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['budgets', 'sync_queue']);
    return this.getBudgets().find((b) => b.id === id)!;
  }
}
