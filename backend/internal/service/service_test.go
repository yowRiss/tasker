package service

import (
	"strings"
	"testing"
)

func TestTitleTrimsAndValidates(t *testing.T) {
	got, err := Title("  Inbox  ", 80)
	if err != nil || got != "Inbox" {
		t.Fatalf("got %q, %v", got, err)
	}
	if _, err := Title("   ", 80); err == nil {
		t.Fatal("blank title accepted")
	}
}

func TestImagePathIsCanonicalAndSafe(t *testing.T) {
	got, err := ImagePath("user", "note", "A strange photo.JPG")
	if err != nil {
		t.Fatal(err)
	}
	if !strings.HasPrefix(got, "notes/user/note/") || !strings.HasSuffix(got, "-a-strange-photo.jpg") {
		t.Fatalf("unexpected path %q", got)
	}
	if _, err := ImagePath("user", "note", "active.svg"); err == nil {
		t.Fatal("SVG accepted")
	}
}
