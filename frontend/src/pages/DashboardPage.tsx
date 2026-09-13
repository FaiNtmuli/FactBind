import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDashboardSummary, getRecentOrders } from '../api/dashboardApi'
import { ErrorMessage } from '../components/ErrorMessage'
import { Loading } from '../components/Loading'
import { StatusBadge } from '../components/StatusBadge'
import type { DashboardSummary } from '../types/dashboard'
import type { OrderSummary } from '../types/order'
import { formatAmount, formatDateTime } from '../utils/format'

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [recentOrders, setRecentOrders] = useState<OrderSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [summaryData, orders] = await Promise.all([getDashboardSummary(), getRecentOrders(5)])
      setSummary(summaryData)
      setRecentOrders(orders)
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const cards = summary
    ? [
        { label: 'Users', value: summary.userCount, hint: `${summary.activeUserCount} active` },
        { label: 'Products', value: summary.productCount, hint: `${summary.onSaleProductCount} on sale` },
        { label: 'Orders', value: summary.orderCount, hint: `${summary.createdOrderCount} created` },
      ]
    : []

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="page-subtitle">Summary of users, products and orders.</p>
        </div>
        <button type="button" className="button button--secondary" onClick={() => void load()}>
          Refresh
        </button>
      </header>

      {loading && <Loading label="Loading dashboard..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={() => void load()} />}

      {!loading && !error && summary && (
        <>
          <div className="card-grid">
            {cards.map((card) => (
              <article className="stat-card" key={card.label}>
                <span className="stat-label">{card.label}</span>
                <strong className="stat-value" data-testid={`stat-${card.label.toLowerCase()}`}>
                  {card.value}
                </strong>
                <span className="stat-hint">{card.hint}</span>
              </article>
            ))}
          </div>

          <h2 className="section-title">Recent orders</h2>
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
                </tr>
              </thead>
              <tbody>
                {recentOrders.map((order) => (
                  <tr key={order.id}>
                    <td>
                      <Link to={`/orders/${order.id}`}>{order.id}</Link>
                    </td>
                    <td>{order.userName}</td>
                    <td>{order.itemCount}</td>
                    <td>{formatAmount(order.totalAmount)}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td>{formatDateTime(order.createdAt)}</td>
                  </tr>
                ))}
                {recentOrders.length === 0 && (
                  <tr>
                    <td colSpan={6} className="table-empty">
                      No orders yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  )
}
