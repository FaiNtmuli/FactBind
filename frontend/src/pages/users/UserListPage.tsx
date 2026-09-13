import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { deleteUser, searchUsers, updateUserStatus } from '../../api/userApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import type { PageResponse } from '../../types/common'
import type { User, UserStatus } from '../../types/user'

const PAGE_SIZE = 10

export function UserListPage() {
  const navigate = useNavigate()
  const [keywordDraft, setKeywordDraft] = useState('')
  const [statusDraft, setStatusDraft] = useState<UserStatus | ''>('')
  const [filters, setFilters] = useState<{ keyword: string; status?: UserStatus }>({ keyword: '' })
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<User> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [reloadToken, setReloadToken] = useState(0)
  const [pendingDelete, setPendingDelete] = useState<User | null>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    searchUsers({
      keyword: filters.keyword || undefined,
      status: filters.status,
      page,
      size: PAGE_SIZE,
    })
      .then((result) => {
        if (!cancelled) {
          setData(result)
        }
      })
      .catch((loadError: unknown) => {
        if (!cancelled) {
          setError(loadError)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [filters, page, reloadToken])

  function reload() {
    setReloadToken((token) => token + 1)
  }

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setFilters({ keyword: keywordDraft.trim(), status: statusDraft || undefined })
  }

  async function handleToggleStatus(user: User) {
    setActionError(null)
    setBusy(true)
    try {
      await updateUserStatus(user.id, user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
      reload()
    } catch (actionFailure) {
      setActionError(actionFailure)
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    if (!pendingDelete) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      await deleteUser(pendingDelete.id)
      setPendingDelete(null)
      reload()
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
          <h1>Users</h1>
          <p className="page-subtitle">Search, filter and manage users.</p>
        </div>
        <button type="button" className="button" onClick={() => navigate('/users/new')}>
          New user
        </button>
      </header>

      <form className="filters" onSubmit={handleSearch}>
        <label>
          Keyword
          <input
            type="text"
            value={keywordDraft}
            placeholder="name or email"
            onChange={(event) => setKeywordDraft(event.target.value)}
          />
        </label>
        <label>
          Status
          <select value={statusDraft} onChange={(event) => setStatusDraft(event.target.value as UserStatus | '')}>
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </label>
        <button type="submit" className="button">
          Search
        </button>
      </form>

      {actionError !== null && <ErrorMessage error={actionError} />}
      {loading && <Loading label="Loading users..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={reload} />}

      {!loading && !error && data && (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Age</th>
                  <th>Status</th>
                  <th className="table-actions-header">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>
                      <Link to={`/users/${user.id}`}>{user.name}</Link>
                    </td>
                    <td>{user.email}</td>
                    <td>{user.age}</td>
                    <td>
                      <StatusBadge status={user.status} />
                    </td>
                    <td className="table-actions">
                      <button type="button" className="link-button" onClick={() => navigate(`/users/${user.id}/edit`)}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="link-button"
                        disabled={busy}
                        onClick={() => void handleToggleStatus(user)}
                      >
                        {user.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                      </button>
                      <button type="button" className="link-button link-button--danger" onClick={() => setPendingDelete(user)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} className="table-empty">
                      No user matches the current filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={setPage}
          />
        </>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete user"
        message={`Delete ${pendingDelete?.name}? This cannot be undone.`}
        confirmLabel="Delete"
        busy={busy}
        error={actionError}
        onConfirm={() => void handleDelete()}
        onCancel={() => {
          setPendingDelete(null)
          setActionError(null)
        }}
      />
    </section>
  )
}
