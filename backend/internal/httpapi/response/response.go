package response

import (
	"context"
	"encoding/json"
	"net/http"
)

type Problem struct {
	Type      string            `json:"type"`
	Title     string            `json:"title"`
	Status    int               `json:"status"`
	Code      string            `json:"code"`
	RequestID string            `json:"request_id"`
	Errors    map[string]string `json:"errors,omitempty"`
}

func JSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
func ProblemJSON(w http.ResponseWriter, r *http.Request, status int, code, title string, fields map[string]string) {
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(Problem{Type: "https://tasker.dev/problems/" + code, Title: title, Status: status, Code: code, RequestID: RequestID(r), Errors: fields})
}
func RequestID(r *http.Request) string {
	if v, ok := r.Context().Value(requestIDKey{}).(string); ok {
		return v
	}
	return ""
}

type requestIDKey struct{}

func WithRequestID(r *http.Request, id string) *http.Request {
	return r.WithContext(context.WithValue(r.Context(), requestIDKey{}, id))
}
