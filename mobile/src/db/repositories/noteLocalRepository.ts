import * as Crypto from 'expo-crypto';
import { getDatabase } from '../database';
import { DatabaseNotifier } from '../notifier';
import { Note, NoteImage, Tag } from '../../shared/types/domain';
import { LocalPickedImage } from '../../services/imageService';

export interface CreateNoteInput {
  title: string;
  content_md?: string;
  tag_ids?: string[];
  task_ids?: string[];
}

export interface UpdateNoteInput {
  title?: string;
  content_md?: string;
  tag_ids?: string[];
  task_ids?: string[];
}

export class NoteLocalRepository {
  static getNotes(search?: string): Note[] {
    const db = getDatabase();
    const conditions: string[] = ['n.is_deleted = 0'];
    const params: any[] = [];

    if (search && search.trim()) {
      conditions.push('(n.title LIKE ? OR n.content_md LIKE ?)');
      const q = `%${search.trim()}%`;
      params.push(q, q);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;
    const sql = `SELECT * FROM notes n ${whereClause} ORDER BY n.updated_at DESC`;
    const rows = db.getAllSync<any>(sql, params);

    return rows.map((r) => {
      const note: Note = {
        id: r.id,
        title: r.title,
        content_md: r.content_md,
        created_at: r.created_at,
        updated_at: r.updated_at,
      };

      note.tags = db.getAllSync<Tag>(
        `SELECT tg.* FROM tags tg
         INNER JOIN note_tags nt ON tg.id = nt.tag_id
         WHERE nt.note_id = ? AND tg.is_deleted = 0`,
        [r.id]
      );

      const links = db.getAllSync<{ task_id: string }>(
        `SELECT task_id FROM note_task_links WHERE note_id = ?`,
        [r.id]
      );
      note.linked_task_ids = links.map((l) => l.task_id);

      note.images = db.getAllSync<NoteImage>(
        `SELECT * FROM note_images WHERE note_id = ?`,
        [r.id]
      );

      return note;
    });
  }

  static getNote(id: string): Note | null {
    const notes = this.getNotes();
    return notes.find((n) => n.id === id) || null;
  }

  static createNote(input: CreateNoteInput): Note {
    const db = getDatabase();
    const noteId = Crypto.randomUUID();
    const now = new Date().toISOString();
    const title = input.title.trim();
    const content = input.content_md || '';

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO notes (id, title, content_md, created_at, updated_at, is_deleted)
         VALUES (?, ?, ?, ?, ?, 0)`,
        [noteId, title, content, now, now]
      );

      if (input.tag_ids && input.tag_ids.length > 0) {
        for (const tagId of input.tag_ids) {
          db.runSync(
            `INSERT INTO note_tags (note_id, tag_id, created_at) VALUES (?, ?, ?)`,
            [noteId, tagId, now]
          );
        }
      }

      if (input.task_ids && input.task_ids.length > 0) {
        for (const taskId of input.task_ids) {
          db.runSync(
            `INSERT INTO note_task_links (note_id, task_id, created_at) VALUES (?, ?, ?)`,
            [noteId, taskId, now]
          );
        }
      }

      const payload = JSON.stringify({
        id: noteId,
        title,
        content_md: content,
        tags: input.tag_ids || [],
        tasks: input.task_ids || [],
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('note', ?, 'CREATE', ?, ?, 'pending')`,
        [noteId, payload, now]
      );
    });

    DatabaseNotifier.notify(['notes', 'sync_queue']);
    return this.getNote(noteId)!;
  }

  static updateNote(id: string, input: UpdateNoteInput): Note {
    const db = getDatabase();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      const sets: string[] = ['updated_at = ?'];
      const params: any[] = [now];

      if (input.title !== undefined) {
        sets.push('title = ?');
        params.push(input.title.trim());
      }
      if (input.content_md !== undefined) {
        sets.push('content_md = ?');
        params.push(input.content_md);
      }

      params.push(id);
      db.runSync(`UPDATE notes SET ${sets.join(', ')} WHERE id = ?`, params);

      if (input.tag_ids !== undefined) {
        db.runSync(`DELETE FROM note_tags WHERE note_id = ?`, [id]);
        for (const tagId of input.tag_ids) {
          db.runSync(
            `INSERT INTO note_tags (note_id, tag_id, created_at) VALUES (?, ?, ?)`,
            [id, tagId, now]
          );
        }
      }

      if (input.task_ids !== undefined) {
        db.runSync(`DELETE FROM note_task_links WHERE note_id = ?`, [id]);
        for (const taskId of input.task_ids) {
          db.runSync(
            `INSERT INTO note_task_links (note_id, task_id, created_at) VALUES (?, ?, ?)`,
            [id, taskId, now]
          );
        }
      }

      const payload = JSON.stringify({
        title: input.title?.trim(),
        content_md: input.content_md,
        tags: input.tag_ids,
        tasks: input.task_ids,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('note', ?, 'UPDATE', ?, ?, 'pending')`,
        [id, payload, now]
      );
    });

    DatabaseNotifier.notify(['notes', 'sync_queue']);
    return this.getNote(id)!;
  }

  static deleteNote(id: string): void {
    const db = getDatabase();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(`UPDATE notes SET is_deleted = 1, updated_at = ? WHERE id = ?`, [now, id]);

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('note', ?, 'DELETE', '{}', ?, 'pending')`,
        [id, now]
      );
    });

    DatabaseNotifier.notify(['notes', 'sync_queue']);
  }

  static addNoteImage(noteId: string, image: LocalPickedImage, altText?: string): NoteImage {
    const db = getDatabase();
    const now = new Date().toISOString();

    db.withTransactionSync(() => {
      db.runSync(
        `INSERT INTO note_images (id, note_id, bucket_id, local_uri, original_filename, mime_type, byte_size, alt_text, width, height, created_at, sync_status)
         VALUES (?, ?, 'note-images', ?, ?, ?, ?, ?, ?, ?, ?, 'pending')`,
        [
          image.id,
          noteId,
          image.localUri,
          image.originalFilename,
          image.mimeType,
          image.byteSize,
          altText || null,
          image.width || null,
          image.height || null,
          now,
        ]
      );

      const payload = JSON.stringify({
        note_id: noteId,
        image_id: image.id,
        local_uri: image.localUri,
        original_filename: image.originalFilename,
        mime_type: image.mimeType,
        byte_size: image.byteSize,
        alt_text: altText || null,
      });

      db.runSync(
        `INSERT INTO sync_queue (entity_type, entity_id, operation, payload, created_at, status)
         VALUES ('note_image', ?, 'UPLOAD_IMAGE', ?, ?, 'pending')`,
        [image.id, payload, now]
      );
    });

    DatabaseNotifier.notify(['notes', 'sync_queue']);

    return {
      id: image.id,
      note_id: noteId,
      bucket_id: 'note-images',
      local_uri: image.localUri,
      original_filename: image.originalFilename,
      mime_type: image.mimeType,
      byte_size: image.byteSize,
      alt_text: altText || null,
      width: image.width || null,
      height: image.height || null,
      created_at: now,
      sync_status: 'pending',
    };
  }
}
