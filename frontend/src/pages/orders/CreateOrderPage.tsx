import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createOrder } from '../../api/orderApi'
import { searchProducts } from '../../api/productApi'
import { searchUsers } from '../../api/userApi'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import type { Product } from '../../types/product'
import type { User } from '../../types/user'
import { formatAmount } from '../../utils/format'

interface ItemRow {
  productId: string
  quantity: string
}

const emptyRow: ItemRow = { productId: '', quantity: '1' }

export function CreateOrderPage() {
  const navigate = useNavigate()

  const [users, setUsers] = useState<User[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [userId, setUserId] = useState('')
  const [remark, setRemark] = useState('')
  const [rows, setRows] = useState<ItemRow[]>([{ ...emptyRow }])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<unknown>(null)
  const [submitError, setSubmitError] = useState<unknown>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [userPage, productPage] = await Promise.all([
        searchUsers({ status: 'ACTIVE', size: 100, page: 0 }),
        searchProducts({ status: 'ON_SALE', size: 100, page: 0 }),
      ])
      setUsers(userPage.content)
      setProducts(productPage.content)
    } catch (error) {
      setLoadError(error)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  function updateRow(index: number, patch: Partial<ItemRow>) {
    setRows((current) => current.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)))
  }

  function addRow() {
    setRows((current) => [...current, { ...emptyRow }])
  }

  function removeRow(index: number) {
    setRows((current) => (current.length === 1 ? current : current.filter((_, rowIndex) => rowIndex !== index)))
  }

  /**
   * The preview is only a client side estimate; the backend always recalculates the total.
   */
  const estimatedTotal = rows.reduce((total, row) => {
    const product = products.find((candidate) => String(candidate.id) === row.productId)
    const quantity = Number(row.quantity)
    if (!product || !Number.isFinite(quantity) || quantity <= 0) {
      return total
    }
    return total + product.price * quantity
  }, 0)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)
    setFormError(null)

    if (userId === '') {
      setFormError('Please choose a customer')
      return
    }
    const items = rows
      .filter((row) => row.productId !== '')
      .map((row) => ({ productId: Number(row.productId), quantity: Number(row.quantity) }))

    if (items.length === 0) {
      setFormError('An order needs at least one product')
      return
    }
    if (items.some((item) => !Number.isInteger(item.quantity) || item.quantity < 1)) {
      setFormError('Every quantity must be at least 1')
      return
    }

    setSaving(true)
    try {
      const created = await createOrder({
        userId: Number(userId),
        remark: remark.trim() === '' ? undefined : remark.trim(),
        items,
      })
      navigate(`/orders/${created.id}`)
    } catch (error) {
      setSubmitError(error)
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <section className="page">
        <Loading label="Loading customers and products..." />
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
          <h1>New order</h1>
          <p className="page-subtitle">
            Prices, subtotals and the total amount are calculated by the backend.
          </p>
        </div>
      </header>

      {formError !== null && (
        <p className="field-error" role="alert">
          {formError}
        </p>
      )}
      {submitError !== null && <ErrorMessage error={submitError} />}

      <form className="form" onSubmit={handleSubmit} noValidate>
        <label>
          Customer
          <select value={userId} onChange={(event) => setUserId(event.target.value)}>
            <option value="">Select an active user</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>
                #{user.id} {user.name} ({user.email})
              </option>
            ))}
          </select>
        </label>

        <label>
          Remark
          <input type="text" value={remark} onChange={(event) => setRemark(event.target.value)} />
        </label>

        <fieldset className="items-editor">
          <legend>Items</legend>
          {rows.map((row, index) => (
            <div className="item-row" key={index}>
              <label>
                Product
                <select value={row.productId} onChange={(event) => updateRow(index, { productId: event.target.value })}>
                  <option value="">Select a product</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      #{product.id} {product.name} - {formatAmount(product.price)} ({product.stock} in stock)
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Quantity
                <input
                  type="number"
                  min={1}
                  value={row.quantity}
                  onChange={(event) => updateRow(index, { quantity: event.target.value })}
                />
              </label>
              <button
                type="button"
                className="button button--secondary"
                onClick={() => removeRow(index)}
                disabled={rows.length === 1}
              >
                Remove
              </button>
            </div>
          ))}
          <button type="button" className="button button--secondary" onClick={addRow}>
            Add item
          </button>
        </fieldset>

        <p className="muted">Estimated total (backend recalculates): {formatAmount(estimatedTotal)}</p>

        <div className="form-actions">
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Creating...' : 'Create order'}
          </button>
          <button type="button" className="button button--secondary" onClick={() => navigate('/orders')}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}
