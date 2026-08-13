/**
 * Dashboard Page
 * Overview page showing summary statistics across all entities.
 * Fetches counts from each API to display real-time pharmacy stats.
 */
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  userApi, medicineApi, categoryApi, manufacturerApi,
  supplierApi, inventoryBatchApi,
  prescriptionApi, dispensingRecordApi, auditLogApi
} from '../api/api';

export default function Dashboard() {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    users: 0, medicines: 0, categories: 0, manufacturers: 0,
    suppliers: 0, batches: 0, prescriptions: 0,
    dispensing: 0, auditLogs: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [users, medicines, categories, manufacturers,
               suppliers, batches, prescriptions,
               dispensing, auditLogs] = await Promise.allSettled([
          userApi.getAll(),
          medicineApi.getAll(),
          categoryApi.getAll(),
          manufacturerApi.getAll(),
          supplierApi.getAll(),
          inventoryBatchApi.getAll(),
          prescriptionApi.getAll(),
          dispensingRecordApi.getAll(),
          auditLogApi.getAll(),
        ]);
        setStats({
          users:            users.status === 'fulfilled' ? users.value.length : 0,
          medicines:        medicines.status === 'fulfilled' ? medicines.value.length : 0,
          categories:       categories.status === 'fulfilled' ? categories.value.length : 0,
          manufacturers:    manufacturers.status === 'fulfilled' ? manufacturers.value.length : 0,
          suppliers:        suppliers.status === 'fulfilled' ? suppliers.value.length : 0,
          batches:          batches.status === 'fulfilled' ? batches.value.length : 0,
          prescriptions:    prescriptions.status === 'fulfilled' ? prescriptions.value.length : 0,
          dispensing:       dispensing.status === 'fulfilled' ? dispensing.value.length : 0,
          auditLogs:        auditLogs.status === 'fulfilled' ? auditLogs.value.length : 0,
        });
      } catch (err) {
        console.error('Failed to load dashboard stats', err);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return (
      <>
        <div className="page-header"><h1>Dashboard</h1></div>
        <div className="page-body"><div className="loading-spinner"><div className="spinner" /></div></div>
      </>
    );
  }

  const cards = [
    { label: 'Medicines',    value: stats.medicines,      icon: '💊', color: 'teal',    path: '/medicines' },
    { label: 'Categories',   value: stats.categories,     icon: '🏷️', color: 'indigo',  path: '/categories' },
    { label: 'Manufacturers',value: stats.manufacturers,  icon: '🏭', color: 'amber',   path: '/manufacturers' },
    { label: 'Suppliers',    value: stats.suppliers,      icon: '🚚', color: 'green',   path: '/suppliers' },
    { label: 'Batches',      value: stats.batches,        icon: '📦', color: 'teal',    path: '/inventory-batches' },
    { label: 'Prescriptions',value: stats.prescriptions,  icon: '📝', color: 'green',   path: '/prescriptions' },
    { label: 'Dispensed',    value: stats.dispensing,     icon: '🧾', color: 'amber',   path: '/dispensing-records' },
    { label: 'Users',        value: stats.users,          icon: '👤', color: 'teal',    path: '/users' },
    { label: 'Audit Logs',   value: stats.auditLogs,      icon: '📜', color: 'gray',    path: '/audit-logs' },
  ];

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <div className="page-header-subtitle">Pharmacy management overview</div>
        </div>
      </div>
      <div className="page-body">
        <div className="stats-grid">
          {cards.map((card) => (
            <div
              key={card.label}
              className="stat-card"
              style={{ cursor: 'pointer' }}
              onClick={() => navigate(card.path)}
            >
              <div className={`stat-icon ${card.color}`}>{card.icon}</div>
              <div className="stat-info">
                <h3>{card.value}</h3>
                <p>{card.label}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
