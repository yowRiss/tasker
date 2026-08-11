import { CREATE_TABLES_SQL } from '../db/schema';
import * as SQLite from 'expo-sqlite';

export function runPureDatabaseVerification(): { success: boolean; details: string[] } {
  const details: string[] = [];
  details.push('Initializing SQLite test database...');

  try {
    const db = SQLite.openDatabaseSync('verification_test.db');
    db.execSync(CREATE_TABLES_SQL);
    details.push('Schema creation passed!');

    // 1. Insert offline task
    const taskId = 'test-task-uuid-101';
    const now = new Date().toISOString();
    db.runSync(
      `INSERT INTO tasks (id, title, description, status, due_date, priority, created_at, updated_at, is_deleted)
       VALUES (?, 'Verification Task', 'Offline task body', 'open', '2026-08-20', 2, ?, ?, 0)`,
      [taskId, now, now]
    );

    // 2. Insert queue item
    db.runSync(
      `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
       VALUES ('task', ?, 'CREATE', '{"title":"Verification Task"}', ?, 'pending')`,
      [taskId, now]
    );

    const pendingCount = db.getFirstSync<{ cnt: number }>(
      `SELECT COUNT(*) as cnt FROM sync_queue WHERE status = 'pending'`
    )?.cnt;
    details.push(`Pending queue item count: ${pendingCount}`);

    // 3. Test ID Remap transaction
    const canonicalServerId = 'server-uuid-5555';
    db.withTransactionSync(() => {
      db.runSync(`UPDATE tasks SET id = ? WHERE id = ?`, [canonicalServerId, taskId]);
      db.runSync(`UPDATE sync_queue SET entity_id = ? WHERE entity_id = ?`, [canonicalServerId, taskId]);
    });

    const remappedRow = db.getFirstSync<{ id: string }>(`SELECT id FROM tasks WHERE id = ?`, [canonicalServerId]);
    if (!remappedRow) throw new Error('Failed to remap ID in tasks table');
    details.push(`Remapped ID verified: ${remappedRow.id}`);

    details.push('Verification pass 100% successful!');
    return { success: true, details };
  } catch (err: any) {
    details.push(`Error: ${err.message}`);
    return { success: false, details };
  }
}
