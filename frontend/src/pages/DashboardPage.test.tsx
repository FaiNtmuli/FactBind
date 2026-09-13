import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { DashboardPage } from './DashboardPage'
import { mockFetch } from '../test/mockFetch'

const summary = {
  userCount: 20,
  activeUserCount: 16,
  productCount: 25,
  onSaleProductCount: 21,
  orderCount: 40,
  createdOrderCount: 12,
}

const recentOrders = [
  {
    id: 9,
    userId: 3,
    userName: 'Carol Clark',
    status: 'PAID',
    totalAmount: 199.8,
    itemCount: 2,
    createdAt: '2024-01-01T10:00:00Z',
  },
]

function renderPage() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  it('shows a loading indicator first', () => {
    mockFetch((call) => (call.url.includes('summary') ? { body: summary } : { body: recentOrders }))

    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Loading dashboard...')
  })

  it('renders the summary counters and the recent orders', async () => {
    mockFetch((call) => (call.url.includes('summary') ? { body: summary } : { body: recentOrders }))

    renderPage()

    expect(await screen.findByTestId('stat-users')).toHaveTextContent('20')
    expect(screen.getByTestId('stat-products')).toHaveTextContent('25')
    expect(screen.getByTestId('stat-orders')).toHaveTextContent('40')
    expect(screen.getByText('Carol Clark')).toBeInTheDocument()
    expect(screen.getByText('Paid')).toBeInTheDocument()
    expect(screen.getByText('199.80')).toBeInTheDocument()
  })

  it('shows an error message when the backend fails', async () => {
    mockFetch(() => ({ status: 500, body: { code: 'INTERNAL_ERROR', message: 'Unexpected server error' } }))

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unexpected server error')
  })
})
