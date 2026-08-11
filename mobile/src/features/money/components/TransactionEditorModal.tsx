import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Modal,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { Account, Category } from '../../../shared/types/domain';
import { CreateTransactionInput } from '../../../db/repositories/moneyLocalRepository';
import { ImageService, LocalPickedImage } from '../../../services/imageService';
import { colors, spacing, radius, typography } from '../../../theme/tokens';

interface TransactionEditorModalProps {
  visible: boolean;
  accounts: Account[];
  categories: Category[];
  onClose: () => void;
  onSave: (input: CreateTransactionInput, receiptImage?: LocalPickedImage) => void;
}

export const TransactionEditorModal: React.FC<TransactionEditorModalProps> = ({
  visible,
  accounts,
  categories,
  onClose,
  onSave,
}) => {
  const [type, setType] = useState<'expense' | 'income' | 'transfer'>('expense');
  const [amountStr, setAmountStr] = useState('');
  const [dateStr, setDateStr] = useState(new Date().toISOString().split('T')[0]);
  const [accountId, setAccountId] = useState<string>(accounts[0]?.id || '');
  const [transferAccountId, setTransferAccountId] = useState<string>('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [description, setDescription] = useState('');
  const [pickedReceipt, setPickedReceipt] = useState<LocalPickedImage | null>(null);

  const handleSave = () => {
    const amount = parseFloat(amountStr);
    if (isNaN(amount) || amount <= 0) {
      Alert.alert('Invalid Amount', 'Please enter a positive numeric amount.');
      return;
    }

    if (!accountId) {
      Alert.alert('Select Account', 'Please select a source account.');
      return;
    }

    if (type === 'transfer') {
      if (!transferAccountId || transferAccountId === accountId) {
        Alert.alert('Invalid Transfer Account', 'Please select a distinct destination account.');
        return;
      }
    } else {
      if (!categoryId) {
        Alert.alert('Select Category', 'Please select a category.');
        return;
      }
    }

    const payload: CreateTransactionInput = {
      transaction_type: type,
      amount,
      transaction_date: dateStr,
      account_id: accountId,
      transfer_account_id: type === 'transfer' ? transferAccountId : undefined,
      category_id: type !== 'transfer' ? categoryId : undefined,
      description: description.trim() || undefined,
    };

    onSave(payload, pickedReceipt || undefined);
    onClose();
  };

  const handlePickReceipt = async (source: 'camera' | 'library') => {
    try {
      const picked = await ImageService.pickImage(source);
      if (picked) setPickedReceipt(picked);
    } catch (err: any) {
      Alert.alert('Receipt Pick Error', err.message);
    }
  };

  const filteredCategories = categories.filter((c) => c.category_type === type);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        style={styles.overlay}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>New Transaction</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Text style={styles.closeText}>✕</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.body} keyboardShouldPersistTaps="handled">
            {/* Type selector */}
            <View style={styles.typeSelector}>
              {[
                { key: 'expense', label: 'Expense' },
                { key: 'income', label: 'Income' },
                { key: 'transfer', label: 'Transfer' },
              ].map((t) => (
                <TouchableOpacity
                  key={t.key}
                  style={[styles.typeTab, type === t.key && styles.typeTabActive]}
                  onPress={() => setType(t.key as any)}
                >
                  <Text style={[styles.typeTabText, type === t.key && styles.typeTabTextActive]}>
                    {t.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <Text style={styles.label}>Amount (IDR) *</Text>
            <TextInput
              style={styles.input}
              value={amountStr}
              onChangeText={setAmountStr}
              keyboardType="numeric"
              placeholder="e.g. 50000"
              placeholderTextColor={colors.textSubtle}
            />

            <Text style={styles.label}>Date (YYYY-MM-DD) *</Text>
            <TextInput
              style={styles.input}
              value={dateStr}
              onChangeText={setDateStr}
              placeholder="2026-08-11"
              placeholderTextColor={colors.textSubtle}
            />

            <Text style={styles.label}>{type === 'transfer' ? 'From Account *' : 'Account *'}</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
              {accounts.map((acc) => (
                <TouchableOpacity
                  key={acc.id}
                  style={[styles.chip, accountId === acc.id && styles.chipSelected]}
                  onPress={() => setAccountId(acc.id)}
                >
                  <Text style={[styles.chipText, accountId === acc.id && styles.chipTextSelected]}>
                    {acc.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            {type === 'transfer' && (
              <>
                <Text style={styles.label}>To Account *</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
                  {accounts
                    .filter((a) => a.id !== accountId)
                    .map((acc) => (
                      <TouchableOpacity
                        key={acc.id}
                        style={[styles.chip, transferAccountId === acc.id && styles.chipSelected]}
                        onPress={() => setTransferAccountId(acc.id)}
                      >
                        <Text style={[styles.chipText, transferAccountId === acc.id && styles.chipTextSelected]}>
                          {acc.name}
                        </Text>
                      </TouchableOpacity>
                    ))}
                </ScrollView>
              </>
            )}

            {type !== 'transfer' && (
              <>
                <Text style={styles.label}>Category *</Text>
                <View style={styles.wrapChipRow}>
                  {filteredCategories.map((cat) => (
                    <TouchableOpacity
                      key={cat.id}
                      style={[styles.chip, categoryId === cat.id && styles.chipSelected]}
                      onPress={() => setCategoryId(cat.id)}
                    >
                      <Text style={[styles.chipText, categoryId === cat.id && styles.chipTextSelected]}>
                        {cat.name}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </>
            )}

            <Text style={styles.label}>Description</Text>
            <TextInput
              style={styles.input}
              value={description}
              onChangeText={setDescription}
              placeholder="e.g. Lunch with team"
              placeholderTextColor={colors.textSubtle}
            />

            <View style={styles.sectionHeaderRow}>
              <Text style={styles.label}>Receipt Image</Text>
              <View style={styles.imageActionRow}>
                <TouchableOpacity style={styles.imgBtn} onPress={() => handlePickReceipt('camera')}>
                  <Text style={styles.imgBtnText}>📷 Camera</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.imgBtn} onPress={() => handlePickReceipt('library')}>
                  <Text style={styles.imgBtnText}>🖼 Gallery</Text>
                </TouchableOpacity>
              </View>
            </View>

            {pickedReceipt && (
              <View style={styles.receiptPreviewBox}>
                <Text style={styles.receiptText}>🧾 Receipt attached: {pickedReceipt.originalFilename}</Text>
              </View>
            )}
          </ScrollView>

          <View style={styles.footer}>
            <TouchableOpacity style={styles.cancelBtn} onPress={onClose}>
              <Text style={styles.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.saveBtn} onPress={handleSave}>
              <Text style={styles.saveBtnText}>Save Transaction</Text>
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
    maxHeight: '90%',
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
  typeSelector: {
    flexDirection: 'row',
    backgroundColor: colors.bgSurfaceMuted,
    borderRadius: radius.md,
    padding: 2,
    marginBottom: spacing.s3,
  },
  typeTab: {
    flex: 1,
    paddingVertical: spacing.s2,
    alignItems: 'center',
    borderRadius: radius.sm,
  },
  typeTabActive: {
    backgroundColor: colors.primary,
  },
  typeTabText: {
    fontSize: typography.sm,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  typeTabTextActive: {
    color: '#FFFFFF',
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
  chipRow: {
    flexDirection: 'row',
    marginBottom: spacing.s2,
  },
  wrapChipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.s2,
    marginBottom: spacing.s2,
  },
  chip: {
    paddingHorizontal: spacing.s3,
    paddingVertical: spacing.s1,
    borderRadius: radius.full,
    borderWidth: 1,
    borderColor: colors.border,
    marginRight: spacing.s1,
  },
  chipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  chipText: {
    fontSize: typography.xs,
    color: colors.textSecondary,
    fontWeight: '500',
  },
  chipTextSelected: {
    color: '#FFFFFF',
    fontWeight: '600',
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
  receiptPreviewBox: {
    backgroundColor: colors.primarySubtle,
    borderRadius: radius.md,
    padding: spacing.s3,
    marginTop: spacing.s2,
  },
  receiptText: {
    fontSize: typography.xs,
    color: colors.primaryHover,
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
