package postgres

import (
	"context"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"tasker/backend/internal/domain"
	"time"
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
const subtaskFields = "id,task_id,title,completed,position,created_at,updated_at"

type SubtaskInput struct {
	ID        *string `json:"id"`
	Title     string  `json:"title"`
	Completed bool    `json:"completed"`
	Position  int     `json:"position"`
}

func scanSubtask(row pgx.Row) (domain.Subtask, error) {
	var x domain.Subtask
	err := row.Scan(&x.ID, &x.TaskID, &x.Title, &x.Completed, &x.Position, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}

func (r *Repository) Subtasks(ctx context.Context, tx pgx.Tx, u, taskID string) ([]domain.Subtask, error) {
	rows, e := tx.Query(ctx, "select "+subtaskFields+" from subtasks where task_id=$1 and user_id=$2 order by position asc, created_at asc", taskID, u)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []domain.Subtask{}
	for rows.Next() {
		x, e := scanSubtask(rows)
		if e != nil {
			return nil, e
		}
		out = append(out, x)
	}
	return out, rows.Err()
}

func (r *Repository) Task(ctx context.Context, tx pgx.Tx, u, id string) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "select "+taskFields+" from tasks where id=$1 and user_id=$2", id, u))
	if e == nil {
		x.Tags, _ = r.taskTags(ctx, tx, u, id)
		x.Subtasks, _ = r.Subtasks(ctx, tx, u, id)
		if x.Tags == nil {
			x.Tags = []domain.Tag{}
		}
		if x.Subtasks == nil {
			x.Subtasks = []domain.Subtask{}
		}
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
	out := []domain.Task{}
	for rows.Next() {
		x, e := scanTask(rows)
		if e != nil {
			rows.Close()
			return nil, e
		}
		out = append(out, x)
	}
	rows.Close()
	if e := rows.Err(); e != nil {
		return nil, e
	}

	for i := range out {
		out[i].Tags, _ = r.taskTags(ctx, tx, u, out[i].ID)
		out[i].Subtasks, _ = r.Subtasks(ctx, tx, u, out[i].ID)
		if out[i].Tags == nil {
			out[i].Tags = []domain.Tag{}
		}
		if out[i].Subtasks == nil {
			out[i].Subtasks = []domain.Subtask{}
		}
	}
	return out, nil
}
func (r *Repository) CreateTask(ctx context.Context, tx pgx.Tx, u, title string, desc, due, project *string, priority int16, tags []string, subtasks []SubtaskInput) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "insert into tasks(user_id,title,description,due_date,project_id,priority)values($1,$2,$3,$4::date,$5,$6)returning "+taskFields, u, title, desc, due, project, priority))
	if e != nil {
		return x, e
	}
	if e = r.setTaskTags(ctx, tx, u, x.ID, tags); e != nil {
		return x, e
	}
	if len(subtasks) > 0 {
		var err error
		x.Subtasks, err = r.SetSubtasks(ctx, tx, u, x.ID, subtasks)
		if err != nil {
			return x, err
		}
	} else {
		x.Subtasks = []domain.Subtask{}
	}
	x.Tags, _ = r.taskTags(ctx, tx, u, x.ID)
	return x, nil
}
func (r *Repository) UpdateTask(ctx context.Context, tx pgx.Tx, u, id string, title, desc, due, project *string, priority *int16, tags *[]string, subtasks *[]SubtaskInput) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "update tasks set title=coalesce($3,title),description=$4,due_date=$5::date,project_id=$6,priority=coalesce($7,priority) where id=$1 and user_id=$2 returning "+taskFields, id, u, title, desc, due, project, priority))
	if e != nil {
		return x, e
	}
	if tags != nil {
		if err := r.setTaskTags(ctx, tx, u, id, *tags); err != nil {
			return x, err
		}
	}
	if subtasks != nil {
		var err error
		x.Subtasks, err = r.SetSubtasks(ctx, tx, u, id, *subtasks)
		if err != nil {
			return x, err
		}
	} else {
		x.Subtasks, _ = r.Subtasks(ctx, tx, u, id)
	}
	x.Tags, _ = r.taskTags(ctx, tx, u, id)
	return x, nil
}
func (r *Repository) Completion(ctx context.Context, tx pgx.Tx, u, id string, completed bool) (domain.Task, error) {
	x, e := scanTask(tx.QueryRow(ctx, "update tasks set status=case when $3 then 'completed' else 'open' end,completed_at=case when $3 then now() else null end where id=$1 and user_id=$2 returning "+taskFields, id, u, completed))
	if e == nil {
		x.Tags, _ = r.taskTags(ctx, tx, u, id)
		x.Subtasks, _ = r.Subtasks(ctx, tx, u, id)
	}
	return x, e
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
func (r *Repository) CreateSubtask(ctx context.Context, tx pgx.Tx, u, taskID, title string, completed bool, position int) (domain.Subtask, error) {
	return scanSubtask(tx.QueryRow(ctx, "insert into subtasks(user_id,task_id,title,completed,position) values($1,$2,$3,$4,$5) returning "+subtaskFields, u, taskID, title, completed, position))
}
func (r *Repository) UpdateSubtask(ctx context.Context, tx pgx.Tx, u, subtaskID string, title *string, completed *bool, position *int) (domain.Subtask, error) {
	return scanSubtask(tx.QueryRow(ctx, "update subtasks set title=coalesce($3,title),completed=coalesce($4,completed),position=coalesce($5,position) where id=$1 and user_id=$2 returning "+subtaskFields, subtaskID, u, title, completed, position))
}
func (r *Repository) DeleteSubtask(ctx context.Context, tx pgx.Tx, u, subtaskID string) error {
	z, e := tx.Exec(ctx, "delete from subtasks where id=$1 and user_id=$2", subtaskID, u)
	if e != nil {
		return e
	}
	if z.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
func (r *Repository) SetSubtasks(ctx context.Context, tx pgx.Tx, u, taskID string, inputs []SubtaskInput) ([]domain.Subtask, error) {
	rows, err := tx.Query(ctx, "select id from subtasks where task_id=$1 and user_id=$2", taskID, u)
	if err != nil {
		return nil, err
	}
	existingIDs := make(map[string]bool)
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			rows.Close()
			return nil, err
		}
		existingIDs[id] = true
	}
	rows.Close()
	if err := rows.Err(); err != nil {
		return nil, err
	}

	keepIDs := make(map[string]bool)
	for idx, in := range inputs {
		pos := in.Position
		if pos == 0 {
			pos = idx
		}
		if in.ID != nil && existingIDs[*in.ID] {
			keepIDs[*in.ID] = true
			if _, err := tx.Exec(ctx, "update subtasks set title=$3, completed=$4, position=$5 where id=$1 and user_id=$2", *in.ID, u, in.Title, in.Completed, pos); err != nil {
				return nil, err
			}
		} else {
			var newID string
			if err := tx.QueryRow(ctx, "insert into subtasks(user_id,task_id,title,completed,position) values($1,$2,$3,$4,$5) returning id", u, taskID, in.Title, in.Completed, pos).Scan(&newID); err != nil {
				return nil, err
			}
			keepIDs[newID] = true
		}
	}

	for id := range existingIDs {
		if !keepIDs[id] {
			if _, err := tx.Exec(ctx, "delete from subtasks where id=$1 and user_id=$2", id, u); err != nil {
				return nil, err
			}
		}
	}

	return r.Subtasks(ctx, tx, u, taskID)
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
	var remAt *time.Time
	var remOffsets []int
	err := row.Scan(&x.ID, &x.Title, &x.ContentMD, &remAt, &remOffsets, &x.CreatedAt, &x.UpdatedAt)
	x.ReminderAt = remAt
	if remOffsets == nil {
		x.ReminderOffsets = []int{}
	} else {
		x.ReminderOffsets = remOffsets
	}
	return x, err
}
func (r *Repository) Note(ctx context.Context, tx pgx.Tx, u, id string) (domain.Note, error) {
	x, e := scanNote(tx.QueryRow(ctx, "select id,title,content_md,reminder_at,reminder_offsets,created_at,updated_at from notes where id=$1 and user_id=$2", id, u))
	return x, e
}
func (r *Repository) Notes(ctx context.Context, tx pgx.Tx, u, q string, limit int) ([]domain.Note, error) {
	sql := "select id,title,''::text,reminder_at,reminder_offsets,created_at,updated_at from notes where user_id=$1"
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
func (r *Repository) CreateNote(ctx context.Context, tx pgx.Tx, u, title, content string, reminderAt *time.Time, reminderOffsets []int) (domain.Note, error) {
	if reminderOffsets == nil {
		reminderOffsets = []int{0}
	}
	return scanNote(tx.QueryRow(ctx, "insert into notes(user_id,title,content_md,reminder_at,reminder_offsets)values($1,$2,$3,$4,$5)returning id,title,content_md,reminder_at,reminder_offsets,created_at,updated_at", u, title, content, reminderAt, reminderOffsets))
}
func (r *Repository) UpdateNote(ctx context.Context, tx pgx.Tx, u, id string, title, content *string, reminderAt *time.Time, reminderAtSet bool, reminderOffsets *[]int) (domain.Note, error) {
	var offsetsArg []int
	if reminderOffsets != nil {
		offsetsArg = *reminderOffsets
	}
	return scanNote(tx.QueryRow(ctx, "update notes set title=coalesce($3,title),content_md=coalesce($4,content_md),reminder_at=case when $5::boolean then $6::timestamptz else reminder_at end,reminder_offsets=coalesce($7,reminder_offsets) where id=$1 and user_id=$2 returning id,title,content_md,reminder_at,reminder_offsets,created_at,updated_at", id, u, title, content, reminderAtSet, reminderAt, offsetsArg))
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

const accountFields = `a.id,a.name,a.account_type,a.currency,a.archived_at,
  coalesce(b.balance, 0)::text,a.created_at,a.updated_at`

func scanAccount(row pgx.Row) (domain.Account, error) {
	var x domain.Account
	err := row.Scan(&x.ID, &x.Name, &x.AccountType, &x.Currency, &x.ArchivedAt, &x.Balance, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}

// accountBalanceJoin represents every transaction as one or two account legs.
// This keeps stored balances immutable and makes transfers balance-neutral.
const accountBalanceJoin = `
left join (
  select account_id, sum(delta) as balance from (
    select account_id, case transaction_type when 'income' then amount else -amount end as delta
    from transactions where user_id=$1
    union all
    select transfer_account_id, amount from transactions
    where user_id=$1 and transaction_type='transfer'
  ) legs group by account_id
) b on b.account_id=a.id`

func (r *Repository) Accounts(ctx context.Context, tx pgx.Tx, userID string, includeArchived bool) ([]domain.Account, error) {
	query := "select " + accountFields + " from accounts a " + accountBalanceJoin + " where a.user_id=$1"
	if !includeArchived {
		query += " and a.archived_at is null"
	}
	query += " order by a.archived_at nulls first, lower(a.name)"
	rows, err := tx.Query(ctx, query, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []domain.Account{}
	for rows.Next() {
		x, err := scanAccount(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}

func (r *Repository) Account(ctx context.Context, tx pgx.Tx, userID, id string) (domain.Account, error) {
	return scanAccount(tx.QueryRow(ctx, "select "+accountFields+" from accounts a "+accountBalanceJoin+" where a.user_id=$1 and a.id=$2", userID, id))
}

func (r *Repository) CreateAccount(ctx context.Context, tx pgx.Tx, userID, name, kind string) (domain.Account, error) {
	return scanAccount(tx.QueryRow(ctx, "insert into accounts(user_id,name,account_type) values($1,$2,$3) returning id,name,account_type,currency,archived_at,'0'::text,created_at,updated_at", userID, name, kind))
}

func (r *Repository) UpdateAccount(ctx context.Context, tx pgx.Tx, userID, id string, name, kind *string, archived *bool) (domain.Account, error) {
	if archived != nil {
		_, err := tx.Exec(ctx, "update accounts set archived_at=case when $4 then coalesce(archived_at, now()) else null end where id=$1 and user_id=$2", id, userID, *archived)
		if err != nil {
			return domain.Account{}, err
		}
	}
	if name != nil || kind != nil {
		_, err := tx.Exec(ctx, "update accounts set name=coalesce($3,name), account_type=coalesce($4,account_type) where id=$1 and user_id=$2", id, userID, name, kind)
		if err != nil {
			return domain.Account{}, err
		}
	}
	return r.Account(ctx, tx, userID, id)
}

func (r *Repository) DeleteAccount(ctx context.Context, tx pgx.Tx, userID, id string) error {
	tag, err := tx.Exec(ctx, "delete from accounts where id=$1 and user_id=$2", id, userID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func scanCategory(row pgx.Row) (domain.Category, error) {
	var x domain.Category
	err := row.Scan(&x.ID, &x.Name, &x.CategoryType, &x.Icon, &x.Color, &x.ArchivedAt, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}

func (r *Repository) Categories(ctx context.Context, tx pgx.Tx, userID, kind string, includeArchived bool) ([]domain.Category, error) {
	args := []any{userID}
	query := "select id,name,category_type,icon,color,archived_at,created_at,updated_at from categories where user_id=$1"
	if kind != "" {
		args = append(args, kind)
		query += fmt.Sprintf(" and category_type=$%d", len(args))
	}
	if !includeArchived {
		query += " and archived_at is null"
	}
	query += " order by category_type, lower(name)"
	rows, err := tx.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []domain.Category{}
	for rows.Next() {
		x, err := scanCategory(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}

func (r *Repository) Category(ctx context.Context, tx pgx.Tx, userID, id string) (domain.Category, error) {
	return scanCategory(tx.QueryRow(ctx, "select id,name,category_type,icon,color,archived_at,created_at,updated_at from categories where user_id=$1 and id=$2", userID, id))
}

func (r *Repository) CreateCategory(ctx context.Context, tx pgx.Tx, userID, name, kind string, icon, color *string) (domain.Category, error) {
	return scanCategory(tx.QueryRow(ctx, "insert into categories(user_id,name,category_type,icon,color) values($1,$2,$3,$4,$5) returning id,name,category_type,icon,color,archived_at,created_at,updated_at", userID, name, kind, icon, color))
}

func (r *Repository) UpdateCategory(ctx context.Context, tx pgx.Tx, userID, id string, name, icon, color *string, archived *bool) (domain.Category, error) {
	if archived != nil {
		_, err := tx.Exec(ctx, "update categories set archived_at=case when $4 then coalesce(archived_at, now()) else null end where id=$1 and user_id=$2", id, userID, *archived)
		if err != nil {
			return domain.Category{}, err
		}
	}
	_, err := tx.Exec(ctx, "update categories set name=coalesce($3,name),icon=$4,color=$5 where id=$1 and user_id=$2", id, userID, name, icon, color)
	if err != nil {
		return domain.Category{}, err
	}
	return r.Category(ctx, tx, userID, id)
}

func (r *Repository) DeleteCategory(ctx context.Context, tx pgx.Tx, userID, id string) error {
	tag, err := tx.Exec(ctx, "delete from categories where id=$1 and user_id=$2", id, userID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *Repository) CreateDefaultCategories(ctx context.Context, tx pgx.Tx, userID string) error {
	_, err := tx.Exec(ctx, `insert into categories(user_id,name,category_type,icon,color) values
($1,'Food','expense','utensils','#E67E22'),($1,'Transport','expense','car','#2980B9'),
($1,'Bills','expense','receipt','#8E44AD'),($1,'Shopping','expense','bag','#16A085'),
($1,'Health','expense','heart','#C0392B'),($1,'Salary','income','wallet','#27AE60')
on conflict (user_id, category_type, lower(name)) do nothing`, userID)
	return err
}

type TransactionFilter struct {
	StartDate, EndDate, AccountID, CategoryID, TransactionType, Query, MinAmount, MaxAmount string
	Limit                                                                                   int
}

const transactionFields = `t.id,t.transaction_type,t.amount::text,t.transaction_date::text,
  t.account_id,a.name,t.transfer_account_id,ta.name,t.category_id,c.name,t.description,
  r.id,r.transaction_id,r.object_path,r.original_filename,r.mime_type,r.byte_size,r.created_at,
  t.created_at,t.updated_at`
const transactionJoins = `
 join accounts a on a.id=t.account_id and a.user_id=t.user_id
 left join accounts ta on ta.id=t.transfer_account_id and ta.user_id=t.user_id
 left join categories c on c.id=t.category_id and c.user_id=t.user_id
 left join transaction_receipts r on r.transaction_id=t.id and r.user_id=t.user_id`

func scanTransaction(row pgx.Row) (domain.Transaction, error) {
	var x domain.Transaction
	var receiptID, receiptTxn, receiptObject, receiptName, receiptMIME *string
	var receiptSize *int
	var receiptCreated *time.Time
	err := row.Scan(&x.ID, &x.TransactionType, &x.Amount, &x.TransactionDate,
		&x.AccountID, &x.AccountName, &x.TransferAccountID, &x.TransferAccountName,
		&x.CategoryID, &x.CategoryName, &x.Description,
		&receiptID, &receiptTxn, &receiptObject, &receiptName, &receiptMIME, &receiptSize, &receiptCreated,
		&x.CreatedAt, &x.UpdatedAt)
	if err != nil {
		return x, err
	}
	if receiptID != nil {
		x.Receipt = &domain.Receipt{ID: *receiptID, TransactionID: *receiptTxn, ObjectPath: *receiptObject, OriginalFilename: *receiptName, MIMEType: *receiptMIME, ByteSize: *receiptSize, CreatedAt: *receiptCreated}
	}
	return x, nil
}

func (r *Repository) Transaction(ctx context.Context, tx pgx.Tx, userID, id string) (domain.Transaction, error) {
	return scanTransaction(tx.QueryRow(ctx, "select "+transactionFields+" from transactions t "+transactionJoins+" where t.user_id=$1 and t.id=$2", userID, id))
}

func (r *Repository) Transactions(ctx context.Context, tx pgx.Tx, userID string, f TransactionFilter) ([]domain.Transaction, error) {
	args := []any{userID}
	query := "select " + transactionFields + " from transactions t " + transactionJoins + " where t.user_id=$1"
	add := func(fragment, value string) {
		if value != "" {
			args = append(args, value)
			query += fmt.Sprintf(" and "+fragment+" $%d", len(args))
		}
	}
	add("t.transaction_date >=", f.StartDate)
	add("t.transaction_date <=", f.EndDate)
	add("t.account_id =", f.AccountID)
	add("t.category_id =", f.CategoryID)
	add("t.transaction_type =", f.TransactionType)
	if f.MinAmount != "" {
		args = append(args, f.MinAmount)
		query += fmt.Sprintf(" and t.amount >= $%d::numeric", len(args))
	}
	if f.MaxAmount != "" {
		args = append(args, f.MaxAmount)
		query += fmt.Sprintf(" and t.amount <= $%d::numeric", len(args))
	}
	if f.Query != "" {
		args = append(args, "%"+f.Query+"%")
		query += fmt.Sprintf(" and coalesce(t.description,'') ilike $%d", len(args))
	}
	args = append(args, f.Limit)
	query += fmt.Sprintf(" order by t.transaction_date desc,t.id desc limit $%d", len(args))
	rows, err := tx.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []domain.Transaction{}
	for rows.Next() {
		x, err := scanTransaction(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}

func (r *Repository) CreateTransaction(ctx context.Context, tx pgx.Tx, userID, kind, amount, date, accountID string, transferAccountID, categoryID, description *string) (domain.Transaction, error) {
	var id string
	err := tx.QueryRow(ctx, `insert into transactions(user_id,transaction_type,amount,transaction_date,account_id,transfer_account_id,category_id,description)
values($1,$2,$3::numeric,$4::date,$5,$6,$7,$8) returning id`, userID, kind, amount, date, accountID, transferAccountID, categoryID, description).Scan(&id)
	if err != nil {
		return domain.Transaction{}, err
	}
	return r.Transaction(ctx, tx, userID, id)
}

func (r *Repository) UpdateTransaction(ctx context.Context, tx pgx.Tx, userID, id, kind, amount, date, accountID string, transferAccountID, categoryID, description *string) (domain.Transaction, error) {
	tag, err := tx.Exec(ctx, `update transactions set transaction_type=$3,amount=$4::numeric,transaction_date=$5::date,account_id=$6,transfer_account_id=$7,category_id=$8,description=$9
where id=$1 and user_id=$2`, id, userID, kind, amount, date, accountID, transferAccountID, categoryID, description)
	if err != nil {
		return domain.Transaction{}, err
	}
	if tag.RowsAffected() == 0 {
		return domain.Transaction{}, pgx.ErrNoRows
	}
	return r.Transaction(ctx, tx, userID, id)
}

func (r *Repository) DeleteTransaction(ctx context.Context, tx pgx.Tx, userID, id string) error {
	tag, err := tx.Exec(ctx, "delete from transactions where id=$1 and user_id=$2", id, userID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *Repository) ActiveAccount(ctx context.Context, tx pgx.Tx, userID, id string) error {
	var ok bool
	err := tx.QueryRow(ctx, "select exists(select 1 from accounts where id=$1 and user_id=$2 and archived_at is null)", id, userID).Scan(&ok)
	if err != nil || !ok {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *Repository) ActiveCategory(ctx context.Context, tx pgx.Tx, userID, id, kind string) error {
	var ok bool
	err := tx.QueryRow(ctx, "select exists(select 1 from categories where id=$1 and user_id=$2 and category_type=$3 and archived_at is null)", id, userID, kind).Scan(&ok)
	if err != nil || !ok {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *Repository) Receipt(ctx context.Context, tx pgx.Tx, userID, id string) (domain.Receipt, error) {
	var x domain.Receipt
	err := tx.QueryRow(ctx, "select id,transaction_id,object_path,original_filename,mime_type,byte_size,created_at from transaction_receipts where id=$1 and user_id=$2", id, userID).Scan(&x.ID, &x.TransactionID, &x.ObjectPath, &x.OriginalFilename, &x.MIMEType, &x.ByteSize, &x.CreatedAt)
	return x, err
}

func (r *Repository) ReceiptForTransaction(ctx context.Context, tx pgx.Tx, userID, transactionID string) (domain.Receipt, error) {
	var x domain.Receipt
	err := tx.QueryRow(ctx, "select id,transaction_id,object_path,original_filename,mime_type,byte_size,created_at from transaction_receipts where transaction_id=$1 and user_id=$2 for update", transactionID, userID).Scan(&x.ID, &x.TransactionID, &x.ObjectPath, &x.OriginalFilename, &x.MIMEType, &x.ByteSize, &x.CreatedAt)
	return x, err
}

func (r *Repository) UpsertReceipt(ctx context.Context, tx pgx.Tx, userID, transactionID, object, name, mime string, size int) (domain.Receipt, error) {
	var x domain.Receipt
	err := tx.QueryRow(ctx, `insert into transaction_receipts(user_id,transaction_id,object_path,original_filename,mime_type,byte_size)
values($1,$2,$3,$4,$5,$6) on conflict(transaction_id) do update set object_path=excluded.object_path,original_filename=excluded.original_filename,mime_type=excluded.mime_type,byte_size=excluded.byte_size
returning id,transaction_id,object_path,original_filename,mime_type,byte_size,created_at`, userID, transactionID, object, name, mime, size).Scan(&x.ID, &x.TransactionID, &x.ObjectPath, &x.OriginalFilename, &x.MIMEType, &x.ByteSize, &x.CreatedAt)
	return x, err
}

func (r *Repository) DeleteReceipt(ctx context.Context, tx pgx.Tx, userID, id string) error {
	tag, err := tx.Exec(ctx, "delete from transaction_receipts where id=$1 and user_id=$2", id, userID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

const budgetFields = `b.id,b.category_id,c.name,b.period_start::text,b.period_end::text,b.amount_limit::text,
  coalesce(s.spent,0)::text,(b.amount_limit-coalesce(s.spent,0))::text,
  round((coalesce(s.spent,0)/b.amount_limit)*100,2)::text,(coalesce(s.spent,0)>b.amount_limit),b.created_at,b.updated_at`
const budgetJoin = ` join categories c on c.id=b.category_id and c.user_id=b.user_id
 left join lateral (
   select sum(t.amount) as spent from transactions t
   where t.user_id=b.user_id and t.category_id=b.category_id and t.transaction_type='expense'
     and t.transaction_date between b.period_start and b.period_end
 ) s on true`

func scanBudget(row pgx.Row) (domain.Budget, error) {
	var x domain.Budget
	err := row.Scan(&x.ID, &x.CategoryID, &x.CategoryName, &x.PeriodStart, &x.PeriodEnd, &x.AmountLimit, &x.Spent, &x.Remaining, &x.PercentUsed, &x.IsOverBudget, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}

func (r *Repository) Budgets(ctx context.Context, tx pgx.Tx, userID string) ([]domain.Budget, error) {
	rows, err := tx.Query(ctx, "select "+budgetFields+" from budgets b "+budgetJoin+" where b.user_id=$1 order by b.period_start desc, lower(c.name)", userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []domain.Budget{}
	for rows.Next() {
		x, err := scanBudget(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}

func (r *Repository) Budget(ctx context.Context, tx pgx.Tx, userID, id string) (domain.Budget, error) {
	return scanBudget(tx.QueryRow(ctx, "select "+budgetFields+" from budgets b "+budgetJoin+" where b.user_id=$1 and b.id=$2", userID, id))
}

func (r *Repository) CreateBudget(ctx context.Context, tx pgx.Tx, userID, categoryID, start, end, amount string) (domain.Budget, error) {
	var id string
	err := tx.QueryRow(ctx, "insert into budgets(user_id,category_id,period_start,period_end,amount_limit) values($1,$2,$3::date,$4::date,$5::numeric) returning id", userID, categoryID, start, end, amount).Scan(&id)
	if err != nil {
		return domain.Budget{}, err
	}
	return r.Budget(ctx, tx, userID, id)
}

func (r *Repository) UpdateBudget(ctx context.Context, tx pgx.Tx, userID, id, categoryID, start, end, amount string) (domain.Budget, error) {
	tag, err := tx.Exec(ctx, "update budgets set category_id=$3,period_start=$4::date,period_end=$5::date,amount_limit=$6::numeric where id=$1 and user_id=$2", id, userID, categoryID, start, end, amount)
	if err != nil {
		return domain.Budget{}, err
	}
	if tag.RowsAffected() == 0 {
		return domain.Budget{}, pgx.ErrNoRows
	}
	return r.Budget(ctx, tx, userID, id)
}

func (r *Repository) DeleteBudget(ctx context.Context, tx pgx.Tx, userID, id string) error {
	tag, err := tx.Exec(ctx, "delete from budgets where id=$1 and user_id=$2", id, userID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func scanRecurring(row pgx.Row) (domain.RecurringTransaction, error) {
	var x domain.RecurringTransaction
	err := row.Scan(&x.ID, &x.TransactionType, &x.Amount, &x.AccountID, &x.CategoryID, &x.Description, &x.Cadence, &x.NextDueDate, &x.EndsOn, &x.IsActive, &x.LastProcessedOn, &x.CreatedAt, &x.UpdatedAt)
	return x, err
}

const recurringFields = "id,transaction_type,amount::text,account_id,category_id,description,cadence,next_due_date::text,ends_on::text,is_active,last_processed_on::text,created_at,updated_at"

func (r *Repository) Recurring(ctx context.Context, tx pgx.Tx, userID, id string, lock bool) (domain.RecurringTransaction, error) {
	q := "select " + recurringFields + " from recurring_transactions where user_id=$1 and id=$2"
	if lock {
		q += " for update"
	}
	return scanRecurring(tx.QueryRow(ctx, q, userID, id))
}

func (r *Repository) RecurringTransactions(ctx context.Context, tx pgx.Tx, userID string, dueOnly bool) ([]domain.RecurringTransaction, error) {
	q := "select " + recurringFields + " from recurring_transactions where user_id=$1"
	if dueOnly {
		q += " and is_active and next_due_date <= current_date and (ends_on is null or next_due_date <= ends_on)"
	}
	q += " order by is_active desc,next_due_date,created_at"
	rows, err := tx.Query(ctx, q, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []domain.RecurringTransaction{}
	for rows.Next() {
		x, err := scanRecurring(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}

func (r *Repository) CreateRecurring(ctx context.Context, tx pgx.Tx, userID, kind, amount, accountID, categoryID string, description *string, cadence, due string, endsOn *string) (domain.RecurringTransaction, error) {
	var id string
	err := tx.QueryRow(ctx, `insert into recurring_transactions(user_id,transaction_type,amount,account_id,category_id,description,cadence,next_due_date,ends_on)
values($1,$2,$3::numeric,$4,$5,$6,$7,$8::date,$9::date) returning id`, userID, kind, amount, accountID, categoryID, description, cadence, due, endsOn).Scan(&id)
	if err != nil {
		return domain.RecurringTransaction{}, err
	}
	return r.Recurring(ctx, tx, userID, id, false)
}

func (r *Repository) UpdateRecurring(ctx context.Context, tx pgx.Tx, userID, id, kind, amount, accountID, categoryID string, description *string, cadence, due string, endsOn *string, active bool) (domain.RecurringTransaction, error) {
	tag, err := tx.Exec(ctx, `update recurring_transactions set transaction_type=$3,amount=$4::numeric,account_id=$5,category_id=$6,description=$7,cadence=$8,next_due_date=$9::date,ends_on=$10::date,is_active=$11
where id=$1 and user_id=$2`, id, userID, kind, amount, accountID, categoryID, description, cadence, due, endsOn, active)
	if err != nil {
		return domain.RecurringTransaction{}, err
	}
	if tag.RowsAffected() == 0 {
		return domain.RecurringTransaction{}, pgx.ErrNoRows
	}
	return r.Recurring(ctx, tx, userID, id, false)
}

func (r *Repository) AdvanceRecurring(ctx context.Context, tx pgx.Tx, userID, id, processed, next string, active bool) error {
	tag, err := tx.Exec(ctx, "update recurring_transactions set last_processed_on=$3::date,next_due_date=$4::date,is_active=$5 where id=$1 and user_id=$2", id, userID, processed, next, active)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *Repository) DeleteRecurring(ctx context.Context, tx pgx.Tx, userID, id string) error {
	tag, err := tx.Exec(ctx, "delete from recurring_transactions where id=$1 and user_id=$2", id, userID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *Repository) Dashboard(ctx context.Context, tx pgx.Tx, userID, start, end, groupBy string) (domain.MoneyDashboard, error) {
	var out domain.MoneyDashboard
	err := tx.QueryRow(ctx, `with legs as (
 select account_id,case transaction_type when 'income' then amount else -amount end delta from transactions where user_id=$1
 union all select transfer_account_id,amount from transactions where user_id=$1 and transaction_type='transfer'
)
select coalesce(sum(legs.delta),0)::text from accounts a left join legs on legs.account_id=a.id where a.user_id=$1 and a.archived_at is null`, userID).Scan(&out.TotalBalance)
	if err != nil {
		return out, err
	}
	err = tx.QueryRow(ctx, `select coalesce(sum(amount) filter(where transaction_type='income'),0)::text,coalesce(sum(amount) filter(where transaction_type='expense'),0)::text
from transactions where user_id=$1 and transaction_date between $2::date and $3::date`, userID, start, end).Scan(&out.Income, &out.Expense)
	if err != nil {
		return out, err
	}
	rows, err := tx.Query(ctx, `select c.id,c.name,coalesce(sum(t.amount),0)::text from categories c join transactions t on t.category_id=c.id and t.user_id=c.user_id
where t.user_id=$1 and t.transaction_type='expense' and t.transaction_date between $2::date and $3::date group by c.id,c.name order by sum(t.amount) desc,lower(c.name)`, userID, start, end)
	if err != nil {
		return out, err
	}
	defer rows.Close()
	out.CategorySpend = []domain.CategorySpend{}
	for rows.Next() {
		var x domain.CategorySpend
		if err := rows.Scan(&x.CategoryID, &x.CategoryName, &x.Amount); err != nil {
			return out, err
		}
		out.CategorySpend = append(out.CategorySpend, x)
	}
	if err := rows.Err(); err != nil {
		return out, err
	}
	unit := map[string]string{"day": "day", "week": "week", "month": "month"}[groupBy]
	if unit == "" {
		unit = "day"
	}
	trendQ := fmt.Sprintf(`select date_trunc('%s',transaction_date)::date::text,coalesce(sum(amount) filter(where transaction_type='income'),0)::text,coalesce(sum(amount) filter(where transaction_type='expense'),0)::text
from transactions where user_id=$1 and transaction_date between $2::date and $3::date group by 1 order by 1`, unit)
	rows, err = tx.Query(ctx, trendQ, userID, start, end)
	if err != nil {
		return out, err
	}
	defer rows.Close()
	out.Trend = []domain.MoneyTrendPoint{}
	for rows.Next() {
		var x domain.MoneyTrendPoint
		if err := rows.Scan(&x.Period, &x.Income, &x.Expense); err != nil {
			return out, err
		}
		out.Trend = append(out.Trend, x)
	}
	return out, rows.Err()
}
