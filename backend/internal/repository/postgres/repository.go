package postgres

import (
	"context"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"tasker/backend/internal/domain"
)

type Repository struct{}

func New() *Repository { return &Repository{} }
func scanProject(r pgx.Row) (domain.Project, error) {
	var x domain.Project
	err := r.Scan(&x.ID, &x.Name, &x.Color, &x.IsArchived, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}
func scanTag(r pgx.Row) (domain.Tag, error) {
	var x domain.Tag
	err := r.Scan(&x.ID, &x.Name, &x.Color, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}
func (r *Repository) Projects(ctx context.Context, tx pgx.Tx, u string) ([]domain.Project, error) {
	rows, e := tx.Query(ctx, "select id,name,color,is_archived,created_at,updated_at from projects where user_id=$1 order by lower(name)", u)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []domain.Project{}
	for rows.Next() {
		x, e := scanProject(rows)
		if e != nil {
			return nil, e
		}
		out = append(out, x)
	}
	return out, rows.Err()
}
func (r *Repository) CreateProject(ctx context.Context, tx pgx.Tx, u, n string, c *string, a bool) (domain.Project, error) {
	return scanProject(tx.QueryRow(ctx, "insert into projects(user_id,name,color,is_archived) values($1,$2,$3,$4) returning id,name,color,is_archived,created_at,updated_at", u, n, c, a))
}
func (r *Repository) UpdateProject(ctx context.Context, tx pgx.Tx, u, id string, n, c *string, a *bool) (domain.Project, error) {
	return scanProject(tx.QueryRow(ctx, "update projects set name=coalesce($3,name),color=$4,is_archived=coalesce($5,is_archived) where id=$1 and user_id=$2 returning id,name,color,is_archived,created_at,updated_at", id, u, n, c, a))
}
func (r *Repository) DeleteProject(ctx context.Context, tx pgx.Tx, u, id string) error {
	tag, e := tx.Exec(ctx, "delete from projects where id=$1 and user_id=$2", id, u)
	if e != nil {
		return e
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) Tags(ctx context.Context, tx pgx.Tx, u string) ([]domain.Tag, error) {
	rows, e := tx.Query(ctx, "select id,name,color,created_at,updated_at from tags where user_id=$1 order by lower(name)", u)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []domain.Tag{}
	for rows.Next() {
		x, e := scanTag(rows)
		if e != nil {
			return nil, e
		}
		out = append(out, x)
	}
	return out, rows.Err()
}
func (r *Repository) CreateTag(ctx context.Context, tx pgx.Tx, u, n string, c *string) (domain.Tag, error) {
	return scanTag(tx.QueryRow(ctx, "insert into tags(user_id,name,color)values($1,$2,$3)returning id,name,color,created_at,updated_at", u, n, c))
}
func (r *Repository) UpdateTag(ctx context.Context, tx pgx.Tx, u, id string, n, c *string) (domain.Tag, error) {
	return scanTag(tx.QueryRow(ctx, "update tags set name=coalesce($3,name),color=$4 where id=$1 and user_id=$2 returning id,name,color,created_at,updated_at", id, u, n, c))
}
func (r *Repository) DeleteTag(ctx context.Context, tx pgx.Tx, u, id string) error {
	z, e := tx.Exec(ctx, "delete from tags where id=$1 and user_id=$2", id, u)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func scanTask(row pgx.Row) (domain.Task, error) {
	var x domain.Task
	err := row.Scan(&x.ID, &x.Title, &x.Description, &x.Status, &x.CompletedAt, &x.DueDate, &x.Priority, &x.ProjectID, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}

const taskFields = "id,title,description,status,completed_at,due_date::text,priority,project_id,created_at,updated_at"

func (r *Repository) Task(ctx context.Context, tx pgx.Tx, u, id string) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "select "+taskFields+" from tasks where id=$1 and user_id=$2", id, u))
	if e == nil {
		x.Tags, _ = r.taskTags(ctx, tx, u, id)
	}
	return x, e
}
func (r *Repository) Tasks(ctx context.Context, tx pgx.Tx, u, status, project, query string, limit int) ([]domain.Task, error) {
	sql := "select " + taskFields + " from tasks where user_id=$1"
	args := []any{u}
	if status != "" {
		args = append(args, status)
		sql += fmt.Sprintf(" and status=$%d", len(args))
	}
	if project != "" {
		args = append(args, project)
		sql += fmt.Sprintf(" and project_id=$%d", len(args))
	}
	if query != "" {
		args = append(args, query)
		sql += fmt.Sprintf(" and search_vector @@ websearch_to_tsquery('simple',$%d)", len(args))
	}
	args = append(args, limit)
	sql += fmt.Sprintf(" order by updated_at desc limit $%d", len(args))
	rows, e := tx.Query(ctx, sql, args...)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []domain.Task{}
	for rows.Next() {
		x, e := scanTask(rows)
		if e != nil {
			return nil, e
		}
		x.Tags, _ = r.taskTags(ctx, tx, u, x.ID)
		out = append(out, x)
	}
	return out, rows.Err()
}
func (r *Repository) CreateTask(ctx context.Context, tx pgx.Tx, u, title string, desc, due, project *string, priority int16, tags []string) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "insert into tasks(user_id,title,description,due_date,project_id,priority)values($1,$2,$3,$4::date,$5,$6)returning "+taskFields, u, title, desc, due, project, priority))
	if e == nil {
		e = r.setTaskTags(ctx, tx, u, x.ID, tags)
	}
	return x, e
}
func (r *Repository) UpdateTask(ctx context.Context, tx pgx.Tx, u, id string, title, desc, due, project *string, priority *int16, tags *[]string) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "update tasks set title=coalesce($3,title),description=$4,due_date=$5::date,project_id=$6,priority=coalesce($7,priority) where id=$1 and user_id=$2 returning "+taskFields, id, u, title, desc, due, project, priority))
	if e == nil && tags != nil {
		e = r.setTaskTags(ctx, tx, u, id, *tags)
	}
	return x, e
}
func (r *Repository) Completion(ctx context.Context, tx pgx.Tx, u, id string, completed bool) (domain.Task, error) {
	return scanTask(tx.QueryRow(ctx, "update tasks set status=case when $3 then 'completed' else 'open' end,completed_at=case when $3 then now() else null end where id=$1 and user_id=$2 returning "+taskFields, id, u, completed))
}
func (r *Repository) DeleteTask(ctx context.Context, tx pgx.Tx, u, id string) error {
	z, e := tx.Exec(ctx, "delete from tasks where id=$1 and user_id=$2", id, u)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) taskTags(ctx context.Context, tx pgx.Tx, u, id string) ([]domain.Tag, error) {
	rows, e := tx.Query(ctx, "select t.id,t.name,t.color,t.created_at,t.updated_at from tags t join task_tags j on j.tag_id=t.id where j.task_id=$1 and j.user_id=$2", id, u)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []domain.Tag{}
	for rows.Next() {
		x, e := scanTag(rows)
		if e != nil {
			return nil, e
		}
		out = append(out, x)
	}
	return out, rows.Err()
}
func (r *Repository) setTaskTags(ctx context.Context, tx pgx.Tx, u, id string, tags []string) error {
	if _, e := tx.Exec(ctx, "delete from task_tags where task_id=$1 and user_id=$2", id, u); e != nil {
		return e
	}
	for _, tag := range tags {
		z, e := tx.Exec(ctx, "insert into task_tags(user_id,task_id,tag_id)select $1,$2,id from tags where id=$3 and user_id=$1", u, id, tag)
		if e != nil {
			return e
		}
		if z.RowsAffected() != 1 {
			return fmt.Errorf("tag unavailable")
		}
	}
	return nil
}
func scanNote(row pgx.Row) (domain.Note, error) {
	var x domain.Note
	err := row.Scan(&x.ID, &x.Title, &x.ContentMD, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}
func (r *Repository) Note(ctx context.Context, tx pgx.Tx, u, id string) (domain.Note, error) {
	x, e := scanNote(tx.QueryRow(ctx, "select id,title,content_md,created_at,updated_at from notes where id=$1 and user_id=$2", id, u))
	return x, e
}
func (r *Repository) Notes(ctx context.Context, tx pgx.Tx, u, q string, limit int) ([]domain.Note, error) {
	sql := "select id,title,''::text,created_at,updated_at from notes where user_id=$1"
	args := []any{u}
	if q != "" {
		args = append(args, q)
		sql += fmt.Sprintf(" and search_vector @@ websearch_to_tsquery('simple',$%d)", len(args))
	}
	args = append(args, limit)
	rows, e := tx.Query(ctx, sql+fmt.Sprintf(" order by updated_at desc limit $%d", len(args)), args...)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []domain.Note{}
	for rows.Next() {
		x, e := scanNote(rows)
		if e != nil {
			return nil, e
		}
		out = append(out, x)
	}
	return out, rows.Err()
}
func (r *Repository) CreateNote(ctx context.Context, tx pgx.Tx, u, title, content string) (domain.Note, error) {
	return scanNote(tx.QueryRow(ctx, "insert into notes(user_id,title,content_md)values($1,$2,$3)returning id,title,content_md,created_at,updated_at", u, title, content))
}
func (r *Repository) UpdateNote(ctx context.Context, tx pgx.Tx, u, id string, title, content *string) (domain.Note, error) {
	return scanNote(tx.QueryRow(ctx, "update notes set title=coalesce($3,title),content_md=coalesce($4,content_md)where id=$1 and user_id=$2 returning id,title,content_md,created_at,updated_at", id, u, title, content))
}
func (r *Repository) DeleteNote(ctx context.Context, tx pgx.Tx, u, id string) error {
	z, e := tx.Exec(ctx, "delete from notes where id=$1 and user_id=$2", id, u)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) Link(ctx context.Context, tx pgx.Tx, u, n, t string) error {
	z, e := tx.Exec(ctx, "insert into note_task_links(user_id,note_id,task_id) select $1,$2,$3 where exists(select 1 from notes where id=$2 and user_id=$1) and exists(select 1 from tasks where id=$3 and user_id=$1) on conflict do nothing", u, n, t)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) Unlink(ctx context.Context, tx pgx.Tx, u, n, t string) error {
	z, e := tx.Exec(ctx, "delete from note_task_links where user_id=$1 and note_id=$2 and task_id=$3", u, n, t)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) Image(ctx context.Context, tx pgx.Tx, u, id string) (domain.Image, error) {
	var x domain.Image
	e := tx.QueryRow(ctx, "select id,note_id,object_path,original_filename,mime_type,byte_size,alt_text,width,height,created_at from note_images where id=$1 and user_id=$2", id, u).Scan(&x.ID, &x.NoteID, &x.ObjectPath, &x.OriginalFilename, &x.MIMEType, &x.ByteSize, &x.AltText, &x.Width, &x.Height, &x.CreatedAt)
	return x, e
}
func (r *Repository) NoteExists(ctx context.Context, tx pgx.Tx, u, id string) error {
	var ok bool
	e := tx.QueryRow(ctx, "select exists(select 1 from notes where id=$1 and user_id=$2)", id, u).Scan(&ok)
	if e != nil || !ok {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) AddImage(ctx context.Context, tx pgx.Tx, u, n, id, path, name, mime string, size int) (domain.Image, error) {
	var x domain.Image
	e := tx.QueryRow(ctx, "insert into note_images(id,user_id,note_id,object_path,original_filename,mime_type,byte_size)values($1,$2,$3,$4,$5,$6,$7)returning id,note_id,object_path,original_filename,mime_type,byte_size,alt_text,width,height,created_at", id, u, n, path, name, mime, size).Scan(&x.ID, &x.NoteID, &x.ObjectPath, &x.OriginalFilename, &x.MIMEType, &x.ByteSize, &x.AltText, &x.Width, &x.Height, &x.CreatedAt)
	return x, e
}
func (r *Repository) UpdateImageAlt(ctx context.Context, tx pgx.Tx, u, id string, alt *string) (domain.Image, error) {
	var x domain.Image
	e := tx.QueryRow(ctx, "update note_images set alt_text=$3 where id=$1 and user_id=$2 returning id,note_id,object_path,original_filename,mime_type,byte_size,alt_text,width,height,created_at", id, u, alt).Scan(&x.ID, &x.NoteID, &x.ObjectPath, &x.OriginalFilename, &x.MIMEType, &x.ByteSize, &x.AltText, &x.Width, &x.Height, &x.CreatedAt)
	return x, e
}
func (r *Repository) DeleteImage(ctx context.Context, tx pgx.Tx, u, id string) error {
	z, e := tx.Exec(ctx, "delete from note_images where id=$1 and user_id=$2", id, u)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) AdminByUsername(ctx context.Context, pool *pgxpool.Pool, username string) (domain.Admin, error) {
	var a domain.Admin
	e := pool.QueryRow(ctx, "select id,username,password_hash from admins where username=$1", username).Scan(&a.ID, &a.Username, &a.PasswordHash)
	return a, e
}
func (r *Repository) AdminCount(ctx context.Context, pool *pgxpool.Pool) (int, error) {
	var n int
	e := pool.QueryRow(ctx, "select count(*) from admins").Scan(&n)
	return n, e
}
func (r *Repository) CreateAdmin(ctx context.Context, pool *pgxpool.Pool, username, hash string) error {
	_, e := pool.Exec(ctx, "insert into admins(username,password_hash)values($1,$2)", username, hash)
	return e
}
func (r *Repository) UpdateAdminPassword(ctx context.Context, pool *pgxpool.Pool, id, hash string) error {
	z, e := pool.Exec(ctx, "update admins set password_hash=$2 where id=$1", id, hash)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}


