import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { searchOrders } from '../../api/orderApi'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import type { PageResponse } from '../../types/common'
import type { Order, OrderStatus } from '../../types/order'
import { formatAmount, formatDateTime } from '../../utils/format'

const PAGE_SIZE = 10

export function OrderListPage() {
  const navigate = useNavigate()
  const [userIdDraft, setUserIdDraft] = useState('')
  const [statusDraft, setStatusDraft] = useState<OrderStatus | ''>('')
  const [filters, setFilters] = useState<{ userId?: number; status?: OrderStatus }>({})
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<Order> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await searchOrders({ userId: filters.userId, status: filters.status, page, size: PAGE_SIZE }))
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [filters, page])

  useEffect(() => {
    void load()
  }, [load])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setFilters({
      userId: userIdDraft.trim() === '' ? undefined : Number(userIdDraft),
      status: statusDraft || undefined,
    })
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Orders</h1>
          <p className="page-subtitle">Filter orders by customer and status.</p>
        </div>
        <button type="button" className="button" onClick={() => navigate('/orders/new')}>
          New order
        </button>
      </header>

      <form className="filters" onSubmit={handleSearch}>
        <label>
          User id
          <input
            type="number"
            min={1}
            value={userIdDraft}
            placeholder="any"
            onChange={(event) => setUserIdDraft(event.target.value)}
          />
        </label>
        <label>
          Status
          <select value={statusDraft} onChange={(event) => setStatusDraft(event.target.value as OrderStatus | '')}>
            <option value="">All</option>
            <option value="CREATED">Created</option>
            <option value="PAID">Paid</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </label>
        <button type="submit" className="button">
          Search
        </button>
      </form>

      {loading && <Loading label="Loading orders..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={() => void load()} />}

      {!loading && !error && data && (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Customer</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th className="table-actions-header">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((order) => (
                  <tr key={order.id}>
                    <td>{order.id}</td>
                    <td>
                      <Link to={`/users/${order.userId}`}>{order.userName}</Link>
                    </td>
                    <td>{order.items.length}</td>
                    <td>{formatAmount(order.totalAmount)}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td className="table-actions">
                      <button type="button" className="link-button" onClick={() => navigate(`/orders/${order.id}`)}>
                        View
                      </button>
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={7} className="table-empty">
                      No order matches the current filters.
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
    </section>
  )
}
