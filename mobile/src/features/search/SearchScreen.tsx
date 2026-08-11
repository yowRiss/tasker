import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  FlatList,
  SafeAreaView,
} from 'react-native';
import { TaskLocalRepository } from '../../db/repositories/taskLocalRepository';
import { NoteLocalRepository } from '../../db/repositories/noteLocalRepository';
import { MoneyLocalRepository } from '../../db/repositories/moneyLocalRepository';
import { TaskItem } from '../tasks/components/TaskItem';
import { NoteItem } from '../notes/components/NoteItem';
import { TransactionItem } from '../money/components/TransactionItem';
import { colors, spacing, radius, typography } from '../../theme/tokens';

export const SearchScreen: React.FC = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<{ type: 'task' | 'note' | 'transaction'; item: any }[]>([]);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    const q = query.trim();
    const tasks = TaskLocalRepository.getTasks({ search: q });
    const notes = NoteLocalRepository.getNotes(q);
    const transactions = MoneyLocalRepository.getTransactions({ search: q });

    const combined = [
      ...tasks.map((item) => ({ type: 'task' as const, item })),
      ...notes.map((item) => ({ type: 'note' as const, item })),
      ...transactions.map((item) => ({ type: 'transaction' as const, item })),
    ];

    setResults(combined);
  }, [query]);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Search</Text>
      </View>

      <View style={styles.searchBar}>
        <TextInput
          style={styles.searchInput}
          value={query}
          onChangeText={setQuery}
          placeholder="Search tasks, notes & transactions..."
          placeholderTextColor={colors.textSubtle}
          autoFocus
        />
      </View>

      <FlatList
        data={results}
        keyExtractor={(res) => `${res.type}-${res.item.id}`}
        contentContainerStyle={styles.listContent}
        renderItem={({ item: res }) => {
          if (res.type === 'task') {
            return (
              <View style={styles.resultGroup}>
                <Text style={styles.groupLabel}>TASK</Text>
                <TaskItem
                  task={res.item}
                  onToggleComplete={() => {}}
                  onPress={() => {}}
                  onDelete={() => {}}
                />
              </View>
            );
          } else if (res.type === 'note') {
            return (
              <View style={styles.resultGroup}>
                <Text style={styles.groupLabel}>NOTE</Text>
                <NoteItem note={res.item} onPress={() => {}} onDelete={() => {}} />
              </View>
            );
          } else {
            return (
              <View style={styles.resultGroup}>
                <Text style={styles.groupLabel}>TRANSACTION</Text>
                <TransactionItem transaction={res.item} />
              </View>
            );
          }
        }}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>
              {query.trim() ? 'No matches found' : 'Type a query to search'}
            </Text>
            <Text style={styles.emptyText}>Search operates 100% offline against local SQLite.</Text>
          </View>
        }
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
    paddingHorizontal: spacing.s4,
    paddingTop: spacing.s3,
    paddingBottom: spacing.s2,
  },
  title: {
    fontSize: typography.xxl,
    fontWeight: '700',
    color: colors.text,
  },
  searchBar: {
    paddingHorizontal: spacing.s4,
    marginBottom: spacing.s3,
  },
  searchInput: {
    height: 44,
    backgroundColor: colors.bgSurface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.s3,
    fontSize: typography.base,
    color: colors.text,
  },
  listContent: {
    paddingHorizontal: spacing.s4,
    paddingBottom: 40,
  },
  resultGroup: {
    marginBottom: spacing.s2,
  },
  groupLabel: {
    fontSize: 10,
    fontWeight: '700',
    color: colors.textSubtle,
    marginBottom: 2,
    marginLeft: 2,
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
});
