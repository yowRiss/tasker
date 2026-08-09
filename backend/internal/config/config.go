package config

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"net/url"
	"os"
	"strings"
)

// Config holds server-only configuration. It is intentionally validated at startup.
type Config struct {
	DatabaseURL, SupabaseURL, ServiceRoleKey, JWTSecret, CORSAllowedOrigin, APIAddr string
}

func Load() (Config, error) {
	serverKey := os.Getenv("SUPABASE_SECRET_KEY")
	if serverKey == "" {
		serverKey = os.Getenv("SUPABASE_SERVICE_ROLE_KEY") // legacy key compatibility
	}
	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		b := make([]byte, 32)
		if _, err := rand.Read(b); err != nil {
			return Config{}, fmt.Errorf("generate JWT secret: %w", err)
		}
		jwtSecret = hex.EncodeToString(b)
		log.Printf("WARNING: JWT_SECRET not set, generated random secret (tokens will not survive restarts)")
	}
	c := Config{DatabaseURL: os.Getenv("DATABASE_URL"), SupabaseURL: strings.TrimRight(os.Getenv("SUPABASE_URL"), "/"), ServiceRoleKey: serverKey, JWTSecret: jwtSecret, CORSAllowedOrigin: os.Getenv("CORS_ALLOWED_ORIGIN"), APIAddr: os.Getenv("API_ADDR")}
	if c.APIAddr == "" {
		c.APIAddr = ":8080"
	}
	for _, item := range []struct{ name, value string }{{"DATABASE_URL", c.DatabaseURL}, {"SUPABASE_URL", c.SupabaseURL}, {"SUPABASE_SECRET_KEY", c.ServiceRoleKey}} {
		if strings.TrimSpace(item.value) == "" {
			return Config{}, fmt.Errorf("%s is required", item.name)
		}
	}
	if _, err := url.ParseRequestURI(c.SupabaseURL); err != nil {
		return Config{}, fmt.Errorf("invalid SUPABASE_URL: %w", err)
	}
	return c, nil
}
