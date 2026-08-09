package service

import (
	"context"
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

