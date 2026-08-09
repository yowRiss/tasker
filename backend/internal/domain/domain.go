package domain

import "time"

type Principal struct {
	UserID   string `json:"id"`
	Username string `json:"username,omitempty"`
}
type Admin struct {
	ID           string `json:"id"`
	Username     string `json:"username"`
	PasswordHash string `json:"-"`
}
type Project struct {
	ID         string    `json:"id"`
	Name       string    `json:"name"`
	Color      *string   `json:"color"`
	IsArchived bool      `json:"is_archived"`
	CreatedAt  time.Time `json:"created_at"`
	UpdatedAt  time.Time `json:"updated_at"`
}
type Tag struct {
	ID        string    `json:"id"`
	Name      string    `json:"name"`
	Color     *string   `json:"color"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}
type Task struct {
	ID          string     `json:"id"`
	Title       string     `json:"title"`
	Description *string    `json:"description"`
	DueDate     *string    `json:"due_date"`
	ProjectID   *string    `json:"project_id"`
	Status      string     `json:"status"`
	CompletedAt *time.Time `json:"completed_at"`
	Priority    int16      `json:"priority"`
	Tags        []Tag      `json:"tags"`
	CreatedAt   time.Time  `json:"created_at"`
	UpdatedAt   time.Time  `json:"updated_at"`
}
type Note struct {
	ID        string       `json:"id"`
	Title     string       `json:"title"`
	ContentMD string       `json:"content_md"`
	Tags      []Tag        `json:"tags"`
	Tasks     []LinkedTask `json:"tasks"`
	CreatedAt time.Time    `json:"created_at"`
	UpdatedAt time.Time    `json:"updated_at"`
}
type LinkedTask struct {
	ID    string `json:"id"`
	Title string `json:"title"`
}
type Image struct {
	ID               string    `json:"id"`
	NoteID           string    `json:"note_id"`
	ObjectPath       string    `json:"object_path"`
	OriginalFilename string    `json:"original_filename"`
	MIMEType         string    `json:"mime_type"`
	ByteSize         int       `json:"byte_size"`
	AltText          *string   `json:"alt_text"`
	Width            *int      `json:"width"`
	Height           *int      `json:"height"`
	CreatedAt        time.Time `json:"created_at"`
}
type Page[T any] struct {
	Items      []T     `json:"items"`
	NextCursor *string `json:"next_cursor,omitempty"`
}
