package domain

import "time"

type Principal struct {
	UserID   string `json:"id"`
	Username string `json:"username,omitempty"`
}
type Admin struct {
	ID           string `json:"id"`
	Username     string `json:"username"`
	PasswordHash string `json:"-"`
}
type Project struct {
	ID         string    `json:"id"`
	Name       string    `json:"name"`
	Color      *string   `json:"color"`
	IsArchived bool      `json:"is_archived"`
	CreatedAt  time.Time `json:"created_at"`
	UpdatedAt  time.Time `json:"updated_at"`
}
type Tag struct {
	ID        string    `json:"id"`
	Name      string    `json:"name"`
	Color     *string   `json:"color"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}
type Subtask struct {
	ID        string    `json:"id"`
	TaskID    string    `json:"task_id"`
	Title     string    `json:"title"`
	Completed bool      `json:"completed"`
	Position  int       `json:"position"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}
type Task struct {
	ID          string     `json:"id"`
	Title       string     `json:"title"`
	Description *string    `json:"description"`
	DueDate     *string    `json:"due_date"`
	ProjectID   *string    `json:"project_id"`
	Status      string     `json:"status"`
	CompletedAt *time.Time `json:"completed_at"`
	Priority    int16      `json:"priority"`
	Tags        []Tag      `json:"tags"`
	Subtasks    []Subtask  `json:"subtasks"`
	CreatedAt   time.Time  `json:"created_at"`
	UpdatedAt   time.Time  `json:"updated_at"`
}
type Note struct {
	ID              string       `json:"id"`
	Title           string       `json:"title"`
	ContentMD       string       `json:"content_md"`
	ReminderAt      *time.Time   `json:"reminder_at,omitempty"`
	ReminderOffsets []int        `json:"reminder_offsets,omitempty"`
	Tags            []Tag        `json:"tags"`
	Tasks           []LinkedTask `json:"tasks"`
	CreatedAt       time.Time    `json:"created_at"`
	UpdatedAt       time.Time    `json:"updated_at"`
}
type LinkedTask struct {
	ID    string `json:"id"`
	Title string `json:"title"`
}
type Image struct {
	ID               string    `json:"id"`
	NoteID           string    `json:"note_id"`
	ObjectPath       string    `json:"object_path"`
	OriginalFilename string    `json:"original_filename"`
	MIMEType         string    `json:"mime_type"`
	ByteSize         int       `json:"byte_size"`
	AltText          *string   `json:"alt_text"`
	Width            *int      `json:"width"`
	Height           *int      `json:"height"`
	CreatedAt        time.Time `json:"created_at"`
}
type Page[T any] struct {
	Items      []T     `json:"items"`
	NextCursor *string `json:"next_cursor,omitempty"`
}

// Money amounts deliberately remain strings at the API boundary. PostgreSQL
// numeric preserves their exact value and the service rejects floating point
// notation before a value reaches the database.
type Account struct {
	ID          string     `json:"id"`
	Name        string     `json:"name"`
	AccountType string     `json:"account_type"`
	Currency    string     `json:"currency"`
	ArchivedAt  *time.Time `json:"archived_at,omitempty"`
	Balance     string     `json:"balance"`
	CreatedAt   time.Time  `json:"created_at"`
	UpdatedAt   time.Time  `json:"updated_at"`
}

type Category struct {
	ID           string     `json:"id"`
	Name         string     `json:"name"`
	CategoryType string     `json:"category_type"`
	Icon         *string    `json:"icon,omitempty"`
	Color        *string    `json:"color,omitempty"`
	ArchivedAt   *time.Time `json:"archived_at,omitempty"`
	CreatedAt    time.Time  `json:"created_at"`
	UpdatedAt    time.Time  `json:"updated_at"`
}

type Receipt struct {
	ID               string    `json:"id"`
	TransactionID    string    `json:"transaction_id"`
	ObjectPath       string    `json:"-"`
	OriginalFilename string    `json:"original_filename"`
	MIMEType         string    `json:"mime_type"`
	ByteSize         int       `json:"byte_size"`
	CreatedAt        time.Time `json:"created_at"`
}

