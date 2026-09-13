import { NavLink, Outlet } from 'react-router-dom'

const navigation = [
  { to: '/', label: 'Dashboard' },
  { to: '/users', label: 'Users' },
  { to: '/products', label: 'Products' },
  { to: '/orders', label: 'Orders' },
]

export function Layout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">MD</span>
          <span className="brand-text">
            MidDemo
            <small>Management System</small>
          </span>
        </div>
        <nav className="nav">
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => (isActive ? 'nav-link nav-link--active' : 'nav-link')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <p className="sidebar-footnote">Native Spring Baseline</p>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
