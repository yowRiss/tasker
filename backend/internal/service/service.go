package service

import (
	"context"
	"errors"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
	"path/filepath"
	"regexp"
	"strings"
	"tasker/backend/internal/domain"
	"tasker/backend/internal/platform/dbtx"
	"tasker/backend/internal/repository/postgres"
	storage "tasker/backend/internal/storage/supabase"
	"time"
)

type Service struct {
	pool    *pgxpool.Pool
	repo    *postgres.Repository
	storage *storage.Storage
}

func New(pool *pgxpool.Pool, repo *postgres.Repository, s *storage.Storage) *Service {
	return &Service{pool: pool, repo: repo, storage: s}
}
func (s *Service) Do(ctx context.Context, p domain.Principal, fn func(pgx.Tx) error) error {
	return dbtx.Within(ctx, s.pool, p.UserID, fn)
}
func Title(v string, max int) (string, error) {
	v = strings.TrimSpace(v)
	if v == "" || len(v) > max {
		return "", fmt.Errorf("must be between 1 and %d characters", max)
	}
	return v, nil
}

var safeStem = regexp.MustCompile(`[^a-z0-9]+`)

var ErrValidation = errors.New("validation failed")

func validationf(format string, args ...any) error {
	return fmt.Errorf("%w: "+format, append([]any{ErrValidation}, args...)...)
}

var exactAmount = regexp.MustCompile(`^[0-9]{1,16}(?:\.[0-9]{1,2})?$`)

func Amount(value string) (string, error) {
	value = strings.TrimSpace(value)
	if !exactAmount.MatchString(value) || value == "0" || value == "0.0" || value == "0.00" {
		return "", validationf("amount must be a positive decimal with at most two fractional digits")
	}
	return value, nil
}

func Date(value string) (string, error) {
	value = strings.TrimSpace(value)
	if _, err := time.Parse("2006-01-02", value); err != nil {
		return "", validationf("must be a YYYY-MM-DD date")
	}
	return value, nil
}

func oneOf(value string, allowed ...string) bool {
	for _, item := range allowed {
		if value == item {
			return true
		}
	}
	return false
}

func ReceiptPath(user, transaction, filename string) (string, error) {
	ext := strings.ToLower(filepath.Ext(filename))
	if ext == ".jpeg" {
		ext = ".jpg"
	}
	if !oneOf(ext, ".jpg", ".png", ".webp", ".gif") {
		return "", validationf("unsupported image extension")
	}
	stem := strings.Trim(safeStem.ReplaceAllString(strings.ToLower(strings.TrimSuffix(filepath.Base(filename), filepath.Ext(filename))), "-"), "-")
	if stem == "" {
		stem = "receipt"
	}
	return fmt.Sprintf("receipts/%s/%s/%s-%s%s", user, transaction, uuid.NewString(), stem, ext), nil
}

