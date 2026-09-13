import { createBrowserRouter } from 'react-router-dom'
import { Layout } from '../components/Layout'
import { DashboardPage } from '../pages/DashboardPage'
import { UserListPage } from '../pages/users/UserListPage'
import { UserDetailPage } from '../pages/users/UserDetailPage'
import { UserFormPage } from '../pages/users/UserFormPage'
import { ProductListPage } from '../pages/products/ProductListPage'
import { ProductDetailPage } from '../pages/products/ProductDetailPage'
import { ProductFormPage } from '../pages/products/ProductFormPage'
import { OrderListPage } from '../pages/orders/OrderListPage'
import { OrderDetailPage } from '../pages/orders/OrderDetailPage'
import { CreateOrderPage } from '../pages/orders/CreateOrderPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <DashboardPage /> },

      { path: 'users', element: <UserListPage /> },
      { path: 'users/new', element: <UserFormPage /> },
      { path: 'users/:id', element: <UserDetailPage /> },
      { path: 'users/:id/edit', element: <UserFormPage /> },

      { path: 'products', element: <ProductListPage /> },
      { path: 'products/new', element: <ProductFormPage /> },
      { path: 'products/:id', element: <ProductDetailPage /> },
      { path: 'products/:id/edit', element: <ProductFormPage /> },

      { path: 'orders', element: <OrderListPage /> },
      { path: 'orders/new', element: <CreateOrderPage /> },
      { path: 'orders/:id', element: <OrderDetailPage /> },
    ],
  },
])
