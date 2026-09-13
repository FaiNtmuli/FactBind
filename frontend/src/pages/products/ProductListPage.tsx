import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { deleteProduct, searchProducts, updateProductStatus } from '../../api/productApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import type { PageResponse } from '../../types/common'
import type { Product, ProductStatus } from '../../types/product'
import { formatAmount } from '../../utils/format'

const PAGE_SIZE = 10

export function ProductListPage() {
  const navigate = useNavigate()
  const [keywordDraft, setKeywordDraft] = useState('')
  const [statusDraft, setStatusDraft] = useState<ProductStatus | ''>('')
  const [filters, setFilters] = useState<{ keyword: string; status?: ProductStatus }>({ keyword: '' })
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<Product> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [reloadToken, setReloadToken] = useState(0)
  const [pendingDelete, setPendingDelete] = useState<Product | null>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    searchProducts({
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

  async function handleToggleStatus(product: Product) {
    setActionError(null)
    setBusy(true)
    try {
      await updateProductStatus(product.id, product.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE')
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
      await deleteProduct(pendingDelete.id)
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
          <h1>Products</h1>
          <p className="page-subtitle">Search products, change stock and manage the sale status.</p>
        </div>
        <button type="button" className="button" onClick={() => navigate('/products/new')}>
          New product
        </button>
      </header>

      <form className="filters" onSubmit={handleSearch}>
        <label>
          Keyword
          <input
            type="text"
            value={keywordDraft}
            placeholder="name or sku"
            onChange={(event) => setKeywordDraft(event.target.value)}
          />
        </label>
        <label>
          Status
          <select value={statusDraft} onChange={(event) => setStatusDraft(event.target.value as ProductStatus | '')}>
            <option value="">All</option>
            <option value="ON_SALE">On sale</option>
            <option value="OFF_SALE">Off sale</option>
          </select>
        </label>
        <button type="submit" className="button">
          Search
        </button>
      </form>

      {actionError !== null && <ErrorMessage error={actionError} />}
      {loading && <Loading label="Loading products..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={reload} />}

      {!loading && !error && data && (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Name</th>
                  <th>SKU</th>
                  <th>Price</th>
                  <th>Stock</th>
                  <th>Status</th>
                  <th className="table-actions-header">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((product) => (
                  <tr key={product.id}>
                    <td>{product.id}</td>
                    <td>
                      <Link to={`/products/${product.id}`}>{product.name}</Link>
                    </td>
                    <td>
                      <code>{product.sku}</code>
                    </td>
                    <td>{formatAmount(product.price)}</td>
                    <td>{product.stock}</td>
                    <td>
                      <StatusBadge status={product.status} />
                    </td>
                    <td className="table-actions">
                      <button type="button" className="link-button" onClick={() => navigate(`/products/${product.id}/edit`)}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="link-button"
                        disabled={busy}
                        onClick={() => void handleToggleStatus(product)}
                      >
                        {product.status === 'ON_SALE' ? 'Take off sale' : 'Put on sale'}
                      </button>
                      <button type="button" className="link-button link-button--danger" onClick={() => setPendingDelete(product)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={7} className="table-empty">
                      No product matches the current filters.
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
        title="Delete product"
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