func ImagePath(user, note, filename string) (string, error) {
	ext := strings.ToLower(filepath.Ext(filename))
	m := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".webp": true, ".gif": true}
	if !m[ext] {
		return "", fmt.Errorf("unsupported image filename")
	}
	rawExt := filepath.Ext(filename)
	rawName := filepath.Base(filename)
	base := rawName[:len(rawName)-len(rawExt)]
	stem := strings.Trim(safeStem.ReplaceAllString(strings.ToLower(base), "-"), "-")
	if stem == "" {
		stem = "image"
	}
	return fmt.Sprintf("notes/%s/%s/%s-%s%s", user, note, uuid.NewString(), stem, ext), nil
}
func (s *Service) UploadImage(ctx context.Context, p domain.Principal, note, filename, mime string, data []byte) (domain.Image, error) {
	if len(data) == 0 || len(data) > 10*1024*1024 {
		return domain.Image{}, fmt.Errorf("invalid image size")
	}
	var out domain.Image
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.repo.NoteExists(ctx, tx, p.UserID, note); e != nil {
			return e
		}
		path, e := ImagePath(p.UserID, note, filename)
		if e != nil {
			return e
		}
		if e = s.storage.Upload(ctx, path, mime, data); e != nil {
			return e
		}
		id := uuid.NewString()
		out, e = s.repo.AddImage(ctx, tx, p.UserID, note, id, path, filename, mime, len(data))
		if e != nil {
			if clean := s.storage.Delete(ctx, path); clean != nil {
			}
			return e
		}
		return nil
	})
	return out, err
}
func (s *Service) DeleteImage(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error {
		x, e := s.repo.Image(ctx, tx, p.UserID, id)
		if e != nil {
			return e
		}
		if e = s.storage.Delete(ctx, x.ObjectPath); e != nil {
			return e
		}
		return s.repo.DeleteImage(ctx, tx, p.UserID, id)
	})
}
func (s *Service) StorageSignedURL(ctx context.Context, object string) (string, error) {
	return s.storage.SignedURL(ctx, object)
}
func (s *Service) EnsureDefaultAdmin(ctx context.Context, defaultUser, defaultPassword string) error {
	count, err := s.repo.AdminCount(ctx, s.pool)
	if err != nil || count > 0 {
		return err
	}
	hash, err := bcrypt.GenerateFromPassword([]byte(defaultPassword), bcrypt.DefaultCost)
	if err != nil {
		return err
	}
	return s.repo.CreateAdmin(ctx, s.pool, defaultUser, string(hash))
}
func (s *Service) Login(ctx context.Context, username, password string) (domain.Admin, error) {
	admin, err := s.repo.AdminByUsername(ctx, s.pool, username)
	if err != nil {
		return domain.Admin{}, fmt.Errorf("invalid credentials")
	}
	if err := bcrypt.CompareHashAndPassword([]byte(admin.PasswordHash), []byte(password)); err != nil {
		return domain.Admin{}, fmt.Errorf("invalid credentials")
	}
	return admin, nil
}
func (s *Service) ChangePassword(ctx context.Context, p domain.Principal, currentPassword, newPassword string) error {
	if len(newPassword) < 6 {
		return fmt.Errorf("password must be at least 6 characters")
	}
	admin, err := s.repo.AdminByUsername(ctx, s.pool, p.Username)
	if err != nil {
		return fmt.Errorf("admin not found")
	}
	if err := bcrypt.CompareHashAndPassword([]byte(admin.PasswordHash), []byte(currentPassword)); err != nil {
		return fmt.Errorf("incorrect current password")
	}
	hash, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
	if err != nil {
		return err
	}
	return s.repo.UpdateAdminPassword(ctx, s.pool, admin.ID, string(hash))
}

func (s *Service) EnsureDefaultCategories(ctx context.Context, p domain.Principal) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error { return s.repo.CreateDefaultCategories(ctx, tx, p.UserID) })
}

func (s *Service) CreateAccount(ctx context.Context, p domain.Principal, name, kind string) (domain.Account, error) {
	name, err := Title(name, 80)
	if err != nil {
		return domain.Account{}, validationf("account name %s", err)
	}
	if !oneOf(kind, "cash", "bank", "e_wallet", "credit_card") {
		return domain.Account{}, validationf("invalid account type")
	}
	var out domain.Account
	err = s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.CreateAccount(ctx, tx, p.UserID, name, kind)
		return e
	})
	return out, err
}

func (s *Service) UpdateAccount(ctx context.Context, p domain.Principal, id string, name, kind *string, archived *bool) (domain.Account, error) {
	if name != nil {
		n, err := Title(*name, 80)
		if err != nil {
			return domain.Account{}, validationf("account name %s", err)
		}
		name = &n
	}
	if kind != nil && !oneOf(*kind, "cash", "bank", "e_wallet", "credit_card") {
		return domain.Account{}, validationf("invalid account type")
	}
	var out domain.Account
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.UpdateAccount(ctx, tx, p.UserID, id, name, kind, archived)
		return e
	})
	return out, err
}

func (s *Service) DeleteAccount(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error { return s.repo.DeleteAccount(ctx, tx, p.UserID, id) })
}

func (s *Service) CreateCategory(ctx context.Context, p domain.Principal, name, kind string, icon, color *string) (domain.Category, error) {
	name, err := Title(name, 80)
	if err != nil {
		return domain.Category{}, validationf("category name %s", err)
	}
	if !oneOf(kind, "income", "expense") {
		return domain.Category{}, validationf("invalid category type")
	}
	if icon != nil && len(*icon) > 80 {
		return domain.Category{}, validationf("icon is too long")
	}
	if color != nil && !regexp.MustCompile(`^#[0-9A-Fa-f]{6}$`).MatchString(*color) {
		return domain.Category{}, validationf("color must be a #RRGGBB value")
	}
	var out domain.Category
	err = s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.CreateCategory(ctx, tx, p.UserID, name, kind, icon, color)
		return e
	})
	return out, err
}

