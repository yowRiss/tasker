import { getDatabase } from '../db/database';
import { apiRequest } from '../services/apiClient';
import { DatabaseNotifier } from '../db/notifier';
import { Task, Project, Tag, Note, Account, Category, Transaction, Budget } from '../shared/types/domain';

export class PullSync {
  static async pullAll(): Promise<void> {
    await Promise.all([
      this.pullProjects(),
      this.pullTags(),
      this.pullTasks(),
      this.pullNotes(),
      this.pullAccounts(),
      this.pullCategories(),
      this.pullTransactions(),
      this.pullBudgets(),
    ]);

    const db = getDatabase();
    const now = new Date().toISOString();
    db.runSync(
      `INSERT OR REPLACE INTO sync_metadata (table_name, last_synced_at) VALUES ('all', ?)`,
      [now]
    );

    DatabaseNotifier.notify(['tasks', 'projects', 'tags', 'notes', 'accounts', 'categories', 'transactions', 'budgets']);
  }

  static async pullProjects(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Project[] }>('/v1/projects');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'project'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM projects WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO projects (id, name, color, is_archived, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, ?, 0)`,
              [item.id, item.name, item.color || null, item.is_archived ? 1 : 0, item.created_at, item.updated_at]
            );
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync projects error:', err);
    }
  }

  static async pullTags(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Tag[] }>('/v1/tags');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'tag'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM tags WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO tags (id, name, color, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, 0)`,
              [item.id, item.name, item.color || null, item.created_at, item.updated_at]
            );
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync tags error:', err);
    }
  }

  static async pullTasks(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Task[] }>('/v1/tasks?status=all&limit=1000');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'task'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM tasks WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO tasks (id, title, description, status, completed_at, due_date, priority, project_id, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`,
              [
                item.id,
                item.title,
                item.description || null,
                item.status,
                item.completed_at || null,
                item.due_date || null,
                item.priority,
                item.project_id || null,
                item.created_at,
                item.updated_at,
              ]
            );

            if (item.tags) {
              db.runSync(`DELETE FROM task_tags WHERE task_id = ?`, [item.id]);
              for (const tag of item.tags) {
                db.runSync(
                  `INSERT OR REPLACE INTO task_tags (task_id, tag_id, created_at) VALUES (?, ?, ?)`,
                  [item.id, tag.id, new Date().toISOString()]
                );
              }
            }

            if (item.subtasks) {
              db.runSync(`DELETE FROM subtasks WHERE task_id = ?`, [item.id]);
              for (const st of item.subtasks) {
                db.runSync(
                  `INSERT OR REPLACE INTO subtasks (id, task_id, title, completed, position, created_at, updated_at, is_deleted)
                   VALUES (?, ?, ?, ?, ?, ?, ?, 0)`,
                  [st.id, item.id, st.title, st.completed ? 1 : 0, st.position, st.created_at, st.updated_at]
                );
              }
            }
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync tasks error:', err);
    }
  }

  static async pullNotes(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Note[] }>('/v1/notes?limit=1000');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'note'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM notes WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO notes (id, title, content_md, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, 0)`,
              [item.id, item.title, item.content_md || '', item.created_at, item.updated_at]
            );

            if (item.tags) {
              db.runSync(`DELETE FROM note_tags WHERE note_id = ?`, [item.id]);
              for (const tag of item.tags) {
                db.runSync(
                  `INSERT OR REPLACE INTO note_tags (note_id, tag_id, created_at) VALUES (?, ?, ?)`,
                  [item.id, tag.id, new Date().toISOString()]
                );
              }
            }
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync notes error:', err);
    }
  }

  static async pullAccounts(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Account[] }>('/v1/accounts');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'account'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM accounts WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO accounts (id, name, account_type, currency, archived_at, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, 'IDR', ?, ?, ?, 0)`,
              [item.id, item.name, item.account_type, item.archived_at || null, item.created_at, item.updated_at]
            );
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync accounts error:', err);
    }
  }

  static async pullCategories(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Category[] }>('/v1/categories');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'category'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM categories WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO categories (id, name, category_type, icon, color, archived_at, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)`,
              [item.id, item.name, item.category_type, item.icon || null, item.color || null, item.archived_at || null, item.created_at, item.updated_at]
            );
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync categories error:', err);
    }
  }

  static async pullTransactions(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Transaction[] }>('/v1/transactions?limit=1000');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'transaction'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM transactions WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO transactions (id, transaction_type, amount, transaction_date, account_id, transfer_account_id, category_id, description, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`,
              [
                item.id,
                item.transaction_type,
                item.amount,
                item.transaction_date,
                item.account_id,
                item.transfer_account_id || null,
                item.category_id || null,
                item.description || null,
                item.created_at,
                item.updated_at,
              ]
            );
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync transactions error:', err);
    }
  }

  static async pullBudgets(): Promise<void> {
    try {
      const res = await apiRequest<{ items: Budget[] }>('/v1/budgets');
      if (!res?.items) return;

      const db = getDatabase();
      const pendingEntities = new Set(
        db.getAllSync<{ entity_id: string }>(
          `SELECT entity_id FROM sync_queue WHERE entity_type = 'budget'`
        ).map((r) => r.entity_id)
      );

      db.withTransactionSync(() => {
        for (const item of res.items) {
          if (pendingEntities.has(item.id)) continue;

          const existing = db.getFirstSync<{ updated_at: string }>(
            `SELECT updated_at FROM budgets WHERE id = ?`,
            [item.id]
          );

          if (!existing || new Date(item.updated_at) >= new Date(existing.updated_at)) {
            db.runSync(
              `INSERT OR REPLACE INTO budgets (id, category_id, period_start, period_end, amount_limit, created_at, updated_at, is_deleted)
               VALUES (?, ?, ?, ?, ?, ?, ?, 0)`,
              [item.id, item.category_id, item.period_start, item.period_end, item.amount_limit, item.created_at, item.updated_at]
            );
          }
        }
      });
    } catch (err) {
      console.warn('Pull sync budgets error:', err);
    }
  }
}
