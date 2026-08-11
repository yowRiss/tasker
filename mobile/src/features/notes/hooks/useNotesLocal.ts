import { useState, useEffect, useCallback } from 'react';
import { NoteLocalRepository, CreateNoteInput, UpdateNoteInput } from '../../../db/repositories/noteLocalRepository';
import { TaskLocalRepository } from '../../../db/repositories/taskLocalRepository';
import { DatabaseNotifier } from '../../../db/notifier';
import { Note, Tag, Task } from '../../../shared/types/domain';
import { LocalPickedImage } from '../../../services/imageService';

export function useNotesLocal(searchQuery: string = '') {
  const [notes, setNotes] = useState<Note[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(() => {
    try {
      const noteList = NoteLocalRepository.getNotes(searchQuery);
      const tagList = TaskLocalRepository.getTags();
      const taskList = TaskLocalRepository.getTasks({ status: 'all' });

      setNotes(noteList);
      setTags(tagList);
      setTasks(taskList);
    } catch (err) {
      console.error('Error loading notes:', err);
    } finally {
      setLoading(false);
    }
  }, [searchQuery]);

  useEffect(() => {
    loadData();

    const unsubNotes = DatabaseNotifier.subscribe('notes', loadData);
    const unsubTags = DatabaseNotifier.subscribe('tags', loadData);
    const unsubTasks = DatabaseNotifier.subscribe('tasks', loadData);

    return () => {
      unsubNotes();
      unsubTags();
      unsubTasks();
    };
  }, [loadData]);

  const createNote = (input: CreateNoteInput) => {
    return NoteLocalRepository.createNote(input);
  };

  const updateNote = (id: string, input: UpdateNoteInput) => {
    return NoteLocalRepository.updateNote(id, input);
  };

  const deleteNote = (id: string) => {
    NoteLocalRepository.deleteNote(id);
  };

  const addNoteImage = (noteId: string, image: LocalPickedImage, altText?: string) => {
    return NoteLocalRepository.addNoteImage(noteId, image, altText);
  };

  return {
    notes,
    tags,
    tasks,
    loading,
    createNote,
    updateNote,
    deleteNote,
    addNoteImage,
    refresh: loadData,
  };
}