func (s *Service) UpdateCategory(ctx context.Context, p domain.Principal, id string, name, icon, color *string, archived *bool) (domain.Category, error) {
	if name != nil {
		n, err := Title(*name, 80)
		if err != nil {
			return domain.Category{}, validationf("category name %s", err)
		}
		name = &n
	}
	if icon != nil && len(*icon) > 80 {
		return domain.Category{}, validationf("icon is too long")
	}
	if color != nil && !regexp.MustCompile(`^#[0-9A-Fa-f]{6}$`).MatchString(*color) {
		return domain.Category{}, validationf("color must be a #RRGGBB value")
	}
	var out domain.Category
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.UpdateCategory(ctx, tx, p.UserID, id, name, icon, color, archived)
		return e
	})
	return out, err
}

func (s *Service) DeleteCategory(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error { return s.repo.DeleteCategory(ctx, tx, p.UserID, id) })
}

type TransactionInput struct {
	TransactionType, Amount, TransactionDate, AccountID string
	TransferAccountID, CategoryID, Description          *string
}

func (s *Service) validateTransaction(ctx context.Context, tx pgx.Tx, p domain.Principal, in *TransactionInput) error {
	if !oneOf(in.TransactionType, "income", "expense", "transfer") {
		return validationf("invalid transaction type")
	}
	amount, err := Amount(in.Amount)
	if err != nil {
		return err
	}
	in.Amount = amount
	date, err := Date(in.TransactionDate)
	if err != nil {
		return err
	}
	in.TransactionDate = date
	if err := s.repo.ActiveAccount(ctx, tx, p.UserID, in.AccountID); err != nil {
		return validationf("account must exist and be active")
	}
	if in.Description != nil && len(*in.Description) > 1000 {
		return validationf("description is too long")
	}
	if in.TransactionType == "transfer" {
		if in.TransferAccountID == nil || *in.TransferAccountID == "" || *in.TransferAccountID == in.AccountID {
			return validationf("a transfer needs a different destination account")
		}
		if in.CategoryID != nil {
			return validationf("transfers cannot have a category")
		}
		if err := s.repo.ActiveAccount(ctx, tx, p.UserID, *in.TransferAccountID); err != nil {
			return validationf("transfer destination must exist and be active")
		}
		return nil
	}
	if in.TransferAccountID != nil {
		return validationf("income and expenses cannot have a transfer account")
	}
	if in.CategoryID == nil || *in.CategoryID == "" {
		return validationf("income and expenses require a category")
	}
	if err := s.repo.ActiveCategory(ctx, tx, p.UserID, *in.CategoryID, in.TransactionType); err != nil {
		return validationf("category must exist, match transaction type, and be active")
	}
	return nil
}

func (s *Service) CreateTransaction(ctx context.Context, p domain.Principal, in TransactionInput) (domain.Transaction, error) {
	var out domain.Transaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.validateTransaction(ctx, tx, p, &in); e != nil {
			return e
		}
		var e error
		out, e = s.repo.CreateTransaction(ctx, tx, p.UserID, in.TransactionType, in.Amount, in.TransactionDate, in.AccountID, in.TransferAccountID, in.CategoryID, in.Description)
		return e
	})
	return out, err
}

func (s *Service) UpdateTransaction(ctx context.Context, p domain.Principal, id string, in TransactionInput) (domain.Transaction, error) {
	var out domain.Transaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.validateTransaction(ctx, tx, p, &in); e != nil {
			return e
		}
		var e error
		out, e = s.repo.UpdateTransaction(ctx, tx, p.UserID, id, in.TransactionType, in.Amount, in.TransactionDate, in.AccountID, in.TransferAccountID, in.CategoryID, in.Description)
		return e
	})
	return out, err
}

func (s *Service) DeleteTransaction(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error { return s.repo.DeleteTransaction(ctx, tx, p.UserID, id) })
}

