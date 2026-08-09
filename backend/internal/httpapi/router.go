package httpapi

import (
	"encoding/json"
	"github.com/go-chi/chi/v5"
	"log/slog"
	"net/http"
	"tasker/backend/internal/auth"
	"tasker/backend/internal/httpapi/handlers"
	"tasker/backend/internal/httpapi/middleware"
)

func New(h *handlers.Handlers, v *auth.Verifier, origin string, logger *slog.Logger) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware.RequestID, middleware.Recovery(logger))
	if origin != "" {
		r.Use(middleware.CORS(origin))
	}
	r.Get("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})
	r.Post("/v1/auth/login", h.Login)
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
	})
	r.NotFound(newSPAHandler().ServeHTTP)
	return r
}
