package middleware

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"log/slog"
	"net/http"
	"strings"

	"tasker/backend/internal/auth"
	"tasker/backend/internal/domain"
	"tasker/backend/internal/httpapi/response"
)

type principalKey struct{}

func Principal(r *http.Request) (domain.Principal, bool) {
	p, ok := r.Context().Value(principalKey{}).(domain.Principal)
	return p, ok
}
func RequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b := make([]byte, 12)
		_, _ = rand.Read(b)
		id := hex.EncodeToString(b)
		w.Header().Set("X-Request-ID", id)
		next.ServeHTTP(w, response.WithRequestID(r, id))
	})
}
func Authenticate(verifier *auth.Verifier) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			h := r.Header.Get("Authorization")
			if !strings.HasPrefix(h, "Bearer ") || strings.TrimSpace(strings.TrimPrefix(h, "Bearer ")) == "" {
				response.ProblemJSON(w, r, 401, "unauthorized", "Authentication required", nil)
				return
			}
			p, err := verifier.Verify(strings.TrimSpace(strings.TrimPrefix(h, "Bearer ")))
			if err != nil {
				response.ProblemJSON(w, r, 401, "invalid_token", "Invalid access token", nil)
				return
			}
			next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), principalKey{}, p)))
		})
	}
}
func Recovery(logger *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			defer func() {
				if x := recover(); x != nil {
					logger.Error("panic", "request_id", response.RequestID(r), "panic", x)
					response.ProblemJSON(w, r, 500, "internal_error", "Internal server error", nil)
				}
			}()
			next.ServeHTTP(w, r)
		})
	}
}
func CORS(origin string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.Header.Get("Origin") == origin {
				w.Header().Set("Access-Control-Allow-Origin", origin)
				w.Header().Set("Vary", "Origin")
				w.Header().Set("Access-Control-Allow-Headers", "Authorization, Content-Type")
				w.Header().Set("Access-Control-Allow-Methods", "GET,POST,PATCH,PUT,DELETE,OPTIONS")
			}
			if r.Method == http.MethodOptions {
				w.WriteHeader(http.StatusNoContent)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
