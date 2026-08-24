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

func TestAmountAndNonNegativeAmount(t *testing.T) {
	if _, err := Amount("150000.50"); err != nil {
		t.Fatalf("valid amount rejected: %v", err)
	}
	if _, err := Amount("0"); err == nil {
		t.Fatal("zero amount should be rejected by Amount()")
	}
	if _, err := Amount("-5000"); err == nil {
		t.Fatal("negative amount should be rejected")
	}
	if _, err := Amount("10.999"); err == nil {
		t.Fatal("three decimals should be rejected")
	}

	val, err := NonNegativeAmount("0")
	if err != nil || val != "0" {
		t.Fatalf("expected 0 for NonNegativeAmount(0), got %v, err=%v", val, err)
	}
	val, err = NonNegativeAmount("50000.00")
	if err != nil || val != "50000.00" {
		t.Fatalf("expected 50000.00 for NonNegativeAmount(50000.00), got %v, err=%v", val, err)
	}
	if _, err := NonNegativeAmount("-10"); err == nil {
		t.Fatal("negative amount should be rejected by NonNegativeAmount")
	}
}
