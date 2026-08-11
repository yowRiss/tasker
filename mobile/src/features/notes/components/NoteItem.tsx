import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { Note } from '../../../shared/types/domain';
import { useItemSyncStatus } from '../../sync/hooks/useItemSyncStatus';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface NoteItemProps {
  note: Note;
  onPress: (note: Note) => void;
  onDelete: (id: string) => void;
}

export const NoteItem: React.FC<NoteItemProps> = ({ note, onPress, onDelete }) => {
  const { isPending, isFailed } = useItemSyncStatus('note', note.id);

  const contentExcerpt = note.content_md
    .replace(/^#+\s+/gm, '')
    .replace(/!\[.*?\]\(.*?\)/g, '')
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .trim();

  return (
    <TouchableOpacity
      style={[styles.card, isFailed && styles.cardFailed]}
      onPress={() => onPress(note)}
      activeOpacity={0.7}
    >
      <View style={styles.headerRow}>
        <Text style={styles.title} numberOfLines={1}>
          {note.title}
        </Text>
        <View style={styles.badgeRow}>
          {isPending && <Text style={styles.syncStatusText}>⏳ Unsynced</Text>}
          {isFailed && <Text style={styles.syncErrorText}>⚠️ Failed</Text>}
          <TouchableOpacity style={styles.deleteBtn} onPress={() => onDelete(note.id)}>
            <Text style={styles.deleteText}>✕</Text>
          </TouchableOpacity>
        </View>
      </View>

      {!!contentExcerpt && (
        <Text style={styles.excerpt} numberOfLines={3}>
          {contentExcerpt}
        </Text>
      )}

      <View style={styles.metaRow}>
        {note.tags?.map((tag) => (
          <View key={tag.id} style={styles.tagChip}>
            <Text style={styles.tagText}>#{tag.name}</Text>
          </View>
        ))}

        {note.images && note.images.length > 0 && (
          <View style={styles.metaChip}>
            <Text style={styles.metaText}>📷 {note.images.length} image{note.images.length > 1 ? 's' : ''}</Text>
          </View>
        )}

        {note.linked_task_ids && note.linked_task_ids.length > 0 && (
          <View style={styles.metaChip}>
            <Text style={styles.metaText}>🔗 {note.linked_task_ids.length} linked task{note.linked_task_ids.length > 1 ? 's' : ''}</Text>
          </View>
        )}
      </View>
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
  },
  cardFailed: {
    borderColor: colors.danger,
    backgroundColor: colors.dangerSoft,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  title: {
    fontSize: typography.base,
    fontWeight: '700',
    color: colors.text,
    flex: 1,
    marginRight: spacing.s2,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.s2,
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
  deleteBtn: {
    padding: spacing.s1,
  },
  deleteText: {
    color: colors.textSubtle,
    fontSize: 16,
  },
  excerpt: {
    fontSize: typography.sm,
    color: colors.textSecondary,
    marginBottom: spacing.s2,
    lineHeight: 18,
  },
  metaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.s1,
    marginTop: spacing.s1,
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
});
