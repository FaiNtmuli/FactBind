import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/http'
import { createUser, getUser, updateUser } from '../../api/userApi'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import type { UserStatus } from '../../types/user'

interface FormState {
  name: string
  email: string
  age: string
  status: UserStatus
}

const emptyForm: FormState = { name: '', email: '', age: '', status: 'ACTIVE' }

/**
 * Client side validation mirrors the Jakarta Validation rules of the backend.
 */
function validate(form: FormState): Record<string, string> {
  const errors: Record<string, string> = {}
  const name = form.name.trim()
  const age = Number(form.age)

  if (name.length < 2 || name.length > 50) {
    errors.name = 'Name must be between 2 and 50 characters'
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = 'Email must be a well-formed email address'
  }
  if (!Number.isInteger(age) || age < 1 || age > 150) {
    errors.age = 'Age must be between 1 and 150'
  }
  return errors
}

export function UserFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = id !== undefined
  const userId = Number(id)
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(isEdit)
  const [loadError, setLoadError] = useState<unknown>(null)
  const [submitError, setSubmitError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    if (!isEdit) {
      return
    }
    setLoading(true)
    setLoadError(null)
    try {
      const user = await getUser(userId)
      setForm({ name: user.name, email: user.email, age: String(user.age), status: user.status })
    } catch (error) {
      setLoadError(error)
    } finally {
      setLoading(false)
    }
  }, [isEdit, userId])

  useEffect(() => {
    void load()
  }, [load])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }

    setSaving(true)
    setSubmitError(null)
    try {
      const saved = isEdit
        ? await updateUser(userId, { name: form.name.trim(), email: form.email.trim(), age: Number(form.age) })
        : await createUser({
            name: form.name.trim(),
            email: form.email.trim(),
            age: Number(form.age),
            status: form.status,
          })
      navigate(`/users/${saved.id}`)
    } catch (error) {
      if (error instanceof ApiError && error.fields) {
        setFieldErrors(error.fields)
      }
      setSubmitError(error)
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <section className="page">
        <Loading label="Loading user..." />
      </section>
    )
  }

  if (loadError !== null) {
    return (
      <section className="page">
        <ErrorMessage error={loadError} onRetry={() => void load()} />
      </section>
    )
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>{isEdit ? `Edit user ${userId}` : 'New user'}</h1>
          <p className="page-subtitle">Fields are validated on the client and on the server.</p>
        </div>
      </header>

      {submitError !== null && <ErrorMessage error={submitError} />}

      <form className="form" onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input
            type="text"
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
          />
          {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
        </label>

        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
          />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </label>

        <label>
          Age
          <input
            type="number"
            value={form.age}
            onChange={(event) => setForm({ ...form, age: event.target.value })}
          />
          {fieldErrors.age && <span className="field-error">{fieldErrors.age}</span>}
        </label>

        {!isEdit && (
          <label>
            Status
            <select
              value={form.status}
              onChange={(event) => setForm({ ...form, status: event.target.value as UserStatus })}
            >
              <option value="ACTIVE">Active</option>
              <option value="DISABLED">Disabled</option>
            </select>
          </label>
        )}

        <div className="form-actions">
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button type="button" className="button button--secondary" onClick={() => navigate('/users')}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}
