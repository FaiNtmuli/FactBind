import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteUser, getUser, updateUserStatus } from '../../api/userApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import type { User } from '../../types/user'
import { formatDateTime } from '../../utils/format'

export function UserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const navigate = useNavigate()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setUser(await getUser(userId))
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void load()
  }, [load])

  async function toggleStatus() {
    if (!user) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      setUser(await updateUserStatus(user.id, user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'))
    } catch (actionFailure) {
      setActionError(actionFailure)
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    if (!user) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      await deleteUser(user.id)
      navigate('/users')
    } catch (actionFailure) {
      setActionError(actionFailure)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>User detail</h1>
          <p className="page-subtitle">
            <Link to="/users">Back to the user list</Link>
          </p>
        </div>
        {user && (
          <div className="header-actions">
            <button type="button" className="button" onClick={() => navigate(`/users/${user.id}/edit`)}>
              Edit
            </button>
            <button type="button" className="button button--secondary" disabled={busy} onClick={() => void toggleStatus()}>
              {user.status === 'ACTIVE' ? 'Disable' : 'Enable'}
            </button>
            <button type="button" className="button button--danger" onClick={() => setConfirmingDelete(true)}>
              Delete
            </button>
          </div>
        )}
      </header>

      {actionError !== null && <ErrorMessage error={actionError} />}
      {loading && <Loading label="Loading user..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={() => void load()} />}

      {!loading && !error && user && (
        <dl className="detail-list">
          <div>
            <dt>Id</dt>
            <dd>{user.id}</dd>
          </div>
          <div>
            <dt>Name</dt>
            <dd>{user.name}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Age</dt>
            <dd>{user.age}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <StatusBadge status={user.status} />
            </dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatDateTime(user.createdAt)}</dd>
          </div>
          <div>
            <dt>Updated</dt>
            <dd>{formatDateTime(user.updatedAt)}</dd>
          </div>
        </dl>
      )}

      <ConfirmDialog
        open={confirmingDelete}
        title="Delete user"
        message="This cannot be undone."
        confirmLabel="Delete"
        busy={busy}
        error={actionError}
        onConfirm={() => void handleDelete()}
        onCancel={() => {
          setConfirmingDelete(false)
          setActionError(null)
        }}
      />
    </section>
  )
}
