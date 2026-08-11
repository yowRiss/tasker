import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  SafeAreaView,
  ActivityIndicator,
} from 'react-native';
import { useTasksLocal } from './hooks/useTasksLocal';
import { TaskItem } from './components/TaskItem';
import { TaskEditorModal } from './components/TaskEditorModal';
import { Task } from '../../shared/types/domain';
import { colors, spacing, radius, typography } from '../../theme/tokens';

export const TasksScreen: React.FC = () => {
  const {
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
  } = useTasksLocal({ status: 'open' });

  const [searchQuery, setSearchQuery] = useState('');
  const [editorVisible, setEditorVisible] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);

  const handleSearch = (text: string) => {
    setSearchQuery(text);
    updateFilters({ search: text });
  };

  const handleOpenNewTask = () => {
    setEditingTask(null);
    setEditorVisible(true);
  };

  const handleOpenEditTask = (task: Task) => {
    setEditingTask(task);
    setEditorVisible(true);
  };

  const handleSaveTask = (input: any, taskId?: string) => {
    if (taskId) {
      updateTask(taskId, input);
    } else {
      createTask(input);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Tasks</Text>
        <TouchableOpacity style={styles.newButton} onPress={handleOpenNewTask}>
          <Text style={styles.newButtonText}>+ Task</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.searchBar}>
        <TextInput
          style={styles.searchInput}
          value={searchQuery}
          onChangeText={handleSearch}
          placeholder="Search tasks..."
          placeholderTextColor={colors.textSubtle}
        />
      </View>

      {/* Filter Tabs */}
      <View style={styles.filterRow}>
        {[
          { key: 'open', label: 'Open' },
          { key: 'completed', label: 'Completed' },
          { key: 'all', label: 'All' },
        ].map((tab) => {
          const isActive = (filters.status || 'open') === tab.key;
          return (
            <TouchableOpacity
              key={tab.key}
              style={[styles.filterTab, isActive && styles.filterTabActive]}
              onPress={() => updateFilters({ status: tab.key as any })}
            >
              <Text style={[styles.filterTabText, isActive && styles.filterTabTextActive]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Main Task List */}
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <FlatList
          data={tasks}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <TaskItem
              task={item}
              onToggleComplete={toggleTaskCompletion}
              onPress={handleOpenEditTask}
              onDelete={deleteTask}
            />
          )}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <Text style={styles.emptyTitle}>No tasks found</Text>
              <Text style={styles.emptyText}>
                {filters.status === 'completed'
                  ? 'No completed tasks yet.'
                  : 'Tap "+ Task" to create a task offline.'}
              </Text>
            </View>
          }
        />
      )}

      {/* FAB */}
      <TouchableOpacity style={styles.fab} onPress={handleOpenNewTask}>
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>

      <TaskEditorModal
        visible={editorVisible}
        task={editingTask}
        projects={projects}
        tags={tags}
        onClose={() => setEditorVisible(false)}
        onSave={handleSaveTask}
        onCreateProject={createProject}
        onCreateTag={createTag}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgPage,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.s4,
    paddingTop: spacing.s3,
    paddingBottom: spacing.s2,
  },
  title: {
    fontSize: typography.xxl,
    fontWeight: '700',
    color: colors.text,
  },
  newButton: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s2,
    borderRadius: radius.md,
  },
  newButtonText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: typography.sm,
  },
  searchBar: {
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s2,
  },
  searchInput: {
    height: 40,
    backgroundColor: colors.bgSurface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.s3,
    fontSize: typography.sm,
    color: colors.text,
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s3,
    gap: spacing.s2,
  },
  filterTab: {
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.full,
    backgroundColor: colors.bgSurfaceMuted,
  },
  filterTabActive: {
    backgroundColor: colors.primary,
  },
  filterTabText: {
    fontSize: typography.xs,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  filterTabTextActive: {
    color: '#FFFFFF',
  },
  listContent: {
    paddingHorizontal: spacing.s4,
    paddingBottom: 80,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyState: {
    padding: spacing.s6,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: typography.lg,
    fontWeight: '600',
    color: colors.textSecondary,
    marginBottom: spacing.s1,
  },
  emptyText: {
    fontSize: typography.sm,
    color: colors.textMuted,
    textAlign: 'center',
  },
  fab: {
    position: 'absolute',
    right: spacing.s4,
    bottom: spacing.s4,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  fabIcon: {
    fontSize: 28,
    color: '#FFFFFF',
    lineHeight: 30,
  },
});
