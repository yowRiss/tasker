import { NetworkMonitor } from './netInfoListener';
import { QueueProcessor } from './queueProcessor';
import { PullSync } from './pullSync';
import { getDatabase } from '../db/database';
import { DatabaseNotifier } from '../db/notifier';

type SyncStateListener = (state: {
  isOnline: boolean;
  isSyncing: boolean;
  pendingCount: number;
  failedCount: number;
}) => void;

class SyncManagerImpl {
  private isSyncingState = false;
  private listeners: Set<SyncStateListener> = new Set();
  private autoSyncTimer: any = null;
  private onAuthErrorCallback?: () => void;

  constructor() {
    this.init();
  }

  private init() {
    NetworkMonitor.subscribe((isOnline) => {
      this.notifyState();
      if (isOnline) {
        this.triggerSync();
      }
    });

    DatabaseNotifier.subscribe('sync_queue', () => {
      this.notifyState();
    });

    // Periodic sync every 60 seconds when online
    this.autoSyncTimer = setInterval(() => {
      if (NetworkMonitor.isOnline && !this.isSyncingState) {
        this.triggerSync();
      }
    }, 60000);
  }

  setOnAuthError(callback: () => void) {
    this.onAuthErrorCallback = callback;
  }

  get isSyncing(): boolean {
    return this.isSyncingState;
  }

  getCounts(): { pendingCount: number; failedCount: number } {
    try {
      const db = getDatabase();
      const rows = db.getAllSync<{ status: string; cnt: number }>(
        `SELECT status, COUNT(*) as cnt FROM sync_queue GROUP BY status`
      );

      let pendingCount = 0;
      let failedCount = 0;

      for (const r of rows) {
        if (r.status === 'pending' || r.status === 'processing') {
          pendingCount += r.cnt;
        } else if (r.status === 'failed') {
          failedCount += r.cnt;
        }
      }

      return { pendingCount, failedCount };
    } catch {
      return { pendingCount: 0, failedCount: 0 };
    }
  }

  subscribe(listener: SyncStateListener): () => void {
    this.listeners.add(listener);
    this.notifyState();
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyState() {
    const { pendingCount, failedCount } = this.getCounts();
    const state = {
      isOnline: NetworkMonitor.isOnline,
      isSyncing: this.isSyncingState,
      pendingCount,
      failedCount,
    };
    this.listeners.forEach((l) => l(state));
  }

  async triggerSync(): Promise<void> {
    if (this.isSyncingState || !NetworkMonitor.isOnline) return;

    this.isSyncingState = true;
    this.notifyState();

    try {
      // 1. Push pending local mutations
      const pushResult = await QueueProcessor.processQueue(this.onAuthErrorCallback);

      if (pushResult.pausedReason === 'auth_error' || pushResult.pausedReason === 'offline') {
        return;
      }

      // 2. Pull server updates
      await PullSync.pullAll();
    } catch (err) {
      console.warn('SyncManager error:', err);
    } finally {
      this.isSyncingState = false;
      this.notifyState();
    }
  }
}

export const SyncManager = new SyncManagerImpl();
