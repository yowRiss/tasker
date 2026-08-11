import { useState, useEffect } from 'react';
import { getDatabase } from '../../../db/database';
import { DatabaseNotifier } from '../../../db/notifier';

export function useItemSyncStatus(entityType: string, entityId: string) {
  const [status, setStatus] = useState<{ isPending: boolean; isFailed: boolean; errorText?: string }>({
    isPending: false,
    isFailed: false,
  });

  const checkStatus = () => {
    try {
      const db = getDatabase();
      const row = db.getFirstSync<{ status: string; last_error: string }>(
        `SELECT status, last_error FROM sync_queue WHERE entity_type = ? AND entity_id = ? ORDER BY id DESC LIMIT 1`,
        [entityType, entityId]
      );

      if (row) {
        setStatus({
          isPending: row.status === 'pending' || row.status === 'processing',
          isFailed: row.status === 'failed',
          errorText: row.last_error,
        });
      } else {
        setStatus({ isPending: false, isFailed: false });
      }
    } catch {
      setStatus({ isPending: false, isFailed: false });
    }
  };

  useEffect(() => {
    checkStatus();
    const unsub = DatabaseNotifier.subscribe('sync_queue', checkStatus);
    return unsub;
  }, [entityType, entityId]);

  return status;
}
