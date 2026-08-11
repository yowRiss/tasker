import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Modal,
  ScrollView,
  Image,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { Note, Tag, Task, NoteImage } from '../../../shared/types/domain';
import { CreateNoteInput, UpdateNoteInput } from '../../../db/repositories/noteLocalRepository';
import { ImageService, LocalPickedImage } from '../../../services/imageService';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface NoteEditorModalProps {
  visible: boolean;
  note?: Note | null;
  tags: Tag[];
  tasks: Task[];
  onClose: () => void;
  onSave: (input: CreateNoteInput | UpdateNoteInput, noteId?: string) => void;
  onAddImage: (noteId: string, image: LocalPickedImage) => NoteImage;
}

export const NoteEditorModal: React.FC<NoteEditorModalProps> = ({
  visible,
  note,
  tags,
  tasks,
  onClose,
  onSave,
  onAddImage,
}) => {
  const [title, setTitle] = useState('');
  const [contentMd, setContentMd] = useState('');
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
  const [selectedTaskIds, setSelectedTaskIds] = useState<string[]>([]);
  const [images, setImages] = useState<NoteImage[]>([]);
  const [previewMode, setPreviewMode] = useState(false);

  useEffect(() => {
    if (note) {
      setTitle(note.title);
      setContentMd(note.content_md);
      setSelectedTagIds(note.tags?.map((t) => t.id) || []);
      setSelectedTaskIds(note.linked_task_ids || []);
      setImages(note.images || []);
    } else {
      setTitle('');
      setContentMd('');
      setSelectedTagIds([]);
      setSelectedTaskIds([]);
      setImages([]);
    }
    setPreviewMode(false);
  }, [note, visible]);

  const handleSave = () => {
    if (!title.trim()) return;

    if (note) {
      onSave(
        {
          title: title.trim(),
          content_md: contentMd,
          tag_ids: selectedTagIds,
          task_ids: selectedTaskIds,
        },
        note.id
      );
    } else {
      onSave({
        title: title.trim(),
        content_md: contentMd,
        tag_ids: selectedTagIds,
        task_ids: selectedTaskIds,
      });
    }

    onClose();
  };

  const handlePickImage = async (source: 'camera' | 'library') => {
    try {
      const picked = await ImageService.pickImage(source);
      if (!picked) return;

      if (note) {
        const newImg = onAddImage(note.id, picked);
        setImages([...images, newImg]);
        const embedToken = `\n![${picked.originalFilename}](note-image:${picked.id})\n`;
        setContentMd((prev) => prev + embedToken);
      } else {
        // If note not created yet, append image placeholder token
        const embedToken = `\n![${picked.originalFilename}](local-file:${picked.localUri})\n`;
        setContentMd((prev) => prev + embedToken);
      }
    } catch (err: any) {
      Alert.alert('Image Pick Error', err.message);
    }
  };

  const handleToggleTag = (tagId: string) => {
    if (selectedTagIds.includes(tagId)) {
      setSelectedTagIds(selectedTagIds.filter((id) => id !== tagId));
    } else {
      setSelectedTagIds([...selectedTagIds, tagId]);
    }
  };

  const handleToggleTaskLink = (taskId: string) => {
    if (selectedTaskIds.includes(taskId)) {
      setSelectedTaskIds(selectedTaskIds.filter((id) => id !== taskId));
    } else {
      setSelectedTaskIds([...selectedTaskIds, taskId]);
    }
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        style={styles.overlay}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>{note ? 'Edit Note' : 'New Note'}</Text>
            <View style={styles.headerRight}>
              <TouchableOpacity
                style={[styles.previewToggle, previewMode && styles.previewToggleActive]}
                onPress={() => setPreviewMode(!previewMode)}
              >
                <Text style={[styles.previewToggleText, previewMode && styles.previewToggleTextActive]}>
                  {previewMode ? 'Edit' : 'Preview'}
                </Text>
              </TouchableOpacity>
              <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
                <Text style={styles.closeText}>✕</Text>
              </TouchableOpacity>
            </View>
          </View>

          <ScrollView style={styles.body} keyboardShouldPersistTaps="handled">
            <Text style={styles.label}>Title *</Text>
            <TextInput
              style={styles.input}
              value={title}
              onChangeText={setTitle}
              placeholder="Note title"
              placeholderTextColor={colors.textSubtle}
            />

            <View style={styles.sectionHeaderRow}>
              <Text style={styles.label}>Markdown Content</Text>
              <View style={styles.imageActionRow}>
                <TouchableOpacity style={styles.imgBtn} onPress={() => handlePickImage('camera')}>
                  <Text style={styles.imgBtnText}>📷 Camera</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.imgBtn} onPress={() => handlePickImage('library')}>
                  <Text style={styles.imgBtnText}>🖼 Gallery</Text>
                </TouchableOpacity>
              </View>
            </View>

            {previewMode ? (
              <View style={styles.previewBox}>
                <Text style={styles.previewTitle}>{title || 'Untitled Note'}</Text>
                <Text style={styles.previewBody}>{contentMd || 'No content.'}</Text>

                {images.length > 0 && (
                  <View style={styles.imageGallery}>
                    <Text style={styles.galleryTitle}>Attached Images:</Text>
                    {images.map((img) => (
                      <View key={img.id} style={styles.imageCard}>
                        <Image source={{ uri: img.local_uri }} style={styles.previewImage} />
                        <Text style={styles.imageCaption}>{img.original_filename} ({img.sync_status})</Text>
                      </View>
                    ))}
                  </View>
                )}
              </View>
            ) : (
              <TextInput
                style={[styles.input, styles.editorInput]}
                value={contentMd}
                onChangeText={setContentMd}
                placeholder="Write Markdown content..."
                placeholderTextColor={colors.textSubtle}
                multiline
                numberOfLines={10}
              />
            )}

            <Text style={styles.label}>Tags</Text>
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

            <Text style={styles.label}>Link Tasks</Text>
            <View style={styles.wrapChipRow}>
              {tasks.slice(0, 10).map((t) => {
                const isLinked = selectedTaskIds.includes(t.id);
                return (
                  <TouchableOpacity
                    key={t.id}
                    style={[styles.filterChip, isLinked && styles.filterChipSelected]}
                    onPress={() => handleToggleTaskLink(t.id)}
                  >
                    <Text style={[styles.filterChipText, isLinked && styles.filterChipTextSelected]}>
                      ✓ {t.title}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </ScrollView>

          <View style={styles.footer}>
            <TouchableOpacity style={styles.cancelBtn} onPress={onClose}>
              <Text style={styles.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.saveBtn} onPress={handleSave}>
              <Text style={styles.saveBtnText}>Save Note</Text>
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
    maxHeight: '92%',
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
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.s3,
  },
  previewToggle: {
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  previewToggleActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  previewToggleText: {
    fontSize: typography.xs,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  previewToggleTextActive: {
    color: '#FFFFFF',
  },
  closeBtn: {
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
  editorInput: {
    height: 160,
    textAlignVertical: 'top',
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.s2,
  },
  imageActionRow: {
    flexDirection: 'row',
    gap: spacing.s2,
  },
  imgBtn: {
    backgroundColor: colors.primarySubtle,
    paddingHorizontal: spacing.s2,
    paddingVertical: 4,
    borderRadius: radius.sm,
  },
  imgBtnText: {
    fontSize: typography.xs,
    color: colors.primaryHover,
    fontWeight: '600',
  },
  previewBox: {
    backgroundColor: colors.bgSurfaceMuted,
    borderRadius: radius.md,
    padding: spacing.s4,
    minHeight: 160,
    marginBottom: spacing.s2,
  },
  previewTitle: {
    fontSize: typography.xl,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.s2,
  },
  previewBody: {
    fontSize: typography.base,
    color: colors.textSecondary,
    lineHeight: 22,
  },
  imageGallery: {
    marginTop: spacing.s4,
  },
  galleryTitle: {
    fontSize: typography.sm,
    fontWeight: '600',
    color: colors.textSecondary,
    marginBottom: spacing.s2,
  },
  imageCard: {
    marginBottom: spacing.s3,
  },
  previewImage: {
    width: '100%',
    height: 180,
    borderRadius: radius.md,
    backgroundColor: colors.neutral200,
  },
  imageCaption: {
    fontSize: typography.xs,
    color: colors.textSubtle,
    marginTop: 4,
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
