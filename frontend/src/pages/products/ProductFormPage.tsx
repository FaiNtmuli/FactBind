import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/http'
import { createProduct, getProduct, updateProduct } from '../../api/productApi'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import type { ProductStatus } from '../../types/product'

interface FormState {
  name: string
  sku: string
  price: string
  stock: string
  status: ProductStatus
}

const emptyForm: FormState = { name: '', sku: '', price: '', stock: '0', status: 'ON_SALE' }

function validate(form: FormState): Record<string, string> {
  const errors: Record<string, string> = {}
  const price = Number(form.price)
  const stock = Number(form.stock)

  if (form.name.trim().length === 0) {
    errors.name = 'Name must not be blank'
  }
  if (form.sku.trim().length === 0) {
    errors.sku = 'SKU must not be blank'
  }
  if (!Number.isFinite(price) || price <= 0) {
    errors.price = 'Price must be greater than 0'
  }
  if (!Number.isInteger(stock) || stock < 0) {
    errors.stock = 'Stock must be greater than or equal to 0'
  }
  return errors
}

export function ProductFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = id !== undefined
  const productId = Number(id)
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
      const product = await getProduct(productId)
      setForm({
        name: product.name,
        sku: product.sku,
        price: String(product.price),
        stock: String(product.stock),
        status: product.status,
      })
    } catch (error) {
      setLoadError(error)
    } finally {
      setLoading(false)
    }
  }, [isEdit, productId])

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
      const payload = {
        name: form.name.trim(),
        sku: form.sku.trim(),
        price: Number(form.price),
        stock: Number(form.stock),
      }
      const saved = isEdit
        ? await updateProduct(productId, payload)
        : await createProduct({ ...payload, status: form.status })
      navigate(`/products/${saved.id}`)
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
        <Loading label="Loading product..." />
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
          <h1>{isEdit ? `Edit product ${productId}` : 'New product'}</h1>
          <p className="page-subtitle">SKUs are unique, prices must be positive.</p>
        </div>
      </header>

      {submitError !== null && <ErrorMessage error={submitError} />}

      <form className="form" onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input type="text" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
        </label>

        <label>
          SKU
          <input type="text" value={form.sku} onChange={(event) => setForm({ ...form, sku: event.target.value })} />
          {fieldErrors.sku && <span className="field-error">{fieldErrors.sku}</span>}
        </label>

        <label>
          Price
          <input
            type="number"
            step="0.01"
            value={form.price}
            onChange={(event) => setForm({ ...form, price: event.target.value })}
          />
          {fieldErrors.price && <span className="field-error">{fieldErrors.price}</span>}
        </label>

        <label>
          Stock
          <input
            type="number"
            value={form.stock}
            onChange={(event) => setForm({ ...form, stock: event.target.value })}
          />
          {fieldErrors.stock && <span className="field-error">{fieldErrors.stock}</span>}
        </label>

        {!isEdit && (
          <label>
            Status
            <select
              value={form.status}
              onChange={(event) => setForm({ ...form, status: event.target.value as ProductStatus })}
            >
              <option value="ON_SALE">On sale</option>
              <option value="OFF_SALE">Off sale</option>
            </select>
          </label>
        )}

        <div className="form-actions">
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button type="button" className="button button--secondary" onClick={() => navigate('/products')}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}
