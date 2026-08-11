import { useState, useEffect, useCallback } from 'react';
import {
  MoneyLocalRepository,
  CreateAccountInput,
  CreateCategoryInput,
  CreateTransactionInput,
  CreateBudgetInput,
} from '../../../db/repositories/moneyLocalRepository';
import { DatabaseNotifier } from '../../../db/notifier';
import { Account, Category, Transaction, Budget } from '../../../shared/types/domain';
import { LocalPickedImage } from '../../../services/imageService';

export function useMoneyLocal(filters: { account_id?: string; category_id?: string; type?: string; search?: string } = {}) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(() => {
    try {
      const accList = MoneyLocalRepository.getAccounts();
      const catList = MoneyLocalRepository.getCategories();
      const txList = MoneyLocalRepository.getTransactions(filters);
      const bgList = MoneyLocalRepository.getBudgets();

      setAccounts(accList);
      setCategories(catList);
      setTransactions(txList);
      setBudgets(bgList);
    } catch (err) {
      console.error('Error loading money data:', err);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    loadData();

    const unsubAccounts = DatabaseNotifier.subscribe('accounts', loadData);
    const unsubCategories = DatabaseNotifier.subscribe('categories', loadData);
    const unsubTx = DatabaseNotifier.subscribe('transactions', loadData);
    const unsubBudgets = DatabaseNotifier.subscribe('budgets', loadData);

    return () => {
      unsubAccounts();
      unsubCategories();
      unsubTx();
      unsubBudgets();
    };
  }, [loadData]);

  const createAccount = (input: CreateAccountInput) => {
    return MoneyLocalRepository.createAccount(input);
  };

  const createCategory = (input: CreateCategoryInput) => {
    return MoneyLocalRepository.createCategory(input);
  };

  const createTransaction = (input: CreateTransactionInput) => {
    return MoneyLocalRepository.createTransaction(input);
  };

  const addReceipt = (transactionId: string, image: LocalPickedImage) => {
    return MoneyLocalRepository.addReceipt(transactionId, image);
  };

  const createBudget = (input: CreateBudgetInput) => {
    return MoneyLocalRepository.createBudget(input);
  };

  const totalBalance = accounts.reduce((acc, a) => acc + (a.current_balance || 0), 0);

  return {
    accounts,
    categories,
    transactions,
    budgets,
    totalBalance,
    loading,
    createAccount,
    createCategory,
    createTransaction,
    addReceipt,
    createBudget,
    refresh: loadData,
  };
}
