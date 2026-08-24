package httpapi

import (
	"encoding/json"
	"github.com/go-chi/chi/v5"
	"log/slog"
	"net/http"
	"strings"
	"tasker/backend/internal/auth"
	"tasker/backend/internal/httpapi/handlers"
	"tasker/backend/internal/httpapi/middleware"
	"time"
)

func New(h *handlers.Handlers, v *auth.Verifier, origin string, logger *slog.Logger) http.Handler {
	r := chi.NewRouter()
	r.Use(func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			p := r.URL.Path
			if strings.HasPrefix(p, "/api/v1/") {
				r.URL.Path = strings.TrimPrefix(p, "/api")
			} else if strings.HasPrefix(p, "/api/") {
				r.URL.Path = "/v1" + strings.TrimPrefix(p, "/api")
			} else if p == "/api/v1" {
				r.URL.Path = "/v1"
			}
			next.ServeHTTP(w, r)
		})
	})
	r.Use(middleware.RequestID, middleware.Logger(logger), middleware.Recovery(logger), middleware.SecurityHeaders)
	if origin != "" {
		r.Use(middleware.CORS(origin))
	}
	r.Get("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	authLimiter := middleware.NewRateLimiter(10, time.Minute)
	r.Group(func(ar chi.Router) {
		ar.Use(authLimiter)
		ar.Post("/v1/auth/login", h.Login)
		ar.Post("/v1/auth/register", h.Register)
	})

	r.Group(func(pr chi.Router) {
		pr.Use(middleware.Authenticate(v))
		pr.Get("/v1/me", h.Me)
		pr.Patch("/v1/auth/password", h.ChangePassword)
		pr.Get("/v1/projects", h.Projects)
		pr.Post("/v1/projects", h.CreateProject)
		pr.Patch("/v1/projects/{projectId}", h.PatchProject)
		pr.Delete("/v1/projects/{projectId}", h.DeleteProject)
		pr.Get("/v1/tags", h.Tags)
		pr.Post("/v1/tags", h.CreateTag)
		pr.Patch("/v1/tags/{tagId}", h.PatchTag)
		pr.Delete("/v1/tags/{tagId}", h.DeleteTag)
		pr.Get("/v1/tasks", h.Tasks)
		pr.Post("/v1/tasks", h.CreateTask)
		pr.Get("/v1/tasks/{taskId}", h.Task)
		pr.Patch("/v1/tasks/{taskId}", h.PatchTask)
		pr.Delete("/v1/tasks/{taskId}", h.DeleteTask)
		pr.Patch("/v1/tasks/{taskId}/completion", h.Completion)
		pr.Post("/v1/tasks/{taskId}/subtasks", h.CreateSubtask)
		pr.Patch("/v1/tasks/{taskId}/subtasks/{subtaskId}", h.PatchSubtask)
		pr.Delete("/v1/tasks/{taskId}/subtasks/{subtaskId}", h.DeleteSubtask)
		pr.Get("/v1/notes", h.Notes)
		pr.Post("/v1/notes", h.CreateNote)
		pr.Get("/v1/notes/{noteId}", h.Note)
		pr.Patch("/v1/notes/{noteId}", h.PatchNote)
		pr.Delete("/v1/notes/{noteId}", h.DeleteNote)
		pr.Put("/v1/notes/{noteId}/tasks/{taskId}", h.Link)
		pr.Delete("/v1/notes/{noteId}/tasks/{taskId}", h.Unlink)
		pr.Post("/v1/notes/{noteId}/images", h.UploadImage)
		pr.Patch("/v1/note-images/{imageId}", h.PatchImage)
		pr.Delete("/v1/note-images/{imageId}", h.DeleteImage)
		pr.Get("/v1/note-images/{imageId}/access", h.ImageAccess)
		pr.Get("/v1/search", h.Search)
		pr.Get("/v1/accounts", h.Accounts)
		pr.Post("/v1/accounts", h.CreateAccount)
		pr.Get("/v1/accounts/{accountId}", h.Account)
		pr.Patch("/v1/accounts/{accountId}", h.PatchAccount)
		pr.Delete("/v1/accounts/{accountId}", h.DeleteAccount)
		pr.Get("/v1/categories", h.Categories)
		pr.Post("/v1/categories", h.CreateCategory)
		pr.Get("/v1/categories/{categoryId}", h.Category)
		pr.Patch("/v1/categories/{categoryId}", h.PatchCategory)
		pr.Delete("/v1/categories/{categoryId}", h.DeleteCategory)
		pr.Get("/v1/transactions", h.Transactions)
		pr.Post("/v1/transactions", h.CreateTransaction)
		pr.Get("/v1/transactions/{transactionId}", h.Transaction)
		pr.Patch("/v1/transactions/{transactionId}", h.PatchTransaction)
		pr.Delete("/v1/transactions/{transactionId}", h.DeleteTransaction)
		pr.Post("/v1/transactions/{transactionId}/receipt", h.UploadReceipt)
		pr.Delete("/v1/transaction-receipts/{receiptId}", h.DeleteReceipt)
		pr.Get("/v1/transaction-receipts/{receiptId}/access", h.ReceiptAccess)
		pr.Get("/v1/budgets", h.Budgets)
		pr.Post("/v1/budgets", h.CreateBudget)
		pr.Get("/v1/budgets/{budgetId}", h.Budget)
		pr.Patch("/v1/budgets/{budgetId}", h.PatchBudget)
		pr.Delete("/v1/budgets/{budgetId}", h.DeleteBudget)
		pr.Get("/v1/recurring-transactions", h.RecurringTransactions)
		pr.Post("/v1/recurring-transactions", h.CreateRecurring)
		pr.Get("/v1/recurring-transactions/due", h.DueRecurring)
		pr.Get("/v1/recurring-transactions/{recurringTransactionId}", h.RecurringTransaction)
		pr.Patch("/v1/recurring-transactions/{recurringTransactionId}", h.PatchRecurring)
		pr.Delete("/v1/recurring-transactions/{recurringTransactionId}", h.DeleteRecurring)
		pr.Post("/v1/recurring-transactions/{recurringTransactionId}/confirm", h.ConfirmRecurring)
		pr.Post("/v1/recurring-transactions/{recurringTransactionId}/skip", h.SkipRecurring)
		pr.Get("/v1/targets", h.Targets)
		pr.Post("/v1/targets", h.CreateTarget)
		pr.Get("/v1/targets/{targetId}", h.Target)
		pr.Patch("/v1/targets/{targetId}", h.PatchTarget)
		pr.Delete("/v1/targets/{targetId}", h.DeleteTarget)
		pr.Post("/v1/targets/{targetId}/contribute", h.ContributeTarget)
		pr.Get("/v1/money/dashboard", h.MoneyDashboard)
	})
	r.NotFound(newSPAHandler().ServeHTTP)
	return r
}

