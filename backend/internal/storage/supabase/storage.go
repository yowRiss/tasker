package supabase

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type Storage struct {
	base, key string
	client    *http.Client
}

func New(base, key string) *Storage {
	return &Storage{base: strings.TrimRight(base, "/"), key: key, client: &http.Client{Timeout: 30 * time.Second}}
}
func (s *Storage) request(ctx context.Context, method, target string, body io.Reader, mime string) (*http.Response, error) {
	r, e := http.NewRequestWithContext(ctx, method, target, body)
	if e != nil {
		return nil, e
	}
	r.Header.Set("Authorization", "Bearer "+s.key)
	r.Header.Set("apikey", s.key)
	if mime != "" {
		r.Header.Set("Content-Type", mime)
	}
	return s.client.Do(r)
}
func escapeObjectPath(object string) string {
	parts := strings.Split(object, "/")
	for i, p := range parts {
		parts[i] = url.PathEscape(p)
	}
	return strings.Join(parts, "/")
}

func (s *Storage) Upload(ctx context.Context, object, mime string, body []byte) error {
	r, e := s.request(ctx, http.MethodPost, s.base+"/storage/v1/object/note-images/"+escapeObjectPath(object), bytes.NewReader(body), mime)
	if e != nil {
		return e
	}
	defer r.Body.Close()
	if r.StatusCode < 200 || r.StatusCode > 299 {
		return fmt.Errorf("storage upload status %d", r.StatusCode)
	}
	return nil
}
func (s *Storage) Delete(ctx context.Context, object string) error {
	b, _ := json.Marshal(map[string][]string{"prefixes": {object}})
	r, e := s.request(ctx, http.MethodDelete, s.base+"/storage/v1/object/note-images", bytes.NewReader(b), "application/json")
	if e != nil {
		return e
	}
	defer r.Body.Close()
	if r.StatusCode < 200 || r.StatusCode > 299 {
		return fmt.Errorf("storage delete status %d", r.StatusCode)
	}
	return nil
}
func (s *Storage) SignedURL(ctx context.Context, object string) (string, error) {
	b, _ := json.Marshal(map[string]int{"expiresIn": 3600})
	r, e := s.request(ctx, http.MethodPost, s.base+"/storage/v1/object/sign/note-images/"+escapeObjectPath(object), bytes.NewReader(b), "application/json")
	if e != nil {
		return "", e
	}
	defer r.Body.Close()
	if r.StatusCode < 200 || r.StatusCode > 299 {
		return "", fmt.Errorf("storage sign status %d", r.StatusCode)
	}
	var out struct {
		SignedURL string `json:"signedURL"`
	}
	if e = json.NewDecoder(r.Body).Decode(&out); e != nil {
		return "", e
	}
	if strings.HasPrefix(out.SignedURL, "http") {
		return out.SignedURL, nil
	}
	u := out.SignedURL
	if !strings.HasPrefix(u, "/storage/v1") {
		u = "/storage/v1" + u
	}
	return s.base + u, nil
}
