import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { UserFormPage } from './UserFormPage'
import { mockFetch } from '../../test/mockFetch'

function renderNewUserPage() {
  return render(
    <MemoryRouter initialEntries={['/users/new']}>
      <Routes>
        <Route path="/users/new" element={<UserFormPage />} />
        <Route path="/users/:id" element={<p>User detail page</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('UserFormPage', () => {
  it('validates the form on the client before calling the API', async () => {
    const calls = mockFetch(() => ({ status: 201, body: {} }))
    const browser = userEvent.setup()

    renderNewUserPage()

    await browser.type(screen.getByLabelText('Name'), 'A')
    await browser.type(screen.getByLabelText('Email'), 'not-an-email')
    await browser.type(screen.getByLabelText('Age'), '0')
    await browser.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Name must be between 2 and 50 characters')).toBeInTheDocument()
    expect(screen.getByText('Email must be a well-formed email address')).toBeInTheDocument()
    expect(screen.getByText('Age must be between 1 and 150')).toBeInTheDocument()
    expect(calls.filter((call) => call.method === 'POST')).toHaveLength(0)
  })

  it('creates the user and navigates to the detail page', async () => {
    const calls = mockFetch(() => ({ status: 201, body: { id: 42 } }))
    const browser = userEvent.setup()

    renderNewUserPage()

    await browser.type(screen.getByLabelText('Name'), 'Nina Novak')
    await browser.type(screen.getByLabelText('Email'), 'nina@example.com')
    await browser.type(screen.getByLabelText('Age'), '33')
    await browser.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('User detail page')).toBeInTheDocument()
    expect(calls.at(-1)).toMatchObject({
      url: '/api/users',
      method: 'POST',
      body: { name: 'Nina Novak', email: 'nina@example.com', age: 33, status: 'ACTIVE' },
    })
  })

  it('renders the field errors returned by the backend', async () => {
    mockFetch(() => ({
      status: 409,
      body: { code: 'DUPLICATE_EMAIL', message: 'Email nina@example.com already exists' },
    }))
    const browser = userEvent.setup()

    renderNewUserPage()

    await browser.type(screen.getByLabelText('Name'), 'Nina Novak')
    await browser.type(screen.getByLabelText('Email'), 'nina@example.com')
    await browser.type(screen.getByLabelText('Age'), '33')
    await browser.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('already exists')
  })
})
