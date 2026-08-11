import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { Task } from '../../../shared/types/domain';
import { useItemSyncStatus } from '../../sync/hooks/useItemSyncStatus';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface TaskItemProps {
  task: Task;
  onToggleComplete: (id: string) => void;
  onPress: (task: Task) => void;
  onDelete: (id: string) => void;
}

const PRIORITY_LABELS: Record<number, { label: string; color: string; bg: string }> = {
  0: { label: 'None', color: colors.textSubtle, bg: colors.neutral100 },
  1: { label: 'Low', color: colors.infoText, bg: colors.infoSoft },
  2: { label: 'Medium', color: colors.warningText, bg: colors.warningSoft },
  3: { label: 'High', color: colors.dangerText, bg: colors.dangerSoft },
};

export const TaskItem: React.FC<TaskItemProps> = ({
  task,
  onToggleComplete,
  onPress,
  onDelete,
}) => {
  const isCompleted = task.status === 'completed';
  const priorityInfo = PRIORITY_LABELS[task.priority] || PRIORITY_LABELS[0];
  const { isPending, isFailed } = useItemSyncStatus('task', task.id);

  return (
    <TouchableOpacity
      style={[styles.card, isCompleted && styles.cardCompleted, isFailed && styles.cardFailed]}
      onPress={() => onPress(task)}
      activeOpacity={0.7}
    >
      <TouchableOpacity
        style={[styles.checkbox, isCompleted && styles.checkboxChecked]}
        onPress={() => onToggleComplete(task.id)}
      >
        {isCompleted && <Text style={styles.checkmark}>✓</Text>}
      </TouchableOpacity>

      <View style={styles.content}>
        <View style={styles.headerRow}>
          <Text style={[styles.title, isCompleted && styles.titleCompleted]}>
            {task.title}
          </Text>

          <View style={styles.badgeRow}>
            {isPending && <Text style={styles.syncStatusText}>⏳ Unsynced</Text>}
            {isFailed && <Text style={styles.syncErrorText}>⚠️ Failed</Text>}

            {task.priority > 0 && (
              <View style={[styles.badge, { backgroundColor: priorityInfo.bg }]}>
                <Text style={[styles.badgeText, { color: priorityInfo.color }]}>
                  {priorityInfo.label}
                </Text>
              </View>
            )}
          </View>
        </View>

        {!!task.description && (
          <Text style={styles.description} numberOfLines={2}>
            {task.description}
          </Text>
        )}

        <View style={styles.metaRow}>
          {!!task.due_date && (
            <View style={styles.metaChip}>
              <Text style={styles.metaText}>📅 {task.due_date}</Text>
            </View>
          )}

          {!!task.project && (
            <View style={[styles.metaChip, { backgroundColor: task.project.color || colors.neutral100 }]}>
              <Text style={styles.metaText}>📁 {task.project.name}</Text>
            </View>
          )}

          {task.tags?.map((tag) => (
            <View key={tag.id} style={styles.tagChip}>
              <Text style={styles.tagText}>#{tag.name}</Text>
            </View>
          ))}

          {task.subtasks && task.subtasks.length > 0 && (
            <View style={styles.metaChip}>
              <Text style={styles.metaText}>
                {task.subtasks.filter((s) => s.completed).length}/{task.subtasks.length} subtasks
              </Text>
            </View>
          )}
        </View>
      </View>

      <TouchableOpacity style={styles.deleteButton} onPress={() => onDelete(task.id)}>
        <Text style={styles.deleteText}>✕</Text>
      </TouchableOpacity>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.bgSurface,
    borderRadius: radius.md,
    padding: spacing.s3,
    marginBottom: spacing.s2,
    borderWidth: 1,
    borderColor: colors.border,
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  cardCompleted: {
    backgroundColor: colors.bgSurfaceMuted,
    opacity: 0.8,
  },
  cardFailed: {
    borderColor: colors.danger,
    backgroundColor: colors.dangerSoft,
  },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 2,
    borderColor: colors.borderStrong,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.s3,
    marginTop: 2,
  },
  checkboxChecked: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  checkmark: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: 'bold',
  },
  content: {
    flex: 1,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 2,
  },
  title: {
    fontSize: typography.base,
    fontWeight: '600',
    color: colors.text,
    flex: 1,
    marginRight: spacing.s2,
  },
  titleCompleted: {
    textDecorationLine: 'line-through',
    color: colors.textMuted,
  },
  description: {
    fontSize: typography.sm,
    color: colors.textSecondary,
    marginBottom: spacing.s2,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.s1,
  },
  syncStatusText: {
    fontSize: 10,
    color: colors.warningText,
    fontWeight: '600',
  },
  syncErrorText: {
    fontSize: 10,
    color: colors.dangerText,
    fontWeight: '700',
  },
  badge: {
    paddingHorizontal: spacing.s2,
    paddingVertical: 2,
    borderRadius: radius.sm,
  },
  badgeText: {
    fontSize: typography.xs,
    fontWeight: '700',
  },
  metaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.s1,
    marginTop: spacing.s1,
  },
  metaChip: {
    backgroundColor: colors.neutral100,
    paddingHorizontal: spacing.s2,
    paddingVertical: 2,
    borderRadius: radius.sm,
  },
  metaText: {
    fontSize: typography.xs,
    color: colors.textSecondary,
  },
  tagChip: {
    backgroundColor: colors.primarySubtle,
    paddingHorizontal: spacing.s2,
    paddingVertical: 2,
    borderRadius: radius.sm,
  },
  tagText: {
    fontSize: typography.xs,
    color: colors.primaryHover,
    fontWeight: '600',
  },
  deleteButton: {
    padding: spacing.s1,
    marginLeft: spacing.s2,
  },
  deleteText: {
    color: colors.textSubtle,
    fontSize: 16,
  },
});
