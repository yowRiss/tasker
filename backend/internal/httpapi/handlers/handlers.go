package handlers

import (
	"encoding/json"
	"errors"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"io"
	"log/slog"
	"net/http"
	"strconv"
	"strings"
	"tasker/backend/internal/auth"
	"tasker/backend/internal/domain"
	"tasker/backend/internal/httpapi/middleware"
	"tasker/backend/internal/httpapi/response"
	"tasker/backend/internal/repository/postgres"
	"tasker/backend/internal/service"
	"time"
)

type Handlers struct {
	service  *service.Service
	repo     *postgres.Repository
	verifier *auth.Verifier
}

func New(s *service.Service, r *postgres.Repository, v *auth.Verifier) *Handlers {
	return &Handlers{service: s, repo: r, verifier: v}
}
func principal(r *http.Request) domain.Principal { p, _ := middleware.Principal(r); return p }
func decode(w http.ResponseWriter, r *http.Request, v any) bool {
	bodyBytes, err := io.ReadAll(io.LimitReader(r.Body, 1<<20))
	if err != nil {
		slog.Error("failed to read request body",
			"request_id", response.RequestID(r),
			"method", r.Method,
			"path", r.URL.Path,
			"remote_addr", r.RemoteAddr,
			"error", err,
		)
		response.ProblemJSON(w, r, 400, "malformed_json", "Failed to read request body: "+err.Error(), nil)
		return false
	}

	bodyStr := string(bodyBytes)

	if len(bodyBytes) == 0 {
		slog.Warn("empty request body received",
			"request_id", response.RequestID(r),
			"method", r.Method,
			"path", r.URL.Path,
			"remote_addr", r.RemoteAddr,
			"content_type", r.Header.Get("Content-Type"),
		)
		detail := map[string]string{
			"method":       r.Method,
			"path":         r.URL.Path,
			"content_type": r.Header.Get("Content-Type"),
			"raw_body":     "[EMPTY BODY]",
		}
		response.ProblemJSON(w, r, 400, "malformed_json", "Malformed JSON: request body is empty (0 bytes)", detail)
		return false
	}

	if err := json.Unmarshal(bodyBytes, v); err != nil {
		slog.Error("json unmarshal failed",
			"request_id", response.RequestID(r),
			"method", r.Method,
			"path", r.URL.Path,
			"remote_addr", r.RemoteAddr,
			"content_type", r.Header.Get("Content-Type"),
			"raw_body", bodyStr,
			"error", err,
		)
		detail := map[string]string{
			"method":       r.Method,
			"path":         r.URL.Path,
			"content_type": r.Header.Get("Content-Type"),
			"raw_body":     bodyStr,
			"decode_error": err.Error(),
		}
		response.ProblemJSON(w, r, 400, "malformed_json", "Malformed JSON: "+err.Error(), detail)
		return false
	}
	return true
}
func writeErr(w http.ResponseWriter, r *http.Request, e error) {
	if errors.Is(e, service.ErrValidation) {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", nil)
		return
	}
	if errors.Is(e, pgx.ErrNoRows) {
		response.ProblemJSON(w, r, 404, "not_found", "Resource not found", nil)
		return
	}
	var dbErr *pgconn.PgError
	if errors.As(e, &dbErr) {
		if dbErr.Code == "23503" || dbErr.Code == "23505" {
			response.ProblemJSON(w, r, 409, "conflict", "The record has dependent data or conflicts with an existing record", nil)
			return
		}
		if dbErr.Code == "22P02" {
			response.ProblemJSON(w, r, 400, "invalid_uuid", "Invalid identifier or syntax format", nil)
			return
		}
	}
	slog.Error("handler error", "request_id", response.RequestID(r), "error", e)
	response.ProblemJSON(w, r, 500, "internal_error", "Internal server error: "+e.Error(), nil)
}
func id(r *http.Request, n string) string { return chi.URLParam(r, n) }
func limit(r *http.Request) int {
	n, _ := strconv.Atoi(r.URL.Query().Get("limit"))
	if n < 1 {
		n = 30
	}
	if n > 100 {
		n = 100
	}
	return n
}
func (h *Handlers) Me(w http.ResponseWriter, r *http.Request) { response.JSON(w, 200, principal(r)) }
func (h *Handlers) Projects(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x []domain.Project
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { var e error; x, e = h.repo.Projects(r.Context(), tx, p.UserID); return e })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, map[string]any{"items": x})
}
func (h *Handlers) CreateProject(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name       string  `json:"name"`
		Color      *string `json:"color"`
		IsArchived bool    `json:"is_archived"`
	}
	if !decode(w, r, &q) {
		return
	}
	n, e := service.Title(q.Name, 80)
	if e != nil {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"name": e.Error()})
		return
	}
	p := principal(r)
	var x domain.Project
	e = h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.CreateProject(r.Context(), tx, p.UserID, n, q.Color, q.IsArchived)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 201, x)
}
func (h *Handlers) PatchProject(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name       *string `json:"name"`
		Color      *string `json:"color"`
		IsArchived *bool   `json:"is_archived"`
	}
	if !decode(w, r, &q) {
		return
	}
	if q.Name != nil {
		n, e := service.Title(*q.Name, 80)
		if e != nil {
			response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"name": e.Error()})
			return
		}
		q.Name = &n
	}
	p := principal(r)
	var x domain.Project
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.UpdateProject(r.Context(), tx, p.UserID, id(r, "projectId"), q.Name, q.Color, q.IsArchived)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) DeleteProject(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { return h.repo.DeleteProject(r.Context(), tx, p.UserID, id(r, "projectId")) })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) Tags(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x []domain.Tag
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { var e error; x, e = h.repo.Tags(r.Context(), tx, p.UserID); return e })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, map[string]any{"items": x})
}
func (h *Handlers) CreateTag(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name  string  `json:"name"`
		Color *string `json:"color"`
	}
	if !decode(w, r, &q) {
		return
	}
	n, e := service.Title(q.Name, 40)
	if e != nil {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"name": e.Error()})
		return
	}
	p := principal(r)
	var x domain.Tag
	e = h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.CreateTag(r.Context(), tx, p.UserID, n, q.Color)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 201, x)
}
func (h *Handlers) PatchTag(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Name  *string `json:"name"`
		Color *string `json:"color"`
	}
	if !decode(w, r, &q) {
		return
	}
	if q.Name != nil {
		n, e := service.Title(*q.Name, 40)
		if e != nil {
			response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"name": e.Error()})
			return
		}
		q.Name = &n
	}
	p := principal(r)
	var x domain.Tag
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.UpdateTag(r.Context(), tx, p.UserID, id(r, "tagId"), q.Name, q.Color)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) DeleteTag(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { return h.repo.DeleteTag(r.Context(), tx, p.UserID, id(r, "tagId")) })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) Tasks(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x []domain.Task
	st := r.URL.Query().Get("status")
	if st == "archived" {
		st = "completed"
	} else if st == "all" {
		st = ""
	}
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.Tasks(r.Context(), tx, p.UserID, st, r.URL.Query().Get("project_id"), r.URL.Query().Get("q"), limit(r))
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, map[string]any{"items": x})
}

