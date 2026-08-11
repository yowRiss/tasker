import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { colors, spacing, typography } from '../../../theme/tokens';

interface SyncStatusBarProps {
  isOnline: boolean;
  pendingCount: number;
  failedCount: number;
  isSyncing: boolean;
  onPressStatus?: () => void;
}

export const SyncStatusBar: React.FC<SyncStatusBarProps> = ({
  isOnline,
  pendingCount,
  failedCount,
  isSyncing,
  onPressStatus,
}) => {
  let statusText = 'Online';
  let badgeColor = colors.success;
  let bgSoft = colors.successSoft;
  let textColor = colors.successText;

  if (!isOnline) {
    statusText = `Offline (${pendingCount} pending)`;
    badgeColor = colors.warning;
    bgSoft = colors.warningSoft;
    textColor = colors.warningText;
  } else if (failedCount > 0) {
    statusText = `${failedCount} sync error${failedCount > 1 ? 's' : ''}`;
    badgeColor = colors.danger;
    bgSoft = colors.dangerSoft;
    textColor = colors.dangerText;
  } else if (isSyncing) {
    statusText = `Syncing (${pendingCount} left)...`;
    badgeColor = colors.primary;
    bgSoft = colors.primarySoft;
    textColor = colors.primaryActive;
  } else if (pendingCount > 0) {
    statusText = `${pendingCount} change${pendingCount > 1 ? 's' : ''} queued`;
    badgeColor = colors.info;
    bgSoft = colors.infoSoft;
    textColor = colors.infoText;
  }

  return (
    <TouchableOpacity
      style={[styles.container, { backgroundColor: bgSoft }]}
      onPress={onPressStatus}
      activeOpacity={0.8}
    >
      <View style={styles.content}>
        <View style={[styles.dot, { backgroundColor: badgeColor }]} />
        <Text style={[styles.text, { color: textColor }]}>{statusText}</Text>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: spacing.s1,
    paddingHorizontal: spacing.s4,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: spacing.s2,
  },
  text: {
    fontSize: typography.xs,
    fontWeight: '600',
  },
});