type Transaction struct {
	ID                  string    `json:"id"`
	TransactionType     string    `json:"transaction_type"`
	Amount              string    `json:"amount"`
	TransactionDate     string    `json:"transaction_date"`
	AccountID           string    `json:"account_id"`
	AccountName         string    `json:"account_name"`
	TransferAccountID   *string   `json:"transfer_account_id,omitempty"`
	TransferAccountName *string   `json:"transfer_account_name,omitempty"`
	CategoryID          *string   `json:"category_id,omitempty"`
	CategoryName        *string   `json:"category_name,omitempty"`
	Description         *string   `json:"description,omitempty"`
	Receipt             *Receipt  `json:"receipt,omitempty"`
	CreatedAt           time.Time `json:"created_at"`
	UpdatedAt           time.Time `json:"updated_at"`
}

type Budget struct {
	ID           string    `json:"id"`
	CategoryID   string    `json:"category_id"`
	CategoryName string    `json:"category_name"`
	PeriodStart  string    `json:"period_start"`
	PeriodEnd    string    `json:"period_end"`
	AmountLimit  string    `json:"amount_limit"`
	Spent        string    `json:"spent"`
	Remaining    string    `json:"remaining"`
	PercentUsed  string    `json:"percent_used"`
	IsOverBudget bool      `json:"is_over_budget"`
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
}

type RecurringTransaction struct {
	ID              string    `json:"id"`
	TransactionType string    `json:"transaction_type"`
	Amount          string    `json:"amount"`
	AccountID       string    `json:"account_id"`
	CategoryID      string    `json:"category_id"`
	Description     *string   `json:"description,omitempty"`
	Cadence         string    `json:"cadence"`
	NextDueDate     string    `json:"next_due_date"`
	EndsOn          *string   `json:"ends_on,omitempty"`
	IsActive        bool      `json:"is_active"`
	LastProcessedOn *string   `json:"last_processed_on,omitempty"`
	CreatedAt       time.Time `json:"created_at"`
	UpdatedAt       time.Time `json:"updated_at"`
}

type CategorySpend struct {
	CategoryID   string `json:"category_id"`
	CategoryName string `json:"category_name"`
	Amount       string `json:"amount"`
}

type MoneyTrendPoint struct {
	Period  string `json:"period"`
	Income  string `json:"income"`
	Expense string `json:"expense"`
}

type MoneyDashboard struct {
	TotalBalance  string            `json:"total_balance"`
	Income        string            `json:"income"`
	Expense       string            `json:"expense"`
	CategorySpend []CategorySpend   `json:"category_spend"`
	Trend         []MoneyTrendPoint `json:"trend"`
	TargetSummary *TargetSummary    `json:"target_summary,omitempty"`
}

type Target struct {
	ID              string    `json:"id"`
	Name            string    `json:"name"`
	TargetAmount    string    `json:"target_amount"`
	CurrentAmount   string    `json:"current_amount"`
	TargetDate      *string   `json:"target_date,omitempty"`
	CategoryID      *string   `json:"category_id,omitempty"`
	CategoryName    *string   `json:"category_name,omitempty"`
	AccountID       *string   `json:"account_id,omitempty"`
	AccountName     *string   `json:"account_name,omitempty"`
	Color           *string   `json:"color,omitempty"`
	Icon            *string   `json:"icon,omitempty"`
	Status          string    `json:"status"`
	Notes           *string   `json:"notes,omitempty"`
	ProgressPercent string    `json:"progress_percent"`
	RemainingAmount string    `json:"remaining_amount"`
	IsAchieved      bool      `json:"is_achieved"`
	CreatedAt       time.Time `json:"created_at"`
	UpdatedAt       time.Time `json:"updated_at"`
}

type TargetSummary struct {
	TotalTargetsCount    int    `json:"total_targets_count"`
	ActiveTargetsCount   int    `json:"active_targets_count"`
	AchievedTargetsCount int    `json:"achieved_targets_count"`
	TotalTargetAmount    string `json:"total_target_amount"`
	TotalCurrentAmount   string `json:"total_current_amount"`
	OverallProgress      string `json:"overall_progress"`
}
