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
import { useNotesLocal } from './hooks/useNotesLocal';
import { NoteItem } from './components/NoteItem';
import { NoteEditorModal } from './components/NoteEditorModal';
import { Note } from '../../shared/types/domain';
import { colors, spacing, radius, typography } from '../../theme/tokens';

export const NotesScreen: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const { notes, tags, tasks, loading, createNote, updateNote, deleteNote, addNoteImage } =
    useNotesLocal(searchQuery);

  const [editorVisible, setEditorVisible] = useState(false);
  const [editingNote, setEditingNote] = useState<Note | null>(null);

  const handleOpenNewNote = () => {
    setEditingNote(null);
    setEditorVisible(true);
  };

  const handleOpenEditNote = (note: Note) => {
    setEditingNote(note);
    setEditorVisible(true);
  };

  const handleSaveNote = (input: any, noteId?: string) => {
    if (noteId) {
      updateNote(noteId, input);
    } else {
      createNote(input);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Notes</Text>
        <TouchableOpacity style={styles.newButton} onPress={handleOpenNewNote}>
          <Text style={styles.newButtonText}>+ Note</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.searchBar}>
        <TextInput
          style={styles.searchInput}
          value={searchQuery}
          onChangeText={setSearchQuery}
          placeholder="Search notes content..."
          placeholderTextColor={colors.textSubtle}
        />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <FlatList
          data={notes}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <NoteItem note={item} onPress={handleOpenEditNote} onDelete={deleteNote} />
          )}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <Text style={styles.emptyTitle}>No notes found</Text>
              <Text style={styles.emptyText}>Tap "+ Note" to capture notes and images offline.</Text>
            </View>
          }
        />
      )}

      <TouchableOpacity style={styles.fab} onPress={handleOpenNewNote}>
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>

      <NoteEditorModal
        visible={editorVisible}
        note={editingNote}
        tags={tags}
        tasks={tasks}
        onClose={() => setEditorVisible(false)}
        onSave={handleSaveNote}
        onAddImage={addNoteImage}
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
    marginBottom: spacing.s3,
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
