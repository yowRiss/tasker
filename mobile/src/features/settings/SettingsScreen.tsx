import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity, SafeAreaView, Alert } from 'react-native';
import { useAuth } from '../auth/AuthContext';
import { colors, spacing, radius, typography } from '../../theme/tokens';

export const SettingsScreen: React.FC = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    Alert.alert('Sign Out', 'Are you sure you want to sign out?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Sign Out', style: 'destructive', onPress: () => logout() },
    ]);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Settings</Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Account</Text>
          <View style={styles.row}>
            <Text style={styles.label}>Logged in as</Text>
            <Text style={styles.value}>{user?.username || 'admin'}</Text>
          </View>
        </View>

        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <Text style={styles.logoutText}>Sign Out</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgPage,
  },
  content: {
    flex: 1,
    padding: spacing.s4,
  },
  title: {
    fontSize: typography.xxl,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.s5,
  },
  section: {
    backgroundColor: colors.bgSurface,
    borderRadius: radius.lg,
    padding: spacing.s4,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.s5,
  },
  sectionTitle: {
    fontSize: typography.xs,
    fontWeight: '700',
    color: colors.textSubtle,
    textTransform: 'uppercase',
    marginBottom: spacing.s3,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  label: {
    fontSize: typography.base,
    color: colors.textSecondary,
  },
  value: {
    fontSize: typography.base,
    fontWeight: '600',
    color: colors.text,
  },
  logoutButton: {
    height: 48,
    backgroundColor: colors.dangerSoft,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.danger,
  },
  logoutText: {
    fontSize: typography.base,
    fontWeight: '600',
    color: colors.dangerText,
  },
});
