import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  Modal,
  FlatList,
  ActivityIndicator,
} from 'react-native';
import { useSyncState } from '../hooks/useSyncState';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface SyncQueueDrawerProps {
  visible: boolean;
  onClose: () => void;
}

export const SyncQueueDrawer: React.FC<SyncQueueDrawerProps> = ({ visible, onClose }) => {
  const {
    isOnline,
    isSyncing,
    pendingCount,
    failedCount,
    queueItems,
    triggerSync,
    retryFailedItem,
    deleteQueueItem,
  } = useSyncState();

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.container}>
          <View style={styles.header}>
            <View>
              <Text style={styles.headerTitle}>Sync Management</Text>
              <Text style={styles.headerSubtitle}>
                {isOnline ? 'Online' : 'Offline'} • {pendingCount} pending, {failedCount} failed
              </Text>
            </View>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Text style={styles.closeText}>✕</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.actionRow}>
            <TouchableOpacity
              style={[styles.syncBtn, (isSyncing || !isOnline) && styles.btnDisabled]}
              onPress={triggerSync}
              disabled={isSyncing || !isOnline}
            >
              {isSyncing ? (
                <ActivityIndicator size="small" color="#FFFFFF" />
              ) : (
                <Text style={styles.syncBtnText}>Sync Now</Text>
              )}
            </TouchableOpacity>
          </View>

          <FlatList
            data={queueItems}
            keyExtractor={(item) => String(item.id)}
            contentContainerStyle={styles.listContent}
            renderItem={({ item }) => {
              const isFailed = item.status === 'failed';
              return (
                <View style={[styles.queueCard, isFailed && styles.queueCardFailed]}>
                  <View style={styles.queueHeader}>
                    <Text style={styles.queueType}>
                      {item.entity_type.toUpperCase()} • {item.operation}
                    </Text>
                    <View
                      style={[
                        styles.statusBadge,
                        isFailed ? styles.badgeFailed : styles.badgePending,
                      ]}
                    >
                      <Text style={styles.badgeText}>{item.status}</Text>
                    </View>
                  </View>

                  <Text style={styles.payloadText} numberOfLines={2}>
                    {item.payload}
                  </Text>

                  {!!item.last_error && (
                    <Text style={styles.errorText}>Error: {item.last_error}</Text>
                  )}

                  <View style={styles.cardActions}>
                    {isFailed && (
                      <TouchableOpacity
                        style={styles.retryBtn}
                        onPress={() => retryFailedItem(item.id)}
                      >
                        <Text style={styles.retryText}>Retry</Text>
                      </TouchableOpacity>
                    )}
                    <TouchableOpacity
                      style={styles.discardBtn}
                      onPress={() => deleteQueueItem(item.id)}
                    >
                      <Text style={styles.discardText}>Discard</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              );
            }}
            ListEmptyComponent={
              <View style={styles.emptyBox}>
                <Text style={styles.emptyTitle}>Queue is Empty</Text>
                <Text style={styles.emptyText}>All local changes have synced to the server.</Text>
              </View>
            }
          />
        </View>
      </View>
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
    maxHeight: '85%',
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
  headerSubtitle: {
    fontSize: typography.xs,
    color: colors.textMuted,
    marginTop: 2,
  },
  closeBtn: {
    padding: spacing.s1,
  },
  closeText: {
    fontSize: 18,
    color: colors.textSubtle,
  },
  actionRow: {
    padding: spacing.s4,
    paddingBottom: spacing.s2,
  },
  syncBtn: {
    height: 44,
    backgroundColor: colors.primary,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnDisabled: {
    opacity: 0.6,
  },
  syncBtnText: {
    color: '#FFFFFF',
    fontSize: typography.base,
    fontWeight: '600',
  },
  listContent: {
    padding: spacing.s4,
  },
  queueCard: {
    backgroundColor: colors.bgSurfaceMuted,
    borderRadius: radius.md,
    padding: spacing.s3,
    marginBottom: spacing.s3,
    borderWidth: 1,
    borderColor: colors.border,
  },
  queueCardFailed: {
    borderColor: colors.danger,
    backgroundColor: colors.dangerSoft,
  },
  queueHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.s1,
  },
  queueType: {
    fontSize: typography.xs,
    fontWeight: '700',
    color: colors.textSecondary,
  },
  statusBadge: {
    paddingHorizontal: spacing.s2,
    paddingVertical: 2,
    borderRadius: radius.sm,
  },
  badgePending: {
    backgroundColor: colors.infoSoft,
  },
  badgeFailed: {
    backgroundColor: colors.danger,
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: colors.text,
  },
  payloadText: {
    fontSize: typography.xs,
    color: colors.textMuted,
    marginBottom: spacing.s2,
  },
  errorText: {
    fontSize: typography.xs,
    color: colors.dangerText,
    fontWeight: '600',
    marginBottom: spacing.s2,
  },
  cardActions: {
    flexDirection: 'row',
    gap: spacing.s2,
    justifyContent: 'flex-end',
  },
  retryBtn: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.sm,
  },
  retryText: {
    color: '#FFFFFF',
    fontSize: typography.xs,
    fontWeight: '600',
  },
  discardBtn: {
    backgroundColor: colors.neutral200,
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.sm,
  },
  discardText: {
    color: colors.textSecondary,
    fontSize: typography.xs,
    fontWeight: '600',
  },
  emptyBox: {
    padding: spacing.s6,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: typography.base,
    fontWeight: '600',
    color: colors.textSecondary,
    marginBottom: spacing.s1,
  },
  emptyText: {
    fontSize: typography.xs,
    color: colors.textMuted,
    textAlign: 'center',
  },
});