func (s *Service) UploadReceipt(ctx context.Context, p domain.Principal, transactionID, filename, mime string, data []byte) (domain.Receipt, error) {
	if len(data) == 0 || len(data) > 10*1024*1024 {
		return domain.Receipt{}, validationf("invalid image size")
	}
	var uploadedPath string
	var old *domain.Receipt
	var out domain.Receipt
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if _, err := s.repo.Transaction(ctx, tx, p.UserID, transactionID); err != nil {
			return err
		}
		path, err := ReceiptPath(p.UserID, transactionID, filename)
		if err != nil {
			return err
		}
		if err = s.storage.Upload(ctx, path, mime, data); err != nil {
			return err
		}
		uploadedPath = path
		if previous, err := s.repo.ReceiptForTransaction(ctx, tx, p.UserID, transactionID); err == nil {
			old = &previous
		} else if !errors.Is(err, pgx.ErrNoRows) {
			return err
		}
		out, err = s.repo.UpsertReceipt(ctx, tx, p.UserID, transactionID, path, filename, mime, len(data))
		if err != nil {
			_ = s.storage.Delete(ctx, path)
			return err
		}
		return nil
	})
	if err != nil {
		if uploadedPath != "" {
			_ = s.storage.Delete(ctx, uploadedPath)
		}
		return domain.Receipt{}, err
	}
	if old != nil && old.ObjectPath != out.ObjectPath {
		_ = s.storage.Delete(ctx, old.ObjectPath)
	}
	return out, nil
}

func (s *Service) DeleteReceipt(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error {
		x, err := s.repo.Receipt(ctx, tx, p.UserID, id)
		if err != nil {
			return err
		}
		if err = s.storage.Delete(ctx, x.ObjectPath); err != nil {
			return err
		}
		return s.repo.DeleteReceipt(ctx, tx, p.UserID, id)
	})
}

type BudgetInput struct{ CategoryID, PeriodStart, PeriodEnd, AmountLimit string }

func (s *Service) validateBudget(ctx context.Context, tx pgx.Tx, p domain.Principal, in *BudgetInput) error {
	start, err := Date(in.PeriodStart)
	if err != nil {
		return err
	}
	in.PeriodStart = start
	end, err := Date(in.PeriodEnd)
	if err != nil {
		return err
	}
	in.PeriodEnd = end
	if end < start {
		return validationf("period end cannot be before period start")
	}
	amount, err := Amount(in.AmountLimit)
	if err != nil {
		return err
	}
	in.AmountLimit = amount
	if err := s.repo.ActiveCategory(ctx, tx, p.UserID, in.CategoryID, "expense"); err != nil {
		return validationf("budget category must exist, be expense, and be active")
	}
	return nil
}

func (s *Service) CreateBudget(ctx context.Context, p domain.Principal, in BudgetInput) (domain.Budget, error) {
	var out domain.Budget
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.validateBudget(ctx, tx, p, &in); e != nil {
			return e
		}
		var e error
		out, e = s.repo.CreateBudget(ctx, tx, p.UserID, in.CategoryID, in.PeriodStart, in.PeriodEnd, in.AmountLimit)
		return e
	})
	return out, err
}

func (s *Service) UpdateBudget(ctx context.Context, p domain.Principal, id string, in BudgetInput) (domain.Budget, error) {
	var out domain.Budget
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.validateBudget(ctx, tx, p, &in); e != nil {
			return e
		}
		var e error
		out, e = s.repo.UpdateBudget(ctx, tx, p.UserID, id, in.CategoryID, in.PeriodStart, in.PeriodEnd, in.AmountLimit)
		return e
	})
	return out, err
}

func (s *Service) DeleteBudget(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error { return s.repo.DeleteBudget(ctx, tx, p.UserID, id) })
}

type RecurringInput struct {
	TransactionType, Amount, AccountID, CategoryID, Cadence, NextDueDate string
	Description, EndsOn                                                  *string
	IsActive                                                             bool
}

