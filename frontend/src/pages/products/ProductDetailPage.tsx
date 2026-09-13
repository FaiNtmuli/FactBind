import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteProduct, getProduct, updateProductStatus, updateProductStock } from '../../api/productApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import type { Product } from '../../types/product'
import { formatAmount, formatDateTime } from '../../utils/format'

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const productId = Number(id)
  const navigate = useNavigate()

  const [product, setProduct] = useState<Product | null>(null)
  const [stockDraft, setStockDraft] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const loaded = await getProduct(productId)
      setProduct(loaded)
      setStockDraft(String(loaded.stock))
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [productId])

  useEffect(() => {
    void load()
  }, [load])

  async function saveStock() {
    if (!product) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const updated = await updateProductStock(product.id, Number(stockDraft))
      setProduct(updated)
      setStockDraft(String(updated.stock))
    } catch (actionFailure) {
      setActionError(actionFailure)
    } finally {
      setBusy(false)
    }
  }

  async function toggleStatus() {
    if (!product) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      setProduct(await updateProductStatus(product.id, product.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'))
    } catch (actionFailure) {
      setActionError(actionFailure)
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    if (!product) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      await deleteProduct(product.id)
      navigate('/products')
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
          <h1>Product detail</h1>
          <p className="page-subtitle">
            <Link to="/products">Back to the product list</Link>
          </p>
        </div>
        {product && (
          <div className="header-actions">
            <button type="button" className="button" onClick={() => navigate(`/products/${product.id}/edit`)}>
              Edit
            </button>
            <button type="button" className="button button--secondary" disabled={busy} onClick={() => void toggleStatus()}>
              {product.status === 'ON_SALE' ? 'Take off sale' : 'Put on sale'}
            </button>
            <button type="button" className="button button--danger" onClick={() => setConfirmingDelete(true)}>
              Delete
            </button>
          </div>
        )}
      </header>

      {actionError !== null && <ErrorMessage error={actionError} />}
      {loading && <Loading label="Loading product..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={() => void load()} />}

      {!loading && !error && product && (
        <>
          <dl className="detail-list">
            <div>
              <dt>Id</dt>
              <dd>{product.id}</dd>
            </div>
            <div>
              <dt>Name</dt>
              <dd>{product.name}</dd>
            </div>
            <div>
              <dt>SKU</dt>
              <dd>
                <code>{product.sku}</code>
              </dd>
            </div>
            <div>
              <dt>Price</dt>
              <dd>{formatAmount(product.price)}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <StatusBadge status={product.status} />
              </dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDateTime(product.createdAt)}</dd>
            </div>
          </dl>

          <div className="panel">
            <h2 className="section-title">Stock</h2>
            <div className="inline-form">
              <label>
                Units in stock
                <input
                  type="number"
                  min={0}
                  value={stockDraft}
                  onChange={(event) => setStockDraft(event.target.value)}
                />
              </label>
              <button type="button" className="button" disabled={busy} onClick={() => void saveStock()}>
                Save stock
              </button>
            </div>
          </div>
        </>
      )}

      <ConfirmDialog
        open={confirmingDelete}
        title="Delete product"
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
