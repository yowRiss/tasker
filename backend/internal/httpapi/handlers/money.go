package handlers

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"strings"

	"tasker/backend/internal/domain"
	"tasker/backend/internal/httpapi/response"
	"tasker/backend/internal/repository/postgres"
	"tasker/backend/internal/service"
)

func boolQuery(r *http.Request, key string) bool {
	v, _ := strconv.ParseBool(r.URL.Query().Get(key))
	return v
}

func (h *Handlers) Accounts(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Accounts(r.Context(), principal(r), boolQuery(r, "include_archived"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}

func (h *Handlers) Account(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Account(r.Context(), principal(r), id(r, "accountId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

func (h *Handlers) CreateAccount(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name        string `json:"name"`
		AccountType string `json:"account_type"`
	}
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.CreateAccount(r.Context(), principal(r), q.Name, q.AccountType)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}

func (h *Handlers) PatchAccount(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name        *string `json:"name"`
		AccountType *string `json:"account_type"`
		IsArchived  *bool   `json:"is_archived"`
	}
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.UpdateAccount(r.Context(), principal(r), id(r, "accountId"), q.Name, q.AccountType, q.IsArchived)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

func (h *Handlers) DeleteAccount(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteAccount(r.Context(), principal(r), id(r, "accountId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handlers) Categories(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Categories(r.Context(), principal(r), r.URL.Query().Get("type"), boolQuery(r, "include_archived"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}

func (h *Handlers) Category(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Category(r.Context(), principal(r), id(r, "categoryId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

func (h *Handlers) CreateCategory(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name         string  `json:"name"`
		CategoryType string  `json:"category_type"`
		Icon         *string `json:"icon"`
		Color        *string `json:"color"`
	}
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.CreateCategory(r.Context(), principal(r), q.Name, q.CategoryType, q.Icon, q.Color)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}

func (h *Handlers) PatchCategory(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name       *string `json:"name"`
		Icon       *string `json:"icon"`
		Color      *string `json:"color"`
		IsArchived *bool   `json:"is_archived"`
	}
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.UpdateCategory(r.Context(), principal(r), id(r, "categoryId"), q.Name, q.Icon, q.Color, q.IsArchived)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

func (h *Handlers) DeleteCategory(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteCategory(r.Context(), principal(r), id(r, "categoryId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// optionalString distinguishes an omitted patch field from an explicit JSON null.
type optionalString struct {
	Set   bool
	Value *string
}

func (o *optionalString) UnmarshalJSON(data []byte) error {
	o.Set = true
	if bytes.Equal(data, []byte("null")) {
		o.Value = nil
		return nil
	}
	var value string
	if err := json.Unmarshal(data, &value); err != nil {
		return err
	}
	o.Value = &value
	return nil
}

type transactionRequest struct {
	TransactionType   *string        `json:"transaction_type"`
	Amount            *string        `json:"amount"`
	TransactionDate   *string        `json:"transaction_date"`
	AccountID         *string        `json:"account_id"`
	TransferAccountID optionalString `json:"transfer_account_id"`
	CategoryID        optionalString `json:"category_id"`
	Description       optionalString `json:"description"`
}

func inputForCreate(q transactionRequest) (service.TransactionInput, error) {
	if q.TransactionType == nil || q.Amount == nil || q.TransactionDate == nil || q.AccountID == nil {
		return service.TransactionInput{}, service.ErrValidation
	}
	return service.TransactionInput{TransactionType: *q.TransactionType, Amount: *q.Amount, TransactionDate: *q.TransactionDate, AccountID: *q.AccountID, TransferAccountID: q.TransferAccountID.Value, CategoryID: q.CategoryID.Value, Description: q.Description.Value}, nil
}
func inputForPatch(q transactionRequest, current domain.Transaction) service.TransactionInput {
	in := service.TransactionInput{TransactionType: current.TransactionType, Amount: current.Amount, TransactionDate: current.TransactionDate, AccountID: current.AccountID, TransferAccountID: current.TransferAccountID, CategoryID: current.CategoryID, Description: current.Description}
	if q.TransactionType != nil {
		in.TransactionType = *q.TransactionType
	}
	if q.Amount != nil {
		in.Amount = *q.Amount
	}
	if q.TransactionDate != nil {
		in.TransactionDate = *q.TransactionDate
	}
	if q.AccountID != nil {
		in.AccountID = *q.AccountID
	}
	if q.TransferAccountID.Set {
		in.TransferAccountID = q.TransferAccountID.Value
	}
	if q.CategoryID.Set {
		in.CategoryID = q.CategoryID.Value
	}
	if q.Description.Set {
		in.Description = q.Description.Value
	}
	return in
}

func (h *Handlers) Transactions(w http.ResponseWriter, r *http.Request) {
	f := postgres.TransactionFilter{StartDate: r.URL.Query().Get("start_date"), EndDate: r.URL.Query().Get("end_date"), AccountID: r.URL.Query().Get("account_id"), CategoryID: r.URL.Query().Get("category_id"), TransactionType: r.URL.Query().Get("type"), Query: strings.TrimSpace(r.URL.Query().Get("q")), MinAmount: r.URL.Query().Get("min_amount"), MaxAmount: r.URL.Query().Get("max_amount"), Limit: limit(r)}
	x, err := h.service.Transactions(r.Context(), principal(r), f)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}
func (h *Handlers) Transaction(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Transaction(r.Context(), principal(r), id(r, "transactionId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}
func (h *Handlers) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	var q transactionRequest
	if !decode(w, r, &q) {
		return
	}
	in, err := inputForCreate(q)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	x, err := h.service.CreateTransaction(r.Context(), principal(r), in)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}
func (h *Handlers) PatchTransaction(w http.ResponseWriter, r *http.Request) {
	var q transactionRequest
	if !decode(w, r, &q) {
		return
	}
	p := principal(r)
	current, err := h.service.Transaction(r.Context(), p, id(r, "transactionId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	x, err := h.service.UpdateTransaction(r.Context(), p, current.ID, inputForPatch(q, current))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}
func (h *Handlers) DeleteTransaction(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteTransaction(r.Context(), principal(r), id(r, "transactionId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func uploadFile(w http.ResponseWriter, r *http.Request) (string, string, []byte, bool) {
	r.Body = http.MaxBytesReader(w, r.Body, 11*1024*1024)
	if err := r.ParseMultipartForm(11 * 1024 * 1024); err != nil {
		response.ProblemJSON(w, r, 413, "request_too_large", "Upload exceeds request limit", nil)
		return "", "", nil, false
	}
	f, head, err := r.FormFile("file")
	if err != nil {
		response.ProblemJSON(w, r, 400, "missing_file", "Image file is required", nil)
		return "", "", nil, false
	}
	defer f.Close()
	b, err := io.ReadAll(io.LimitReader(f, 10*1024*1024+1))
	if err != nil || len(b) > 10*1024*1024 {
		response.ProblemJSON(w, r, 413, "file_too_large", "Image exceeds 10 MiB", nil)
		return "", "", nil, false
	}
	mime := http.DetectContentType(b)
	if mime == "image/jpg" {
		mime = "image/jpeg"
	}
	if mime != "image/jpeg" && mime != "image/png" && mime != "image/webp" && mime != "image/gif" {
		response.ProblemJSON(w, r, 415, "unsupported_media", "Supported image types are JPEG, PNG, WebP, and GIF", nil)
		return "", "", nil, false
	}
	return head.Filename, mime, b, true
}
func (h *Handlers) UploadReceipt(w http.ResponseWriter, r *http.Request) {
	name, mime, data, ok := uploadFile(w, r)
	if !ok {
		return
	}
	x, err := h.service.UploadReceipt(r.Context(), principal(r), id(r, "transactionId"), name, mime, data)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}
func (h *Handlers) DeleteReceipt(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteReceipt(r.Context(), principal(r), id(r, "receiptId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (h *Handlers) ReceiptAccess(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Receipt(r.Context(), principal(r), id(r, "receiptId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	url, err := h.service.StorageSignedURL(r.Context(), x.ObjectPath)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"url": url, "expires_in": 3600})
}

type budgetRequest struct {
	CategoryID  string `json:"category_id"`
	PeriodStart string `json:"period_start"`
	PeriodEnd   string `json:"period_end"`
	AmountLimit string `json:"amount_limit"`
}

func budgetInput(q budgetRequest) service.BudgetInput {
	return service.BudgetInput{CategoryID: q.CategoryID, PeriodStart: q.PeriodStart, PeriodEnd: q.PeriodEnd, AmountLimit: q.AmountLimit}
}
func (h *Handlers) Budgets(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Budgets(r.Context(), principal(r))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}
func (h *Handlers) Budget(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Budget(r.Context(), principal(r), id(r, "budgetId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}
func (h *Handlers) CreateBudget(w http.ResponseWriter, r *http.Request) {
	var q budgetRequest
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.CreateBudget(r.Context(), principal(r), budgetInput(q))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}
func (h *Handlers) PatchBudget(w http.ResponseWriter, r *http.Request) {
	var q budgetRequest
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.UpdateBudget(r.Context(), principal(r), id(r, "budgetId"), budgetInput(q))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}
func (h *Handlers) DeleteBudget(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteBudget(r.Context(), principal(r), id(r, "budgetId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

type recurringRequest struct {
	TransactionType string  `json:"transaction_type"`
	Amount          string  `json:"amount"`
	AccountID       string  `json:"account_id"`
	CategoryID      string  `json:"category_id"`
	Description     *string `json:"description"`
	Cadence         string  `json:"cadence"`
	NextDueDate     string  `json:"next_due_date"`
	EndsOn          *string `json:"ends_on"`
	IsActive        bool    `json:"is_active"`
}

func recurringInput(q recurringRequest) service.RecurringInput {
	return service.RecurringInput{TransactionType: q.TransactionType, Amount: q.Amount, AccountID: q.AccountID, CategoryID: q.CategoryID, Description: q.Description, Cadence: q.Cadence, NextDueDate: q.NextDueDate, EndsOn: q.EndsOn, IsActive: q.IsActive}
}
func (h *Handlers) RecurringTransactions(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Recurring(r.Context(), principal(r), false)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}
func (h *Handlers) RecurringTransaction(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.RecurringOne(r.Context(), principal(r), id(r, "recurringTransactionId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}
func (h *Handlers) CreateRecurring(w http.ResponseWriter, r *http.Request) {
	var q recurringRequest
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.CreateRecurring(r.Context(), principal(r), recurringInput(q))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}
func (h *Handlers) PatchRecurring(w http.ResponseWriter, r *http.Request) {
	var q recurringRequest
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.UpdateRecurring(r.Context(), principal(r), id(r, "recurringTransactionId"), recurringInput(q))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}
func (h *Handlers) DeleteRecurring(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteRecurring(r.Context(), principal(r), id(r, "recurringTransactionId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (h *Handlers) DueRecurring(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Recurring(r.Context(), principal(r), true)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}
func (h *Handlers) ConfirmRecurring(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.ConfirmRecurring(r.Context(), principal(r), id(r, "recurringTransactionId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}
func (h *Handlers) SkipRecurring(w http.ResponseWriter, r *http.Request) {
	if err := h.service.SkipRecurring(r.Context(), principal(r), id(r, "recurringTransactionId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handlers) MoneyDashboard(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Dashboard(r.Context(), principal(r), r.URL.Query().Get("start_date"), r.URL.Query().Get("end_date"), r.URL.Query().Get("group_by"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

type targetRequest struct {
	Name          string         `json:"name"`
	TargetAmount  string         `json:"target_amount"`
	CurrentAmount *string        `json:"current_amount"`
	TargetDate    optionalString `json:"target_date"`
	CategoryID    optionalString `json:"category_id"`
	AccountID     optionalString `json:"account_id"`
	Color         optionalString `json:"color"`
	Icon          optionalString `json:"icon"`
	Status        *string        `json:"status"`
	Notes         optionalString `json:"notes"`
}

func targetInputForCreate(q targetRequest) service.TargetInput {
	curr := "0"
	if q.CurrentAmount != nil {
		curr = *q.CurrentAmount
	}
	return service.TargetInput{
		Name:          q.Name,
		TargetAmount:  q.TargetAmount,
		CurrentAmount: curr,
		TargetDate:    q.TargetDate.Value,
		CategoryID:    q.CategoryID.Value,
		AccountID:     q.AccountID.Value,
		Color:         q.Color.Value,
		Icon:          q.Icon.Value,
		Status:        q.Status,
		Notes:         q.Notes.Value,
	}
}

func targetInputForPatch(q targetRequest, current domain.Target) service.TargetInput {
	in := service.TargetInput{
		Name:          current.Name,
		TargetAmount:  current.TargetAmount,
		CurrentAmount: current.CurrentAmount,
		TargetDate:    current.TargetDate,
		CategoryID:    current.CategoryID,
		AccountID:     current.AccountID,
		Color:         current.Color,
		Icon:          current.Icon,
		Status:        &current.Status,
		Notes:         current.Notes,
	}
	if strings.TrimSpace(q.Name) != "" {
		in.Name = q.Name
	}
	if strings.TrimSpace(q.TargetAmount) != "" {
		in.TargetAmount = q.TargetAmount
	}
	if q.CurrentAmount != nil {
		in.CurrentAmount = *q.CurrentAmount
	}
	if q.TargetDate.Set {
		in.TargetDate = q.TargetDate.Value
	}
	if q.CategoryID.Set {
		in.CategoryID = q.CategoryID.Value
	}
	if q.AccountID.Set {
		in.AccountID = q.AccountID.Value
	}
	if q.Color.Set {
		in.Color = q.Color.Value
	}
	if q.Icon.Set {
		in.Icon = q.Icon.Value
	}
	if q.Status != nil {
		in.Status = q.Status
	}
	if q.Notes.Set {
		in.Notes = q.Notes.Value
	}
	return in
}

type targetContributeRequest struct {
	Amount     string `json:"amount"`
	IsWithdraw bool   `json:"is_withdraw"`
}

func (h *Handlers) Targets(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Targets(r.Context(), principal(r), r.URL.Query().Get("status"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{"items": x})
}

func (h *Handlers) Target(w http.ResponseWriter, r *http.Request) {
	x, err := h.service.Target(r.Context(), principal(r), id(r, "targetId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

func (h *Handlers) CreateTarget(w http.ResponseWriter, r *http.Request) {
	var q targetRequest
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.CreateTarget(r.Context(), principal(r), targetInputForCreate(q))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusCreated, x)
}

func (h *Handlers) PatchTarget(w http.ResponseWriter, r *http.Request) {
	var q targetRequest
	if !decode(w, r, &q) {
		return
	}
	p := principal(r)
	current, err := h.service.Target(r.Context(), p, id(r, "targetId"))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	x, err := h.service.UpdateTarget(r.Context(), p, current.ID, targetInputForPatch(q, current))
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

func (h *Handlers) DeleteTarget(w http.ResponseWriter, r *http.Request) {
	if err := h.service.DeleteTarget(r.Context(), principal(r), id(r, "targetId")); err != nil {
		writeErr(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handlers) ContributeTarget(w http.ResponseWriter, r *http.Request) {
	var q targetContributeRequest
	if !decode(w, r, &q) {
		return
	}
	x, err := h.service.ContributeTarget(r.Context(), principal(r), id(r, "targetId"), q.Amount, q.IsWithdraw)
	if err != nil {
		writeErr(w, r, err)
		return
	}
	response.JSON(w, http.StatusOK, x)
}