func (s *Service) validateRecurring(ctx context.Context, tx pgx.Tx, p domain.Principal, in *RecurringInput) error {
	if !oneOf(in.TransactionType, "income", "expense") {
		return validationf("invalid transaction type")
	}
	if !oneOf(in.Cadence, "weekly", "monthly", "yearly") {
		return validationf("invalid cadence")
	}
	amount, err := Amount(in.Amount)
	if err != nil {
		return err
	}
	in.Amount = amount
	due, err := Date(in.NextDueDate)
	if err != nil {
		return err
	}
	in.NextDueDate = due
	if in.EndsOn != nil {
		end, err := Date(*in.EndsOn)
		if err != nil {
			return err
		}
		if end < due {
			return validationf("end date cannot be before next due date")
		}
		in.EndsOn = &end
	}
	if in.Description != nil && len(*in.Description) > 1000 {
		return validationf("description is too long")
	}
	if err := s.repo.ActiveAccount(ctx, tx, p.UserID, in.AccountID); err != nil {
		return validationf("account must exist and be active")
	}
	if err := s.repo.ActiveCategory(ctx, tx, p.UserID, in.CategoryID, in.TransactionType); err != nil {
		return validationf("category must exist, match transaction type, and be active")
	}
	return nil
}

func (s *Service) CreateRecurring(ctx context.Context, p domain.Principal, in RecurringInput) (domain.RecurringTransaction, error) {
	var out domain.RecurringTransaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.validateRecurring(ctx, tx, p, &in); e != nil {
			return e
		}
		var e error
		out, e = s.repo.CreateRecurring(ctx, tx, p.UserID, in.TransactionType, in.Amount, in.AccountID, in.CategoryID, in.Description, in.Cadence, in.NextDueDate, in.EndsOn)
		return e
	})
	return out, err
}

func (s *Service) UpdateRecurring(ctx context.Context, p domain.Principal, id string, in RecurringInput) (domain.RecurringTransaction, error) {
	var out domain.RecurringTransaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		if e := s.validateRecurring(ctx, tx, p, &in); e != nil {
			return e
		}
		var e error
		out, e = s.repo.UpdateRecurring(ctx, tx, p.UserID, id, in.TransactionType, in.Amount, in.AccountID, in.CategoryID, in.Description, in.Cadence, in.NextDueDate, in.EndsOn, in.IsActive)
		return e
	})
	return out, err
}

func (s *Service) DeleteRecurring(ctx context.Context, p domain.Principal, id string) error {
	return s.Do(ctx, p, func(tx pgx.Tx) error { return s.repo.DeleteRecurring(ctx, tx, p.UserID, id) })
}

func nextDue(date, cadence string) (string, error) {
	parsed, err := time.Parse("2006-01-02", date)
	if err != nil {
		return "", err
	}
	switch cadence {
	case "weekly":
		parsed = parsed.AddDate(0, 0, 7)
	case "monthly":
		parsed = parsed.AddDate(0, 1, 0)
	case "yearly":
		parsed = parsed.AddDate(1, 0, 0)
	default:
		return "", validationf("invalid cadence")
	}
	return parsed.Format("2006-01-02"), nil
}

func (s *Service) processRecurring(ctx context.Context, p domain.Principal, id string, createTransaction bool) (domain.Transaction, error) {
	var out domain.Transaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		recurring, err := s.repo.Recurring(ctx, tx, p.UserID, id, true)
		if err != nil {
			return err
		}
		today := time.Now().Format("2006-01-02")
		if !recurring.IsActive || recurring.NextDueDate > today || (recurring.EndsOn != nil && recurring.NextDueDate > *recurring.EndsOn) {
			return validationf("recurring transaction is not due")
		}
		if createTransaction {
			out, err = s.repo.CreateTransaction(ctx, tx, p.UserID, recurring.TransactionType, recurring.Amount, recurring.NextDueDate, recurring.AccountID, nil, &recurring.CategoryID, recurring.Description)
			if err != nil {
				return err
			}
		}
		next, err := nextDue(recurring.NextDueDate, recurring.Cadence)
		if err != nil {
			return err
		}
		active := recurring.EndsOn == nil || next <= *recurring.EndsOn
		return s.repo.AdvanceRecurring(ctx, tx, p.UserID, recurring.ID, recurring.NextDueDate, next, active)
	})
	return out, err
}

func (s *Service) ConfirmRecurring(ctx context.Context, p domain.Principal, id string) (domain.Transaction, error) {
	return s.processRecurring(ctx, p, id, true)
}
func (s *Service) SkipRecurring(ctx context.Context, p domain.Principal, id string) error {
	_, err := s.processRecurring(ctx, p, id, false)
	return err
}

