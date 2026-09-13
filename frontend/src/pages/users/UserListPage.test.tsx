import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { UserListPage } from './UserListPage'
import { mockFetch, pageResponse, user } from '../../test/mockFetch'

function renderPage() {
  return render(
    <MemoryRouter>
      <UserListPage />
    </MemoryRouter>,
  )
}

describe('UserListPage', () => {
  it('renders the users returned by the API', async () => {
    mockFetch(() => ({ body: pageResponse([user(), user({ id: 2, name: 'Bob Brown', status: 'DISABLED' })]) }))

    renderPage()

    expect(await screen.findByText('Alice Anderson')).toBeInTheDocument()
    expect(screen.getByText('Bob Brown')).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Disabled' })).toBeInTheDocument()
    expect(screen.getByText(/2 items/)).toBeInTheDocument()
  })

  it('sends keyword, status and pagination when searching', async () => {
    const calls = mockFetch(() => ({ body: pageResponse([user()]) }))
    const browser = userEvent.setup()

    renderPage()
    await screen.findByText('Alice Anderson')

    await browser.type(screen.getByLabelText('Keyword'), 'alice')
    await browser.selectOptions(screen.getByLabelText('Status'), 'ACTIVE')
    await browser.click(screen.getByRole('button', { name: 'Search' }))

    const lastCall = calls.at(-1)
    expect(lastCall?.url).toContain('keyword=alice')
    expect(lastCall?.url).toContain('status=ACTIVE')
    expect(lastCall?.url).toContain('page=0')
    expect(lastCall?.url).toContain('size=10')
  })

  it('shows an error message when the list request fails', async () => {
    mockFetch(() => ({ status: 500, body: { code: 'INTERNAL_ERROR', message: 'Unexpected server error' } }))

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unexpected server error')
  })

  it('shows an empty state when no user matches', async () => {
    mockFetch(() => ({ body: pageResponse([]) }))

    renderPage()

    expect(await screen.findByText('No user matches the current filters.')).toBeInTheDocument()
  })
})
