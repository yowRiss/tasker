import { getDatabase } from '../db/database';
import { DatabaseNotifier } from '../db/notifier';

export class IdRemapper {
  static remapId(entityType: string, oldId: string, newId: string) {
    if (oldId === newId) return;

    const db = getDatabase();

    db.withTransactionSync(() => {
      if (entityType === 'task') {
        db.runSync(`UPDATE tasks SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE subtasks SET task_id = ? WHERE task_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE task_tags SET task_id = ? WHERE task_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE note_task_links SET task_id = ? WHERE task_id = ?`, [newId, oldId]);
      } else if (entityType === 'project') {
        db.runSync(`UPDATE projects SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE tasks SET project_id = ? WHERE project_id = ?`, [newId, oldId]);
      } else if (entityType === 'tag') {
        db.runSync(`UPDATE tags SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE task_tags SET tag_id = ? WHERE tag_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE note_tags SET tag_id = ? WHERE tag_id = ?`, [newId, oldId]);
      } else if (entityType === 'note') {
        db.runSync(`UPDATE notes SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE note_tags SET note_id = ? WHERE note_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE note_task_links SET note_id = ? WHERE note_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE note_images SET note_id = ? WHERE note_id = ?`, [newId, oldId]);
      } else if (entityType === 'account') {
        db.runSync(`UPDATE accounts SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE transactions SET account_id = ? WHERE account_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE transactions SET transfer_account_id = ? WHERE transfer_account_id = ?`, [newId, oldId]);
      } else if (entityType === 'category') {
        db.runSync(`UPDATE categories SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE transactions SET category_id = ? WHERE category_id = ?`, [newId, oldId]);
        db.runSync(`UPDATE budgets SET category_id = ? WHERE category_id = ?`, [newId, oldId]);
      } else if (entityType === 'transaction') {
        db.runSync(`UPDATE transactions SET id = ? WHERE id = ?`, [newId, oldId]);
        db.runSync(`UPDATE transaction_receipts SET transaction_id = ? WHERE transaction_id = ?`, [newId, oldId]);
      } else if (entityType === 'budget') {
        db.runSync(`UPDATE budgets SET id = ? WHERE id = ?`, [newId, oldId]);
      }

      db.runSync(`UPDATE sync_queue SET entity_id = ? WHERE entity_id = ?`, [newId, oldId]);
    });

    DatabaseNotifier.notify(['tasks', 'projects', 'tags', 'notes', 'accounts', 'categories', 'transactions', 'budgets', 'sync_queue']);
  }
}
