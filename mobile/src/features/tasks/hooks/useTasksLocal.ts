import { useState, useEffect, useCallback } from 'react';
import { TaskLocalRepository, TaskFilters, CreateTaskInput, UpdateTaskInput } from '../../../db/repositories/taskLocalRepository';
import { DatabaseNotifier } from '../../../db/notifier';
import { Task, Project, Tag } from '../../../shared/types/domain';

export function useTasksLocal(initialFilters: TaskFilters = { status: 'open' }) {
  const [filters, setFilters] = useState<TaskFilters>(initialFilters);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(() => {
    try {
      const taskList = TaskLocalRepository.getTasks(filters);
      const projList = TaskLocalRepository.getProjects();
      const tagList = TaskLocalRepository.getTags();

      setTasks(taskList);
      setProjects(projList);
      setTags(tagList);
    } catch (err) {
      console.error('Error reading local tasks from SQLite:', err);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    loadData();

    const unsubTasks = DatabaseNotifier.subscribe('tasks', loadData);
    const unsubProjects = DatabaseNotifier.subscribe('projects', loadData);
    const unsubTags = DatabaseNotifier.subscribe('tags', loadData);

    return () => {
      unsubTasks();
      unsubProjects();
      unsubTags();
    };
  }, [loadData]);

  const updateFilters = (newFilters: Partial<TaskFilters>) => {
    setFilters((prev) => ({ ...prev, ...newFilters }));
  };

  const createTask = (input: CreateTaskInput) => {
    return TaskLocalRepository.createTask(input);
  };

  const updateTask = (id: string, input: UpdateTaskInput) => {
    return TaskLocalRepository.updateTask(id, input);
  };

  const toggleTaskCompletion = (id: string) => {
    return TaskLocalRepository.toggleTaskCompletion(id);
  };

  const deleteTask = (id: string) => {
    TaskLocalRepository.deleteTask(id);
  };

  const createProject = (name: string, color?: string) => {
    return TaskLocalRepository.createProject(name, color);
  };

  const createTag = (name: string, color?: string) => {
    return TaskLocalRepository.createTag(name, color);
  };

  return {
    tasks,
    projects,
    tags,
    loading,
    filters,
    updateFilters,
    createTask,
    updateTask,
    toggleTaskCompletion,
    deleteTask,
    createProject,
    createTag,
    refresh: loadData,
  };
}
