import { useState, useEffect } from 'react';
import { SyncManager } from '../../../sync/syncManager';
import { getDatabase } from '../../../db/database';
import { SyncQueueItem } from '../../../shared/types/domain';

export function useSyncState() {
  const [syncState, setSyncState] = useState({
    isOnline: true,
    isSyncing: false,
    pendingCount: 0,
    failedCount: 0,
  });

  const [queueItems, setQueueItems] = useState<SyncQueueItem[]>([]);

  const loadQueueItems = () => {
    try {
      const db = getDatabase();
      const items = db.getAllSync<SyncQueueItem>(`SELECT * FROM sync_queue ORDER BY id ASC`);
      setQueueItems(items);
    } catch {
      setQueueItems([]);
    }
  };

  useEffect(() => {
    const unsub = SyncManager.subscribe((state: { isOnline: boolean; isSyncing: boolean; pendingCount: number; failedCount: number }) => {
      setSyncState(state);
      loadQueueItems();
    });
    return unsub;
  }, []);

  const triggerSync = () => {
    SyncManager.triggerSync();
  };

  const retryFailedItem = (id: number) => {
    try {
      const db = getDatabase();
      db.runSync(`UPDATE sync_queue SET status = 'pending', retry_count = 0 WHERE id = ?`, [id]);
      loadQueueItems();
      SyncManager.triggerSync();
    } catch (err) {
      console.error('Error retrying queue item:', err);
    }
  };

  const deleteQueueItem = (id: number) => {
    try {
      const db = getDatabase();
      db.runSync(`DELETE FROM sync_queue WHERE id = ?`, [id]);
      loadQueueItems();
    } catch (err) {
      console.error('Error deleting queue item:', err);
    }
  };

  return {
    ...syncState,
    queueItems,
    triggerSync,
    retryFailedItem,
    deleteQueueItem,
    refreshQueue: loadQueueItems,
  };
}
