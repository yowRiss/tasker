import React from 'react';
import { StyleSheet, View, Text } from 'react-native';
import { Transaction } from '../../../shared/types/domain';
import { useItemSyncStatus } from '../../sync/hooks/useItemSyncStatus';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface TransactionItemProps {
  transaction: Transaction;
}

export const TransactionItem: React.FC<TransactionItemProps> = ({ transaction }) => {
  const { isPending, isFailed } = useItemSyncStatus('transaction', transaction.id);

  const isIncome = transaction.transaction_type === 'income';
  const isExpense = transaction.transaction_type === 'expense';
  const isTransfer = transaction.transaction_type === 'transfer';

  const formattedAmount = new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0,
  }).format(transaction.amount);

  let prefix = '';
  let amountColor = colors.text;

  if (isIncome) {
    prefix = '+';
    amountColor = colors.success;
  } else if (isExpense) {
    prefix = '-';
    amountColor = colors.danger;
  } else if (isTransfer) {
    prefix = '↔ ';
    amountColor = colors.info;
  }

  return (
    <View style={[styles.card, isFailed && styles.cardFailed]}>
      <View style={styles.leftCol}>
        <View style={styles.titleRow}>
          <Text style={styles.description} numberOfLines={1}>
            {transaction.description || (isTransfer ? 'Account Transfer' : 'Transaction')}
          </Text>
          {isPending && <Text style={styles.syncStatusText}>⏳</Text>}
          {isFailed && <Text style={styles.syncErrorText}>⚠️</Text>}
        </View>

        <View style={styles.subMeta}>
          <Text style={styles.date}>{transaction.transaction_date}</Text>
          {!!transaction.account && (
            <Text style={styles.accountName}>• {transaction.account.name}</Text>
          )}
          {isTransfer && !!transaction.transfer_account && (
            <Text style={styles.accountName}>➜ {transaction.transfer_account.name}</Text>
          )}
          {!!transaction.category && (
            <View style={[styles.catBadge, { backgroundColor: transaction.category.color || colors.neutral100 }]}>
              <Text style={styles.catText}>{transaction.category.name}</Text>
            </View>
          )}
        </View>
      </View>

      <View style={styles.rightCol}>
        <Text style={[styles.amount, { color: amountColor }]}>
          {prefix}{formattedAmount}
        </Text>
        {!!transaction.receipt && (
          <Text style={styles.receiptBadge}>🧾 Receipt</Text>
        )}
      </View>
    </View>
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
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  cardFailed: {
    borderColor: colors.danger,
    backgroundColor: colors.dangerSoft,
  },
  leftCol: {
    flex: 1,
    marginRight: spacing.s2,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.s1,
    marginBottom: 2,
  },
  description: {
    fontSize: typography.base,
    fontWeight: '600',
    color: colors.text,
    flexShrink: 1,
  },
  syncStatusText: {
    fontSize: 10,
  },
  syncErrorText: {
    fontSize: 10,
  },
  subMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: spacing.s1,
  },
  date: {
    fontSize: typography.xs,
    color: colors.textSubtle,
  },
  accountName: {
    fontSize: typography.xs,
    color: colors.textSecondary,
  },
  catBadge: {
    paddingHorizontal: spacing.s2,
    paddingVertical: 1,
    borderRadius: radius.sm,
  },
  catText: {
    fontSize: 10,
    fontWeight: '600',
    color: colors.text,
  },
  rightCol: {
    alignItems: 'flex-end',
  },
  amount: {
    fontSize: typography.base,
    fontWeight: '700',
  },
  receiptBadge: {
    fontSize: 10,
    color: colors.primary,
    fontWeight: '600',
    marginTop: 2,
  },
});
