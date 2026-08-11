import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Modal,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { Task, Project, Tag } from '../../../shared/types/domain';
import { CreateTaskInput, UpdateTaskInput } from '../../../db/repositories/taskLocalRepository';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface TaskEditorModalProps {
  visible: boolean;
  task?: Task | null;
  projects: Project[];
  tags: Tag[];
  onClose: () => void;
  onSave: (input: CreateTaskInput | UpdateTaskInput, taskId?: string) => void;
  onCreateProject: (name: string) => Project;
  onCreateTag: (name: string) => Tag;
}

export const TaskEditorModal: React.FC<TaskEditorModalProps> = ({
  visible,
  task,
  projects,
  tags,
  onClose,
  onSave,
  onCreateProject,
  onCreateTag,
}) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState<number>(0);
  const [projectId, setProjectId] = useState<string | undefined>(undefined);
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
  const [subtasks, setSubtasks] = useState<{ id?: string; title: string; completed?: boolean }[]>([]);
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');

  const [newProjName, setNewProjName] = useState('');
  const [showAddProj, setShowAddProj] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [showAddTag, setShowAddTag] = useState(false);

  useEffect(() => {
    if (task) {
      setTitle(task.title);
      setDescription(task.description || '');
      setDueDate(task.due_date || '');
      setPriority(task.priority);
      setProjectId(task.project_id || undefined);
      setSelectedTagIds(task.tags?.map((t) => t.id) || []);
      setSubtasks(task.subtasks?.map((s) => ({ id: s.id, title: s.title, completed: s.completed })) || []);
    } else {
      setTitle('');
      setDescription('');
      setDueDate('');
      setPriority(0);
      setProjectId(undefined);
      setSelectedTagIds([]);
      setSubtasks([]);
    }
  }, [task, visible]);

  const handleSave = () => {
    if (!title.trim()) return;

    if (task) {
      const updatePayload: UpdateTaskInput = {
        title: title.trim(),
        description: description.trim() || undefined,
        due_date: dueDate.trim() || null,
        priority,
        project_id: projectId || null,
        tag_ids: selectedTagIds,
        subtasks,
      };
      onSave(updatePayload, task.id);
    } else {
      const createPayload: CreateTaskInput = {
        title: title.trim(),
        description: description.trim() || undefined,
        due_date: dueDate.trim() || undefined,
        priority,
        project_id: projectId,
        tag_ids: selectedTagIds,
        subtasks: subtasks.map((s) => s.title),
      };
      onSave(createPayload);
    }

    onClose();
  };

  const handleAddSubtask = () => {
    if (!newSubtaskTitle.trim()) return;
    setSubtasks([...subtasks, { title: newSubtaskTitle.trim(), completed: false }]);
    setNewSubtaskTitle('');
  };

  const handleRemoveSubtask = (index: number) => {
    setSubtasks(subtasks.filter((_, i) => i !== index));
  };

  const handleToggleTag = (tagId: string) => {
    if (selectedTagIds.includes(tagId)) {
      setSelectedTagIds(selectedTagIds.filter((id) => id !== tagId));
    } else {
      setSelectedTagIds([...selectedTagIds, tagId]);
    }
  };

  const handleCreateProject = () => {
    if (!newProjName.trim()) return;
    const p = onCreateProject(newProjName.trim());
    setProjectId(p.id);
    setNewProjName('');
    setShowAddProj(false);
  };

  const handleCreateTag = () => {
    if (!newTagName.trim()) return;
    const t = onCreateTag(newTagName.trim());
    setSelectedTagIds([...selectedTagIds, t.id]);
    setNewTagName('');
    setShowAddTag(false);
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        style={styles.overlay}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>{task ? 'Edit Task' : 'New Task'}</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <Text style={styles.closeText}>✕</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.body} keyboardShouldPersistTaps="handled">
            <Text style={styles.label}>Title *</Text>
            <TextInput
              style={styles.input}
              value={title}
              onChangeText={setTitle}
              placeholder="Task title"
              placeholderTextColor={colors.textSubtle}
            />

            <Text style={styles.label}>Description</Text>
            <TextInput
              style={[styles.input, styles.multilineInput]}
              value={description}
              onChangeText={setDescription}
              placeholder="Add optional notes or details"
              placeholderTextColor={colors.textSubtle}
              multiline
              numberOfLines={3}
            />

            <Text style={styles.label}>Due Date (YYYY-MM-DD)</Text>
            <TextInput
              style={styles.input}
              value={dueDate}
              onChangeText={setDueDate}
              placeholder="e.g. 2026-08-15"
              placeholderTextColor={colors.textSubtle}
            />

            <Text style={styles.label}>Priority</Text>
            <View style={styles.priorityRow}>
              {[
                { val: 0, label: 'None' },
                { val: 1, label: 'Low' },
                { val: 2, label: 'Med' },
                { val: 3, label: 'High' },
              ].map((p) => (
                <TouchableOpacity
                  key={p.val}
                  style={[
                    styles.priorityChip,
                    priority === p.val && styles.priorityChipSelected,
                  ]}
                  onPress={() => setPriority(p.val)}
                >
                  <Text
                    style={[
                      styles.priorityText,
                      priority === p.val && styles.priorityTextSelected,
                    ]}
                  >
                    {p.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <View style={styles.sectionHeaderRow}>
              <Text style={styles.label}>Project</Text>
              <TouchableOpacity onPress={() => setShowAddProj(!showAddProj)}>
                <Text style={styles.actionText}>+ New Project</Text>
              </TouchableOpacity>
            </View>

            {showAddProj && (
              <View style={styles.inlineForm}>
                <TextInput
                  style={[styles.input, { flex: 1, marginBottom: 0 }]}
                  value={newProjName}
                  onChangeText={setNewProjName}
                  placeholder="Project name"
                />
                <TouchableOpacity style={styles.smallAddBtn} onPress={handleCreateProject}>
                  <Text style={styles.smallAddBtnText}>Add</Text>
                </TouchableOpacity>
              </View>
            )}

            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
              <TouchableOpacity
                style={[
                  styles.filterChip,
                  !projectId && styles.filterChipSelected,
                ]}
                onPress={() => setProjectId(undefined)}
              >
                <Text style={[styles.filterChipText, !projectId && styles.filterChipTextSelected]}>
                  No Project
                </Text>
              </TouchableOpacity>
              {projects.map((p) => (
                <TouchableOpacity
                  key={p.id}
                  style={[
                    styles.filterChip,
                    projectId === p.id && styles.filterChipSelected,
                  ]}
                  onPress={() => setProjectId(p.id)}
                >
                  <Text style={[styles.filterChipText, projectId === p.id && styles.filterChipTextSelected]}>
                    📁 {p.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            <View style={styles.sectionHeaderRow}>
              <Text style={styles.label}>Tags</Text>
              <TouchableOpacity onPress={() => setShowAddTag(!showAddTag)}>
                <Text style={styles.actionText}>+ New Tag</Text>
              </TouchableOpacity>
            </View>

            {showAddTag && (
              <View style={styles.inlineForm}>
                <TextInput
                  style={[styles.input, { flex: 1, marginBottom: 0 }]}
                  value={newTagName}
                  onChangeText={setNewTagName}
                  placeholder="Tag name"
                />
                <TouchableOpacity style={styles.smallAddBtn} onPress={handleCreateTag}>
                  <Text style={styles.smallAddBtnText}>Add</Text>
                </TouchableOpacity>
              </View>
            )}

            <View style={styles.wrapChipRow}>
              {tags.map((t) => {
                const isSelected = selectedTagIds.includes(t.id);
                return (
                  <TouchableOpacity
                    key={t.id}
                    style={[styles.filterChip, isSelected && styles.filterChipSelected]}
                    onPress={() => handleToggleTag(t.id)}
                  >
                    <Text style={[styles.filterChipText, isSelected && styles.filterChipTextSelected]}>
                      #{t.name}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>

            <Text style={styles.label}>Subtasks</Text>
            {subtasks.map((st, i) => (
              <View key={i} style={styles.subtaskRow}>
                <Text style={styles.subtaskTitle}>• {st.title}</Text>
                <TouchableOpacity onPress={() => handleRemoveSubtask(i)}>
                  <Text style={styles.removeText}>✕</Text>
                </TouchableOpacity>
              </View>
            ))}

            <View style={styles.inlineForm}>
              <TextInput
                style={[styles.input, { flex: 1, marginBottom: 0 }]}
                value={newSubtaskTitle}
                onChangeText={setNewSubtaskTitle}
                placeholder="Add subtask item"
              />
              <TouchableOpacity style={styles.smallAddBtn} onPress={handleAddSubtask}>
                <Text style={styles.smallAddBtnText}>+ Subtask</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>

          <View style={styles.footer}>
            <TouchableOpacity style={styles.cancelBtn} onPress={onClose}>
              <Text style={styles.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.saveBtn} onPress={handleSave}>
              <Text style={styles.saveBtnText}>Save Task</Text>
            </TouchableOpacity>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.4)',
    justifyContent: 'flex-end',
  },
  container: {
    backgroundColor: colors.bgSurface,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    maxHeight: '90%',
    paddingBottom: spacing.s4,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.s4,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  headerTitle: {
    fontSize: typography.lg,
    fontWeight: '700',
    color: colors.text,
  },
  closeButton: {
    padding: spacing.s1,
  },
  closeText: {
    fontSize: 18,
    color: colors.textSubtle,
  },
  body: {
    padding: spacing.s4,
  },
  label: {
    fontSize: typography.sm,
    fontWeight: '600',
    color: colors.textSecondary,
    marginBottom: spacing.s1,
    marginTop: spacing.s2,
  },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s2,
    fontSize: typography.base,
    color: colors.text,
    backgroundColor: colors.bgSurface,
    marginBottom: spacing.s2,
  },
  multilineInput: {
    height: 70,
    textAlignVertical: 'top',
  },
  priorityRow: {
    flexDirection: 'row',
    gap: spacing.s2,
    marginBottom: spacing.s2,
  },
  priorityChip: {
    flex: 1,
    paddingVertical: spacing.s2,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
  },
  priorityChipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  priorityText: {
    fontSize: typography.sm,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  priorityTextSelected: {
    color: '#FFFFFF',
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.s2,
  },
  actionText: {
    fontSize: typography.xs,
    fontWeight: '600',
    color: colors.primary,
  },
  inlineForm: {
    flexDirection: 'row',
    gap: spacing.s2,
    alignItems: 'center',
    marginBottom: spacing.s2,
  },
  smallAddBtn: {
    backgroundColor: colors.primarySubtle,
    paddingHorizontal: spacing.s3,
    height: 40,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  smallAddBtnText: {
    color: colors.primaryHover,
    fontWeight: '600',
    fontSize: typography.sm,
  },
  chipRow: {
    flexDirection: 'row',
    marginBottom: spacing.s2,
  },
  wrapChipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.s2,
    marginBottom: spacing.s2,
  },
  filterChip: {
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.full,
    borderWidth: 1,
    borderColor: colors.border,
    marginRight: spacing.s1,
  },
  filterChipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  filterChipText: {
    fontSize: typography.xs,
    color: colors.textSecondary,
    fontWeight: '500',
  },
  filterChipTextSelected: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
  subtaskRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.s1,
  },
  subtaskTitle: {
    fontSize: typography.sm,
    color: colors.text,
  },
  removeText: {
    color: colors.danger,
    fontSize: 14,
  },
  footer: {
    flexDirection: 'row',
    padding: spacing.s4,
    gap: spacing.s3,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  cancelBtn: {
    flex: 1,
    height: 44,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancelBtnText: {
    fontSize: typography.base,
    color: colors.textSecondary,
    fontWeight: '600',
  },
  saveBtn: {
    flex: 1,
    height: 44,
    borderRadius: radius.md,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  saveBtnText: {
    fontSize: typography.base,
    color: '#FFFFFF',
    fontWeight: '600',
  },
});
