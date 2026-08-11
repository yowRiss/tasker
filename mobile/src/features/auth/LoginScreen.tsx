import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { useAuth } from './AuthContext';
import { AuthService } from '../../services/authService';
import { colors, spacing, radius, typography } from '../../theme/tokens';

export const LoginScreen: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [apiUrl, setApiUrl] = useState('http://10.0.2.2:8080');
  const [showApiConfig, setShowApiConfig] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    AuthService.getApiUrl().then(setApiUrl);
  }, []);

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      setError('Please enter both username and password.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      await AuthService.setApiUrl(apiUrl);
      await login(username.trim(), password.trim());
    } catch (err: any) {
      setError(err.message || 'Login failed. Check credentials and server URL.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
        <View style={styles.card}>
          <View style={styles.headerContainer}>
            <View style={styles.logoBadge}>
              <Text style={styles.logoText}>T</Text>
            </View>
            <Text style={styles.title}>Personal Workspace</Text>
            <Text style={styles.subtitle}>Sign in to sync your tasks, notes, and money</Text>
          </View>

          {error && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Username</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              autoCapitalize="none"
              placeholder="admin"
              placeholderTextColor={colors.textSubtle}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Password</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              placeholder="••••••••"
              placeholderTextColor={colors.textSubtle}
            />
          </View>

          <TouchableOpacity
            style={styles.toggleConfigButton}
            onPress={() => setShowApiConfig(!showApiConfig)}
          >
            <Text style={styles.toggleConfigText}>
              {showApiConfig ? 'Hide Server Config' : 'Configure Server URL'}
            </Text>
          </TouchableOpacity>

          {showApiConfig && (
            <View style={styles.inputGroup}>
              <Text style={styles.label}>Backend API URL</Text>
              <TextInput
                style={styles.input}
                value={apiUrl}
                onChangeText={setApiUrl}
                autoCapitalize="none"
                placeholder="http://10.0.2.2:8080"
                placeholderTextColor={colors.textSubtle}
              />
              <Text style={styles.helperText}>
                Android emulator uses http://10.0.2.2:8080 for localhost.
              </Text>
            </View>
          )}

          <TouchableOpacity
            style={[styles.loginButton, loading && styles.buttonDisabled]}
            onPress={handleLogin}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text style={styles.loginButtonText}>Sign In</Text>
            )}
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgPage,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.s4,
  },
  card: {
    backgroundColor: colors.bgSurface,
    borderRadius: radius.xl,
    padding: spacing.s5,
    borderWidth: 1,
    borderColor: colors.border,
    elevation: 2,
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
  },
  headerContainer: {
    alignItems: 'center',
    marginBottom: spacing.s5,
  },
  logoBadge: {
    width: 48,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.s3,
  },
  logoText: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '700',
  },
  title: {
    fontSize: typography.xl,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.s1,
  },
  subtitle: {
    fontSize: typography.sm,
    color: colors.textMuted,
    textAlign: 'center',
  },
  errorBox: {
    backgroundColor: colors.dangerSoft,
    borderRadius: radius.md,
    padding: spacing.s3,
    marginBottom: spacing.s4,
    borderWidth: 1,
    borderColor: colors.danger,
  },
  errorText: {
    color: colors.dangerText,
    fontSize: typography.sm,
  },
  inputGroup: {
    marginBottom: spacing.s4,
  },
  label: {
    fontSize: typography.sm,
    fontWeight: '600',
    color: colors.textSecondary,
    marginBottom: spacing.s1,
  },
  input: {
    height: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.s3,
    fontSize: typography.base,
    color: colors.text,
    backgroundColor: colors.bgSurface,
  },
  helperText: {
    fontSize: typography.xs,
    color: colors.textSubtle,
    marginTop: spacing.s1,
  },
  toggleConfigButton: {
    alignSelf: 'flex-start',
    marginBottom: spacing.s4,
  },
  toggleConfigText: {
    fontSize: typography.xs,
    color: colors.primary,
    fontWeight: '600',
  },
  loginButton: {
    height: 48,
    backgroundColor: colors.primary,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.s2,
  },
  buttonDisabled: {
    opacity: 0.7,
  },
  loginButtonText: {
    color: '#FFFFFF',
    fontSize: typography.base,
    fontWeight: '600',
  },
});
