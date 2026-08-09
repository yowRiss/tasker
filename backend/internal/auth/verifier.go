package auth

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"tasker/backend/internal/domain"
)

// Verifier signs and verifies HMAC-SHA256 JWTs using a local secret.
type Verifier struct {
	secret []byte
}

// NewVerifier creates a verifier with the given HMAC secret.
func NewVerifier(secret string) *Verifier {
	return &Verifier{secret: []byte(secret)}
}

// Sign creates a signed JWT for the given admin.
func (v *Verifier) Sign(adminID, username string) (string, error) {
	now := time.Now()
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"sub":      adminID,
		"username": username,
		"iat":      now.Unix(),
		"exp":      now.Add(24 * time.Hour).Unix(),
	})
	return token.SignedString(v.secret)
}

// Verify validates the token and returns a Principal.
func (v *Verifier) Verify(raw string) (domain.Principal, error) {
	claims := jwt.MapClaims{}
	token, err := jwt.ParseWithClaims(raw, claims, func(t *jwt.Token) (any, error) {
		if t.Method.Alg() != jwt.SigningMethodHS256.Alg() {
			return nil, fmt.Errorf("unexpected signing method")
		}
		return v.secret, nil
	}, jwt.WithExpirationRequired(), jwt.WithLeeway(30*time.Second))
	if err != nil || !token.Valid {
		return domain.Principal{}, fmt.Errorf("verify JWT: %w", err)
	}
	sub, _ := claims["sub"].(string)
	if _, err := uuid.Parse(sub); err != nil {
		return domain.Principal{}, fmt.Errorf("invalid sub")
	}
	username, _ := claims["username"].(string)
	return domain.Principal{UserID: sub, Username: username}, nil
}
