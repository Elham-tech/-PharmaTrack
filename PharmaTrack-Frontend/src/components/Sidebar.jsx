/**
 * Sidebar Component
 * Navigation sidebar with grouped menu sections for the pharmacy management system.
 */
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

const menuSections = [
  {
    title: 'Overview',
    items: [
      { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    ],
  },
  {
    title: 'Pharmacy',
    items: [
      { path: '/medicines', label: 'Medicines', icon: '💊' },
      { path: '/categories', label: 'Categories', icon: '🏷️' },
      { path: '/manufacturers', label: 'Manufacturers', icon: '🏭' },
      { path: '/suppliers', label: 'Suppliers', icon: '🚚' },
    ],
  },
  {
    title: 'Inventory',
    items: [
      { path: '/inventory-batches', label: 'Batches', icon: '📦' },
      { path: '/stock-movements', label: 'Stock Movements', icon: '🔄' },
    ],
  },
  {
    title: 'Dispensing',
    items: [
      { path: '/prescriptions', label: 'Prescriptions', icon: '📝' },
      { path: '/dispensing-records', label: 'Dispensing Records', icon: '🧾' },
    ],
  },
  {
    title: 'Administration',
    items: [
      { path: '/users', label: 'Users', icon: '👤' },
      { path: '/audit-logs', label: 'Audit Logs', icon: '📜' },
    ],
  },
];

export default function Sidebar() {
  const location = useLocation();
  const { user, logout } = useAuth();
  const displayName = user?.fullName || user?.username || 'User';
  const authorities = (user?.authorities || []).map(a => a.name).join(', ') || 'No authorities';

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-logo">P</div>
        <div className="sidebar-brand">Pharma<span>Track</span></div>
      </div>
      <nav className="sidebar-nav">
        {menuSections.map((section) => (
          <div key={section.title} className="sidebar-section">
            <div className="sidebar-section-title">{section.title}</div>
            {section.items.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `sidebar-link ${isActive ? 'active' : ''}`
                }
              >
                <span className="icon">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
      <div className="sidebar-user">
        <div className="sidebar-avatar">{displayName.charAt(0).toUpperCase()}</div>
        <div className="sidebar-user-info">
          <div className="sidebar-user-name">{displayName}</div>
          <div className="sidebar-user-authorities">{authorities}</div>
        </div>
        <button className="sidebar-logout" title="Log out" onClick={logout}>⎋</button>
      </div>
    </aside>
  );
}