type subtaskPayload struct {
	ID        *string    `json:"id"`
	TaskID    *string    `json:"task_id"`
	Title     string     `json:"title"`
	Completed bool       `json:"completed"`
	Position  int        `json:"position"`
	CreatedAt *time.Time `json:"created_at"`
	UpdatedAt *time.Time `json:"updated_at"`
}

type taskInput struct {
	ID          *string           `json:"id"`
	Title       *string           `json:"title"`
	Description *string           `json:"description"`
	DueDate     *string           `json:"due_date"`
	Priority    *int16            `json:"priority"`
	ProjectID   *string           `json:"project_id"`
	Status      *string           `json:"status"`
	CompletedAt *time.Time        `json:"completed_at"`
	TagIDs      *[]string         `json:"tag_ids"`
	Tags        *[]any            `json:"tags"`
	Subtasks    *[]subtaskPayload `json:"subtasks"`
	CreatedAt   *time.Time        `json:"created_at"`
	UpdatedAt   *time.Time        `json:"updated_at"`
}

func cleanStrPtr(p *string) *string {
	if p == nil || strings.TrimSpace(*p) == "" {
		return nil
	}
	s := strings.TrimSpace(*p)
	return &s
}

func (h *Handlers) CreateTask(w http.ResponseWriter, r *http.Request) {
	var q taskInput
	if !decode(w, r, &q) {
		return
	}
	if q.Title == nil {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": "title is required"})
		return
	}
	n, e := service.Title(*q.Title, 280)
	if e != nil {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": e.Error()})
		return
	}
	prio := int16(0)
	if q.Priority != nil {
		prio = *q.Priority
	}
	if prio < 0 || prio > 3 {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"priority": "must be between 0 and 3"})
		return
	}
	tags := []string{}
	if q.TagIDs != nil {
		tags = *q.TagIDs
	}
	var subtasks []postgres.SubtaskInput
	if q.Subtasks != nil {
		subtasks = make([]postgres.SubtaskInput, len(*q.Subtasks))
		for i, st := range *q.Subtasks {
			subtasks[i] = postgres.SubtaskInput{
				ID:        cleanStrPtr(st.ID),
				Title:     st.Title,
				Completed: st.Completed,
				Position:  st.Position,
			}
		}
	}
	p := principal(r)
	desc := cleanStrPtr(q.Description)
	due := cleanStrPtr(q.DueDate)
	projectID := cleanStrPtr(q.ProjectID)
	var x domain.Task
	e = h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.CreateTask(r.Context(), tx, p.UserID, n, desc, due, projectID, prio, tags, subtasks)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 201, x)
}
func (h *Handlers) Task(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x domain.Task
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.Task(r.Context(), tx, p.UserID, id(r, "taskId"))
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) PatchTask(w http.ResponseWriter, r *http.Request) {
	var q taskInput
	if !decode(w, r, &q) {
		return
	}
	if q.Title != nil {
		n, e := service.Title(*q.Title, 280)
		if e != nil {
			response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": e.Error()})
			return
		}
		q.Title = &n
	}
	if q.Priority != nil && (*q.Priority < 0 || *q.Priority > 3) {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"priority": "must be between 0 and 3"})
		return
	}
	var subtasks *[]postgres.SubtaskInput
	if q.Subtasks != nil {
		converted := make([]postgres.SubtaskInput, len(*q.Subtasks))
		for i, st := range *q.Subtasks {
			converted[i] = postgres.SubtaskInput{
				ID:        cleanStrPtr(st.ID),
				Title:     st.Title,
				Completed: st.Completed,
				Position:  st.Position,
			}
		}
		subtasks = &converted
	}
	var desc, due, projectID *string
	if q.Description != nil {
		desc = cleanStrPtr(q.Description)
	}
	if q.DueDate != nil {
		due = cleanStrPtr(q.DueDate)
	}
	if q.ProjectID != nil {
		projectID = cleanStrPtr(q.ProjectID)
	}
	p := principal(r)
	var x domain.Task
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.UpdateTask(r.Context(), tx, p.UserID, id(r, "taskId"), q.Title, desc, due, projectID, q.Priority, q.TagIDs, subtasks)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) Completion(w http.ResponseWriter, r *http.Request) {
	var q struct {
		Completed bool `json:"completed"`
	}
	if !decode(w, r, &q) {
		return
	}
	p := principal(r)
	var x domain.Task
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.Completion(r.Context(), tx, p.UserID, id(r, "taskId"), q.Completed)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) DeleteTask(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { return h.repo.DeleteTask(r.Context(), tx, p.UserID, id(r, "taskId")) })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) CreateSubtask(w http.ResponseWriter, r *http.Request) {
	var q struct {
		ID        *string    `json:"id"`
		TaskID    *string    `json:"task_id"`
		Title     string     `json:"title"`
		Completed bool       `json:"completed"`
		Position  int        `json:"position"`
		CreatedAt *time.Time `json:"created_at"`
		UpdatedAt *time.Time `json:"updated_at"`
	}
	if !decode(w, r, &q) {
		return
	}
	n, e := service.Title(q.Title, 280)
	if e != nil {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": e.Error()})
		return
	}
	p := principal(r)
	var x domain.Subtask
	e = h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.CreateSubtask(r.Context(), tx, p.UserID, id(r, "taskId"), n, q.Completed, q.Position)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 201, x)
}
func (h *Handlers) PatchSubtask(w http.ResponseWriter, r *http.Request) {
	var q struct {
		ID        *string    `json:"id"`
		TaskID    *string    `json:"task_id"`
		Title     *string    `json:"title"`
		Completed *bool      `json:"completed"`
		Position  *int       `json:"position"`
		CreatedAt *time.Time `json:"created_at"`
		UpdatedAt *time.Time `json:"updated_at"`
	}
	if !decode(w, r, &q) {
		return
	}
	if q.Title != nil {
		n, e := service.Title(*q.Title, 280)
		if e != nil {
			response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": e.Error()})
			return
		}
		q.Title = &n
	}
	p := principal(r)
	var x domain.Subtask
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.UpdateSubtask(r.Context(), tx, p.UserID, id(r, "subtaskId"), q.Title, q.Completed, q.Position)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) DeleteSubtask(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		return h.repo.DeleteSubtask(r.Context(), tx, p.UserID, id(r, "subtaskId"))
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) Notes(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x []domain.Note
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.Notes(r.Context(), tx, p.UserID, r.URL.Query().Get("q"), limit(r))
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, map[string]any{"items": x})
}

