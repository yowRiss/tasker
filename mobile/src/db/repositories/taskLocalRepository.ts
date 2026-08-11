import * as Crypto from 'expo-crypto';
import { getDatabase } from '../database';
import { DatabaseNotifier } from '../notifier';
import { Task, Subtask, Project, Tag } from '../../shared/types/domain';

export interface TaskFilters {
  status?: 'all' | 'open' | 'completed';
  project_id?: string;
  tag_id?: string;
  priority?: number;
  search?: string;
}

export interface CreateTaskInput {
  title: string;
  description?: string;
  due_date?: string;
  priority?: number;
  project_id?: string;
  tag_ids?: string[];
  subtasks?: string[];
}

export interface UpdateTaskInput {
  title?: string;
  description?: string;
  due_date?: string | null;
  priority?: number;
  project_id?: string | null;
  tag_ids?: string[];
  subtasks?: { id?: string; title: string; completed?: boolean }[];
}

export class TaskLocalRepository {
  static getProjects(): Project[] {
    const db = getDatabase();
    return db.getAllSync<Project>(
      `SELECT * FROM projects WHERE is_deleted = 0 ORDER BY LOWER(name) ASC`
    );
  }

  static createProject(name: string, color?: string): Project {
    const db = getDatabase();
    const id = Crypto.randomUUID();
    const now = new Date().toISOString();

    db.runSync(
      `INSERT INTO projects (id, name, color, is_archived, created_at, updated_at, is_deleted)
       VALUES (?, ?, ?, 0, ?, ?, 0)`,
      [id, name.trim(), color || null, now, now]
    );

    const payload = JSON.stringify({ name: name.trim(), color });
    db.runSync(
      `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
       VALUES ('project', ?, 'CREATE', ?, ?, 'pending')`,
      [id, payload, now]
    );

    DatabaseNotifier.notify(['projects', 'sync_queue']);
    return { id, name: name.trim(), color, is_archived: false, created_at: now, updated_at: now };
  }

  static getTags(): Tag[] {
    const db = getDatabase();
    return db.getAllSync<Tag>(
      `SELECT * FROM tags WHERE is_deleted = 0 ORDER BY LOWER(name) ASC`
    );
  }

  static createTag(name: string, color?: string): Tag {
    const db = getDatabase();
    const id = Crypto.randomUUID();
    const now = new Date().toISOString();

    db.runSync(
      `INSERT INTO tags (id, name, color, created_at, updated_at, is_deleted)
       VALUES (?, ?, ?, ?, ?, 0)`,
      [id, name.trim(), color || null, now, now]
    );

    const payload = JSON.stringify({ name: name.trim(), color });
    db.runSync(
      `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
       VALUES ('tag', ?, 'CREATE', ?, ?, 'pending')`,
      [id, payload, now]
    );

    DatabaseNotifier.notify(['tags', 'sync_queue']);
    return { id, name: name.trim(), color, created_at: now, updated_at: now };
  }

