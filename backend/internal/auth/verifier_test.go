package auth

import (
	"testing"
	"time"

	"github.com/google/uuid"
)

func TestVerifierSignAndVerify(t *testing.T) {
	secret := "test-secret-key-1234567890123456"
	verifier := NewVerifier(secret)
	adminID := uuid.New().String()
	username := "admin"

	t.Run("default sign expires in 24 hours", func(t *testing.T) {
		tokenStr, err := verifier.Sign(adminID, username)
		if err != nil {
			t.Fatalf("unexpected sign error: %v", err)
		}
		principal, err := verifier.Verify(tokenStr)
		if err != nil {
			t.Fatalf("unexpected verify error: %v", err)
		}
		if principal.UserID != adminID {
			t.Errorf("expected UserID %s, got %s", adminID, principal.UserID)
		}
		if principal.Username != username {
			t.Errorf("expected Username %s, got %s", username, principal.Username)
		}
	})

	t.Run("sign with 7 days TTL", func(t *testing.T) {
		tokenStr, err := verifier.SignWithTTL(adminID, username, 7*24*time.Hour)
		if err != nil {
			t.Fatalf("unexpected sign error: %v", err)
		}
		principal, err := verifier.Verify(tokenStr)
		if err != nil {
			t.Fatalf("unexpected verify error: %v", err)
		}
		if principal.UserID != adminID {
			t.Errorf("expected UserID %s, got %s", adminID, principal.UserID)
		}
		if principal.Username != username {
			t.Errorf("expected Username %s, got %s", username, principal.Username)
		}
	})
}