func (s *Service) Dashboard(ctx context.Context, p domain.Principal, start, end, groupBy string) (domain.MoneyDashboard, error) {
	if _, err := Date(start); err != nil {
		return domain.MoneyDashboard{}, err
	}
	if _, err := Date(end); err != nil {
		return domain.MoneyDashboard{}, err
	}
	if end < start {
		return domain.MoneyDashboard{}, validationf("period end cannot be before period start")
	}
	if groupBy != "" && !oneOf(groupBy, "day", "week", "month") {
		return domain.MoneyDashboard{}, validationf("invalid group_by")
	}
	var out domain.MoneyDashboard
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.Dashboard(ctx, tx, p.UserID, start, end, groupBy)
		return e
	})
	return out, err
}

func (s *Service) Accounts(ctx context.Context, p domain.Principal, includeArchived bool) ([]domain.Account, error) {
	var out []domain.Account
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.Accounts(ctx, tx, p.UserID, includeArchived)
		return e
	})
	return out, err
}
func (s *Service) Account(ctx context.Context, p domain.Principal, id string) (domain.Account, error) {
	var out domain.Account
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Account(ctx, tx, p.UserID, id); return e })
	return out, err
}
func (s *Service) Categories(ctx context.Context, p domain.Principal, kind string, includeArchived bool) ([]domain.Category, error) {
	if kind != "" && !oneOf(kind, "income", "expense") {
		return nil, validationf("invalid category type")
	}
	var out []domain.Category
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.Categories(ctx, tx, p.UserID, kind, includeArchived)
		return e
	})
	return out, err
}
func (s *Service) Category(ctx context.Context, p domain.Principal, id string) (domain.Category, error) {
	var out domain.Category
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Category(ctx, tx, p.UserID, id); return e })
	return out, err
}
func (s *Service) Transactions(ctx context.Context, p domain.Principal, f postgres.TransactionFilter) ([]domain.Transaction, error) {
	if f.StartDate != "" {
		if _, err := Date(f.StartDate); err != nil {
			return nil, err
		}
	}
	if f.EndDate != "" {
		if _, err := Date(f.EndDate); err != nil {
			return nil, err
		}
	}
	if f.StartDate != "" && f.EndDate != "" && f.EndDate < f.StartDate {
		return nil, validationf("end date cannot be before start date")
	}
	if f.TransactionType != "" && !oneOf(f.TransactionType, "income", "expense", "transfer") {
		return nil, validationf("invalid transaction type")
	}
	if f.MinAmount != "" {
		if _, err := Amount(f.MinAmount); err != nil {
			return nil, err
		}
	}
	if f.MaxAmount != "" {
		if _, err := Amount(f.MaxAmount); err != nil {
			return nil, err
		}
	}
	var out []domain.Transaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Transactions(ctx, tx, p.UserID, f); return e })
	return out, err
}
func (s *Service) Transaction(ctx context.Context, p domain.Principal, id string) (domain.Transaction, error) {
	var out domain.Transaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Transaction(ctx, tx, p.UserID, id); return e })
	return out, err
}
func (s *Service) Budgets(ctx context.Context, p domain.Principal) ([]domain.Budget, error) {
	var out []domain.Budget
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Budgets(ctx, tx, p.UserID); return e })
	return out, err
}
func (s *Service) Budget(ctx context.Context, p domain.Principal, id string) (domain.Budget, error) {
	var out domain.Budget
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Budget(ctx, tx, p.UserID, id); return e })
	return out, err
}
func (s *Service) Recurring(ctx context.Context, p domain.Principal, dueOnly bool) ([]domain.RecurringTransaction, error) {
	var out []domain.RecurringTransaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error {
		var e error
		out, e = s.repo.RecurringTransactions(ctx, tx, p.UserID, dueOnly)
		return e
	})
	return out, err
}
func (s *Service) RecurringOne(ctx context.Context, p domain.Principal, id string) (domain.RecurringTransaction, error) {
	var out domain.RecurringTransaction
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Recurring(ctx, tx, p.UserID, id, false); return e })
	return out, err
}
func (s *Service) Receipt(ctx context.Context, p domain.Principal, id string) (domain.Receipt, error) {
	var out domain.Receipt
	err := s.Do(ctx, p, func(tx pgx.Tx) error { var e error; out, e = s.repo.Receipt(ctx, tx, p.UserID, id); return e })
	return out, err
}
