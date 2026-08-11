import { TaskLocalRepository } from '../db/repositories/taskLocalRepository';
import { NoteLocalRepository } from '../db/repositories/noteLocalRepository';
import { MoneyLocalRepository } from '../db/repositories/moneyLocalRepository';
import { getDatabase } from '../db/database';
import { IdRemapper } from './idRemapper';
import { SyncQueueItem } from '../shared/types/domain';

export class SyncVerification {
  static runOfflineVerificationPass(): {
    success: boolean;
    queueCount: number;
    log: string[];
  } {
    const log: string[] = [];
    log.push('Starting Phase 6 Verification Pass...');

    try {
      // 1. Create Task Offline
      const task = TaskLocalRepository.createTask({
        title: 'Airplane Mode Verification Task',
        description: 'Testing offline queue insertion',
        priority: 2,
        due_date: '2026-08-30',
        subtasks: ['Subtask 1', 'Subtask 2'],
      });
      log.push(`Created task offline with local ID: ${task.id}`);

      // 2. Create Note Offline
      const note = NoteLocalRepository.createNote({
        title: 'Offline Note Verification',
        content_md: '# Heading\nOffline note content created during airplane mode.',
      });
      log.push(`Created note offline with local ID: ${note.id}`);

      // 3. Create Account & Transaction Offline
      const acc = MoneyLocalRepository.createAccount({
        name: 'Offline Cash Vault',
        account_type: 'cash',
      });
      log.push(`Created account offline with local ID: ${acc.id}`);

      const cat = MoneyLocalRepository.createCategory({
        name: 'Groceries Offline',
        category_type: 'expense',
      });

      const tx = MoneyLocalRepository.createTransaction({
        transaction_type: 'expense',
        amount: 75000,
        transaction_date: '2026-08-11',
        account_id: acc.id,
        category_id: cat.id,
        description: 'Supermarket purchase offline',
      });
      log.push(`Created transaction offline with local ID: ${tx.id}`);

      // 4. Verify SQLite Queue
      const db = getDatabase();
      const queueItems = db.getAllSync<SyncQueueItem>(`SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY id ASC`);
      log.push(`Total pending mutations in sync_queue: ${queueItems.length}`);

      // 5. Test ID Remapping Simulation
      const serverTaskId = 'server-uuid-task-9999';
      IdRemapper.remapId('task', task.id, serverTaskId);
      const remappedTask = TaskLocalRepository.getTask(serverTaskId);
      if (!remappedTask) {
        throw new Error('ID Remapper failed to translate task primary key');
      }
      log.push(`Successfully remapped task ID ${task.id} -> ${serverTaskId}`);

      log.push('Offline verification pass completed successfully!');
      return { success: true, queueCount: queueItems.length, log };
    } catch (err: any) {
      log.push(`Verification error: ${err.message}`);
      return { success: false, queueCount: 0, log };
    }
  }
}
