import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { Account } from '../../../shared/types/domain';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface AccountCardProps {
  account: Account;
  onPress?: (account: Account) => void;
}

export const AccountCard: React.FC<AccountCardProps> = ({ account, onPress }) => {
  const balance = account.current_balance || 0;
  const formattedBalance = new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0,
  }).format(balance);

  const getAccountTypeLabel = (type: string) => {
    switch (type) {
      case 'cash':
        return '💵 Cash';
      case 'bank':
        return '🏦 Bank Account';
      case 'e_wallet':
        return '📱 E-Wallet';
      case 'credit_card':
        return '💳 Credit Card';
      default:
        return type;
    }
  };

  return (
    <TouchableOpacity
      style={styles.card}
      onPress={() => onPress && onPress(account)}
      activeOpacity={0.8}
    >
      <Text style={styles.typeLabel}>{getAccountTypeLabel(account.account_type)}</Text>
      <Text style={styles.name} numberOfLines={1}>
        {account.name}
      </Text>
      <Text style={[styles.balance, balance < 0 && styles.negativeBalance]}>
        {formattedBalance}
      </Text>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.bgSurface,
    borderRadius: radius.lg,
    padding: spacing.s4,
    borderWidth: 1,
    borderColor: colors.border,
    minWidth: 140,
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
  },
  typeLabel: {
    fontSize: typography.xs,
    color: colors.textSubtle,
    fontWeight: '600',
    marginBottom: 4,
  },
  name: {
    fontSize: typography.sm,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.s2,
  },
  balance: {
    fontSize: typography.lg,
    fontWeight: '700',
    color: colors.text,
  },
  negativeBalance: {
    color: colors.danger,
  },
});
