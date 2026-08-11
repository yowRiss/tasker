import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  ScrollView,
  SafeAreaView,
  ActivityIndicator,
} from 'react-native';
import { useMoneyLocal } from './hooks/useMoneyLocal';
import { AccountCard } from './components/AccountCard';
import { TransactionItem } from './components/TransactionItem';
import { TransactionEditorModal } from './components/TransactionEditorModal';
import { colors, spacing, radius, typography } from '../../theme/tokens';
import { LocalPickedImage } from '../../services/imageService';

export const MoneyScreen: React.FC = () => {
  const [filterType, setFilterType] = useState<'all' | 'income' | 'expense' | 'transfer'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  const {
    accounts,
    categories,
    transactions,
    budgets,
    totalBalance,
    loading,
    createTransaction,
    addReceipt,
  } = useMoneyLocal({ type: filterType, search: searchQuery });

  const [editorVisible, setEditorVisible] = useState(false);

  const formattedTotal = new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0,
  }).format(totalBalance);

  const handleSaveTransaction = (input: any, receiptImage?: LocalPickedImage) => {
    const tx = createTransaction(input);
    if (receiptImage && tx) {
      addReceipt(tx.id, receiptImage);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Money</Text>
        <TouchableOpacity style={styles.newButton} onPress={() => setEditorVisible(true)}>
          <Text style={styles.newButtonText}>+ Entry</Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Net Worth Card */}
        <View style={styles.netWorthCard}>
          <Text style={styles.netWorthLabel}>Total Net Worth</Text>
          <Text style={styles.netWorthValue}>{formattedTotal}</Text>
        </View>

        {/* Accounts Horizontal Scroll */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Accounts</Text>
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.accountRow}>
          {accounts.map((acc) => (
            <View key={acc.id} style={styles.accountCardWrapper}>
              <AccountCard account={acc} />
            </View>
          ))}
        </ScrollView>

        {/* Budgets Section */}
        {budgets.length > 0 && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Category Budgets</Text>
            </View>
            <View style={styles.budgetList}>
              {budgets.map((b) => {
                const percent = Math.min(100, Math.round(((b.spent_amount || 0) / b.amount_limit) * 100));
                const isOver = (b.spent_amount || 0) > b.amount_limit;
                return (
                  <View key={b.id} style={styles.budgetCard}>
                    <View style={styles.budgetHeader}>
                      <Text style={styles.budgetName}>{b.category?.name || 'Category'}</Text>
                      <Text style={[styles.budgetPercent, isOver && styles.overBudget]}>
                        {percent}% {isOver ? '(Over)' : ''}
                      </Text>
                    </View>
                    <View style={styles.progressBarBg}>
                      <View
                        style={[
                          styles.progressBarFill,
                          { width: `${percent}%` },
                          isOver && styles.progressOverFill,
                        ]}
                      />
                    </View>
                  </View>
                );
              })}
            </View>
          </>
        )}

        {/* Transactions Header & Filters */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Transactions</Text>
        </View>

        <View style={styles.searchBar}>
          <TextInput
            style={styles.searchInput}
            value={searchQuery}
            onChangeText={setSearchQuery}
            placeholder="Search transactions..."
            placeholderTextColor={colors.textSubtle}
          />
        </View>

        <View style={styles.filterRow}>
          {[
            { key: 'all', label: 'All' },
            { key: 'expense', label: 'Expense' },
            { key: 'income', label: 'Income' },
            { key: 'transfer', label: 'Transfer' },
          ].map((tab) => {
            const isActive = filterType === tab.key;
            return (
              <TouchableOpacity
                key={tab.key}
                style={[styles.filterTab, isActive && styles.filterTabActive]}
                onPress={() => setFilterType(tab.key as any)}
              >
                <Text style={[styles.filterTabText, isActive && styles.filterTabTextActive]}>
                  {tab.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {loading ? (
          <ActivityIndicator size="large" color={colors.primary} style={{ marginVertical: 20 }} />
        ) : (
          <View style={styles.txList}>
            {transactions.map((tx) => (
              <TransactionItem key={tx.id} transaction={tx} />
            ))}
            {transactions.length === 0 && (
              <View style={styles.emptyState}>
                <Text style={styles.emptyTitle}>No transactions found</Text>
                <Text style={styles.emptyText}>Tap "+ Entry" to record income, expense, or transfers offline.</Text>
              </View>
            )}
          </View>
        )}
      </ScrollView>

      <TouchableOpacity style={styles.fab} onPress={() => setEditorVisible(true)}>
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>

      <TransactionEditorModal
        visible={editorVisible}
        accounts={accounts}
        categories={categories}
        onClose={() => setEditorVisible(false)}
        onSave={handleSaveTransaction}
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
  scrollContent: {
    paddingBottom: 80,
  },
  netWorthCard: {
    backgroundColor: colors.primary,
    borderRadius: radius.xl,
    padding: spacing.s5,
    marginHorizontal: spacing.s4,
    marginBottom: spacing.s4,
  },
  netWorthLabel: {
    fontSize: typography.xs,
    color: colors.primarySoft,
    fontWeight: '600',
    textTransform: 'uppercase',
    marginBottom: 4,
  },
  netWorthValue: {
    fontSize: typography.xxxl,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  sectionHeader: {
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s2,
    marginTop: spacing.s2,
  },
  sectionTitle: {
    fontSize: typography.base,
    fontWeight: '700',
    color: colors.text,
  },
  accountRow: {
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s4,
  },
  accountCardWrapper: {
    marginRight: spacing.s3,
  },
  budgetList: {
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s4,
    gap: spacing.s2,
  },
  budgetCard: {
    backgroundColor: colors.bgSurface,
    borderRadius: radius.md,
    padding: spacing.s3,
    borderWidth: 1,
    borderColor: colors.border,
  },
  budgetHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: spacing.s1,
  },
  budgetName: {
    fontSize: typography.sm,
    fontWeight: '600',
    color: colors.text,
  },
  budgetPercent: {
    fontSize: typography.xs,
    fontWeight: '700',
    color: colors.textSecondary,
  },
  overBudget: {
    color: colors.danger,
  },
  progressBarBg: {
    height: 8,
    backgroundColor: colors.bgSurfaceMuted,
    borderRadius: radius.full,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: colors.primary,
  },
  progressOverFill: {
    backgroundColor: colors.danger,
  },
  searchBar: {
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s2,
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
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s3,
    gap: spacing.s2,
  },
  filterTab: {
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.full,
    backgroundColor: colors.bgSurfaceMuted,
  },
  filterTabActive: {
    backgroundColor: colors.primary,
  },
  filterTabText: {
    fontSize: typography.xs,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  filterTabTextActive: {
    color: '#FFFFFF',
  },
  txList: {
    paddingHorizontal: spacing.s4,
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
