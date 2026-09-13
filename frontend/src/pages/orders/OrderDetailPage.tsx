import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteOrder, getOrder, updateOrderStatus } from '../../api/orderApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorMessage } from '../../components/ErrorMessage'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import type { Order, OrderStatus } from '../../types/order'
import { formatAmount, formatDateTime } from '../../utils/format'

/**
 * Mirrors the transition table of the backend, so the UI only offers valid next steps.
 */
const allowedTransitions: Record<OrderStatus, OrderStatus[]> = {
  CREATED: ['PAID', 'CANCELLED'],
  PAID: ['COMPLETED'],
  CANCELLED: [],
  COMPLETED: [],
}

const transitionLabels: Record<OrderStatus, string> = {
  CREATED: 'Reset to created',
  PAID: 'Mark as paid',
  CANCELLED: 'Cancel order',
  COMPLETED: 'Complete order',
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const orderId = Number(id)
  const navigate = useNavigate()

  const [order, setOrder] = useState<Order | null>(null)
  const [notify, setNotify] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setOrder(await getOrder(orderId))
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [orderId])

  useEffect(() => {
    void load()
  }, [load])

  async function changeStatus(status: OrderStatus) {
    setBusy(true)
    setActionError(null)
    try {
      setOrder(await updateOrderStatus(orderId, status, notify))
    } catch (actionFailure) {
      setActionError(actionFailure)
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    setBusy(true)
    setActionError(null)
    try {
      await deleteOrder(orderId)
      navigate('/orders')
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
          <h1>Order #{orderId}</h1>
          <p className="page-subtitle">
            <Link to="/orders">Back to the order list</Link>
          </p>
        </div>
        {order && (
          <button type="button" className="button button--danger" onClick={() => setConfirmingDelete(true)}>
            Delete
          </button>
        )}
      </header>

      {actionError !== null && <ErrorMessage error={actionError} />}
      {loading && <Loading label="Loading order..." />}
      {!loading && error !== null && <ErrorMessage error={error} onRetry={() => void load()} />}

      {!loading && !error && order && (
        <>
          <dl className="detail-list">
            <div>
              <dt>Customer</dt>
              <dd>
                <Link to={`/users/${order.userId}`}>{order.userName}</Link>
              </dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{order.userEmail}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <StatusBadge status={order.status} />
              </dd>
            </div>
            <div>
              <dt>Total amount</dt>
              <dd>{formatAmount(order.totalAmount)}</dd>
            </div>
            <div>
              <dt>Remark</dt>
              <dd>{order.remark ?? '-'}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDateTime(order.createdAt)}</dd>
            </div>
          </dl>

          <h2 className="section-title">Items</h2>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Unit price</th>
                  <th>Quantity</th>
                  <th>Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <Link to={`/products/${item.productId}`}>{item.productName}</Link>
                    </td>
                    <td>{formatAmount(item.unitPrice)}</td>
                    <td>{item.quantity}</td>
                    <td>{formatAmount(item.subtotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="panel">
            <h2 className="section-title">Change status</h2>
            <label className="checkbox">
              <input type="checkbox" checked={notify} onChange={(event) => setNotify(event.target.checked)} />
              Request a notification (query parameter <code>notify=true</code>)
            </label>
            <div className="form-actions">
              {allowedTransitions[order.status].map((status) => (
                <button
                  key={status}
                  type="button"
                  className="button button--secondary"
                  disabled={busy}
                  onClick={() => void changeStatus(status)}
                >
                  {transitionLabels[status]}
                </button>
              ))}
              {allowedTransitions[order.status].length === 0 && (
                <p className="muted">This order is in a final status and cannot be changed.</p>
              )}
            </div>
          </div>
        </>
      )}

      <ConfirmDialog
        open={confirmingDelete}
        title="Delete order"
        message="Deleting an order does not give the stock back."
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
