import { getDatabase } from '../db/database';
import { apiRequest, ApiError } from '../services/apiClient';
import { ImageService } from '../services/imageService';
import { IdRemapper } from './idRemapper';
import { DatabaseNotifier } from '../db/notifier';
import { SyncQueueItem } from '../shared/types/domain';

export interface QueueProcessorResult {
  processedCount: number;
  failedCount: number;
  pausedReason?: 'offline' | 'transient_error' | 'auth_error';
}

export class QueueProcessor {
  private static isProcessing = false;

  static async processQueue(onAuthError?: () => void): Promise<QueueProcessorResult> {
    if (this.isProcessing) {
      return { processedCount: 0, failedCount: 0 };
    }

    this.isProcessing = true;
    let processedCount = 0;
    let failedCount = 0;

    try {
      const db = getDatabase();
      const items = db.getAllSync<SyncQueueItem>(
        `SELECT * FROM sync_queue WHERE status IN ('pending', 'failed') AND retry_count < 10 ORDER BY id ASC`
      );

      for (const item of items) {
        db.runSync(`UPDATE sync_queue SET status = 'processing' WHERE id = ?`, [item.id]);
        DatabaseNotifier.notify(['sync_queue']);

        try {
          const res = await this.executeMutation(item);

          if (res && res.id && res.id !== item.entity_id) {
            IdRemapper.remapId(item.entity_type, item.entity_id, res.id);
          }

          db.runSync(`DELETE FROM sync_queue WHERE id = ?`, [item.id]);
          processedCount++;
          DatabaseNotifier.notify(['sync_queue']);
        } catch (err: any) {
          if (err instanceof ApiError) {
            if (err.status === 401) {
              db.runSync(`UPDATE sync_queue SET status = 'pending' WHERE id = ?`, [item.id]);
              DatabaseNotifier.notify(['sync_queue']);
              if (onAuthError) onAuthError();
              return { processedCount, failedCount, pausedReason: 'auth_error' };
            }

            if (err.status === 0 || err.status >= 500) {
              db.runSync(
                `UPDATE sync_queue SET status = 'pending', retry_count = retry_count + 1, last_error = ? WHERE id = ?`,
                [err.message, item.id]
              );
              DatabaseNotifier.notify(['sync_queue']);
              return { processedCount, failedCount, pausedReason: 'transient_error' };
            }

            db.runSync(
              `UPDATE sync_queue SET status = 'failed', retry_count = retry_count + 1, last_error = ? WHERE id = ?`,
              [err.message, item.id]
            );
            failedCount++;
            DatabaseNotifier.notify(['sync_queue']);
          } else {
            db.runSync(
              `UPDATE sync_queue SET status = 'pending', retry_count = retry_count + 1, last_error = ? WHERE id = ?`,
              [String(err), item.id]
            );
            DatabaseNotifier.notify(['sync_queue']);
            return { processedCount, failedCount, pausedReason: 'transient_error' };
          }
        }
      }

      return { processedCount, failedCount };
    } finally {
      this.isProcessing = false;
    }
  }

  private static async executeMutation(item: SyncQueueItem): Promise<any> {
    const payload = JSON.parse(item.payload || '{}');

    switch (item.entity_type) {
      case 'task':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/tasks', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/tasks/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/tasks/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'project':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/projects', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/projects/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/projects/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'tag':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/tags', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/tags/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/tags/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'note':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/notes', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/notes/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/notes/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'note_image':
        if (item.operation === 'UPLOAD_IMAGE') {
          return ImageService.uploadNoteImage(payload.note_id, payload.image_id, payload.local_uri);
        }
        break;

      case 'account':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/accounts', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/accounts/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/accounts/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'category':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/categories', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/categories/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/categories/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'transaction':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/transactions', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/transactions/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/transactions/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      case 'transaction_receipt':
        if (item.operation === 'UPLOAD_RECEIPT') {
          return ImageService.uploadTransactionReceipt(payload.transaction_id, payload.local_uri);
        }
        break;

      case 'budget':
        if (item.operation === 'CREATE') {
          return apiRequest('/v1/budgets', { method: 'POST', body: JSON.stringify(payload) });
        } else if (item.operation === 'UPDATE') {
          return apiRequest(`/v1/budgets/${item.entity_id}`, { method: 'PATCH', body: JSON.stringify(payload) });
        } else if (item.operation === 'DELETE') {
          return apiRequest(`/v1/budgets/${item.entity_id}`, { method: 'DELETE' });
        }
        break;

      default:
        throw new Error(`Unsupported entity type: ${item.entity_type}`);
    }
  }
}
