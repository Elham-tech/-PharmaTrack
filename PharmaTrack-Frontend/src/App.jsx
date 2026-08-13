import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import Layout from './components/Layout'
import { AuthProvider, useAuth } from './components/AuthContext'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Users from './pages/Users'
import Medicines from './pages/Medicines'
import Categories from './pages/Categories'
import Manufacturers from './pages/Manufacturers'
import Suppliers from './pages/Suppliers'
import InventoryBatches from './pages/InventoryBatches'
import StockMovements from './pages/StockMovements'
import Prescriptions from './pages/Prescriptions'
import DispensingRecords from './pages/DispensingRecords'
import AuditLogs from './pages/AuditLogs'

/**
 * RequireAuth Gate
 * Blocks protected routes until the session is confirmed by /api/auth/me.
 * Unauthenticated visitors are redirected to the login page (remembering
 * where they wanted to go so login can send them back).
 */
function RequireAuth({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="auth-loading">
        <div className="loading-spinner"><div className="spinner" /></div>
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return children;
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="users" element={<Users />} />
            <Route path="medicines" element={<Medicines />} />
            <Route path="categories" element={<Categories />} />
            <Route path="manufacturers" element={<Manufacturers />} />
            <Route path="suppliers" element={<Suppliers />} />
            <Route path="inventory-batches" element={<InventoryBatches />} />
            <Route path="stock-movements" element={<StockMovements />} />
            <Route path="prescriptions" element={<Prescriptions />} />
            <Route path="dispensing-records" element={<DispensingRecords />} />
            <Route path="audit-logs" element={<AuditLogs />} />
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
  )
}

export default App
