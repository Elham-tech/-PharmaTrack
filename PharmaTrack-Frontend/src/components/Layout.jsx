/**
 * Layout Component
 * Main application layout wrapper with sidebar, header, and content area.
 */
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import { ToastProvider } from './Toast';

export default function Layout() {
  return (
    <ToastProvider>
      <div className="app-layout">
        <Sidebar />
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </ToastProvider>
  );
}