  static getTasks(filters: TaskFilters = {}): Task[] {
    const db = getDatabase();
    const conditions: string[] = ['t.is_deleted = 0'];
    const params: any[] = [];

    if (filters.status && filters.status !== 'all') {
      conditions.push('t.status = ?');
      params.push(filters.status);
    }
    if (filters.project_id) {
      conditions.push('t.project_id = ?');
      params.push(filters.project_id);
    }
    if (filters.priority !== undefined && filters.priority !== null) {
      conditions.push('t.priority = ?');
      params.push(filters.priority);
    }
    if (filters.search && filters.search.trim()) {
      conditions.push('(t.title LIKE ? OR t.description LIKE ?)');
      const q = `%${filters.search.trim()}%`;
      params.push(q, q);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
    const sql = `
      SELECT t.*, p.name as project_name, p.color as project_color
      FROM tasks t
      LEFT JOIN projects p ON t.project_id = p.id
      ${whereClause}
      ORDER BY 
        CASE WHEN t.due_date IS NULL THEN 1 ELSE 0 END ASC,
        t.due_date ASC,
        t.priority DESC,
        t.updated_at DESC
    `;

    const rawRows = db.getAllSync<any>(sql, params);

    return rawRows.map((row) => {
      const task: Task = {
        id: row.id,
        title: row.title,
        description: row.description,
        status: row.status,
        completed_at: row.completed_at,
        due_date: row.due_date,
        priority: row.priority,
        project_id: row.project_id,
        project: row.project_id ? {
          id: row.project_id,
          name: row.project_name,
          color: row.project_color,
          is_archived: false,
          created_at: '',
          updated_at: '',
        } : null,
        created_at: row.created_at,
        updated_at: row.updated_at,
      };

      // Fetch tags for this task
      task.tags = db.getAllSync<Tag>(
        `SELECT tg.* FROM tags tg
         INNER JOIN task_tags tt ON tg.id = tt.tag_id
         WHERE tt.task_id = ? AND tg.is_deleted = 0`,
        [row.id]
      );

      // Fetch subtasks
      task.subtasks = db.getAllSync<Subtask>(
        `SELECT * FROM subtasks WHERE task_id = ? AND is_deleted = 0 ORDER BY position ASC`,
        [row.id]
      );

      return task;
    });
  }

  static getTask(id: string): Task | null {
    const tasks = this.getTasks();
    return tasks.find((t) => t.id === id) || null;
  }

  static createTask(input: CreateTaskInput): Task {
    const db = getDatabase();
    const taskId = Crypto.randomUUID();
    const now = new Date().toISOString();
    const priority = input.priority !== undefined ? input.priority : 0;

    db.withTransactionSync(() => {
      // 1. Insert task
      db.runSync(
        `INSERT INTO tasks (id, title, description, status, due_date, priority, project_id, created_at, updated_at, is_deleted)
         VALUES (?, ?, ?, 'open', ?, ?, ?, ?, ?, 0)`,
        [
          taskId,
          input.title.trim(),
          input.description?.trim() || null,
          input.due_date || null,
          priority,
          input.project_id || null,
          now,
          now,
        ]
      );

      // 2. Insert tags
      if (input.tag_ids && input.tag_ids.length > 0) {
        for (const tagId of input.tag_ids) {
          db.runSync(
            `INSERT INTO task_tags (task_id, tag_id, created_at) VALUES (?, ?, ?)`,
            [taskId, tagId, now]
          );
        }
      }

      // 3. Insert subtasks
      if (input.subtasks && input.subtasks.length > 0) {
        let pos = 0;
        for (const subTitle of input.subtasks) {
          if (!subTitle.trim()) continue;
          const subId = Crypto.randomUUID();
          db.runSync(
            `INSERT INTO subtasks (id, task_id, title, completed, position, created_at, updated_at, is_deleted)
             VALUES (?, ?, ?, 0, ?, ?, ?, 0)`,
            [subId, taskId, subTitle.trim(), pos++, now, now]
          );
        }
      }

      // 4. Enqueue in sync_queue
      const payload = JSON.stringify({
        id: taskId,
        title: input.title.trim(),
        description: input.description?.trim() || null,
        due_date: input.due_date || null,
        priority,
        project_id: input.project_id || null,
        tags: input.tag_ids || [],
        subtasks: (input.subtasks || []).map((s) => ({ title: s.trim() })),
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('task', ?, 'CREATE', ?, ?, 'pending')`,
        [taskId, payload, now]
      );
    });

    DatabaseNotifier.notify(['tasks', 'sync_queue']);
    return this.getTask(taskId)!;
  }

  static updateTask(id: string, input: UpdateTaskInput): Task {
    const db = getDatabase();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      // 1. Update task fields
      const sets: string[] = ['updated_at = ?'];
      const params: any[] = [now];

      if (input.title !== undefined) {
        sets.push('title = ?');
        params.push(input.title.trim());
      }
      if (input.description !== undefined) {
        sets.push('description = ?');
        params.push(input.description ? input.description.trim() : null);
      }
      if (input.due_date !== undefined) {
        sets.push('due_date = ?');
        params.push(input.due_date);
      }
      if (input.priority !== undefined) {
        sets.push('priority = ?');
        params.push(input.priority);
      }
      if (input.project_id !== undefined) {
        sets.push('project_id = ?');
        params.push(input.project_id);
      }

      params.push(id);
      db.runSync(`UPDATE tasks SET ${sets.join(', ')} WHERE id = ?`, params);

      // 2. Update tags if supplied
      if (input.tag_ids !== undefined) {
        db.runSync(`DELETE FROM task_tags WHERE task_id = ?`, [id]);
        for (const tagId of input.tag_ids) {
          db.runSync(
            `INSERT INTO task_tags (task_id, tag_id, created_at) VALUES (?, ?, ?)`,
            [id, tagId, now]
          );
        }
      }

      // 3. Update subtasks if supplied
      if (input.subtasks !== undefined) {
        db.runSync(`DELETE FROM subtasks WHERE task_id = ?`, [id]);
        let pos = 0;
        for (const sub of input.subtasks) {
          if (!sub.title.trim()) continue;
          const subId = sub.id || Crypto.randomUUID();
          db.runSync(
            `INSERT INTO subtasks (id, task_id, title, completed, position, created_at, updated_at, is_deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, 0)`,
            [subId, id, sub.title.trim(), sub.completed ? 1 : 0, pos++, now, now]
          );
        }
      }

      // 4. Enqueue UPDATE payload
      const payload = JSON.stringify({
        title: input.title?.trim(),
        description: input.description !== undefined ? (input.description ? input.description.trim() : null) : undefined,
        due_date: input.due_date,
        priority: input.priority,
        project_id: input.project_id,
        tags: input.tag_ids,
        subtasks: input.subtasks ? input.subtasks.map((s) => ({ id: s.id, title: s.title.trim(), completed: s.completed })) : undefined,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('task', ?, 'UPDATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['tasks', 'sync_queue']);
    return this.getTask(id)!;
  }

  static toggleTaskCompletion(id: string): Task {
    const db = getDatabase();
    const now = new Date().toISOString();
    const task = this.getTask(id);
    if (!task) throw new Error('Task not found');

    const newStatus = task.status === 'open' ? 'completed' : 'open';
    const completedAt = newStatus === 'completed' ? now : null;

    db.withTransactionSync(() => {
      db.runSync(
        `UPDATE tasks SET status = ?, completed_at = ?, updated_at = ? WHERE id = ?`,
        [newStatus, completedAt, now, id]
      );

      const payload = JSON.stringify({
        status: newStatus,
        completed_at: completedAt,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('task', ?, 'UPDATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['tasks', 'sync_queue']);
    return this.getTask(id)!;
  }

  static deleteTask(id: string): void {
    const db = getDatabase();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `UPDATE tasks SET is_deleted = 1, updated_at = ? WHERE id = ?`,
        [now, id]
      );

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('task', ?, 'DELETE', '{}', ?, 'pending')`,
        [id, now]
      );
    });

    DatabaseNotifier.notify(['tasks', 'sync_queue']);
  }
}