type noteInput struct {
	Title           *string    `json:"title"`
	ContentMD       *string    `json:"content_md"`
	ReminderAt      *time.Time `json:"reminder_at"`
	ReminderOffsets *[]int     `json:"reminder_offsets"`
}

func (h *Handlers) CreateNote(w http.ResponseWriter, r *http.Request) {
	var q noteInput
	if !decode(w, r, &q) || q.Title == nil {
		return
	}
	n, e := service.Title(*q.Title, 280)
	if e != nil {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": e.Error()})
		return
	}
	content := ""
	if q.ContentMD != nil {
		content = *q.ContentMD
	}
	var offsets []int
	if q.ReminderOffsets != nil {
		offsets = *q.ReminderOffsets
	}
	p := principal(r)
	var x domain.Note
	e = h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.CreateNote(r.Context(), tx, p.UserID, n, content, q.ReminderAt, offsets)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 201, x)
}
func (h *Handlers) Note(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x domain.Note
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.Note(r.Context(), tx, p.UserID, id(r, "noteId"))
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) PatchNote(w http.ResponseWriter, r *http.Request) {
	bodyBytes, err := io.ReadAll(r.Body)
	if err != nil {
		response.ProblemJSON(w, r, 400, "invalid_json", "Invalid JSON body", nil)
		return
	}
	var q noteInput
	if err := json.Unmarshal(bodyBytes, &q); err != nil {
		response.ProblemJSON(w, r, 400, "invalid_json", "Invalid JSON body", nil)
		return
	}
	var raw map[string]any
	_ = json.Unmarshal(bodyBytes, &raw)
	_, reminderAtSet := raw["reminder_at"]

	if q.Title != nil {
		n, e := service.Title(*q.Title, 280)
		if e != nil {
			response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"title": e.Error()})
			return
		}
		q.Title = &n
	}
	p := principal(r)
	var x domain.Note
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.UpdateNote(r.Context(), tx, p.UserID, id(r, "noteId"), q.Title, q.ContentMD, q.ReminderAt, reminderAtSet, q.ReminderOffsets)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) DeleteNote(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { return h.repo.DeleteNote(r.Context(), tx, p.UserID, id(r, "noteId")) })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) Link(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error { return h.repo.Link(r.Context(), tx, p.UserID, id(r, "noteId"), id(r, "taskId")) })
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) Unlink(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		return h.repo.Unlink(r.Context(), tx, p.UserID, id(r, "noteId"), id(r, "taskId"))
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) UploadImage(w http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(w, r.Body, 11*1024*1024)
	if e := r.ParseMultipartForm(11 * 1024 * 1024); e != nil {
		response.ProblemJSON(w, r, 413, "request_too_large", "Upload exceeds request limit", nil)
		return
	}
	f, head, e := r.FormFile("file")
	if e != nil {
		response.ProblemJSON(w, r, 400, "missing_file", "Image file is required", nil)
		return
	}
	defer f.Close()
	b, e := io.ReadAll(io.LimitReader(f, 10*1024*1024+1))
	if e != nil || len(b) > 10*1024*1024 {
		response.ProblemJSON(w, r, 413, "file_too_large", "Image exceeds 10 MiB", nil)
		return
	}
	typ := http.DetectContentType(b)
	if typ == "image/jpg" {
		typ = "image/jpeg"
	}
	if typ != "image/jpeg" && typ != "image/png" && typ != "image/webp" && typ != "image/gif" {
		response.ProblemJSON(w, r, 415, "unsupported_media", "Supported image types are JPEG, PNG, WebP, and GIF", nil)
		return
	}
	p := principal(r)
	x, e := h.service.UploadImage(r.Context(), p, id(r, "noteId"), head.Filename, typ, b)
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 201, map[string]any{"image": x, "token": "note-image:" + x.ID})
}
func (h *Handlers) PatchImage(w http.ResponseWriter, r *http.Request) {
	var q struct {
		AltText *string `json:"alt_text"`
	}
	if !decode(w, r, &q) {
		return
	}
	if q.AltText != nil && len(*q.AltText) > 280 {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"alt_text": "must be at most 280 characters"})
		return
	}
	p := principal(r)
	var x domain.Image
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.UpdateImageAlt(r.Context(), tx, p.UserID, id(r, "imageId"), q.AltText)
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, x)
}
func (h *Handlers) DeleteImage(w http.ResponseWriter, r *http.Request) {
	e := h.service.DeleteImage(r.Context(), principal(r), id(r, "imageId"))
	if e != nil {
		writeErr(w, r, e)
		return
	}
	w.WriteHeader(204)
}
func (h *Handlers) ImageAccess(w http.ResponseWriter, r *http.Request) {
	p := principal(r)
	var x domain.Image
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		var e error
		x, e = h.repo.Image(r.Context(), tx, p.UserID, id(r, "imageId"))
		return e
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	url, e := h.service.StorageSignedURL(r.Context(), x.ObjectPath)
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, map[string]any{"url": url, "expires_in": 3600})
}
func (h *Handlers) Search(w http.ResponseWriter, r *http.Request) {
	q := strings.TrimSpace(r.URL.Query().Get("q"))
	if q == "" {
		response.ProblemJSON(w, r, 422, "validation_failed", "Validation failed", map[string]string{"q": "is required"})
		return
	}
	scope := r.URL.Query().Get("scope")
	p := principal(r)
	out := map[string]any{}
	e := h.service.Do(r.Context(), p, func(tx pgx.Tx) error {
		if scope == "" || scope == "all" || scope == "tasks" {
			x, e := h.repo.Tasks(r.Context(), tx, p.UserID, "", "", q, limit(r))
			if e != nil {
				return e
			}
			out["tasks"] = x
		}
		if scope == "" || scope == "all" || scope == "notes" {
			x, e := h.repo.Notes(r.Context(), tx, p.UserID, q, limit(r))
			if e != nil {
				return e
			}
			out["notes"] = x
		}
		return nil
	})
	if e != nil {
		writeErr(w, r, e)
		return
	}
	response.JSON(w, 200, out)
}
func (h *Handlers) Login(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Username   string `json:"username"`
		Password   string `json:"password"`
		RememberMe bool   `json:"remember_me"`
		Remember   bool   `json:"rememberMe"`
	}
	if !decode(w, r, &req) {
		return
	}
	if req.Username == "" || req.Password == "" {
		response.ProblemJSON(w, r, 400, "invalid_input", "Username and password are required", nil)
		return
	}
	admin, err := h.service.Login(r.Context(), req.Username, req.Password)
	if err != nil {
		response.ProblemJSON(w, r, 401, "invalid_credentials", "Invalid username or password", nil)
		return
	}
	ttl := 24 * time.Hour
	if req.RememberMe || req.Remember {
		ttl = 7 * 24 * time.Hour
	}
	token, err := h.verifier.SignWithTTL(admin.ID, admin.Username, ttl)
	if err != nil {
		response.ProblemJSON(w, r, 500, "token_error", "Failed to generate token", nil)
		return
	}
	response.JSON(w, 200, map[string]any{
		"token": token,
		"user": map[string]string{
			"id":       admin.ID,
			"username": admin.Username,
		},
	})
}
func (h *Handlers) Register(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Username   string `json:"username"`
		Password   string `json:"password"`
		RememberMe bool   `json:"remember_me"`
		Remember   bool   `json:"rememberMe"`
	}
	if !decode(w, r, &req) {
		return
	}
	if req.Username == "" || req.Password == "" {
		response.ProblemJSON(w, r, 400, "invalid_input", "Username and password are required", nil)
		return
	}
	admin, err := h.service.Register(r.Context(), req.Username, req.Password)
	if err != nil {
		response.ProblemJSON(w, r, 400, "registration_failed", err.Error(), nil)
		return
	}
	ttl := 24 * time.Hour
	if req.RememberMe || req.Remember {
		ttl = 7 * 24 * time.Hour
	}
	token, err := h.verifier.SignWithTTL(admin.ID, admin.Username, ttl)
	if err != nil {
		response.ProblemJSON(w, r, 500, "token_error", "Failed to generate token", nil)
		return
	}
	response.JSON(w, 201, map[string]any{
		"token": token,
		"user": map[string]string{
			"id":       admin.ID,
			"username": admin.Username,
		},
	})
}
func (h *Handlers) ChangePassword(w http.ResponseWriter, r *http.Request) {
	var req struct {
		CurrentPassword string `json:"current_password"`
		NewPassword     string `json:"new_password"`
	}
	if !decode(w, r, &req) {
		return
	}
	p := principal(r)
	if err := h.service.ChangePassword(r.Context(), p, req.CurrentPassword, req.NewPassword); err != nil {
		response.ProblemJSON(w, r, 400, "password_change_failed", err.Error(), nil)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
